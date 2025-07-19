package kam.kamsTweaks.features.landclaims;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle.DustOptions;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

public class LandClaims implements Listener {
    public final List<Claim> claims = new ArrayList<>();
    public final List<Claim> claiming = new ArrayList<>();

    private File claimsFile;
    FileConfiguration claimsConfig;

    LandClaimsProtection prot;
    LandClaimGui gui;

    public void setup() {
        prot.init();
        getServer().getPluginManager().registerEvents(prot, KamsTweaks.getInstance());
        getServer().getPluginManager().registerEvents(gui, KamsTweaks.getInstance());
        getServer().getPluginManager().registerEvents(this, KamsTweaks.getInstance());
    }

    public LandClaims() {
        prot = new LandClaimsProtection(this);
        gui = new LandClaimGui(this);
    }

    public void showArea(
            Player player,
            Location corner1,
            Location corner2,
            double step,
            int durationTicks,
	    Color color
    ) {
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
                if (ticks >= durationTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                ticks++;

                var ploc = player.getLocation();
                if (!world.equals(ploc.getWorld())) return;

                if (ticks % 7 == 1) {
                    for (double x = minX; x <= maxX; x += step) {
                        for (double y = minY; y <= maxY; y += step) {
                            for (double z = minZ; z <= maxZ; z += step) {
                                int faces = 0;
                                if (x == minX || x == maxX) faces++;
                                if (y == minY || y == maxY) faces++;
                                if (z == minZ || z == maxZ) faces++;

                                // Only edges and corners
                                if (faces >= 2) {
                                    Location loc = new Location(world, x, y, z);
                                    if (ploc.distance(loc) > 100) continue;
                                    player.spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0, new DustOptions(color, 1.0F));
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(KamsTweaks.getInstance(), 0L, 1L); // Runs every tick
    }

    boolean inBounds(Location target, Location bound1, Location bound2) {
        if (bound1.getWorld() != target.getWorld()) return false;

        int minX = Math.min(bound1.getBlockX(), bound2.getBlockX());
        int maxX = Math.max(bound1.getBlockX(), bound2.getBlockX());
        int minY = Math.min(bound1.getBlockY(), bound2.getBlockY());
        int maxY = Math.max(bound1.getBlockY(), bound2.getBlockY());
        int minZ = Math.min(bound1.getBlockZ(), bound2.getBlockZ());
        int maxZ = Math.max(bound1.getBlockZ(), bound2.getBlockZ());

        return target.getBlockX() >= minX && target.getBlockX() <= maxX
            && target.getBlockY() >= minY && target.getBlockY() <= maxY
            && target.getBlockZ() >= minZ && target.getBlockZ() <= maxZ;
    }

    boolean hasPermission(OfflinePlayer player, Claim claim, ClaimPermission perm) {
        if (claim == null) return true;
        if (player != null && claim.m_owner != null && claim.m_owner.getUniqueId().equals(player.getUniqueId())) return true;
        ClaimPermission claimPerm = claim.m_perms.getOrDefault(player != null ? getServer().getOfflinePlayer(player.getUniqueId()) : null, claim.m_default);
        return claimPerm.compareTo(perm) >= 0;
    }

    Claim getClaim(Location where) {
        for (Claim claim : claims) {
            if (claim.m_start.getWorld() != where.getWorld()) continue;
            if (inBounds(where, claim.m_start, claim.m_end)) {
                return claim;
            }
        }
        return null;
    }

    void handleItem(PlayerInteractEvent e) {
        Location loc = e.getPlayer().getLocation();
        if (e.getClickedBlock() != null) {
            loc = e.getClickedBlock().getLocation();
            for (Claim claim : claiming) {
                if (claim.m_owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                    for (Claim other : claims) {
                        if (inBounds(loc, other.m_start, other.m_end)) {
                            if (other.m_owner != null && !other.m_owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                                e.getPlayer().sendMessage(Component.text("This land is already claimed by ").color(NamedTextColor.RED).append(Component.text(other.m_owner.getName() == null ? "Unknown player" : other.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                                return;
                            } else if(other.m_owner == null) {
                                e.getPlayer().sendMessage(Component.text("This land is already claimed by ").color(NamedTextColor.RED).append(Component.text("the server").color(NamedTextColor.GOLD)).append(Component.text(".")));
                                return;
                            }
                        }
                    }
                    if (claim.m_start != null) {
                        if (claim.m_start.getWorld() != loc.getWorld()) {
                            e.getPlayer().sendMessage(Component.text("You can't claim across dimensions - go back to the dimension you started in!").color(NamedTextColor.RED));
                            return;
                        }
                        var max = KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claim-size", 27000);
                        var has = Math.abs(claim.m_start.x() - loc.x()) * Math.abs(claim.m_start.y() - loc.y()) * Math.abs(claim.m_start.z() - loc.z());
                        if (has > max) {
                            e.getPlayer().sendMessage(Component.text("You can't claim more than " + max + " blocks - you are trying to claim " + has + ".").color(NamedTextColor.RED));
                            return;
                        }
                        claim.m_end = loc;
                        for (Claim other : claims) {
                            if (inBounds(other.m_start, claim.m_start, claim.m_end) || inBounds(other.m_end, claim.m_start, claim.m_end)) {
                                if (other.m_owner != null && !other.m_owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                                    e.getPlayer().sendMessage(Component.text("This land intersects a claim by ").color(NamedTextColor.RED).append(Component.text(other.m_owner.getName() == null ? "Unknown player" : other.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                                    return;
                                } else if(other.m_owner == null) {
                                    e.getPlayer().sendMessage(Component.text("This land intersects a claim by ").color(NamedTextColor.RED).append(Component.text("the server").color(NamedTextColor.GOLD)).append(Component.text(".")));
                                    return;
                                }
                            }
                        }
                        claims.add(claim);
                        claiming.remove(claim);
                        e.getPlayer().sendMessage(Component.text("Territory claimed.").color(NamedTextColor.GREEN));
                    } else {
                        claim.m_start = loc;
                        e.getPlayer().sendMessage(Component.text("Now click the other corner with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW)).append(Component.text(")").color(NamedTextColor.BLUE)).color(NamedTextColor.BLUE));
                    }
                    return;
                }
            }
        }
        gui.showClaimGui(e.getPlayer(), loc);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("claims")
                .then(Commands.literal("config").requires(sender -> sender.getSender().hasPermission("kamstweaks.landclaims.configure"))
                    .then(Commands.literal("enabled")
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                            KamsTweaks.getInstance().getConfig().set("land-claims.enabled", ctx.getArgument("enabled", Boolean.class));
                            ctx.getSource().getSender().sendMessage("Successfully " + (ctx.getArgument("enabled", Boolean.class) == true ? "enabled" : "disabled") + " land claims." );
                            return Command.SINGLE_SUCCESS;
                        }))
                        .executes(ctx -> {
                            ctx.getSource().getSender().sendMessage("Land claims are currently " + KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true) + "." );
                            return Command.SINGLE_SUCCESS;
                        })
                    )
                    .then(Commands.literal("admin-bypass-claims")
                            .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                                KamsTweaks.getInstance().getConfig().set("land-claims.admin-bypass-claims", ctx.getArgument("enabled", Boolean.class));
                                ctx.getSource().getSender().sendMessage("Successfully " + (ctx.getArgument("enabled", Boolean.class) == true ? "enabled" : "disabled") + " admins bypassing land claims." );
                                return Command.SINGLE_SUCCESS;
                            }))
                            .executes(ctx -> {
                                ctx.getSource().getSender().sendMessage("Admins bypassing land claims is currently " + KamsTweaks.getInstance().getConfig().getBoolean("land-claims.admin-bypass-claims", true) + "." );
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .then(Commands.literal("max-claims")
                            .then(Commands.argument("claims", IntegerArgumentType.integer()).executes(ctx -> {
                                KamsTweaks.getInstance().getConfig().set("land-claims.max-claims", ctx.getArgument("claims", Integer.class));
                                ctx.getSource().getSender().sendMessage("Successfully set max claims to " + ctx.getArgument("claims", Integer.class) + ".");
                                return Command.SINGLE_SUCCESS;
                            }))
                            .executes(ctx -> {
                                ctx.getSource().getSender().sendMessage("Max claims is currently " + KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claims", 10) + "." );
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                    .then(Commands.literal("max-claim-size")
                            .then(Commands.argument("size", IntegerArgumentType.integer()).executes(ctx -> {
                                KamsTweaks.getInstance().getConfig().set("land-claims.max-claim-size", ctx.getArgument("size", Integer.class));
                                ctx.getSource().getSender().sendMessage("Successfully set max claim size to " + ctx.getArgument("size", Integer.class) + ".");
                                return Command.SINGLE_SUCCESS;
                            }))
                            .executes(ctx -> {
                                ctx.getSource().getSender().sendMessage("Max claim size is currently " + KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claim-size", 27000) + "." );
                                return Command.SINGLE_SUCCESS;
                            })
                    )
                )
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
                    })
                )
                .then(Commands.literal("at")
                    .then(Commands.argument("pos", ArgumentTypes.blockPosition()).executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                            sender.sendPlainMessage("Land claims are disabled.");
                            return Command.SINGLE_SUCCESS;
                        }
                        Entity exec = ctx.getSource().getExecutor();
                        final BlockPositionResolver blockPositionResolver = ctx.getArgument("pos", BlockPositionResolver.class);
                        final BlockPosition loc = blockPositionResolver.resolve(ctx.getSource());
                        Claim res = getClaim(loc.toLocation(exec == null ? getServer().getWorlds().getFirst() : exec.getWorld()));
                        if (res == null)
                            sender.sendMessage(Component.text("This land isn't claimed."));
                        else if (res.m_owner == null)
                            sender.sendMessage(Component.text("This claim is owned by the server."));
                        else
                            sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.m_owner.getName() == null ? "Unknown player" : res.m_owner.getName()).color(NamedTextColor.GOLD)));
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("world", ArgumentTypes.world())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                                sender.sendPlainMessage("Land claims are disabled.");
                                return Command.SINGLE_SUCCESS;
                            }
                            final BlockPosition loc = ctx.getArgument("pos", BlockPositionResolver.class).resolve(ctx.getSource());
                            final World world = ctx.getArgument("world", World.class);
                            if (world == null) {
                                sender.sendMessage("Cannot check owner of claim without world.");
                                return Command.SINGLE_SUCCESS;
                            }
                            Claim res = getClaim(loc.toLocation(world));
                            if (res == null)
                                sender.sendMessage(Component.text("This land isn't claimed."));
                            else if (res.m_owner == null)
                                sender.sendMessage(Component.text("This claim is owned by the server."));
                            else
                                sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.m_owner.getName() == null ? "Unknown player" : res.m_owner.getName()).color(NamedTextColor.GOLD)));
                            return Command.SINGLE_SUCCESS;
                        }))
                    )
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                            sender.sendPlainMessage("Land claims are disabled.");
                            return Command.SINGLE_SUCCESS;
                        }
                        Entity executor = ctx.getSource().getExecutor();
                        if (executor == null) {
                            sender.sendMessage("Cannot check owner of claim without position.");
                            return Command.SINGLE_SUCCESS;
                        }
                        Claim res = getClaim(executor.getLocation());
                        if (res == null)
                            sender.sendMessage(Component.text("This land isn't claimed."));
                        else if (res.m_owner == null)
                            sender.sendMessage(Component.text("This claim is owned by the server."));
                        else
                            sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.m_owner.getName() == null ? "Unknown player" : res.m_owner.getName()).color(NamedTextColor.GOLD)));
                        return Command.SINGLE_SUCCESS;
                    })
                );

        LiteralCommandNode<CommandSourceStack> buildCommand = command.build();
        commands.registrar().register(buildCommand);
    }

    private void setupFile() {
        claimsFile = new File(KamsTweaks.getInstance().getDataFolder(), "claims.yml");
        if (!claimsFile.exists()) {
            boolean mkdir = claimsFile.getParentFile().mkdirs();
            KamsTweaks.getInstance().saveResource("claims.yml", false);
        }
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);
    }

    public void saveClaims() {
        setupFile();
        claimsConfig.set("claims", null); // clear previous entries

        int i = 0;
        for (Claim claim : claims) {
            String path = "claims." + i;
            if (claim.m_owner != null) claimsConfig.set(path + ".owner", claim.m_owner.getUniqueId().toString());
            claimsConfig.set(path + ".corner1", serializeLocation(claim.m_start));
            claimsConfig.set(path + ".corner2", serializeLocation(claim.m_end));
            claimsConfig.set(path + ".default", claim.m_default.name());
            claim.m_perms.forEach((player, perm) -> claimsConfig.set(path + ".perms." + player.getUniqueId(), perm.name()));
            i++;
        }

        claimsConfig.set("entities", null); // clear previous entries

        KamsTweaks.getInstance().m_entityClaims.claims.forEach((uuid, claim) -> {
            String path = "entities." + uuid;
            if (claim.m_owner != null) claimsConfig.set(path + ".owner", claim.m_owner.getUniqueId().toString());
            claimsConfig.set(path + ".default", claim.m_default.name());
            claim.m_perms.forEach((player, perm) -> claimsConfig.set(path + ".perms." + player.getUniqueId(), perm.name()));
        });

        try {
            claimsConfig.save(claimsFile);
        } catch (IOException e) {
            KamsTweaks.getInstance().getLogger().warning(e.getMessage());
        }
    }

    public void loadClaims() {
        setupFile();
        claims.clear();
        if (claimsConfig.contains("claims")) {
            for (String key : Objects.requireNonNull(claimsConfig.getConfigurationSection("claims")).getKeys(false)) {
                try {
                    String ownerStr = claimsConfig.getString("claims." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    String corner1Str = claimsConfig.getString("claims." + key + ".corner1");
                    assert corner1Str != null;
                    Location corner1 = deserializeLocation(corner1Str);
                    if (corner1.getWorld() == null) continue;
                    String corner2Str = claimsConfig.getString("claims." + key + ".corner2");
                    assert corner2Str != null;
                    Location corner2 = deserializeLocation(corner2Str);
                    Claim claim = new Claim(owner == null ? null : getServer().getOfflinePlayer(owner), corner1, corner2);
                    claim.m_default = ClaimPermission.valueOf(claimsConfig.getString("claims." + key + ".default"));
                    if (claimsConfig.contains("claims." + key + ".perms")) {
                        for (String uuid : Objects.requireNonNull(claimsConfig.getConfigurationSection("claims." + key + ".perms")).getKeys(false)) {
                            claim.m_perms.put(Bukkit.getOfflinePlayer(UUID.fromString(uuid)), ClaimPermission.valueOf(claimsConfig.getString("claims." + key + ".perms." + uuid)));
                        }
                    }

                    claims.add(claim);
                } catch (Exception e) {
                    KamsTweaks.getInstance().getLogger().warning(e.getMessage());
                }
            }
        }

        KamsTweaks.getInstance().m_entityClaims.claims.clear();
        if (claimsConfig.contains("entities")) {
            for (String key : Objects.requireNonNull(claimsConfig.getConfigurationSection("entities")).getKeys(false)) {
                try {
                    String ownerStr = claimsConfig.getString("entities." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    EntityClaims.EntityClaim claim = new EntityClaims.EntityClaim(owner == null ? null : getServer().getOfflinePlayer(owner));
                    claim.m_default = EntityClaims.EntityPermission.valueOf(claimsConfig.getString("entities." + key + ".default"));
                    if (claimsConfig.contains("entities." + key + ".perms")) {
                        for (String uuid : Objects.requireNonNull(claimsConfig.getConfigurationSection("entities." + key + ".perms")).getKeys(false)) {
                            claim.m_perms.put(Bukkit.getOfflinePlayer(UUID.fromString(uuid)), EntityClaims.EntityPermission.valueOf(claimsConfig.getString("entities." + key + ".perms." + uuid)));
                        }
                    }
                    KamsTweaks.getInstance().m_entityClaims.claims.put(UUID.fromString(key), claim);
                } catch (Exception e) {
                    KamsTweaks.getInstance().getLogger().warning(e.getMessage());
                }
            }
        }
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld() == null ? "" : loc.getWorld().getUID() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    private Location deserializeLocation(String s) {
        String[] parts = s.split(",");
        UUID worldUuid = UUID.fromString(parts[0]);
        return new Location(
                getServer().getWorld(worldUuid),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
        );
    }

    public static class Claim {
        Location m_start;
        Location m_end;
        OfflinePlayer m_owner;
        Map<OfflinePlayer, ClaimPermission> m_perms = new HashMap<>();
        ClaimPermission m_default = ClaimPermission.DOORS;
        public Claim(OfflinePlayer owner, Location start, Location end) {
            m_owner = owner;
            m_start = start;
            m_end = end;
        }
    }

    public enum ClaimPermission {
        NONE,
        DOORS,
        INTERACT,
        BLOCKS
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        if (ItemManager.getType(e.getItemInHand()) == ItemManager.ItemType.CLAIMER) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        if (!e.getPlayer().hasPlayedBefore()) {
            e.getPlayer().getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.CLAIMER).clone());
        }
    }
}
