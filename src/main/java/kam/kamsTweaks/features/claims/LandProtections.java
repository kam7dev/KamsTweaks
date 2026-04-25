package kam.kamsTweaks.features.claims;

import com.destroystokyo.paper.event.block.AnvilDamagedEvent;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.Names;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

import kam.kamsTweaks.features.claims.LandClaims.*;

public class LandProtections implements Listener {
    private LandClaims claims;

    public void setup(LandClaims landClaims) {
        this.claims = landClaims;
    }

    // shortcut
    void message(Player player, Component component) {
        claims.instance.message(player, component);
    }

    @EventHandler
    public void onLecternTake(PlayerTakeLecternBookEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("land-claims.enabled", true)) return;

        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(e.getLectern().getLocation());
        if (claim == null) return;
        if (!claim.hasPermissions(player, LandPermission.INTERACT_BLOCK, AdvancedLandPermission.LECTERN_TAKE)) {
            message(player, Component.text("You don't have block interaction permissions here! (Claim owned by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLecternInsert(PlayerInsertLecternBookEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("land-claims.enabled", true)) return;

        Player player = e.getPlayer();
        LandClaim claim = claims.getClaim(e.getLectern().getLocation());
        if (claim == null) return;
        if (!claim.hasPermissions(player, LandPermission.INTERACT_BLOCK, AdvancedLandPermission.LECTERN_INSERT)) {
            message(player, Component.text("You don't have block interaction permissions here! (Claim owned by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrain(CauldronLevelChangeEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("land-claims.enabled", true)) return;
        Player who = e.getEntity() instanceof Player ? (Player) e.getEntity() : null;
        LandClaim claim = claims.getClaim(e.getBlock().getLocation());
        if (claim == null) return;
        if (!claim.hasPermissions(who, LandPermission.INTERACT_BLOCK, AdvancedLandPermission.DRAIN_CAULDRON)) {
            if (who != null) message(who, Component.text("You don't have block interaction permissions here! (Claim owned by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onAnvil(AnvilDamagedEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("land-claims.enabled", true)) return;
        e.getInventory().getViewers().stream()
                .filter(h -> h instanceof Player)
                .map(h -> (Player) h)
                .findFirst()
                .ifPresent(player -> {
                    LandClaim claim = claims.getClaim(e.getInventory().getLocation());
                    if (claim == null) return;
                    if (!claim.hasPermissions(player, LandPermission.INTERACT_BLOCK, AdvancedLandPermission.DAMAGE_ANVIL)) {
                        message(player, Component.text("You don't have block break permissions here! (Claim owned by ").append(Names.instance.getRenderedName(claim.owner), Component.text(")")));
                        e.setCancelled(true);
                    }
                });

    }
}
