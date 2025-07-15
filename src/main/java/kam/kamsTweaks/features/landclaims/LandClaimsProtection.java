package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
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
//
//    @EventHandler
//    public void onEntityInteractAS(PlayerArmorStandManipulateEvent e) {
//        onEntityInteract(e);
//    }

    @EventHandler
    public void onTnt(EntityExplodeEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        if (e.getEntity() instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) {
                LandClaims.Claim claim = lc.getClaim(e.getEntity().getLocation());
                if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                        player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                        return;
                    }
                    player.sendMessage(Component.text("This land is claimed by ").append(Component.text(claim.m_owner == null ? "the server" : claim.m_owner.getName() == null ? "Unknown player" : claim.m_owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                    e.setCancelled(true);
                    tnt.remove();
                }
            } else {
                LandClaims.Claim claim = lc.getClaim(e.getEntity().getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    e.setCancelled(true);
                    tnt.remove();
                }
            }
        } else if (e.getEntity() instanceof EnderCrystal ec) {
            LandClaims.Claim claim = lc.getClaim(e.getEntity().getLocation());
            if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                e.setCancelled(true);
                ec.remove();
            }
        }
    }

    @EventHandler
    public void onItemDamage(EntityDamageByEntityEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) return;
        if (event.getDamager() instanceof TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) {
                LandClaims.Claim claim = lc.getClaim(tnt.getLocation());
                if (!lc.hasPermission(player, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            } else {
                LandClaims.Claim claim = lc.getClaim(tnt.getLocation());
                if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getDamager() instanceof EnderCrystal ec) {
            LandClaims.Claim claim = lc.getClaim(ec.getLocation());
            if (!lc.hasPermission(null, claim, LandClaims.ClaimPermission.BLOCKS)) {
                event.setCancelled(true);
            }
        }
    }
}
