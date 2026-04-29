package kam.kamsTweaks.features.claims;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Names;
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
            var loc = event.getClickedBlock().getLocation();
            if (landClaims.currentlyClaiming.containsKey(plr)) {
                var claim = landClaims.currentlyClaiming.get(plr);
                if (claim.start == null) {
                    var blOwner = LandClaims.getBlockLockerOwner(event.getPlayer(), loc, loc);
                    if (blOwner != null) {
                        event.getPlayer().sendMessage(Component.text("This is a locked block owned by ")
                                .append(Names.instance.getRenderedName(blOwner), Component.text(".")).color(NamedTextColor.RED));
                        return true;
                    }
                    for (var c : landClaims.claims) {
                        if (c.inBounds(loc)) {
                            if (c.owner == null || (!c.owner.getUniqueId().equals(event.getPlayer().getUniqueId()))) {
                                Component name;
                                if (c.owner == null) {
                                    name = Component.text("the server").color(NamedTextColor.GOLD);
                                } else {
                                    name = Names.instance.getRenderedName(c.owner);
                                }
                                event.getPlayer().sendMessage(Component.text("This land is already claimed by ")
                                        .append(name, Component.text(".")).color(NamedTextColor.RED));
                                return true;
                            }
                        }
                    }
                    claim.start = loc;
                    event.getPlayer()
                            .sendMessage(Component.text("Now click the other corner with your claim tool. (If you lost it, run ")
                                    .append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool"))
                                            .color(NamedTextColor.YELLOW))
                                    .append(Component.text(")").color(NamedTextColor.BLUE)).color(NamedTextColor.BLUE));
                } else {
                    if (claim.start.getWorld() != loc.getWorld()) {
                        event.getPlayer().sendMessage(
                                Component.text("You can't claim across dimensions - go back to the dimension you started in!")
                                        .color(NamedTextColor.RED));
                        return true;
                    }
                    plr.sendMessage(Component.text("Running claim checks asynchronously to prevent server lag. Depending on the claim size this may take a minute."));
                    Bukkit.getAsyncScheduler().runNow(KamsTweaks.get(), task -> {
                        var maxArea = KamsTweaks.get().getConfig().getInt("land-claims.max-claim-size", 50000);
                        var has = Math.abs(claim.start.x() - loc.x()) * Math.abs(claim.start.y() - loc.y())
                                * Math.abs(claim.start.z() - loc.z());
                        var value = Math.ceil(has / maxArea);
                        Logger.info("Z");
                        if (value > 1) {
                            Logger.info("T");
                            var total = 0;
                            for (var c : landClaims.claims) {
                                if (c.owner != null && c.owner.getUniqueId() == plr.getUniqueId()) total++;
                            }
                            var maxCt = KamsTweaks.get().getConfig().getInt("land-claims.max-claims", 30);
                            if (total + value >= maxCt) {
                                var diff = (value + total - maxCt);
                                event.getPlayer().sendMessage(Component.text("You can't claim more than " + maxArea + " blocks in an unextended claim - you are trying to claim " + has + ". You need " + diff + " more unused claim" + (diff == 1 ? "" : "s") + " for an extension (costs " + value + ").").color(NamedTextColor.RED));
                            } else {
                                Logger.info("Test");
                            }
                            return;
                        }
                        var blOwner = LandClaims.getBlockLockerOwner(event.getPlayer(), claim.start, loc);
                        if (blOwner != null) {
                            event.getPlayer().sendMessage(Component.text("This land contains a locked block owned by ")
                                    .append(Names.instance.getRenderedName(blOwner), Component.text(".")).color(NamedTextColor.RED));
                            return;
                        }
                        claim.end = loc;
                        for (var other : landClaims.claims) {
                            if (claim.intersects(other)) {
                                if (other.owner == null || !other.owner.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                                    Component name;
                                    if (other.owner == null) {
                                        name = Component.text("the server").color(NamedTextColor.GOLD);
                                    } else {
                                        name = Names.instance.getRenderedName(other.owner);
                                    }
                                    event.getPlayer().sendMessage(Component.text("This land intersects a claim by ")
                                            .append(name, Component.text(".")).color(NamedTextColor.RED));
                                    return;
                                } else if (other.owner.getUniqueId().equals(event.getPlayer().getUniqueId())
                                        && other.config.priority >= claim.config.priority) {
                                    claim.config.priority = other.config.priority + 1;
                                }
                            }
                        }
                        landClaims.claims.add(claim);
                        landClaims.currentlyClaiming.remove(event.getPlayer());
                        event.getPlayer().sendMessage(Component.text("Territory claimed (").color(NamedTextColor.GREEN).append(
                                Component.text(claim.id).color(NamedTextColor.GOLD), Component.text(")").color(NamedTextColor.GREEN)));
                    });
                    return true;
                }

                return true;
            }
            new LandClaimPage(event.getPlayer(), landClaims.getClaim(event.getClickedBlock().getLocation())).show();
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            new Homepage(event.getPlayer()).show();
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
