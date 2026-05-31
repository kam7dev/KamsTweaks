package kam.kamsTweaks.features;

import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

public class Zombify extends Feature {
    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("zombify.enabled", "zombify.enabled", true, "kamstweaks.configure"));
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
