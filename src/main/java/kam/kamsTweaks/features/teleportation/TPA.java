package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TPA {
    TeleportationHandler handler;

    LinkedHashMap<Player, Pair<Player, Pair<Listener, Integer>>> tpas = new LinkedHashMap<>();

    public void init(TeleportationHandler handler) {
        this.handler = handler;
    }

    void accept(Player acceptor, Player requester) {
        if (!tpas.containsKey(requester)) return;
        Bukkit.getScheduler().cancelTask(tpas.get(requester).second.second);
        tpas.remove(requester);
        HandlerList.unregisterAll(tpas.get(requester).second.first);
        double time = KamsTweaks.getInstance().getConfig().getDouble("teleportation.timer");
        requester.sendMessage(
                Component.text("You have accepted the TPA request from ").color(NamedTextColor.GOLD)
                        .append(acceptor.displayName().color(NamedTextColor.RED))
                        .append(Component.text(". " + (time > 0 ? "They will be teleported to you in " : "They have been teleported to you.")).color(NamedTextColor.GOLD))
                        .append(Component.text(time > 0 ? (time + " seconds") : "").color(NamedTextColor.RED))
                        .append(Component.text(".").color(NamedTextColor.GOLD)));
        acceptor.sendMessage(
                requester.displayName().color(NamedTextColor.RED)
                        .append(Component.text(" has accepted your TPA request. ").color(NamedTextColor.GOLD))
                        .append(Component.text(time > 0 ? "You will be teleported to them in " : "You have been teleported to them").color(NamedTextColor.GOLD))
                        .append(Component.text(time > 0 ? (time + " seconds") : "")).color(NamedTextColor.RED)
                        .append(Component.text(time > 0 ? ", please do not move." : ".").color(NamedTextColor.GOLD)));
        handler.scheduleTeleport(requester, acceptor, time);
    }

    void decline(Player decliner, Player requester) {
        if (!tpas.containsKey(requester)) return;
        Bukkit.getScheduler().cancelTask(tpas.get(requester).second.second);
        tpas.remove(requester);
        HandlerList.unregisterAll(tpas.get(requester).second.first);
        decliner.sendMessage(
                Component.text("You have declined the TPA request from ").color(NamedTextColor.GOLD)
                        .append(decliner.displayName().color(NamedTextColor.RED))
                        .append(Component.text(". ").color(NamedTextColor.GOLD)));
        requester.sendMessage(
                requester.displayName().color(NamedTextColor.RED)
                        .append(Component.text(" has denied your TPA request. ").color(NamedTextColor.GOLD)));
        handler.teleportations.remove(requester);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> tpa = Commands.literal("tpa")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .then(Commands.argument("player", ArgumentTypes.player()).executes(ctx -> {
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
                if (plr.equals(player)) {
                    sender.sendMessage(Component.text("You can't teleport to yourself, silly!").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
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
                    void cancel(Player disconner) {
                        if (disconner == player) {
                            plr.sendMessage(Component.text("Teleport request cancelled because ").color(NamedTextColor.GOLD)
                                    .append(player.displayName().color(NamedTextColor.RED))
                                    .append(Component.text(" disconnected.").color(NamedTextColor.GOLD)));
                        } else if (disconner == plr) {
                            player.sendMessage(Component.text("Teleport request cancelled because ").color(NamedTextColor.GOLD)
                                    .append(plr.displayName().color(NamedTextColor.RED))
                                    .append(Component.text(" disconnected.").color(NamedTextColor.GOLD)));
                        }
                        handler.teleportations.remove(player);
                        tpas.remove(player);
                        HandlerList.unregisterAll(ref.first);
                        Bukkit.getScheduler().cancelTask(task);
                    }
                    @EventHandler
                    public void onPlayerLeave(PlayerQuitEvent event) {
                        if (event.getPlayer().equals(player)) {
                            cancel(player);
                        } else if (event.getPlayer().equals(plr)) {
                            cancel(plr);
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
        LiteralArgumentBuilder<CommandSourceStack> tpaccept = Commands.literal("tpaccept")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .executes(ctx -> {
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

        LiteralArgumentBuilder<CommandSourceStack> tpdecline = Commands.literal("tpdecline")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .executes(ctx -> {
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
                decline(player, who);
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
                decline(player, who);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /tpa.");
            return Command.SINGLE_SUCCESS;
        }));
        commands.registrar().register(tpdecline.build());
        LiteralArgumentBuilder<CommandSourceStack> tpcancel = Commands.literal("tpcancel")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.tpa"))
                .executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.tpa.enabled", true)) {
                sender.sendPlainMessage("/tpa is disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                if (!tpas.containsKey(player)) {
                    sender.sendMessage(Component.text("You don't have any outgoing TPA requests.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                var info = tpas.get(player);
                Bukkit.getScheduler().cancelTask(info.second.second);
                tpas.remove(player);
                HandlerList.unregisterAll(info.second.first);
                player.sendMessage(
                        Component.text("You have cancelled the TPA request to ").color(NamedTextColor.GOLD)
                                .append(info.first.displayName().color(NamedTextColor.RED))
                                .append(Component.text(". ").color(NamedTextColor.GOLD)));
                info.first.sendMessage(
                        player.displayName().color(NamedTextColor.RED)
                                .append(Component.text(" has cancelled their TPA request to you. ").color(NamedTextColor.GOLD)));
                handler.teleportations.remove(player);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /tpa.");
            return Command.SINGLE_SUCCESS;
        });
        commands.registrar().register(tpcancel.build());
    }
}
