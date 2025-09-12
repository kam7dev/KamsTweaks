package kam.kamsTweaks.features.claims;

import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ClaimProtections implements Listener {
    Claims claims = null;

    public void setup(Claims claims) {
        this.claims = claims;
    }

    final List<UUID> hasMessaged = new ArrayList<>();

    // just a helper cause its nicer
    void message(Player player, Component message) {
        if (hasMessaged.contains(player.getUniqueId()))
            return;
        hasMessaged.add(player.getUniqueId());
        player.sendMessage(message);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getPlayer().getTargetEntity(5) instanceof Creature)
            return;
        if (e.getItem() != null && ItemManager.getType(e.getItem()) == ItemManager.ItemType.CLAIMER) {
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                    claims.handleItem(e);
                }
                e.setCancelled(true);
                return;
            } else if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                if (KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true)) {
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("land-claims.enabled", true))
                        return;
                    assert e.getClickedBlock() != null;
                    Claims.LandClaim res = claims.getLandClaim(e.getClickedBlock().getLocation());
                    if (res == null) e.getPlayer().sendMessage(Component.text("This land isn't claimed."));
                    else if (res.owner == null) e.getPlayer().sendMessage(Component.text("This claim is owned by the server."));
                    else e.getPlayer().sendMessage(Component.text("This claim is owned by ").append(Component.text(res.owner.getName() == null ? "Unknown player" : res.owner.getName()).color(NamedTextColor.GOLD)));
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
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
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
}
