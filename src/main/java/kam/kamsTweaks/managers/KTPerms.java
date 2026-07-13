package kam.kamsTweaks.managers;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.HashMap;
import java.util.Map;

public enum KTPerms {

    // Generic
    CONFIGURE("kamstweaks.configure", "Configure the mod's options", PermissionDefault.OP),
    LOGGER("kamstweaks.logger", "Manage the mod's logger", PermissionDefault.OP),
    ITEM_GIVE("kamstweaks.item.give", "Give self KamsTweaks items", PermissionDefault.OP),
    SAVE("kamstweaks.save", "Save the mod's data", PermissionDefault.OP),

    // Smaller features
    KEEPINV("kamstweaks.keepinv", "Toggle keep inventory for self", PermissionDefault.TRUE),
    SILK_SPAWNERS("kamstweaks.silk-spawners", "Pick up spawners with silk touch", PermissionDefault.TRUE),
    NO_TOO_EXPENSIVE("kamstweaks.no-too-expensive", "Removes too expensive.", PermissionDefault.TRUE),
    ENABLE_DRAGON_FIGHT("kamstweaks.enable-dragon-fight", "Enable the dragon fight", PermissionDefault.OP),
    VIRTUAL_INSANITY("kamstweaks.virtual-insanity", "Use the Virtual Insanity joke command", PermissionDefault.TRUE),
    SLASH_HAT("kamstweaks.slash-hat", "Put any item on head with /hat", PermissionDefault.TRUE),

    GRAVES("kamstweaks.graves", "Have graves on death", PermissionDefault.TRUE),

    // Claims
    CLAIMS_LAND("kamstweaks.claims.land", "Claim land", PermissionDefault.TRUE),
    CLAIMS_ENTITY("kamstweaks.claims.entity", "Claim entities", PermissionDefault.TRUE),
    CLAIMS_MANAGE("kamstweaks.claims.manage", "Manage other people's claims", PermissionDefault.OP),

    // Teleportation
    TP_BACK("kamstweaks.teleportation.back", "Use /back", PermissionDefault.TRUE),
    TP_SPAWN("kamstweaks.teleportation.spawn", "Use /spawn", PermissionDefault.TRUE),
    TP_WARP("kamstweaks.teleportation.warp", "Use warps", PermissionDefault.TRUE),
    TP_WARP_ADD("kamstweaks.teleportation.warp.add", "Add warps", PermissionDefault.OP),
    TP_HOME("kamstweaks.teleportation.home", "Use homes", PermissionDefault.TRUE),
    TP_TPA_HERE("kamstweaks.teleportation.tpa.here", "Request to teleport other players to them", PermissionDefault.TRUE),
    TP_TPA("kamstweaks.teleportation.tpa", "Request to teleport to other players", PermissionDefault.FALSE, Map.of(TP_TPA_HERE, true)),
    TP("kamstweaks.teleportation", "Use the teleportation features", PermissionDefault.TRUE, Map.of(
            TP_BACK, true,
            TP_SPAWN, true,
            TP_WARP, true,
            TP_HOME, true,
            TP_TPA, true
    )),

    // Nicknames
    NICKNAME_MINIMESSAGE("kamstweaks.nicks.minimessage", "Use minimessage for nicknames", PermissionDefault.TRUE),
    NICKNAME_COLORS("kamstweaks.nicks.colors", "Use colors in nicknames", PermissionDefault.TRUE),
    NICKNAME("kamstweaks.nicks", "Set a nickname", PermissionDefault.TRUE, Map.of(NICKNAME_MINIMESSAGE, true, NICKNAME_COLORS, true)),

    // Staff stuff
    VANISH("kamstweaks.staff.vanish", "Appear offline to other players by choice", PermissionDefault.OP),

    ;
    public final String id;
    public final String description;
    public final PermissionDefault default_;
    public final Map<String, Boolean> children = new HashMap<>();

    KTPerms(String id, String description, PermissionDefault default_, Map<KTPerms, Boolean> children) {
        this.id = id;
        this.description = description;
        this.default_ = default_;
        var _ = children.entrySet().stream().map((entry) -> this.children.put(entry.getKey().id, entry.getValue()));
    }

    KTPerms(String id, String description, PermissionDefault default_) {
        this(id, description, default_, Map.of());
    }

    public static void registerPermissions() {
        for (var perm : KTPerms.values()) {
            Bukkit.getPluginManager().addPermission(new Permission(perm.id, perm.description, perm.default_));
        }
    }

    public static boolean hasPermission(Permissible who, String perm) {
        return who.hasPermission(perm);
    }

    public static boolean hasPermission(Permissible who, KTPerms perm) {
        return who.hasPermission(perm.id);
    }

    public static boolean hasPermission(CommandSourceStack who, KTPerms perm) {
        return who.getSender().hasPermission(perm.id);
    }
}
