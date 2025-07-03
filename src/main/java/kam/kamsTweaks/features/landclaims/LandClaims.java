package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class LandClaims implements Listener {
    private final List<Claim> claims = new ArrayList<>();
    public record Result(boolean permission, Claim claim) {}
    private File claimsFile;
    private FileConfiguration claimsConfig;
    boolean loadSuccess = false;

    Result getClaim(Player player, Location where, ClaimPermission perm) {
        for (Claim claim : claims) {
            if (claim.m_start.getWorld() != where.getWorld()) continue;
            int minX = Math.min(claim.m_start.getBlockX(), claim.m_end.getBlockX());
            int maxX = Math.max(claim.m_start.getBlockX(), claim.m_end.getBlockX());
            int minY = Math.min(claim.m_start.getBlockY(), claim.m_end.getBlockY());
            int maxY = Math.max(claim.m_start.getBlockY(), claim.m_end.getBlockY());
            int minZ = Math.min(claim.m_start.getBlockZ(), claim.m_end.getBlockZ());
            int maxZ = Math.max(claim.m_start.getBlockZ(), claim.m_end.getBlockZ());
            if (where.getBlockX() >= minX && where.getBlockX() <= maxX) {
                if (where.getBlockY() >= minY && where.getBlockY() <= maxY) {
                    if (where.getBlockZ() >= minZ && where.getBlockZ() <= maxZ) {
                        if (claim.m_owner != null && claim.m_owner == player) return new Result(true, claim);
                        ClaimPermission claimPerm = claim.m_perms.getOrDefault(player, claim.m_default);
                        if (claimPerm.compareTo(perm) >= 0) {
                            return new Result(true, claim);
                        }
                        return new Result(false, claim);
                    }
                }
            }
        }
        return new Result(true, null);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Result res = getClaim(player, e.getBlock().getLocation(), ClaimPermission.BLOCKS);
        if (!res.permission) {
            player.sendMessage(Component.text("This land is claimed by ").append(Component.text(res.claim.m_owner == null ? "Unknown" : res.claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        Result res = getClaim(player, e.getBlock().getLocation(), ClaimPermission.BLOCKS);
        if (!res.permission) {
            player.sendMessage(Component.text("This land is claimed by ").append(Component.text(res.claim.m_owner == null ? "Unknown" : res.claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = e.getPlayer();
            assert e.getClickedBlock() != null;
            if (e.getClickedBlock().getType().toString().contains("DOOR")) {
                Result res = getClaim(player, e.getClickedBlock().getLocation(), ClaimPermission.DOORS);
                if (!res.permission) {
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(res.claim.m_owner == null ? "Unknown" : res.claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                    e.setCancelled(true);
                }
            } else {
                Result res = getClaim(player, e.getClickedBlock().getLocation(), ClaimPermission.INTERACT);
                if (!res.permission) {
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(res.claim.m_owner == null ? "Unknown" : res.claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onTnt(EntityExplodeEvent e) {
        if (e.getEntity() instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) {
                Result res = getClaim(player, e.getEntity().getLocation(), ClaimPermission.BLOCKS);
                if (!res.permission) {
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(res.claim.m_owner == null ? "Unknown" : res.claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                    e.setCancelled(true);
                    tnt.remove();
                }
            } else {
                Result res = getClaim(null, e.getEntity().getLocation(), ClaimPermission.BLOCKS);
                if (!res.permission) {
                    e.setCancelled(true);
                    tnt.remove();
                }
            }
        }
    }

    @EventHandler
    public void onItemDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) {
                Result res = getClaim(player, tnt.getLocation(), ClaimPermission.BLOCKS);
                if (!res.permission) {
                    event.setCancelled(true);
                }
            } else {
                Result res = getClaim(null, tnt.getLocation(), ClaimPermission.BLOCKS);
                if (!res.permission) {
                    event.setCancelled(true);
                }
            }
        }
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
            UUID owner = claimsConfig.getString("claims." + key + ".owner") == null ? null : UUID.fromString(claimsConfig.getString("claims." + key + ".owner"));
            Location corner1 = deserializeLocation(claimsConfig.getString("claims." + key + ".corner1"));
            Location corner2 = deserializeLocation(claimsConfig.getString("claims." + key + ".corner2"));

            claims.add(new Claim(owner == null ? null : getServer().getOfflinePlayer(owner), corner1, corner2));
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
