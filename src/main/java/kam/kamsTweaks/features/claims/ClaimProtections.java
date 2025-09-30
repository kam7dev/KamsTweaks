package kam.kamsTweaks.features.claims;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.SeedDispenser;
import kam.kamsTweaks.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ClaimProtections implements Listener {
    Claims claims = null;

    public void setup(Claims claims) {
        this.claims = claims;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.getInstance(), hasMessaged::clear, 1, 1);
    }

    final List<UUID> hasMessaged = new ArrayList<>();

    // just a helper cause its nicer
    void message(Player player, Component message) {
        if (hasMessaged.contains(player.getUniqueId()))
            return;
        hasMessaged.add(player.getUniqueId());
        player.sendActionBar(message);
    }

    boolean useClaimTool(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                claims.handleItem(e);
            }
            e.setCancelled(true);
            return true;
        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
                    return true;
                assert e.getClickedBlock() != null;
                Claims.LandClaim res = claims.getLandClaim(e.getClickedBlock().getLocation(), true);
                if (res == null) e.getPlayer().sendMessage(Component.text("This land isn't claimed."));
                else if (res.owner == null)
                    e.getPlayer().sendMessage(Component.text("This claim is owned by the server."));
                else
                    e.getPlayer().sendMessage(Component.text("This claim is owned by ").append(Component.text(res.owner.getName() == null ? "Unknown player" : res.owner.getName()).color(NamedTextColor.GOLD)));
            }
            e.setCancelled(true);
            return true;
        } else if (e.getAction() == Action.LEFT_CLICK_AIR) {
            e.setCancelled(true);
            return true;
        }
        return false;
    }

    /// Land Claims
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getPlayer().getTargetEntity(5) instanceof Creature)
            return;
        if (e.getItem() != null && ItemManager.getType(e.getItem()) == ItemManager.ItemType.CLAIMER) {
            if (useClaimTool(e)) return;
        }
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;

        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            assert e.getClickedBlock() != null;
            //noinspection deprecation
            if (e.getClickedBlock().getType().isInteractable()) {
                Player player = e.getPlayer();
                assert e.getClickedBlock() != null;
                Claims.LandClaim claim = claims.getLandClaim(e.getClickedBlock().getLocation());
                if (claim == null) return;
                if (e.getClickedBlock().getType().toString().contains("DOOR")) {
                    if (!claim.hasPermission(player, Claims.ClaimPermission.INTERACT_DOOR) && !claim.hasPermission(player, Claims.ClaimPermission.INTERACT_BLOCK)) {
                        Component name;
                        if (claim.owner.isOnline()) {
                            name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                        } else {
                            name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                        }
                        message(player, Component.text("You don't have door permissions here! (Claim owned by ").append(name, Component.text(")")));
                        e.setCancelled(true);
                    }
                } else {
                    if (!claim.hasPermission(player, Claims.ClaimPermission.INTERACT_BLOCK)) {
                        Component name;
                        if (claim.owner.isOnline()) {
                            name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                        } else {
                            name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                        }
                        message(player, Component.text("You don't have interaction permissions here! (Claim owned by ").append(name, Component.text(")")));
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        Claims.LandClaim claim = claims.getLandClaim(e.getBlock().getLocation());
        if (claim == null) return;
        if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_BREAK)) {
            Component name;
            if (claim.owner.isOnline()) {
                name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
            } else {
                name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
            }
            message(player, Component.text("You don't have block break permissions here! (Claim owned by ").append(name, Component.text(")")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        Claims.LandClaim claim = claims.getLandClaim(e.getBlock().getLocation());
        if (claim == null) return;
        if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
            Component name;
            if (claim.owner.isOnline()) {
                name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
            } else {
                name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
            }
            message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
            e.setCancelled(true);
        }
    }


    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        Claims.LandClaim claim = claims.getLandClaim(e.getBlock().getLocation());
        if (claim == null) return;
        if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
            Component name;
            if (claim.owner.isOnline()) {
                name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
            } else {
                name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
            }
            e.setCancelled(true);
            message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
        }
    }

    @EventHandler
    public void onBucketPickup(PlayerBucketFillEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        Claims.LandClaim claim = claims.getLandClaim(e.getBlock().getLocation());
        if (claim == null) return;
        if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_BREAK)) {
            Component name;
            if (claim.owner.isOnline()) {
                name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
            } else {
                name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
            }
            e.setCancelled(true);
            message(player, Component.text("You don't have block break permissions here! (Claim owned by ").append(name, Component.text(")")));
        }
    }

    @EventHandler
    public void onDispenser(BlockDispenseEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Claims.LandClaim to = claims.getLandClaim(e.getBlock().getRelative(((Directional) e.getBlock().getBlockData()).getFacing()).getLocation());
        Claims.LandClaim in = claims.getLandClaim(e.getBlock().getLocation());
        if (to == null) return;
        if (!to.hasPermission(in != null ? in.owner : null, Claims.ClaimPermission.BLOCK_PLACE)) {
            String lowered = e.getItem().getType().toString().toLowerCase();
            if (lowered.contains("bucket") || lowered.contains("shulker") || SeedDispenser.matForSeed(e.getItem()) != null) {
                e.setCancelled(true);
            } else {
                switch (e.getItem().getType()) {
                    case FLINT_AND_STEEL, BONE_MEAL, CARVED_PUMPKIN, WITHER_SKELETON_SKULL, SHEARS, TNT, FIRE,
                         ARMOR_STAND:
                        e.setCancelled(true);
                }
            }
        } else {

            // claim in claims check
            if (in != null) {
                var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    String lowered = e.getItem().getType().toString().toLowerCase();
                    if (lowered.contains("bucket") || lowered.contains("shulker")) {
                        e.setCancelled(true);
                    } else {
                        switch (e.getItem().getType()) {
                            case FLINT_AND_STEEL, BONE_MEAL, CARVED_PUMPKIN, WITHER_SKELETON_SKULL, SHEARS, TNT, FIRE,
                                 ARMOR_STAND:
                                e.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onGrow(StructureGrowEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Block tree = event.getLocation().getBlock();
        Claims.LandClaim in = claims.getLandClaim(tree.getLocation());
        for (BlockState state : event.getBlocks()) {
            var block = state.getBlock();
            Claims.LandClaim to = claims.getLandClaim(block.getLocation());
            if (to == null) continue;
            if (!to.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
                event.setCancelled(true);
                return;
            }

            // claim in claims check
            if (in != null) {
                var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onBonemeal(BlockFertilizeEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        Claims.LandClaim in = claims.getLandClaim(e.getBlock().getLocation());
        List<Component> who = new ArrayList<>();
        if (in != null && !in.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
            Component name;
            if (in.owner.isOnline()) {
                name = Objects.requireNonNull(in.owner.getPlayer()).displayName();
            } else {
                name = Component.text(in.owner != null ? Objects.requireNonNull(in.owner.getName()) : "the server").color(NamedTextColor.GOLD);
            }
            who.add(name);
            e.setCancelled(true);
            e.getBlock().getState().update(true, false);
        }

        // funnily enough, setCancelled doesn't affect blocks in this list
        List<BlockState> toProtect = new ArrayList<>();
        for (BlockState blockstate : e.getBlocks()) {
            Block block = blockstate.getBlock();
            Claims.LandClaim to = claims.getLandClaim(block.getLocation());
            if (to == null)
                continue;
            if (!to.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
                Component name;
                if (to.owner.isOnline()) {
                    name = Objects.requireNonNull(to.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(to.owner != null ? Objects.requireNonNull(to.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                if (!who.contains(name))
                    who.add(name);
                toProtect.add(blockstate);
            }
        }

        Component plrs = Component.empty();
        for (var plr : who) {
            if (!plrs.children().isEmpty())
                plrs = plrs.append(Component.text(", "));
            plrs = plrs.append(plr);
        }
        if (player != null && !plrs.children().isEmpty())
            message(player, Component.text("You don't have block place permissions here! (Claim(s) owned by ").append(plrs, Component.text(")")));
        e.getBlocks().removeAll(toProtect);
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;

        InventoryHolder entity = e.getInventory().getHolder();
        if (entity instanceof AbstractHorse || entity instanceof ChestBoat || entity instanceof StorageMinecart) {
            Player player = (Player) e.getPlayer();
            Claims.LandClaim claim = claims.getLandClaim(((Entity) entity).getLocation());
            if (claim == null) return;
            if (!claim.hasPermission(player, Claims.ClaimPermission.INTERACT_BLOCK)) {
                Component name;
                if (claim.owner.isOnline()) {
                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                message(player, Component.text("You don't have interaction permissions here! (Claim owned by ").append(name, Component.text(")")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Entity entity = e.getRightClicked();
        if (entity instanceof ItemFrame || entity instanceof ArmorStand) {
            Player player = e.getPlayer();
            Claims.LandClaim claim = claims.getLandClaim(entity.getLocation());
            Claims.LandClaim origin = null;
            if (entity.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "origin"))) {
                Location loc = LocationUtils.deserializeBlockPos(Objects.requireNonNull(entity.getPersistentDataContainer().get(new NamespacedKey("kamstweaks", "origin"), PersistentDataType.STRING)));
                origin = claims.getLandClaim(loc);
            }
            if (claim == null) return;
            if (!claim.hasPermission(player, Claims.ClaimPermission.INTERACT_BLOCK) && (origin != null && !origin.hasPermission(player, Claims.ClaimPermission.INTERACT_BLOCK))) {
                Component name;
                if (claim.owner.isOnline()) {
                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                message(player, Component.text("You don't have interaction permissions here! (Claim owned by ").append(name, Component.text(")")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangPlace(HangingPlaceEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (e.getPlayer() instanceof Player player) {
            Claims.LandClaim claim = claims.getLandClaim(e.getEntity().getLocation());
            if (claim == null) return;
            if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
                Component name;
                if (claim.owner.isOnline()) {
                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangDestroy(HangingBreakByEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (e.getRemover() instanceof Player player) {
            Claims.LandClaim claim = claims.getLandClaim(e.getEntity().getLocation());
            if (claim == null) return;
            if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
                Component name;
                if (claim.owner.isOnline()) {
                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (e.getPlayer() instanceof Player player) {
            Claims.LandClaim claim = claims.getLandClaim(e.getEntity().getLocation());
            if (claim == null) return;
            if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
                Component name;
                if (claim.owner.isOnline()) {
                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
                e.setCancelled(true);
            } else {
                e.getEntity().getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "origin"), PersistentDataType.STRING, LocationUtils.serializeBlockPos(e.getEntity().getLocation()));
            }
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Entity entity = e.getVehicle();
        if (!(entity instanceof AbstractHorse || entity instanceof ChestBoat || entity instanceof StorageMinecart))
            return;
        Claims.LandClaim claim = claims.getLandClaim(e.getVehicle().getLocation());
        if (claim == null) return;
        if (e.getAttacker() instanceof Player player) {
            if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_BREAK)) {
                Component name;
                if (claim.owner.isOnline()) {
                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                message(player, Component.text("You don't have block break permissions here! (Claim owned by ").append(name, Component.text(")")));
                e.setCancelled(true);
            }
        } else {
            if (!claim.hasPermission(null, Claims.ClaimPermission.BLOCK_BREAK)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Entity entity = e.getEntity();
        if (!(entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof AbstractHorse
                || entity instanceof ChestBoat || entity instanceof StorageMinecart))
            return;
        if (e.getDamager() instanceof Player player) {
            Claims.LandClaim claim = claims.getLandClaim(e.getEntity().getLocation());
            if (claim == null) return;
            Claims.LandClaim origin = null;
            if (entity.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "origin"))) {
                Location loc = LocationUtils.deserializeBlockPos(Objects.requireNonNull(entity.getPersistentDataContainer().get(new NamespacedKey("kamstweaks", "origin"), PersistentDataType.STRING)));
                origin = claims.getLandClaim(loc);
            }
            if (!claim.hasPermission(player, Claims.ClaimPermission.BLOCK_BREAK) && (origin != null && !origin.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE))) {
                Component name;
                if (claim.owner.isOnline()) {
                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                message(player, Component.text("You don't have block break permissions here! (Claim owned by ").append(name, Component.text(")")));
                e.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void onEntityInteractAt(PlayerInteractAtEntityEvent e) {
        onEntityInteract(e);
    }

    @EventHandler
    public void onKaboom(EntityExplodeEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (e.getEntity() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            List<Component> who = new ArrayList<>();
            List<Block> toProtect = new ArrayList<>();
            for (Block block : e.blockList()) {
                Claims.LandClaim to = claims.getLandClaim(block.getLocation());
                if (to == null)
                    continue;
                if (!to.hasPermission(player, Claims.ClaimPermission.BLOCK_BREAK)) {
                    Component name;
                    if (to.owner != null && to.owner.isOnline()) {
                        name = Objects.requireNonNull(to.owner.getPlayer()).displayName();
                    } else {
                        name = Component.text(to.owner != null ? Objects.requireNonNull(to.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                    }
                    if (!who.contains(name))
                        who.add(name);
                    toProtect.add(block);
                }
            }
            Component plrs = Component.empty();
            for (var plr : who) {
                if (!plrs.children().isEmpty())
                    plrs = plrs.append(Component.text(", "));
                plrs = plrs.append(plr);
            }
            if (!plrs.children().isEmpty())
                message(player, Component.text("You don't have block break permissions here! (Claim(s) owned by ").append(plrs, Component.text(")")));
            e.blockList().removeAll(toProtect);
            for (Block block : toProtect) {
                block.getState().update(true, false);
            }
        } else {
            List<Block> toProtect = new ArrayList<>();
            for (Block block : e.blockList()) {
                Claims.LandClaim to = claims.getLandClaim(block.getLocation());
                if (to != null && !to.hasPermission(null, Claims.ClaimPermission.BLOCK_BREAK)) {
                    toProtect.add(block);
                }
            }
            e.blockList().removeAll(toProtect);
            for (Block block : toProtect) {
                block.getState().update(true, false);
            }
        }
    }

    @EventHandler
    public void onKaboom(org.bukkit.event.block.BlockExplodeEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        List<Block> toProtect = new ArrayList<>();
        Claims.LandClaim in = claims.getLandClaim(e.getBlock().getLocation());
        for (Block block : e.blockList()) {
            Claims.LandClaim to = claims.getLandClaim(block.getLocation());
            if (to == null) continue;
            if (!to.hasPermission(in != null ? in.owner : null, Claims.ClaimPermission.BLOCK_BREAK)) {
                toProtect.add(block);
                continue;
            }

            // claim in claims check
            if (in != null) {
                var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    toProtect.add(block);
                }
            }
        }
        e.blockList().removeAll(toProtect);
        for (Block block : toProtect) {
            block.getState().update(true, false);
        }
    }

    @EventHandler
    public void onSponge(org.bukkit.event.block.SpongeAbsorbEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        List<BlockState> toProtect = new ArrayList<>();
        Claims.LandClaim in = claims.getLandClaim(e.getBlock().getLocation());
        for (BlockState blockstate : e.getBlocks()) {
            Block block = blockstate.getBlock();
            Claims.LandClaim to = claims.getLandClaim(block.getLocation());
            if (to == null) continue;
            if (!to.hasPermission(in != null ? in.owner : null, Claims.ClaimPermission.BLOCK_BREAK)) {
                toProtect.add(blockstate);
                continue;
            }

            // claim in claims check
            if (in != null) {
                var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    toProtect.add(blockstate);
                }
            }
        }
        e.getBlocks().removeAll(toProtect);
    }

    @EventHandler
    public void onFlow(org.bukkit.event.block.BlockFromToEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Claims.LandClaim to = claims.getLandClaim(event.getToBlock().getLocation());
        Claims.LandClaim in = claims.getLandClaim(event.getBlock().getLocation());
        if (to == null) return;
        if (!to.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
            event.setCancelled(true);
            return;
        }

        // claim in claims check
        if (in != null) {
            var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
            var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
            if (placeIn != placeTo && !placeTo) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBurn(org.bukkit.event.block.BlockBurnEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Claims.LandClaim in = null;
        if (event.getIgnitingBlock() != null) {
            in = claims.getLandClaim(event.getIgnitingBlock().getLocation());
        }
        Claims.LandClaim to = claims.getLandClaim(event.getBlock().getLocation());
        if (to == null) return;
        if (!to.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
            event.setCancelled(true);
            return;
        }

        // claim in claims check
        if (in != null) {
            var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
            var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
            if (placeIn != placeTo && !placeTo) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        Claims.LandClaim to = claims.getLandClaim(event.getBlock().getLocation());
        switch (event.getCause()) {
            case LAVA, SPREAD -> {
                if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
                    return;
                Claims.LandClaim in = event.getIgnitingBlock() != null ? claims.getLandClaim(event.getIgnitingBlock().getLocation()) : null;
                if (to == null) return;
                if (!to.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }

                // claim in claims check
                if (in != null) {
                    var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                    }
                }
            }
            case LIGHTNING, ENDER_CRYSTAL, EXPLOSION -> {
                if (to != null && !to.hasPermission(null, Claims.ClaimPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                }
            }
            case FIREBALL, ARROW -> {
                if (event.getIgnitingEntity() instanceof Projectile projectile) {
                    Object shooter = projectile.getShooter();
                    if (shooter instanceof Player player) {
                        if (to != null && !to.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
                            Component name = to.owner != null ? Component.text(to.owner.getName() == null ? "Unknown player" : to.owner.getName()).color(NamedTextColor.GOLD) : Component.text("the server").color(NamedTextColor.GOLD);
                            message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
                            event.setCancelled(true);
                        }
                    } else {
                        if (to != null && !to.hasPermission(null, Claims.ClaimPermission.BLOCK_PLACE)) {
                            event.setCancelled(true);
                        }
                    }
                } else {
                    Claims.LandClaim claim = claims.getLandClaim(event.getBlock().getLocation());
                    if (event.getIgnitingEntity() instanceof Player player) {
                        if (claim != null && !claim.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
                            Component name = claim.owner != null ? Component.text(claim.owner.getName() == null ? "Unknown player" : claim.owner.getName()).color(NamedTextColor.GOLD) : Component.text("the server").color(NamedTextColor.GOLD);
                            message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
                            event.setCancelled(true);
                        }
                    } else {
                        if (claim != null && !claim.hasPermission(null, Claims.ClaimPermission.BLOCK_PLACE)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
            case FLINT_AND_STEEL -> {
                if (event.getIgnitingEntity() instanceof Player player) {
                    if (to != null && !to.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
                        Component name = to.owner != null ? Component.text(to.owner.getName() == null ? "Unknown player" : to.owner.getName()).color(NamedTextColor.GOLD) : Component.text("the server").color(NamedTextColor.GOLD);
                        message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
                        event.setCancelled(true);
                    }
                } else {
                    if (to != null && !to.hasPermission(null, Claims.ClaimPermission.BLOCK_PLACE)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonExtend(org.bukkit.event.block.BlockPistonExtendEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Block piston = event.getBlock();
        Directional dir = (Directional) piston.getBlockData();
        BlockFace face = dir.getFacing();
        Claims.LandClaim in = claims.getLandClaim(piston.getLocation());

        Block front = piston.getRelative(face);
        Claims.LandClaim to = claims.getLandClaim(front.getLocation());
        if (to != null) {
            if (!to.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
                event.setCancelled(true);
                return;
            }

            if (in != null) {
                var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        for (Block block : event.getBlocks()) {
            Block target = block.getRelative(face);
            Claims.LandClaim bIn = claims.getLandClaim(target.getLocation());
            if (bIn != null) {
                if (!bIn.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }
                if (in != null) {
                    var placeTo = bIn.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            Claims.LandClaim bTo = claims.getLandClaim(target.getLocation());
            if (bTo != null) {
                if (!bTo.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }
                if (in != null) {
                    var placeTo = bTo.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonRetract(org.bukkit.event.block.BlockPistonRetractEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Block piston = event.getBlock();
        Directional dir = (Directional) piston.getBlockData();
        BlockFace face = dir.getFacing().getOppositeFace();

        Claims.LandClaim in = claims.getLandClaim(piston.getLocation());
        Block front = piston.getRelative(face);
        Claims.LandClaim to = claims.getLandClaim(front.getLocation());
        if (to != null) {
            if (!to.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
                event.setCancelled(true);
                return;
            }

            if (in != null) {
                var placeTo = to.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                if (placeIn != placeTo && !placeTo) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        for (Block block : event.getBlocks()) {
            Block target = block.getRelative(face);
            Claims.LandClaim bIn = claims.getLandClaim(block.getLocation());
            if (bIn != null) {
                if (!bIn.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }
                if (in != null) {
                    var placeTo = bIn.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            Claims.LandClaim bTo = claims.getLandClaim(target.getLocation());
            if (bTo != null) {
                if (!bTo.hasPermission(in == null ? null : in.owner, Claims.ClaimPermission.BLOCK_PLACE)) {
                    event.setCancelled(true);
                    return;
                }
                if (in != null) {
                    var placeTo = bTo.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    var placeIn = in.defaults.contains(Claims.ClaimPermission.BLOCK_PLACE);
                    if (placeIn != placeTo && !placeTo) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemDamage(EntityDamageByEntityEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (!(event.getEntity() instanceof Item)) return;
        Entity damager = event.getDamager();
        Claims.LandClaim claim = claims.getLandClaim(event.getEntity().getLocation());
        if (damager instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) {
                if (claim != null && !claim.hasPermission(player, Claims.ClaimPermission.BLOCK_BREAK)) {
                    event.setCancelled(true);
                }
            } else {
                if (claim != null && !claim.hasPermission(null, Claims.ClaimPermission.BLOCK_BREAK)) {
                    event.setCancelled(true);
                }
            }
        } else if (damager instanceof EnderCrystal || damager instanceof Creeper || damager instanceof ExplosiveMinecart) {
            if (claim != null && !claim.hasPermission(null, Claims.ClaimPermission.BLOCK_BREAK)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMobTrample(EntityInteractEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (event.getEntity() instanceof Player)
            return;
        if (event.getBlock().getType() != org.bukkit.Material.FARMLAND)
            return;
        Claims.LandClaim claim = claims.getLandClaim(event.getBlock().getLocation());
        if (claim != null && !claim.hasPermission(null, Claims.ClaimPermission.BLOCK_BREAK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerTrample(PlayerInteractEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (event.getAction() != Action.PHYSICAL)
            return;
        if (event.getClickedBlock() == null)
            return;
        Player player = event.getPlayer();
        Claims.LandClaim claim = claims.getLandClaim(event.getClickedBlock().getLocation());
        if (claim != null && !claim.hasPermission(player, Claims.ClaimPermission.BLOCK_BREAK)) {
            Component name = claim.owner != null ? Component.text(claim.owner.getName() == null ? "Unknown player" : claim.owner.getName()).color(NamedTextColor.GOLD) : Component.text("the server").color(NamedTextColor.GOLD);
            message(player, Component.text("You don't have block break permissions here! (Claim owned by ").append(name, Component.text(")")));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (event.getEntityType() == EntityType.SHEEP)
            return;
        Claims.LandClaim claim = claims.getLandClaim(event.getBlock().getLocation());
        if (event.getEntity() instanceof Player player) {
            if (claim != null && !claim.hasPermission(player, Claims.ClaimPermission.BLOCK_BREAK)) {
                Component name = claim.owner != null ? Component.text(claim.owner.getName() == null ? "Unknown player" : claim.owner.getName()).color(NamedTextColor.GOLD) : Component.text("the server").color(NamedTextColor.GOLD);
                message(player, Component.text("You don't have block break permissions here! (Claim owned by ").append(name, Component.text(")")));
                event.setCancelled(true);
            }
        }
        if (event.getEntity() instanceof FallingBlock fb) {
            if (event.getTo() == org.bukkit.Material.AIR && !fb.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "startlocation"), PersistentDataType.STRING)) {
                fb.getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "startlocation"), PersistentDataType.STRING, LocationUtils.serializeBlockPos(fb.getLocation()));
            } else {
                Claims.LandClaim orig = null;
                if (fb.getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "startlocation"), PersistentDataType.STRING)) {
                    Location origin = LocationUtils.deserializeBlockPos(Objects.requireNonNull(fb.getPersistentDataContainer().get(new NamespacedKey("kamstweaks", "startlocation"), PersistentDataType.STRING)));
                    orig = claims.getLandClaim(origin);
                }
                if (claim != null && !claim.hasPermission(orig != null ? orig.owner : null, Claims.ClaimPermission.BLOCK_BREAK)) {
                    event.setCancelled(true);
                    org.bukkit.inventory.ItemStack drop = new org.bukkit.inventory.ItemStack(event.getBlockData().getMaterial());
                    org.bukkit.inventory.meta.ItemMeta meta = drop.getItemMeta();
                    if (meta instanceof org.bukkit.inventory.meta.BlockDataMeta bdMeta) {
                        bdMeta.setBlockData(event.getBlockData());
                        drop.setItemMeta(bdMeta);
                    }
                    fb.getWorld().dropItemNaturally(fb.getLocation(), drop);
                    fb.remove();
                }
            }
        }
    }

    @EventHandler
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Claims.LandClaim claim = claims.getLandClaim(event.getBlock().getLocation());
        if (event.getEntity() instanceof Player player) {
            if (claim != null && !claim.hasPermission(player, Claims.ClaimPermission.BLOCK_PLACE)) {
                Component name = claim.owner != null ? Component.text(claim.owner.getName() == null ? "Unknown player" : claim.owner.getName()).color(NamedTextColor.GOLD) : Component.text("the server").color(NamedTextColor.GOLD);
                message(player, Component.text("You don't have block place permissions here! (Claim owned by ").append(name, Component.text(")")));
                event.setCancelled(true);
            }
        } else {
            if (claim != null && !claim.hasPermission(null, Claims.ClaimPermission.BLOCK_PLACE)) {
                event.setCancelled(true);
            }
        }
    }

    /// Entity Claims
    @EventHandler(priority = EventPriority.HIGH)
    public void EConEntityInteract(PlayerInteractEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("entity-claims.enabled", true)) return;
        if (e.getRightClicked() instanceof Mob c) {
            if (ItemManager.getType(e.getPlayer().getInventory().getItemInMainHand()) == ItemManager.ItemType.CLAIMER) {
                e.setCancelled(true);
                if (claims.entityClaims.containsKey(e.getRightClicked().getUniqueId())) {
                    OfflinePlayer owner = claims.entityClaims.get(e.getRightClicked().getUniqueId()).owner;
                    if (owner != null && owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                        claims.dialogGui.openECPage(e.getPlayer());
                        return;
                    }
                    if (hasMessaged.contains(e.getPlayer().getUniqueId())) return;
                    hasMessaged.add(e.getPlayer().getUniqueId());
                    e.getPlayer().sendMessage(Component.text("This entity is already claimed by ").append(Component.text(owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                    return;
                }
                if (!e.getPlayer().hasPermission("kamstweaks.landclaims.claim")) {
                    e.getPlayer().sendMessage(Component.text("You do not have permission to claim entities.").color(NamedTextColor.RED));
                    return;
                }
                int count = 0;
                for (var claim : claims.entityClaims.values()) {
                    if (claim.owner.getUniqueId().equals(e.getPlayer().getUniqueId())) count += 1;
                }
                var max = KamsTweaks.getInstance().getConfig().getInt("entity-claims.max-claims", 1000);
                if (count >= max) {
                    e.getPlayer().sendMessage(Component.text("You already have the max number of claims! (" + count + "/" + max + ")").color(NamedTextColor.RED));
                    return;
                }
                claims.dialogGui.openECPage(e.getPlayer(), e.getRightClicked());
                return;
            }
            var claim = claims.entityClaims.get(c.getUniqueId());
            if (claim == null) return;
            if (!claim.hasPermission(e.getPlayer(), Claims.ClaimPermission.INTERACT_ENTITY)) {
                Component name;
                if (claim.owner.isOnline()) {
                    name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                } else {
                    name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                }
                message(e.getPlayer(), Component.text("You don't have permission to interact with this entity! (Entity claimed by ").append(name, Component.text(")")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("entity-claims.enabled", true)) return;
        Claims.EntityClaim claim = claims.entityClaims.get(event.getEntity().getUniqueId());
        if (claim == null) return;
        switch (event.getCause()) {
            case ENTITY_ATTACK, ENTITY_EXPLOSION, ENTITY_SWEEP_ATTACK -> {
                if (event.getEntity() instanceof Mob mob) {
                    if (mob.getTarget() != null) {
                        if (mob.getTarget() == event.getDamageSource().getCausingEntity()) {
                            return;
                        }
                    }
                }
                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                    if (claim.hasPermission(player, Claims.ClaimPermission.DAMAGE_ENTITY)) return;
                    Component name;
                    if (claim.owner.isOnline()) {
                        name = Objects.requireNonNull(claim.owner.getPlayer()).displayName();
                    } else {
                        name = Component.text(claim.owner != null ? Objects.requireNonNull(claim.owner.getName()) : "the server").color(NamedTextColor.GOLD);
                    }
                    message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(name, Component.text(")")));
                    event.setCancelled(true);
                } else {
                    if (claim.hasPermission(null, Claims.ClaimPermission.DAMAGE_ENTITY)) return;
                    event.setCancelled(true);
                }
            }
            default -> event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() == null) return;
        if (event.getEntity() instanceof Mob entity) {
            var claim = claims.getEntityClaim(entity);
            if (claim != null) {
                if (!claim.canAggro) event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBurn(EntityCombustEvent event) {
        if (event.getEntity() instanceof Mob entity) {
            var claim = claims.getEntityClaim(entity);
            if (claim != null) {
                if (!claim.canAggro) event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void entityDie(EntityDeathEvent e) {
        claims.entityClaims.remove(e.getEntity().getUniqueId());
        if (e.getEntity() instanceof EnderDragon) {
            claims.disabledClaims.put(e.getEntity().getWorld(), 5 * 60);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getCaught() instanceof Mob mob) {
            var claim = claims.getEntityClaim(mob);
            if (claim == null || !claim.hasPermission(e.getPlayer(), Claims.ClaimPermission.INTERACT_ENTITY)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoinWorld(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getWorld().getEnderDragonBattle() != null && event.getWorld().getEnderDragonBattle().getEnderDragon() != null) {
                player.sendMessage(Component.text("Claims are currently disabled in this world due to an ongoing dragon fight. They will be re-enabled 5 minutes after the fight.").color(NamedTextColor.YELLOW));
            } else if (claims.disabledClaims.containsKey(event.getWorld())) {
                player.sendMessage(Component.text("Claims are currently disabled in this world due to a recent dragon fight. They will be re-enabled in " + claims.disabledClaims.get(event.getWorld()) + " seconds.").color(NamedTextColor.YELLOW));
            }
        }
    }
}
