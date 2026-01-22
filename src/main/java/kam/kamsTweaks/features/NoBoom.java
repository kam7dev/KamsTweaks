package kam.kamsTweaks.features;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class NoBoom extends Feature {
    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("no-explosions.enabled", "no-explosions.enabled", false, "kamstweaks.configure"));
    }

    @Override
    public void shutdown() {}

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {}

    @Override
    public void loadData() {}

    @Override
    public void saveData() {}

    @EventHandler
    public void onExplode(BlockExplodeEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("no-explosions.enabled", false)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("no-explosions.enabled", false)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onExplode(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null){
            //noinspection deprecation
            if (!e.getClickedBlock().getType().isInteractable()) return;
            if ((e.getClickedBlock().getType().equals(Material.RESPAWN_ANCHOR) && !e.getClickedBlock().getWorld().isRespawnAnchorWorks()) || (e.getClickedBlock().getType().name().contains("BED") && !e.getClickedBlock().getWorld().isBedWorks())) {
                e.setCancelled(true);
            }
        }
    }
}
