package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.managers.KTPerms;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.features.fun.nicknames.Names;
import kam.kamsTweaks.features.gameplay.PVP;
import kam.kamsTweaks.utils.UserDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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
    private final Map<UUID, Boolean> tpAuto = new HashMap<>();
    private final Map<UUID, List<UUID>> tpBlock = new HashMap<>();

    private void sendRequest(Player sender, Player target, boolean here) {
        if (tpas.containsKey(sender)) {
            sender.sendMessage(KTStrings.getFor(KTStrings.TPA_ALREADY_OUTGOING).color(NamedTextColor.RED));
            return;
        }
        var handler = TeleportFeatures.get();
        if (handler.teleportations.containsKey(sender)) {
            sender.sendMessage(KTStrings.getFor(KTStrings.TP_ALREADY_TELEPORTING).color(NamedTextColor.RED));
            return;
        }
        if (handler.onCooldown.containsKey(sender)) {
            sender.sendMessage(KTStrings.getFor(KTStrings.TP_COOLDOWN, Component.text(handler.onCooldown.get(sender))).color(NamedTextColor.RED));
            return;
        }
        if (target.equals(sender)) {
            sender.sendMessage(KTStrings.getFor(KTStrings.TPA_SELF).color(NamedTextColor.RED));
            return;
        }
        if (target.isDead()) {
            sender.sendMessage(KTStrings.getFor(KTStrings.TPA_OTHER_DEAD).color(NamedTextColor.RED));
            return;
        }
        if (sender.isDead()) {
            sender.sendMessage(KTStrings.getFor(KTStrings.TPA_DEAD).color(NamedTextColor.RED));
            return;
        }
        if (!KTPerms.hasPermission(sender, here ? KTPerms.TP_TPA : KTPerms.TP_TPA_HERE)) {
            sender.sendMessage(KTStrings.getFor(KTStrings.OTHER_NO_PERMS).color(NamedTextColor.RED));
            return;
        }

        if (tpBlock.containsKey(target.getUniqueId()) && tpBlock.get(target.getUniqueId()).contains(sender.getUniqueId())) {
            sender.sendMessage(KTStrings.getFor(KTStrings.TPA_BLOCKED, target.displayName()).color(NamedTextColor.RED));
            return;
        }

        sender.sendMessage(KTStrings.getFor(KTStrings.TPA_SENT,
                KTStrings.getFor(here ? KTStrings.TPA_HERE : KTStrings.TPA),
                target.displayName(),
                Component.text("/tpcancel").clickEvent(ClickEvent.runCommand("tpcancel")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED)).color(NamedTextColor.GOLD));
        target.sendMessage(KTStrings.getFor(KTStrings.TPA_RECIEVED,
                sender.displayName(),
                KTStrings.getFor(here ? KTStrings.TPA_HERE : KTStrings.TPA),
                Component.text("/tpaccept").clickEvent(ClickEvent.runCommand("tpaccept")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED),
                Component.text("/tpdecline").clickEvent(ClickEvent.runCommand("tpdecline")).color(NamedTextColor.RED).decorate(TextDecoration.UNDERLINED)).color(NamedTextColor.GOLD));

        Listener listener = new Listener() {
            @EventHandler
            public void onPlayerLeave(PlayerQuitEvent event) {
                var left = event.getPlayer().equals(sender) ? sender : event.getPlayer().equals(target) ? target : null;
                if (left == target) {
                    sender.sendMessage(KTStrings.getFor(KTStrings.TPA_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_LEFT, left.displayName())).color(NamedTextColor.GOLD));
                } else if (left == sender) {
                    target.sendMessage(KTStrings.getFor(KTStrings.TPA_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_LEFT, left.displayName())).color(NamedTextColor.GOLD));
                } else return;
                cancel(sender, target);
            }

            @EventHandler
            public void onPlayerDie(PlayerDeathEvent event) {
                var died = event.getPlayer().equals(sender) ? sender : event.getPlayer().equals(target) ? target : null;
                if (died == target) {
                    sender.sendMessage(KTStrings.getFor(KTStrings.TPA_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_DEATH, died.displayName())).color(NamedTextColor.GOLD));
                    target.sendMessage(KTStrings.getFor(KTStrings.TPA_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_DEATH)).color(NamedTextColor.GOLD));
                } else if (died == sender) {
                    target.sendMessage(KTStrings.getFor(KTStrings.TPA_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_DEATH, died.displayName())).color(NamedTextColor.GOLD));
                    sender.sendMessage(KTStrings.getFor(KTStrings.TPA_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_DEATH)).color(NamedTextColor.GOLD));
                } else return;
                cancel(sender, target);
            }
        };
        Bukkit.getPluginManager().registerEvents(listener, KamsTweaks.get());

        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
            sender.sendMessage(KTStrings.getFor(KTStrings.TPA_TO_EXPIRED, target.displayName()).color(NamedTextColor.GOLD));
            target.sendMessage(KTStrings.getFor(KTStrings.TPA_FROM_EXPIRED, sender.displayName()).color(NamedTextColor.GOLD));
            tpas.remove(sender);
            handler.teleportations.remove(sender);
            HandlerList.unregisterAll(listener);
        }, 1200);

        tpas.put(sender, new TPARequest(target, here, listener, taskId));

        if (!here && tpAuto.containsKey(target.getUniqueId()) && tpAuto.get(target.getUniqueId())) {
            target.sendMessage(KTStrings.getFor(KTStrings.TPA_AUTOACCEPTED).color(NamedTextColor.GOLD));
            accept(target, sender);
        }
    }

    private void accept(Player acceptor, Player requester) {
        var handler = TeleportFeatures.get();
        TPARequest req = tpas.get(requester);
        if (req == null) return;
        if (req.here) {
            if (handler.onCooldown.containsKey(acceptor)) {
                acceptor.sendMessage(KTStrings.getFor(KTStrings.TP_COOLDOWN, Component.text(handler.onCooldown.get(acceptor))).color(NamedTextColor.RED));
                return;
            }
        } else {
            if (handler.onCooldown.containsKey(requester)) {
                acceptor.sendMessage(KTStrings.getFor(KTStrings.TP_OTHER_COOLDOWN, requester.displayName(), Component.text(handler.onCooldown.get(requester))).color(NamedTextColor.RED));
                return;
            }
        }

        Bukkit.getScheduler().cancelTask(req.taskId);
        HandlerList.unregisterAll(req.listener);
        tpas.remove(requester);

        double time = KamsTweaks.get().getConfig().getDouble("teleportation.timer");

        if (req.here) {
            handler.scheduleTeleport(acceptor, requester, time);
        } else {
            handler.scheduleTeleport(requester, acceptor, time);
        }

        acceptor.sendMessage(KTStrings.getFor(KTStrings.TPA_ACCEPTED, requester.displayName(), KTStrings.getFor(req.here ? KTStrings.TPA_WILL : KTStrings.TPA_OTHER_WILL, Component.text(time))).color(NamedTextColor.GOLD));
        requester.sendMessage(KTStrings.getFor(KTStrings.TPA_OTHER_ACCEPTED, acceptor.displayName(), KTStrings.getFor(req.here ? KTStrings.TPA_OTHER_WILL : KTStrings.TPA_WILL, Component.text(time))).color(NamedTextColor.GOLD));
    }

    private void decline(Player decliner, Player requester) {
        TPARequest req = tpas.get(requester);
        if (req == null) return;

        Bukkit.getScheduler().cancelTask(req.taskId);
        HandlerList.unregisterAll(req.listener);
        tpas.remove(requester);

        decliner.sendMessage(KTStrings.getFor(KTStrings.TPA_DECLINED, requester.displayName()).color(NamedTextColor.GOLD));
        requester.sendMessage(KTStrings.getFor(KTStrings.TPA_OTHER_DECLINED, decliner.displayName()).color(NamedTextColor.GOLD));

        TeleportFeatures.get().teleportations.remove(requester);
    }

    private void cancel(Player sender, Player target) {
        TPARequest req = tpas.remove(sender);
        if (req == null) return;
        Bukkit.getScheduler().cancelTask(req.taskId);
        HandlerList.unregisterAll(req.listener);
        TeleportFeatures.get().teleportations.remove(sender);
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("tpa")
                .requires(source -> KTPerms.hasPermission(source, KTPerms.TP_TPA))
                .then(Commands.argument("player", ArgumentTypes.player()).executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        if (PVP.instance.inCombat.containsKey(p.getUniqueId())) {
                            p.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
                        if (UserDataManager.get(target.getUniqueId(), "vanished", false)) {
                            ctx.getSource().getSender().sendMessage(Component.translatable("argument.entity.notfound.player").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        sendRequest(p, target, false);
                    }
                    return Command.SINGLE_SUCCESS;
                })).build());
        commands.registrar().register(Commands.literal("tpahere")
                .requires(source -> KTPerms.hasPermission(source, KTPerms.TP_TPA_HERE))
                .then(Commands.argument("player", ArgumentTypes.player()).executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        if (PVP.instance.inCombat.containsKey(p.getUniqueId())) {
                            p.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
                        if (UserDataManager.get(target.getUniqueId(), "vanished", false)) {
                            ctx.getSource().getSender().sendMessage(Component.translatable("argument.entity.notfound.player").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        sendRequest(p, target, true);
                    }
                    return Command.SINGLE_SUCCESS;
                })).build());
        commands.registrar().register(Commands.literal("tpaccept")
                .executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        if (PVP.instance.inCombat.containsKey(p.getUniqueId())) {
                            p.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        Player requester = tpas.entrySet().stream()
                                .filter(e -> e.getValue().target.equals(p))
                                .map(Map.Entry::getKey).findFirst().orElse(null);
                        if (requester != null) accept(p, requester);
                        else p.sendMessage(KTStrings.getFor(KTStrings.TPA_NO_PENDING).color(NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                }).build());
        commands.registrar().register(Commands.literal("tpdecline")
                .executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        if (PVP.instance.inCombat.containsKey(p.getUniqueId())) {
                            p.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        Player requester = tpas.entrySet().stream()
                                .filter(e -> e.getValue().target.equals(p))
                                .map(Map.Entry::getKey).findFirst().orElse(null);
                        if (requester != null) decline(p, requester);
                        else p.sendMessage(KTStrings.getFor(KTStrings.TPA_NO_PENDING).color(NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                }).build());
        commands.registrar().register(Commands.literal("tpcancel")
                .executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        TPARequest req = tpas.get(p);
                        if (req != null) {
                            cancel(p, req.target);
                            req.target.sendMessage(KTStrings.getFor(KTStrings.TPA_OTHER_CANCELLED, p.displayName()).color(NamedTextColor.GOLD));
                            p.sendMessage(KTStrings.getFor(KTStrings.TPA_CANCELLED, req.target.displayName()).color(NamedTextColor.GOLD));
                        } else {
                            p.sendMessage(KTStrings.getFor(KTStrings.TPA_NO_PENDING, p.displayName()).color(NamedTextColor.GOLD));
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }).build());
        commands.registrar().register(Commands.literal("tpauto")
                .executes(ctx -> {
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        UUID playerUUID = p.getUniqueId();
                        boolean newValue = true;
                        if (tpAuto.containsKey(playerUUID)) newValue = !tpAuto.get(playerUUID);

                        tpAuto.put(p.getUniqueId(), newValue);
                        if (newValue) {
                            p.sendMessage(KTStrings.getFor(KTStrings.TPA_AUTO_ENABLED).color(NamedTextColor.GOLD));
                        } else {
                            p.sendMessage(KTStrings.getFor(KTStrings.TPA_AUTO_DISABLED).color(NamedTextColor.GOLD));
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }).build());
        commands.registrar().register(Commands.literal("tpblock")
                .then(Commands.argument("player", ArgumentTypes.player())
                        .executes(ctx -> {
                            Entity exec = ctx.getSource().getExecutor();
                            if (exec instanceof Player p) {
                                UUID playerUUID = p.getUniqueId();
                                List<UUID> list;
                                if (!tpBlock.containsKey(playerUUID)) {
                                    list = new ArrayList<>();
                                    tpBlock.put(playerUUID, list);
                                } else {
                                    list = tpBlock.get(playerUUID);
                                }

                                Player target = ctx.getArgument("player", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
                                if (UserDataManager.get(target.getUniqueId(), "vanished", false)) {
                                    ctx.getSource().getSender().sendMessage(Component.translatable("argument.entity.notfound.player").color(NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                if (list.contains(target.getUniqueId())) {
                                    list.remove(target.getUniqueId());
                                    p.sendMessage(KTStrings.getFor(KTStrings.TPA_REMOVE_BLOCK, Names.getName(target).color(NamedTextColor.RED)));
                                } else {
                                    list.add(target.getUniqueId());
                                    p.sendMessage(KTStrings.getFor(KTStrings.TPA_ADD_BLOCK, Names.getName(target).color(NamedTextColor.RED)));
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })).build());
    }

    @Override
    public void loadData() {
        tpAuto.clear();
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        if (config.contains("tpa-settings.tp-auto")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("tpa-settings.tp-auto")).getKeys(false)) {
                try {
                    UUID player = UUID.fromString(key);
                    Boolean playerTPAuto = config.getBoolean("tpa-settings.tp-auto." + key);
                    tpAuto.put(player, playerTPAuto);
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }

        tpBlock.clear();
        if (config.contains("tpa-settings.tp-block")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("tpa-settings.tp-block")).getKeys(false)) {
                try {
                    UUID player = UUID.fromString(key);
                    List<UUID> playerTPBlock = new ArrayList<>();
                    for (var str : config.getStringList("tpa-settings.tp-block." + key)) {
                        playerTPBlock.add(UUID.fromString(str));
                    }
                    tpBlock.put(player, playerTPBlock);
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
    }

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        config.set("tpa-settings", null);
        tpAuto.forEach((uuid, tpauto) -> {
            if (tpauto != null) config.set("tpa-settings.tp-auto." + uuid, tpauto);
        });
        tpBlock.forEach((uuid, tpblock) -> {
            List<String> list = new ArrayList<>();
            if (tpblock != null) {
                for (var u : tpblock) {
                    list.add(u.toString());
                }
                config.set("tpa-settings.tp-block." + uuid, list);
            }
        });
    }
}
