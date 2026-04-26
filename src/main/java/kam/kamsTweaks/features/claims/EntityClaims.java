package kam.kamsTweaks.features.claims;

import kam.kamsTweaks.Logger;
import kam.kamsTweaks.features.claims.Claims.OptBool;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityClaims {
    public Map<UUID, EntityClaim> claims = new HashMap<>();

    Claims instance;

    public void setup(Claims instance) {
        this.instance = instance;
    }

    public @Nullable EntityClaim getClaim(Entity entity) {
        return claims.getOrDefault(entity.getUniqueId(), null);
    }

    public enum EntityPermission {
        INTERACT,
        DAMAGE
    }

    public enum AdvancedEntityPermission {

    }

    public static class Permissions implements Cloneable{
        Entity who;
        Map<EntityPermission, OptBool> bools;
        Map<AdvancedEntityPermission, OptBool> advancedBools;

        public Permissions(Entity who) {
            this.who = who;
        }

        public static Permissions defaultPerms;
        static {
            defaultPerms = new Permissions(null);
            defaultPerms.bools.put(EntityPermission.INTERACT, OptBool.FALSE);
            defaultPerms.bools.put(EntityPermission.DAMAGE, OptBool.FALSE);
//            defaultPerms.advancedBools.put(AdvancedEntityPermission.S, OptBool.FALSE);
        }

        OptBool getBoolPermission(EntityPermission perm) {
            return bools.getOrDefault(perm, OptBool.DEFAULT);
        }

        OptBool getBoolPermission(AdvancedEntityPermission perm) {
            return advancedBools.getOrDefault(perm, OptBool.DEFAULT);
        }

        @Override
        public Permissions clone() {
            try {
                return (Permissions) super.clone();
            } catch (Exception e) {
                Logger.handleException(e);
                return null;
            }
        }
    }

    public static class EntityClaim {
        public OfflinePlayer owner;
        public static int nextId;
        public int id;
        public String name = "Unnamed claim";

        public UUID entity;
        public boolean canAggro = false;

        public Map<Entity, EntityClaims.Permissions> perms;
        public EntityClaims.Permissions defaultPerms = EntityClaims.Permissions.defaultPerms.clone();
        public EntityClaims.Permissions defaultEntityPerms = EntityClaims.Permissions.defaultPerms.clone();

        public EntityClaim(OfflinePlayer owner, UUID entity) {
            this.id = nextId;
            nextId++;
            this.owner = owner;
            this.entity = entity;
        }

        public EntityClaim(OfflinePlayer owner, int id, UUID entity) {
            nextId = Math.max(nextId, id + 1);
            this.id = id;
            this.owner = owner;
            this.entity = entity;
        }

        public EntityClaim(EntityClaim orig, UUID entity) {
            this.id = nextId;
            nextId++;
            this.owner = orig.owner;
            this.entity = entity;

            canAggro = orig.canAggro;
            perms = orig.perms;
            defaultPerms = orig.defaultPerms;
            defaultEntityPerms = orig.defaultEntityPerms;
        }

        public EntityClaim(EntityClaim orig, UUID entity, int id) {
            nextId = Math.max(nextId, id + 1);
            this.id = id;
            this.owner = orig.owner;
            this.entity = entity;

            canAggro = orig.canAggro;
            perms = orig.perms;
            defaultPerms = orig.defaultPerms;
            defaultEntityPerms = orig.defaultEntityPerms;
        }
    }

    public boolean isClaimable(Entity e) {
        if (e instanceof Boat) return true;
        if (!(e instanceof Mob)) return false;
        switch (e.getType()) {
            case ELDER_GUARDIAN, ENDER_DRAGON, WITHER, WARDEN -> {
                return false;
            }
            default -> {
                return true;
            }
        }
    }
}
