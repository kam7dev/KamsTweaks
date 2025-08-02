package kam.kamsTweaks;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import kam.kamsTweaks.features.*;
import kam.kamsTweaks.features.landclaims.EntityClaims;
import kam.kamsTweaks.features.landclaims.LandClaims;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;


public final class KamsTweaks extends JavaPlugin {
    private static KamsTweaks m_instance = null;
    SeedDispenser m_seedDispenser = new SeedDispenser();
    public LandClaims m_landClaims = new LandClaims();
    public EntityClaims m_entityClaims = new EntityClaims();
    SilkSpawner m_silkSpawner = new SilkSpawner();
    Names m_names = new Names();

    public static KamsTweaks getInstance() {
        return m_instance;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        m_instance = this;
        Logger.init();
        this.saveDefaultConfig();
        loadConfigs();
        m_landClaims.loadClaims();
        m_names.loadNames();

        m_landClaims.setup();
        m_entityClaims.init();
        m_names.init();
        
        getServer().getPluginManager().registerEvents(m_entityClaims, this);
        getServer().getPluginManager().registerEvents(m_seedDispenser, this);
        getServer().getPluginManager().registerEvents(m_silkSpawner, this);
        getServer().getPluginManager().registerEvents(m_names, this);
        getServer().getPluginManager().registerEvents(new TrollRemover(), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            m_landClaims.registerCommands(commands);
            new SlashHat().registerCommands(commands);
            m_names.registerCommands(commands);
        });
    }

    @Override
    public void onDisable() {
        m_landClaims.saveClaims();
        m_names.saveNames();
        saveConfigs();
    }

    private File generalFile;
    FileConfiguration generalConfig;

    public void saveConfigs() {
        // General Config
        generalFile = new File(KamsTweaks.getInstance().getDataFolder(), "data.yml");
        if (!generalFile.exists()) {
            boolean ignored = generalFile.getParentFile().mkdirs();
            KamsTweaks.getInstance().saveResource("data.yml", false);
        }
        try {
            generalConfig.save(generalFile);
        } catch (IOException e) {
            Logger.warn(e.getMessage());
        }

        // Default Config
        saveConfig();
    }

    public void loadConfigs() {
        // General Config
        generalFile = new File(KamsTweaks.getInstance().getDataFolder(), "data.yml");
        if (!generalFile.exists()) {
            boolean ignored = generalFile.getParentFile().mkdirs();
            KamsTweaks.getInstance().saveResource("data.yml", false);
        }
        generalConfig = YamlConfiguration.loadConfiguration(generalFile);
    }

    public FileConfiguration getGeneralConfig() {
        return generalConfig;
    }
}
