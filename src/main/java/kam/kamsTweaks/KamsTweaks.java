package kam.kamsTweaks;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import kam.kamsTweaks.features.SeedDispenser;
import kam.kamsTweaks.features.landclaims.LandClaims;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class KamsTweaks extends JavaPlugin {
    private static KamsTweaks m_instance = null;
    SeedDispenser m_seedDispenser = new SeedDispenser();
    LandClaims m_landClaims = new LandClaims();

    public static KamsTweaks getInstance() {
        return m_instance;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        m_instance = this;

        this.saveDefaultConfig();
        m_landClaims.loadClaims();

        getServer().getPluginManager().registerEvents(m_seedDispenser, this);
        getServer().getPluginManager().registerEvents(m_landClaims, this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            m_landClaims.registerCommands(commands);
        });

    }

    @Override
    public void onDisable() {
        m_landClaims.saveClaims();
    }
}
