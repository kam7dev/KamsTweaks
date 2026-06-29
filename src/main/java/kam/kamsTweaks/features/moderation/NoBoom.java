package kam.kamsTweaks.features.moderation;

import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.ConfigCommand;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class NoBoom extends Feature {
    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("no-explosions.enabled", "no-explosions.enabled", false, "kamstweaks.configure"));
    }

    @EventHandler
    public void onExplode(BlockExplodeEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("no-explosions.enabled", false)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("no-explosions.enabled", false)) return;
        e.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onExplode(PlayerInteractEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("no-explode.enabled", false)) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            if (!e.getClickedBlock().getType().isInteractable()) return;

            // waiting on a replacement api for this
            if ((e.getClickedBlock().getType().equals(Material.RESPAWN_ANCHOR) && !e.getClickedBlock().getWorld().isRespawnAnchorWorks()) || (e.getClickedBlock().getType().name().contains("BED") && !e.getClickedBlock().getWorld().isBedWorks())) {
                e.setCancelled(true);
            }
        }
    }
}
