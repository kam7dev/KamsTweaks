package kam.kamsTweaks.managers;

import org.bukkit.permissions.PermissionDefault;

public enum KTPerms {



    ;
    final String id;
    final String description;
    final PermissionDefault default_;

    KTPerms(String id, String description, PermissionDefault default_) {
        this.id = id;
        this.description = description;
        this.default_ = default_;
    }

    public static void registerPermissions() {

    }
}
