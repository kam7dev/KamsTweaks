package kam.kamsTweaks.utils;

import kam.kamsTweaks.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Inventories {
    public static void saveInventory(Inventory inventory, FileConfiguration config, String key) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            config.set(key + ".slot." + i, item);
        }
    }

    public static Inventory loadInventory(Component title, int size, FileConfiguration config, String key) {
        Inventory inventory = Bukkit.createInventory(null, size, title);
        for (int i = 0; i < size; i++) {
            ItemStack item = config.getItemStack(key + ".slot." + i);
            inventory.setItem(i, item);
        }
        return inventory;
    }
}
