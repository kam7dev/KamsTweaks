package kam.kamsTweaks.features.gameplay;

import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.ConfigCommand;
import kam.kamsTweaks.utils.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;

public class SilkSpawner extends Feature {
    @Override
    public void setup() {
        ConfigCommand.addConfig(
                new ConfigCommand.BoolConfig("silk-spawners.enabled", "silk-spawners.enabled", true, "kamstweaks.configure"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("silk-spawners.enabled", true))
            return;
        if (!e.getPlayer().hasPermission("kamstweaks.silkspawner"))
            return;
        if (e.getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0) {
            if (e.getBlock().getType() == Material.SPAWNER && e.isDropItems()) {
                e.setDropItems(false);
                e.setExpToDrop(0);
                ItemStack spawner = new ItemStack(Material.SPAWNER, 1);
                var spawns = ((CreatureSpawner) e.getBlock().getState()).getSpawnedType();
                if (spawns != null) {
                    BlockStateMeta meta = (BlockStateMeta) spawner.getItemMeta();
                    meta.displayName(KTStrings.getFor(KTStrings.SPAWNER, Component.translatable(spawns.translationKey()).color(NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
                    var state = meta.getBlockState();
                    ((CreatureSpawner) state).setSpawnedType(spawns);
                    meta.setBlockState(state);
                    spawner.setItemMeta(meta);
                }
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), spawner);
            }
        }
    }

    // For older spawners to still work
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("silk-spawners.enabled", true))
            return;
        if (e.getBlock().getState() instanceof CreatureSpawner spawner) {
            var name = e.getItemInHand().getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey("kamstweaks", "spawner-mob"), PersistentDataType.STRING);
            if (name == null)
                return;
            EntityType type = EntityType.valueOf(name);
            spawner.setSpawnedType(type);
            spawner.update(true);
        }
    }
}
