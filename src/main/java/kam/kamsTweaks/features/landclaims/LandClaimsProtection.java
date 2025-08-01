package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LandClaimsProtection implements Listener {
    LandClaims lc;

    public LandClaimsProtection(LandClaims lc) {
        this.lc = lc;
    }

    final List<UUID> hasMessaged = new ArrayList<>();

    void message(Player player, String owner, boolean bypass) {
        if (hasMessaged.contains(player.getUniqueId()))
            return;
        hasMessaged.add(player.getUniqueId());
        if (bypass)
            player.sendMessage(
                    Component.text("This land is claimed by ").append(Component.text(owner).color(NamedTextColor.GOLD))
                            .append(Component.text(", but you are bypassing the claim.")));
        else
            player.sendMessage(Component.text("This land is claimed by ")
                    .append(Component.text(owner).color(NamedTextColor.GOLD)).append(Component.text(".")));
    }

    public void init() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.getInstance(), hasMessaged::clear, 1, 1);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        LandClaims.Claim claim = lc.getClaim(e.getBlock().getLocation());
        if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
            // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
            //     message(player,
            //             claim.m_owner == null ? "the server"
            //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
            //             true);
            //     return;
            // }
            message(player, claim.m_owner == null ? "the server"
                    : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(), false);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Player player = e.getPlayer();
        LandClaims.Claim claim = lc.getClaim(e.getBlock().getLocation());
        if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
            // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
            //     message(player,
            //             claim.m_owner == null ? "the server"
            //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
            //             true);
            //     return;
            // }
            message(player, claim.m_owner == null ? "the server"
                    : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(), false);
            e.setCancelled(true);
        }
    }

    // Slightly altered from https://www.spigotmc.org/threads/check-if-a-block-is-interactable.535861/
    // Originally by LoneDev

    public static boolean isInteractable(Block block){
        Material type = block.getType();
        @SuppressWarnings("deprecation")
        boolean interactable = type.isInteractable();
        if (!interactable) return false;
        switch (type) {
            case ACACIA_STAIRS:
            case ANDESITE_STAIRS:
            case BAMBOO_STAIRS:
            case BAMBOO_MOSAIC_STAIRS:
            case BIRCH_STAIRS:
            case BLACKSTONE_STAIRS:
            case BRICK_STAIRS:
            case COBBLESTONE_STAIRS:
            case CRIMSON_STAIRS:
            case DARK_OAK_STAIRS:
            case DARK_PRISMARINE_STAIRS:
            case DIORITE_STAIRS:
            case END_STONE_BRICK_STAIRS:
            case GRANITE_STAIRS:
            case JUNGLE_STAIRS:
            case MOSSY_COBBLESTONE_STAIRS:
            case MOSSY_STONE_BRICK_STAIRS:
            case NETHER_BRICK_STAIRS:
            case OAK_STAIRS:
            case PALE_OAK_STAIRS:
            case POLISHED_ANDESITE_STAIRS:
            case POLISHED_BLACKSTONE_BRICK_STAIRS:
            case POLISHED_BLACKSTONE_STAIRS:
            case POLISHED_DIORITE_STAIRS:
            case POLISHED_GRANITE_STAIRS:
            case PRISMARINE_BRICK_STAIRS:
            case PRISMARINE_STAIRS:
            case PURPUR_STAIRS:
            case QUARTZ_STAIRS:
            case RED_NETHER_BRICK_STAIRS:
            case RED_SANDSTONE_STAIRS:
            case SANDSTONE_STAIRS:
            case SMOOTH_QUARTZ_STAIRS:
            case SMOOTH_RED_SANDSTONE_STAIRS:
            case SMOOTH_SANDSTONE_STAIRS:
            case SPRUCE_STAIRS:
            case STONE_BRICK_STAIRS:
            case STONE_STAIRS:
            case WARPED_STAIRS:
            case ACACIA_FENCE:
            case BAMBOO_FENCE:
            case BIRCH_FENCE:
            case CRIMSON_FENCE:
            case DARK_OAK_FENCE:
            case JUNGLE_FENCE:
            case MOVING_PISTON:
            case NETHER_BRICK_FENCE:
            case OAK_FENCE:
            case PALE_OAK_FENCE:
            case SPRUCE_FENCE:
            case WARPED_FENCE:
                return false;
            default: return true;
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getPlayer().getTargetEntity(5) instanceof Creature)
            return;
        if (e.getItem() != null && ItemManager.getType(e.getItem()) == ItemManager.ItemType.CLAIMER) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
                    lc.handleItem(e);
                e.setCancelled(true);
                return;
            } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
                        return;
                    assert e.getClickedBlock() != null;
                    LandClaims.Claim res = lc.getClaim(e.getClickedBlock().getLocation());
                    if (res == null)
                        e.getPlayer().sendMessage(Component.text("This land isn't claimed."));
                    else if (res.m_owner == null)
                        e.getPlayer().sendMessage(Component.text("This claim is owned by the server."));
                    else
                        e.getPlayer().sendMessage(Component.text("This claim is owned by ")
                                .append(Component.text(
                                                res.m_owner.getName() == null ? "Unknown player" : res.m_owner.getName())
                                        .color(NamedTextColor.GOLD)));
                }
                e.setCancelled(true);
                return;
            } else if (e.getAction() == Action.LEFT_CLICK_AIR) {
                e.setCancelled(true);
                return;
            }
        }
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && isInteractable(e.getClickedBlock())) {
            Player player = e.getPlayer();
            assert e.getClickedBlock() != null;
            LandClaims.Claim claim = lc.getClaim(e.getClickedBlock().getLocation());
            if (e.getClickedBlock().getType().toString().contains("DOOR")) {
                if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.DOORS)) {
                    // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                    //     message(player, claim.m_owner == null ? "the server"
                    //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                    //             true);
                    //     return;
                    // }
                    message(player,
                            claim.m_owner == null ? "the server"
                                    : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                            false);
                    e.setCancelled(true);
                }
            } else {
                if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.INTERACT)) {
                    // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                    //     message(player, claim.m_owner == null ? "the server"
                    //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                    //             true);
                    //     return;
                    // }
                    message(player,
                            claim.m_owner == null ? "the server"
                                    : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                            false);
                    e.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        InventoryHolder entity = e.getInventory().getHolder();
        if (entity instanceof AbstractHorse || entity instanceof ChestBoat || entity instanceof StorageMinecart) {
            Player player = (Player) e.getPlayer();
            LandClaims.Claim claim = lc.getClaim(((Entity) entity).getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.INTERACT)) {
                // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                //     message(player,
                //             claim.m_owner == null ? "the server"
                //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                //             true);
                //     return;
                // }
                message(player,
                        claim.m_owner == null ? "the server"
                                : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                        false);
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
            LandClaims.Claim claim = lc.getClaim(entity.getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.INTERACT)) {
                // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                //     message(player,
                //             claim.m_owner == null ? "the server"
                //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                //             true);
                //     return;
                // }
                message(player,
                        claim.m_owner == null ? "the server"
                                : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                        false);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangDestroy(HangingBreakByEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (e.getRemover() instanceof Player player) {
            LandClaims.Claim claim = lc.getClaim(e.getEntity().getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                //     message(player,
                //             claim.m_owner == null ? "the server"
                //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                //             true);
                //     return;
                // }
                message(player,
                        claim.m_owner == null ? "the server"
                                : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                        false);
                e.setCancelled(true);
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
        if (e.getAttacker() instanceof Player player) {
            LandClaims.Claim claim = lc.getClaim(e.getVehicle().getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                //     message(player,
                //             claim.m_owner == null ? "the server"
                //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                //             true);
                //     return;
                // }
                message(player,
                        claim.m_owner == null ? "the server"
                                : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                        false);
                e.setCancelled(true);
            }
        } else {
            LandClaims.Claim claim = lc.getClaim(e.getVehicle().getLocation());
            if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
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
            LandClaims.Claim claim = lc.getClaim(e.getEntity().getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                //     message(player,
                //             claim.m_owner == null ? "the server"
                //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                //             true);
                //     return;
                // }
                message(player,
                        claim.m_owner == null ? "the server"
                                : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                        false);
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
            List<String> who = new ArrayList<>();
            // boolean bypass = player.hasPermission("kamstweaks.landclaims.bypass");
            List<Block> toProtect = new ArrayList<>();
            for (Block block : e.blockList()) {
                LandClaims.Claim claim = lc.getClaim(block.getLocation());
                if (claim == null)
                    continue;
                if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    var a = claim.m_owner == null ? "the server"
                            : (claim.m_owner.getName() == null || claim.m_owner.getName().isEmpty())
                            ? "Unknown player"
                            : claim.m_owner.getName();
                    if (!who.contains(a))
                        who.add(a);
                    toProtect.add(block);
                }
            }
            StringBuilder plrs = new StringBuilder();
            for (var plr : who) {
                if (plr.isEmpty())
                    continue;
                if (!plrs.isEmpty())
                    plrs.append(", ");
                plrs.append(plr);
            }
            // if (bypass && !plrs.isEmpty()) {
            //     message(player, plrs.toString(), true);
            //     return;
            // }
            message(player, plrs.toString(), false);
            e.blockList().removeAll(toProtect);
            for (Block block : toProtect) {
                block.getState().update(true, false);
            }
        } else {
            List<Block> toProtect = new ArrayList<>();
            for (Block block : e.blockList()) {
                LandClaims.Claim claim = lc.getClaim(block.getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
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
    public void onKaboom(BlockExplodeEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        List<Block> toProtect = new ArrayList<>();
        for (Block block : e.blockList()) {
            LandClaims.Claim claim = lc.getClaim(block.getLocation());
            if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                toProtect.add(block);
            }
        }
        e.blockList().removeAll(toProtect);
        for (Block block : toProtect) {
            block.getState().update(true, false);
        }
    }

    @EventHandler
    public void onFlow(BlockFromToEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        LandClaims.Claim in = lc.getClaim(event.getToBlock().getLocation());
        LandClaims.Claim to = lc.getClaim(event.getBlock().getLocation());
        if (in != null
                && !lc.hasPermission(to == null ? null : to.m_owner, in, LandClaims.ClaimPermission.BLOCKS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        LandClaims.Claim in = null;
        if (event.getIgnitingBlock() != null) {
            in = lc.getClaim(event.getIgnitingBlock().getLocation());
        }
        LandClaims.Claim to = lc.getClaim(event.getBlock().getLocation());
        if (to != null
                && !lc.hasPermission(in == null ? null : in.m_owner, to, LandClaims.ClaimPermission.BLOCKS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        switch (event.getCause()) {
            case LAVA, SPREAD -> {
                if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
                    return;
                assert event.getIgnitingBlock() != null;
                LandClaims.Claim in = lc.getClaim(event.getBlock().getLocation());
                LandClaims.Claim to = lc.getClaim(event.getIgnitingBlock().getLocation());
                if (in != null && !lc.hasPermission(to == null ? null : to.m_owner, in,
                        LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            }
            case LIGHTNING, ENDER_CRYSTAL, EXPLOSION -> {
                LandClaims.Claim claim = lc.getClaim(event.getBlock().getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            }
            case FIREBALL, ARROW -> {
                if (event.getIgnitingEntity() instanceof Projectile projectile) {
                    var shooter = projectile.getShooter();
                    LandClaims.Claim claim = lc.getClaim(event.getBlock().getLocation());
                    if (shooter instanceof Player player) {
                        if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                            // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                            //     message(player, claim.m_owner == null ? "the server"
                            //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                            //             true);
                            //     return;
                            // }
                            message(player, claim.m_owner == null ? "the server"
                                            : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                                    false);
                            event.setCancelled(true);
                        }
                    } else {
                        if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                            event.setCancelled(true);
                        }
                    }
                } else {
                    LandClaims.Claim claim = lc.getClaim(event.getBlock().getLocation());
                    if (event.getIgnitingEntity() instanceof Player player) {
                        if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                            // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                            //     message(player, claim.m_owner == null ? "the server"
                            //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                            //             true);
                            //     return;
                            // }
                            message(player, claim.m_owner == null ? "the server"
                                            : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                                    false);
                            event.setCancelled(true);
                        }
                    } else {
                        if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
            case FLINT_AND_STEEL -> {
                LandClaims.Claim claim = lc.getClaim(event.getBlock().getLocation());
                if (event.getIgnitingEntity() instanceof Player player) {
                    if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                        // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                        //     message(player, claim.m_owner == null ? "the server"
                        //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                        //             true);
                        //     return;
                        // }
                        message(player, claim.m_owner == null ? "the server"
                                        : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                                false);
                        event.setCancelled(true);
                    }
                } else {
                    if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Block piston = event.getBlock();
        LandClaims.Claim claim = lc.getClaim(piston.getLocation());

        {
            LandClaims.Claim to = lc
                    .getClaim(piston.getRelative(((Directional) piston.getBlockData()).getFacing()).getLocation());
            if (to != null && !lc.hasPermission(claim == null ? null : claim.m_owner, to,
                    LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
            LandClaims.Claim in = lc
                    .getClaim(piston.getRelative(((Directional) piston.getBlockData()).getFacing()).getLocation());
            if (in != null && !lc.hasPermission(claim == null ? null : claim.m_owner, in,
                    LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
        }

        for (Block block : event.getBlocks()) {
            LandClaims.Claim to = lc
                    .getClaim(block.getRelative(((Directional) piston.getBlockData()).getFacing()).getLocation());
            if (to != null && !lc.hasPermission(claim == null ? null : claim.m_owner, to,
                    LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
            LandClaims.Claim in = lc
                    .getClaim(block.getRelative(((Directional) piston.getBlockData()).getFacing()).getLocation());
            if (in != null && !lc.hasPermission(claim == null ? null : claim.m_owner, in,
                    LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        Block piston = event.getBlock();
        LandClaims.Claim claim = lc.getClaim(piston.getLocation());

        {
            LandClaims.Claim to = lc.getClaim(
                    piston.getRelative(((Directional) piston.getBlockData()).getFacing().getOppositeFace())
                            .getLocation());
            if (to != null && !lc.hasPermission(claim == null ? null : claim.m_owner, to,
                    LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
            LandClaims.Claim in = lc.getClaim(
                    piston.getRelative(((Directional) piston.getBlockData()).getFacing().getOppositeFace())
                            .getLocation());
            if (in != null && !lc.hasPermission(claim == null ? null : claim.m_owner, in,
                    LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
        }

        for (Block block : event.getBlocks()) {
            LandClaims.Claim to = lc
                    .getClaim(block.getRelative(((Directional) piston.getBlockData()).getFacing().getOppositeFace())
                            .getLocation());
            if (to != null && !lc.hasPermission(claim == null ? null : claim.m_owner, to,
                    LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
            LandClaims.Claim in = lc
                    .getClaim(block.getRelative(((Directional) piston.getBlockData()).getFacing().getOppositeFace())
                            .getLocation());
            if (in != null && !lc.hasPermission(claim == null ? null : claim.m_owner, in,
                    LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onItemDamage(EntityDamageByEntityEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        switch (event.getDamager()) {
            case TNTPrimed tnt -> {
                if (tnt.getSource() instanceof Player player) {
                    LandClaims.Claim claim = lc.getClaim(event.getEntity().getLocation());
                    if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                        event.setCancelled(true);
                    }
                } else {
                    LandClaims.Claim claim = lc.getClaim(event.getEntity().getLocation());
                    if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                        event.setCancelled(true);
                    }
                }
            }
            case EnderCrystal ignored1 -> {
                LandClaims.Claim claim = lc.getClaim(event.getEntity().getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            }
            case Creeper ignored -> {
                LandClaims.Claim claim = lc.getClaim(event.getEntity().getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            }
            case ExplosiveMinecart ignored -> {
                LandClaims.Claim claim = lc.getClaim(event.getEntity().getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onHopperPull(InventoryMoveItemEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (event.getDestination().getType() == InventoryType.HOPPER) {
            if (event.getSource().getHolder() == null || event.getDestination().getHolder() == null)
                return;
            LandClaims.Claim in;
            LandClaims.Claim to;
            if (event.getSource().getHolder() instanceof BlockInventoryHolder) {
                in = lc.getClaim(((BlockInventoryHolder) event.getSource().getHolder()).getBlock().getLocation());
            } else if (event.getSource().getHolder() instanceof DoubleChest) {
                in = lc.getClaim(((DoubleChest) event.getSource().getHolder()).getLocation());
            } else if (event.getSource().getHolder() instanceof HopperMinecart) {
                in = lc.getClaim(((HopperMinecart) event.getSource().getHolder()).getLocation());
            } else if (event.getSource().getHolder() instanceof StorageMinecart) {
                in = lc.getClaim(((StorageMinecart) event.getSource().getHolder()).getLocation());
            } else {
                return;
            }
            if (event.getDestination().getHolder() instanceof BlockInventoryHolder) {
                to = lc.getClaim(
                        ((BlockInventoryHolder) event.getDestination().getHolder()).getBlock().getLocation());
            } else if (event.getSource().getHolder() instanceof DoubleChest) {
                to = lc.getClaim(((DoubleChest) event.getDestination().getHolder()).getLocation());
            } else if (event.getDestination().getHolder() instanceof HopperMinecart) {
                to = lc.getClaim(((HopperMinecart) event.getDestination().getHolder()).getLocation());
            } else if (event.getDestination().getHolder() instanceof StorageMinecart) {
                to = lc.getClaim(((StorageMinecart) event.getDestination().getHolder()).getLocation());
            } else {
                return;
            }
            if (in != null
                    && !lc.hasPermission(to == null ? null : to.m_owner, in, LandClaims.ClaimPermission.INTERACT)) {
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
        if (event.getBlock().getType() != Material.FARMLAND)
            return;
        LandClaims.Claim claim = lc.getClaim(event.getBlock().getLocation());
        if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerTrample(PlayerInteractEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (event.getAction() != Action.PHYSICAL)
            return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.FARMLAND)
            return;
        Player player = event.getPlayer();
        LandClaims.Claim claim = lc.getClaim(event.getClickedBlock().getLocation());
        if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
            // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
            //     message(player,
            //             claim.m_owner == null ? "the server"
            //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
            //             true);
            //     return;
            // }
            message(player, claim.m_owner == null ? "the server"
                    : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(), false);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
            return;
        if (event.getEntityType() == EntityType.SHEEP)
            return;
        LandClaims.Claim claim = lc.getClaim(event.getBlock().getLocation());
        if (event.getEntity() instanceof Player player) {
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                // if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                //     message(player,
                //             claim.m_owner == null ? "the server"
                //                     : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(),
                //             true);
                //     return;
                // }
                message(player, claim.m_owner == null ? "the server"
                        : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName(), false);
                event.setCancelled(true);
            }
        } else {
            if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
            }
        }
    }
}
