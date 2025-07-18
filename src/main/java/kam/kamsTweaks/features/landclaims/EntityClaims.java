package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

public class EntityClaims implements Listener {
    public final Map<UUID, EntityClaim> claims = new HashMap<>();
    boolean hasPermission(Player player, Entity entity, EntityPermission permission) {
        if (!claims.containsKey(entity.getUniqueId())) return true;
        EntityClaim claim = claims.get(entity.getUniqueId());
        if (player != null && claim.m_owner != null && claim.m_owner.getUniqueId().equals(player.getUniqueId())) return true;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("entity-claims.enabled", true)) return;
        if (e.getRightClicked() instanceof Creature c) {
            if (ItemManager.getType(e.getPlayer().getInventory().getItemInMainHand()) == ItemManager.ItemType.CLAIMER) {
                e.setCancelled(true);
                if (c instanceof Monster) return;
                if (claims.containsKey(e.getRightClicked().getUniqueId())) {
                    OfflinePlayer owner = claims.get(e.getRightClicked().getUniqueId()).m_owner;
                    if (owner.getUniqueId().equals(e.getPlayer().getUniqueId())) {
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
                    e.getPlayer().sendMessage(Component.text("This entity is claimed by ").append(Component.text(owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                    return;
                }
                e.getPlayer().sendMessage(Component.text("This entity is claimed by ").append(Component.text(owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("entity-claims.enabled", true)) return;
        if (e.getEntity() instanceof Creature c) {
            if (c instanceof Monster) return;
            if (!(e.getDamager() instanceof Player)) return;
            if (!hasPermission((Player) e.getDamager(), c, EntityPermission.KILL)) {
                OfflinePlayer owner = claims.get(c.getUniqueId()).m_owner;
                if (e.getDamager().hasPermission("kamstweaks.landclaims.bypass")) {
                    e.getDamager().sendMessage(Component.text("This entity is claimed by ").append(Component.text(owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(", but you are bypassing the claim.")));
                    return;
                }
                e.getDamager().sendMessage(Component.text("This entity is claimed by ").append(Component.text(owner == null ? "the server" : owner.getName() == null ? "Unknown player" : owner.getName()).color(NamedTextColor.GOLD)).append(Component.text(".")));
                e.setCancelled(true);
            }
        }
    }
}
