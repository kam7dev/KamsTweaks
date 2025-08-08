package kam.kamsTweaks.features;

import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.features.landclaims.LandClaims;
import kam.kamsTweaks.utils.Inventories;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static kam.kamsTweaks.features.landclaims.LandClaims.deserializeLocation;
import static kam.kamsTweaks.features.landclaims.LandClaims.serializeLocation;
import static org.bukkit.Bukkit.getServer;

public class Graves implements Listener {
    Map<Integer, Grave> graves = new HashMap<>();
    static int highest = 0;
    @EventHandler
    public void onDie(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().isEmpty() && player.getTotalExperience() == 0) return;
        Grave grave = new Grave(player, player.getLocation());
        graves.put(grave.id, grave);
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (entity instanceof ArmorStand stand) {
            Player player = e.getPlayer();
            NamespacedKey key = new NamespacedKey("kamstweaks", "grave");
            if (!stand.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) return;
            e.setCancelled(true);
            if (!KamsTweaks.getInstance().getConfig().getBoolean("graves.enabled", true))
                return;
            @SuppressWarnings("DataFlowIssue") int id = stand.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
            if (!graves.containsKey(id)) return;
            var grave = graves.get(id);
            if (grave.getOwner().getUniqueId().equals(player.getUniqueId())) {
                player.openInventory(grave.getInventory());
                if (grave.experience != 0) {
                    changePlayerExp(player, grave.experience);
                    grave.experience = 0;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteractAt(PlayerInteractAtEntityEvent e) {
        onEntityInteract(e);
    }

    public static class Grave {
        OfflinePlayer owner;
        Inventory inventory;
        Location location;
        int experience;
        int id;
        public Grave(OfflinePlayer owner, Inventory inventory, Location location, int experience) {
            this.owner = owner;
            this.inventory = inventory;
            this.location = location;
            this.experience = experience;
        }

        public Grave(Player owner, Location location) {
            this.owner = owner;
            this.location = location;
            PlayerInventory inv = owner.getInventory();
            this.inventory = Bukkit.createInventory(null, 45);
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.isEmpty()) {
                    inventory.setItem(i, item);
                    inv.setItem(i, null);
                }
            }
            inventory.setItem(36, inv.getHelmet());
            inv.setHelmet(null);
            inventory.setItem(37, inv.getChestplate());
            inv.setChestplate(null);
            inventory.setItem(38, inv.getLeggings());
            inv.setLeggings(null);
            inventory.setItem(39, inv.getBoots());
            inv.setBoots(null);
            inventory.setItem(40, inv.getItemInOffHand());
            inv.setItemInOffHand(null);
            this.experience = getPlayerExp(owner);
            this.id = highest;
            highest++;
            createStand();
        }

        public OfflinePlayer getOwner() {
            return owner;
        }

        public Inventory getInventory() {
            return inventory;
        }

        public Location getLocation() {
            return location;
        }

        public void createStand() {
            ArmorStand stand = location.getWorld().spawn(location.addRotation(90, 0).subtract(0, 1.4375, 0), ArmorStand.class);
            stand.setGravity(false);
            stand.setItem(EquipmentSlot.HEAD, new ItemStack(Material.STONE_BRICK_WALL));
            stand.setCustomNameVisible(true);
            stand.setBasePlate(false);
            stand.setInvisible(true);
            stand.setInvulnerable(true);
            stand.customName(Component.text(owner.getName() == null ? "Unknown" : owner.getName()).color(NamedTextColor.GOLD).append(Component.text("'s Grave").color(NamedTextColor.WHITE)));
            stand.getPersistentDataContainer().set(new NamespacedKey("kamstweaks", "grave"), PersistentDataType.INTEGER, id);
        }
    }

    public void saveGraves() {
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
        graves.forEach((id, grave) -> {
            Inventories.saveInventory(grave.getInventory(), config, "graves." + id);
            config.set("graves." + id + ".location", serializeLocation(grave.location));
            config.set("graves." + id + ".owner", grave.owner.getUniqueId().toString());
            config.set("graves." + id + ".xp", grave.experience);
        });
    }

    public void loadGraves() {
        graves.clear();
        highest = 0;
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
        if (config.contains("graves")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("graves")).getKeys(false)) {
                try {
                    String ownerStr = config.getString("graves." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    String locationStr = config.getString("graves." + key + ".location");
                    int xp = (int) config.get("graves." + key + ".xp", 0);
                    assert locationStr != null;
                    Location location = deserializeLocation(locationStr);
                    if (location.getWorld() == null) continue;
                    Inventory inv = Inventories.loadInventory(Component.empty(), 36, config, "graves." + key);
                    Grave grave = new Grave(owner == null ? null : getServer().getOfflinePlayer(owner), inv, location, xp);
                    int id = Integer.parseInt(key);
                    if (highest < id) highest = id;
                    grave.id = id;
                    graves.put(id, grave);
                } catch (Exception e) {
                    Logger.warn(e.getMessage());
                }
            }
        }
    }

    // From essentials

    // Calculate amount of EXP needed to level up
    public static int getExpToLevelUp(int level){
        if(level <= 15){
            return 2*level+7;
        } else if(level <= 30){
            return 5*level-38;
        } else {
            return 9*level-158;
        }
    }

    // Calculate total experience up to a level
    public static int getExpAtLevel(int level){
        if(level <= 16){
            return (int) (Math.pow(level,2) + 6*level);
        } else if(level <= 31){
            return (int) (2.5*Math.pow(level,2) - 40.5*level + 360.0);
        } else {
            return (int) (4.5*Math.pow(level,2) - 162.5*level + 2220.0);
        }
    }

    // Calculate player's current EXP amount
    public static int getPlayerExp(Player player){
        int exp = 0;
        int level = player.getLevel();

        // Get the amount of XP in past levels
        exp += getExpAtLevel(level);

        // Get amount of XP towards next level
        exp += Math.round(getExpToLevelUp(level) * player.getExp());

        return exp;
    }

    // Give or take EXP
    public static int changePlayerExp(Player player, int exp){
        // Get player's current exp
        int currentExp = getPlayerExp(player);

        // Reset player's current exp to 0
        player.setExp(0);
        player.setLevel(0);

        // Give the player their exp back, with the difference
        int newExp = currentExp + exp;
        player.giveExp(newExp);

        // Return the player's new exp amount
        return newExp;
    }

}
