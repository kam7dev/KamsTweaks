package kam.kamsTweaks.features.tp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Warp extends Feature {
    Map<String, Location> warps = new HashMap<>();

    @Override
    public void setup() { }

    @Override
    public void shutdown() { }


    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.getInstance().getDataConfig();
        config.set("warps", null);
        warps.forEach((name, loc) -> {
            if (loc != null) config.set("warps." + name, LocationUtils.serializeLocation(loc));
        });
    }

    @Override
    public void loadData() {
        warps.clear();
        FileConfiguration config = KamsTweaks.getInstance().getDataConfig();
        if (config.contains("warps")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("warps")).getKeys(false)) {
                try {
                    String locStr = config.getString("warps." + key);
                    assert locStr != null;
                    Location loc = LocationUtils.deserializeLocation(locStr);
                    if (loc.getWorld() == null) continue;
                    warps.put(key, loc);
                } catch (Exception e) {
                    Logger.excs.add(e);
                    Logger.warn(e.getMessage());
                }
            }
        }
    }


    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> warpCmd = Commands.literal("warp")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.warp"))
                .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> {
                    for (String warp : warps.keySet()) {
                        if (warp.contains(builder.getRemaining().toLowerCase()) || builder.getRemaining().isEmpty())
                            builder.suggest(warp);
                    }
                    return builder.buildFuture();
                }).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendPlainMessage("Warps are disabled.");
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
                        String warp = ctx.getArgument("name", String.class);
                        if (!warps.containsKey(warp)) {
                            sender.sendMessage(Component.text("Warp \"" + warp + "\" does not exist.").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        double time = KamsTweaks.getInstance().getConfig().getDouble("teleportation.timer");
                        sender.sendMessage(
                                Component.text("Teleporting to the ").color(NamedTextColor.GOLD)
                                        .append(Component.text(warp).color(NamedTextColor.RED))
                                        .append(Component.text(" warp").color(NamedTextColor.GOLD))
                                        .append(Component.text(time > 0 ? " in " : ".").color(NamedTextColor.GOLD))
                                        .append(Component.text(time > 0 ? (time + "") : "").color(NamedTextColor.RED))
                                        .append(Component.text(time > 0 ? " seconds, please do not move." : "").color(NamedTextColor.GOLD)));
                        Location loc = warps.get(warp);
                        handler.scheduleTeleport(player, loc, time);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage("Only players can use warps.");
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(warpCmd.build());
        LiteralArgumentBuilder<CommandSourceStack> delWarpCmd = Commands.literal("delwarp")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.delwarp"))
                .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> {
                    for (String warp : warps.keySet()) {
                        if (warp.contains(builder.getRemaining().toLowerCase()) || builder.getRemaining().isEmpty())
                            builder.suggest(warp);
                    }
                    return builder.buildFuture();
                }).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendPlainMessage("Warps are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    String warp = ctx.getArgument("name", String.class);
                    if (!warps.containsKey(warp)) {
                        sender.sendMessage(Component.text("Warp \"" + warp + "\" does not exist.").color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    warps.remove(warp);
                    sender.sendMessage("Deleted warp " + warp + ".");
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(delWarpCmd.build());
        LiteralArgumentBuilder<CommandSourceStack> warpsCmd = Commands.literal("warps")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.warps"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendPlainMessage("Warps are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    //if (executor instanceof Player) {
                        Component c = Component.text("The server has the following warps:").color(NamedTextColor.GOLD);
                        for (var warp : warps.keySet()) {
                            c = c.appendNewline().append(Component.text(warp).color(NamedTextColor.WHITE));
                        }
                        sender.sendMessage(c);
                        return Command.SINGLE_SUCCESS;
                    //}
                    //sender.sendMessage("Only players can use warps.");
                    //return Command.SINGLE_SUCCESS;
                });
        commands.registrar().register(warpsCmd.build());

        LiteralArgumentBuilder<CommandSourceStack> addwarp = Commands.literal("addwarp")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.addwarp"))
                .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendPlainMessage("Warps are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        String warp = ctx.getArgument("name", String.class);
                        warps.put(warp, player.getLocation());
                        sender.sendMessage(Component.text("Successfully created the warp ").color(NamedTextColor.GOLD)
                                .append(Component.text(warp).color(NamedTextColor.RED))
                                .append(Component.text(".").color(NamedTextColor.GOLD)));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage("Only players can use warps.");
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("location", ArgumentTypes.blockPosition()).executes(ctx -> {
		 CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendPlainMessage("Warps are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                        String warp = ctx.getArgument("name", String.class);
			BlockPosition pos = ctx.getArgument("location", BlockPositionResolver.class).resolve(ctx.getSource()); 
                        warps.put(warp, pos.toLocation(Bukkit.getWorlds().get(0)));
                        sender.sendMessage(Component.text("Successfully created the warp ").color(NamedTextColor.GOLD)
                                .append(Component.text(warp).color(NamedTextColor.RED))
                                .append(Component.text(".").color(NamedTextColor.GOLD)));
                    return Command.SINGLE_SUCCESS;
	}).then(Commands.argument("world", ArgumentTypes.world()).executes(ctx -> {
		 CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendPlainMessage("Warps are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                        String warp = ctx.getArgument("name", String.class);
			BlockPosition pos = ctx.getArgument("location", BlockPositionResolver.class).resolve(ctx.getSource());
			World world = ctx.getArgument("world", World.class);
                        warps.put(warp, pos.toLocation(world));
                        sender.sendMessage(Component.text("Successfully created the warp ").color(NamedTextColor.GOLD)
                                .append(Component.text(warp).color(NamedTextColor.RED))
                                .append(Component.text(".").color(NamedTextColor.GOLD)));
                    return Command.SINGLE_SUCCESS;
	}))));
        commands.registrar().register(addwarp.build());
    }
}
