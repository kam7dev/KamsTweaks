package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
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

import static kam.kamsTweaks.features.landclaims.LandClaims.deserializeLocation;
import static kam.kamsTweaks.features.landclaims.LandClaims.serializeLocation;

public class Spawn {
    TeleportationHandler handler;
    Map<String, Location> warps = new HashMap<>();
    public void init(TeleportationHandler handler) {
        this.handler = handler;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.spawn"))
                .executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.spawn.enabled", true)) {
                sender.sendPlainMessage("Warps are disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                if (handler.teleportations.containsKey(player)) {
                    sender.sendMessage(Component.text("You are already teleporting somewhere.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                String warp = ctx.getArgument("name", String.class);
                if (!warps.containsKey(warp)) {
                    sender.sendMessage(Component.text("Warp \"" + warp + "\" does not exist.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                double time = KamsTweaks.getInstance().getConfig().getDouble("teleportation.timer");
                sender.sendMessage(
                        Component.text("Teleporting to the " + warp + " warp")
                                .color(NamedTextColor.GOLD)
                                .append(Component.text(time > 0 ? (" in " + time + " seconds, ") : "").color(NamedTextColor.RED))
                                .append(Component.text(time > 0 ? "please do not move." : ".").color(NamedTextColor.GOLD)));
                Location loc = warps.get(warp);
                handler.scheduleTeleport(player, loc, time);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use warps.");
            return Command.SINGLE_SUCCESS;
        });
        commands.registrar().register(spawn.build());
    }
}
