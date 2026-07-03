package kam.kamsTweaks.features.gameplay;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.utils.UserDataManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class UserKeepInv extends Feature {
    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("keepinv")
                .then(Commands.literal("toggle").executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("keepinv.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, KTStrings.getFor(KTStrings.KEEPINV)));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (ctx.getSource().getExecutor() instanceof Player player) {
                        var newState = !UserDataManager.get(player.getUniqueId(), "keepinv.enabled", true);
                        UserDataManager.put(player.getUniqueId(), "keepinv.enabled", newState, Boolean.class);
                        player.sendMessage(KTStrings.getFor(KTStrings.KEEPINV_TOGGLED, KTStrings.getFor(newState ? KTStrings.ON : KTStrings.OFF)));
                    } else {
                        sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                    }
                    return Command.SINGLE_SUCCESS;
                })).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("keepinv.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, KTStrings.getFor(KTStrings.KEEPINV)));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (ctx.getSource().getExecutor() instanceof Player player) {
                        player.sendMessage(KTStrings.getFor(KTStrings.KEEPINV_STATUS, KTStrings.getFor(UserDataManager.get(player.getUniqueId(), "keepinv.enabled", true) ? KTStrings.ON : KTStrings.OFF).color(NamedTextColor.GOLD)));
                    } else {
                        sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .build());
    }

    @EventHandler
    void onDeath(PlayerDeathEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("keepinv.enabled", true)) return;
        if (UserDataManager.get(e.getPlayer().getUniqueId(), "keepinv.enabled", true)) {
            e.setKeepInventory(true);
            for (var item : e.getPlayer().getInventory().getContents()) {
                if (item == null || item.isEmpty()) continue;
                e.getDrops().removeFirst();
            }
            for (int i = 0; i <= 40; i++) {
                var item = e.getPlayer().getInventory().getItem(i);
                if (item != null && !item.isEmpty() && item.getItemMeta().hasEnchant(Enchantment.VANISHING_CURSE)) e.getPlayer().getInventory().setItem(i, ItemStack.empty());
            }
        }
    }
}
