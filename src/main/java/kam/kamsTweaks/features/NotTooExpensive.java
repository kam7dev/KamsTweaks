package kam.kamsTweaks.features;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;

public class NotTooExpensive extends Feature {
    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("not-too-expensive.enabled", "not-too-expensive.enabled", true, "kamstweaks.configure"));
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
    public void onAnvil(PrepareAnvilEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("not-too-expensive.enabled", false)) return;
        ItemStack item = event.getInventory().getFirstItem();
        if (item == null) return;
        if (!(item.getItemMeta() instanceof Repairable meta)) return;
        if (meta.getRepairCost() == -1 || meta.getRepairCost() > 24) meta.setRepairCost(24);
        item.setItemMeta(meta);
    }
}
