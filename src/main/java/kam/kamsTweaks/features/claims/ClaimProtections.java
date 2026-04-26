package kam.kamsTweaks.features.claims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.features.Names;
import kam.kamsTweaks.features.SeedDispenser;
import kam.kamsTweaks.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Hangable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.BlockProjectileSource;

import java.util.*;

public class ClaimProtections implements Listener {
//    Claims claims = null;
//
//    public void setup(Claims claims) {
//        this.claims = claims;
//    }
//
//    boolean useClaimTool(PlayerInteractEvent e) {
//        assert e.getItem() != null;
//        if (e.getItem().getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "yummy"))) {
//            if (e.getAction() != Action.RIGHT_CLICK_AIR) e.setCancelled(true);
//            return true;
//        }
//        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
//            if (KamsTweaks.get().getConfig().getBoolean("land-claims.enabled", true)) {
//                claims.handleItem(e);
//            }
//            e.setCancelled(true);
//            return true;
//        } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
//            if (KamsTweaks.get().getConfig().getBoolean("land-claims.enabled", true)) {
//                if (!KamsTweaks.get().getConfig().getBoolean("land-claims.enabled", true))
//                    return true;
//                assert e.getClickedBlock() != null;
//                var loc = e.getClickedBlock().getLocation();
//                if (claims.currentlyClaiming.containsKey(e.getPlayer())) {
//                    var claim = claims.currentlyClaiming.get(e.getPlayer());
//                    if (claim.start != null) {
//                        var col = Color.GREEN;
//                        for (var other : claims.landClaims) {
//                            if (other.intersects(claim.start, loc)) {
//                                if (other.owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
//                                    if (col != Color.RED) {
//                                        col = Color.AQUA;
//                                    }
//                                    claims.dialogGui.showArea(e.getPlayer(), other.start, other.end, 1, 20, Color.PURPLE);
//                                } else {
//                                    col = Color.RED;
//                                    claims.dialogGui.showArea(e.getPlayer(), other.start, other.end, 1, 20, Color.ORANGE);
//                                }
//                            }
//                        }
//                        claims.dialogGui.showArea(e.getPlayer(), claim.start, loc, 1, 100, col);
//                    }
//                }
//                Claims.LandClaim res = claims.getLandClaim(loc, true);
//                if (res == null) e.getPlayer().sendMessage(Component.text("This land isn't claimed."));
//                else if (res.owner == null)
//                    e.getPlayer().sendMessage(Component.text("This claim is owned by the server."));
//                else
//                    e.getPlayer().sendMessage(Component.text("This claim is owned by ").append(Names.instance.getRenderedName(res.owner)));
//            }
//            e.setCancelled(true);
//            return true;
//        } else if (e.getAction() == Action.LEFT_CLICK_AIR) {
//            e.setCancelled(true);
//            return true;
//        }
//        return false;
//    }
//
//    /// Entity Claims
//    @EventHandler(priority = EventPriority.HIGH)
//    public void EConEntityInteract(PlayerInteractEntityEvent e) {
//        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true)) return;
//		if (claims.isClaimable(e.getRightClicked())) {
//            if (ItemManager.getType(e.getPlayer().getInventory().getItemInMainHand()) == ItemManager.ItemType.CLAIM_TOOL) {
//                e.setCancelled(true);
//                if (e.getRightClicked() instanceof Tameable tameable && tameable.getOwnerUniqueId() != null) {
//                    if (tameable.getOwnerUniqueId() != e.getPlayer().getUniqueId()) {
//                        if (hasMessaged.contains(e.getPlayer().getUniqueId())) return;
//                        hasMessaged.add(e.getPlayer().getUniqueId());
//                        e.getPlayer().sendMessage(Component.text("This entity is tamed and cannot be claimed."));
//                        return;
//                    }
//                }
//                if (claims.entityClaims.containsKey(e.getRightClicked().getUniqueId())) {
//                    OfflinePlayer owner = claims.entityClaims.get(e.getRightClicked().getUniqueId()).owner;
//                    if (owner != null && owner.getUniqueId().equals(e.getPlayer().getUniqueId()) || e.getPlayer().hasPermission("kamstweaks.claims.manage")) {
//                        claims.dialogGui.openECPage(e.getPlayer());
//                        return;
//                    }
//                    if (hasMessaged.contains(e.getPlayer().getUniqueId())) return;
//                    hasMessaged.add(e.getPlayer().getUniqueId());
//                    e.getPlayer().sendMessage(Component.text("This entity is already claimed by ").append(owner == null ? Component.text("the server") : Names.instance.getRenderedName(owner), Component.text(".")));
//                    return;
//                }
//                if (!e.getPlayer().hasPermission("kamstweaks.claims.claim")) {
//                    e.getPlayer().sendMessage(Component.text("You do not have permission to claim entities.").color(NamedTextColor.RED));
//                    return;
//                }
//                int count = 0;
//                for (var claim : claims.entityClaims.values()) {
//                    if (claim.owner.getUniqueId().equals(e.getPlayer().getUniqueId())) count += 1;
//                }
//                var max = KamsTweaks.get().getConfig().getInt("entity-claims.max-claims", 1000);
//                if (count >= max) {
//                    e.getPlayer().sendMessage(Component.text("You already have the max number of claims! (" + count + "/" + max + ")").color(NamedTextColor.RED));
//                    return;
//                }
//                claims.dialogGui.openECPage(e.getPlayer(), e.getRightClicked());
//                return;
//            }
//            var claim = claims.entityClaims.get(e.getRightClicked().getUniqueId());
//            if (claim == null) return;
//            if (!claim.hasPermission(e.getPlayer(), Claims.ClaimPermission.INTERACT_ENTITY)) {
//                message(e.getPlayer(), Component.text("You don't have permission to interact with this entity! (Entity claimed by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
//                e.setCancelled(true);
//            }
//		}
//    }
//
//    @EventHandler
//    public void onECVehicleDestroy(VehicleDestroyEvent e) {
//        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true))
//            return;
//        Entity entity = e.getVehicle();
//        Claims.EntityClaim claim = claims.getEntityClaim(entity);
//        if (claim == null) return;
//        if (e.getAttacker() instanceof Player player) {
//            if (!claim.hasPermission(player, Claims.ClaimPermission.DAMAGE_ENTITY)) {
//                message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
//                e.setCancelled(true);
//            }
//        } else {
//            if (!claim.hasPermission(null, Claims.ClaimPermission.DAMAGE_ENTITY)) {
//                e.setCancelled(true);
//            }
//        }
//    }
//
//    @EventHandler
//    public void onECVehicleDamage(VehicleDamageEvent e) {
//        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true))
//            return;
//        Entity entity = e.getVehicle();
//        Claims.EntityClaim claim = claims.getEntityClaim(entity);
//        if (claim == null) return;
//        if (e.getAttacker() instanceof Player player) {
//            if (!claim.hasPermission(player, Claims.ClaimPermission.DAMAGE_ENTITY)) {
//                message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
//                e.setCancelled(true);
//            }
//        } else {
//            if (!claim.hasPermission(null, Claims.ClaimPermission.DAMAGE_ENTITY)) {
//                e.setCancelled(true);
//            }
//        }
//    }
//
//    @EventHandler
//    public void onDamage(EntityDamageEvent event) {
//        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true)) return;
//        Claims.EntityClaim claim = claims.entityClaims.get(event.getEntity().getUniqueId());
//        switch (event.getCause()) {
//            case VOID, KILL -> {}
//            case ENTITY_ATTACK, ENTITY_EXPLOSION, ENTITY_SWEEP_ATTACK -> {
//                if (event.getEntity() instanceof Mob mob) {
//                    if (mob.getTarget() != null) {
//                        if (mob.getTarget() == event.getDamageSource().getCausingEntity()) {
//                            return;
//                        }
//                    }
//                }
//                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
//                    if (ItemManager.ItemType.CLAIM_TOOL.equals(ItemManager.getType(player.getInventory().getItemInMainHand())) && claims.isClaimable(event.getEntity())) {
//                        if (claim == null) player.sendMessage(Component.text("This entity isn't claimed."));
//                        else if (claim.owner == null)
//                            player.sendMessage(Component.text("This entity is owned by the server."));
//                        else
//                            player.sendMessage(Component.text("This entity is owned by ").append(Names.instance.getRenderedName(claim.owner)));
//                        event.setCancelled(true);
//                        return;
//                    }
//                    if (claim == null) return;
//                    if (claim.hasPermission(player, Claims.ClaimPermission.DAMAGE_ENTITY)) return;
//                    message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
//                    event.setCancelled(true);
//                } else if (claim != null) {
//                    if (claim.hasPermission(null, Claims.ClaimPermission.DAMAGE_ENTITY)) return;
//                    event.setCancelled(true);
//                }
//            }
//            case PROJECTILE -> {
//                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
//                    if (ItemManager.ItemType.CLAIM_TOOL.equals(ItemManager.getType(player.getInventory().getItemInMainHand())) && claims.isClaimable(event.getEntity())) {
//                        if (claim == null) player.sendMessage(Component.text("This entity isn't claimed."));
//                        else if (claim.owner == null)
//                            player.sendMessage(Component.text("This entity is owned by the server."));
//                        else
//                            player.sendMessage(Component.text("This entity is owned by ").append(Names.instance.getRenderedName(claim.owner)));
//                        event.setCancelled(true);
//                        return;
//                    }
//                    if (claim == null) return;
//                    if (claim.hasPermission(player, Claims.ClaimPermission.DAMAGE_ENTITY)) return;
//                    message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
//                    event.setCancelled(true);
//                }
//            }
//            default -> {
//                if (claim != null) {
//                    event.setCancelled(true);
//                }
//            }
//        }
//    }
//
//    @EventHandler
//    public void onTarget(EntityTargetEvent event) {
//        if (event.getTarget() == null) return;
//        if (event.getEntity() instanceof Mob entity) {
//            var claim = claims.getEntityClaim(entity);
//            if (claim != null) {
//                if (!claim.canAggro) event.setCancelled(true);
//            }
//        }
//    }
//
//    @EventHandler
//    public void onTransform(EntityTransformEvent e) {
//        if (e.getTransformReason() == EntityTransformEvent.TransformReason.LIGHTNING) {
//            if (e.getEntity() instanceof Mob entity) {
//                var claim = claims.getEntityClaim(entity);
//                if (claim != null) {
//                    if (!claim.defaults.contains(Claims.ClaimPermission.DAMAGE_ENTITY)) {
//                        e.setCancelled(true);
//                        return;
//                    }
//                }
//            }
//        }
//        if (claims.entityClaims.containsKey(e.getEntity().getUniqueId())) {
//            var claim = claims.entityClaims.get(e.getEntity().getUniqueId());
//            boolean hasReplaced = false;
//            for (var entity : e.getTransformedEntities()) {
//                Claims.EntityClaim newC;
//                if (!hasReplaced) {
//                    newC = new Claims.EntityClaim(claim, entity.getUniqueId(), claim.id);
//                } else {
//                    newC = new Claims.EntityClaim(claim, entity.getUniqueId());
//                }
//                claims.entityClaims.put(entity.getUniqueId(), newC);
//            }
//            claims.entityClaims.remove(e.getEntity().getUniqueId());
//        }
//    }
//
//    @EventHandler
//    public void onBurn(EntityCombustEvent event) {
//        if (event.getEntity() instanceof Mob entity) {
//            var claim = claims.getEntityClaim(entity);
//            if (claim != null) {
//                if (!claim.canAggro) event.setCancelled(true);
//            }
//        }
//    }
//
//    @EventHandler
//    public void entityDie(EntityDeathEvent e) {
//        // delayed to next tick so transformations work
//        Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> claims.entityClaims.remove(e.getEntity().getUniqueId()), 0);
//        if (e.getEntity() instanceof EnderDragon && e.getEntity().getWorld().getEnvironment() == World.Environment.THE_END) {
//            claims.disabledClaims.put(e.getEntity().getWorld(), 5 * 60);
//        }
//    }
//
//    @EventHandler
//    public void onFish(PlayerFishEvent e) {
//        if (e.getCaught() != null) {
//            var claim = claims.getEntityClaim(e.getCaught());
//            if (claim != null && !claim.hasPermission(e.getPlayer(), Claims.ClaimPermission.INTERACT_ENTITY)) e.setCancelled(true);
//        }
//    }
//
//    @SuppressWarnings("deprecation")
//    @EventHandler
//    public void onSleep(PlayerBedEnterEvent event) {
//        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.NOT_SAFE)
//            return;
//
//        Block block = event.getBed();
//
//        Block head = ((Bed) block.getBlockData()).getPart().equals(Bed.Part.HEAD) ? block : block.getRelative(((Directional) block).getFacing());
//
//        boolean unsafe = block.getWorld().getNearbyEntities(head.getLocation(), 8, 5, 8)
//                .stream()
//                .filter(e -> e instanceof Monster)
//                .anyMatch(type -> !claims.entityClaims.containsKey(type.getUniqueId()));
//        if (!unsafe) {
//            event.setUseBed(Event.Result.ALLOW);
//            event.setCancelled(false);
//        }
//    }
}
