package kam.kamsTweaks.features.tp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    @Override
    public void setup() {}

    @Override
    public void shutdown() {}

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.getInstance().getDataConfig();
        config.set("homes", null);

        homes.forEach((uuid, loc) -> {
            if (loc != null) config.set("homes." + uuid, LocationUtils.serializeLocation(loc));
        });
    }

    @Override
    public void loadData() {
        homes.clear();
        FileConfiguration config = KamsTweaks.getInstance().getDataConfig();
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
                    Logger.excs.add(e);
                    Logger.warn(e.getMessage());
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
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.homes.enabled", true)) {
                        sender.sendPlainMessage("Homes are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    var handler = TeleportFeatures.get();
                    if (executor instanceof Player player) {
                        if (handler.teleportations.containsKey(player)) {
                            sender.sendMessage(Component.text("You are already teleporting somewhere.").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (handler.onCooldown.containsKey(player)) {
                            sender.sendMessage(Component.text("You're currently on teleportation cooldown for " + handler.onCooldown.get(player) + " seconds.").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (!homes.containsKey(player.getUniqueId())) {
                            sender.sendMessage(Component.text("You do not have a home.").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        int time = KamsTweaks.getInstance().getConfig().getInt("teleportation.timer");
                        sender.sendMessage(
                                Component.text("Teleporting to your home")
                                        .color(NamedTextColor.GOLD)
                                        .append(Component.text(time > 0 ? " in " : ".").color(NamedTextColor.GOLD))
                                        .append(Component.text(time > 0 ? (time + " seconds") : "").color(NamedTextColor.RED))
                                        .append(Component.text(time > 0 ? ", please do not move." : "").color(NamedTextColor.GOLD)));
                        Location loc = homes.get(player.getUniqueId());
                        handler.scheduleTeleport(player, loc, time);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage("Only players can use /home.");
                    return Command.SINGLE_SUCCESS;
                });
        commands.registrar().register(home.build());

        LiteralArgumentBuilder<CommandSourceStack> sethome = Commands.literal("sethome")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.homes"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.homes.enabled", true)) {
                        sender.sendPlainMessage("Homes are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        homes.put(player.getUniqueId(), player.getLocation());
                        sender.sendMessage(Component.text("Your home has been set.").color(NamedTextColor.GOLD));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage("Only players can use /sethome.");
                    return Command.SINGLE_SUCCESS;
                });
        commands.registrar().register(sethome.build());
    }
}