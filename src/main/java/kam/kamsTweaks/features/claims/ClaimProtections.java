package kam.kamsTweaks.features.claims;

import kam.kamsTweaks.ItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ClaimProtections implements Listener {
    Claims claims = null;

    public void setup(Claims claims) {
        this.claims = claims;
    }

    @EventHandler
    public void onItem(PlayerInteractEvent e) {
        if (ItemManager.ItemType.CLAIMER.equals(ItemManager.getType(e.getItem()))) {
            claims.dialogGui.openMainPage(e.getPlayer());
        } else {
            if (e.getAction().equals(Action.LEFT_CLICK_AIR)) {
                e.getPlayer().getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.CLAIMER));
            }
        }
    }
}
