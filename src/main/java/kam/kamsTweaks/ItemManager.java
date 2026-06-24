package kam.kamsTweaks;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.JukeboxPlayable;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class ItemManager implements Listener {
    public enum ItemType {
        CLAIM_TOOL("claimer", KTStrings.ITEM_CLAIM_TOOL),
        GRAVE_HEAD("grave_head", KTStrings.GRAVE_TITLE),
        STONE_BRICKSTEP("stone_brickstep", KTStrings.ITEM_STONE_BRICKSTEP),

        ;

        public final NamespacedKey key;
        public final Component name;
        ItemType(NamespacedKey key, Component name) {
            this.key = key;
            this.name = name;
        }
        ItemType(String namespace, String key, Component name) {
            this(new NamespacedKey(namespace, key), name);
        }
        ItemType(String key, Component name) {
            this(new NamespacedKey("kamstweaks", key), name);
        }
        ItemType(NamespacedKey key, KTStrings name) {
            this(key, KTStrings.getFor(name));
        }
        ItemType(String namespace, String key, KTStrings name) {
            this(new NamespacedKey(namespace, key), KTStrings.getFor(name));
        }
        ItemType(String key, KTStrings name) {
            this(new NamespacedKey("kamstweaks", key), KTStrings.getFor(name));
        }
    }
    public enum ItemTag {
        ;

        public final NamespacedKey key;
        ItemTag(NamespacedKey key) {
            this.key = key;
        }
        ItemTag(String namespace, String key) {
            this.key = new NamespacedKey(namespace, key);
        }
        ItemTag(String key) {
            this.key = new NamespacedKey("kamstweaks", key);
        }
    }

    private static Map<ItemType, ItemStack> items;
    private static final NamespacedKey key = new NamespacedKey("kamstweaks", "item");

    private static ItemStack makeBaseItem(ItemType type) {
        ItemStack item = new ItemStack(Material.MUSIC_DISC_PIGSTEP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(type.name.decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.key.getKey());
            item.setItemMeta(meta);
        }
        item.unsetData(DataComponentTypes.JUKEBOX_PLAYABLE);
        return item;
    }

    public static void init() {
        items = new HashMap<>();
        {
            var item = makeBaseItem(ItemType.CLAIM_TOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.lore(List.of(Component.translatable("enchantment.minecraft.protection").append(Component.space(), Component.translatable("enchantment.level.5")).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
                meta.setEnchantmentGlintOverride(true);
                meta.setMaxStackSize(64);
            }
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "structure_void"));
        }
        {
            var item = makeBaseItem(ItemType.GRAVE_HEAD);
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "stone_brick_wall"));
            item.setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(EquipmentSlot.HEAD));
            items.put(ItemType.GRAVE_HEAD, item);
        }
        {
            var item = makeBaseItem(ItemType.STONE_BRICKSTEP);
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "stone_brick_wall"));
            item.setData(DataComponentTypes.JUKEBOX_PLAYABLE, JukeboxPlayable.jukeboxPlayable(JukeboxSong.PIGSTEP));
            items.put(ItemType.STONE_BRICKSTEP, item);
        }
        Bukkit.addRecipe(new SmithingTransformRecipe(new NamespacedKey("kamstweaks", "stone_brickstep"), createItem(ItemType.STONE_BRICKSTEP), RecipeChoice.empty(), new RecipeChoice.ExactChoice(ItemStack.of(Material.MUSIC_DISC_PIGSTEP)), new RecipeChoice.ExactChoice(ItemStack.of(Material.STONE_BRICK_WALL))));
    }

    public static ItemStack createItem(ItemType type) {
        return items.get(type);
    }

    private static ItemType fromString(String type) {
        return switch (type) {
            case "claimer" -> ItemType.CLAIM_TOOL;
            case "grave_head" -> ItemType.GRAVE_HEAD;
            case "stone_brickstep" -> ItemType.STONE_BRICKSTEP;
            default -> null;
        };
    }

    public static ItemType getType(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey("kamstweaks", "item");
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                if (data == null) return null;
                if (data.equals("claimer") && meta.hasEnchants()) {
                    meta.removeEnchantments();
                    meta.setEnchantmentGlintOverride(true);
                    item.setItemMeta(meta);
                }
                return fromString(data);
            }
        }
        return null;
    }

    public static void registerCommand(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("itemmuseum").requires(source -> {
            if (source.getSender() instanceof Player plr) {
                return plr.getUniqueId().toString().equals("b638c3bb-1c3b-4928-97f7-1c8f75d7a59b");
            }
            return false;
        }).then(Commands.argument("item", StringArgumentType.word()).requires(source -> {
            if (source.getSender() instanceof Player plr) {
                return plr.getUniqueId().toString().equals("b638c3bb-1c3b-4928-97f7-1c8f75d7a59b");
            }
            return false;
        }).suggests((ctx, builder) -> {
            for (var val : ItemType.values()) {
                if (val.key.getKey().contains(builder.getRemaining().toLowerCase()) || builder.getRemaining().isEmpty())
                    builder.suggest(val.key.getKey());
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            var pre = ctx.getArgument("item", String.class);
            var type = fromString(pre);
            var sender = ctx.getSource().getSender();
            if (type == null) {
                sender.sendMessage(Component.text("Item '" + pre + "' doesn't exist."));
                return Command.SINGLE_SUCCESS;
            }
            ((Player) sender).getInventory().addItem(ItemManager.createItem(type));
            sender.sendMessage(Component.text("Gave '" + pre + "'."));
            return Command.SINGLE_SUCCESS;
        })).build());
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        var view = event.getView();
        view.setMaximumRepairCost(Integer.MAX_VALUE);
        if (view.getRepairCost() > 30) view.setRepairCost((int) (30 + (Math.log(view.getRepairCost()) * Math.log(view.getRepairCost()) * 2)));
    }
}
