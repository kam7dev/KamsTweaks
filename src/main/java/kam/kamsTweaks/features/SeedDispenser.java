package kam.kamsTweaks.features;

import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;

import static org.bukkit.Bukkit.getServer;

public class SeedDispenser implements Listener {

    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("seed-dispenser.enabled", "seed-dispenser.enabled", true, "kamstweaks.configure"));
    }

    public Material matForSeed(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        return switch (item.getType()) {
            case WHEAT_SEEDS -> Material.WHEAT;
            case BEETROOT_SEEDS -> Material.BEETROOTS;
            case MELON_SEEDS -> Material.MELON_STEM;
            case PUMPKIN_SEEDS -> Material.PUMPKIN_STEM;
            case TORCHFLOWER_SEEDS -> Material.TORCHFLOWER_CROP;
            case CARROT -> Material.CARROTS;
            case POTATO -> Material.POTATOES;
            default -> null;
        };
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent e) {
        if (!KamsTweaks.getInstance().getConfig().getBoolean("seed-dispenser.enabled", true)) return;
        Block block = e.getBlock();
        if (block.getType() == Material.DISPENSER) {
            Directional directional = (Directional) block.getBlockData();
            Block farm = block.getRelative(directional.getFacing());
            if (farm.getType() == Material.FARMLAND) {
                Container container = (Container) block.getState();
                Material mat = matForSeed(e.getItem());
                if (mat != null) {
                    e.setCancelled(true);
                    Block toPlace = farm.getRelative(BlockFace.UP);
                    if (toPlace.getType() != Material.AIR) {
                        return;
                    }
                    ItemStack[] contents = container.getInventory().getContents();
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack slot = contents[i];
                        if (slot == null) continue;
                        if (slot.isSimilar(e.getItem())) {
                            int amount = slot.getAmount();
                            if (amount > 1) {
                                slot.setAmount(amount - 1);
                                container.getInventory().setItem(i, slot);
                            } else {
                                container.getInventory().setItem(i, null);
                            }
                            toPlace.setType(mat);
                            return;
                        }
                    }
                    getServer().getScheduler().runTask(KamsTweaks.getInstance(), () -> {
                        ItemStack[] contents2 = container.getInventory().getContents();
                        for (int i = 0; i < contents2.length; i++) {
                            ItemStack slot = contents2[i];
                            if (slot == null) continue;
                            if (slot.isSimilar(e.getItem())) {
                                int amount = slot.getAmount();
                                if (amount > 1) {
                                    slot.setAmount(amount - 1);
                                    container.getInventory().setItem(i, slot);
                                } else {
                                    container.getInventory().setItem(i, null);
                                }
                                toPlace.setType(mat);
                                return;
                            }
                        }
                    });
                }
            }
        }
    }
}
