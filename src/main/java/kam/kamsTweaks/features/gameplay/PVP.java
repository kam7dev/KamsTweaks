package kam.kamsTweaks.features.gameplay;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import kam.kamsTweaks.*;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.features.claims.Claims;
import kam.kamsTweaks.features.fun.nicknames.Names;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
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

import java.util.*;

import static kam.kamsTweaks.features.claims.EntityProtections.getTrueHitter;
import static kam.kamsTweaks.features.claims.LandClaims.nonNull;

public class PVP extends Feature {
    public static PVP instance;

    public static final Integer COMBAT_TIMER = 15;
    public Map<UUID, CombatHelper> inCombat = new HashMap<>();

    public class CombatHelper implements Listener {
        public int timerId;
        public Mannequin fake;
        public Inventory copy;
        public UUID uuid;
        public int level;
        public int selectedSlot;
        public int timeLeft = COMBAT_TIMER;

        void cancel() {
            Bukkit.getScheduler().cancelTask(timerId);
            HandlerList.unregisterAll(this);
            inCombat.remove(uuid);
            if (fake != null) fake.remove();
            fake = null;
        }
    }

    @Override
    public void setup() {
        instance = this;
        Config.addConfig(new Config.BoolConfigOption("player-pvp-toggle.enabled", "player-pvp-toggle.enabled", false, "kamstweaks.configure"));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean canCombat(Player who) {
        if (inCombat.containsKey(who.getUniqueId())) return true;
        var claim = Claims.get().landClaims.getClaim(who.getLocation());
        if (claim == null) return true;
        return claim.config.pvp;
    }

    void putInCombat(Player who, Component otherName) {
        if (!Config.getBool("pvp.combat-log", true)) return;
        if (inCombat.containsKey(who.getUniqueId())) {
            inCombat.get(who.getUniqueId()).timeLeft = COMBAT_TIMER;
            return;
        }

        who.sendMessage(KTStrings.getFor(KTStrings.PVP_IN_COMBAT, otherName).color(NamedTextColor.YELLOW));

        var ref = new CombatHelper() {
            @EventHandler(ignoreCancelled = true)
            public void onLeave(PlayerQuitEvent event) {
                if (event.getPlayer().getUniqueId().equals(this.uuid)) {
                    this.fake = spawnFakePlayer(event.getPlayer());
                    this.copy = Inventories.copyPlayerInventory(event.getPlayer().getInventory());
                    this.selectedSlot = event.getPlayer().getInventory().getHeldItemSlot();
                    this.level = event.getPlayer().getLevel();
                }
            }
            @EventHandler(ignoreCancelled = true)
            public void onJoin(PlayerJoinEvent event) {
                if (event.getPlayer().getUniqueId().equals(this.uuid)) {
                    if (this.fake != null) {
                        event.getPlayer().setHealth(this.fake.getHealth());
                        event.getPlayer().getInventory().setHelmet(this.fake.getEquipment().getHelmet());
                        event.getPlayer().getInventory().setChestplate(this.fake.getEquipment().getChestplate());
                        event.getPlayer().getInventory().setLeggings(this.fake.getEquipment().getLeggings());
                        event.getPlayer().getInventory().setBoots(this.fake.getEquipment().getBoots());
                        event.getPlayer().getInventory().setItemInMainHand(this.fake.getEquipment().getItemInMainHand());
                        event.getPlayer().getInventory().setItemInOffHand(this.fake.getEquipment().getItemInOffHand());
                        event.getPlayer().clearActivePotionEffects();
                        event.getPlayer().addPotionEffects(this.fake.getActivePotionEffects());
                        this.fake.remove();
                        this.fake = null;
                    }

                    Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> event.getPlayer().sendMessage(KTStrings.getFor(KTStrings.PVP_CURRENT_COMBAT).color(NamedTextColor.YELLOW)));
                }
            }

            @EventHandler(ignoreCancelled = true)
            public void onEntityDeath(EntityDeathEvent event) {
                var entity = event.getEntity();
                if (event.getEntity().getUniqueId().equals(this.uuid)) {
                    this.cancel();
                    event.getEntity().sendMessage(KTStrings.getFor(KTStrings.PVP_OUT_OF_COMBAT).color(NamedTextColor.GREEN));
                    if (event.getDamageSource().getCausingEntity() instanceof Player who) {
                        if (inCombat.containsKey(who.getUniqueId())) {
                            inCombat.get(who.getUniqueId()).cancel();
                            who.sendMessage(KTStrings.getFor(KTStrings.PVP_OUT_OF_COMBAT).color(NamedTextColor.GREEN));
                        }
                    }
                } else if (entity == this.fake) {
                    UserDataManager.put(this.uuid, "pvp.cl.dead", true);
                    this.cancel();
                    entity.getWorld().spawn(entity.getLocation(), ExperienceOrb.class).setExperience(Math.min(this.level * 7, 100));
                    if (!UserDataManager.get(this.uuid, "keepinv.enabled", true)) {
                        UserDataManager.put(this.uuid, "pvp.cl.clear", true);
                        this.copy.setItem(36, entity.getEquipment().getBoots());
                        this.copy.setItem(37, entity.getEquipment().getLeggings());
                        this.copy.setItem(38, entity.getEquipment().getChestplate());
                        this.copy.setItem(39, entity.getEquipment().getHelmet());
                        this.copy.setItem(40, entity.getEquipment().getItemInOffHand());
                        this.copy.setItem(this.selectedSlot, entity.getEquipment().getItemInMainHand());
                        for (var item : this.copy.getContents()) {
                            if (item == null) continue;
                            if (item.getItemMeta().hasEnchant(Enchantment.VANISHING_CURSE)) continue;
                            entity.getWorld().dropItemNaturally(entity.getLocation(), item);
                        }
                    } else {
                        UserDataManager.put(this.uuid, "pvp.cl.clear", false);
                        UserDataManager.putItemStack(this.uuid, "pvp.cl.mainhand", entity.getEquipment().getItemInMainHand());
                        UserDataManager.putItemStack(this.uuid, "pvp.cl.offhand", entity.getEquipment().getItemInOffHand());
                        UserDataManager.putItemStack(this.uuid, "pvp.cl.helmet", entity.getEquipment().getHelmet());
                        UserDataManager.putItemStack(this.uuid, "pvp.cl.chestplate", entity.getEquipment().getChestplate());
                        UserDataManager.putItemStack(this.uuid, "pvp.cl.leggings", entity.getEquipment().getLeggings());
                        UserDataManager.putItemStack(this.uuid, "pvp.cl.boots", entity.getEquipment().getBoots());
                    }
                }
            }

            @EventHandler(ignoreCancelled = true)
            public void onEntityDamage(EntityDamageByEntityEvent event) {
                if (event.getEntity() != this.fake) return;
                var hitter = getTrueHitter(event.getDamageSource().getCausingEntity());
                if (!(hitter instanceof Player other)) return;
                if (!canCombat(other)) {
                    event.setCancelled(true);
                    other.sendMessage(KTStrings.getFor(KTStrings.LC_PVP_CLAIM_DISABLED).color(NamedTextColor.RED));
                    return;
                }
                this.timeLeft = COMBAT_TIMER;
                putInCombat(other, this.fake.customName());
            }
        };
        ref.uuid = who.getUniqueId();
        ref.timerId = Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), () -> {
            int val = ref.timeLeft - 1;
            if (val <= 0) {
                ref.cancel();
                var current = Bukkit.getPlayer(ref.uuid);
                if (current != null) current.sendMessage(KTStrings.getFor(KTStrings.PVP_OUT_OF_COMBAT).color(NamedTextColor.GREEN));
                return;
            }
            ref.timeLeft = val;
        }, 20, 20);
        Bukkit.getPluginManager().registerEvents(ref, KamsTweaks.get());

        inCombat.put(who.getUniqueId(), ref);
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player target && e.getDamageSource().getCausingEntity() instanceof Player causer) {
            if (target == causer) return;
            if (!canCombat(target)) {
                e.setCancelled(true);
                causer.sendMessage(KTStrings.getFor(KTStrings.LC_PVP_CLAIM_DISABLED, Names.getName(target)).color(NamedTextColor.RED));
            } else if (!canCombat(causer)) {
                e.setCancelled(true);
                causer.sendMessage(KTStrings.getFor(KTStrings.LC_PVP_CLAIM_DISABLED).color(NamedTextColor.RED));
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
