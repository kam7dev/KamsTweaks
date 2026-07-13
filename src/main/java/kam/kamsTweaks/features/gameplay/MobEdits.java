package kam.kamsTweaks.features.gameplay;

import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.Config;
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
        Config.bool("mob-edits.always-zombify", true).build().add();
        Config.bool("mob-edits.stand-arms", true).build().add();
    }

    @EventHandler
    void onStandPlace(EntityPlaceEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("mob-edits.stand-arms", true)) return;
        var plr = e.getPlayer();
        if (plr != null && plr.isSneaking()) return;
        if (e.getEntity() instanceof ArmorStand stand) {
            stand.setArms(true);
        }
    }

    @EventHandler
    void onEntityDeath(EntityDeathEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("mob-edits.always-zombify", true)) return;
        if (e.getEntity() instanceof Villager villager && e.getDamageSource().getDirectEntity() instanceof Zombie) {
            e.setCancelled(true);
            villager.zombify();
        }
    }
}
