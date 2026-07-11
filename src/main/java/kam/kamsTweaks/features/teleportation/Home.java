package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.features.gameplay.PVP;
import kam.kamsTweaks.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Home extends Feature {
    Map<UUID, Location> homes = new HashMap<>();
    Map<UUID, Integer> onSetHomeCooldown = new HashMap<>();

    void cooldown(Player player) {
        onSetHomeCooldown.put(player.getUniqueId(), Config.getInt("teleportation.homes.sethome-cooldown", 900));
        var r = new Runnable() {
            int id = 0;

            @Override
            public void run() {
                int val = onSetHomeCooldown.get(player.getUniqueId()) - 1;
                if (val <= 0) {
                    Bukkit.getScheduler().cancelTask(id);
                    onSetHomeCooldown.remove(player.getUniqueId());
                    return;
                }
                onSetHomeCooldown.put(player.getUniqueId(), val);
            }
        };

        r.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), r, 20, 20);
    }

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        config.set("homes", null);

        homes.forEach((uuid, loc) -> {
            if (loc != null) config.set("homes." + uuid, LocationUtils.serializeLocation(loc));
        });
    }

    @Override
    public void loadData() {
        homes.clear();
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        if (config.contains("homes")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("homes")).getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(key);
                    String locStr = config.getString("homes." + key);
                    assert locStr != null;
                    Location loc = LocationUtils.deserializeLocation(locStr);
                    if (loc.getWorld() == null) continue;
                    homes.put(owner, loc);
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> home = Commands.literal("home")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.homes"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.homes.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.HOMES)));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    var handler = TeleportFeatures.get();
                    if (executor instanceof Player player) {
                        if (PVP.instance.inCombat.containsKey(player.getUniqueId())) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (handler.teleportations.containsKey(player)) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.TP_ALREADY_TELEPORTING).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (handler.onCooldown.containsKey(player)) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.TP_COOLDOWN, Component.text(handler.onCooldown.get(player))).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (!homes.containsKey(player.getUniqueId())) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.HOMES_NONE).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        int time = KamsTweaks.get().getConfig().getInt("teleportation.timer");
                        sender.sendMessage(KTStrings.getFor(KTStrings.TP_TO_HOME, Component.text(time).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
                        Location loc = homes.get(player.getUniqueId());
                        handler.scheduleTeleport(player, loc, time);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, KTStrings.getFor(KTStrings.HOMES)));
                    return Command.SINGLE_SUCCESS;
                });
        commands.registrar().register(home.build());

        LiteralArgumentBuilder<CommandSourceStack> sethome = Commands.literal("sethome")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.homes"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.homes.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.HOMES)));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        if (onSetHomeCooldown.containsKey(player.getUniqueId())) {
                            var timeLeft = onSetHomeCooldown.get(player.getUniqueId());
                            if (timeLeft >= 60) {
                                int min = timeLeft / 60;
                                int sec = timeLeft - min * 60;
                                player.sendMessage(KTStrings.getFor(KTStrings.COOLDOWN_MS, Component.text("/sethome"), Component.text(min), Component.text(sec)).color(NamedTextColor.RED));
                            } else {
                                player.sendMessage(KTStrings.getFor(KTStrings.COOLDOWN, Component.text("/sethome"), Component.text(timeLeft)).color(NamedTextColor.RED));
                            }
                            return Command.SINGLE_SUCCESS;
                        }
                        homes.put(player.getUniqueId(), player.getLocation());
                        sender.sendMessage(KTStrings.getFor(KTStrings.HOMES_SET).color(NamedTextColor.GOLD));
                        cooldown(player);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, KTStrings.getFor(KTStrings.HOMES)));
                    return Command.SINGLE_SUCCESS;
                });
        commands.registrar().register(sethome.build());
    }
}