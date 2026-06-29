package kam.kamsTweaks.features.moderation;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.registry.keys.DataComponentTypeKeys;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.ConfigCommand;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

@SuppressWarnings("UnstableApiUsage")
public class ItemDataFilter extends Feature {

    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("potion-filter.enabled", "potion-filter.enabled", false, "kamstweaks.configure"));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("potion-filter.enabled", false)) return;
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
