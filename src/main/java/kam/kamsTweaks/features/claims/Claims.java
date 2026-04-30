package kam.kamsTweaks.features.claims;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Names;
import kam.kamsTweaks.features.claims.gui.FLAlertLayer;
import kam.kamsTweaks.features.claims.gui.Homepage;
import kam.kamsTweaks.features.claims.gui.LandClaimPage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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
    public ClaimProtections protections = new ClaimProtections();
    public LandClaims landClaims = new LandClaims();
    public EntityClaims entityClaims = new EntityClaims();

    private static Claims instance;

    private File claimsFile;
    FileConfiguration claimsConfig;

    final List<UUID> hasMessaged = new ArrayList<>();

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
        var claimCmd = Commands.literal("claims").executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                new Homepage(player).show();

                if (player != sender) {
                    sender.sendMessage(Component.text("Showed claims gui to ").append(Names.instance.getRenderedName(player), Component.text(".")).color(NamedTextColor.GOLD));
                }
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        });

        var getTool = Commands.literal("get-tool").executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                player.getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.CLAIM_TOOL));
                if (player == sender) {
                    sender.sendMessage(Component.text("Right click to use the claim tool.").color(NamedTextColor.GOLD));
                } else {
                    sender.sendMessage(Component.text("Gave claim tool to ").append(Names.instance.getRenderedName(player), Component.text(".")).color(NamedTextColor.GOLD));
                }
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        });

        landClaims.registerCommands(commands, claimCmd);

        claimCmd.then(getTool);
        commands.registrar().register(claimCmd.build());
    }

    @Override
    public void loadData() {

    }

    @Override
    public void saveData() {

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
        if (event.getItem().getPersistentDataContainer().has(ItemManager.ItemTag.YUMMY.key)) {
            if (event.getAction() != Action.RIGHT_CLICK_AIR) event.setCancelled(true);
            return true;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            assert event.getClickedBlock() != null;
            landClaims.handleTool(plr, event.getClickedBlock().getLocation());
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

    public enum OptBool {
        TRUE,
        FALSE,
        DEFAULT,
    }

    public static Claims get() {
        return instance;
    }
}
