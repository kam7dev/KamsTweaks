package kam.kamsTweaks.features.claims;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.features.fun.nicknames.Names;
import kam.kamsTweaks.features.claims.gui.Homepage;
import kam.kamsTweaks.managers.KTItems;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

public class Claims extends Feature {
    public LandClaims landClaims = new LandClaims();
    public EntityClaims entityClaims = new EntityClaims();

    private static Claims instance;

    private File claimsFile;
    FileConfiguration claimsConfig;

    final List<UUID> hasMessaged = new ArrayList<>();
    final List<UUID> hasMessagedTM = new ArrayList<>();

    public Claims() {
        instance = this;
    }

    @Override
    public void setup() {

        Config.addConfig(new Config.BoolConfigOption("land-claims.enabled", "land-claims.enabled", true, "kamstweaks.configure"));
        Config.addConfig(new Config.IntConfigOption("land-claims.max-claims", "land-claims.max-claims", 30, "kamstweaks.configure"));
        Config.addConfig(new Config.IntConfigOption("land-claims.max-claim-size", "land-claims.max-claim-size", 50000, "kamstweaks.configure"));

        landClaims.setup(this);
        entityClaims.setup(this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), () -> {
            hasMessaged.clear();
            hasMessagedTM.clear();
        }, 1, 1);
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        var claimCmd = Commands.literal("claims").executes(ctx -> {
            if (!Config.getBool("land-claims.enabled", true) && !Config.getBool("entity-claims.enabled", true)) {
                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.CLAIMS)));
                return Command.SINGLE_SUCCESS;
            }
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                new Homepage(player).show();

                if (player != sender) {
                    sender.sendMessage(KTStrings.getFor(KTStrings.CLAIMS_SHOWED_GUI_TO, Names.getName(player)).color(NamedTextColor.GOLD));
                }
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/claims")).color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        });

        var getTool = Commands.literal("get-tool").executes(ctx -> {
            if (!Config.getBool("land-claims.enabled", true) && !Config.getBool("entity-claims.enabled", true)) {
                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.CLAIMS)));
                return Command.SINGLE_SUCCESS;
            }
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                player.getInventory().addItem(KTItems.createItem(KTItems.ItemType.CLAIM_TOOL));
                if (player == sender) {
                    sender.sendMessage(KTStrings.getFor(KTStrings.CLAIM_TOOL_HINT).color(NamedTextColor.GOLD));
                } else {
                    sender.sendMessage(KTStrings.getFor(KTStrings.CLAIMS_GAVE_TOOL_TO, Names.getName(player)).color(NamedTextColor.GOLD));
                }
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/claims get-tool")).color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        });

        landClaims.registerCommands(commands, claimCmd);
        entityClaims.registerCommands(commands, claimCmd);

        claimCmd.then(getTool);
        commands.registrar().register(claimCmd.build());
    }

    @Override
    public void loadData() {
        setupFile();
        landClaims.load();
        entityClaims.load();
    }

    @Override
    public void saveData() {
        setupFile();
        landClaims.save();
        entityClaims.save();

        // it wasnt removing these before
        claimsConfig.set("claims", null);
        claimsConfig.set("entities", null);

        try {
            claimsConfig.save(claimsFile);
        } catch (Exception e) {
            Logger.handleException(e);
        }
    }

    private void setupFile() {
        claimsFile = new File(KamsTweaks.get().getDataFolder(), "claims.yml");
        if (!claimsFile.exists()) {
            boolean ignored = claimsFile.getParentFile().mkdirs();
            KamsTweaks.get().saveResource("claims.yml", false);
        }
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
    }

    public boolean useClaimTool(PlayerInteractEvent event) {
        var plr = event.getPlayer();
        assert event.getItem() != null;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            assert event.getClickedBlock() != null;
            landClaims.handleTool(plr, event.getClickedBlock().getLocation());
            return true;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            assert event.getClickedBlock() != null;
            var claim = landClaims.getClaim(event.getClickedBlock().getLocation());
            if (claim == null) {
                plr.sendMessage(KTStrings.getFor(KTStrings.LC_UNCLAIMED));
            } else {
                plr.sendMessage(KTStrings.getFor(KTStrings.LC_OWNED_BY, claim.getOwnerName()));
            }
            return true;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            new Homepage(plr).show();
            return true;
        }
        return false;
    }

    // just a helper cause its nicer
    public void message(Entity player, Component message) {
        if (!(player instanceof Player)) return;
        if (hasMessaged.contains(player.getUniqueId()))
            return;
        hasMessaged.add(player.getUniqueId());
        player.sendActionBar(message);
    }

    public void messageTest(Entity player) {
        if (!(player instanceof Player)) return;
        if (hasMessagedTM.contains(player.getUniqueId()))
            return;
        hasMessagedTM.add(player.getUniqueId());
        player.sendMessage(KTStrings.getFor(KTStrings.PERMS_TEST_MODE_HINT));
    }

    public enum OptBool {
        True,
        False,
        Default,
    }

    public enum ManagementType {
        Owner,
        Trusted,
        Op,
        None
    }

    public static Claims get() {
        return instance;
    }
}
