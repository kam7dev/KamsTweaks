package kam.kamsTweaks.features.landclaims;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle.DustOptions;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static kam.kamsTweaks.features.landclaims.LandClaimGui.createGuiItem;
import static org.bukkit.Bukkit.getServer;

public class LandClaims {
    public final List<Claim> claims = new ArrayList<>();
    public final List<Claim> claiming = new ArrayList<>();

    private File claimsFile;
    private FileConfiguration claimsConfig;
    boolean loadSuccess = false;

    public List<Entity> cleanupList = new ArrayList<>();

    LandClaimsProtection prot;
    LandClaimGui gui;

    public void setup() {
        getServer().getPluginManager().registerEvents(prot, KamsTweaks.getInstance());
        getServer().getPluginManager().registerEvents(gui, KamsTweaks.getInstance());
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

                for (double x = minX; x <= maxX; x += step) {
                    for (double y = minY; y <= maxY; y += step) {
                        for (double z = minZ; z <= maxZ; z += step) {
                            int faces = 0;
                            if (x == minX || x == maxX) faces++;
                            if (y == minY || y == maxY) faces++;
                            if (z == minZ || z == maxZ) faces++;

                            // Only edges and corners
                            if (faces >= 2) {
                                player.spawnParticle(Particle.DUST, new Location(world, x, y, z), 0, 0, 0, 0, 0, new DustOptions(color, 1.0F));
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
        if (target.getBlockX() >= minX && target.getBlockX() <= maxX) {
            if (target.getBlockY() >= minY && target.getBlockY() <= maxY) {
                if (target.getBlockZ() >= minZ && target.getBlockZ() <= maxZ) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean hasPermission(Player player, Claim claim, ClaimPermission perm) {
        if (player != null && claim != null && claim.m_owner != null && claim.m_owner.getUniqueId().equals(player.getUniqueId())) return true;
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
        if (e.getClickedBlock() != null) {
            for (Claim claim : claiming) {
                if (claim.m_owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                    Location loc = e.getClickedBlock().getLocation();
                    for (Claim other : claims) {
                        if (inBounds(loc, other.m_start, other.m_end)) {
                            if (other.m_owner != null && !other.m_owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                                e.getPlayer().sendMessage(Component.text("This land is already claimed by ").color(NamedTextColor.RED).append(Component.text(other.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
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
                        claim.m_end = loc;
                        for (Claim other : claims) {
                            if (inBounds(other.m_start, claim.m_start, claim.m_end) || inBounds(other.m_end, claim.m_start, claim.m_end)) {
                                if (other.m_owner != null && !other.m_owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                                    e.getPlayer().sendMessage(Component.text("This land intersects a claim by ").color(NamedTextColor.RED).append(Component.text(other.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
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
        gui.showClaimGui(e.getPlayer());
    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("claims")
                .then(Commands.literal("get-tool")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
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
                        Entity exec = ctx.getSource().getExecutor();
                        final BlockPositionResolver blockPositionResolver = ctx.getArgument("pos", BlockPositionResolver.class);
                        final BlockPosition loc = blockPositionResolver.resolve(ctx.getSource());
                        if (loc == null) {
                            sender.sendMessage("Cannot check owner of claim without position.");
                            return Command.SINGLE_SUCCESS;
                        }
                        Claim res = getClaim(loc.toLocation(exec == null ? getServer().getWorlds().get(0) : exec.getWorld()));
                        if (res == null)
                            sender.sendMessage(Component.text("This land isn't claimed."));
                        else if (res.m_owner == null)
                            sender.sendMessage(Component.text("This claim is owned by the server."));
                        else
                            sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.m_owner.getName()).color(NamedTextColor.GOLD)));
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.argument("world", ArgumentTypes.world())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            Entity exec = ctx.getSource().getExecutor();
                            final BlockPosition loc = ctx.getArgument("pos", BlockPositionResolver.class).resolve(ctx.getSource());
                            final World world = ctx.getArgument("world", World.class);
                            if (loc == null) {
                                sender.sendMessage("Cannot check owner of claim without position.");
                                return Command.SINGLE_SUCCESS;
                            }
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
                                sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.m_owner.getName()).color(NamedTextColor.GOLD)));
                            return Command.SINGLE_SUCCESS;
                        }))
                    )
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
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
                            sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.m_owner.getName()).color(NamedTextColor.GOLD)));
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
        if (!loadSuccess) return;
        setupFile();
        claimsConfig.set("claims", null); // clear previous entries

        int i = 0;
        for (Claim claim : claims) {
            String path = "claims." + i;
            if (claim.m_owner != null) claimsConfig.set(path + ".owner", claim.m_owner.getUniqueId().toString());
            claimsConfig.set(path + ".corner1", serializeLocation(claim.m_start));
            claimsConfig.set(path + ".corner2", serializeLocation(claim.m_end));
            claimsConfig.set(path + ".default", claim.m_default.name());
            claim.m_perms.forEach((player, perm) -> {
                claimsConfig.set(path + ".perms." + player.getUniqueId(), perm.name());
            });
            i++;
        }

        try {
            claimsConfig.save(claimsFile);
        } catch (IOException e) {
            KamsTweaks.getInstance().getLogger().warning(e.getMessage());
        }
    }

    public void loadClaims() {
        setupFile();
        claims.clear();
        if (!claimsConfig.contains("claims")) {
            loadSuccess = true;
            return;
        }
        for (String key : claimsConfig.getConfigurationSection("claims").getKeys(false)) {
            try {
                UUID owner = claimsConfig.getString("claims." + key + ".owner") == null ? null : UUID.fromString(claimsConfig.getString("claims." + key + ".owner"));
                Location corner1 = deserializeLocation(claimsConfig.getString("claims." + key + ".corner1"));
                Location corner2 = deserializeLocation(claimsConfig.getString("claims." + key + ".corner2"));
                Claim claim = new Claim(owner == null ? null : getServer().getOfflinePlayer(owner), corner1, corner2);
                claim.m_default = ClaimPermission.valueOf(claimsConfig.getString("claims." + key + ".default"));
                if (claimsConfig.contains("claims." + key + ".perms")) {
                    for (String uuid : claimsConfig.getConfigurationSection("claims." + key + ".perms").getKeys(false)) {
                        claim.m_perms.put(Bukkit.getOfflinePlayer(UUID.fromString(uuid)), ClaimPermission.valueOf(claimsConfig.getString("claims." + key + ".perms." + uuid)));
                    }
                }

                claims.add(claim);
            } catch (Exception e) {
                KamsTweaks.getInstance().getLogger().info(e.getMessage());
            }
        }
        loadSuccess = true;
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    private Location deserializeLocation(String s) {
        String[] parts = s.split(",");
        return new Location(
                getServer().getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
        );
    }

    public void onExit() {
        for (Entity e : cleanupList) {
            e.remove();
        }
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
}
