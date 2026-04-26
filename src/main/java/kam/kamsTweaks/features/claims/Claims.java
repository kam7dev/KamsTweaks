package kam.kamsTweaks.features.claims;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

public class Claims extends Feature {
    public ClaimProtections protections = new ClaimProtections();
    public ClaimsDialogGui dialogGui = new ClaimsDialogGui();
    public LandClaims landClaims = new LandClaims();
    public EntityClaims entityClaims = new EntityClaims();

    private static Claims instance;

    private File claimsFile;
    FileConfiguration claimsConfig;

    final List<UUID> hasMessaged = new ArrayList<>();

    // just a helper cause its nicer
    public void message(Entity player, Component message) {
        if (!(player instanceof Player)) return;
        if (hasMessaged.contains(player.getUniqueId()))
            return;
        hasMessaged.add(player.getUniqueId());
        player.sendActionBar(message);
    }

    @Override
    public void setup() {
        instance = this;

        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("land-claims.enabled", "land-claims.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("land-claims.max-claims", "land-claims.max-claims", 30, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("land-claims.max-claim-size", "land-claims.max-claim-size", 50000, "kamstweaks.configure"));

        landClaims.setup(this);
        entityClaims.setup(this);
//        protections.setup(this);
//        dialogGui.setup(this);

        Bukkit.getServer().getPluginManager().registerEvents(protections, KamsTweaks.get());
        Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), hasMessaged::clear, 1, 1);
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {

    }

    private void setupFile() {
        claimsFile = new File(KamsTweaks.get().getDataFolder(), "claims.yml");
        if (!claimsFile.exists()) {
            boolean ignored = claimsFile.getParentFile().mkdirs();
            KamsTweaks.get().saveResource("claims.yml", false);
        }
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
    }

    public boolean isClaimable(Entity e) {
        if (e instanceof Boat) return true;
        if (!(e instanceof Mob)) return false;
        switch (e.getType()) {
            case ELDER_GUARDIAN, ENDER_DRAGON, WITHER, WARDEN -> {
                return false;
            }
            default -> {
                return true;
            }
        }
    }

    @Override
    public void loadData() {

    }

    @Override
    public void saveData() {

    }

    public enum OptBool {
        TRUE,
        FALSE,
        DEFAULT,
    }

    public static Claims get() {
        return instance;
    }
}
