package kam.kamsTweaks.features.claims;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Names;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import nl.rutgerkok.blocklocker.BlockLockerAPIv2;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import kam.kamsTweaks.features.claims.Claims.OptBool;
import org.jetbrains.annotations.Nullable;

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
        public OfflinePlayer owner;
        public static int nextId;
        public int id;

        Location start;
        Location end;
        Integer claimCount = 0;

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

        public LandClaim(OfflinePlayer owner, Location start, Location end) {
            this.id = nextId;
            nextId++;
            this.owner = owner;
            this.start = start;
            this.end = end;
        }

        public LandClaim(OfflinePlayer owner, int id, Location start, Location end) {
            nextId = Math.max(nextId, id + 1);
            this.id = id;
            this.owner = owner;
            this.start = start;
            this.end = end;
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

    public static OfflinePlayer isBlockLocked(Player player, Location start, Location end) {
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
