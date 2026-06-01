package kam.kamsTweaks.features.claims;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Names;
import kam.kamsTweaks.features.claims.gui.FLAlertLayer;
import kam.kamsTweaks.features.claims.gui.LandClaimPage;
import kam.kamsTweaks.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import kam.kamsTweaks.features.claims.Claims.OptBool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class LandClaims implements Listener {
    public List<LandClaim> claims = new ArrayList<>();

    // for stuff like dragon fight
    public Map<World, Integer> disabled = new HashMap<>();
    public Map<Player, LandClaim> currentlyClaiming = new HashMap<>();

    Claims instance;
    public LandProtections prots = new LandProtections();

    public void setup(Claims instance) {
        this.instance = instance;

        prots.setup(this);

        Bukkit.getServer().getPluginManager().registerEvents(this, KamsTweaks.get());
        Bukkit.getServer().getPluginManager().registerEvents(prots, KamsTweaks.get());

        Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), () -> {
            for (var world : disabled.keySet()) {
                var val = disabled.get(world) - 1;
                if (val == 0) {
                    disabled.remove(world);
                    for (var plr : world.getPlayers()) {
                        plr.sendMessage(Component.text("Claims are now re-enabled in this dimension.").color(NamedTextColor.GREEN));
                    }
                } else {
                    disabled.put(world, val);
                }
            }
        }, 20, 20);
    }

    void savePerms(FileConfiguration config, String path, Permissions perms) {
        if (perms.who != null) config.set(path + ".who", perms.who.toString());
        perms.bools.forEach((perm, val) -> config.set(path + ".bools." + perm.name(), val.name()));
        perms.advancedBools.forEach((perm, val) -> config.set(path + ".advbools." + perm.name(), val.name()));
    }

    public void save() {
        var cfg = Claims.get().claimsConfig;
//        cfg.set("claims", null);
        var primary = Bukkit.getWorlds().getFirst().getUID();
        cfg.set(primary + ".landv3", null);

        for (var claim : claims) {
            var path = primary + ".landv3." + claim.id;
            cfg.set(path + ".id", claim.id);
            if (claim.owner != null) cfg.set(path + ".owner", claim.owner.getUniqueId().toString());
            cfg.set(path + ".start", LocationUtils.serializeBlockPos(claim.start));
            cfg.set(path + ".end", LocationUtils.serializeBlockPos(claim.end));
            cfg.set(path + ".slots", claim.slots);

            cfg.set(path + ".config.name", claim.config.name);
            cfg.set(path + ".config.prio", claim.config.priority);
//            cfg.set(path + ".config.grass-spread", claim.config.grassSpread.name());
//            cfg.set(path + ".config.fire-spread", claim.config.fireSpread.name());
//            cfg.set(path + ".config.water-flow", claim.config.waterFlow.name());
//            cfg.set(path + ".config.hopper-transfer", claim.config.hopperTransfer.name());
//            cfg.set(path + ".config.gravity", claim.config.gravity.name());
//            cfg.set(path + ".config.trees-grow", claim.config.treesGrow.name());

            savePerms(cfg, path + ".default", claim.defaultPerms);
            savePerms(cfg, path + ".entity", claim.defaultEntityPerms);
            claim.perms.forEach((uuid, perms) -> savePerms(cfg, path + ".perms." + uuid, perms));
        }
    }

    // I got tired of typing the long thing
    public static <T> @NotNull T nonNull(@Nullable T t) {
        return Objects.requireNonNull(t);
    }

    void loadPerms(FileConfiguration config, String path, Permissions perms) {
        try {
            var who = config.getString(path + ".who");
            if (who != null) perms.who = UUID.fromString(who);
            if (config.contains(path + ".bools")) {
                for (var ps : nonNull(config.getConfigurationSection(path  + ".bools")).getKeys(false)) {
                    var perm = LandPermission.valueOf(ps);
                    perms.bools.put(perm, OptBool.valueOf(config.getString(path + ".bools." + ps)));
                }
            }
            if (config.contains(path + ".advbools")) {
                for (var ps : nonNull(config.getConfigurationSection(path  + ".advbools")).getKeys(false)) {
                    var perm = AdvancedLandPermission.valueOf(ps);
                    perms.advancedBools.put(perm, OptBool.valueOf(config.getString(path + ".advbools." + ps)));
                }
            }
        } catch (Exception e) {
            Logger.error("Failed loading perms from " + path + ".");
            Logger.handleException(e);
        }
    }

    public void load() {
        claims.clear();
        var cfg = Claims.get().claimsConfig;
        var primary = Bukkit.getWorlds().getFirst().getUID();
        if (cfg.contains(primary + ".landv3")) {
            for (var key : nonNull(cfg.getConfigurationSection(primary + ".landv3")).getKeys(false)) {
                try {
                    var path = primary + ".landv3." + key;
                    var uuid = cfg.contains(path + ".owner") ? UUID.fromString(nonNull(cfg.getString(path + ".owner"))) : null;
                    var id = cfg.getInt(path + ".id");
                    var start = LocationUtils.deserializeBlockPos(nonNull(cfg.getString(path + ".start")));
                    var end = LocationUtils.deserializeBlockPos(nonNull(cfg.getString(path + ".end")));
                    var claim = new LandClaim(uuid != null ? Bukkit.getOfflinePlayer(uuid) : null, id, start, end);
                    claim.slots = cfg.getInt(path + ".slots");

                    claim.config.name = nonNull(cfg.getString(path + ".config.name"));
                    claim.config.priority = cfg.getInt(path + ".config.priority");
//                    claim.config.grassSpread = ConfigEnum1.valueOf(nonNull(cfg.getString(path + ".config.grass-spread")));
//                    claim.config.fireSpread = ConfigEnum1.valueOf(nonNull(cfg.getString(path + ".config.fire-spread")));
//                    claim.config.waterFlow = ConfigEnum1.valueOf(nonNull(cfg.getString(path + ".config.water-flow")));
//                    claim.config.hopperTransfer = ConfigEnum1.valueOf(nonNull(cfg.getString(path + ".config.hopper-transfer")));
//                    claim.config.gravity = ConfigEnum1.valueOf(nonNull(cfg.getString(path + ".config.gravity")));
//                    claim.config.treesGrow = ConfigEnum1.valueOf(nonNull(cfg.getString(path + ".config.trees-grow")));

                    loadPerms(cfg, path + ".default", claim.defaultPerms);
                    loadPerms(cfg, path + ".entity", claim.defaultEntityPerms);
                    if (cfg.contains(path + ".perms")) {
                        for (var perm : nonNull(cfg.getConfigurationSection(path + ".perms")).getKeys(false)) {
                            loadPerms(cfg, path + ".perms." + uuid, claim.getPerms(UUID.fromString(perm + ".who")));
                        }
                    }

                    claims.add(claim);
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
        loadLegacy();
    }

    public void loadLegacy() {
        var cfg = Claims.get().claimsConfig;
        // V2 syntax
        if (cfg.contains("claims")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("claims")).getKeys(false)) {
                try {
                    String ownerStr = cfg.getString("claims." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    String corner1Str = cfg.getString("claims." + key + ".corner1");
                    assert corner1Str != null;
                    Location corner1 = LocationUtils.deserializeBlockPos(corner1Str);
                    if (corner1.getWorld() == null) continue;
                    String corner2Str = cfg.getString("claims." + key + ".corner2");
                    assert corner2Str != null;
                    Location corner2 = LocationUtils.deserializeBlockPos(corner2Str);
                    LandClaim claim;
                    if (cfg.contains("claims." + key + ".id")) {
                        claim = new LandClaim(owner == null ? null : Bukkit.getServer().getOfflinePlayer(owner), cfg.getInt("claims." + key + ".id"), corner1, corner2);
                    } else {
                        claim = new LandClaim(owner == null ? null : Bukkit.getServer().getOfflinePlayer(owner), corner1, corner2);
                    }
                    if (cfg.contains("claims." + key + ".name")) {
                        claim.config.name = cfg.getString("claims." + key + ".name");
                    }
                    if (cfg.contains("claims." + key + ".prio")) {
                        claim.config.priority = cfg.getInt("claims." + key + ".prio");
                    }
                    try {
                        if (cfg.contains("claims." + key + ".defaults")) {
                            claim.defaultPerms.bools.put(LandPermission.DOOR_INTERACT, OptBool.False);
                            for (String def : Objects.requireNonNull(cfg.getStringList("claims." + key + ".defaults"))) {
                                switch (def) {
                                    case "INTERACT_DOOR":  {
                                        claim.defaultPerms.bools.put(LandPermission.DOOR_INTERACT, OptBool.True);
                                        break;
                                    }
                                    case "INTERACT_BLOCK":  {
                                        claim.defaultPerms.bools.put(LandPermission.BLOCK_INTERACT, OptBool.True);
                                        claim.defaultPerms.bools.put(LandPermission.DOOR_INTERACT, OptBool.True);
                                        break;
                                    }
                                    case "BLOCK_BREAK":  {
                                        claim.defaultPerms.bools.put(LandPermission.BLOCK_BREAK, OptBool.True);
                                        break;
                                    }
                                    case "BLOCK_PLACE":  {
                                        claim.defaultPerms.bools.put(LandPermission.BLOCK_PLACE, OptBool.True);
                                        break;
                                    }
                                }
                                claim.defaultEntityPerms = claim.defaultPerms.clone();
                            }
                        }
                    } catch (NullPointerException e) {
                        Logger.exceptions.add(e);
                        Logger.warn(e.getMessage());
                        claim.defaultPerms.bools = new HashMap<>();
                    }

                    try {
                        if (cfg.contains("claims." + key + ".permissions")) {
                            for (String uuidStr : Objects.requireNonNull(cfg.getConfigurationSection("claims." + key + ".permissions")).getKeys(false)) {
                                var uuid = UUID.fromString(uuidStr);
                                var perms = claim.getPerms(uuid);
                                perms.bools.put(LandPermission.DOOR_INTERACT, OptBool.False);
                                for (String def : Objects.requireNonNull(cfg.getStringList("claims." + key + ".permissions." + uuidStr))) {
                                    switch (def) {
                                        case "INTERACT_DOOR":  {
                                            perms.bools.put(LandPermission.DOOR_INTERACT, OptBool.True);
                                            break;
                                        }
                                        case "INTERACT_BLOCK":  {
                                            perms.bools.put(LandPermission.BLOCK_INTERACT, OptBool.True);
                                            perms.bools.put(LandPermission.DOOR_INTERACT, OptBool.True);
                                            break;
                                        }
                                        case "BLOCK_BREAK":  {
                                            perms.bools.put(LandPermission.BLOCK_BREAK, OptBool.True);
                                            break;
                                        }
                                        case "BLOCK_PLACE":  {
                                            perms.bools.put(LandPermission.BLOCK_PLACE, OptBool.True);
                                            break;
                                        }
                                    }
                                    claim.defaultEntityPerms = claim.defaultPerms.clone();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.exceptions.add(e);
                        Logger.warn(e.getMessage());
                    }
                    claims.add(claim);
                } catch (Exception e) {
                    Logger.exceptions.add(e);
                    Logger.warn(e.getMessage());
                }
            }
        }
        cfg.set("claims", null);
    }

    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands, LiteralArgumentBuilder<CommandSourceStack> baseCmd) {
        Command<CommandSourceStack> bcb = ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                new LandClaimPage(player).show();

                if (player != sender) {
                    sender.sendMessage(Component.text("Showed land claims gui to ").append(Names.instance.getRenderedName(player), Component.text(".")).color(NamedTextColor.GOLD));
                }
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        };

        var land = Commands.literal("lc").executes(bcb);
        var baseLand = Commands.literal("land").executes(bcb);

        List<LiteralArgumentBuilder<CommandSourceStack>> cmdList = new ArrayList<>();

        var create = Commands.literal("create").executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor != sender) {
                sender.sendMessage(Component.text("You can't start claiming for someone else.").color(NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }
            if (executor instanceof Player player) {
                startClaiming(player, true);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        });
        cmdList.add(create);

        var delete = Commands.literal("delete").then(Commands.argument("id", IntegerArgumentType.integer()).suggests((ctx, builder) -> {
            if (!(ctx.getSource().getSender() instanceof Player player))
                return builder.buildFuture();
            for (var claim : claims) {
                if (claim.owner != null && player.getUniqueId().equals(claim.owner.getUniqueId())) {
                    builder.suggest(claim.id);
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            var id = ctx.getArgument("id", Integer.class);
            if (executor instanceof Player player) {
                var claim = getClaim(id);
                if (claim == null) {
                    sender.sendMessage(Component.text("This claim doesn't exist.").color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                deleteClaim(claim, player);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }));
        cmdList.add(delete);

        create.then(Commands.argument("pos1", ArgumentTypes.blockPosition()).then(Commands.argument("pos2", ArgumentTypes.blockPosition()).executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor != sender) {
                sender.sendMessage(Component.text("You can't create claims for someone else.").color(NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }
            if (executor instanceof Player player) {
                if (!startClaiming(player, false)) return Command.SINGLE_SUCCESS;
                var claim = currentlyClaiming.get(player);
                var pos1 = ctx.getArgument("pos1", BlockPositionResolver.class).resolve(ctx.getSource());
                var pos2 = ctx.getArgument("pos2", BlockPositionResolver.class).resolve(ctx.getSource());
                if (!setCorner1(player, claim, pos1.toLocation(player.getWorld()), false)) return Command.SINGLE_SUCCESS;
                if (!setCorner2(player, claim, pos2.toLocation(claim.start.getWorld()))) return Command.SINGLE_SUCCESS;

                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        })));

        cmdList.add(Commands.literal("cancel").executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor != sender) {
                sender.sendMessage(Component.text("You can't stop claiming for someone else.").color(NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }
            if (executor instanceof Player player) {
                stopClaiming(player);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }));

        cmdList.add(Commands.literal("list").executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                listClaims(player, sender);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }));

        cmdList.add(Commands.literal("view").executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                Claims.get().landClaims.showClaims(player);
                if (sender == player) {
                    sender.sendMessage(Component.text("Nearby claims are being highlighted."));
                } else {
                    sender.sendMessage(Component.text("Highlighting nearby claims for ").append(Names.instance.getRenderedName(player), Component.text(".")));
                }
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }));


        for (var thing : cmdList) {
            land.then(thing);
            baseLand.then(thing);
        }

        baseCmd.then(baseLand);
        commands.registrar().register(land.build());
    }

    public @Nullable LandClaim getClaim(Location where) {
        return getClaim(where, false);
    }

    public @Nullable LandClaim getClaim(Location where, boolean ignoresWorldDisable) {
        if (!ignoresWorldDisable && ((where.getWorld().getEnderDragonBattle() != null && where.getWorld().getEnderDragonBattle().getEnderDragon() != null) || disabled.containsKey(where.getWorld()))) {
            if (where.distance(new Location(where.getWorld(),0.f, where.y(), 0.f)) < 200) return null;
        }
        LandClaims.LandClaim ret = null;
        for (var claim : claims) {
            if (claim.inBounds(where)) {
                if (ret == null || claim.config.priority > ret.config.priority) {
                    ret = claim;
                }
            }
        }
        return ret;
    }

    public @Nullable LandClaim getClaim(int id) {
        return claims.stream().filter(carnet -> id == carnet.id).findFirst().orElse(null);
    }

    public boolean startClaiming(Player who, boolean sMessage) {
        if (currentlyClaiming.containsKey(who)) {
            who.sendMessage(Component.text("You're already claiming land. (run ").append(Component.text("/claims land cancel").clickEvent(ClickEvent.runCommand("claims land cancel")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(" to cancel)")).color(NamedTextColor.RED));
        } else {
            var total = 0;
            for (var c : claims) {
                if (c.owner != null && c.owner.getUniqueId() == who.getUniqueId()) total += c.slots;
            }
            if (total + 1 > KamsTweaks.get().getConfig().getInt("land-claims.max-claims", 30)) {
                who.sendMessage(Component.text("You have used all of your claim slots. Delete some to free up slots.").color(NamedTextColor.RED));
                return false;
            }
            if (sMessage) who.sendMessage(Component.text("Right click the first corner of where you want to claim with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(")")).color(NamedTextColor.GOLD));
            currentlyClaiming.put(who, new LandClaim(who, null, null));
        }
        return true;
    }

    public void stopClaiming(Player who) {
        if (currentlyClaiming.containsKey(who)) {
            who.sendMessage(Component.text("Stopped claiming.").color(NamedTextColor.GOLD));
            currentlyClaiming.remove(who);
        } else {
            who.sendMessage(Component.text("You aren't currently claiming land. (run ").append(Component.text("/claims land create").clickEvent(ClickEvent.runCommand("claims land cancel")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(" or use the claim tool to start)")).color(NamedTextColor.RED));
        }
    }

    public boolean setCorner1(Player who, LandClaim claim, Location loc, boolean sMessage) {
        for (var c : claims) {
            if (c.inBounds(loc)) {
                if (c.owner == null || (!c.owner.getUniqueId().equals(who.getUniqueId()))) {
                    Component name;
                    if (c.owner == null) {
                        name = Component.text("the server").color(NamedTextColor.GOLD);
                    } else {
                        name = Names.instance.getRenderedName(c.owner);
                    }
                    who.sendMessage(Component.text("This land is already claimed by ").append(name, Component.text(".")).color(NamedTextColor.RED));
                    return false;
                }
            }
        }
        claim.start = loc;
        if (sMessage) who.sendMessage(Component.text("Now click the other corner with your claim tool. (If you lost it, run ").append(
                Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW),
                Component.text(")").color(NamedTextColor.BLUE)).color(NamedTextColor.BLUE));
        return true;
    }

    public boolean setCorner2(Player who, LandClaim claim, Location loc) {
        var maxArea = KamsTweaks.get().getConfig().getInt("land-claims.max-claim-size", 50000);
        int has = (Math.abs(claim.start.getBlockX() - loc.getBlockX()) + 1) * (Math.abs(claim.start.getBlockY() - loc.getBlockY()) + 1)
                * (Math.abs(claim.start.getBlockZ() - loc.getBlockZ()) + 1);
        int value = (int) Math.ceil((double) has / maxArea);
        claim.slots = value;
        var total = 0;
        for (var c : claims) {
            if (c.owner != null && c.owner.getUniqueId() == who.getUniqueId()) total += c.slots;
        }
        var maxCt = KamsTweaks.get().getConfig().getInt("land-claims.max-claims", 30);
        if (value > 1) {
            if (total + value > maxCt) {
                var diff = (value + total - maxCt);
                who.sendMessage(Component.text("You can't claim more than " + maxArea + " blocks in an unextended claim, while you are trying to claim " + has + ". You need " + diff + " more claim slot" + (diff == 1 ? "" : "s") + " for an extension (costs " + value + ").").color(NamedTextColor.RED));
                return false;
            }
            new FLAlertLayer(who, Component.text("Extend claim?"),
                    Component.text("This claim is larger than " + maxArea + " blocks. Do you want to use " + (value - 1) + " extra claim slot" + (value == 2 ? "" : "s") + " to extend it?"),
                    Component.text("Yes").color(NamedTextColor.GREEN), Component.text("No").color(NamedTextColor.RED), second -> {
                if (!second) {
                    finishClaiming(who, claim, loc);
                }
            }).show();
        } else if (total + 1 > maxCt) {
            who.sendMessage(Component.text("You have used all of your claim slots. Delete some to free up slots.").color(NamedTextColor.RED));
            return false;
        }
        finishClaiming(who, claim, loc);
        return true;
    }

    public void finishClaiming(Player who, LandClaim claim, Location loc) {
        claim.end = loc;
        for (var other : claims) {
            if (claim.intersects(other)) {
                if (other.owner == null || !other.owner.getUniqueId().equals(who.getUniqueId())) {
                    Component name;
                    if (other.owner == null) {
                        name = Component.text("the server").color(NamedTextColor.GOLD);
                    } else {
                        name = Names.instance.getRenderedName(other.owner);
                    }
                    who.sendMessage(Component.text("This land intersects a claim by ")
                            .append(name, Component.text(".")).color(NamedTextColor.RED));
                    return;
                } else if (other.owner.getUniqueId().equals(who.getUniqueId())
                        && other.config.priority >= claim.config.priority) {
                    claim.config.priority = other.config.priority + 1;
                }
            }
        }
        claims.add(claim);
        currentlyClaiming.remove(who);
        who.sendMessage(Component.text("Territory claimed (").color(NamedTextColor.GREEN).append(
                Component.text(claim.id).color(NamedTextColor.GOLD), Component.text(")").color(NamedTextColor.GREEN)));
    }

    public void handleTool(Player plr, Location loc) {
        if (currentlyClaiming.containsKey(plr)) {
            var claim = currentlyClaiming.get(plr);
            if (claim.start == null) {
                setCorner1(plr, claim, loc, true);
            } else {
                if (claim.start.getWorld() != loc.getWorld()) {
                    plr.sendMessage(
                            Component.text("You can't claim across dimensions - go back to the dimension you started in!")
                                    .color(NamedTextColor.RED));
                    return;
                }
                setCorner2(plr, claim, loc);
            }

            return;
        }
        new LandClaimPage(plr, getClaim(loc)).show();
    }

    public void deleteClaim(LandClaim claim, Player who) {
        var mt = claim.getManagementType(who);
        if (mt == Claims.ManagementType.None) {
            who.sendMessage(Component.text("You cannot manage this claim.").color(NamedTextColor.RED));
            return;
        } else if (mt == Claims.ManagementType.Op) {
            KamsTweaks.get().sendToOps(Component.text("[" + who.getName() + ": Deleted " + claim.getOwnerUsername() + "'s land claim]").decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), who);
            Logger.warn("[Claim management] " + who.getName() + " just deleted " + claim.getOwnerUsername() + "'s land claim.");
        }
        claims.remove(claim);
        who.sendMessage(Component.text("Deleted claim successfully.").color(NamedTextColor.GREEN));
    }

    public enum LandPermission {
        DOOR_INTERACT("Interact with Doors", OptBool.True),
        BLOCK_INTERACT("Interact with Blocks"),
        BLOCK_BREAK("Break Blocks"),
        BLOCK_PLACE("Place Blocks"),

        ;

        public final String label;
        public final OptBool defaultValue;

        LandPermission(String label, OptBool defaultValue) {
            this.label = label;
            this.defaultValue = defaultValue;
        }
        LandPermission(String label) {
            this.label = label;
            this.defaultValue = OptBool.False;
        }
    }

    public enum AdvancedLandPermission {
        LECTERN_INSERT("Place into Lecterns"),
        LECTERN_TAKE("Take from Lecterns"),
        DRAIN_CAULDRON("Drain Cauldrons"),
        DAMAGE_ANVIL("Damage Anvils"),

        EMPTY_BUCKETS("Empty Buckets"),
        FILL_BUCKETS("Fill Buckets"),

        ITEM_FRAME_ITEM_ROTATE("Rotate Items in Frames"),
        ITEM_FRAME_ITEM_TAKE("Take Items from Frames"),
        ITEM_FRAME_ITEM_PLACE("Place Items into Frames"),
        ARMOR_STAND_ITEM_TAKE("Take from Armor Stands"),
        ARMOR_STAND_ITEM_PLACE("Place onto Armor Stands"),



        ;

        public final String label;
        public final OptBool defaultValue;

        AdvancedLandPermission(String label, OptBool defaultValue) {
            this.label = label;
            this.defaultValue = defaultValue;
        }
        AdvancedLandPermission(String label) {
            this.label = label;
            this.defaultValue = OptBool.False;
        }
    }

    public static class Permissions implements Cloneable {
        public LandClaim claim;
        public UUID who;
        boolean isClaimDefault = false;
        Map<LandPermission, OptBool> bools = new HashMap<>();
        Map<AdvancedLandPermission, OptBool> advancedBools = new HashMap<>();

        public Permissions(LandClaim claim, UUID who) {
            this.claim = claim;
            this.who = who;
        }

        public Permissions(LandClaim claim, boolean isClaimDefault) {
            this.claim = claim;
            this.isClaimDefault = isClaimDefault;
        }

        public OptBool getBoolPermission(LandPermission perm) {
            if (isClaimDefault) return bools.getOrDefault(perm, perm.defaultValue);
            return bools.getOrDefault(perm, OptBool.Default);
        }

        public OptBool getBoolPermission(LandPermission perm, Permissions backup) {
            if (isClaimDefault) return bools.getOrDefault(perm, perm.defaultValue);
            var ret = bools.getOrDefault(perm, OptBool.Default);
            if (backup != null && ret == OptBool.Default) ret = backup.getBoolPermission(perm);
            return ret;
        }

        public OptBool getBoolPermission(AdvancedLandPermission perm) {
            if (isClaimDefault) return advancedBools.getOrDefault(perm, perm.defaultValue);
            return advancedBools.getOrDefault(perm, OptBool.Default);
        }

        public OptBool getBoolPermission(AdvancedLandPermission perm, Permissions backup) {
            if (isClaimDefault) return advancedBools.getOrDefault(perm, perm.defaultValue);
            var ret = advancedBools.getOrDefault(perm, OptBool.Default);
            if (backup != null && ret == OptBool.Default) ret = backup.getBoolPermission(perm);
            return ret;
        }

        public void setBoolPermission(LandPermission perm, OptBool value) {
            bools.put(perm, value);
        }

        public void setBoolPermission(AdvancedLandPermission perm, OptBool value) {
            advancedBools.put(perm, value);
        }

        @Override
        public Permissions clone() {
            try {
                return (Permissions) super.clone();
            } catch (Exception e) {
                Logger.handleException(e);
                return null;
            }
        }
    }

//    public enum ConfigEnum1 {
//        None("None"),
//        FromInside("From Inside Claim"),
//        All("All");
//
//        public final String label;
//        ConfigEnum1(String label) {
//            this.label = label;
//        }
//    }

    public static class ClaimConfig {
        public String name = "Unnamed Claim";
        public Integer priority = 0;

//        // TODO: these
//        public ConfigEnum1 grassSpread = ConfigEnum1.FromInside;
//        public ConfigEnum1 fireSpread = ConfigEnum1.FromInside;
//        public ConfigEnum1 waterFlow = ConfigEnum1.FromInside;
//        public ConfigEnum1 hopperTransfer = ConfigEnum1.FromInside; // from outside
//        public ConfigEnum1 gravity = ConfigEnum1.FromInside;
//        public ConfigEnum1 treesGrow = ConfigEnum1.FromInside;
    }

    public static class LandClaim {
        public @Nullable OfflinePlayer owner;
        public static int nextId;
        public int id;

        public Location start;
        public Location end;
        public int slots = 0;

        public ClaimConfig config = new ClaimConfig();

        public Map<UUID, Permissions> perms = new HashMap<>();
        public Permissions defaultPerms = new Permissions(this, true).clone();
        public Permissions defaultEntityPerms = new Permissions(this, null).clone();

        public Permissions getPerms(UUID who) {
            if (!perms.containsKey(who)) {
                var p = new Permissions(this, who);
                perms.put(who, p);
                return p;
            }
            return perms.get(who);
        }

        public LandClaim(@Nullable OfflinePlayer owner, Location start, Location end) {
            this.id = nextId;
            nextId++;
            this.owner = owner;
            this.start = start;
            this.end = end;
        }

        public LandClaim(@Nullable OfflinePlayer owner, int id, Location start, Location end) {
            nextId = Math.max(nextId, id + 1);
            this.id = id;
            this.owner = owner;
            this.start = start;
            this.end = end;
        }

        public Location getMin() {
            return new Location(start.getWorld(), Math.min(start.x(), end.x()), Math.min(start.y(), end.y()), Math.min(start.z(), end.z()));
        }

        public Location getMax() {
            return new Location(start.getWorld(), Math.max(start.x(), end.x()), Math.max(start.y(), end.y()), Math.max(start.z(), end.z()));
        }

        public Vector3f getSize() {
            var max = getMax();
            var min = getMin();
            return new Vector3f((float) Math.abs(max.getBlockX() - min.getBlockX()), (float) Math.abs(max.getBlockY() - min.getBlockY()), (float) Math.abs(max.getBlockZ() - min.getBlockZ()));
        }

        public Claims.ManagementType getManagementType(Player who) {
            if (owner != null && who.getUniqueId().equals(owner.getUniqueId())) return Claims.ManagementType.Owner;
            if (who.hasPermission("kamstweaks.claims.manage")) return Claims.ManagementType.Op;
            return Claims.ManagementType.None;
        }

        public boolean hasPermission(Object who, LandPermission perm) {
            // no player
            if (who == null) return defaultPerms.getBoolPermission(perm) == OptBool.True;

            UUID uuid;
            if (who instanceof OfflinePlayer plr) uuid = plr.getUniqueId();
            else if (who instanceof Entity e) uuid = e.getUniqueId();
            else {
                return defaultPerms.getBoolPermission(perm) == OptBool.True;
            }

            // owner
            if (owner != null && owner.getUniqueId().equals(uuid)) return true;

            // explicit perms
            if (perms.containsKey(uuid)) {
                var info = perms.get(uuid);
                var has = info.getBoolPermission(perm);
                if (has != OptBool.Default) {
                    return has == OptBool.True;
                }
            }

            // default entity permissions
            if (!(who instanceof OfflinePlayer)) {
                var has = defaultEntityPerms.getBoolPermission(perm);
                if (has != OptBool.Default) {
                    return has == OptBool.True;
                }
            }

            // default permissions
            return defaultPerms.getBoolPermission(perm) == OptBool.True;
        }

        public OptBool hasPermission(Object who, AdvancedLandPermission perm) {
            // no player
            if (who == null) return defaultPerms.getBoolPermission(perm);

            UUID uuid;
            if (who instanceof OfflinePlayer plr) uuid = plr.getUniqueId();
            else if (who instanceof Entity e) uuid = e.getUniqueId();
            else {
                return defaultPerms.getBoolPermission(perm);
            }

            // owner
            if (owner != null && owner.getUniqueId().equals(uuid)) return OptBool.Default;

            // explicit perms
            if (perms.containsKey(uuid)) {
                var info = perms.get(uuid);
                var has = info.getBoolPermission(perm);
                if (has != OptBool.Default) {
                    return has;
                }
            }

            // default entity permissions
            if (!(who instanceof OfflinePlayer)) {
                var has = defaultEntityPerms.getBoolPermission(perm);
                if (has != OptBool.Default) {
                    return has;
                }
            }

            // default permissions
            return defaultPerms.getBoolPermission(perm);
        }

        public record MPRes(boolean result, String message) {}
        @SuppressWarnings("BooleanMethodIsAlwaysInverted") // what is it talking about
        public MPRes hasPermissions(Object who, LandPermission gen, AdvancedLandPermission... perms) {
            for (var perm : perms) {
                var has = hasPermission(who, perm);
                if (has != OptBool.Default) return new MPRes(has == OptBool.True, perm.label);
            }
            return new MPRes(hasPermission(who, gen), gen.label);
        }

        boolean intersects(LandClaim other) {
            if (start.getWorld() != other.start.getWorld()) return false;

            Set<org.bukkit.util.Vector> a = new HashSet<>();
            Set<org.bukkit.util.Vector> b = new HashSet<>();
            for (int x = Math.min(start.getBlockX(), end.getBlockX()); x <= Math.max(start.getBlockX(), end.getBlockX()); x++) {
                for (int y = Math.min(start.getBlockY(), end.getBlockY()); y <= Math.max(start.getBlockY(), end.getBlockY()); y++) {
                    for (int z = Math.min(start.getBlockZ(), end.getBlockZ()); z <= Math.max(start.getBlockZ(), end.getBlockZ()); z++) {
                        a.add(new org.bukkit.util.Vector(x, y, z));
                    }
                }
            }

            for (int x = Math.min(other.start.getBlockX(), other.end.getBlockX()); x <= Math.max(other.start.getBlockX(), other.end.getBlockX()); x++) {
                for (int y = Math.min(other.start.getBlockY(), other.end.getBlockY()); y <= Math.max(other.start.getBlockY(), other.end.getBlockY()); y++) {
                    for (int z = Math.min(other.start.getBlockZ(), other.end.getBlockZ()); z <= Math.max(other.start.getBlockZ(), other.end.getBlockZ()); z++) {
                        b.add(new org.bukkit.util.Vector(x, y, z));
                    }
                }
            }

            if (a.size() > b.size()) {
                for (org.bukkit.util.Vector block : b) {
                    if (a.contains(block)) return true;
                }
            } else {
                for (org.bukkit.util.Vector block : a) {
                    if (b.contains(block)) return true;
                }
            }

            return false;
        }

        boolean intersects(Location oStart, Location oEnd) {
            if (start.getWorld() != oStart.getWorld()) return false;

            Set<org.bukkit.util.Vector> a = new HashSet<>();
            Set<org.bukkit.util.Vector> b = new HashSet<>();
            for (int x = Math.min(start.getBlockX(), end.getBlockX()); x <= Math.max(start.getBlockX(), end.getBlockX()); x++) {
                for (int y = Math.min(start.getBlockY(), end.getBlockY()); y <= Math.max(start.getBlockY(), end.getBlockY()); y++) {
                    for (int z = Math.min(start.getBlockZ(), end.getBlockZ()); z <= Math.max(start.getBlockZ(), end.getBlockZ()); z++) {
                        a.add(new org.bukkit.util.Vector(x, y, z));
                    }
                }
            }

            for (int x = Math.min(oStart.getBlockX(), oEnd.getBlockX()); x <= Math.max(oStart.getBlockX(), oEnd.getBlockX()); x++) {
                for (int y = Math.min(oStart.getBlockY(), oEnd.getBlockY()); y <= Math.max(oStart.getBlockY(), oEnd.getBlockY()); y++) {
                    for (int z = Math.min(oStart.getBlockZ(), oEnd.getBlockZ()); z <= Math.max(oStart.getBlockZ(), oEnd.getBlockZ()); z++) {
                        b.add(new org.bukkit.util.Vector(x, y, z));
                    }
                }
            }

            if (a.size() > b.size()) {
                for (org.bukkit.util.Vector block : b) {
                    if (a.contains(block)) return true;
                }
            } else {
                for (Vector block : a) {
                    if (b.contains(block)) return true;
                }
            }

            return false;
        }

        public boolean inBounds(Location location) {
            if (start.getWorld() != location.getWorld()) return false;

            int minX = Math.min(start.getBlockX(), end.getBlockX());
            int maxX = Math.max(start.getBlockX(), end.getBlockX());
            int minY = Math.min(start.getBlockY(), end.getBlockY());
            int maxY = Math.max(start.getBlockY(), end.getBlockY());
            int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
            int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());

            return location.getBlockX() >= minX && location.getBlockX() <= maxX
                    && location.getBlockY() >= minY && location.getBlockY() <= maxY
                    && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
        }

        public Component getOwnerName() {
            if (owner == null) return Component.text("the server").color(NamedTextColor.GOLD);
            return Names.instance.getRenderedName(owner);
        }

        public String getOwnerUsername() {
            if (owner == null) return "the server";
            return owner.getName();
        }
    }

    public static void showArea(Player player, Location corner1, Location corner2, double step, int durationTicks, Color color) {
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX()) + 1;

        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY()) + 1;

        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ()) + 1;

        World world = corner1.getWorld(); // Assumes both corners are in same world

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                try {
                    if (ticks >= durationTicks || !player.isOnline()) {
                        this.cancel();
                        return;
                    }
                    ticks++;
                    if (ticks % 9 != 0) return;

                    var ploc = player.getLocation();
                    if (!world.equals(ploc.getWorld())) return;

                    for (double x = minX; x <= maxX; x += step) {
                        for (double y = minY; y <= maxY; y += step) {
                            for (double z = minZ; z <= maxZ; z += step) {
                                int faces = 0;
                                if (x == minX || x == maxX) faces++;
                                if (y == minY || y == maxY) faces++;
                                if (z == minZ || z == maxZ) faces++;

                                if (faces >= 2) {
                                    Location loc = new Location(world, x, y, z);
                                    if (ploc.distance(loc) > 100) continue;
                                    player.spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0, new Particle.DustOptions(color, 1.0F));
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    Logger.exceptions.add(exception);
                    Logger.error(exception.getMessage());
                }
            }
        }.runTaskTimer(KamsTweaks.get(), 0L, 1L);
    }

    public void listClaims(OfflinePlayer who, CommandSender receiver) {
        Component msg = Component.empty();
        int i = 0;
        for (LandClaim claim : claims) {
            if (claim.owner != null && who.getUniqueId().equals(claim.owner.getUniqueId())) {
                i++;
                msg = msg.append(Component.newline(),
                        Component.text("("), Component.text(claim.id).color(NamedTextColor.GOLD), Component.text(") "),
                        Component.text(claim.config.name).color(NamedTextColor.AQUA),
                        Component.text(" (priority "), Component.text(claim.config.priority).color(NamedTextColor.YELLOW), Component.text("): "),
                        Component.text(claim.start.getBlockX() + ", " + claim.start.getBlockY() + ", " + claim.start.getBlockZ()).color(NamedTextColor.GREEN),
                        Component.text(" to "), Component.text(claim.end.getBlockX() + ", " + claim.end.getBlockY() + ", " + claim.end.getBlockZ()).color(NamedTextColor.GREEN),
                        Component.text(" in "), Component.text(claim.start.getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE),
                        claim.slots > 1 ? (Component.text(" (").append(Component.text(claim.slots).color(NamedTextColor.YELLOW), Component.text(" slots)"))) : Component.empty());
            }
        }

        receiver.sendMessage(Component.text().append(who == receiver ? Component.text("You have ") : Names.instance.getRenderedName(who).append(Component.text(" has ")), Component.text(i).color(NamedTextColor.GOLD), Component.text(" land claims"), Component.text("."), msg));
    }

    public void listClaims(Player who) {
        listClaims(who, who);
    }

    public void showClaims(Player who) {
        for (var claim : claims) {
            Color c;
            if (claim.owner != null && claim.owner.getUniqueId().equals(who.getUniqueId())) {
                c = Color.GREEN;
            } else {
                if (claim.hasPermission(who, LandPermission.BLOCK_BREAK) && claim.hasPermission(who, LandPermission.BLOCK_PLACE) && claim.hasPermission(who, LandPermission.BLOCK_INTERACT)) {
                    c = Color.AQUA;
                } else if (claim.hasPermission(who, LandPermission.BLOCK_BREAK) || claim.hasPermission(who, LandPermission.BLOCK_PLACE)) {
                    c = Color.FUCHSIA;
                } else if (claim.hasPermission(who, LandPermission.BLOCK_INTERACT)) {
                    c = Color.PURPLE;
                } else if (claim.hasPermission(who, LandPermission.DOOR_INTERACT)) {
                    c = Color.ORANGE;
                } else {
                    c = Color.RED;
                }
            }
            LandClaims.showArea(who, claim.start, claim.end, 1, 200, c);
        }
    }

    @EventHandler
    public void onJoinWorld(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getWorld().getEnderDragonBattle() != null && event.getWorld().getEnvironment() == World.Environment.THE_END && event.getWorld().getEnderDragonBattle().getEnderDragon() != null) {
                player.sendMessage(Component.text("Claims are currently disabled at the end island due to an ongoing dragon fight. They will be re-enabled 5 minutes after the fight.").color(NamedTextColor.YELLOW));
            } else if (disabled.containsKey(event.getWorld())) {
                player.sendMessage(Component.text("Claims are currently disabled at the end island due to a recent dragon fight. They will be re-enabled in " + disabled.get(event.getWorld()) + " seconds.").color(NamedTextColor.YELLOW));
            }
        }
    }
}
