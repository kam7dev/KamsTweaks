package kam.kamsTweaks.features.claims;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Names;
import kam.kamsTweaks.features.claims.gui.LandClaimPage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.rutgerkok.blocklocker.BlockLockerAPIv2;
import org.bukkit.*;
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

        var testClaim = new LandClaim(null, new Location(Bukkit.getServer().getWorlds().getFirst(), -10, 50, -10), new Location(Bukkit.getServer().getWorlds().getFirst(), 10, 150, 10));
        claims.add(testClaim);
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

        var create = Commands.literal("create").executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor != sender) {
                sender.sendMessage(Component.text("You can't start claiming for someone else.").color(NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }
            if (executor instanceof Player player) {
                startClaiming(player);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(Component.text("Only a player can run this.").color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        });

//        create.then(Commands.argument("pos1", ArgumentTypes.blockPosition()).then(Commands.argument("pos2", ArgumentTypes.blockPosition()).executes(ctx -> {
//
//            return Command.SINGLE_SUCCESS;
//        })));

        var cancel = Commands.literal("cancel").executes(ctx -> {
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
        });

        land.then(create);
        baseLand.then(create);

        land.then(cancel);
        baseLand.then(cancel);

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

    public void startClaiming(Player who) {
        if (currentlyClaiming.containsKey(who)) {
            who.sendMessage(Component.text("You're already claiming land. (run ").append(Component.text("/claims land cancel").clickEvent(ClickEvent.runCommand("claims land cancel")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(" to cancel)")).color(NamedTextColor.RED));
        } else {
            who.sendMessage(Component.text("Right click the first corner of where you want to claim with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(")")).color(NamedTextColor.GOLD));
            currentlyClaiming.put(who, new LandClaim(who, null, null));
        }
    }

    public void stopClaiming(Player who) {
        if (currentlyClaiming.containsKey(who)) {
            who.sendMessage(Component.text("Stopped claiming.").color(NamedTextColor.GOLD));
            currentlyClaiming.remove(who);
        } else {
            who.sendMessage(Component.text("You aren't currently claiming land. (run ").append(Component.text("/claims land create").clickEvent(ClickEvent.runCommand("claims land cancel")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(" or use the claim tool to start)")).color(NamedTextColor.RED));
        }
    }

    public enum LandPermission {
        DOOR_INTERACT("Interact with Doors"),
        BLOCK_INTERACT("Interact with Blocks"),
        BLOCK_BREAK("Break Blocks"),
        BLOCK_PLACE("Place Blocks"),

        ;

        public final String label;
        LandPermission(String label) {
            this.label = label;
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
        AdvancedLandPermission(String label) {
            this.label = label;
        }
    }

    public static class Permissions implements Cloneable{
        public LandClaim claim;
        public UUID who;
        Map<LandPermission, OptBool> bools = new HashMap<>();
        Map<AdvancedLandPermission, OptBool> advancedBools = new HashMap<>();

        public Permissions(LandClaim claim, UUID who) {
            this.claim = claim;
            this.who = who;
        }

        public static Permissions defaultPerms;
        static {
            defaultPerms = new Permissions(null, null);
            defaultPerms.bools.put(LandPermission.BLOCK_BREAK, OptBool.FALSE);
            defaultPerms.bools.put(LandPermission.BLOCK_PLACE, OptBool.FALSE);
            defaultPerms.bools.put(LandPermission.BLOCK_INTERACT, OptBool.FALSE);
            defaultPerms.bools.put(LandPermission.DOOR_INTERACT, OptBool.TRUE);
//            defaultPerms.advancedBools.put(AdvancedLandPermission.S, OptBool.FALSE);
        }

        public OptBool getBoolPermission(LandPermission perm) {
            return bools.getOrDefault(perm, OptBool.DEFAULT);
        }

        public OptBool getBoolPermission(AdvancedLandPermission perm) {
            return advancedBools.getOrDefault(perm, OptBool.DEFAULT);
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

    public enum ConfigEnum1 {
        None("None"),
        FromInside("From Inside Claim"),
        All("All");

        public final String label;
        ConfigEnum1(String label) {
            this.label = label;
        }
    }

    public static class ClaimConfig {
        public String name = "Unnamed Claim";
        public Integer priority = 0;

        // TODO: these
        public ConfigEnum1 grassSpread = ConfigEnum1.FromInside;
        public ConfigEnum1 fireSpread = ConfigEnum1.FromInside;
        public ConfigEnum1 waterFlow = ConfigEnum1.FromInside;
        public ConfigEnum1 hopperTransfer = ConfigEnum1.FromInside; // from outside
        public ConfigEnum1 gravity = ConfigEnum1.FromInside;
        public ConfigEnum1 treesGrow = ConfigEnum1.FromInside;
    }

    public static class LandClaim {
        public @Nullable OfflinePlayer owner;
        public static int nextId;
        public int id;

        public Location start;
        public Location end;
        public int claimCount = 0;

        public ClaimConfig config = new ClaimConfig();

        public Map<UUID, Permissions> perms = new HashMap<>();
        public Permissions defaultPerms = Permissions.defaultPerms.clone();
        public Permissions defaultEntityPerms = Permissions.defaultPerms.clone();

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

        public boolean hasPermission(Object who, LandPermission perm) {
            // no player
            if (who == null) return defaultPerms.getBoolPermission(perm) == OptBool.TRUE;

            UUID uuid;
            if (who instanceof OfflinePlayer plr) uuid = plr.getUniqueId();
            else if (who instanceof Entity e) uuid = e.getUniqueId();
            else {
                return defaultPerms.getBoolPermission(perm) == OptBool.TRUE;
            }

            // owner
            if (owner != null && owner.getUniqueId().equals(uuid)) return true;

            // explicit perms
            if (perms.containsKey(uuid)) {
                var info = perms.get(uuid);
                var has = info.getBoolPermission(perm);
                if (has != OptBool.DEFAULT) {
                    return has == OptBool.TRUE;
                }
            }

            // default entity permissions
            if (!(who instanceof OfflinePlayer)) {
                var has = defaultEntityPerms.getBoolPermission(perm);
                if (has != OptBool.DEFAULT) {
                    return has == OptBool.TRUE;
                }
            }

            // default permissions
            return defaultPerms.getBoolPermission(perm) == OptBool.TRUE;
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
            if (owner != null && owner.getUniqueId().equals(uuid)) return OptBool.DEFAULT;

            // explicit perms
            if (perms.containsKey(uuid)) {
                var info = perms.get(uuid);
                var has = info.getBoolPermission(perm);
                if (has != OptBool.DEFAULT) {
                    return has;
                }
            }

            // default entity permissions
            if (!(who instanceof OfflinePlayer)) {
                var has = defaultEntityPerms.getBoolPermission(perm);
                if (has != OptBool.DEFAULT) {
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
                if (has != OptBool.DEFAULT) return new MPRes(has == OptBool.TRUE, perm.label);
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
                    Logger.excs.add(exception);
                    Logger.error(exception.getMessage());
                }
            }
        }.runTaskTimer(KamsTweaks.get(), 0L, 1L);
    }

    public static OfflinePlayer getBlockLockerOwner(Player player, Location start, Location end) {
        if (Bukkit.getPluginManager().isPluginEnabled("BlockLocker")) {
            if (start.getWorld() != end.getWorld()) return null;
            var world = start.getWorld();
            for (var x = Math.min(start.getBlockX(), end.getBlockX()); x <= Math.max(start.getBlockX(), end.getBlockX()); x++) {
                for (var y = Math.min(start.getBlockY(), end.getBlockY()); y <= Math.max(start.getBlockY(), end.getBlockY()); y++) {
                    for (var z = Math.min(start.getBlockZ(), end.getBlockZ()); z <= Math.max(start.getBlockZ(), end.getBlockZ()); z++) {
                        var block = world.getBlockAt(x, y, z);
                        if (BlockLockerAPIv2.isProtected(block) && !BlockLockerAPIv2.isOwner(player, block))
                            //noinspection OptionalGetWithoutIsPresent
                            return BlockLockerAPIv2.getOwner(block).get();
                    }
                }
            }
        }
        return null;
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
