package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.gameplay.PVP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Spawn extends Feature {
    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.spawn"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.spawn.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/spawn")));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        if (PVP.instance.inCombat.containsKey(player.getUniqueId())) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        var handler = TeleportFeatures.get();
                        if (handler.teleportations.containsKey(player)) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.TP_ALREADY_TELEPORTING).color(NamedTextColor.GOLD));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (handler.onCooldown.containsKey(player)) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.TP_COOLDOWN, Component.text(handler.onCooldown.get(player)).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
                            return Command.SINGLE_SUCCESS;
                        }
                        int time = KamsTweaks.get().getConfig().getInt("teleportation.timer");
                        sender.sendMessage(KTStrings.getFor(KTStrings.TP_TO_SPAWN, Component.text(time).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
                        Location loc = Bukkit.getWorlds().getFirst().getSpawnLocation().toHighestLocation().add(.5, 1, .5);
                        handler.scheduleTeleport(player, loc, time);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/spawn")));
                    return Command.SINGLE_SUCCESS;
                });
        commands.registrar().register(spawn.build());
    }
}
