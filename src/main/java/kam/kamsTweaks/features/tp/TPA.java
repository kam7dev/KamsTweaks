package kam.kamsTweaks.features.tp;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TPA extends Feature {
    private static class TPARequest {
        Player target;
        boolean here;
        Listener listener;
        int taskId;
        public TPARequest(Player target, boolean here, Listener listener, int taskId) {
            this.target = target;
            this.here = here;
            this.listener = listener;
            this.taskId = taskId;
        }
    }

    private final Map<Player, TPARequest> tpas = new LinkedHashMap<>();

    private void sendRequest(Player sender, Player target, boolean here) {
        if (tpas.containsKey(sender)) {
            sender.sendMessage(Component.text("You already have an outgoing TPA request.").color(NamedTextColor.RED));
            return;
        }
        var handler = TeleportFeatures.get();
        if (handler.teleportations.containsKey(sender)) {
            sender.sendMessage(Component.text("You are already teleporting somewhere.").color(NamedTextColor.RED));
            return;
        }
        if (handler.onCooldown.containsKey(sender)) {
            sender.sendMessage(Component.text("You're currently on teleportation cooldown for " + handler.onCooldown.get(sender) + " seconds.").color(NamedTextColor.RED));
            return;
        }
        if (target.equals(sender)) {
            sender.sendMessage(Component.text("You can't teleport to yourself, silly!").color(NamedTextColor.RED));
            return;
        }

        String type = here ? "TPAHere" : "TPA";
        sender.sendMessage(
                Component.text(type + " request sent to ").color(NamedTextColor.GOLD)
                        .append(target.displayName().color(NamedTextColor.RED))
                        .append(Component.text(". They have 60 seconds to accept it. You can cancel this by running ").color(NamedTextColor.GOLD))
                        .append(Component.text("/tpcancel").clickEvent(ClickEvent.runCommand("tpcancel")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED))
                        .append(Component.text(".").color(NamedTextColor.GOLD)));

        target.sendMessage(sender.displayName().color(NamedTextColor.RED)
                .append(Component.text(here ? " requested you teleport to them. Run " : " requested to teleport to you. Run ").color(NamedTextColor.GOLD))
                .append(Component.text("/tpaccept").clickEvent(ClickEvent.runCommand("tpaccept")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED))
                .append(Component.text(" to accept it, or ").color(NamedTextColor.GOLD))
                .append(Component.text("/tpdecline").clickEvent(ClickEvent.runCommand("tpdecline")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED))
                .append(Component.text(" to decline. This request expires in 60 seconds.").color(NamedTextColor.GOLD)));

        Listener listener = new Listener() {
            @EventHandler
            public void onPlayerLeave(PlayerQuitEvent event) {
                var left = event.getPlayer().equals(sender) ? sender : event.getPlayer().equals(target) ? target : null;
                if (left != null) {
                    cancel(sender, target, left.displayName().color(NamedTextColor.RED).append(Component.text(" left").color(NamedTextColor.GOLD)));
                }
            }
            @EventHandler
            public void onPlayerDie(PlayerDeathEvent event) {
                var died = event.getPlayer().equals(sender) ? sender : event.getPlayer().equals(target) ? target : null;
                if (died != null) {
                    cancel(sender, target, died.displayName().color(NamedTextColor.RED).append(Component.text(" died").color(NamedTextColor.GOLD)));
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(listener, KamsTweaks.getInstance());

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
            sender.sendMessage(Component.text("Your request to ").color(NamedTextColor.GOLD)
                    .append(target.displayName().color(NamedTextColor.RED))
                    .append(Component.text(" has expired.").color(NamedTextColor.GOLD)));
            target.sendMessage(sender.displayName().color(NamedTextColor.RED)
                    .append(Component.text("'s request has expired.").color(NamedTextColor.GOLD)));
            tpas.remove(sender);
            handler.teleportations.remove(sender);
            HandlerList.unregisterAll(listener);
        }, 1200);

        tpas.put(sender, new TPARequest(target, here, listener, taskId));
    }

    private void accept(Player acceptor, Player requester) {
        var handler = TeleportFeatures.get();
        TPARequest req = tpas.get(requester);
        if (req == null) return;
        if (req.here) {
            if (handler.onCooldown.containsKey(acceptor)) {
                acceptor.sendMessage(Component.text("You're currently on teleportation cooldown for " + handler.onCooldown.get(acceptor) + " seconds.").color(NamedTextColor.RED));
                return;
            }
        } else {
            if (handler.onCooldown.containsKey(requester)) {
                acceptor.sendMessage(requester.displayName().append(Component.text(" is currently on teleportation cooldown.").color(NamedTextColor.RED)));
                return;
            }
        }

        Bukkit.getScheduler().cancelTask(req.taskId);
        HandlerList.unregisterAll(req.listener);
        tpas.remove(requester);

        double time = KamsTweaks.getInstance().getConfig().getDouble("teleportation.timer");

        if (req.here) {
            handler.scheduleTeleport(acceptor, requester, time);
        } else {
            handler.scheduleTeleport(requester, acceptor, time);
        }

        acceptor.sendMessage(Component.text("You accepted the request from ").color(NamedTextColor.GOLD)
                .append(requester.displayName().color(NamedTextColor.RED))
                .append(Component.text(". " + (
                        req.here ? (time > 0 ? "You will be teleported to them in " : "You have been teleported to them")
                                : (time > 0 ? "They will be teleported to you in " : "They have been teleported to you")
                )).color(NamedTextColor.GOLD))
                .append(Component.text(time > 0 ? (time + " seconds") : "").color(NamedTextColor.RED))
                .append(Component.text(".").color(NamedTextColor.GOLD)));
        requester.sendMessage(Component.text("Your request to ").color(NamedTextColor.GOLD)
                .append(acceptor.displayName().color(NamedTextColor.RED))
                .append(Component.text(" was accepted.").color(NamedTextColor.GOLD))
                .append(Component.text(". " + (
                        req.here ? (time > 0 ? "They will be teleported to you in " : "They have been teleported to you")
                                : (time > 0 ? "You will be teleported to them in " : "You have been teleported to them")
                )).color(NamedTextColor.GOLD))
                .append(Component.text(time > 0 ? (time + " seconds") : "").color(NamedTextColor.RED))
                .append(Component.text(".").color(NamedTextColor.GOLD)));
    }

    private void decline(Player decliner, Player requester) {
        TPARequest req = tpas.get(requester);
        if (req == null) return;

        Bukkit.getScheduler().cancelTask(req.taskId);
        HandlerList.unregisterAll(req.listener);
        tpas.remove(requester);

        decliner.sendMessage(Component.text("You declined the request from ").color(NamedTextColor.GOLD)
                .append(requester.displayName().color(NamedTextColor.RED))
                .append(Component.text(".").color(NamedTextColor.GOLD)));
        requester.sendMessage(Component.text("Your request to ").color(NamedTextColor.GOLD)
                .append(decliner.displayName().color(NamedTextColor.RED))
                .append(Component.text(" was declined.").color(NamedTextColor.GOLD)));
        TeleportFeatures.get().teleportations.remove(requester);
    }

    private void cancel(Player sender, Player target, Component reason) {
        TPARequest req = tpas.remove(sender);
        if (req == null) return;
        Bukkit.getScheduler().cancelTask(req.taskId);
        HandlerList.unregisterAll(req.listener);
        target.sendMessage(Component.text("Teleport request cancelled because ").color(NamedTextColor.GOLD)
                .append(sender.displayName().color(NamedTextColor.RED))
                .append(reason));
        TeleportFeatures.get().teleportations.remove(sender);
    }

    @Override
    public void setup() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("tpa")
                .requires(src -> src.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .then(Commands.argument("player", ArgumentTypes.player()).executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
                        sendRequest(p, target, false);
                    }
                    return Command.SINGLE_SUCCESS;
                })).build());
        commands.registrar().register(Commands.literal("tpahere")
                .requires(src -> src.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .then(Commands.argument("player", ArgumentTypes.player()).executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
                        sendRequest(p, target, true);
                    }
                    return Command.SINGLE_SUCCESS;
                })).build());
        commands.registrar().register(Commands.literal("tpaccept")
                .requires(src -> src.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        Player requester = tpas.entrySet().stream()
                                .filter(e -> e.getValue().target.equals(p))
                                .map(Map.Entry::getKey).findFirst().orElse(null);
                        if (requester != null) accept(p, requester);
                        else p.sendMessage(Component.text("No pending requests.").color(NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                }).build());
        commands.registrar().register(Commands.literal("tpdecline")
                .requires(src -> src.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        Player requester = tpas.entrySet().stream()
                                .filter(e -> e.getValue().target.equals(p))
                                .map(Map.Entry::getKey).findFirst().orElse(null);
                        if (requester != null) decline(p, requester);
                        else p.sendMessage(Component.text("No pending requests.").color(NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                }).build());
        commands.registrar().register(Commands.literal("tpcancel")
                .requires(src -> src.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        TPARequest req = tpas.get(p);
                        if (req != null) cancel(p, req.target, p.displayName().color(NamedTextColor.RED).append(Component.text(" cancelled it").color(NamedTextColor.GOLD)));
                        else p.sendMessage(Component.text("No outgoing requests.").color(NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                }).build());
    }

    @Override
    public void loadData() {

    }

    @Override
    public void saveData() {

    }
}