package kam.kamsTweaks.features;

import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.utils.Inventories;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static kam.kamsTweaks.features.landclaims.LandClaims.deserializeLocation;
import static kam.kamsTweaks.features.landclaims.LandClaims.serializeLocation;
import static org.bukkit.Bukkit.getServer;

public class Graves implements Listener {
    List<Grave> graves = new ArrayList<>();
    @EventHandler
    public void onDie(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Grave grave = new Grave(player, player.getLocation());
        graves.add(grave);
        event.setKeepInventory(true);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[]{});
        player.getInventory().setItemInOffHand(null);
        player.setExp(0);
    }

    public static class Grave {
        OfflinePlayer owner;
        Inventory inventory;
        Location location;
        float experience;
        public Grave(OfflinePlayer owner, Inventory inventory, Location location, float experience) {
            this.owner = owner;
            this.inventory = inventory;
            this.location = location;
            this.experience = experience;
        }

        public Grave(Player owner, Location location) {
            this.owner = owner;
            this.location = location;
            PlayerInventory inv = owner.getInventory();
            this.inventory = Bukkit.createInventory(null, 36);
            for (int i = 0; i < 27; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.isEmpty()) {
                    inventory.setItem(i, item);
                    inv.setItem(i, null);
                }
            }
            inventory.setItem(27, inv.getHelmet());
            inv.setHelmet(null);
            inventory.setItem(28, inv.getChestplate());
            inv.setChestplate(null);
            inventory.setItem(29, inv.getLeggings());
            inv.setLeggings(null);
            inventory.setItem(30, inv.getBoots());
            inv.setBoots(null);
            inventory.setItem(31, inv.getItemInOffHand());
            inv.setItemInOffHand(null);
            this.experience = owner.getExp();
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
    }

    public void saveGraves() {
        int i = 0;
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
        for (Grave grave : graves) {
            Inventories.saveInventory(grave.getInventory(), config, "graves." + i);
            config.set("graves." + i + ".location", serializeLocation(grave.location));
            config.set("graves." + i + ".owner", grave.owner.getUniqueId().toString());
            config.set("graves." + i + ".xp", grave.experience);
            i++;
        }
    }

    public void loadGraves() {
        int i = 0;
        graves.clear();
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
        if (config.contains("graves")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("graves")).getKeys(false)) {
                try {
                    String ownerStr = config.getString("graves." + key + ".owner");
                    UUID owner = ownerStr == null ? null : UUID.fromString(ownerStr);
                    String locationStr = config.getString("graves." + key + ".location");
                    float xp = (float) config.get("graves." + key + ".xp", 0);
                    assert locationStr != null;
                    Location location = deserializeLocation(locationStr);
                    if (location.getWorld() == null) continue;
                    Inventory inv = Inventories.loadInventory(Component.empty(), 36, config, "graves." + key);
                    Grave grave = new Grave(owner == null ? null : getServer().getOfflinePlayer(owner), inv, location, xp);
                    graves.add(grave);
                } catch (Exception e) {
                    Logger.warn(e.getMessage());
                }
            }
        }
    }
}
