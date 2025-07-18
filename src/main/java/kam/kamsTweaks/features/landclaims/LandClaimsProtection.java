package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Piston;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
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

public class LandClaimsProtection implements Listener {
    LandClaims lc;
    public LandClaimsProtection(LandClaims lc) {
        this.lc = lc;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        Player player = e.getPlayer();
        LandClaims.Claim claim = lc.getClaim(e.getBlock().getLocation());
        if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
            if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                return;
            }
            player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        Player player = e.getPlayer();
        LandClaims.Claim claim = lc.getClaim(e.getBlock().getLocation());
        if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
            if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                return;
            }
            player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getPlayer().getTargetEntity(5) instanceof Creature) return;
        if (e.getItem() != null && e.getItem().isSimilar(ItemManager.createItem(ItemManager.ItemType.CLAIMER)) && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) lc.handleItem(e);
            e.setCancelled(true);
            return;
        }
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = e.getPlayer();
            assert e.getClickedBlock() != null;
            LandClaims.Claim claim = lc.getClaim(e.getClickedBlock().getLocation());
            if (e.getClickedBlock().getType().toString().contains("DOOR")) {
                if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.DOORS)) {
                    if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                        player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                        return;
                    }
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                    e.setCancelled(true);
                }
            } else {
                if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.INTERACT)) {
                    if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                        player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                        return;
                    }
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        InventoryHolder entity = e.getInventory().getHolder();
        if (entity instanceof AbstractHorse || entity instanceof ChestBoat || entity instanceof StorageMinecart) {
            Player player = (Player) e.getPlayer();
            LandClaims.Claim claim = lc.getClaim(((Entity) entity).getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.INTERACT)) {
                if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                    return;
                }
                player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        Entity entity = e.getRightClicked();
        if (entity instanceof ItemFrame || entity instanceof ArmorStand) {
            Player player = e.getPlayer();
            LandClaims.Claim claim = lc.getClaim(entity.getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.INTERACT)) {
                if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                    return;
                }
                player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangDestroy(HangingBreakByEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        if (e.getRemover() instanceof Player player) {
            LandClaims.Claim claim = lc.getClaim(e.getEntity().getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                    return;
                }
                player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        Entity entity = e.getVehicle();
        if (!(entity instanceof AbstractHorse || entity instanceof ChestBoat || entity instanceof StorageMinecart)) return;
        if (e.getAttacker() instanceof Player player) {
            LandClaims.Claim claim = lc.getClaim(e.getVehicle().getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                    return;
                }
                player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        Entity entity = e.getEntity();
        if (!(entity instanceof ItemFrame || entity instanceof ArmorStand || entity instanceof AbstractHorse || entity instanceof ChestBoat || entity instanceof StorageMinecart)) return;
        if (e.getDamager() instanceof Player player) {
            LandClaims.Claim claim = lc.getClaim(e.getEntity().getLocation());
            if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                    return;
                }
                player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
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
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        boolean saidThingy = false;
        switch (e.getEntity()) {
            case TNTPrimed tnt -> {
                if (tnt.getSource() instanceof Player player) {
                    for (Block block : e.blockList()) {
                        LandClaims.Claim claim = lc.getClaim(block.getLocation());
                        if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                            if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                                if (!saidThingy) {
                                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                                    saidThingy = true;
                                }
                            }
                            if (!saidThingy) {
                                player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                                saidThingy = true;
                            }
                            e.setCancelled(true);
                            tnt.remove();
                        }
                    }
                } else {
                    for (Block block : e.blockList()) {
                        LandClaims.Claim claim = lc.getClaim(block.getLocation());
                        if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                            e.setCancelled(true);
                            tnt.remove();
                        }
                    }
                }
            }
            case EnderCrystal ec -> {
                for (Block block : e.blockList()) {
                    LandClaims.Claim claim = lc.getClaim(block.getLocation());
                    if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                        e.setCancelled(true);
                        ec.remove();
                    }
                }

            }
            case ExplosiveMinecart mc -> {
                for (Block block : e.blockList()) {
                    LandClaims.Claim claim = lc.getClaim(block.getLocation());
                    if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                        e.setCancelled(true);
                        mc.remove();
                    }
                }

            }
            case Creeper cr -> {
                for (Block block : e.blockList()) {
                    LandClaims.Claim claim = lc.getClaim(block.getLocation());
                    if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                        e.setCancelled(true);
                        cr.remove();
                    }
                }

            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        Block piston = event.getBlock();
        LandClaims.Claim claim = lc.getClaim(piston.getLocation());

        {
            LandClaims.Claim to = lc.getClaim(piston.getRelative(((Directional) piston.getBlockData()).getFacing()).getLocation());
            if (to != null && !lc.hasPermission(claim == null ? null : claim.m_owner, to, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
            LandClaims.Claim in = lc.getClaim(piston.getRelative(((Directional) piston.getBlockData()).getFacing()).getLocation());
            if (in != null && !lc.hasPermission(claim == null ? null : claim.m_owner, in, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
        }

        for (Block block : event.getBlocks()) {
            LandClaims.Claim to = lc.getClaim(block.getRelative(((Directional) piston.getBlockData()).getFacing()).getLocation());
            if (to != null && !lc.hasPermission(claim == null ? null : claim.m_owner, to, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
            LandClaims.Claim in = lc.getClaim(block.getRelative(((Directional) piston.getBlockData()).getFacing()).getLocation());
            if (in != null && !lc.hasPermission(claim == null ? null : claim.m_owner, in, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        Block piston = event.getBlock();
        LandClaims.Claim claim = lc.getClaim(piston.getLocation());

        {
            LandClaims.Claim to = lc.getClaim(piston.getRelative(((Directional) piston.getBlockData()).getFacing().getOppositeFace()).getLocation());
            if (to != null && !lc.hasPermission(claim == null ? null : claim.m_owner, to, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
            LandClaims.Claim in = lc.getClaim(piston.getRelative(((Directional) piston.getBlockData()).getFacing().getOppositeFace()).getLocation());
            if (in != null && !lc.hasPermission(claim == null ? null : claim.m_owner, in, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
        }

        for (Block block : event.getBlocks()) {
            LandClaims.Claim to = lc.getClaim(block.getRelative(((Directional) piston.getBlockData()).getFacing().getOppositeFace()).getLocation());
            if (to != null && !lc.hasPermission(claim == null ? null : claim.m_owner, to, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
            LandClaims.Claim in = lc.getClaim(block.getRelative(((Directional) piston.getBlockData()).getFacing().getOppositeFace()).getLocation());
            if (in != null && !lc.hasPermission(claim == null ? null : claim.m_owner, in, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onItemDamage(EntityDamageByEntityEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
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
            case EnderCrystal ec -> {
                LandClaims.Claim claim = lc.getClaim(event.getEntity().getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            }
            case Creeper cr -> {
                LandClaims.Claim claim = lc.getClaim(event.getEntity().getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            }
            case ExplosiveMinecart em -> {
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
        if (event.getDestination().getType() == InventoryType.HOPPER) {
            if (event.getSource().getHolder() == null || event.getDestination().getHolder() == null) return;
            LandClaims.Claim in = lc.getClaim(((BlockInventoryHolder) event.getSource().getHolder()).getBlock().getLocation());
            LandClaims.Claim to = lc.getClaim(((BlockInventoryHolder) event.getDestination().getHolder()).getBlock().getLocation());
            if (in != null && !lc.hasPermission(to == null ? null : to.m_owner, in, LandClaims.ClaimPermission.INTERACT)) {
                event.setCancelled(true);
            }
        }
    }
}
