package kam.kamsTweaks.features.tp;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.claims.Claims;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeleportFeatures extends Feature {
    public Map<Player, Location> locations = new HashMap<>();
    public Map<Player, Integer> teleportations = new HashMap<>();
    public Map<Player, Integer> onCooldown = new HashMap<>();

    List<Feature> features = new ArrayList<>();

    static TeleportFeatures instance;

    public TeleportFeatures() {
        // rtp is being removed due to its mass lag causing
        features.add(new Spawn());
        features.add(new Back());
        features.add(new Home());
        features.add(new Warp());
        features.add(new TPA());
    }

    @Override
    public void setup() {
        instance = this;
        for (var feature : features) {
            feature.setup();
        }
        for (var feature : features) {
            Bukkit.getServer().getPluginManager().registerEvents(feature, KamsTweaks.getInstance());
        }
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("teleportation.timer", "teleportation.timer", 5, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("teleportation.cooldown", "teleportation.cooldown", 10, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.homes.enabled", "teleportation.homes.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.spawn.enabled", "teleportation.spawn.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.tpa.enabled", "teleportation.tpa.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.warp.enabled", "teleportation.warp.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.back.enabled", "teleportation.back.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.rtp.enabled", "teleportation.rtp.enabled", true, "kamstweaks.configure"));
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

        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            var passengers = vehicle.getPassengers();
            for (var passenger : passengers) {
                if (Claims.get().entityClaims.containsKey(passenger.getUniqueId())) {
                    var claim = Claims.get().entityClaims.get(passenger.getUniqueId());
                    if (!claim.hasPermission(player, Claims.ClaimPermission.DAMAGE_ENTITY)) {
                        player.sendMessage(Component.text("Cancelled teleport because you don't have permission to ").append(Component.text("kill").color(NamedTextColor.AQUA).decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.callback(audience -> audience.sendMessage(Component.text("It requires the damage permission because teleporting the mob would allow a method of killing a claimed entity.")))), Component.text(" an entity in this vehicle.")));
                        return;
                    }
                }
            }
        }
        onCooldown.put(player, KamsTweaks.getInstance().getConfig().getInt("teleportation.cooldown"));
        locations.put(player, player.getLocation());
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
            var passengers = vehicle.getPassengers();
            for (var passenger : passengers) {
                passenger.leaveVehicle();
            }
            vehicle.teleport(location);
            if (vehicle instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 15, 100));
            for (var passenger : passengers) {
                passenger.teleport(location);
                vehicle.addPassenger(passenger);
                if (passenger instanceof LivingEntity le) le.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 15, 100));
            }
            Bukkit.getScheduler().runTaskLater(KamsTweaks.getInstance(), () -> vehicle.addPassenger(player), 1L);
        } else {
            player.teleport(location);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 15, 100));
        }

        r.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.getInstance(), r, 20, 20);
    }

    public void scheduleTeleport(Player player, Location location, double time) {
        var ref = new Object() {
            Listener listener;
        };

        int task = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
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
            @EventHandler
            public void onPlayerLeave(PlayerQuitEvent event) {
                if (event.getPlayer().equals(player)) {
                    cancel();
                }
            }
            @EventHandler
            public void onPlayerDeath(PlayerDeathEvent event) {
                if (event.getPlayer().equals(player)) {
                    player.sendMessage(Component.text("Teleport cancelled because you died.").color(NamedTextColor.GOLD));
                    cancel();
                }
            }
            @EventHandler
            public void onPlayerDamage(EntityDamageEvent event) {
                if (event.getEntity().equals(player)) {
                    player.sendMessage(Component.text("Teleport cancelled because you took damage.").color(NamedTextColor.GOLD));
                    cancel();
                }
            }
            @EventHandler
            public void onTeleport(PlayerTeleportEvent event) {
                if (event.getPlayer().equals(player)) {
                    player.sendMessage(Component.text("Teleport cancelled because you teleported.").color(NamedTextColor.GOLD));
                    cancel();
                }
            }
            @EventHandler
            public void onPlayerMove(PlayerMoveEvent event) {
                if (event.getPlayer().equals(player) && event.hasChangedBlock()) {
                    player.sendMessage(Component.text("Teleport cancelled because you moved.").color(NamedTextColor.GOLD));
                    cancel();
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(ref.listener, KamsTweaks.getInstance());
    }

    public void scheduleTeleport(Player player, Entity target, double time) {
        var ref = new Object() {
            Listener listener;
        };

        int task = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
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
            @EventHandler
            public void onPlayerLeave(PlayerQuitEvent event) {
                if (event.getPlayer().equals(player)) {
                    target.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(player.displayName().color(NamedTextColor.RED))
                            .append(Component.text(" left.").color(NamedTextColor.GOLD)));
                    cancel();
                } else if (event.getPlayer().equals(target)) {
                    player.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(event.getPlayer().displayName().color(NamedTextColor.RED))
                            .append(Component.text(" left.").color(NamedTextColor.GOLD)));
                    cancel();
                }
            }
            @EventHandler
            public void onPlayerDie(PlayerDeathEvent event) {
                if (event.getPlayer().equals(player)) {
                    target.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(player.displayName().color(NamedTextColor.RED))
                            .append(Component.text(" died.").color(NamedTextColor.GOLD)));
                    cancel();
                } else if (event.getPlayer().equals(target)) {
                    player.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(event.getPlayer().displayName().color(NamedTextColor.RED))
                            .append(Component.text(" died.").color(NamedTextColor.GOLD)));
                    cancel();
                }
            }
            @EventHandler
            public void onPlayerTeleport(PlayerTeleportEvent event) {
                if (event.getPlayer().equals(player)) {
                    target.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(player.displayName().color(NamedTextColor.RED))
                            .append(Component.text(" teleported.").color(NamedTextColor.GOLD)));
                    cancel();
                } else if (event.getPlayer().equals(target)) {
                    player.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(event.getPlayer().displayName().color(NamedTextColor.RED))
                            .append(Component.text(" teleported.").color(NamedTextColor.GOLD)));
                    cancel();
                }
            }
            @EventHandler
            public void onPlayerDamage(EntityDamageEvent event) {
                if (event.getEntity().equals(player)) {
                    target.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(player.displayName().color(NamedTextColor.RED))
                            .append(Component.text(" took damage.").color(NamedTextColor.GOLD)));
                    cancel();
                } else if (event.getEntity().equals(target)) {
                    player.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(event.getEntity().name().color(NamedTextColor.RED))
                            .append(Component.text(" took damage.").color(NamedTextColor.GOLD)));
                    cancel();
                }
            }
            @EventHandler
            public void onPlayerMove(PlayerMoveEvent event) {
                if (event.getPlayer().equals(player) && event.hasChangedBlock()) {
                    target.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(player.displayName().color(NamedTextColor.RED))
                            .append(Component.text(" moved.").color(NamedTextColor.GOLD)));
                    player.sendMessage(Component.text("Teleport cancelled because you moved.").color(NamedTextColor.GOLD));
                    cancel();
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(ref.listener, KamsTweaks.getInstance());
    }

    public static TeleportFeatures get() {
        return instance;
    }
}
