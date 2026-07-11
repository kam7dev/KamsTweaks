package kam.kamsTweaks.managers;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.item.JukeboxPlayable;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.features.fun.nicknames.Names;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class KTItems implements Listener {
    public enum ItemType {
        CLAIM_TOOL("claimer", KTStrings.ITEM_CLAIM_TOOL),
        NAME_TAG_PRO_MAX("name_tag_pro_max", KTStrings.ITEM_NAME_TAG_PRO_MAX),
        GRAVE_HEAD("grave_head", KTStrings.GRAVE_TITLE),
        STONE_BRICKSTEP("stone_brickstep", KTStrings.ITEM_STONE_BRICKSTEP),
        ICE_MOUNTAIN("ice_mountain", KTStrings.ITEM_ICE_MOUNTAIN),
        LAVA_PATH("lava_path", KTStrings.ITEM_LAVA_PATH),

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
        WAXED("waxed");

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

    public static Map<ItemType, ItemStack> items;
    public static final NamespacedKey key = new NamespacedKey("kamstweaks", "item");

    private static ItemStack makeBaseItem(ItemType type, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.key.getKey());
            item.setItemMeta(meta);
        }
        item.setData(DataComponentTypes.ITEM_NAME, type.name.decoration(TextDecoration.ITALIC, false));
        item.unsetData(DataComponentTypes.JUKEBOX_PLAYABLE);
        return item;
    }

    private static ItemStack makeBaseItem(ItemType type) {
        return makeBaseItem(type, Material.MUSIC_DISC_PIGSTEP);
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
                item.setItemMeta(meta);
            }
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key("minecraft", "structure_void"));
            items.put(ItemType.CLAIM_TOOL, item);
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
        {
            var item = makeBaseItem(ItemType.ICE_MOUNTAIN);
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key("kamstweaks", "ice_mountain"));
            var reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.JUKEBOX_SONG).get(Key.key("kamstweaks", "ice_mountain"));
            if (reg != null)  item.setData(DataComponentTypes.JUKEBOX_PLAYABLE, JukeboxPlayable.jukeboxPlayable(reg));
            items.put(ItemType.ICE_MOUNTAIN, item);
        }
        {
            var item = makeBaseItem(ItemType.LAVA_PATH);
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key("kamstweaks", "lava_path"));
            var reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.JUKEBOX_SONG).get(Key.key("kamstweaks", "lava_path"));
            if (reg != null)  item.setData(DataComponentTypes.JUKEBOX_PLAYABLE, JukeboxPlayable.jukeboxPlayable(reg));
            items.put(ItemType.LAVA_PATH, item);
        }
        {
            var item = makeBaseItem(ItemType.NAME_TAG_PRO_MAX, Material.NAME_TAG);
            item.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            items.put(ItemType.NAME_TAG_PRO_MAX, item);
        }
    }

    public static ItemStack createItem(ItemType type) {
        return items.get(type).clone();
    }

    private static ItemType fromString(String type) {
        return switch (type) {
            case "claimer" -> ItemType.CLAIM_TOOL;
            case "grave_head" -> ItemType.GRAVE_HEAD;
            case "stone_brickstep" -> ItemType.STONE_BRICKSTEP;
            case "ice_mountain" -> ItemType.ICE_MOUNTAIN;
            case "lava_path" -> ItemType.LAVA_PATH;
            case "name_tag_pro_max" -> ItemType.NAME_TAG_PRO_MAX;
            default -> null;
        };
    }

    public static ItemType getType(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
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
    }

    public static void registerKTSub(LiteralArgumentBuilder<CommandSourceStack> base) {
        base.then(Commands.literal("give").requires(source -> source.getSender().hasPermission("kamstweaks.items.give")).then(Commands.argument("item", StringArgumentType.word()).requires(source -> {
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
            // i dont really see a need to use translation strings for a dev command
            if (type == null) {
                sender.sendMessage(Component.text("Item '" + pre + "' doesn't exist."));
                return Command.SINGLE_SUCCESS;
            }
            ((Player) sender).getInventory().addItem(KTItems.createItem(type));
            sender.sendMessage(Component.text("Gave '" + pre + "'."));
            return Command.SINGLE_SUCCESS;
        })));
    }

    private static final String PACK_ID = "1V21rNSU933OJpMYJrVIbovwbocVGlmq1";
    private static final String PACK_SHA1 = "39237c8f99e1d20b48b9d49a87f206aef9618349";
    private static final ResourcePackInfo PACK_INFO = ResourcePackInfo.resourcePackInfo()
            .uri(URI.create("https://drive.google.com/uc?export=download&id=" + PACK_ID))
            .hash(PACK_SHA1)
            .build();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!Config.getBool("suggest-resource-pack", true)) return;
        final ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(PACK_INFO)
                .prompt(Component.text("This resource pack is for KamsTweaks."))
                .required(false)
                .replace(false)
                .build();

        event.getPlayer().sendResourcePacks(request);
    }

    public static void convertTo(ItemStack item, ItemType type, Key songKey) {
        item.setData(DataComponentTypes.ITEM_MODEL, songKey);
        var reg = RegistryAccess.registryAccess().getRegistry(RegistryKey.JUKEBOX_SONG).get(songKey);
        if (reg != null) item.setData(DataComponentTypes.JUKEBOX_PLAYABLE, JukeboxPlayable.jukeboxPlayable(reg));
        item.setData(DataComponentTypes.ITEM_NAME, type.name.decoration(TextDecoration.ITALIC, false));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.key.getKey());
            item.setItemMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClick(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        if (event.getPlayer().isSneaking()) return;
        var item = event.getPlayer().getInventory().getItemInMainHand();
        if (getType(item) != ItemType.NAME_TAG_PRO_MAX) return;
        event.setCancelled(true);
        Names.setName(target, new Names.NameData("minimessage", item.effectiveName()));
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) item.setAmount(item.getAmount() - 1);
    }
}
