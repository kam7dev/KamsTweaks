package kam.kamsTweaks.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class Inventories {
    public static void saveInventory(Inventory inventory, Configuration config, String key) {
        if (inventory == null) return;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            config.set(key + ".slot." + i, item);
        }
    }

    public static Inventory loadInventory(Component title, int size, Configuration config, String key) {
        Inventory inventory = Bukkit.createInventory(null, size, title);
        for (int i = 0; i < size; i++) {
            ItemStack item = config.getItemStack(key + ".slot." + i);
            inventory.setItem(i, item);
        }
        return inventory;
    }

    public static Inventory copyPlayerInventory(PlayerInventory inv) {
        Inventory ret = Bukkit.createInventory(null, 45, Component.empty());
        for (int slot = 0; slot < 36; slot++) {
            ret.setItem(slot, inv.getItem(slot));
        }
        inv.setItem(36, inv.getBoots());
        inv.setItem(37, inv.getLeggings());
        inv.setItem(38, inv.getChestplate());
        inv.setItem(39, inv.getHelmet());
        inv.setItem(40, inv.getItemInOffHand());
        return ret;
    }
}
