package kam.kamsTweaks.features.claims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.Names;
import kam.kamsTweaks.features.claims.gui.EntityClaimPage;
import kam.kamsTweaks.features.claims.gui.FLAlertLayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.*;
import kam.kamsTweaks.features.claims.EntityClaims.*;

public class EntityProtections implements Listener {
    private EntityClaims claims;

    public void setup(EntityClaims entityClaims) {
        this.claims = entityClaims;
    }

    // shortcut
    void message(Entity player, Component component) {
        claims.instance.message(player, component);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true)) return;
        var who = e.getPlayer();
        var entity = e.getRightClicked();
        if (claims.isClaimable(entity)) {
            var claim = claims.getClaim(entity);
            if (ItemManager.getType(who.getInventory().getItemInMainHand()) == ItemManager.ItemType.CLAIM_TOOL) {
                e.setCancelled(true);
                if (entity instanceof Tameable tameable && tameable.getOwnerUniqueId() != null) {
                    if (tameable.getOwnerUniqueId() != who.getUniqueId()) {
                        message(who, Component.text("This entity is tamed and cannot be claimed."));
                        return;
                    }
                }
                if (claim != null) {
                    if (claim.owner != null && claim.owner.getUniqueId().equals(who.getUniqueId()) || who.hasPermission("kamstweaks.claims.manage")) {
                        new EntityClaimPage(who, entity).show();
                        return;
                    }
                    message(who, Component.text("This entity is already claimed by ").append(claim.getOwnerName(), Component.text(".")));
                    return;
                }
                if (!who.hasPermission("kamstweaks.claims.claim")) {
                    who.sendMessage(Component.text("You do not have permission to claim entities.").color(NamedTextColor.RED));
                    return;
                }
                int count = 0;
                for (var c : claims.claims.values()) {
                    if (c.owner != null && who.getUniqueId().equals(c.owner.getUniqueId())) count += 1;
                }
                var max = KamsTweaks.get().getConfig().getInt("entity-claims.max-claims", 1000);
                if (count >= max) {
                    who.sendMessage(Component.text("You already have the max number of claims! (" + count + "/" + max + ")").color(NamedTextColor.RED));
                    return;
                }
                new FLAlertLayer(who,
                        Component.text("Claim ").append(Names.instance.getEntityRenderedName(entity), Component.text("?")),
                        Component.empty(),
                        Component.text("Yes"),
                        Component.text("No"),
                        second -> {
                            if (!second) {
                                Claims.get().entityClaims.createClaim(who, entity);
                            }
                        }).show();
                return;
            }
            if (claim == null) return;
            if (!claim.hasPermission(who, EntityPermission.INTERACT)) {
                message(who, Component.text("You don't have permission to interact with this entity! (Entity claimed by ").append(claim.getOwnerName(), Component.text(")")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true))
            return;
        Entity entity = e.getVehicle();
        EntityClaim claim = claims.getClaim(entity);
        if (claim == null) return;
        if (e.getAttacker() instanceof Player player) {
            if (!claim.hasPermission(player, EntityPermission.DAMAGE)) {
                message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(claim.getOwnerName(), Component.text(")")));
                e.setCancelled(true);
            }
        } else {
            if (!claim.hasPermission(null, EntityPermission.DAMAGE)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onECVehicleDamage(VehicleDamageEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true))
            return;
        Entity entity = e.getVehicle();
        EntityClaim claim = claims.getClaim(entity);
        if (claim == null) return;
        if (e.getAttacker() instanceof Player player) {
            if (!claim.hasPermission(player, EntityPermission.DAMAGE)) {
                message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(claim.getOwnerName(), Component.text(")")));
                e.setCancelled(true);
            }
        } else {
            if (!claim.hasPermission(null, EntityPermission.DAMAGE)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true)) return;
        EntityClaim claim = claims.getClaim(event.getEntity());
        switch (event.getCause()) {
            case VOID, KILL -> {}
            case ENTITY_ATTACK, ENTITY_EXPLOSION, ENTITY_SWEEP_ATTACK -> {
                if (event.getEntity() instanceof Mob mob) {
                    if (mob.getTarget() != null) {
                        if (mob.getTarget() == event.getDamageSource().getCausingEntity()) {
                            return;
                        }
                    }
                }
                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                    if (ItemManager.ItemType.CLAIM_TOOL.equals(ItemManager.getType(player.getInventory().getItemInMainHand())) && claims.isClaimable(event.getEntity())) {
                        if (claim == null) player.sendMessage(Component.text("This entity isn't claimed."));
                        else if (claim.owner == null)
                            player.sendMessage(Component.text("This entity is owned by the server."));
                        else
                            player.sendMessage(Component.text("This entity is owned by ").append(Names.instance.getRenderedName(claim.owner)));
                        event.setCancelled(true);
                        return;
                    }
                    if (claim == null) return;
                    if (claim.hasPermission(player, EntityPermission.DAMAGE)) return;
                    message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(claim.getOwnerName(), Component.text(")")));
                    event.setCancelled(true);
                } else if (claim != null) {
                    if (claim.hasPermission(null, EntityPermission.DAMAGE)) return;
                    event.setCancelled(true);
                }
            }
            case PROJECTILE -> {
                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                    if (ItemManager.ItemType.CLAIM_TOOL.equals(ItemManager.getType(player.getInventory().getItemInMainHand())) && claims.isClaimable(event.getEntity())) {
                        if (claim == null) player.sendMessage(Component.text("This entity isn't claimed."));
                        else if (claim.owner == null)
                            player.sendMessage(Component.text("This entity is owned by the server."));
                        else
                            player.sendMessage(Component.text("This entity is owned by ").append(Names.instance.getRenderedName(claim.owner)));
                        event.setCancelled(true);
                        return;
                    }
                    if (claim == null) return;
                    if (claim.hasPermission(player, EntityPermission.DAMAGE)) return;
                    message(player, Component.text("You don't have permission to damage this entity! (Entity claimed by ").append(claim.getOwnerName(), Component.text(")")));
                    event.setCancelled(true);
                }
            }
            default -> {
                if (claim != null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() == null) return;
        if (event.getEntity() instanceof Mob entity) {
            var claim = claims.getClaim(entity);
            if (claim != null) {
                if (!claim.config.canAggro) event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTransform(EntityTransformEvent e) {
        if (e.getEntity() instanceof Mob entity) {
            var claim = claims.getClaim(entity);
            if (e.getTransformReason() == EntityTransformEvent.TransformReason.LIGHTNING) {
                if (claim != null) {
                    if (!claim.hasPermission(null, EntityPermission.DAMAGE)) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            if (claim != null) {
                boolean hasReplaced = false;
                for (var newEntity : e.getTransformedEntities()) {
                    EntityClaim newC;
                    if (!hasReplaced) {
                        newC = new EntityClaim(claim, newEntity.getUniqueId(), claim.id);
                    } else {
                        newC = new EntityClaim(claim, newEntity.getUniqueId());
                    }
                    claims.claims.put(newEntity.getUniqueId(), newC);
                }
                claims.claims.remove(entity.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onBurn(EntityCombustEvent event) {
        if (event.getEntity() instanceof Mob entity) {
            var claim = claims.getClaim(entity);
            if (claim != null) {
                if (!claim.config.canAggro) event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void entityDie(EntityDeathEvent e) {
        // delayed to next tick so transformations work
        Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> claims.claims.remove(e.getEntity().getUniqueId()), 0);
        if (e.getEntity() instanceof EnderDragon && e.getEntity().getWorld().getEnvironment() == World.Environment.THE_END) {
            claims.instance.landClaims.disabled.put(e.getEntity().getWorld(), 5 * 60);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getCaught() != null) {
            var claim = claims.getClaim(e.getCaught());
            if (claim != null && !claim.hasPermission(e.getPlayer(), EntityPermission.INTERACT)) e.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onSleep(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.NOT_SAFE)
            return;

        Block block = event.getBed();

        Block head = ((Bed) block.getBlockData()).getPart().equals(Bed.Part.HEAD) ? block : block.getRelative(((Directional) block).getFacing());

        boolean unsafe = block.getWorld().getNearbyEntities(head.getLocation(), 8, 5, 8)
                .stream()
                .filter(e -> e instanceof Monster)
                .anyMatch(type -> !claims.claims.containsKey(type.getUniqueId()));
        if (!unsafe) {
            event.setUseBed(Event.Result.ALLOW);
            event.setCancelled(false);
        }
    }
}
