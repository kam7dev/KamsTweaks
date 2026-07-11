package kam.kamsTweaks.features.fun;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.managers.KTStrings;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public class SlashHat extends Feature {
    @Override
    public void setup() {
        Config.addConfig(new Config.BoolConfigOption("slash-hat.enabled", "slash-hat.enabled", true, "kamstweaks.configure"));
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("hat")
                .requires(source -> source.getSender().hasPermission("kamstweaks.hat"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("slash-hat.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/hat")));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        PlayerInventory inv = player.getInventory();
                        ItemStack hand = inv.getItemInMainHand();
                        ItemStack helmet = inv.getHelmet();
                        // no cheat
                        if (helmet.containsEnchantment(Enchantment.BINDING_CURSE)) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.SLASHHAT_BINDING, helmet.displayName()));
                            return Command.SINGLE_SUCCESS;
                        }
                        inv.setItemInMainHand(helmet);
                        inv.setHelmet(hand);
                        sender.sendMessage(KTStrings.getFor(KTStrings.SLASHHAT_EQUIP, hand.displayName()));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                    return Command.SINGLE_SUCCESS;
                });
        LiteralCommandNode<CommandSourceStack> buildCommand = command.build();
        commands.registrar().register(buildCommand);

    }
}
