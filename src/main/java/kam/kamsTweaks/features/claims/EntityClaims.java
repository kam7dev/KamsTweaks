package kam.kamsTweaks.features.claims;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.utils.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.features.fun.Names;
import kam.kamsTweaks.features.claims.Claims.OptBool;
import kam.kamsTweaks.features.claims.gui.EntityClaimPage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static kam.kamsTweaks.features.claims.LandClaims.nonNull;

import java.util.*;

public class EntityClaims {
    public Map<UUID, EntityClaim> claims = new HashMap<>();

    Claims instance;
    public EntityProtections prots = new EntityProtections();

    public void setup(Claims instance) {
        this.instance = instance;
        prots.setup(this);
        Bukkit.getServer().getPluginManager().registerEvents(prots, KamsTweaks.get());
    }

    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands, LiteralArgumentBuilder<CommandSourceStack> baseCmd) {
        Command<CommandSourceStack> bcb = ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                new EntityClaimPage(player).show();
                if (player != sender) {
                    sender.sendMessage(KTStrings.getFor(KTStrings.CLAIMS_SHOWED_GUI_TO, Names.instance.getRenderedName(player)).color(NamedTextColor.GOLD));
                }
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/claims entity")).color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        };

        var entity = Commands.literal("ec").executes(bcb);
        var baseEntity = Commands.literal("entity").executes(bcb);

        List<LiteralArgumentBuilder<CommandSourceStack>> cmdList = new ArrayList<>();

        var create = Commands.literal("create").then(Commands.argument("entity", ArgumentTypes.entity()).executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            var target = ctx.getArgument("entity", EntitySelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
            if (executor != sender) {
                sender.sendMessage(KTStrings.getFor(KTStrings.CLAIMS_CANT_FOR_OTHERS).color(NamedTextColor.RED));
                return Command.SINGLE_SUCCESS;
            }
            if (executor instanceof Player player) {
                createClaim(player, target);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/claims entity create")).color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }));
        cmdList.add(create);

        var delete = Commands.literal("delete").then(Commands.argument("id", IntegerArgumentType.integer()).suggests((ctx, builder) -> {
            if (!(ctx.getSource().getSender() instanceof Player player))
                return builder.buildFuture();
            claims.forEach((uuid, claim) -> {
                if (claim.owner != null && player.getUniqueId().equals(claim.owner.getUniqueId())) {
                    builder.suggest(claim.id);
                }
            });
            return builder.buildFuture();
        }).executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            var id = ctx.getArgument("id", Integer.class);
            if (executor instanceof Player player) {
                var claim = getClaim(id);
                if (claim == null) {
                    sender.sendMessage(KTStrings.getFor(KTStrings.CLAIMS_NONEXISTENT).color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                deleteClaim(claim, player);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/claims entity delete")).color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        })).then(Commands.argument("uuid", ArgumentTypes.entity()).executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            var id = ctx.getArgument("uuid", EntitySelectorArgumentResolver.class).resolve(ctx.getSource()).getFirst();
            if (executor instanceof Player player) {
                var claim = getClaim(id);
                if (claim == null) {
                    sender.sendMessage(KTStrings.getFor(KTStrings.CLAIMS_NONEXISTENT).color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                deleteClaim(claim, player);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/claims entity delete")).color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }));
        cmdList.add(delete);

        cmdList.add(Commands.literal("list").executes(ctx -> {
            var sender = ctx.getSource().getSender();
            var executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                listClaims(player, sender);
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/claims entity list")).color(NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }));

        for (var thing : cmdList) {
            entity.then(thing);
            baseEntity.then(thing);
        }

        baseCmd.then(baseEntity);
        commands.registrar().register(entity.build());
    }

    void savePerms(FileConfiguration config, String path, Permissions perms) {
        if (perms.who != null) config.set(path + ".who", perms.who.toString());
        perms.bools.forEach((perm, val) -> config.set(path + ".bools." + perm.name(), val.name()));
        perms.advancedBools.forEach((perm, val) -> config.set(path + ".advbools." + perm.name(), val.name()));
    }

    public void save() {
        var cfg = Claims.get().claimsConfig;
        var primary = Bukkit.getWorlds().getFirst().getUID();
        cfg.set(primary + ".entityv3", null);

        claims.forEach((uuid, claim) -> {
            var path = primary + ".entityv3." + claim.entity;
            cfg.set(path + ".id", claim.id);
            if (claim.owner != null) cfg.set(path + ".owner", claim.owner.getUniqueId().toString());

            cfg.set(path + ".config.aggro", claim.config.canAggro);
            cfg.set(path + ".config.test", claim.config.testMode);

            savePerms(cfg, path + ".default", claim.defaultPerms);
            savePerms(cfg, path + ".entity", claim.defaultEntityPerms);
            claim.perms.forEach((u, perms) -> savePerms(cfg, path + ".perms." + u, perms));
        });
    }

    void loadPerms(FileConfiguration config, String path, Permissions perms) {
        try {
            var who = config.getString(path + ".who");
            if (who != null) perms.who = UUID.fromString(who);
            if (config.contains(path + ".bools")) {
                for (var ps : nonNull(config.getConfigurationSection(path  + ".bools")).getKeys(false)) {
                    var perm = EntityPermission.valueOf(ps);
                    perms.bools.put(perm, OptBool.valueOf(config.getString(path + ".bools." + ps)));
                }
            }
            if (config.contains(path + ".advbools")) {
                for (var ps : nonNull(config.getConfigurationSection(path  + ".advbools")).getKeys(false)) {
                    var perm = AdvancedEntityPermission.valueOf(ps);
                    perms.advancedBools.put(perm, OptBool.valueOf(config.getString(path + ".advbools." + ps)));
                }
            }
        } catch (Exception e) {
            Logger.error("Failed loading perms from " + path + ".");
            Logger.handleException(e);
        }
    }

    void loadLegacy() {
        var cfg = Claims.get().claimsConfig;
        if (cfg.contains("entities")) {
            for (String key : Objects.requireNonNull(cfg.getConfigurationSection("entities")).getKeys(false)) {
                try {
                    UUID entity = UUID.fromString(key);
                    String ownerStr = cfg.getString("entities." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    EntityClaim claim;
                    if (cfg.contains("entities." + key + ".id")) {
                        claim = new EntityClaim(owner == null ? null : Bukkit.getServer().getOfflinePlayer(owner), cfg.getInt("entities." + key + ".id"), entity);
                    } else {
                        claim = new EntityClaim(owner == null ? null : Bukkit.getServer().getOfflinePlayer(owner), entity);
                    }
                    try {
                        if (cfg.contains("entities." + key + ".defaults")) {
                            for (String def : Objects.requireNonNull(cfg.getStringList("entities." + key + ".defaults"))) {
                                switch(def) {
                                    case "INTERACT_ENTITY": {
                                        claim.defaultPerms.bools.put(EntityPermission.INTERACT, OptBool.True);
                                        break;
                                    }
                                    case "DAMAGE_ENTITY": {
                                        claim.defaultPerms.bools.put(EntityPermission.DAMAGE, OptBool.True);
                                        break;
                                    }
                                }
                            }
                        } else if (cfg.contains("entities." + key + ".default")) {
                            switch (Objects.requireNonNull(cfg.getString("entities." + key + ".default"))) {
                                case "INTERACT":
                                    claim.defaultPerms.bools.put(EntityPermission.INTERACT, OptBool.True);
                                    break;
                                case "KILL":
                                    claim.defaultPerms.bools.put(EntityPermission.DAMAGE, OptBool.True);
                                    break;
                            }
                        }
                    } catch (NullPointerException e) {
                        Logger.exceptions.add(e);
                        Logger.warn(e.getMessage());

                    }

                    try {
                        if (cfg.contains("entities." + key + ".permissions")) {
                            for (String uuid : Objects.requireNonNull(cfg.getConfigurationSection("entities." + key + ".permissions")).getKeys(false)) {
                                var perms = claim.getPerms(UUID.fromString(uuid));
                                for (String def : Objects.requireNonNull(cfg.getStringList("entities." + key + ".permissions." + uuid))) {
                                    switch(def) {
                                        case "INTERACT_ENTITY": {
                                            perms.bools.put(EntityPermission.INTERACT, OptBool.True);
                                            break;
                                        }
                                        case "DAMAGE_ENTITY": {
                                            perms.bools.put(EntityPermission.DAMAGE, OptBool.True);
                                            break;
                                        }
                                    }
                                }
                            }
                             
                        } else if (cfg.contains("entities." + key + ".perms")) {
                            for (String uuid : Objects.requireNonNull(cfg.getConfigurationSection("entities." + key + ".perms")).getKeys(false)) {
                                var perms = claim.getPerms(UUID.fromString(uuid));
                                switch (Objects.requireNonNull(cfg.getString("entities." + key + ".perms." + uuid))) {
                                    case "INTERACT":
                                        perms.bools.put(EntityPermission.INTERACT, OptBool.True);
                                        break;
                                    case "KILL":
                                        perms.bools.put(EntityPermission.DAMAGE, OptBool.True);
                                        break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.exceptions.add(e);
                        Logger.warn(e.getMessage());
                    }
                    claims.put(UUID.fromString(key), claim);
                } catch (Exception e) {
                    Logger.exceptions.add(e);
                    Logger.warn(e.getMessage());
                }
            }
        }
        cfg.set("entities", null);

    }

    public void load() {
        claims.clear();
        var cfg = Claims.get().claimsConfig;
        var primary = Bukkit.getWorlds().getFirst().getUID();
        if (cfg.contains(primary + ".entityv3")) {
            for (var key : nonNull(cfg.getConfigurationSection(primary + ".entityv3.")).getKeys(false)) {
                try {
                    var path = primary + ".entityv3." + key;
                    var uuid = UUID.fromString(key);
                    var oUuid = cfg.contains(path + ".owner") ? UUID.fromString(nonNull(cfg.getString(path + ".owner"))) : null;
                    var id = cfg.getInt(path + ".id");
                    var claim = new EntityClaim(oUuid != null ? Bukkit.getOfflinePlayer(oUuid) : null, id, uuid);
                    claim.config.canAggro = cfg.getBoolean(path + ".config.aggro");
                    claim.config.testMode = cfg.getBoolean(path + ".config.test");

                    loadPerms(cfg, path + ".default", claim.defaultPerms);
                    loadPerms(cfg, path + ".entity", claim.defaultEntityPerms);
                    if (cfg.contains(path + ".perms")) {
                        for (var perm : nonNull(cfg.getConfigurationSection(path + ".perms")).getKeys(false)) {
                            try {
                                loadPerms(cfg, path + ".perms." + uuid, claim.getPerms(UUID.fromString(perm)));
                            } catch (Exception e) {
                                Logger.error("Failed to load permissions for " + uuid + " in entity claim " + id);
                                Logger.handleException(e);
                            }
                        }
                    }

                    claims.put(uuid, claim);
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
        try {
            loadLegacy();
        } catch (Exception e) {
            Logger.handleException(e);
        }
    }

    public void listClaims(Player who, CommandSender receiver) {
        var ref = new Object() {
            Component msg = Component.empty();
            int i = 0;
        };
        claims.forEach((uuid, claim) -> {
            if (claim.owner != null && who.getUniqueId().equals(claim.owner.getUniqueId())) {
                var entity = Bukkit.getEntity(uuid);
                if (entity != null) {
                    ref.i++;
                    ref.msg = ref.msg.appendNewline().append(KTStrings.getFor(KTStrings.EC_INFO,
                            Component.text(claim.id).color(NamedTextColor.GOLD),
                        Names.instance.getEntityRenderedName(entity).color(NamedTextColor.AQUA),
                            Component.text(entity.getLocation().getBlockX() + ", " + entity.getLocation().getBlockY() + ", " + entity.getLocation().getBlockZ()).color(NamedTextColor.GREEN),
                            Component.text(entity.getLocation().getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE)));
                }
            }
        });
        if (receiver == who) {
            receiver.sendMessage(KTStrings.getFor(KTStrings.CLAIMS_YOU_HAVE, Component.text(ref.i).color(NamedTextColor.GOLD), KTStrings.getFor(KTStrings.EC)).append(ref.msg));
        } else {
            receiver.sendMessage(KTStrings.getFor(KTStrings.CLAIMS_THEY_HAVE, Names.instance.getRenderedName(who), Component.text(ref.i).color(NamedTextColor.GOLD), KTStrings.getFor(KTStrings.EC)).append(ref.msg));
        }
    }

    public void listClaims(Player who) {
        listClaims(who, who);
    }

    public void createClaim(Player who, Entity entity) {
        var max = KamsTweaks.get().getConfig().getInt("entity-claims.max-claims", 1000);
        int count = 0;
        for (var c : claims.values()) {
            if (c.owner == null || (!c.owner.getUniqueId().equals(who.getUniqueId()))) {
                count += 1;
            }
        }
        if (count >= max) {
            who.sendMessage(KTStrings.getFor(KTStrings.EC_MAX, Component.text(count), Component.text(max)).color(NamedTextColor.RED));
            return;
        }
        if (!isClaimable(entity)) {
            who.sendMessage(KTStrings.getFor(KTStrings.EC_UNCLAIMABLE).color(NamedTextColor.RED));
            return;
        }
        if (claims.containsKey(entity.getUniqueId())) {
            who.sendMessage(KTStrings.getFor(KTStrings.EC_ALREADY_CLAIMED, claims.get(entity.getUniqueId()).getOwnerName()).color(NamedTextColor.RED));
        } else {
            var claim = new EntityClaim(who, entity.getUniqueId());
            claims.put(entity.getUniqueId(), claim);
            entity.setFireTicks(0);
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
                mob.setRemoveWhenFarAway(false);
            }
            entity.setPersistent(true);
            who.sendMessage(KTStrings.getFor(KTStrings.EC_CLAIMED, Names.instance.getEntityRenderedName(entity), Component.text(claim.id).color(NamedTextColor.GOLD)));
        }
    }

    public void deleteClaim(EntityClaim claim, Player who) {
        var mt = claim.getManagementType(who);
        if (mt == Claims.ManagementType.None) {
            who.sendMessage(KTStrings.getFor(KTStrings.CLAIM_CANT_MANAGE).color(NamedTextColor.RED));
            return;
        } else if (mt == Claims.ManagementType.Op) {
            KamsTweaks.get().sendToOps(KTStrings.getFor(KTStrings.CLAIM_OP_DELETE, Component.text(who.getName()), Component.text(claim.getOwnerUsername()), KTStrings.getFor(KTStrings.ENTITY)).decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), who);
            Logger.warn("[Claim management] " + who.getName() + " just deleted " + claim.getOwnerUsername() + "'s entity claim.");
        }
        claims.remove(claim.entity);
        who.sendMessage(KTStrings.getFor(KTStrings.CLAIM_DELETED).color(NamedTextColor.GREEN));
    }

    public @Nullable EntityClaim getClaim(Entity entity) {
        if (entity == null) return null;
        return claims.getOrDefault(entity.getUniqueId(), null);
    }

    public @Nullable EntityClaim getClaim(int id) {
        return claims.values().stream().filter(carnet -> id == carnet.id).findFirst().orElse(null);
    }

    public enum EntityPermission {
        INTERACT(KTStrings.getFor(KTStrings.EC_INTERACT)),
        DAMAGE(KTStrings.getFor(KTStrings.EC_DAMAGE)),

        ;

        public final Component label;
        public final OptBool defaultValue;

        EntityPermission(Component label, OptBool defaultValue) {
            this.label = label;
            this.defaultValue = defaultValue;
        }
        EntityPermission(Component label) {
            this.label = label;
            this.defaultValue = OptBool.False;
        }
    }

    public enum AdvancedEntityPermission {

        ;

        public final Component label;
        public final OptBool defaultValue;

        AdvancedEntityPermission(Component label, OptBool defaultValue) {
            this.label = label;
            this.defaultValue = defaultValue;
        }
        AdvancedEntityPermission(Component label) {
            this.label = label;
            this.defaultValue = OptBool.False;
        }
    }

    public static class Permissions implements Cloneable {
        public EntityClaim claim;
        public UUID who;
        boolean isClaimDefault = false;
        Map<EntityPermission, OptBool> bools = new HashMap<>();
        Map<AdvancedEntityPermission, OptBool> advancedBools = new HashMap<>();

        public Permissions(EntityClaim claim, UUID who) {
            this.claim = claim;
            this.who = who;
        }

        public Permissions(EntityClaim claim, boolean isClaimDefault) {
            this.claim = claim;
            this.isClaimDefault = isClaimDefault;
        }

        public OptBool getBoolPermission(EntityPermission perm) {
            if (isClaimDefault) return bools.getOrDefault(perm, perm.defaultValue);
            return bools.getOrDefault(perm, OptBool.Default);
        }

        public OptBool getBoolPermission(EntityPermission perm, Permissions backup) {
            if (isClaimDefault) return bools.getOrDefault(perm, perm.defaultValue);
            var ret = bools.getOrDefault(perm, OptBool.Default);
            if (backup != null && ret == OptBool.Default) ret = backup.getBoolPermission(perm);
            return ret;
        }

        public OptBool getBoolPermission(AdvancedEntityPermission perm) {
            if (isClaimDefault) return advancedBools.getOrDefault(perm, perm.defaultValue);
            return advancedBools.getOrDefault(perm, OptBool.Default);
        }

        public OptBool getBoolPermission(AdvancedEntityPermission perm, Permissions backup) {
            if (isClaimDefault) return advancedBools.getOrDefault(perm, perm.defaultValue);
            var ret = advancedBools.getOrDefault(perm, OptBool.Default);
            if (backup != null && ret == OptBool.Default) ret = backup.getBoolPermission(perm);
            return ret;
        }

        public void setBoolPermission(EntityPermission perm, OptBool value) {
            bools.put(perm, value);
        }

        public void setBoolPermission(AdvancedEntityPermission perm, OptBool value) {
            advancedBools.put(perm, value);
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

    public static class ClaimConfig {
        public Boolean testMode = false;

        public Boolean canAggro = false;
    }

    public static class EntityClaim {
        public @Nullable OfflinePlayer owner;
        public static int nextId;
        public int id;

        public UUID entity;
        public ClaimConfig config = new ClaimConfig();

        public Map<UUID, Permissions> perms = new HashMap<>();
        public Permissions defaultPerms = new Permissions(this, true).clone();
        public Permissions defaultEntityPerms = new Permissions(this, null).clone();

        public Permissions getPerms(UUID who) {
            if (!perms.containsKey(who)) {
                var p = new Permissions(this, who);
                perms.put(who, p);
                return p;
            }
            return perms.get(who);
        }

        public EntityClaim(@Nullable OfflinePlayer owner, UUID entity) {
            this.id = nextId;
            nextId++;
            this.owner = owner;
            this.entity = entity;
        }

        public EntityClaim(@Nullable OfflinePlayer owner, int id, UUID entity) {
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

            config.canAggro = orig.config.canAggro;
            config.testMode = orig.config.testMode;

            orig.perms.forEach((uuid, perm) -> {
                var clone = perm.clone();
                clone.who = entity;
                clone.claim = this;
                perms.put(uuid, clone);
            });

            defaultPerms = orig.defaultPerms.clone();
            defaultPerms.who = entity;
            defaultPerms.claim = this;

            defaultEntityPerms = orig.defaultEntityPerms.clone();
            defaultEntityPerms.who = entity;
            defaultEntityPerms.claim = this;
        }

        public EntityClaim(EntityClaim orig, UUID entity, int id) {
            nextId = Math.max(nextId, id + 1);
            this.id = id;
            this.owner = orig.owner;
            this.entity = entity;

            config.canAggro = orig.config.canAggro;
            config.testMode = orig.config.testMode;

            orig.perms.forEach((uuid, perm) -> {
                var clone = perm.clone();
                clone.who = entity;
                clone.claim = this;
                perms.put(uuid, clone);
            });

            defaultPerms = orig.defaultPerms.clone();
            defaultPerms.who = entity;
            defaultPerms.claim = this;

            defaultEntityPerms = orig.defaultEntityPerms.clone();
            defaultEntityPerms.who = entity;
            defaultEntityPerms.claim = this;
        }

        public boolean hasPermission(Object who, EntityPermission perm) {
            // no player
            if (who == null) return defaultPerms.getBoolPermission(perm) == OptBool.True;

            UUID uuid;
            if (who instanceof OfflinePlayer plr) uuid = plr.getUniqueId();
            else if (who instanceof Entity e) uuid = e.getUniqueId();
            else {
                return defaultPerms.getBoolPermission(perm) == OptBool.True;
            }

            // owner
            if (owner != null && owner.getUniqueId().equals(uuid)) {
                if (config.testMode) Claims.get().messageTest((Entity) who);
                else return true;
            }

            // explicit perms
            if (perms.containsKey(uuid)) {
                var info = perms.get(uuid);
                var has = info.getBoolPermission(perm);
                if (has != OptBool.Default) {
                    return has == OptBool.True;
                }
            }

            // default entity permissions
            if (!(who instanceof OfflinePlayer)) {
                var has = defaultEntityPerms.getBoolPermission(perm);
                if (has != OptBool.Default) {
                    return has == OptBool.True;
                }
            }

            // default permissions
            return defaultPerms.getBoolPermission(perm) == OptBool.True;
        }

        public OptBool hasPermission(Object who, AdvancedEntityPermission perm) {
            // no player
            if (who == null) return defaultPerms.getBoolPermission(perm);

            UUID uuid;
            if (who instanceof OfflinePlayer plr) uuid = plr.getUniqueId();
            else if (who instanceof Entity e) uuid = e.getUniqueId();
            else {
                return defaultPerms.getBoolPermission(perm);
            }

            // owner
            if (owner != null && owner.getUniqueId().equals(uuid)) {
                if (config.testMode) Claims.get().messageTest((Entity) who);
                else return OptBool.Default;
            }

            // explicit perms
            if (perms.containsKey(uuid)) {
                var info = perms.get(uuid);
                var has = info.getBoolPermission(perm);
                if (has != OptBool.Default) {
                    return has;
                }
            }

            // default entity permissions
            if (!(who instanceof OfflinePlayer)) {
                var has = defaultEntityPerms.getBoolPermission(perm);
                if (has != OptBool.Default) {
                    return has;
                }
            }

            // default permissions
            return defaultPerms.getBoolPermission(perm);
        }

        public Claims.ManagementType getManagementType(Player who) {
            if (owner != null && who.getUniqueId().equals(owner.getUniqueId())) return Claims.ManagementType.Owner;
            if (who.hasPermission("kamstweaks.claims.manage")) return Claims.ManagementType.Op;
            return Claims.ManagementType.None;
        }

        public record MPRes(boolean result, Component message) {}
        @SuppressWarnings("BooleanMethodIsAlwaysInverted") // what is it talking about
        public MPRes hasPermissions(Object who, EntityPermission gen, AdvancedEntityPermission... perms) {
            for (var perm : perms) {
                var has = hasPermission(who, perm);
                if (has != OptBool.Default) return new MPRes(has == OptBool.True, perm.label);
            }
            return new MPRes(hasPermission(who, gen), gen.label);
        }

        public Component getOwnerName() {
            if (owner == null) return KTStrings.getFor(KTStrings.THE_SERVER).color(NamedTextColor.GOLD);
            return Names.instance.getRenderedName(owner);
        }

        public String getOwnerUsername() {
            if (owner == null) return "the server";
            return owner.getName();
        }
    }

    NamespacedKey unclaimable = new NamespacedKey("kamstweaks", "unclaimable");

    public boolean isClaimable(Entity e) {
        if (e instanceof Boat) return true;
        if (!(e instanceof Mob)) return false;
        if (e.getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER) return false;
        if (e.getPersistentDataContainer().has(unclaimable)) return false;
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
