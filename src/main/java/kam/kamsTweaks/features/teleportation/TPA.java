package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TPA {
    TeleportationHandler handler;

    LinkedHashMap<Player, Pair<Player, Pair<Listener, Integer>>> tpas = new LinkedHashMap<>();

    public void init(TeleportationHandler handler) {
        this.handler = handler;
    }

    void accept(Player who, Player teleporting) {
        if (!tpas.containsKey(teleporting)) return;
        Bukkit.getScheduler().cancelTask(tpas.get(teleporting).second.second);
        tpas.remove(teleporting);
        double time = KamsTweaks.getInstance().getConfig().getDouble("teleportation.timer");
        teleporting.sendMessage(
                Component.text("You have accepted the TPA request from ").color(NamedTextColor.GOLD)
                        .append(who.displayName().color(NamedTextColor.RED))
                        .append(Component.text(". " + (time > 0 ? "They have been teleported to you." : "They will be teleported to you")).color(NamedTextColor.GOLD))
                        .append(Component.text(time > 0 ? (" in " + time + " seconds") : "")).color(NamedTextColor.RED)
                        .append(Component.text(".").color(NamedTextColor.GOLD)));
        teleporting.sendMessage(
                who.displayName().color(NamedTextColor.RED)
                        .append(Component.text(" has accepted your TPA request. ").color(NamedTextColor.GOLD))
                        .append(Component.text(time > 0 ? "You will be teleported to them" : "You have been teleported to them").color(NamedTextColor.GOLD))
                        .append(Component.text(time > 0 ? (" in " + time + " seconds") : "")).color(NamedTextColor.RED)
                        .append(Component.text(time > 0 ? ", please do not move." : ".").color(NamedTextColor.GOLD)));
        handler.scheduleTeleport(teleporting, who, time);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> tpa = Commands.literal("tpa").then(Commands.argument("player", ArgumentTypes.player()).executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.tpa.enabled", true)) {
                sender.sendPlainMessage("/tpa is disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                if (tpas.containsKey(player)) {
                    sender.sendMessage(Component.text("You already have an outgoing TPA request.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                if (handler.teleportations.containsKey(player)) {
                    sender.sendMessage(Component.text("You are already teleporting somewhere.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                final PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
                final Player plr = resolver.resolve(ctx.getSource()).getFirst();
                sender.sendMessage(
                        Component.text("TPA request sent to ").color(NamedTextColor.GOLD)
                                .append(plr.displayName().color(NamedTextColor.RED))
                                .append(Component.text(". They have 60 seconds to accept it. You can cancel this by running ").color(NamedTextColor.GOLD))
                                .append(Component.text("/tpcancel").clickEvent(ClickEvent.runCommand("tpcancel")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED))
                                .append(Component.text(".").color(NamedTextColor.GOLD)));
                plr.sendMessage(player.displayName().color(NamedTextColor.RED)
                        .append(Component.text(" requested to teleport to you. Run ").color(NamedTextColor.GOLD))
                        .append(Component.text("/tpaccept").clickEvent(ClickEvent.runCommand("tpaccept")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED))
                        .append(Component.text(" to accept it, or ").color(NamedTextColor.GOLD))
                        .append(Component.text("/tpdecline").clickEvent(ClickEvent.runCommand("tpdecline")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED))
                        .append(Component.text(" to decline. This request expires in 60 seconds.").color(NamedTextColor.GOLD)));

                Pair<Listener, Integer> ref = new Pair<>(null, null);
                int task = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
                    sender.sendMessage(
                            Component.text("Your TPA request to ").color(NamedTextColor.GOLD)
                                    .append(plr.displayName().color(NamedTextColor.RED))
                                    .append(Component.text(" has expired.").color(NamedTextColor.GOLD)));
                    plr.sendMessage(player.displayName().color(NamedTextColor.RED)
                            .append(Component.text("'s TPA request to you has expired.").color(NamedTextColor.GOLD)));
                    tpas.remove(player);
                    handler.teleportations.remove(player);
                    HandlerList.unregisterAll(ref.first);
                }, 1200);
                ref.second = task;
                tpas.put(player, new Pair<>(plr, ref));
                ref.first = new Listener() {
                    void cancel() {
                        handler.teleportations.remove(player);
                        tpas.remove(player);
                        HandlerList.unregisterAll(ref.first);
                        Bukkit.getScheduler().cancelTask(task);
                        plr.sendMessage(Component.text("Teleport request cancelled because ").color(NamedTextColor.GOLD)
                                .append(player.displayName().color(NamedTextColor.RED))
                                .append(Component.text(" disconnected.").color(NamedTextColor.GOLD)));
                    }
                    @EventHandler
                    public void onPlayerLeave(PlayerQuitEvent event) {
                        if (event.getPlayer().equals(player)) {
                            cancel();
                        }
                    }
                };
                Bukkit.getPluginManager().registerEvents(ref.first, KamsTweaks.getInstance());
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /tpa.");
            return Command.SINGLE_SUCCESS;
        }));
        commands.registrar().register(tpa.build());
        LiteralArgumentBuilder<CommandSourceStack> tpaccept = Commands.literal("tpaccept").executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.tpa.enabled", true)) {
                sender.sendPlainMessage("/tpa is disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                Player who = null;
                for (Player p : tpas.sequencedKeySet()) {
                    var pair = tpas.get(p);
                    if (pair.first.equals(player)) {
                        who = p;
                        break;
                    }
                }
                if (who == null) {
                    sender.sendMessage(Component.text("You don't have any incoming TPA requests.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                accept(player, who);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /tpa.");
            return Command.SINGLE_SUCCESS;
        }).then(Commands.argument("player", ArgumentTypes.player()).executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.tpa.enabled", true)) {
                sender.sendPlainMessage("/tpa is disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                final PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
                final Player who = resolver.resolve(ctx.getSource()).getFirst();
                if (!tpas.containsKey(who) || tpas.get(who).first != player) {
                    sender.sendMessage(Component.text("You don't have any incoming TPA requests from ").color(NamedTextColor.RED).append(who.displayName().color(NamedTextColor.GOLD)).append(Component.text(".").color(NamedTextColor.RED)));
                    return Command.SINGLE_SUCCESS;
                }
                accept(player, who);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /tpa.");
            return Command.SINGLE_SUCCESS;
        }));
        commands.registrar().register(tpaccept.build());
    }
}
