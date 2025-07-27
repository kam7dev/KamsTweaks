package kam.kamsTweaks.features;

import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.events.SafeEventHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class SilkSpawner {
    @SafeEventHandler
    public void onBreak(BlockBreakEvent e) throws Exception {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("silk-spawners.enabled", true)) return;
        if (e.getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0) {
            if (e.getBlock().getType() == Material.SPAWNER && e.isDropItems()) {
                e.setDropItems(false);
                e.setExpToDrop(0);
                ItemStack spawner = new ItemStack(Material.SPAWNER, 1);
                EntityType spawns = ((CreatureSpawner) e.getBlock().getState()).getSpawnedType();
                if (spawns != null) {
                    ItemMeta meta = spawner.getItemMeta();
                    meta.displayName(Component.translatable(spawns.translationKey()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).append(Component.text(" Spawner").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE)));
                    meta.lore(Collections.singletonList(Component.text("Spawns ").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE).append(Component.translatable(spawns.translationKey()).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))));
                    meta.getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "spawner-mob"), PersistentDataType.STRING, spawns.toString());
                    spawner.setItemMeta(meta);
                }
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), spawner);
            }
        }
    }

    @SafeEventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("silk-spawners.enabled", true)) return;
        if (e.getBlock().getState() instanceof CreatureSpawner spawner) {
            var name = e.getItemInHand().getItemMeta().getPersistentDataContainer().get(new NamespacedKey("kamstweaks", "spawner-mob"), PersistentDataType.STRING);
            if (name == null) return;
            EntityType type = EntityType.valueOf(name);
            spawner.setSpawnedType(type);
            spawner.update(true);
        }
    }
}
