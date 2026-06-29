package kam.kamsTweaks.features.claims;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.FishHookStateChangeEvent;
import kam.kamsTweaks.gameplay.ItemManager;
import kam.kamsTweaks.utils.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.fun.Names;
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

    public static Entity getTrueHitter(Entity hitter) {
        if (hitter instanceof Projectile proj) {
            return (Entity) proj.getShooter();
        }
        return hitter;
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
                        message(who, KTStrings.getFor(KTStrings.EC_TAMED));
                        return;
                    }
                }
                if (claim != null) {
                    if (claim.owner != null && claim.owner.getUniqueId().equals(who.getUniqueId()) || who.hasPermission("kamstweaks.claims.manage")) {
                        new EntityClaimPage(who, entity).show();
                        return;
                    }
                    message(who, KTStrings.getFor(KTStrings.EC_ALREADY_CLAIMED, claim.getOwnerName()));
                    return;
                }
                if (!who.hasPermission("kamstweaks.claims.claim")) {
                    return;
                }
                int count = 0;
                for (var c : claims.claims.values()) {
                    if (c.owner != null && who.getUniqueId().equals(c.owner.getUniqueId())) count += 1;
                }
                var max = KamsTweaks.get().getConfig().getInt("entity-claims.max-claims", 1000);
                if (count >= max) {
                    who.sendMessage(KTStrings.getFor(KTStrings.EC_MAX, Component.text(count), Component.text(max)).color(NamedTextColor.RED));
                    return;
                }
                new FLAlertLayer(who,
                        KTStrings.getFor(KTStrings.EC_CONFIRM, Names.instance.getEntityRenderedName(entity)),
                        Component.empty(),
                        KTStrings.getFor(KTStrings.YES),
                        KTStrings.getFor(KTStrings.NO),
                        second -> {
                            if (!second) {
                                Claims.get().entityClaims.createClaim(who, entity);
                            }
                        }).show();
                return;
            }
            if (claim == null) return;
            var perm = EntityPermission.INTERACT;
            if (!claim.hasPermission(who, perm)) {
                message(who, KTStrings.getFor(KTStrings.EC_NO_PERM, perm.label, claim.getOwnerName()));
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
        var perm = EntityPermission.DAMAGE;
        if (!claim.hasPermission(e.getAttacker(), perm)) {
            message(e.getAttacker(), KTStrings.getFor(KTStrings.EC_NO_PERM, perm.label, claim.getOwnerName()));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onECVehicleDamage(VehicleDamageEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true))
            return;
        Entity entity = e.getVehicle();
        EntityClaim claim = claims.getClaim(entity);
        if (claim == null) return;
        var perm = EntityPermission.DAMAGE;
        if (!claim.hasPermission(e.getAttacker(), perm)) {
            message(e.getAttacker(), KTStrings.getFor(KTStrings.EC_NO_PERM, perm.label, claim.getOwnerName()));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!KamsTweaks.get().getConfig().getBoolean("entity-claims.enabled", true)) return;
        EntityClaim claim = claims.getClaim(event.getEntity());
        var perm = EntityPermission.DAMAGE;
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
                        if (claim == null) {
                            player.sendMessage(KTStrings.getFor(KTStrings.EC_UNCLAIMED));
                        } else {
                            player.sendMessage(KTStrings.getFor(KTStrings.EC_UNCLAIMED, claim.getOwnerName()));
                        }
                        event.setCancelled(true);
                        return;
                    }
                }
                if (claim == null) return;
                if (claim.hasPermission(event.getDamageSource().getCausingEntity(), perm)) return;
                message(event.getDamageSource().getCausingEntity(), KTStrings.getFor(KTStrings.EC_NO_PERM, perm.label, claim.getOwnerName()));
                event.setCancelled(true);
            }
            case PROJECTILE -> {
                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                    if (claim == null) return;
                    if (claim.hasPermission(player, perm)) return;
                    message(player, KTStrings.getFor(KTStrings.EC_NO_PERM, perm.label, claim.getOwnerName()));
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
            switch (e.getTransformReason()) {
                case EntityTransformEvent.TransformReason.LIGHTNING, DROWNED, FROZEN:
                    if (claim != null) {
                        if (!claim.hasPermission(null, EntityPermission.DAMAGE)) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                    break;
                default: break;
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
    public void onFish(FishHookStateChangeEvent e) {
        if (e.getNewHookState() == FishHook.HookState.HOOKED_ENTITY) {
            var caught = e.getEntity().getHookedEntity();
            var claim = claims.getClaim(caught);
            if (claim != null && !claim.hasPermission(e.getEntity().getShooter(), EntityPermission.INTERACT)) {
                e.getEntity().setHookedEntity(null);
                if (e.getEntity().getShooter() instanceof Player plr) message(plr, KTStrings.getFor(KTStrings.EC_NO_PERM, KTStrings.getFor(KTStrings.EC_INTERACT), claim.getOwnerName()));
            }
        }
    }

    @EventHandler
    public void onKnockback(EntityKnockbackByEntityEvent e) {
        var entity = e.getEntity();
        var attacker = getTrueHitter(e.getHitBy());
        var claim = claims.getClaim(entity);
        if (claim != null && !claim.hasPermission(attacker, EntityPermission.DAMAGE)) {
            e.setCancelled(true);
            message(attacker, KTStrings.getFor(KTStrings.EC_NO_PERM, KTStrings.getFor(KTStrings.EC_DAMAGE), claim.getOwnerName()));
        }
    }

    @EventHandler
    public void onKnockback(EntityKnockbackEvent e) {
        if (e instanceof EntityKnockbackByEntityEvent) return;
        var entity = e.getEntity();
        var claim = claims.getClaim(entity);
        if (claim != null && !claim.hasPermission(null, EntityPermission.DAMAGE)) {
            e.setCancelled(true);
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
