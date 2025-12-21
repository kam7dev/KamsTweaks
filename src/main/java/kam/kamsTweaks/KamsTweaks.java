package kam.kamsTweaks;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import kam.kamsTweaks.features.*;
import kam.kamsTweaks.features.claims.Claims;
//import kam.kamsTweaks.features.discord.DIFeature;
import kam.kamsTweaks.features.tp.TeleportFeatures;
import kam.kamsTweaks.utils.KamsTweaksPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class KamsTweaks extends JavaPlugin {
  private static KamsTweaks m_instance = null;

  public static KamsTweaks getInstance() {
    return m_instance;
  }

  List<Feature> features = new ArrayList<>();

  KamsTweaks() {
    // this being shortest name to tallest wasn't originally intentional lmao
    features.add(new Names());
    features.add(new Claims());
    features.add(new Graves());
    features.add(new SlashHat());
    features.add(new SilkSpawner());
    features.add(new SeedDispenser());
    features.add(new TeleportFeatures());
    features.add(new Zombify());
    features.add(new ChatFilter());
//    features.add(new DIFeature());
  }

  @Override
  public void onEnable() {
    m_instance = this;
    Logger.init();
    this.saveDefaultConfig();
    loadConfigs();

    for (var feature : features) {
      feature.loadData();
    }
    for (var feature : features) {
      feature.setup();
    }
    for (var feature : features) {
      getServer().getPluginManager().registerEvents(feature, this);
    }
    getServer().getPluginManager().registerEvents(new ItemManager(), this);

    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
      for (var feature : features) {
        feature.registerCommands(commands);
      }
      ConfigCommand.registerCommand(commands);
    });

    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::save, 20 * 60 * 5, 20 * 60 * 5);

    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
      new KamsTweaksPlaceholder().register();
    }

  }

  @Override
  public void onDisable() {
    save();
    for (var feature : features) {
      feature.shutdown();
    }
  }

  public void save() {
    for (var feature : features) {
      feature.saveData();
    }
    saveConfigs();
    Logger.info("Saved KamsTweaks.");
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
      Logger.excs.add(e);
    }

    // Default Config
    saveConfig();
  }

  public void loadConfigs() {
      getConfig().options().copyDefaults(true);
    // General Config
    generalFile = new File(KamsTweaks.getInstance().getDataFolder(), "data.yml");
    if (!generalFile.exists()) {
      boolean ignored = generalFile.getParentFile().mkdirs();
      KamsTweaks.getInstance().saveResource("data.yml", false);
    }
    generalConfig = YamlConfiguration.loadConfiguration(generalFile);
    saveDefaultConfig();
  }

  public FileConfiguration getDataConfig() {
    return generalConfig;
  }
}
