package kam.kamsTweaks;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.FoodProperties;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemManager implements Listener {
    public enum ItemType {
        CLAIMER,
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
                        Component.text("Claim Tool").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
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
                if (data.equals("claimer") && meta.displayName().equals(Component.text("Land Claim Tool").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))) {
                    meta.displayName(Component.text("Claim Tool").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                    item.setItemMeta(meta);
                }
                return switch (data) {
                    case "claimer" -> ItemType.CLAIMER;
                    default -> null;
                };
            }
        }
        return null;
    }

    List<String> funny = List.of("Mmm... Chezburger", "Nom nom nom", "Pizza!", "Can I have chezburger too", "Yummm", "Hamburger", "Yummers.");
    Random rng = new Random();
    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    void yummy(PlayerCommandPreprocessEvent event) {
        if (!event.getPlayer().getName().equals("km7dev")) return;
        var split = event.getMessage().split(" ");
        if (split[0].equals("/yummy")) {
            var item = event.getPlayer().getInventory().getItemInMainHand();
            if (ItemType.CLAIMER.equals(ItemManager.getType(item))) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text(funny.get(rng.nextInt(0, funny.size() - 1))));
                var meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "yummy"), PersistentDataType.BOOLEAN, true);
                item.setItemMeta(meta);
                item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(split.length < 2 ? 1.6f : Float.parseFloat(split[1]))
                        .sound(split.length < 3 ? Registry.SOUNDS.getKey(Sound.AMBIENT_CAVE).key() : Key.key(split[2])));
            }
        }
    }
}
