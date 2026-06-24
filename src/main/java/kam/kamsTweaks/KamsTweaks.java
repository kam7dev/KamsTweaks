package kam.kamsTweaks;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import kam.kamsTweaks.ext.GeyserItemData;
import kam.kamsTweaks.features.*;
import kam.kamsTweaks.features.claims.Claims;
//import kam.kamsTweaks.features.discord.DIFeature;
import kam.kamsTweaks.features.tp.TeleportFeatures;
import kam.kamsTweaks.ext.KamsTweaksPlaceholder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class KamsTweaks extends JavaPlugin {
    private static KamsTweaks m_instance = null;

    public static KamsTweaks get() {
        return m_instance;
    }

    GeyserItemData bits;
    List<Feature> features = new ArrayList<>();

    KamsTweaks() {
        features.add(new UserDataManager());

        features.add(new Names());
        features.add(new Claims());
        features.add(new Graves());
        features.add(new NoBoom());
        features.add(new MobEdits());
        features.add(new SlashHat());
        features.add(new ChatFilter());
        features.add(new UserKeepInv());
        features.add(new SilkSpawner());
        features.add(new SeedDispenser());
        features.add(new ItemDataFilter());
        features.add(new PVP());
        features.add(new TeleportFeatures());
    }

    @Override
    public void onEnable() {
        m_instance = this;
        Logger.init();
        this.saveDefaultConfig();
        loadConfigs();

        for (var feature : features) {
            try {
                feature.setup();
            }  catch(Exception e) {
                Logger.handleException(e);
            }
        }
        for (var feature : features) {
            try {
                feature.loadData();
            } catch(Exception e) {
                Logger.handleException(e);
            }
        }
        for (var feature : features) {
            getServer().getPluginManager().registerEvents(feature, this);
        }
        getServer().getPluginManager().registerEvents(new ItemManager(), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            for (var feature : features) {
                try {
                    feature.registerCommands(commands);
                }  catch(Exception e) {
                    Logger.handleException(e);
                }
            }
            ConfigCommand.registerCommand(commands);
            ItemManager.registerCommand(commands);
        });

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::save, 20 * 60 * 5, 20 * 60 * 5);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            new KamsTweaksPlaceholder().register();
        }

        ItemManager.init();

        if (getServer().getPluginManager().isPluginEnabled("Geyser-Spigot")) bits = new GeyserItemData();
    }

    @Override
    public void onDisable() {
        save();
        for (var feature : features) {
            try {
                feature.shutdown();
            } catch(Exception e) {
                Logger.handleException(e);
            }
        }
    }

    public void save() {
        for (var feature : features) {
            try {
                feature.saveData();
            } catch(Exception e) {
                Logger.handleException(e);
            }
        }
        saveConfigs();
        Logger.info("Saved KamsTweaks.");
    }

    public void sendToOps(Component message, List<Player> exclude) {
        Bukkit.getOperators().forEach(offlinePlayer -> {
            if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() instanceof Player player && !exclude.contains(player)) {
                player.sendMessage(message);
            }
        });
    }

    public void sendToOps(Component message, Player... who) {
        sendToOps(message, List.of(who));
    }

    public void sendToOps(Component message) {
        sendToOps(message, List.of());
    }

    private File generalFile;
    FileConfiguration generalConfig;

    public void saveConfigs() {
        // General Config
        generalFile = new File(KamsTweaks.get().getDataFolder(), "data.yml");
        if (!generalFile.exists()) {
            boolean ignored = generalFile.getParentFile().mkdirs();
            KamsTweaks.get().saveResource("data.yml", false);
        }
        try {
            generalConfig.save(generalFile);
        } catch (IOException e) {
            Logger.handleException(e);
        }

        // Default Config
        saveConfig();
    }

    public void loadConfigs() {
        getConfig().options().copyDefaults(true);
        // General Config
        generalFile = new File(KamsTweaks.get().getDataFolder(), "data.yml");
        if (!generalFile.exists()) {
            boolean ignored = generalFile.getParentFile().mkdirs();
            KamsTweaks.get().saveResource("data.yml", false);
        }
        generalConfig = YamlConfiguration.loadConfiguration(generalFile);
        saveDefaultConfig();
    }

    public FileConfiguration getDataConfig() {
        return generalConfig;
    }
}
