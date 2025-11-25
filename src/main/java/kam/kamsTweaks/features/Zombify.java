package kam.kamsTweaks.features;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public class Zombify extends Feature {
    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("zombify.enabled", "zombify.enabled", true, "kamstweaks.configure"));
    }

    @EventHandler
    void onEntityDeath(EntityDeathEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("zombify.enabled", true)) return;
        if (e.getEntity() instanceof Villager villager && e.getDamageSource().getDirectEntity() instanceof Zombie) {
            e.setCancelled(true);
            villager.zombify();
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {

    }

    @Override
    public void loadData() {

    }

    @Override
    public void saveData() {

    }
}
