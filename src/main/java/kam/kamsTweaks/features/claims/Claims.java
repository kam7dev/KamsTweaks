package kam.kamsTweaks.features.claims;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Claims extends Feature {
    public ClaimProtections protections = new ClaimProtections();
    public ClaimsDialogGui dialogGui = new ClaimsDialogGui();
    public List<LandClaim> landClaims = new ArrayList<>();
    public List<Player> currentlyClaiming = new ArrayList<>();

    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("land-claims.enabled", "land-claims.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("land-claims.max-claims", "land-claims.max-claims", 30, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.IntegerConfig("land-claims.max-claim-size", "land-claims.max-claim-size", 50000, "kamstweaks.configure"));
        protections.setup(this);
        dialogGui.setup(this);
        Bukkit.getServer().getPluginManager().registerEvents(protections, KamsTweaks.getInstance());
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {

    }

    @Override
    public void loadData() {

    }

    @Override
    public void saveData() {

    }

    public static class Claim {
        public OfflinePlayer owner;
        public Map<OfflinePlayer, List<ClaimPermission>> perms = new HashMap<>();
        public List<ClaimPermission> defaults = new ArrayList<>();
        public boolean hasPermission(OfflinePlayer player, ClaimPermission perm) {
            if (perms.containsKey(player)) {
                var p = perms.get(player);
                return p.contains(perm);
            }
            return false;
        }

        public Claim(OfflinePlayer owner) {
            this.owner = owner;
        }
    }

    public enum ClaimPermission {
        // claim perms
        INTERACT_DOOR,
        INTERACT_BLOCK,
        BLOCK_PLACE,
        BLOCK_BREAK,

        // entity perms
        DAMAGE_ENTITY,
        INTERACT_ENTITY,

        DEFAULT, // incompatible with all others
    }


    public static class LandClaim extends Claim {
        Location start;
        Location end;
        String name = "Unnamed claim";
        Integer priority = 0;

        public LandClaim(OfflinePlayer owner, Location start, Location end) {
            super(owner);
            this.defaults.add(ClaimPermission.INTERACT_DOOR);
            this.start = start;
            this.end = end;
        }
    }

    public @Nullable LandClaim getLandClaim(Location where) {
        LandClaim ret = null;
        for (var claim : landClaims) {
            if (LocationUtils.inBounds(where, claim.start, claim.end)) {
                if (ret == null || claim.priority > ret.priority) {
                    ret = claim;
                }
            }
        }
        return ret;
    }
}
