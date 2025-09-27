package kam.kamsTweaks.features.claims;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class Claims extends Feature {
    public ClaimProtections protections = new ClaimProtections();
    public ClaimsDialogGui dialogGui = new ClaimsDialogGui();

    public List<LandClaim> landClaims = new ArrayList<>();
    public Map<Player, LandClaim> currentlyClaiming = new HashMap<>();

    public Map<UUID, EntityClaim> entityClaims = new HashMap<>();
    // for stuff like dragon fight
    public Map<World, Integer> disabledClaims = new HashMap<>();

    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("land-claims.enabled", "land-claims.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("land-claims.max-claims", "land-claims.max-claims", 30, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("land-claims.max-claim-size", "land-claims.max-claim-size", 50000, "kamstweaks.configure"));
        protections.setup(this);
        dialogGui.setup(this);
        Bukkit.getServer().getPluginManager().registerEvents(protections, KamsTweaks.getInstance());
        Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.getInstance(), () -> {
            for (var world : disabledClaims.keySet()) {
                var val = disabledClaims.get(world) - 1;
                if (val == 0) {
                    disabledClaims.remove(world);
                    for (var plr : world.getPlayers()) {
                        plr.sendMessage(Component.text("Claims are now re-enabled in this dimension.").color(NamedTextColor.GREEN));
                    }
                } else {
                    disabledClaims.put(world, val);
                }
            }
        }, 20, 20);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("claims")
                .then(Commands.literal("get-tool")
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                                sender.sendPlainMessage("Land claims are disabled.");
                                return Command.SINGLE_SUCCESS;
                            }
                            Entity executor = ctx.getSource().getExecutor();
                            if (!(executor instanceof Player player)) {
                                sender.sendPlainMessage("Only players get the claim tool.");
                                return Command.SINGLE_SUCCESS;
                            }
                            player.getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.CLAIMER).clone());
                            return Command.SINGLE_SUCCESS;
                        })).build());
    }

    private File claimsFile;
    FileConfiguration claimsConfig;

    private void setupFile() {
        claimsFile = new File(KamsTweaks.getInstance().getDataFolder(), "claims.yml");
        if (!claimsFile.exists()) {
            boolean ignored = claimsFile.getParentFile().mkdirs();
            KamsTweaks.getInstance().saveResource("claims.yml", false);
        }
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
    }

    public boolean isClaimable(Entity e) {
        if (!(e instanceof Mob)) return false;
        switch(e.getType()) {
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
        setupFile();
        landClaims.clear();
        if (claimsConfig.contains("claims")) {
            for (String key : Objects.requireNonNull(claimsConfig.getConfigurationSection("claims")).getKeys(false)) {
                try {
                    String ownerStr = claimsConfig.getString("claims." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    String corner1Str = claimsConfig.getString("claims." + key + ".corner1");
                    assert corner1Str != null;
                    Location corner1 = LocationUtils.deserializeBlockPos(corner1Str);
                    if (corner1.getWorld() == null) continue;
                    String corner2Str = claimsConfig.getString("claims." + key + ".corner2");
                    assert corner2Str != null;
                    Location corner2 = LocationUtils.deserializeBlockPos(corner2Str);
                    LandClaim claim = new LandClaim(owner == null ? null : Bukkit.getServer().getOfflinePlayer(owner), corner1, corner2);
                    claim.defaults = new ArrayList<>();
                    try {
                        if (claimsConfig.contains("claims." + key + ".defaults")) {
                            for (String def : Objects.requireNonNull(claimsConfig.getConfigurationSection("claims." + key + ".defaults")).getKeys(false)) {
                                claim.defaults.add(ClaimPermission.valueOf(def));
                            }
                        } else if (claimsConfig.contains("claims." + key + ".default")) {
                            switch(Objects.requireNonNull(claimsConfig.getString("claims." + key + ".default"))) {
                                case "BLOCKS":
                                    claim.defaults.add(ClaimPermission.BLOCK_PLACE);
                                    claim.defaults.add(ClaimPermission.BLOCK_BREAK);
                                    claim.defaults.add(ClaimPermission.INTERACT_BLOCK);
                                    break;
                                case "INTERACT":
                                    claim.defaults.add(ClaimPermission.INTERACT_BLOCK);
                                    break;
                                case "NONE":
                                    break;
                                // doors is also done here
                                default:
                                    claim.defaults.add(ClaimPermission.INTERACT_DOOR);
                                    break;
                            }
                        }
                    } catch(NullPointerException e) {
                        Logger.warn(e.getMessage());
                        claim.defaults = new ArrayList<>();
                        claim.defaults.add(ClaimPermission.INTERACT_DOOR);
                    }

                    if (claimsConfig.contains("claims." + key + ".perms")) {
                        for (String uuid : Objects.requireNonNull(claimsConfig.getConfigurationSection("claims." + key + ".perms")).getKeys(false)) {
                            List<ClaimPermission> perms = new ArrayList<>();
                            switch(Objects.requireNonNull(claimsConfig.getString("claims." + key + ".perms." + uuid))) {
                                case "BLOCKS":
                                    perms.add(ClaimPermission.BLOCK_PLACE);
                                    perms.add(ClaimPermission.BLOCK_BREAK);
                                    perms.add(ClaimPermission.INTERACT_BLOCK);
                                    break;
                                case "INTERACT":
                                    perms.add(ClaimPermission.INTERACT_BLOCK);
                                    break;
                                case "NONE":
                                    break;
                                // doors is also done here
                                default:
                                    perms.add(ClaimPermission.INTERACT_DOOR);
                                    break;
                            }
                            claim.perms.put(Bukkit.getOfflinePlayer(UUID.fromString(uuid)), perms);
                        }
                    }
                    landClaims.add(claim);
                } catch (Exception e) {
                    Logger.warn(e.getMessage());
                }
            }
        }
    }

    @Override
    public void saveData() {

    }

    public static class Claim {
        public OfflinePlayer owner;
        public Map<OfflinePlayer, List<ClaimPermission>> perms = new HashMap<>();
        public List<ClaimPermission> defaults = new ArrayList<>();
        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        public boolean hasPermission(OfflinePlayer player, ClaimPermission perm) {
            if (player == null) return defaults.contains(perm);
            if (owner.equals(player.getPlayer())) return true;
            if (perms.containsKey(player)) {
                var p = perms.get(player);
                if (p.contains(ClaimPermission.DEFAULT)) {
                    return defaults.contains(perm);
                }
                return p.contains(perm);
            } else {
                return defaults.contains(perm);
            }
        }

        public Claim(OfflinePlayer owner) {
            this.owner = owner;
        }
    }

    public enum ClaimPermission {
        // claim perms
        INTERACT_DOOR,
        INTERACT_BLOCK,
        BLOCK_PLACE,
        BLOCK_BREAK,

        // entity perms
        DAMAGE_ENTITY,
        INTERACT_ENTITY,

        DEFAULT, // incompatible with all others
    }


    public static class LandClaim extends Claim {
        Location start;
        Location end;
        String name = "Unnamed claim";
        Integer priority = 0;

        public LandClaim(OfflinePlayer owner, Location start, Location end) {
            super(owner);
            this.defaults.add(ClaimPermission.INTERACT_DOOR);
            this.start = start;
            this.end = end;
        }

        boolean intersects(LandClaim other) {
            if (start.getWorld() != other.start.getWorld()) return false;

            Set<Vector> a = new HashSet<>();
            Set<Vector> b = new HashSet<>();
            for (int x = Math.min(start.getBlockX(), end.getBlockX()); x <= Math.max(start.getBlockX(), end.getBlockX()); x++) {
                for (int y = Math.min(start.getBlockY(), end.getBlockY()); y <= Math.max(start.getBlockY(), end.getBlockY()); y++) {
                    for (int z = Math.min(start.getBlockZ(), end.getBlockZ()); z <= Math.max(start.getBlockZ(), end.getBlockZ()); z++) {
                        a.add(new Vector(x, y, z));
                    }
                }
            }

            for (int x = Math.min(other.start.getBlockX(), other.end.getBlockX()); x <= Math.max(other.start.getBlockX(), other.end.getBlockX()); x++) {
                for (int y = Math.min(other.start.getBlockY(), other.end.getBlockY()); y <= Math.max(other.start.getBlockY(), other.end.getBlockY()); y++) {
                    for (int z = Math.min(other.start.getBlockZ(), other.end.getBlockZ()); z <= Math.max(other.start.getBlockZ(), other.end.getBlockZ()); z++) {
                        b.add(new Vector(x, y, z));
                    }
                }
            }

            if (a.size() > b.size()) {
                for (Vector block : b) {
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
    }

    public static class EntityClaim extends Claim {
        public EntityClaim(OfflinePlayer owner) {
            super(owner);
        }
        public boolean canAggro = false;
    }

    public @Nullable EntityClaim getEntityClaim(Entity entity) {
        return entityClaims.getOrDefault(entity.getUniqueId(), null);
    }

    public @Nullable LandClaim getLandClaim(Location where) {
        return getLandClaim(where, false);
    }

    public @Nullable LandClaim getLandClaim(Location where, boolean ignoresWorldDisable) {
        if (!ignoresWorldDisable && ((where.getWorld().getEnderDragonBattle() != null && where.getWorld().getEnderDragonBattle().getEnderDragon() != null) || disabledClaims.containsKey(where.getWorld()))) return null;
        LandClaim ret = null;
        for (var claim : landClaims) {
            if (claim.inBounds(where)) {
                if (ret == null || claim.priority > ret.priority) {
                    ret = claim;
                }
            }
        }
        return ret;
    }

    public void handleItem(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            dialogGui.openMainPage(event.getPlayer());
        } else {
            var loc = event.getClickedBlock().getLocation();
            if (currentlyClaiming.containsKey(event.getPlayer())) {
                var claim = currentlyClaiming.get(event.getPlayer());
                if (claim.start == null) {
                    for (var c : landClaims) {
                        if (c.inBounds(loc)) {
                            if (c.owner == null || (!c.owner.getUniqueId().equals(event.getPlayer().getUniqueId()))) {
                                Component name;
                                if (c.owner == null) {
                                    name = Component.text("the server").color(NamedTextColor.GOLD);
                                } else if (c.owner.isOnline()) {
                                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                                } else {
                                    name = Component.text(Objects.requireNonNull(c.owner.getName())).color(NamedTextColor.GOLD);
                                }
                                event.getPlayer().sendMessage(Component.text("This land is already claimed by ").append(name, Component.text(".")).color(NamedTextColor.RED));
                                return;
                            }
                        }
                    }
                    claim.start = loc;
                    event.getPlayer().sendMessage(Component.text("Now click the other corner with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW)).append(Component.text(")").color(NamedTextColor.BLUE)).color(NamedTextColor.BLUE));
                } else {
                    if (claim.start.getWorld() != loc.getWorld()) {
                        event.getPlayer().sendMessage(Component.text("You can't claim across dimensions - go back to the dimension you started in!").color(NamedTextColor.RED));
                        return;
                    }
                    var max = KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claim-size", 50000);
                    var has = Math.abs(claim.start.x() - loc.x()) * Math.abs(claim.start.y() - loc.y()) * Math.abs(claim.start.z() - loc.z());
                    if (has > max) {
                        event.getPlayer().sendMessage(Component.text("You can't claim more than " + max + " blocks - you are trying to claim " + has + ".").color(NamedTextColor.RED));
                        return;
                    }
                    claim.end = loc;
                    for (var other : landClaims) {
                        if (claim.intersects(other)) {
                            if (other.owner == null || !other.owner.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                                Component name;
                                if (other.owner == null) {
                                    name = Component.text("the server").color(NamedTextColor.GOLD);
                                } else if (other.owner.isOnline()) {
                                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                                } else {
                                    name = Component.text(Objects.requireNonNull(other.owner.getName())).color(NamedTextColor.GOLD);
                                }
                                event.getPlayer().sendMessage(Component.text("This land intersects a claim by ").append(name, Component.text(".")).color(NamedTextColor.RED));
                                return;
                            }
                        }
                    }
                    landClaims.add(claim);
                    currentlyClaiming.remove(event.getPlayer());
                    event.getPlayer().sendMessage(Component.text("Territory claimed.").color(NamedTextColor.GREEN));
                }
            } else {
                dialogGui.openLCPage(event.getPlayer(), getLandClaim(event.getClickedBlock().getLocation(), true));
            }
        }
    }
}
