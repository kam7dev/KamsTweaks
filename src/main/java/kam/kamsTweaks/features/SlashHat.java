package kam.kamsTweaks.features;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;

public class SlashHat {
	@SuppressWarnings("UnstableApiUsage")
	public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
		LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("hat").executes(ctx -> {
			CommandSender sender = ctx.getSource().getSender();
                        if (!KamsTweaks.getInstance().getConfig().getBoolean("slash-hat", true)) {
				sender.sendPlainMessage("/hat is disabled.");
				return Command.SINGLE_SUCCESS;
			}
			Entity executor = ctx.getSource().getExecutor();
			if (executor instanceof Player player) {
				  PlayerInventory inv = player.getInventory();
				  ItemStack hand = inv.getItemInMainHand();
				  ItemStack helmet = inv.getHelmet();
				  inv.setItemInMainHand(helmet);
				  inv.setHelmet(hand);
				  sender.sendMessage(Component.text("You're now wearing ").append(hand.displayName()).append(Component.text(".")));
				  return Command.SINGLE_SUCCESS;
			}
			sender.sendMessage("Only players can use /hat.");
			return Command.SINGLE_SUCCESS;
		});
		LiteralCommandNode<CommandSourceStack> buildCommand = command.build();
		commands.registrar().register(buildCommand);
	}
}
