package kam.kamsTweaks.features;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
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
        if (!KamsTweaks.getInstance().getConfig().getBoolean("no-explode.enabled", false)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("no-explode.enabled", false)) return;
        e.setCancelled(true);
    }
}
