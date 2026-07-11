package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.features.gameplay.PVP;
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
    public void saveData() {
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        config.set("warps", null);
        warps.forEach((name, loc) -> {
            if (loc != null) config.set("warps." + name, LocationUtils.serializeLocation(loc));
        });
    }

    @Override
    public void loadData() {
        warps.clear();
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        if (config.contains("warps")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("warps")).getKeys(false)) {
                try {
                    String locStr = config.getString("warps." + key);
                    assert locStr != null;
                    Location loc = LocationUtils.deserializeLocation(locStr);
                    if (loc.getWorld() == null) continue;
                    warps.put(key, loc);
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
    }


    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> warpCmd = Commands.literal("warp")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.warp"))
                .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> {
                    for (String warp : warps.keySet()) {
                        if (warp.toLowerCase().contains(builder.getRemaining().toLowerCase()) || builder.getRemaining().isEmpty())
                            builder.suggest(warp);
                    }
                    return builder.buildFuture();
                }).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.WARPS)));
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
                        String warp = ctx.getArgument("name", String.class);
                        if (!warps.containsKey(warp)) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.WARP_NOT_EXIST, Component.text(warp)).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        double time = KamsTweaks.get().getConfig().getDouble("teleportation.timer");
                        sender.sendMessage(KTStrings.getFor(KTStrings.TP_TO_WARP,
                                Component.text(warp).color(NamedTextColor.RED),
                                Component.text((int) time).color(NamedTextColor.RED)
                        ).color(NamedTextColor.GOLD));
                        Location loc = warps.get(warp);
                        handler.scheduleTeleport(player, loc, time);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, KTStrings.getFor(KTStrings.WARPS)));
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(warpCmd.build());
        LiteralArgumentBuilder<CommandSourceStack> delWarpCmd = Commands.literal("delwarp")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.delwarp"))
                .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> {
                    for (String warp : warps.keySet()) {
                        if (warp.toLowerCase().contains(builder.getRemaining().toLowerCase()) || builder.getRemaining().isEmpty())
                            builder.suggest(warp);
                    }
                    return builder.buildFuture();
                }).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.WARPS)));
                        return Command.SINGLE_SUCCESS;
                    }
                    String warp = ctx.getArgument("name", String.class);
                    if (!warps.containsKey(warp)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.WARP_NOT_EXIST, Component.text(warp)).color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    warps.remove(warp);
                    sender.sendMessage(KTStrings.getFor(KTStrings.WARP_DELETED, Component.text(warp)).color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(delWarpCmd.build());
        LiteralArgumentBuilder<CommandSourceStack> warpsCmd = Commands.literal("warps")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.warps"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.WARPS)));
                        return Command.SINGLE_SUCCESS;
                    }
                    Component c = KTStrings.getFor(KTStrings.WARP_COUNT, Component.text(warps.size())).color(NamedTextColor.GOLD);
                    for (var warp : warps.keySet()) {
                        var loc = warps.get(warp);
                        c = c.appendNewline().append(KTStrings.getFor(KTStrings.WARP_INFO,
                                Component.text(warp).color(NamedTextColor.GOLD),
                                Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()).color(NamedTextColor.GREEN),
                                Component.text(loc.getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE)).color(NamedTextColor.WHITE));
                    }
                    sender.sendMessage(c);
                    return Command.SINGLE_SUCCESS;
                });
        commands.registrar().register(warpsCmd.build());

        LiteralArgumentBuilder<CommandSourceStack> addwarp = Commands.literal("addwarp")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.addwarp"))
                .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.WARPS)));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        String warp = ctx.getArgument("name", String.class);
                        warps.put(warp, player.getLocation());
                        sender.sendMessage(KTStrings.getFor(KTStrings.WARP_CREATED, Component.text(warp).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, KTStrings.getFor(KTStrings.WARPS)));
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("location", ArgumentTypes.blockPosition()).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.WARPS)));
                        return Command.SINGLE_SUCCESS;
                    }
                    String warp = ctx.getArgument("name", String.class);
                    BlockPosition pos = ctx.getArgument("location", BlockPositionResolver.class).resolve(ctx.getSource());
                    warps.put(warp, pos.toLocation(Bukkit.getWorlds().getFirst()));
                    sender.sendMessage(KTStrings.getFor(KTStrings.WARP_CREATED, Component.text(warp).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("world", ArgumentTypes.world()).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.warp.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.WARPS)));
                        return Command.SINGLE_SUCCESS;
                    }
                    String warp = ctx.getArgument("name", String.class);
                    BlockPosition pos = ctx.getArgument("location", BlockPositionResolver.class).resolve(ctx.getSource());
                    World world = ctx.getArgument("world", World.class);
                    warps.put(warp, pos.toLocation(world));
                    sender.sendMessage(KTStrings.getFor(KTStrings.WARP_CREATED, Component.text(warp).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
                    return Command.SINGLE_SUCCESS;
                }))));
        commands.registrar().register(addwarp.build());
    }
}
