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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle.DustOptions;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class LandClaims implements Listener {
    private final List<Claim> claims = new ArrayList<>();
    private final List<Claim> claiming = new ArrayList<>();
    public record Result(boolean permission, Claim claim) {}
    private File claimsFile;
    private FileConfiguration claimsConfig;
    boolean loadSuccess = false;

    private final Inventory inv;
    private final ItemStack claimGuiItem;
    private final ItemStack showClaimsItem;

    public LandClaims() {
        inv = Bukkit.createInventory(null, 9, Component.text("Land Claims"));
        claimGuiItem = createGuiItem(Material.SHIELD, Component.text("Claim").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Claim land.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        showClaimsItem = createGuiItem(Material.GLASS, Component.text("View claims").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false), Component.text("Show claims.").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        inv.setItem(3, claimGuiItem);
        inv.setItem(5, showClaimsItem);
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
                                Location loc = new Location(world, x, y, z);
				DustOptions options = new DustOptions(color, 1.0F); 
                                player.spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0, options);
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

    Result getClaim(Player player, Location where, ClaimPermission perm) {
        for (Claim claim : claims) {
            if (claim.m_start.getWorld() != where.getWorld()) continue;
            if (inBounds(where, claim.m_start, claim.m_end)) {
                if (claim.m_owner != null && claim.m_owner == player) return new Result(true, claim);
                ClaimPermission claimPerm = claim.m_perms.getOrDefault(player, claim.m_default);
                if (claimPerm.compareTo(perm) >= 0) {
                    return new Result(true, claim);
                }
                return new Result(false, claim);
            }
        }
        return new Result(true, null);
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
            if(e.getCurrentItem() == null) return;
            if (e.getCurrentItem().isSimilar(claimGuiItem)) {
                e.getWhoClicked().closeInventory();
                OfflinePlayer plr = Bukkit.getOfflinePlayer(e.getWhoClicked().getUniqueId());
                for (Claim claim : claiming) {
                    if (claim.m_owner == plr) {
                        e.getWhoClicked().sendMessage(Component.text("You're already currently claiming land!").color(NamedTextColor.RED));
                        return;
                    }
                }
                e.getWhoClicked().sendMessage(Component.text("Click the first corner of where you want to claim with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW)).append(Component.text(")").color(NamedTextColor.GOLD)).color(NamedTextColor.GOLD));
                Claim c = new Claim(plr, null, null);
                claiming.add(c);
            } else if (e.getCurrentItem().isSimilar(showClaimsItem)) {
                e.getWhoClicked().closeInventory();
                Player plr = Bukkit.getPlayer(e.getWhoClicked().getUniqueId());
                for (Claim claim : claims) {
			Color c;
			if (claim.m_owner == plr) {
				c = Color.GREEN;
			} else {
				switch(claim.m_perms.getOrDefault(plr, claim.m_default)) {
					case NONE: 
						c = Color.RED;
						break;
					case DOORS:
						c = Color.ORANGE;
						break;
					case INTERACT: 
						c = Color.PURPLE;
						break;
					case BLOCKS:
						c = Color.AQUA;
						break;
					default:
						c = Color.SILVER;
						break;
				}
			}
                    showArea(plr.getPlayer(), claim.m_start, claim.m_end, 1, 100, c);
                }
            }
        }
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

    void handleItem(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            for (Claim claim : claiming) {
                if (claim.m_owner == e.getPlayer()) {
                    Location loc = e.getClickedBlock().getLocation();
                    for (Claim other : claims) {
                        if (other.m_owner != e.getPlayer() && inBounds(loc, other.m_start, other.m_end)) {
                            e.getPlayer().sendMessage(Component.text("This land is already claimed by ").color(NamedTextColor.RED).append(Component.text(other.m_owner == null ? "Unknown" : other.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                            return;
                        }
                    }
                    if (claim.m_start != null) {
                        if (claim.m_start.getWorld() != loc.getWorld()) {
                            e.getPlayer().sendMessage(Component.text("You can't claim across dimensions - go back to the dimension you started in!").color(NamedTextColor.RED));
                            return;
                        }
                        claim.m_end = loc;
                        for (Claim other : claims) {
                            if (other.m_owner != e.getPlayer() && (inBounds(other.m_start, claim.m_start, claim.m_end) || inBounds(other.m_end, claim.m_start, claim.m_end))) {
                                e.getPlayer().sendMessage(Component.text("This claim intersects with a claim by ").color(NamedTextColor.RED).append(Component.text(other.m_owner == null ? "Unknown" : other.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                                return;
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
        e.getPlayer().openInventory(inv);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() != null && e.getItem().isSimilar(ItemManager.createItem(ItemManager.ItemType.CLAIMER)) && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            handleItem(e);
            e.setCancelled(true);
            return;
        }

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

    protected ItemStack createGuiItem(final Material material, final Component name, final Component... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(Arrays.asList(lore));
        item.setItemMeta(meta);

        return item;
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
                        Result res = getClaim(null, loc.toLocation(exec == null ? getServer().getWorlds().get(0) : exec.getWorld()), ClaimPermission.NONE);
                        if (res.claim == null)
                            sender.sendMessage(Component.text("This land isn't claimed."));
                        else if (res.claim.m_owner == null)
                            sender.sendMessage(Component.text("This claim doesn't have an owner."));
                        else
                            sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.claim.m_owner.getName()).color(NamedTextColor.GOLD)));
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
                            Result res = getClaim(null, loc.toLocation(world), ClaimPermission.NONE);
                            if (res.claim == null)
                                sender.sendMessage(Component.text("This land isn't claimed."));
                            else if (res.claim.m_owner == null)
                                sender.sendMessage(Component.text("This claim doesn't have an owner."));
                            else
                                sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.claim.m_owner.getName()).color(NamedTextColor.GOLD)));
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
                        Result res = getClaim(null, executor.getLocation(), ClaimPermission.NONE);
                        if (res.claim == null)
                            sender.sendMessage(Component.text("This land isn't claimed."));
                        else if (res.claim.m_owner == null)
                            sender.sendMessage(Component.text("This claim doesn't have an owner."));
                        else
                            sender.sendMessage(Component.text("This claim is owned by ").append(Component.text(res.claim.m_owner.getName()).color(NamedTextColor.GOLD)));
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
