package kam.kamsTweaks.features.trolls;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import kam.kamsTweaks.ItemManager;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;

public class AshswagTrolls implements Listener {
    public void onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(this, KamsTweaks.getInstance());
    }

    @EventHandler
    public void onArmor(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        if (!hasPerms(player)) return;
        if (ItemManager.getType(event.getOldItem()) != ItemManager.ItemType.FLYING_BOOTS && ItemManager.getType(event.getNewItem()) == ItemManager.ItemType.FLYING_BOOTS) {
            player.setAllowFlight(true);
        } else if (ItemManager.getType(event.getOldItem()) == ItemManager.ItemType.FLYING_BOOTS && ItemManager.getType(event.getNewItem()) != ItemManager.ItemType.FLYING_BOOTS) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.getMessage().equalsIgnoreCase("!prankitems") && hasPerms(event.getPlayer())) {
            event.getPlayer().getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.FLYING_BOOTS));
            event.getPlayer().getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.BLINDNESS_WAND));
            event.getPlayer().getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.LEVITATION_SWORD));
            event.getPlayer().getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.KNOCKBACK_STICK));
            event.getPlayer().getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.FAKE_TNT));
            event.getPlayer().getInventory().addItem(ItemManager.createItem(ItemManager.ItemType.PORTAL_BOW));
            event.getPlayer().sendMessage(Component.text(">:)").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && event.getEntity() instanceof LivingEntity entity) {
            if (!hasPerms(player)) return;
            switch(ItemManager.getType(player.getInventory().getItemInMainHand())) {
                case ItemManager.ItemType.BLINDNESS_WAND -> {
                    event.setDamage(0);
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 600, 0, false, false, true));
                }
                case ItemManager.ItemType.LEVITATION_SWORD -> {
                    event.setDamage(0);
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 600, 0, false, false, true));
                }
                case ItemManager.ItemType.KNOCKBACK_STICK -> event.setDamage(0);
                case null, default -> {}
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    void onPlace(PlayerInteractEvent event) {
        if (!hasPerms(event.getPlayer())) return;
        if (ItemManager.getType(event.getPlayer().getInventory().getItemInMainHand()) == ItemManager.ItemType.FAKE_TNT) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            Action action = event.getAction();
            Location location;

            if (action == Action.RIGHT_CLICK_BLOCK) {
                assert event.getClickedBlock() != null;
                location = event.getClickedBlock().getLocation();
            } else if (action == Action.RIGHT_CLICK_AIR) {
                double reachDistance = 5.0;
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection().normalize();
                location = eyeLoc.add(direction.multiply(reachDistance));
            } else {
                return;
            }
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    Location loc = location.clone().add(x, 0, z);
                    TNTPrimed tnt = event.getPlayer().getWorld().spawn(loc, TNTPrimed.class);
                    tnt.setFuseTicks(40);
                    tnt.setYield(0);
                    tnt.setIsIncendiary(false);
                }
            }
        }
    }

    @EventHandler
    void onShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!hasPerms(player)) return;
            if (ItemManager.getType(event.getBow()) == ItemManager.ItemType.PORTAL_BOW) {
                event.getProjectile().getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "teleporter"), PersistentDataType.STRING, player.getUniqueId().toString());
            }
        }
    }

    @EventHandler
    void onArrowLand(ProjectileHitEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(new NamespacedKey("kamstweaks", "teleporter"), PersistentDataType.STRING)) {
            Player player = Bukkit.getPlayer(UUID.fromString(Objects.requireNonNull(event.getEntity().getPersistentDataContainer().get(new NamespacedKey("kamstweaks", "teleporter"), PersistentDataType.STRING))));
            if (player == null) return;
            event.setCancelled(true);
            Location loc = event.getEntity().getLocation();
            loc.setYaw(player.getLocation().getYaw());
            loc.setPitch(player.getLocation().getPitch());
            player.teleport(loc);
            event.getEntity().remove();
        }
    }

    boolean hasPerms(Player player) {
        return player.getName().equals("km7dev");
    }

}
