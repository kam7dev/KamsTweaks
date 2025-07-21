package kam.kamsTweaks;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import kam.kamsTweaks.features.SeedDispenser;
import kam.kamsTweaks.features.SilkSpawner;
import kam.kamsTweaks.features.landclaims.EntityClaims;
import kam.kamsTweaks.features.landclaims.LandClaims;
import org.bukkit.plugin.java.JavaPlugin;

public final class KamsTweaks extends JavaPlugin {
    private static KamsTweaks m_instance = null;
    SeedDispenser m_seedDispenser = new SeedDispenser();
    public LandClaims m_landClaims = new LandClaims();
    public EntityClaims m_entityClaims = new EntityClaims();
    SilkSpawner m_silkSpawner = new SilkSpawner();

    public static KamsTweaks getInstance() {
        return m_instance;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        m_instance = this;

        this.saveDefaultConfig();
        m_landClaims.loadClaims();

        m_landClaims.setup();
        getServer().getPluginManager().registerEvents(m_entityClaims, this);
        getServer().getPluginManager().registerEvents(m_seedDispenser, this);
        getServer().getPluginManager().registerEvents(m_silkSpawner, this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            m_landClaims.registerCommands(commands);
        });

    }

    @Override
    public void onDisable() {
        m_landClaims.saveClaims();
    }
}
