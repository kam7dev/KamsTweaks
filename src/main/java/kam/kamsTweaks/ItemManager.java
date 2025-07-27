package kam.kamsTweaks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class ItemManager {
    public enum ItemType {
        CLAIMER,

        // Troll items to be removed via TrollRemover
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
                meta.displayName(
                        Component.text("Land Claim Tool").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                meta.addEnchant(Enchantment.PROTECTION, 5, true);
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "claimer");
                item.setItemMeta(meta);
            }
            items.put(ItemType.CLAIMER, item);
        }
    }

    public static ItemStack createItem(ItemType type) {
        if (!initialized) init();
        return items.get(type);
    }

    public static ItemType getType(ItemStack item) {
        if (item == null) return null;
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
