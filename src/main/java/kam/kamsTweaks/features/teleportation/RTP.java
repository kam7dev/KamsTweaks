package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class RTP {
    TeleportationHandler handler;
    Random rand = new Random();
    public void init(TeleportationHandler handler) {
        this.handler = handler;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("rtp")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.rtp"))
                .executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.rtp.enabled", true)) {
                sender.sendPlainMessage("/rtp is disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                if (handler.teleportations.containsKey(player)) {
                    sender.sendMessage(Component.text("You are already teleporting somewhere.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                if (handler.onCooldown.contains(player)) {
                    sender.sendMessage(Component.text("You're currently on teleportation cooldown.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                double time = KamsTweaks.getInstance().getConfig().getDouble("teleportation.timer");
                sender.sendMessage(
                        Component.text("Teleporting to a random location")
                                .color(NamedTextColor.GOLD)
                                .append(Component.text(time > 0 ? " in " : ".").color(NamedTextColor.GOLD))
                                .append(Component.text(time > 0 ? (time + " seconds") : "").color(NamedTextColor.RED))
                                .append(Component.text(time > 0 ? ", please do not move." : "").color(NamedTextColor.GOLD)));
                Location loc = Bukkit.getWorlds().getFirst().getSpawnLocation().clone().add(rand.nextInt(200001) - 100000, 0, rand.nextInt(200001) - 100000).toHighestLocation().add(.5, 1, .5);
                handler.scheduleTeleport(player, loc, time);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /rtp.");
            return Command.SINGLE_SUCCESS;
        });
        commands.registrar().register(spawn.build());
    }
}
