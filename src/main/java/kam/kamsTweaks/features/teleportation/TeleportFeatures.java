package kam.kamsTweaks.features.teleportation;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.utils.ConfigCommand;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.claims.Claims;
import kam.kamsTweaks.features.claims.EntityClaims;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TeleportFeatures extends Feature {
    public Map<UUID, Location> locations = new HashMap<>();
    public Map<Player, Integer> teleportations = new HashMap<>();
    public Map<Player, Integer> onCooldown = new HashMap<>();

    List<Feature> features = new ArrayList<>();

    static TeleportFeatures instance;

    public TeleportFeatures() {
        instance = this;
        features.add(new Spawn());
        features.add(new Back());
        features.add(new Home());
        features.add(new Warp());
        features.add(new TPA());
    }

    @Override
    public void setup() {
        for (var feature : features) {
            feature.setup();
        }
        for (var feature : features) {
            Bukkit.getServer().getPluginManager().registerEvents(feature, KamsTweaks.get());
        }
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("teleportation.timer", "teleportation.timer", 5, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("teleportation.cooldown", "teleportation.cooldown", 10, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.homes.enabled", "teleportation.homes.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.spawn.enabled", "teleportation.spawn.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.tpa.enabled", "teleportation.tpa.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.warp.enabled", "teleportation.warp.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.back.enabled", "teleportation.back.enabled", true, "kamstweaks.configure"));
    }

    @Override
    public void shutdown() {
        for (var feature : features) {
            feature.shutdown();
        }
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        for (var feature : features) {
            feature.registerCommands(commands);
        }
    }

    @Override
    public void loadData() {
        for (var feature : features) {
            feature.loadData();
        }
    }

    @Override
    public void saveData() {
        for (var feature : features) {
            feature.saveData();
        }
    }

    public void teleport(Player player, Location location) {
        teleportations.remove(player);
        var o = new Object() {
            int task;
            Listener listener;
        };
        o.listener = new Listener() {
            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            void onAttack(EntityDamageByEntityEvent e) {
                if (e.getDamager() == player) {
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_IMMUNITY_LOST).color(NamedTextColor.YELLOW));
                    player.removePotionEffect(PotionEffectType.RESISTANCE);
                    HandlerList.unregisterAll(o.listener);
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(o.listener, KamsTweaks.get());
        o.task = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> HandlerList.unregisterAll(o.listener), 15 * 20);

        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            var passengers = vehicle.getPassengers();
            for (var passenger : passengers) {
                var claim = Claims.get().entityClaims.getClaim(passenger);
                if (claim != null) {
                    if (!claim.hasPermission(player, EntityClaims.EntityPermission.DAMAGE)) {
                        player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_PASSENGER)).color(NamedTextColor.GOLD).decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.callback(audience -> audience.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL_PASSENGER_INFO)))));
                        return;
                    }
                }
            }
        }
        onCooldown.put(player, KamsTweaks.get().getConfig().getInt("teleportation.cooldown"));
        locations.put(player.getUniqueId(), player.getLocation());
        var r = new Runnable() {
            int id = 0;

            @Override
            public void run() {
                int val = onCooldown.get(player) - 1;
                if (val <= 0) {
                    Bukkit.getScheduler().cancelTask(id);
                    onCooldown.remove(player);
                    return;
                }
                onCooldown.put(player, val);
            }
        };
        if (vehicle != null) {
            if (vehicle instanceof Player p && p.isSleeping()) p.wakeup(false);
            vehicle.teleport(location);
            if (vehicle instanceof LivingEntity le) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 10 * 15, 100));
            }
            for (var passenger : vehicle.getPassengers()) {
                if (passenger instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 10 * 15, 100));
            }
            Bukkit.getScheduler().runTaskLater(KamsTweaks.get(), () -> vehicle.addPassenger(player), 1L);
        } else {
            if (player.isSleeping()) player.wakeup(false);
            player.teleport(location);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 10 * 15, 100));
        }

        r.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), r, 20, 20);
    }

    public void scheduleTeleport(Player player, Location location, double time) {
        var ref = new Object() {
            Listener listener;
        };

        int task = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
            HandlerList.unregisterAll(ref.listener);
            teleport(player, location);
        }, (long) time * 20);
        teleportations.put(player, task);

        ref.listener = new Listener() {
            void cancel() {
                teleportations.remove(player);
                HandlerList.unregisterAll(ref.listener);
                Bukkit.getScheduler().cancelTask(task);
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerLeave(PlayerQuitEvent event) {
                if (event.getPlayer().equals(player)) {
                    cancel();
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerDeath(PlayerDeathEvent event) {
                if (event.getPlayer().equals(player)) {
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_DEATH)).color(NamedTextColor.GOLD));
                    cancel();
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerDamage(EntityDamageEvent event) {
                if (event.getEntity().equals(player)) {
                    if (player.isBlocking()) {
                        if (!(event.getDamageSource().getCausingEntity() instanceof Player)) return;
                    }
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_DAMAGE)).color(NamedTextColor.GOLD));
                    cancel();
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onTeleport(PlayerTeleportEvent event) {
                if (event.getPlayer().equals(player)) {
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_TP)).color(NamedTextColor.GOLD));
                    cancel();
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerMove(PlayerMoveEvent event) {
                if (event.getPlayer().equals(player) && event.hasChangedBlock()) {
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_MOVE)).color(NamedTextColor.GOLD));
                    cancel();
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(ref.listener, KamsTweaks.get());
    }

    public void scheduleTeleport(Player player, Entity target, double time) {
        var ref = new Object() {
            Listener listener;
        };

        int task = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
            HandlerList.unregisterAll(ref.listener);
            teleport(player, target.getLocation());
        }, (long) time * 20);
        teleportations.put(player, task);

        ref.listener = new Listener() {
            void cancel() {
                teleportations.remove(player);
                HandlerList.unregisterAll(ref.listener);
                Bukkit.getScheduler().cancelTask(task);
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerLeave(PlayerQuitEvent event) {
                if (event.getPlayer().equals(player)) {
                    target.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_LEFT, event.getPlayer().displayName())).color(NamedTextColor.GOLD));
                    cancel();
                } else if (event.getPlayer().equals(target)) {
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_LEFT, event.getPlayer().displayName())).color(NamedTextColor.GOLD));
                    cancel();
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerDie(PlayerDeathEvent event) {
                if (event.getPlayer().equals(player)) {
                    target.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_DEATH, event.getPlayer().displayName())).color(NamedTextColor.GOLD));
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_DEATH)).color(NamedTextColor.GOLD));
                    cancel();
                } else if (event.getPlayer().equals(target)) {
                    target.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_DEATH)).color(NamedTextColor.GOLD));
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_DEATH, event.getPlayer().displayName())).color(NamedTextColor.GOLD));
                    cancel();
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerTeleport(PlayerTeleportEvent event) {
                if (event.getPlayer().equals(player)) {
                    target.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_TP, event.getPlayer().displayName())).color(NamedTextColor.GOLD));
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_TP)).color(NamedTextColor.GOLD));
                    cancel();
                } else if (event.getPlayer().equals(target)) {
                    target.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_TP)).color(NamedTextColor.GOLD));
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_TP, event.getPlayer().displayName())).color(NamedTextColor.GOLD));
                    cancel();
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerDamage(EntityDamageEvent event) {
                if (event.getEntity().equals(player)) {
                    if (player.isBlocking()) {
                        if (!(event.getDamageSource().getCausingEntity() instanceof Player)) return;
                    }
                    target.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_DAMAGE, ((Player) event.getEntity()).displayName())).color(NamedTextColor.GOLD));
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_DAMAGE)).color(NamedTextColor.GOLD));
                    cancel();
                } else if (event.getEntity().equals(target)) {
                    if (target instanceof HumanEntity human && human.isBlocking()) {
                        if (!(event.getDamageSource().getCausingEntity() instanceof Player)) return;
                    }
                    target.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_DAMAGE)).color(NamedTextColor.GOLD));
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_DAMAGE, ((Player) event.getEntity()).displayName())).color(NamedTextColor.GOLD));
                    cancel();
                }
            }

            @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
            public void onPlayerMove(PlayerMoveEvent event) {
                if (event.getPlayer().equals(player) && event.hasChangedBlock()) {
                    target.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_OTHER_MOVE, event.getPlayer().displayName())).color(NamedTextColor.GOLD));
                    player.sendMessage(KTStrings.getFor(KTStrings.TP_CANCEL, KTStrings.getFor(KTStrings.TP_CANCEL_MOVE)).color(NamedTextColor.GOLD));
                    cancel();
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(ref.listener, KamsTweaks.get());
    }

    public static TeleportFeatures get() {
        return instance;
    }
}
