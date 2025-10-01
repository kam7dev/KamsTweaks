package kam.kamsTweaks.features.tp;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

public class Back extends Feature {
    @Override
    public void setup() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("back")
                .requires(source -> source.getSender().hasPermission("kamstweaks.teleports.back"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("teleportation.back.enabled", true)) {
                        sender.sendPlainMessage("/back is disabled.");
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
                        if (!handler.locations.containsKey(player)) {
                            sender.sendMessage(Component.text("You have not teleported anywhere recently.").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        int time = KamsTweaks.getInstance().getConfig().getInt("teleportation.timer");
                        sender.sendMessage(
                                Component.text("Returning to previous location").color(NamedTextColor.GOLD)
                                        .append(Component.text(time > 0 ? " in " : ".").color(NamedTextColor.GOLD))
                                        .append(Component.text(time > 0 ? (time + " seconds") : "").color(NamedTextColor.RED))
                                        .append(Component.text(time > 0 ? ", please do not move." : "").color(NamedTextColor.GOLD)));
                        handler.scheduleTeleport(player, handler.locations.get(player), time);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage("Only players can use /back.");
                    return Command.SINGLE_SUCCESS;
                });
        LiteralCommandNode<CommandSourceStack> buildCommand = command.build();
        commands.registrar().register(buildCommand);
    }

    @Override
    public void loadData() {

    }

    @Override
    public void saveData() {

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getPlayer();
        TeleportFeatures.get().locations.put(player, player.getLocation());
        player.sendMessage(Component.text("Return to your death location with /back.").color(NamedTextColor.GOLD));
    }
}