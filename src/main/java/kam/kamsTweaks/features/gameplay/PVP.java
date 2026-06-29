package kam.kamsTweaks.features.gameplay;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.features.fun.Names;
import kam.kamsTweaks.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static kam.kamsTweaks.features.claims.EntityProtections.getTrueHitter;
import static kam.kamsTweaks.features.claims.LandClaims.nonNull;

public class PVP extends Feature {
    private final Map<UUID, Boolean> pvp = new HashMap<>();
    public Map<Player, Integer> onCooldown = new HashMap<>();

    public static PVP instance;

    public static final Integer COMBAT_TIMER = 15;
    public Map<UUID, Integer> inCombat = new HashMap<>();

    @Override
    public void setup() {
        instance = this;
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("player-pvp-toggle.enabled", "player-pvp-toggle.enabled", false, "kamstweaks.configure"));
    }

    void cooldown(Player player) {
        onCooldown.put(player, 15);
        var r = new Runnable() {
            int id = 0;

            @Override
            public void run() {
                int val = onCooldown.get(player) - 1;
                if (val <= 0) {
                    Bukkit.getScheduler().cancelTask(id);
                    onCooldown.remove(player);
                    return;
                }
                onCooldown.put(player, val);
            }
        };

        r.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), r, 20, 20);
    }

    void putInCombat(Player who, Component otherName) {
        if (inCombat.containsKey(who.getUniqueId())) {
            inCombat.put(who.getUniqueId(), COMBAT_TIMER);
            return;
        }

        who.sendMessage(KTStrings.getFor(KTStrings.PVP_IN_COMBAT, otherName).color(NamedTextColor.YELLOW));

        inCombat.put(who.getUniqueId(), COMBAT_TIMER);
        var ref = new Object() {
            Listener listener;
            int timer;
            Mannequin fake;
            Inventory copy;
            final UUID uuid = who.getUniqueId();
            int level;
            int selectedSlot;

            void cancel() {
                Bukkit.getScheduler().cancelTask(timer);
                HandlerList.unregisterAll(listener);
                inCombat.remove(uuid);
                if (fake != null) fake.remove();
                fake = null;
            }
        };

        ref.timer = Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), () -> {
            int val = inCombat.get(who.getUniqueId()) - 1;
            if (val <= 0) {
                ref.cancel();
                var current = Bukkit.getPlayer(ref.uuid);
                if (current != null) current.sendMessage(KTStrings.getFor(KTStrings.PVP_OUT_OF_COMBAT).color(NamedTextColor.GREEN));
                return;
            }
            inCombat.put(who.getUniqueId(), val);
        }, 20, 20);
        ref.listener = new Listener() {
            @EventHandler(ignoreCancelled = true)
            public void onLeave(PlayerQuitEvent event) {
                if (event.getPlayer().getUniqueId().equals(ref.uuid)) {
                    ref.fake = spawnFakePlayer(event.getPlayer());
                    ref.copy = Inventories.copyPlayerInventory(event.getPlayer().getInventory());
                    ref.selectedSlot = event.getPlayer().getInventory().getHeldItemSlot();
                    ref.level = event.getPlayer().getLevel();
                }
            }
            @EventHandler(ignoreCancelled = true)
            public void onJoin(PlayerJoinEvent event) {
                if (event.getPlayer().getUniqueId().equals(ref.uuid)) {
                    if (ref.fake != null) {
                        event.getPlayer().setHealth(ref.fake.getHealth());
                        event.getPlayer().getInventory().setHelmet(ref.fake.getEquipment().getHelmet());
                        event.getPlayer().getInventory().setChestplate(ref.fake.getEquipment().getChestplate());
                        event.getPlayer().getInventory().setLeggings(ref.fake.getEquipment().getLeggings());
                        event.getPlayer().getInventory().setBoots(ref.fake.getEquipment().getBoots());
                        event.getPlayer().getInventory().setItemInMainHand(ref.fake.getEquipment().getItemInMainHand());
                        event.getPlayer().getInventory().setItemInOffHand(ref.fake.getEquipment().getItemInOffHand());
                        event.getPlayer().clearActivePotionEffects();
                        event.getPlayer().addPotionEffects(ref.fake.getActivePotionEffects());
                        ref.fake.remove();
                        ref.fake = null;
                    }

                    Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> event.getPlayer().sendMessage(KTStrings.getFor(KTStrings.PVP_CURRENT_COMBAT).color(NamedTextColor.YELLOW)));
                }
            }

            @EventHandler(ignoreCancelled = true)
            public void onEntityDeath(EntityDeathEvent event) {
                var entity = event.getEntity();
                if (event.getEntity().getUniqueId().equals(ref.uuid)) {
                    ref.cancel();
                    event.getEntity().sendMessage(KTStrings.getFor(KTStrings.PVP_OUT_OF_COMBAT).color(NamedTextColor.GREEN));
                } else if (entity == ref.fake) {
                    UserDataManager.put(ref.uuid, "pvp.cl.dead", true);
                    ref.cancel();
                    entity.getWorld().spawn(entity.getLocation(), ExperienceOrb.class).setExperience(Math.min(ref.level * 7, 100));
                    if (!UserDataManager.get(ref.uuid, "keepinv.enabled", true)) {
                        UserDataManager.put(ref.uuid, "pvp.cl.clear", true);
                        ref.copy.setItem(36, entity.getEquipment().getBoots());
                        ref.copy.setItem(37, entity.getEquipment().getLeggings());
                        ref.copy.setItem(38, entity.getEquipment().getChestplate());
                        ref.copy.setItem(39, entity.getEquipment().getHelmet());
                        ref.copy.setItem(40, entity.getEquipment().getItemInOffHand());
                        ref.copy.setItem(ref.selectedSlot, entity.getEquipment().getItemInMainHand());
                        for (var item : ref.copy.getContents()) {
                            if (item == null) continue;
                            if (item.getItemMeta().hasEnchant(Enchantment.VANISHING_CURSE)) continue;
                            entity.getWorld().dropItemNaturally(entity.getLocation(), item);
                        }
                        Logger.info("Dropping");
                    } else {
                        UserDataManager.put(ref.uuid, "pvp.cl.clear", false);
                        UserDataManager.putItemStack(ref.uuid, "pvp.cl.mainhand", entity.getEquipment().getItemInMainHand());
                        UserDataManager.putItemStack(ref.uuid, "pvp.cl.offhand", entity.getEquipment().getItemInOffHand());
                        UserDataManager.putItemStack(ref.uuid, "pvp.cl.helmet", entity.getEquipment().getHelmet());
                        UserDataManager.putItemStack(ref.uuid, "pvp.cl.chestplate", entity.getEquipment().getChestplate());
                        UserDataManager.putItemStack(ref.uuid, "pvp.cl.leggings", entity.getEquipment().getLeggings());
                        UserDataManager.putItemStack(ref.uuid, "pvp.cl.boots", entity.getEquipment().getBoots());
                    }
                }
            }

            @EventHandler(ignoreCancelled = true)
            public void onEntityDamage(EntityDamageByEntityEvent event) {
                if (event.getEntity() != ref.fake) return;
                var hitter = getTrueHitter(event.getDamageSource().getCausingEntity());
                if (!(hitter instanceof Player other)) return;
                if (!pvp.getOrDefault(other.getUniqueId(), true)) {
                    event.setCancelled(true);
                    other.sendMessage(KTStrings.getFor(KTStrings.PVP_YOU_DISABLED).color(NamedTextColor.RED));
                    return;
                }
                inCombat.put(ref.uuid, COMBAT_TIMER);
                putInCombat(other, ref.fake.customName());
            }
        };

        Bukkit.getPluginManager().registerEvents(ref.listener, KamsTweaks.get());
    }

    @SuppressWarnings("UnstableApiUsage")
    Mannequin spawnFakePlayer(Player who) {
        var dummy = who.getWorld().spawn(who.getLocation(), Mannequin.class);
        dummy.setRotation(who.getYaw(), who.getPitch());
        dummy.setPersistent(false);
        dummy.setRemoveWhenFarAway(false);
        dummy.customName(who.displayName());
        dummy.setDescription(KTStrings.getFor(KTStrings.PVP_COMBAT_LOGGED).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC));
        dummy.setProfile(ResolvableProfile.resolvableProfile().uuid(who.getUniqueId()).build());

        dummy.getEquipment().setHelmet(who.getInventory().getHelmet());
        dummy.getEquipment().setChestplate(who.getInventory().getChestplate());
        dummy.getEquipment().setLeggings(who.getInventory().getLeggings());
        dummy.getEquipment().setBoots(who.getInventory().getBoots());
        dummy.getEquipment().setItemInMainHand(who.getInventory().getItemInMainHand());
        dummy.getEquipment().setItemInOffHand(who.getInventory().getItemInOffHand());

        nonNull(dummy.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(nonNull(who.getAttribute(Attribute.MAX_HEALTH)).getValue());
        dummy.addPotionEffects(who.getActivePotionEffects());
        dummy.setHealth(who.getHealth());
        dummy.setMainHand(who.getMainHand());
        dummy.setImmovable(true);
        return dummy;
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("pvp")
                .then(Commands.literal("on")
                        .executes(ctx -> {
                            if (!KamsTweaks.get().getConfig().getBoolean("player-pvp-toggle.enabled", true)) {
                                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/pvp")));
                                return Command.SINGLE_SUCCESS;
                            }
                            Entity exec = ctx.getSource().getExecutor();
                            if (exec instanceof Player p) {
                                if (onCooldown.containsKey(p)) {
                                    p.sendMessage(KTStrings.getFor(KTStrings.COOLDOWN, Component.text("/pvp"), Component.text(onCooldown.get(p))).color(NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                if (inCombat.containsKey(p.getUniqueId())) {
                                    p.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                UUID playerUUID = p.getUniqueId();
                                pvp.put(playerUUID, true);
                                p.sendMessage(KTStrings.getFor(KTStrings.PVP_ENABLE).color(NamedTextColor.GREEN));
                                cooldown(p);
                                return Command.SINGLE_SUCCESS;
                            }
                            ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("off")
                        .executes(ctx -> {
                            if (!KamsTweaks.get().getConfig().getBoolean("player-pvp-toggle.enabled", true)) {
                                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/pvp")));
                                return Command.SINGLE_SUCCESS;
                            }
                            Entity exec = ctx.getSource().getExecutor();
                            if (exec instanceof Player p) {
                                if (onCooldown.containsKey(p)) {
                                    p.sendMessage(KTStrings.getFor(KTStrings.COOLDOWN, Component.text("/pvp"), Component.text(onCooldown.get(p))).color(NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                if (inCombat.containsKey(p.getUniqueId())) {
                                    p.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                UUID playerUUID = p.getUniqueId();
                                pvp.put(playerUUID, false);
                                p.sendMessage(KTStrings.getFor(KTStrings.PVP_DISABLE).color(NamedTextColor.RED));
                                cooldown(p);
                                return Command.SINGLE_SUCCESS;
                            }
                            ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                            return Command.SINGLE_SUCCESS;
                        }))
                .executes(ctx -> {
                    if (!KamsTweaks.get().getConfig().getBoolean("player-pvp-toggle.enabled", true)) {
                        ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/pvp")));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        UUID playerUUID = p.getUniqueId();
                        if (pvp.getOrDefault(playerUUID, true)) {
                            p.sendMessage(KTStrings.getFor(KTStrings.PVP_STATUS_ENABLED).color(NamedTextColor.GOLD));
                        } else {
                            p.sendMessage(KTStrings.getFor(KTStrings.PVP_STATUS_DISABLED).color(NamedTextColor.GOLD));
                        }
                        return Command.SINGLE_SUCCESS;
                    }
                    ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                    return Command.SINGLE_SUCCESS;
                }).build());
    }

    @Override
    public void loadData() {
        pvp.clear();
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        if (config.contains("player-pvp.enabled")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("player-pvp.enabled")).getKeys(false)) {
                try {
                    pvp.put(UUID.fromString(key), config.getBoolean("player-pvp.enabled." + key));
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
    }

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        config.set("player-pvp", null);
        pvp.forEach((uuid, en) -> {
            if (en != null) config.set("player-pvp.enabled." + uuid, en);
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("player-pvp-toggle.enabled", true))
            return;
        if (e.getEntity() instanceof Player target && getTrueHitter(e.getDamageSource().getCausingEntity()) instanceof Player causer) {
            if (!pvp.getOrDefault(target.getUniqueId(), true)) {
                e.setCancelled(true);
                causer.sendMessage(KTStrings.getFor(KTStrings.PVP_TARGET_DISABLED, Names.instance.getRenderedName(target)).color(NamedTextColor.RED));
            } else if (!pvp.getOrDefault(causer.getUniqueId(), true)) {
                e.setCancelled(true);
                causer.sendMessage(KTStrings.getFor(KTStrings.PVP_YOU_DISABLED).color(NamedTextColor.RED));
            } else {
                putInCombat(target, causer.displayName());
                putInCombat(causer, target.displayName());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        var plr = event.getPlayer();
        if (UserDataManager.get(plr.getUniqueId(), "pvp.cl.dead", false)) {
            UserDataManager.erase(plr.getUniqueId(), "pvp.cl.dead");
            plr.teleport(Objects.requireNonNullElse(plr.getRespawnLocation(), Bukkit.getWorlds().getFirst().getSpawnLocation()));
            plr.setHealth(nonNull(plr.getAttribute(Attribute.MAX_HEALTH)).getValue());
            plr.setFoodLevel(20);
            plr.setSaturation(5);
            plr.setDeathScreenScore(0);
            plr.setExp(0);
            plr.setLevel(0);

            if (UserDataManager.get(plr.getUniqueId(), "pvp.cl.clear", false)) {
                plr.getInventory().clear();
            } else {
                plr.getInventory().setItemInMainHand(UserDataManager.getItemStack(plr.getUniqueId(), "pvp.cl.mainhand", plr.getInventory().getItemInMainHand()));
                plr.getInventory().setItemInOffHand(UserDataManager.getItemStack(plr.getUniqueId(), "pvp.cl.offhand", plr.getInventory().getItemInOffHand()));
                plr.getInventory().setHelmet(UserDataManager.getItemStack(plr.getUniqueId(), "pvp.cl.helmet", plr.getInventory().getHelmet()));
                plr.getInventory().setChestplate(UserDataManager.getItemStack(plr.getUniqueId(), "pvp.cl.chestplate", plr.getInventory().getChestplate()));
                plr.getInventory().setLeggings(UserDataManager.getItemStack(plr.getUniqueId(), "pvp.cl.leggings", plr.getInventory().getLeggings()));
                plr.getInventory().setBoots(UserDataManager.getItemStack(plr.getUniqueId(), "pvp.cl.boots", plr.getInventory().getBoots()));
                for (int i = 0; i <= 40; i++) {
                    var item = plr.getInventory().getItem(i);
                    if (item != null && !item.isEmpty() && item.getItemMeta().hasEnchant(Enchantment.VANISHING_CURSE)) plr.getInventory().setItem(i, ItemStack.empty());
                }
            }
            UserDataManager.erase(plr.getUniqueId(), "pvp.cl.clear");
        }
    }
}
