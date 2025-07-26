package kam.kamsTweaks.features;

import kam.kamsTweaks.ItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TrollRemover implements Listener {
    @EventHandler
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
}
