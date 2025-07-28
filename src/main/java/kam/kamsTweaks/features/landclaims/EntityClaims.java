package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class EntityClaims implements Listener {
    public final Map<UUID, EntityClaim> claims = new HashMap<>();

    boolean hasPermission(Player player, Entity entity, EntityPermission permission) {
        if (!claims.containsKey(entity.getUniqueId())) return true;
        EntityClaim claim = claims.get(entity.getUniqueId());
        if (player != null && claim.m_owner != null && claim.m_owner.getUniqueId().equals(player.getUniqueId()))
            return true;
        EntityPermission claimPerm = claim.m_perms.getOrDefault(player != null ? getServer().getOfflinePlayer(player.getUniqueId()) : null, claim.m_default);
        return claimPerm.compareTo(permission) >= 0;
    }

    public enum EntityPermission {
        NONE,
        INTERACT,
        KILL
    }

    public static class EntityClaim {
        OfflinePlayer m_owner;
        Map<OfflinePlayer, EntityPermission> m_perms = new HashMap<>();
        EntityPermission m_default = EntityPermission.NONE;

        public EntityClaim(OfflinePlayer owner) {
            m_owner = owner;
        }
    }

    final List<UUID> hasMessaged = new ArrayList<>();

    void message(Player player, String owner, boolean bypass) {
        if (hasMessaged.contains(player.getUniqueId())) return;
        hasMessaged.add(player.getUniqueId());
        if (bypass)
            player.sendMessage(Component.text("This entity is claimed by ").append(Component.text(owner).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
        else
            player.sendMessage(Component.text("This entity is claimed by ").append(Component.text(owner).color(NamedTextColor.GOLD)).append(Component.text(".")));
    }

    public void init() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.getInstance(), hasMessaged::clear, 1, 1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("entity-claims.enabled", true)) return;
        if (e.getRightClicked() instanceof Creature c) {
            if (ItemManager.getType(e.getPlayer().getInventory().getItemInMainHand()) == ItemManager.ItemType.CLAIMER) {
                e.setCancelled(true);
                if (c instanceof Monster) return;
                if (claims.containsKey(e.getRightClicked().getUniqueId())) {
                    OfflinePlayer owner = claims.get(e.getRightClicked().getUniqueId()).m_owner;
                    if (owner != null && owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
                        KamsTweaks.getInstance().m_landClaims.gui.showClaimGui(e.getPlayer(), null);
                        var ui = KamsTweaks.getInstance().m_landClaims.gui.guis.get(e.getPlayer());
                        ui.changeToScreen(ui.getScreen(5));
                        ui.targetEntity = e.getRightClicked();
                        return;
                    }
                    e.getPlayer().sendMessage(Component.text("This entity is already claimed by ").append(Component.text(owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                    return;
                }
                KamsTweaks.getInstance().m_landClaims.gui.showClaimGui(e.getPlayer(), null);
                var ui = KamsTweaks.getInstance().m_landClaims.gui.guis.get(e.getPlayer());
                ui.confirmType = "entity-claim";
                ui.getScreen(4).changeTitle(Component.text("Claim this " + e.getRightClicked().getType() + "?"));
                ui.changeToScreen(ui.getScreen(4));
                ui.targetEntity = e.getRightClicked();
                return;
            }
            if (c instanceof Monster) return;
            if (!hasPermission(e.getPlayer(), c, EntityPermission.INTERACT)) {
                OfflinePlayer owner = claims.get(e.getRightClicked().getUniqueId()).m_owner;
                if (e.getPlayer().hasPermission("kamstweaks.landclaims.bypass")) {
                    message(e.getPlayer(), owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName(), true);
                    return;
                }
                message(e.getPlayer(), owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName(), false);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityEntityDamage(EntityDamageByEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("entity-claims.enabled", true)) return;
        if (e.getEntity() instanceof Creature c) {
            if (c instanceof Monster) return;
            if (c instanceof Wolf wolf) {
                if (wolf.getTarget() != null) {
                    if (wolf.getTarget() == e.getDamager()) {
                        Logger.info("Test");
                        return;
                    }
                }
            }
            if (e.getDamager() instanceof Player player) {
                if (hasPermission(player, c, EntityPermission.KILL)) return;
                EntityClaim claim = claims.get(c.getUniqueId());
                if (claim == null) return;
                OfflinePlayer owner = claim.m_owner;
                if (player.hasPermission("kamstweaks.landclaims.bypass")) {
                    message(player, owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName(), true);
                    return;
                }
                message(player, owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName(), false);
                e.setCancelled(true);
            } else {
                if (hasPermission(null, c, EntityPermission.KILL)) return;
                EntityClaim claim = claims.get(c.getUniqueId());
                if (claim == null) return;
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityBlockDamage(EntityDamageByBlockEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("entity-claims.enabled", true)) return;
        if (e.getEntity() instanceof Creature c) {
            if (c instanceof Monster) return;
            if (hasPermission(null, c, EntityPermission.KILL)) return;
            EntityClaim claim = claims.get(c.getUniqueId());
            if (claim == null) return;
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("entity-claims.enabled", true)) return;
        if (event.getEntity() instanceof Creature c) {
            if (c instanceof Monster) return;
            if (hasPermission(null, c, EntityPermission.KILL)) return;
            EntityClaim claim = claims.get(c.getUniqueId());
            if (claim == null) return;
            switch (event.getCause()) {
                case FIRE, FIRE_TICK, FALL, DROWNING, CAMPFIRE, SUFFOCATION -> event.setCancelled(true);
                default -> {}
            }
        }
    }

    @EventHandler
    public void entityDie(EntityDeathEvent e) {
        claims.remove(e.getEntity().getUniqueId());
    }
}
