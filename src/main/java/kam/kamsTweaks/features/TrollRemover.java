package kam.kamsTweaks.features;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import kam.kamsTweaks.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;

import java.util.ArrayList;
import java.util.List;

public class TrollRemover implements Listener {

    private static final List<ItemManager.ItemType> trollItems = List.of(
            ItemManager.ItemType.BLINDNESS_WAND,
            ItemManager.ItemType.FAKE_TNT,
            ItemManager.ItemType.FLYING_BOOTS,
            ItemManager.ItemType.LEVITATION_SWORD,
            ItemManager.ItemType.PORTAL_BOW,
            ItemManager.ItemType.KNOCKBACK_STICK
    );

    private boolean isTrollItem(ItemStack item) {
        return item != null && trollItems.contains(ItemManager.getType(item));
    }

    private void removeTrollItemsFromBundle(BundleMeta meta) {
        List<ItemStack> keep = new ArrayList<>();
        for (ItemStack i : meta.getItems()) {
            if (!isTrollItem(i)) {
                keep.add(i);
            }
        }
        meta.setItems(keep);
    }

    private void processItem(ItemStack item, List<ItemStack> toRemove) {
        if (isTrollItem(item)) {
            toRemove.add(item);
        } else if (item != null && item.getItemMeta() instanceof BundleMeta meta) {
            removeTrollItemsFromBundle(meta);
        }
    }

    private void removeTrollItemsFromInventory(Iterable<ItemStack> inventory, List<ItemStack> toRemove) {
        inventory.forEach(item -> processItem(item, toRemove));
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        removeTrollItemsFromInventory(event.getPlayer().getInventory(), toRemove);
        toRemove.forEach(event.getPlayer().getInventory()::remove);
    }

    @EventHandler
    void onUse(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (isTrollItem(item)) {
            event.getPlayer().getInventory().remove(item);
        } else if (item != null && item.getItemMeta() instanceof BundleMeta meta) {
            removeTrollItemsFromBundle(meta);
        }
    }

    @EventHandler
    void onEquip(PlayerArmorChangeEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        processItem(event.getOldItem(), toRemove);
        processItem(event.getNewItem(), toRemove);
        toRemove.forEach(event.getPlayer().getInventory()::remove);
    }

    @EventHandler
    void onSlotChange(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (isTrollItem(currentItem)) {
            event.getInventory().remove(currentItem);
        } else if (currentItem != null && currentItem.getItemMeta() instanceof BundleMeta meta) {
            removeTrollItemsFromBundle(meta);
        }
    }

    @EventHandler
    void onSlotChange(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (isTrollItem(item)) {
            event.getSource().remove(item);
            event.getDestination().remove(item);
        } else if (item != null && item.getItemMeta() instanceof BundleMeta meta) {
            removeTrollItemsFromBundle(meta);
        }
    }

    @EventHandler
    void onSlotChange(InventoryPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (isTrollItem(item)) {
            event.getInventory().remove(item);
        } else if (item.getItemMeta() instanceof BundleMeta meta) {
            removeTrollItemsFromBundle(meta);
        }
    }

    @EventHandler
    void onInvOpen(InventoryOpenEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        removeTrollItemsFromInventory(event.getInventory(), toRemove);
        removeTrollItemsFromInventory(event.getPlayer().getInventory(), toRemove);
        toRemove.forEach(item -> {
            event.getInventory().remove(item);
            event.getPlayer().getInventory().remove(item);
        });
    }

    @EventHandler
    void onServerStart(org.bukkit.event.server.ServerLoadEvent event) {
        var world = Bukkit.getWorlds();
        for (var w : world) {
            for (var entity : w.getEntities()) {
                if (entity instanceof InventoryHolder holder) {
                    List<ItemStack> toRemove = new ArrayList<>();
                    removeTrollItemsFromInventory(holder.getInventory(), toRemove);
                    toRemove.forEach(holder.getInventory()::remove);
                } else if (entity instanceof ItemFrame frame) {
                    ItemStack item = frame.getItem();
                    if (isTrollItem(item)) {
                        frame.setItem(null);
                    }
                } else if (entity instanceof Item itemE) {
                    ItemStack item = itemE.getItemStack();
                    if (isTrollItem(item)) {
                        itemE.remove();
                    }
                }
            }
        }
    }
}
