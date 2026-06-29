package kam.kamsTweaks.features.gameplay;

import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.ConfigCommand;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPlaceEvent;

public class MobEdits extends Feature {
    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("zombify.enabled", "zombify.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("stand-arms.enabled", "stand-arms.enabled", true, "kamstweaks.configure"));
    }

    @EventHandler
    void onStandPlace(EntityPlaceEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("stand-arms.enabled", true)) return;
        var plr = e.getPlayer();
        if (plr != null && plr.isSneaking()) return;
        if (e.getEntity() instanceof ArmorStand stand) {
            stand.setArms(true);
        }
    }

    @EventHandler
    void onEntityDeath(EntityDeathEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("zombify.enabled", true)) return;
        if (e.getEntity() instanceof Villager villager && e.getDamageSource().getDirectEntity() instanceof Zombie) {
            e.setCancelled(true);
            villager.zombify();
        }
    }
}
