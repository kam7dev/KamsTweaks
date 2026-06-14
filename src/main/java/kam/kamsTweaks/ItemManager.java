package kam.kamsTweaks;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class ItemManager implements Listener {
    public enum ItemType {
        CLAIM_TOOL("claimer");

        public final NamespacedKey key;
        ItemType(NamespacedKey key) {
            this.key = key;
        }
        ItemType(String namespace, String key) {
            this.key = new NamespacedKey(namespace, key);
        }
        ItemType(String key) {
            this.key = new NamespacedKey("kamstweaks", key);
        }
    }
    public enum ItemTag {
        YUMMY("yummy");

        public final NamespacedKey key;
        ItemTag(NamespacedKey key) {
            this.key = key;
        }
        ItemTag(String namespace, String key) {
            this.key = new NamespacedKey(namespace, key);
        }
        ItemTag(String key) {
            this.key = new NamespacedKey("kamstweaks", key);
        }
    }

    private static Map<ItemType, ItemStack> items;
    private static boolean initialized = false;

    static void init() {
        initialized = true;
        items = new HashMap<>();
        {
            ItemStack item = new ItemStack(Material.MUSIC_DISC_PIGSTEP);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(KTStrings.getFor(KTStrings.ITEM_CLAIM_TOOL).decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(Component.translatable("enchantment.minecraft.protection").append(Component.space(), Component.translatable("enchantment.level.5")).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                meta.setEnchantmentGlintOverride(true);
                meta.setMaxStackSize(64);
                NamespacedKey key = new NamespacedKey("kamstweaks", "item");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "claimer");
                item.setItemMeta(meta);
                item.unsetData(DataComponentTypes.JUKEBOX_PLAYABLE);
                item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "structure_void"));
            }
            items.put(ItemType.CLAIM_TOOL, item);
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
                if (data.equals("claimer") && meta.hasEnchants()) {
                    meta.removeEnchantments();
                    meta.setEnchantmentGlintOverride(true);
                    item.setItemMeta(meta);
                }
                //noinspection SwitchStatementWithTooFewBranches
                return switch (data) {
                    case "claimer" -> ItemType.CLAIM_TOOL;
                    default -> null;
                };
            }
        }
        return null;
    }
}
