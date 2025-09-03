package kam.kamsTweaks.features.teleportation;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeleportationHandler {
    public Map<Player, Location> locations = new HashMap<>();
    public Map<Player, Integer> teleportations = new HashMap<>();
    public List<Player> onCooldown = new ArrayList<>();

    SetHome setHome = new SetHome();
    TPA tpa = new TPA();
    Warp warp = new Warp();
    Back back = new Back();
    Spawn spawn = new Spawn();
    RTP rtp = new RTP();

    public void teleport(Player player, Location location) {
        onCooldown.add(player);
        locations.put(player, player.getLocation());
        player.teleport(location);
        teleportations.remove(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
            onCooldown.remove(player);
        }, 20 * KamsTweaks.getInstance().getConfig().getLong("teleportation.cooldown"));
    }

    public void scheduleTeleport(Player player, Location location, double time) {
        var ref = new Object() {
            Listener listener;
        };

        int task = Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
            teleport(player, location);
            HandlerList.unregisterAll(ref.listener);
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
            teleport(player, target.getLocation());
            HandlerList.unregisterAll(ref.listener);
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
            public void onPlayerMove(PlayerMoveEvent event) {
                if (event.getPlayer().equals(player) && event.hasChangedBlock()) {
                    target.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(player.displayName().color(NamedTextColor.RED))
                            .append(Component.text(" moved.").color(NamedTextColor.GOLD)));
                    player.sendMessage(Component.text("Teleport cancelled because you moved.").color(NamedTextColor.GOLD));
                    cancel();
                }
            }

            @EventHandler
            public void onPlayerDamage(EntityDamageEvent event) {
                if (event instanceof Player player_ && player_.equals(player)) {
                    target.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                            .append(player.displayName().color(NamedTextColor.RED))
                            .append(Component.text(" took damage.").color(NamedTextColor.GOLD)));
                    player.sendMessage(Component.text("Teleport cancelled because you took damage.").color(NamedTextColor.GOLD));
                    cancel();
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(ref.listener, KamsTweaks.getInstance());
    }

    public void setup() {
        setHome.init(this);
        tpa.init(this);
        warp.init(this);
        back.init(this);
        spawn.init(this);
        rtp.init(this);
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("teleportation.timer", "teleportation.timer", 5, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("teleportation.cooldown", "teleportation.cooldown", 30, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.homes.enabled", "teleportation.homes.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.spawn.enabled", "teleportation.spawn.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.tpa.enabled", "teleportation.tpa.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.warp.enabled", "teleportation.warp.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.back.enabled", "teleportation.back.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("teleportation.rtp.enabled", "teleportation.rtp.enabled", true, "kamstweaks.configure"));
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        setHome.registerCommands(commands);
        tpa.registerCommands(commands);
        warp.registerCommands(commands);
        back.registerCommands(commands);
        spawn.registerCommands(commands);
        rtp.registerCommands(commands);
    }

    public void save() {
        setHome.saveHomes();
        warp.saveWarps();
    }

    public void load() {
        setHome.loadHomes();
        warp.loadWarps();
    }
}
