package kam.kamsTweaks;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import kam.kamsTweaks.ext.*;
import kam.kamsTweaks.features.*;
import kam.kamsTweaks.features.claims.Claims;
import kam.kamsTweaks.features.fun.*;
import kam.kamsTweaks.features.fun.nicknames.Names;
import kam.kamsTweaks.features.gameplay.*;
import kam.kamsTweaks.features.moderation.*;
import kam.kamsTweaks.features.teleportation.TeleportFeatures;
import kam.kamsTweaks.managers.KTItems;
import kam.kamsTweaks.managers.KTPerms;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        features.add(new PVP());
        features.add(new Names());
        features.add(new Claims());
        features.add(new Graves());
        features.add(new NoBoom());
        features.add(new Vanish());
        features.add(new MobEdits());
        features.add(new SlashHat());
        features.add(new ChatFilter());
        features.add(new UserKeepInv());
        features.add(new SilkSpawner());
        features.add(new SeedDispenser());
        features.add(new VirtualInsanity());
        features.add(new DragonFightLock());
        features.add(new TeleportFeatures());
    }

    @Override
    public void onEnable() {
        m_instance = this;
        Logger.init();
        this.saveDefaultConfig();
        loadConfigs();

        KTPerms.registerPermissions();
        KTItems.init();
        if (Config.getBool("gameplay-pack.enabled", true)) features.add(new GameplayPack());

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
        getServer().getPluginManager().registerEvents(new KTItems(), this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            for (var feature : features) {
                try {
                    feature.registerCommands(commands);
                }  catch(Exception e) {
                    Logger.handleException(e);
                }
            }
            var base = Commands.literal("kamstweaks").then(Commands.literal("version").executes(ctx -> {
                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.VERSION, Component.text(KamsTweaks.get().getPluginMeta().getVersion())));
                return Command.SINGLE_SUCCESS;
            })).then(Commands.literal("save").executes(ctx -> {
                KamsTweaks.get().save();
                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.SAVED).color(NamedTextColor.GREEN));
                return Command.SINGLE_SUCCESS;
            }).requires(source -> KTPerms.hasPermission(source, KTPerms.SAVE)));
            for (var feature : features) {
                try {
                    feature.registerKTSub(base);
                }  catch(Exception e) {
                    Logger.handleException(e);
                }
            }
            Config.registerKTSub(base);
            Logger.registerKTSub(base);
            commands.registrar().register(base.build(), List.of("kt"));
            KTItems.registerCommand(commands);
        });

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::save, 20 * 60 * 5, 20 * 60 * 5);

        Config.bool("ext.discordsrv.enabled", true).requiresRestart(true).build().add();
        Config.string("ext.discordsrv.chat-filter.channel-id").requiresRestart(true).build().add();
        Config.string("ext.discordsrv.chat-filter.role-id").requiresRestart(true).build().add();
        if (Config.getBool("ext.discordsrv.enabled", true)) SRVHelper.init();

        Config.bool("ext.geyser.enabled", true).requiresRestart(true).build().add();
        Config.bool("ext.geyser.kamstweaks-item", true).requiresRestart(true).build().add();
        Config.bool("ext.geyser.anvixos-bits-items", true).requiresRestart(true).build().add();
        if (getServer().getPluginManager().isPluginEnabled("Geyser-Spigot") && Config.getBool("ext.geyser.enabled", true)) bits = new GeyserItemData();
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
        try {
            generalConfig = YamlConfiguration.loadConfiguration(generalFile);
        } catch (Exception e) {
            Logger.handleException(e);
        }
        saveDefaultConfig();
    }

    public FileConfiguration getDataConfig() {
        return generalConfig;
    }
}
