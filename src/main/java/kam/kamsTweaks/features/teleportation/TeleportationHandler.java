package kam.kamsTweaks.features.teleportation;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TeleportationHandler {
    public Map<Player, Location> locations = new HashMap<>();
    public Map<Player, Integer> teleportations = new HashMap<>();

    SetHome setHome = new SetHome();
    TPA tpa = new TPA();
    Warp warp = new Warp();
    Back back = new Back();
    Spawn spawn = new Spawn();

    public void teleport(Player player, Location location) {
        locations.put(player, player.getLocation());
        player.teleport(location);
        teleportations.remove(player);
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
                target.sendMessage(Component.text("Teleport cancelled because ").color(NamedTextColor.GOLD)
                        .append(player.displayName().color(NamedTextColor.RED))
                        .append(Component.text(" moved.").color(NamedTextColor.GOLD)));
                player.sendMessage(Component.text("Teleport cancelled because you moved.").color(NamedTextColor.GOLD));
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
            public void onPlayerMove(PlayerMoveEvent event) {
                if (event.getPlayer().equals(player) && event.hasChangedBlock()) {
                    cancel();
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(ref.listener, KamsTweaks.getInstance());
    }

    public void init() {
        setHome.init(this);
        tpa.init(this);
        warp.init(this);
        back.init(this);
        spawn.init(this);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        setHome.registerCommands(commands);
        tpa.registerCommands(commands);
        warp.registerCommands(commands);
        back.registerCommands(commands);
        spawn.registerCommands(commands);
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
