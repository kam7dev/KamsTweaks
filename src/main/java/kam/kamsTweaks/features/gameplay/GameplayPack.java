package kam.kamsTweaks.features.gameplay;

import io.papermc.paper.datacomponent.DataComponentTypes;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.features.fun.nicknames.Names;
import kam.kamsTweaks.managers.KTItems;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.managers.KTStrings;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class GameplayPack extends Feature {
    boolean canFreezeflame;
    boolean canWax;

    @Override
    public void setup() {
        if (Config.getBool("gameplay-pack.stone_brickstep.obtainable", true)) {
            Bukkit.addRecipe(new SmithingTransformRecipe(
                    new NamespacedKey("kamstweaks", "stone_brickstep"),
                    KTItems.createItem(KTItems.ItemType.STONE_BRICKSTEP),
                    RecipeChoice.empty(), // template
                    new RecipeChoice.ExactChoice(ItemStack.of(Material.MUSIC_DISC_PIGSTEP)), // base
                    new RecipeChoice.ExactChoice(ItemStack.of(Material.STONE_BRICK_WALL)))); // addition
        }
        canFreezeflame = Config.getBool("gameplay-pack.freezeflame.obtainable", true);
        canWax = Config.getBool("gameplay-pack.freezeflame.waxable", true);
        if (canWax) {
            {
                var item = KTItems.createItem(KTItems.ItemType.LAVA_PATH);
                var meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(KTItems.ItemTag.WAXED.key, PersistentDataType.BOOLEAN, true);
                meta.lore(List.of(KTStrings.getFor(KTStrings.ITEM_TAG_WAXED).decoration(TextDecoration.ITALIC, false)));
                item.setItemMeta(meta);

                var wax = new ShapelessRecipe(new NamespacedKey("kamstweaks", "wax_lava_path"), item);
                wax.addIngredient(KTItems.createItem(KTItems.ItemType.LAVA_PATH));
                wax.addIngredient(Material.HONEYCOMB);
                Bukkit.addRecipe(wax);

                var axe = new ShapelessRecipe(new NamespacedKey("kamstweaks", "axe_lava_path"), KTItems.createItem(KTItems.ItemType.LAVA_PATH));
                axe.addIngredient(item);
                axe.addIngredient(new RecipeChoice.MaterialChoice(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
                Bukkit.addRecipe(axe);
            }
            {
                var item = KTItems.createItem(KTItems.ItemType.ICE_MOUNTAIN);
                var meta = item.getItemMeta();
                meta.getPersistentDataContainer().set(KTItems.ItemTag.WAXED.key, PersistentDataType.BOOLEAN, true);
                meta.lore(List.of(KTStrings.getFor(KTStrings.ITEM_TAG_WAXED).decoration(TextDecoration.ITALIC, false)));
                item.setItemMeta(meta);

                var wax = new ShapelessRecipe(new NamespacedKey("kamstweaks", "wax_ice_mountain"), item);
                wax.addIngredient(KTItems.createItem(KTItems.ItemType.ICE_MOUNTAIN));
                wax.addIngredient(Material.HONEYCOMB);
                Bukkit.addRecipe(wax);

                var axe = new ShapelessRecipe(new NamespacedKey("kamstweaks", "axe_ice_mountain"), KTItems.createItem(KTItems.ItemType.ICE_MOUNTAIN));
                axe.addIngredient(item);
                axe.addIngredient(new RecipeChoice.MaterialChoice(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE));
                Bukkit.addRecipe(axe);
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!canWax) return;
        if (event.getRecipe() instanceof ShapelessRecipe recipe) {
            if (recipe.key().asString().equals("kamstweaks:axe_lava_path") || recipe.key().asString().equals("kamstweaks:axe_ice_mountain")) {
                int where = 1;
                for (var slot : event.getInventory().getMatrix()) {
                    if (slot != null && slot.getType().name().toLowerCase().contains("axe")) {
                        slot.setData(DataComponentTypes.DAMAGE, Objects.requireNonNullElse(slot.getData(DataComponentTypes.DAMAGE), 0) + 1);
                        var item = slot.clone();
                        var view = event.getView();
                        var inv = event.getInventory();
                        var plr = event.getWhoClicked();
                        int loc = where;
                        Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
                            if (plr.getOpenInventory() == view) {
                                inv.setItem(loc, item);
                            } else {
                                plr.getInventory().addItem(item).forEach((i, s) -> plr.getWorld().dropItemNaturally(plr.getLocation(), s));
                            }
                        });
                        break;
                    }
                    where++;
                }
            }
        }
    }

    @EventHandler
    public void onItemRename(PrepareAnvilEvent event) {
        if (event.getInventory().getFirstItem() == null || event.getView().getRenameText() == null) return;
        switch (KTItems.getType(event.getInventory().getFirstItem())) {
            case LAVA_PATH, ICE_MOUNTAIN: break;
            case null, default: return;
        }
        if (!event.getView().getRenameText().equals(Names.instance.pt.serialize(event.getInventory().getFirstItem().displayName()))) {
            event.setResult(null);
        }
    }

    @EventHandler
    public void onJukebox(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getPlayer().isSneaking()) return;
        var block = event.getClickedBlock();
        assert(block != null);
        if (!(block.getState() instanceof Jukebox)) return;
        var item = event.getItem();
        if (block.getLocation().getWorld().getEnvironment() == World.Environment.NETHER) {
            if (event.getHand() == null) return;
            if (KTItems.getType(item) == KTItems.ItemType.ICE_MOUNTAIN && !item.getPersistentDataContainer().has(KTItems.ItemTag.WAXED.key)) {
                KTItems.convertTo(item, KTItems.ItemType.LAVA_PATH, KTItems.ItemType.LAVA_PATH.key);
                event.getPlayer().getInventory().setItem(event.getHand(), item);
            }
        } else if (block.getLocation().getWorld().getEnvironment() == World.Environment.THE_END) {
            if (event.getHand() == null) return;
            if (KTItems.getType(item) == KTItems.ItemType.LAVA_PATH && !item.getPersistentDataContainer().has(KTItems.ItemTag.WAXED.key)) {
                KTItems.convertTo(item, KTItems.ItemType.ICE_MOUNTAIN, KTItems.ItemType.ICE_MOUNTAIN.key);
                event.getPlayer().getInventory().setItem(event.getHand(), item);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!canFreezeflame) return;
        if (event.getEntity().getType() == EntityType.STRAY) {
            if (event.getDamageSource().getCausingEntity() == null) return;
            var hitter = event.getDamageSource().getCausingEntity();
            Player causingPlayer = null;
            switch (hitter.getType()) {
                case BLAZE, GHAST:
                    if (((Mob) hitter).getTarget() instanceof Player player) {
                        causingPlayer = player;
                    }
                    break;
                case PLAYER:
                    if (event.getDamageSource().getDirectEntity() instanceof Fireball) {
                        causingPlayer = (Player) hitter;
                        break;
                    }
                    return;
                default: return;
            }

            event.getDrops().add(KTItems.createItem(KTItems.ItemType.ICE_MOUNTAIN));
            if (causingPlayer == null) return;
            var adv = Bukkit.getAdvancement(new NamespacedKey("kamstweaks", "gameplay/freezeflame"));
            if (adv == null) return;
            AdvancementProgress progress = causingPlayer.getAdvancementProgress(adv);
            for(String criteria : progress.getRemainingCriteria())
                progress.awardCriteria(criteria);
        }
    }
}
