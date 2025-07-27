package kam.kamsTweaks.features;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.utils.events.SafeEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TrollRemover implements Listener {
    @SafeEventHandler
    void onJoin(PlayerJoinEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        event.getPlayer().getInventory().forEach(item -> {
            switch(ItemManager.getType(item)) {
                case BLINDNESS_WAND, FAKE_TNT, FLYING_BOOTS, LEVITATION_SWORD, PORTAL_BOW, KNOCKBACK_STICK -> toRemove.add(item);
                case null, default -> {}
            }
        });
        for (ItemStack item : toRemove) {
            event.getPlayer().getInventory().remove(item);
        }
    }

    @SafeEventHandler
    void onUse(PlayerInteractEvent event) {
        switch (ItemManager.getType(event.getItem())) {
            case BLINDNESS_WAND, FAKE_TNT, FLYING_BOOTS, LEVITATION_SWORD, PORTAL_BOW, KNOCKBACK_STICK ->
                    event.getPlayer().getInventory().remove(event.getItem());
            case null, default -> {
            }
        }
    }

    @SafeEventHandler
    void onEquip(PlayerArmorChangeEvent event) {
        switch(ItemManager.getType(event.getOldItem())) {
            case BLINDNESS_WAND, FAKE_TNT, FLYING_BOOTS, LEVITATION_SWORD, PORTAL_BOW, KNOCKBACK_STICK -> event.getPlayer().getInventory().remove(event.getOldItem());
            case null, default -> {}
        }
        switch(ItemManager.getType(event.getNewItem())) {
            case BLINDNESS_WAND, FAKE_TNT, FLYING_BOOTS, LEVITATION_SWORD, PORTAL_BOW, KNOCKBACK_STICK -> event.getPlayer().getInventory().remove(event.getNewItem());
            case null, default -> {}
        }
    }
}
