package kam.kamsTweaks.features.tp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
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
    public void setup() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.spawn"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.spawn.enabled", true)) {
                        sender.sendPlainMessage("/spawn is disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        var handler = TeleportFeatures.get();
                        if (handler.teleportations.containsKey(player)) {
                            sender.sendMessage(Component.text("You are already teleporting somewhere.").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (handler.onCooldown.containsKey(player)) {
                            sender.sendMessage(Component.text("You're currently on teleportation cooldown for " + handler.onCooldown.get(player) + " seconds.").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        int time = KamsTweaks.getInstance().getConfig().getInt("teleportation.timer");
                        sender.sendMessage(
                                Component.text("Teleporting to spawn")
                                        .color(NamedTextColor.GOLD)
                                        .append(Component.text(time > 0 ? " in " : ".").color(NamedTextColor.GOLD))
                                        .append(Component.text(time > 0 ? (time + " seconds") : "").color(NamedTextColor.RED))
                                        .append(Component.text(time > 0 ? ", please do not move." : "").color(NamedTextColor.GOLD)));
                        Location loc = Bukkit.getWorlds().getFirst().getSpawnLocation().toHighestLocation().add(.5, 1, .5);
                        handler.scheduleTeleport(player, loc, time);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage("Only players can use /spawn.");
                    return Command.SINGLE_SUCCESS;
                });
        commands.registrar().register(spawn.build());
    }

    @Override
    public void loadData() {

    }

    @Override
    public void saveData() {

    }
}
