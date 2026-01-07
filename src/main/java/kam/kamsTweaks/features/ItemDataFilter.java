package kam.kamsTweaks.features;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.registry.keys.DataComponentTypeKeys;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class ItemDataFilter extends Feature {

    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("potion-filter.enabled", "potion-filter.enabled", false, "kamstweaks.configure"));
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
    public void onInteract(PlayerInteractEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("potion-filter.enabled", false)) return;
        var item = e.getItem();
        if (item == null) return;
        if (item.hasData(DataComponentTypes.POTION_CONTENTS)) {
            var data = item.getData(DataComponentTypes.POTION_CONTENTS);
            if (data == null) return;
            if ((data.potion() == PotionType.HARMING || data.potion() == PotionType.STRONG_HARMING))
                e.setCancelled(true);
            for (var eff : data.allEffects()) {
                if (eff.getType() == PotionEffectType.POISON) {
                    e.setCancelled(true);
                } else if (eff.getType() == PotionEffectType.WITHER) {
                    e.setCancelled(true);
                }
            }
        }
        for (var t : item.getDataTypes()) {
            if (t.key().equals(DataComponentTypeKeys.ENTITY_DATA.key())) {
                item.resetData(t);
            }
        }
    }
}
