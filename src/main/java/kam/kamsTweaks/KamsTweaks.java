package kam.kamsTweaks;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import kam.kamsTweaks.features.SeedDispenser;
import kam.kamsTweaks.features.SilkSpawner;
import kam.kamsTweaks.features.TrollRemover;
import kam.kamsTweaks.features.landclaims.EntityClaims;
import kam.kamsTweaks.features.landclaims.LandClaims;
import kam.kamsTweaks.utils.events.SafeEventManager;
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
        Logger.init();
        try {
            this.saveDefaultConfig();
            m_landClaims.loadClaims();

            m_landClaims.setup();
            m_entityClaims.init();
            SafeEventManager.register(m_entityClaims, this);
            SafeEventManager.register(m_seedDispenser, this);
            SafeEventManager.register(m_silkSpawner, this);
            SafeEventManager.register(new TrollRemover(), this);

            this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
                m_landClaims.registerCommands(commands);
                Logger.registerCommands(commands);
            });
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        try {
            m_landClaims.saveClaims();
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }
}
