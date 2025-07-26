package kam.kamsTweaks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class ItemManager {
    public enum ItemType {
        CLAIMER,
        FLYING_BOOTS,
        BLINDNESS_WAND,
        LEVITATION_SWORD,
        KNOCKBACK_STICK,
        FAKE_TNT,
        PORTAL_BOW
    }
    private static Map<ItemType, ItemStack> items;
    private static boolean initialized = false;

    static void init() {
        initialized = true;
        items = new HashMap<>();
        {
            ItemStack item = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Land Claim Tool").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                meta.addEnchant(Enchantment.PROTECTION, 5, true);
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "claimer");
                item.setItemMeta(meta);
            }
            items.put(ItemType.CLAIMER, item);
        }

        {
            ItemStack item = new ItemStack(Material.IRON_BOOTS);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true);
                meta.displayName(Component.text("Flying Boots").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "flying_boots");
                item.setItemMeta(meta);
            }
            items.put(ItemType.FLYING_BOOTS, item);
        }
        {
            ItemStack item = new ItemStack(Material.GOLDEN_HOE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true);
                meta.displayName(Component.text("Blindness Wand").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "blindness_wand");
                item.setItemMeta(meta);
            }
            items.put(ItemType.BLINDNESS_WAND, item);
        }
        {
            ItemStack item = new ItemStack(Material.GOLDEN_SWORD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true);
                meta.displayName(Component.text("Levitation Sword").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "levitation_sword");
                item.setItemMeta(meta);
            }
            items.put(ItemType.LEVITATION_SWORD, item);
        }
        {
            ItemStack item = new ItemStack(Material.STICK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Knockback Stick").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "knockback_stick");
                meta.addEnchant(Enchantment.KNOCKBACK, 10, true);
                item.setItemMeta(meta);
            }
            items.put(ItemType.KNOCKBACK_STICK, item);
        }
        {
            ItemStack item = new ItemStack(Material.TNT);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Fake TNT").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "fake_tnt");
                item.setItemMeta(meta);
            }
            items.put(ItemType.FAKE_TNT, item);
        }
        {
            ItemStack item = new ItemStack(Material.BOW);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Portal Bow").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "portal_bow");
                meta.setUnbreakable(true);
                meta.addEnchant(Enchantment.INFINITY, 1, false);
                item.setItemMeta(meta);
            }
            items.put(ItemType.PORTAL_BOW, item);
        }
    }

    public static ItemStack createItem(ItemType type) {
        if (!initialized) init();
        return items.get(type);
    }

    public static ItemType getType(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey("kamstweaks", "item");
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (data == null) return null;
                return switch (data) {
                    case "claimer" -> ItemType.CLAIMER;
                    case "flying_boots" -> ItemType.FLYING_BOOTS;
                    case "blindness_wand" -> ItemType.BLINDNESS_WAND;
                    case "levitation_sword" -> ItemType.LEVITATION_SWORD;
                    case "knockback_stick" -> ItemType.KNOCKBACK_STICK;
                    case "fake_tnt" -> ItemType.FAKE_TNT;
                    case "portal_bow" -> ItemType.PORTAL_BOW;
                    default -> null;
                };
            }
        }
        return null;
    }
}
