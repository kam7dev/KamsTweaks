package kam.kamsTweaks.features.landclaims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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
        }
    }
}
