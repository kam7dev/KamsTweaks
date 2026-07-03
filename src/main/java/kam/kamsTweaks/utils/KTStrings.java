package kam.kamsTweaks.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public enum KTStrings {
    // Items
    ITEM_CLAIM_TOOL,
    ITEM_STONE_BRICKSTEP,
    ITEM_ICE_MOUNTAIN,
    ITEM_LAVA_PATH,
    ITEM_NAME_TAG_PRO_MAX,

    ITEM_TAG_WAXED,

    TOO_EXPENSIVE_FIX,

    // Generic
    PLAYERS_ONLY,
    DISABLED_SINGULAR,
    DISABLED_PLURAL,
    CLOSE,
    YES,
    NO,
    ON,
    OFF,
    DEFAULT,
    CONFIRM,
    CONFIRM_DESC,
    DISCARD,
    DISCARD_DESC,
    IRREVERSIBLE,
    SETTINGS,
    DELETE,
    NAME,
    PRIORITY,
    THE_SERVER,
    SAVED,
    VERSION,
    COOLDOWN,
    COOLDOWN_MS,

    // Automod
    AUTOMOD_NAME,
    AUTOMOD_ANVIL,
    AUTOMOD_SIGN,
    AUTOMOD_CHAT,
    AUTOMOD_CLAIM,

    // Claims
    CLAIMS,
    CLAIM_TOOL_INFO,
    CLAIM_TOOL_HINT,
    CLAIM_DESC,
    CLAIM_CREATE,
    CLAIM_CANCEL,
    CLAIM_EDIT,
    CLAIM_EDIT_TITLE,
    CLAIM_VIEW_ALL,
    CLAIM_HIGHLIGHTED,
    CLAIM_LIST,
    CLAIM_DELETE_ALL,
    CLAIM_DELETE_TITLE,
    CLAIMS_DELETE_ALL_TITLE,
    CLAIM_DELETE_CONFIRM,
    CLAIM_DELETE_ALL_CONFIRM,
    CLAIM_ALL_DELETED,
    CLAIM_CANT_MANAGE,
    CLAIM_OP_WARNING,
    CLAIM_OP_EDIT_SETTINGS,
    CLAIM_OP_EDIT_PERMS,
    CLAIM_SETTINGS,
    CLAIM_REGULAR,
    CLAIM_REGULAR_DESC,
    CLAIM_ADVANCED,
    CLAIM_ADVANCED_DESC,
    CLAIM_HIGHLIGHTED_FOR,
    CLAIMS_SHOWED_GUI_TO,
    CLAIMS_GAVE_TOOL_TO,
    CLAIMS_NONEXISTENT,
    CLAIM_OP_DELETE,
    CLAIM_DELETED,
    CLAIMS_YOU_HAVE,
    CLAIMS_THEY_HAVE,
    CLAIMS_NOT_CLAIMING,
    CLAIMS_CANT_FOR_OTHERS,

    PERMS_DEFAULT_PLAYER,
    PERMS_DEFAULT_ENTITY,
    PERMS_PLAYER,
    PERMS_EDIT,
    PERMS_EDIT_ENTITY,
    PERMS_EDIT_DEFAULT,
    PERMS_EDIT_ENTITY_DEFAULT,
    PERMS_TEST_MODE,
    PERMS_TEST_MODE_HINT,

    LC,
    LAND,
    LC_DOOR_INTERACT,
    LC_BLOCK_INTERACT,
    LC_BLOCK_BREAK,
    LC_BLOCK_PLACE,
    LC_LECTERN_INSERT,
    LC_LECTERN_TAKE,
    LC_DRAIN_CAULDRON,
    LC_DAMAGE_ANVIL,
    LC_EMPTY_BUCKETS,
    LC_FILL_BUCKETS,
    LC_ITEM_FRAME_ITEM_ROTATE,
    LC_ITEM_FRAME_ITEM_TAKE,
    LC_ITEM_FRAME_ITEM_PLACE,
    LC_ARMOR_STAND_ITEM_TAKE,
    LC_ARMOR_STAND_ITEM_PLACE,
    LC_VEHICLE_PLACE,
    LC_VEHICLE_DESTROY,
    LC_DISABLED,
    LC_ENABLING,
    LC_REENABLED,
    LC_ALREADY_CLAIMING,
    LC_MAXED_SLOTS,
    LC_START_CLAIMING,
    LC_STOPPED_CLAIMING,
    LC_ALREADY_CLAIMED,
    LC_CORNER2,
    LC_EXTENDABLE_TOO_LARGE,
    LC_EXTEND,
    LC_EXTEND_DESC,
    LC_NO_SLOTS,
    LC_INTERSECTS,
    LC_CLAIMED,
    LC_ACROSS_DIMENSIONS,
    LC_SLOTS,
    LC_INFO,
    LC_NO_PERM,
    LC_UNCLAIMED,
    LC_OWNED_BY,
    LC_PVP_ENABLE_WARN,
    LC_PVP_ENABLE,
    LC_PVP_DISABLE,
    LC_PVP_CLAIM_DISABLED,

    EC,
    ENTITY,
    EC_INTERACT,
    EC_DAMAGE,
    EC_AGGRO,
    EC_INFO,
    EC_MAX,
    EC_UNCLAIMABLE,
    EC_ALREADY_CLAIMED,
    EC_CLAIMED,
    EC_TAMED,
    EC_CONFIRM,
    EC_NO_PERM,
    EC_UNCLAIMED,
    EC_OWNED_BY,

    // Dragon Toggle
    DRAGON_TOGGLE,

    // Graves
    GRAVE_INFO,
    GRAVE_EXPIRED,
    GRAVE_TIME_LEFT,
    GRAVE_COUNT,
    GRAVE_RECOVERY_MAX,
    GRAVE_RECOVERY,
    GRAVE_NO_ID,
    GRAVE_DELETED,
    GRAVE_NEW,
    GRAVE_TITLE,
    GRAVE_BLOCK_TITLE,
    GRAVE_NAME,
    GRAVE_TO_BLOCK,
    GRAVE_FROM_BLOCK,
    GRAVE_5_MINS,
    GRAVE_1_MIN,
    GRAVE_30_SEC,
    GRAVE_EXPIRE_NOW,
    GRAVE_WELCOME,
    GRAVE_TOGGLED,
    GRAVE_STATUS,

    // User Keep Inventory
    KEEPINV,
    KEEPINV_TOGGLED,
    KEEPINV_STATUS,

    // Names
    NAMES,
    NAME_LENGTH,
    NAME_EMPTY,
    NAME_IN_USE,
    NAME_SET,
    NAME_WHOIS,
    NAME_WHOIS_NONE,

    // PVP Toggle
    PVP,
    PVP_IN_COMBAT,
    PVP_OUT_OF_COMBAT,
    PVP_COMBAT_LOGGED,
    PVP_CURRENT_COMBAT,
    PVP_BLOCK_COMMAND,

    // Silk Spawners
    SPAWNER,

    // /hat
    SLASHHAT_BINDING,
    SLASHHAT_EQUIP,

    // TP
    TP_IMMUNITY_LOST,
    TP_CANCEL,
    TP_CANCEL_DAMAGE,
    TP_CANCEL_DEATH,
    TP_CANCEL_TP,
    TP_CANCEL_MOVE,
    TP_CANCEL_OTHER_DAMAGE,
    TP_CANCEL_OTHER_LEFT,
    TP_CANCEL_OTHER_DEATH,
    TP_CANCEL_OTHER_TP,
    TP_CANCEL_OTHER_MOVE,
    TP_CANCEL_PASSENGER,
    TP_CANCEL_PASSENGER_INFO,
    TP_ALREADY_TELEPORTING,
    TP_COOLDOWN,
    TP_OTHER_COOLDOWN,

    WARPS,
    WARP_NOT_EXIST,
    WARP_CREATED,
    WARP_DELETED,
    WARP_COUNT,
    WARP_INFO,
    TP_TO_WARP,

    BACK_NO_RECENT,
    TP_TO_BACK,

    TP_TO_SPAWN,

    HOMES,
    HOMES_NONE,
    HOMES_SET,
    TP_TO_HOME,

    TPA_ALREADY_OUTGOING,
    TPA_SELF,
    TPA_DEAD,
    TPA_OTHER_DEAD,
    TPA_BLOCKED,
    TPA_SENT,
    TPA_RECIEVED,
    TPA,
    TPA_HERE,
    TPA_TO_EXPIRED,
    TPA_FROM_EXPIRED,
    TPA_ACCEPTED,
    TPA_DECLINED,
    TPA_WILL,
    TPA_OTHER_WILL,
    TPA_AUTOACCEPTED,
    TPA_OTHER_ACCEPTED,
    TPA_OTHER_DECLINED,
    TPA_NO_PENDING,
    TPA_ADD_BLOCK,
    TPA_REMOVE_BLOCK,
    TPA_AUTO_ENABLED,
    TPA_AUTO_DISABLED,
    TPA_CANCELLED,
    TPA_OTHER_CANCELLED,
    TPA_CANCEL,

    VANISH_OP_VANISHED,
    VANISH_OP_UNVANISHED,
    VANISH_VANISHED,
    VANISH_UNVANISHED,
    VANISH_VANISHED_OTHER,
    VANISH_UNVANISHED_OTHER,
    VANISH_STATUS_V,
    VANISH_STATUS_U,
    ;

    public static Component getFor(KTStrings key, @NotNull ComponentLike... args) {
        switch (key) {
            case ITEM_CLAIM_TOOL -> {
                return Component.translatable("kamstweaks.item.claim_tool", "Claim Tool", args);
            }
            case ITEM_STONE_BRICKSTEP -> {
                return Component.translatable("kamstweaks.item.stone_brickstep", "Stone Brickstep", args);
            }
            case ITEM_ICE_MOUNTAIN -> {
                return Component.translatable("kamstweaks.item.ice_mountain", "Music Disc", args);
            }
            case ITEM_LAVA_PATH -> {
                return Component.translatable("kamstweaks.item.lava_path", "Music Disc", args);
            }
            case ITEM_NAME_TAG_PRO_MAX -> {
                return Component.translatable("kamstweaks.item.name_tag_pro_max", "Name Tag Pro Max", args);
            }
            case ITEM_TAG_WAXED -> {
                return Component.translatable("kamstweaks.item.tag.waxed", "Waxed", args);
            }
            case TOO_EXPENSIVE_FIX -> {
                return Component.translatable("kamstweaks.item.hint.too_expensive_fix", "If the anvil says too expensive, try installing %s and try again. If you already have the mod installed you can ignore this.", args);
            }

            case PLAYERS_ONLY -> {
                return Component.translatable("kamstweaks.commands.players_only", "Only players can use %s.", args);
            }
            case DISABLED_SINGULAR -> {
                return Component.translatable("kamstweaks.commands.disabled_singular", "%s is disabled.", args);
            }
            case DISABLED_PLURAL -> {
                return Component.translatable("kamstweaks.commands.disabled_plural", "%s are disabled.", args);
            }
            case CLOSE -> {
                return Component.translatable("kamstweaks.generic.close", "Close", args);
            }
            case YES -> {
                return Component.translatable("kamstweaks.generic.yes", "Yes", args);
            }
            case NO -> {
                return Component.translatable("kamstweaks.generic.no", "No", args);
            }
            case ON -> {
                return Component.translatable("kamstweaks.generic.on", "On", args);
            }
            case OFF -> {
                return Component.translatable("kamstweaks.generic.off", "Off", args);
            }
            case DEFAULT -> {
                return Component.translatable("kamstweaks.generic.default", "Default (%s)", args);
            }
            case CONFIRM -> {
                return Component.translatable("kamstweaks.generic.confirm", "Confirm", args);
            }
            case CONFIRM_DESC -> {
                return Component.translatable("kamstweaks.generic.confirm_desc", "Click to confirm your changes.", args);
            }
            case DISCARD -> {
                return Component.translatable("kamstweaks.generic.discard", "Discard", args);
            }
            case DISCARD_DESC -> {
                return Component.translatable("kamstweaks.generic.discard_desc", "Click to discard your changes.", args);
            }
            case IRREVERSIBLE -> {
                return Component.translatable("kamstweaks.generic.irreversible", "This is irreversible.", args);
            }
            case SETTINGS -> {
                return Component.translatable("kamstweaks.generic.settings", "Settings", args);
            }
            case DELETE -> {
                return Component.translatable("kamstweaks.generic.delete", "Delete", args);
            }
            case NAME -> {
                return Component.translatable("kamstweaks.generic.name", "Name", args);
            }
            case PRIORITY -> {
                return Component.translatable("kamstweaks.generic.priority", "Priority", args);
            }
            case THE_SERVER -> {
                return Component.translatable("kamstweaks.generic.the_server", "The Server", args);
            }
            case SAVED -> {
                return Component.translatable("kamstweaks.generic.saved", "Saved KamsTweaks.", args);
            }
            case VERSION -> {
                return Component.translatable("kamstweaks.generic.version", "KamsTweaks is on version %s.", args);
            }
            case COOLDOWN -> {
                return Component.translatable("kamstweaks.generic.cooldown", "%s is on cooldown for %s seconds.", args);
            }
            case COOLDOWN_MS -> {
                return Component.translatable("kamstweaks.generic.cooldown_ms", "%s is on cooldown for %s minutes %s seconds.", args);
            }

            case AUTOMOD_NAME -> {
                return Component.translatable("kamstweaks.automod.name", "Nickname by %s was caught by the %s automod: %s", args);
            }
            case AUTOMOD_ANVIL -> {
                return Component.translatable("kamstweaks.automod.anvil", "Anvil rename by %s was caught by the %s automod: %s", args);
            }
            case AUTOMOD_SIGN -> {
                return Component.translatable("kamstweaks.automod.sign", "Sign by %s was caught by the %s automod: %s", args);
            }
            case AUTOMOD_CHAT -> {
                return Component.translatable("kamstweaks.automod.chat", "Message by %s was caught by the %s automod: %s", args);
            }
            case AUTOMOD_CLAIM -> {
                return Component.translatable("kamstweaks.automod.claim", "Claim rename by %s was caught by the %s automod: %s", args);
            }

            case CLAIMS -> {
                return Component.translatable("kamstweaks.claims", "Claims", args);
            }
            case CLAIM_TOOL_INFO -> {
                return Component.translatable("kamstweaks.claims.tool_info", "Claim Tool Info", args);
            }
            case CLAIM_TOOL_HINT -> {
                return Component.translatable("kamstweaks.claims.tool_hint", "Right click to use the claim tool.", args);
            }
            case CLAIM_DESC -> {
                return Component.translatable("kamstweaks.claims.description", """
                        Welcome to the Claim Tool! This explanation will guide you through its usage.
                        
                        To create a land claim, you must select an area by right-clicking two different blocks (or the same block twice if you want). Claims do not extend upward or downward infinitely, so to claim a tall build you need to select the lowest point and the highest point as well.
                        
                        You can also claim mobs, armor stands, boats, etc. These are entity claims.
                        
                        You are able to edit your claims to set the default permissions (which apply to all players) and give specific permissions for specific players, or set the behavior of the claimed entity. You can also change the name of your land claims and set their priority.
                        
                        Server operators are able to edit or delete your claims at any time if they need to.
                        
                        Written by Jaid""", args);
            }
            case CLAIM_CREATE -> {
                return Component.translatable("kamstweaks.claims.create", "Create a Claim", args);
            }
            case CLAIM_CANCEL -> {
                return Component.translatable("kamstweaks.claims.cancel", "Cancel Claiming", args);
            }
            case CLAIM_EDIT -> {
                return Component.translatable("kamstweaks.claims.edit", "Edit Claim", args);
            }
            case CLAIM_EDIT_TITLE -> {
                return Component.translatable("kamstweaks.claims.edit_title", "Edit Claim: %s", args);
            }
            case CLAIM_VIEW_ALL -> {
                return Component.translatable("kamstweaks.claims.view_all", "View All Claims", args);
            }
            case CLAIM_HIGHLIGHTED -> {
                return Component.translatable("kamstweaks.claims.highlighted", "Nearby claims are being highlighted.", args);
            }
            case CLAIM_LIST -> {
                return Component.translatable("kamstweaks.claims.list", "List Your Claims", args);
            }
            case CLAIM_DELETE_ALL -> {
                return Component.translatable("kamstweaks.claims.delete_all", "Delete ALL of Your Claims", args);
            }
            case CLAIM_DELETE_TITLE -> {
                return Component.translatable("kamstweaks.claims.delete_title", "Delete this %s?", args);
            }
            case CLAIMS_DELETE_ALL_TITLE -> {
                return Component.translatable("kamstweaks.claims.delete_all_title", "Delete ALL of your %s?", args);
            }
            case CLAIM_DELETE_CONFIRM -> {
                return Component.translatable("kamstweaks.claims.delete_confirm", "Are you sure you want to delete this %s?", args);
            }
            case CLAIM_DELETE_ALL_CONFIRM -> {
                return Component.translatable("kamstweaks.claims.delete_all_confirm", "Are you sure you want to ALL of your %s?", args);
            }
            case CLAIM_ALL_DELETED -> {
                return Component.translatable("kamstweaks.claims.all_deleted", "Deleted all of your %s successfully.", args);
            }
            case CLAIM_CANT_MANAGE -> {
                return Component.translatable("kamstweaks.claims.cant_manage", "You cannot manage this claim.", args);
            }
            case CLAIM_OP_WARNING -> {
                return Component.translatable("kamstweaks.claims.op_warning", "Careful! This claim is owned by %s!\nYou can only edit this because you are an operator.\nOther ops will be notified.", args);
            }
            case CLAIM_OP_EDIT_SETTINGS -> {
                return Component.translatable("kamstweaks.claims.op_settings", "[%s: Edited settings for %s's %s]", args);
            }
            case CLAIM_OP_EDIT_PERMS -> {
                return Component.translatable("kamstweaks.claims.op_perms", "[%s: Edited permissions for %s's %s]", args);
            }
            case CLAIM_SETTINGS -> {
                return Component.translatable("kamstweaks.claims.edit_settings", "Edit Claim Settings: %s", args);
            }
            case CLAIM_REGULAR -> {
                return Component.translatable("kamstweaks.claims.regular", "Regular Options", args);
            }
            case CLAIM_REGULAR_DESC -> {
                return Component.translatable("kamstweaks.claims.regular_desc", "Click to edit regular options", args);
            }
            case CLAIM_ADVANCED -> {
                return Component.translatable("kamstweaks.claims.advanced", "Advanced Options", args);
            }
            case CLAIM_ADVANCED_DESC -> {
                return Component.translatable("kamstweaks.claims.advanced_desc", "Click to edit advanced options", args);
            }
            case CLAIM_HIGHLIGHTED_FOR -> {
                return Component.translatable("kamstweaks.claims.highlighted_for", "Highlighting nearby claims for %s.", args);
            }
            case CLAIMS_SHOWED_GUI_TO -> {
                return Component.translatable("kamstweaks.claims.showed_gui_to", "Showed claims gui to %s.", args);
            }
            case CLAIMS_GAVE_TOOL_TO -> {
                return Component.translatable("kamstweaks.claims.gave_tool_to", "Gave claim tool to %s.", args);
            }
            case CLAIMS_NONEXISTENT -> {
                return Component.translatable("kamstweaks.claims.not_exist", "This claim does not exist.", args);
            }
            case CLAIM_OP_DELETE -> {
                return Component.translatable("kamstweaks.claims.op_delete", "[%s: Deleted %s's %s]", args);
            }
            case CLAIM_DELETED -> {
                return Component.translatable("kamstweaks.claims.deleted", "Deleted claim successfully.", args);
            }
            case CLAIMS_YOU_HAVE -> {
                return Component.translatable("kamstweaks.claims.claims_have", "You have %s %s.", args);
            }
            case CLAIMS_THEY_HAVE -> {
                return Component.translatable("kamstweaks.claims.claims_other_has", "%s has %s %s.", args);
            }
            case CLAIMS_CANT_FOR_OTHERS -> {
                return Component.translatable("kamstweaks.claims.cant_for_others", "You can't claim for other people.", args);
            }
            case PERMS_DEFAULT_PLAYER -> {
                return Component.translatable("kamstweaks.claims.perms.default_player", "Default Player Permissions", args);
            }
            case PERMS_DEFAULT_ENTITY -> {
                return Component.translatable("kamstweaks.claims.perms.default_entity", "Default Entity Permissions", args);
            }
            case PERMS_PLAYER -> {
                return Component.translatable("kamstweaks.claims.perms.player", "Player Permissions", args);
            }
            case PERMS_EDIT -> {
                return Component.translatable("kamstweaks.claims.perms.edit", "Edit Permissions: %s", args);
            }
            case PERMS_EDIT_ENTITY -> {
                return Component.translatable("kamstweaks.claims.perms.edit_entity", "Edit %s's Perms: %s", args);
            }
            case PERMS_EDIT_DEFAULT -> {
                return Component.translatable("kamstweaks.claims.perms.edit_default_player", "Edit Default Player Permissions", args);
            }
            case PERMS_EDIT_ENTITY_DEFAULT -> {
                return Component.translatable("kamstweaks.claims.perms.edit_default_entity", "Edit Default Entity Permissions", args);
            }
            case PERMS_TEST_MODE -> {
                return Component.translatable("kamstweaks.claims.perms.test_mode", "Test Mode");
            }
            case PERMS_TEST_MODE_HINT -> {
                return Component.translatable("kamstweaks.claims.perms.test_mode_hint", "You're currently in test mode. Disable this in your claim's settings.");
            }

            case LC -> {
                return Component.translatable("kamstweaks.claims.land", "Land Claims", args);
            }
            case LAND -> {
                return Component.translatable("kamstweaks.claims.land.singular", "Land Claim", args);
            }
            case LC_DOOR_INTERACT -> {
                return Component.translatable("kamstweaks.claims.land.door_interact", "Interact with Doors", args);
            }
            case LC_BLOCK_INTERACT -> {
                return Component.translatable("kamstweaks.claims.land.block_interact", "Interact with Blocks", args);
            }
            case LC_BLOCK_BREAK -> {
                return Component.translatable("kamstweaks.claims.land.block_break", "Break Blocks", args);
            }
            case LC_BLOCK_PLACE -> {
                return Component.translatable("kamstweaks.claims.land.block_place", "Place Blocks", args);
            }
            case LC_LECTERN_INSERT -> {
                return Component.translatable("kamstweaks.claims.land.lectern_insert", "Place into Lecterns", args);
            }
            case LC_LECTERN_TAKE -> {
                return Component.translatable("kamstweaks.claims.land.lectern_take", "Take from Lecterns", args);
            }
            case LC_DRAIN_CAULDRON -> {
                return Component.translatable("kamstweaks.claims.land.drain_cauldron", "Drain Cauldrons", args);
            }
            case LC_DAMAGE_ANVIL -> {
                return Component.translatable("kamstweaks.claims.land.damage_anvil", "Damage Anvils", args);
            }
            case LC_EMPTY_BUCKETS -> {
                return Component.translatable("kamstweaks.claims.land.empty_buckets", "Empty Buckets", args);
            }
            case LC_FILL_BUCKETS -> {
                return Component.translatable("kamstweaks.claims.land.fill_buckets", "Fill Buckets", args);
            }
            case LC_ITEM_FRAME_ITEM_ROTATE -> {
                return Component.translatable("kamstweaks.claims.land.item_frame_rotate", "Rotate Items in Frames", args);
            }
            case LC_ITEM_FRAME_ITEM_TAKE -> {
                return Component.translatable("kamstweaks.claims.land.item_frame_take", "Take Items from Frames", args);
            }
            case LC_ITEM_FRAME_ITEM_PLACE -> {
                return Component.translatable("kamstweaks.claims.land.item_frame_place", "Place Items into Frames", args);
            }
            case LC_ARMOR_STAND_ITEM_TAKE -> {
                return Component.translatable("kamstweaks.claims.land.armor_stand_take", "Take from Armor Stands", args);
            }
            case LC_ARMOR_STAND_ITEM_PLACE -> {
                return Component.translatable("kamstweaks.claims.land.armor_stand_place", "Place onto Armor Stands", args);
            }
            case LC_VEHICLE_DESTROY -> {
                return Component.translatable("kamstweaks.claims.land.vehicle_destroy", "Destroy Vehicles", args);
            }
            case LC_VEHICLE_PLACE -> {
                return Component.translatable("kamstweaks.claims.land.vehicle_place", "Place Vehicles", args);
            }
            case LC_DISABLED -> {
                return Component.translatable("kamstweaks.claims.land.end_disable", "Claims are currently disabled at the end island due to an ongoing dragon fight. They will be re-enabled 5 minutes after the fight.", args);
            }
            case LC_ENABLING -> {
                return Component.translatable("kamstweaks.claims.land.end_enabling", "Claims are currently disabled at the end island due to a recent dragon fight. They will be re-enabled in %s seconds.", args);
            }
            case LC_REENABLED -> {
                return Component.translatable("kamstweaks.claims.land.end_reenabled", "Claims are now re-enabled in this dimension.", args);
            }
            case LC_ALREADY_CLAIMING -> {
                return Component.translatable("kamstweaks.claims.land.already", "You're already claiming land. (run %s to cancel)", args);
            }
            case LC_MAXED_SLOTS -> {
                return Component.translatable("kamstweaks.claims.land.maxed-slots", "You have used all of your claim slots. Delete some to free up slots.", args);
            }
            case LC_START_CLAIMING -> {
                return Component.translatable("kamstweaks.claims.land.start", "Right click the first corner of where you want to claim with your claim tool. (If you lost it, run %s)", args);
            }
            case LC_STOPPED_CLAIMING -> {
                return Component.translatable("kamstweaks.claims.land.stop", "Stopped claiming land.", args);
            }
            case CLAIMS_NOT_CLAIMING -> {
                return Component.translatable("kamstweaks.claims.land.not_claiming", "You aren't currently claiming land. (run %s or use the claim tool to start)", args);
            }
            case LC_ALREADY_CLAIMED -> {
                return Component.translatable("kamstweaks.claims.land.already_claimed", "This land is already claimed by %s.", args);
            }
            case LC_CORNER2 -> {
                return Component.translatable("kamstweaks.claims.land.corner2", "Now click the other corner with your claim tool. (If you lost it, run %s)", args);
            }
            case LC_EXTENDABLE_TOO_LARGE -> {
                return Component.translatable("kamstweaks.claims.land.too_large", "You can't claim more than %s blocks in an unextended claim, while you are trying to claim %s. You need %s more claim slot(s) for an extension (costs %s).", args);
            }
            case LC_EXTEND -> {
                return Component.translatable("kamstweaks.claims.land.extend", "Extend claim?", args);
            }
            case LC_EXTEND_DESC -> {
                return Component.translatable("kamstweaks.claims.land.extend_desc", "This claim is larger than %s blocks. Do you want to use %s extra claim slot(s) to extend it?", args);
            }
            case LC_NO_SLOTS -> {
                return Component.translatable("kamstweaks.claims.land.no_slots", "You have used up too many slots to claim this. Delete some to free up slots.", args);
            }
            case LC_INTERSECTS -> {
                return Component.translatable("kamstweaks.claims.land.intersects", "This land is intersects a claim by %s.", args);
            }
            case LC_CLAIMED -> {
                return Component.translatable("kamstweaks.claims.land.claimed", "Territory claimed (%s)", args);
            }
            case LC_ACROSS_DIMENSIONS -> {
                return Component.translatable("kamstweaks.claims.land.dimensions", "You can't claim across dimensions - go back to the dimension you started in!", args);
            }
            case LC_SLOTS -> {
                return Component.translatable("kamstweaks.claims.land.slots", "(%s slots)", args);
            }
            case LC_INFO -> {
                return Component.translatable("kamstweaks.claims.land.info", "(%s) %s (priority %s): %s to %s in %s %s", args);
            }
            case LC_NO_PERM -> {
                return Component.translatable("kamstweaks.claims.land.no_perms", "You don't have permission to %s here! (Claim owned by %s)", args);
            }
            case LC_UNCLAIMED -> {
                return Component.translatable("kamstweaks.claims.land.unclaimed", "This land isn't claimed.", args);
            }
            case LC_OWNED_BY -> {
                return Component.translatable("kamstweaks.claims.land.owned_by", "This land is owned by %s.", args);
            }
            case LC_PVP_ENABLE_WARN -> {
                return Component.translatable("kamstweaks.claims.land.pvp.enable_warn", "PVP in this claim will be enabled in %s seconds.", args);
            }
            case LC_PVP_ENABLE -> {
                return Component.translatable("kamstweaks.claims.land.pvp.enable", "PVP in this claim is now enabled.", args);
            }
            case LC_PVP_DISABLE -> {
                return Component.translatable("kamstweaks.claims.land.pvp.disable", "PVP in this claim is now disabled. Ongoing fights may continue.", args);
            }
            case LC_PVP_CLAIM_DISABLED -> {
                return Component.translatable("kamstweaks.claims.land.pvp.disabled", "This claim has PVP disabled.", args);
            }

            case EC -> {
                return Component.translatable("kamstweaks.claims.entity", "Entity Claims", args);
            }
            case ENTITY -> {
                return Component.translatable("kamstweaks.claims.entity.singular", "Entity Claim", args);
            }
            case EC_INTERACT -> {
                return Component.translatable("kamstweaks.claims.entity.interact", "Interact with this entity", args);
            }
            case EC_DAMAGE -> {
                return Component.translatable("kamstweaks.claims.entity.damage", "Damage this entity", args);
            }
            case EC_AGGRO -> {
                return Component.translatable("kamstweaks.claims.entity.aggro", "Can Aggro", args);
            }
            case EC_INFO -> {
                return Component.translatable("kamstweaks.claims.entity.info", "(%s) %s: %s in %s", args);
            }
            case EC_MAX -> {
                return Component.translatable("kamstweaks.claims.entity.max", "You already have the max number of claims! (%s/%s)", args);
            }
            case EC_UNCLAIMABLE -> {
                return Component.translatable("kamstweaks.claims.entity.unclaimable", "This entity is not claimable!", args);
            }
            case EC_ALREADY_CLAIMED -> {
                return Component.translatable("kamstweaks.claims.entity.already_claimed", "This entity is already claimed by %s.", args);
            }
            case EC_CLAIMED -> {
                return Component.translatable("kamstweaks.claims.entity.claimed", "Claimed %s successfully (%s).", args);
            }
            case EC_TAMED -> {
                return Component.translatable("kamstweaks.claims.entity.tamed", "This entity is tamed and cannot be claimed.", args);
            }
            case EC_CONFIRM -> {
                return Component.translatable("kamstweaks.claims.entity.confirm", "Claim %s?", args);
            }
            case EC_NO_PERM -> {
                return Component.translatable("kamstweaks.claims.entity.no_perms", "You don't have permission to %s! (Entity claimed by %s)", args);
            }
            case EC_UNCLAIMED -> {
                return Component.translatable("kamstweaks.claims.entity.unclaimed", "This entity isn't claimed.", args);
            }
            case EC_OWNED_BY -> {
                return Component.translatable("kamstweaks.claims.entity.owned_by", "This entity is owned by %s.", args);
            }

            case DRAGON_TOGGLE -> {
                return Component.translatable("kamstweaks.dragon.blocked", "The dragon fight is currently disabled. If this is unintended, ask an admin to run %s.", args);
            }

            case GRAVE_INFO -> {
                return Component.translatable("kamstweaks.graves.info", "Grave %s:  %s in %s (%s)", args);
            }
            case GRAVE_EXPIRED -> {
                return Component.translatable("kamstweaks.graves.expired", "expired", args);
            }
            case GRAVE_TIME_LEFT -> {
                return Component.translatable("kamstweaks.graves.time_left", "%s seconds left", args);
            }
            case GRAVE_COUNT -> {
                return Component.translatable("kamstweaks.graves.count", "You have %s grave(s).", args);
            }
            case GRAVE_RECOVERY_MAX -> {
                return Component.translatable("kamstweaks.graves.recovery_max", "You are already recovering %s graves.", args);
            }
            case GRAVE_RECOVERY -> {
                return Component.translatable("kamstweaks.graves.recovery", "You have 10 minutes to recover your grave. You can recover a grave repeatedly, but only %s can be recovered at a time.", args);
            }
            case GRAVE_NO_ID -> {
                return Component.translatable("kamstweaks.graves.no_id", "You don't have a grave with id %s.", args);
            }
            case GRAVE_DELETED -> {
                return Component.translatable("kamstweaks.graves.deleted", "Grave deleted successfully.", args);
            }
            case GRAVE_NEW -> {
                return Component.translatable("kamstweaks.graves.new", "You have a new grave at %s in %s.", args);
            }
            case GRAVE_TITLE -> {
                return Component.translatable("kamstweaks.graves.inv_title", "Grave", args);
            }
            case GRAVE_BLOCK_TITLE -> {
                return Component.translatable("kamstweaks.graves.inv_title_block", "Grave (Stations)", args);
            }
            case GRAVE_NAME -> {
                return Component.translatable("kamstweaks.graves.name", "%s's Grave", args);
            }
            case GRAVE_TO_BLOCK -> {
                return Component.translatable("kamstweaks.graves.to_block", "To Station Inventory", args);
            }
            case GRAVE_FROM_BLOCK -> {
                return Component.translatable("kamstweaks.graves.from_block", "To Main Inventory", args);
            }
            case GRAVE_EXPIRE_NOW -> {
                return Component.translatable("kamstweaks.graves.just_expired", "Your grave (%s) just expired.", args);
            }
            case GRAVE_5_MINS -> {
                return Component.translatable("kamstweaks.graves.5_mins", "Your grave (%s) expires in 5 minutes!", args);
            }
            case GRAVE_1_MIN -> {
                return Component.translatable("kamstweaks.graves.1_min", "Your grave (%s) expires in 1 minute!", args);
            }
            case GRAVE_30_SEC -> {
                return Component.translatable("kamstweaks.graves.30_sec", "Your grave (%s) expires in 30 seconds!", args);
            }
            case GRAVE_WELCOME -> {
                return Component.translatable("kamstweaks.graves.welcome", "You have %s active grave(s) and %s expired grave(s).", args);
            }
            case GRAVE_TOGGLED -> {
                return Component.translatable("kamstweaks.graves.toggled", "Graves are now %s.", args);
            }
            case GRAVE_STATUS -> {
                return Component.translatable("kamstweaks.graves.status", "Graves are currently %s.", args);
            }

            case KEEPINV -> {
                return Component.translatable("kamstweaks.keepinv", "Keep Inventory", args);
            }
            case KEEPINV_TOGGLED -> {
                return Component.translatable("kamstweaks.keepinv.toggled", "Keep inventory is now %s.", args);
            }
            case KEEPINV_STATUS -> {
                return Component.translatable("kamstweaks.keepinv.status", "Keep inventory is currently %s.", args);
            }

            case NAMES -> {
                return Component.translatable("kamstweaks.names", "Nicknames", args);
            }
            case NAME_LENGTH -> {
                return Component.translatable("kamstweaks.names.length", "Nicknames cannot be longer than %s characters.", args);
            }
            case NAME_EMPTY -> {
                return Component.translatable("kamstweaks.names.empty", "Nicknames cannot be empty.", args);
            }
            case NAME_IN_USE -> {
                return Component.translatable("kamstweaks.names.in_use", "Someone already has that nickname.", args);
            }
            case NAME_SET -> {
                return Component.translatable("kamstweaks.names.set", "Your nickname is now %s.", args);
            }
            case NAME_WHOIS -> {
                return Component.translatable("kamstweaks.names.whois", "%s is %s.", args);
            }
            case NAME_WHOIS_NONE -> {
                return Component.translatable("kamstweaks.names.whois_none", "No one is using the nickname %s.", args);
            }

            case PVP -> {
                return Component.translatable("kamstweaks.pvp", "PVP", args);
            }
            case PVP_IN_COMBAT -> {
                return Component.translatable("kamstweaks.pvp.in_combat", "You are now in combat with %s, do not leave.", args);
            }
            case PVP_OUT_OF_COMBAT -> {
                return Component.translatable("kamstweaks.pvp.out_of_combat", "You are no longer in combat.", args);
            }
            case PVP_COMBAT_LOGGED -> {
                return Component.translatable("kamstweaks.pvp.combat_logged", "(Combat Logged)", args);
            }
            case PVP_CURRENT_COMBAT -> {
                return Component.translatable("kamstweaks.pvp.current_combat", "You are currently in combat, do not leave.", args);
            }
            case PVP_BLOCK_COMMAND -> {
                return Component.translatable("kamstweaks.pvp.blocked_command", "You cannot run this since you are currently in combat.", args);
            }

            case SPAWNER -> {
                return Component.translatable("kamstweaks.silkspawners.item_name", "%s Spawner", args);
            }

            case SLASHHAT_BINDING -> {
                return Component.translatable("kamstweaks.slashhat.cursed", "%s can't be taken off.", args);
            }
            case SLASHHAT_EQUIP -> {
                return Component.translatable("kamstweaks.slashhat.equip", "Now wearing %s.", args);
            }

            case TP_IMMUNITY_LOST -> {
                return Component.translatable("kamstweaks.tp.immunity_lost", "Teleport immunity lost since you attacked someone/something.", args);
            }
            case TP_CANCEL -> {
                return Component.translatable("kamstweaks.tp.cancel", "Teleport cancelled because %s.", args);
            }
            case TP_CANCEL_DAMAGE -> {
                return Component.translatable("kamstweaks.tp.cancel_damage", "you took damage", args);
            }
            case TP_CANCEL_DEATH -> {
                return Component.translatable("kamstweaks.tp.cancel_death", "you died", args);
            }
            case TP_CANCEL_TP -> {
                return Component.translatable("kamstweaks.tp.cancel_tp", "you teleported", args);
            }
            case TP_CANCEL_MOVE -> {
                return Component.translatable("kamstweaks.tp.cancel_move", "you moved", args);
            }
            case TP_CANCEL_OTHER_DAMAGE -> {
                return Component.translatable("kamstweaks.tp.cancel_other_damage", "%s took damage", args);
            }
            case TP_CANCEL_OTHER_DEATH -> {
                return Component.translatable("kamstweaks.tp.cancel_other_death", "%s died", args);
            }
            case TP_CANCEL_OTHER_TP -> {
                return Component.translatable("kamstweaks.tp.cancel_other_tp", "%s teleported", args);
            }
            case TP_CANCEL_OTHER_MOVE -> {
                return Component.translatable("kamstweaks.tp.cancel_other_move", "%s moved", args);
            }
            case TP_CANCEL_OTHER_LEFT -> {
                return Component.translatable("kamstweaks.tp.cancel_other_left", "%s left", args);
            }
            case TP_CANCEL_PASSENGER -> {
                return Component.translatable("kamstweaks.tp.cancel_passenger", "you don't have permission to damage an entity in this vehicle", args);
            }
            case TP_CANCEL_PASSENGER_INFO -> {
                return Component.translatable("kamstweaks.tp.cancel_passenger_info", "It requires the damage permission because teleporting the mob would allow a method of killing a claimed entity.", args);
            }
            case TP_ALREADY_TELEPORTING -> {
                return Component.translatable("kamstweaks.tp.already_teleporting", "You are already teleporting somewhere.", args);
            }
            case TP_COOLDOWN -> {
                return Component.translatable("kamstweaks.tp.cooldown", "You're on teleportation cooldown for %s seconds.", args);
            }
            case TP_OTHER_COOLDOWN -> {
                return Component.translatable("kamstweaks.tp.other_cooldown", "%s is teleportation cooldown for %s seconds.", args);
            }

            case WARPS -> {
                return Component.translatable("kamstweaks.tp.warps", "Warps", args);
            }
            case WARP_NOT_EXIST -> {
                return Component.translatable("kamstweaks.tp.warp.not_exist", "Warp %s does not exist.", args);
            }
            case WARP_CREATED -> {
                return Component.translatable("kamstweaks.tp.warp.created", "Created warp \"%s\" successfully.", args);
            }
            case WARP_DELETED -> {
                return Component.translatable("kamstweaks.tp.warp.deleted", "Deleted warp \"%s\" successfully.", args);
            }
            case WARP_COUNT -> {
                return Component.translatable("kamstweaks.tp.warp.count", "The server has %s warp(s):", args);
            }
            case WARP_INFO -> {
                return Component.translatable("kamstweaks.tp.warp.info", "%s: %s in %s", args);
            }
            case TP_TO_WARP -> {
                return Component.translatable("kamstweaks.tp.warp.to", "Teleporting to the %s warp in %s seconds, please do not move.", args);
            }

            case BACK_NO_RECENT -> {
                return Component.translatable("kamstweaks.tp.back.no_recent", "You have not teleported anywhere recently.", args);
            }
            case TP_TO_BACK -> {
                return Component.translatable("kamstweaks.tp.back.to", "Returning to previous location in %s seconds, please do not move.", args);
            }

            case TP_TO_SPAWN -> {
                return Component.translatable("kamstweaks.tp.spawn.to", "Teleporting to spawn in %s seconds, please do not move.", args);
            }

            case HOMES -> {
                return Component.translatable("kamstweaks.tp.homes", "Homes", args);
            }
            case HOMES_NONE -> {
                return Component.translatable("kamstweaks.tp.homes.none", "You don't have a home.", args);
            }
            case HOMES_SET -> {
                return Component.translatable("kamstweaks.tp.homes.set", "Your home has been set.", args);
            }
            case TP_TO_HOME -> {
                return Component.translatable("kamstweaks.tp.homes.to", "Teleporting to your home in %s seconds, please do not move.", args);
            }

            case TPA_ALREADY_OUTGOING -> {
                return Component.translatable("kamstweaks.tp.tpa.outgoing", "You already have an outgoing TPA request.", args);
            }
            case TPA_SELF -> {
                return Component.translatable("kamstweaks.tp.tpa.self", "You can't teleport to yourself, silly!", args);
            }
            case TPA_DEAD -> {
                return Component.translatable("kamstweaks.tp.tpa.dead", "You can't teleport while dead.", args);
            }
            case TPA_OTHER_DEAD -> {
                return Component.translatable("kamstweaks.tp.tpa.other_dead", "That person is currently dead.", args);
            }
            case TPA_BLOCKED -> {
                return Component.translatable("kamstweaks.tp.tpa.blocked", "The player you want to teleport to has you blocked.", args);
            }
            case TPA_SENT -> {
                return Component.translatable("kamstweaks.tp.tpa.sent", "%s request sent to %s. They have 60 seconds to accept it. You can cancel this by running %s.", args);
            }
            case TPA_RECIEVED -> {
                return Component.translatable("kamstweaks.tp.tpa.recieved", "%s has sent you a %s request. Run %s to accept it, or %s to decline. It expires in 60 seconds.", args);
            }
            case TPA -> {
                return Component.translatable("kamstweaks.tp.tpa", "TPA", args);
            }
            case TPA_HERE -> {
                return Component.translatable("kamstweaks.tp.tpa.here", "TPAHere", args);
            }
            case TPA_TO_EXPIRED -> {
                return Component.translatable("kamstweaks.tp.tpa.to_expired", "Your request to %s has expired.", args);
            }
            case TPA_FROM_EXPIRED -> {
                return Component.translatable("kamstweaks.tp.tpa.from_expired", "%s's request has expired.", args);
            }
            case TPA_ACCEPTED -> {
                return Component.translatable("kamstweaks.tp.tpa.accept", "You accepted the request from %s. %s", args);
            }
            case TPA_DECLINED -> {
                return Component.translatable("kamstweaks.tp.tpa.decline", "You declined the request from %s.", args);
            }
            case TPA_WILL -> {
                return Component.translatable("kamstweaks.tp.tpa.will", "You will be teleported to them in %s seconds.", args);
            }
            case TPA_OTHER_WILL -> {
                return Component.translatable("kamstweaks.tp.tpa.other_will", "They will be teleported to you in %s seconds.", args);
            }
            case TPA_AUTOACCEPTED -> {
                return Component.translatable("kamstweaks.tp.tpa.auto", "TPA request accepted automatically.", args);
            }
            case TPA_OTHER_ACCEPTED -> {
                return Component.translatable("kamstweaks.tp.tpa.other_accept", "%s accepted your request. %s", args);
            }
            case TPA_OTHER_DECLINED -> {
                return Component.translatable("kamstweaks.tp.tpa.other_decline", "%s declined your request.", args);
            }
            case TPA_NO_PENDING -> {
                return Component.translatable("kamstweaks.tp.tpa.no_pending", "You have no pending TPA requests.", args);
            }
            case TPA_ADD_BLOCK -> {
                return Component.translatable("kamstweaks.tp.tpa.block", "Blocked %s. They can no longer send you TPA requests.", args);
            }
            case TPA_REMOVE_BLOCK -> {
                return Component.translatable("kamstweaks.tp.tpa.unblock", "Unblocked %s. They can now send you TPA requests.", args);
            }
            case TPA_AUTO_ENABLED -> {
                return Component.translatable("kamstweaks.tp.tpa.auto_enable", "TPAuto enabled. All incoming TPA requests will be accepted automatically.", args);
            }
            case TPA_AUTO_DISABLED -> {
                return Component.translatable("kamstweaks.tp.tpa.auto_disable", "TPAuto disabled. Incoming TPA requests will not be accepted automatically.", args);
            }
            case TPA_CANCELLED -> {
                return Component.translatable("kamstweaks.tp.tpa.cancelled", "TPA request to %s cancelled.", args);
            }
            case TPA_OTHER_CANCELLED -> {
                return Component.translatable("kamstweaks.tp.tpa.other_cancelled", "%s cancelled their TPA request.", args);
            }
            case TPA_CANCEL -> {
                return Component.translatable("kamstweaks.tp.tpa.cancel", "TPA request cancelled because %s.", args);
            }

            case VANISH_OP_VANISHED -> {
                return Component.translatable("kamstweaks.vanish.op_vanished", "[%s: Enabled vanish for %s]", args);
            }
            case VANISH_OP_UNVANISHED -> {
                return Component.translatable("kamstweaks.vanish.op_unvanished", "[%s: Disabled vanish for %s]", args);
            }
            case VANISH_VANISHED -> {
                return Component.translatable("kamstweaks.vanish.vanished", "You are now vanished.", args);
            }
            case VANISH_UNVANISHED -> {
                return Component.translatable("kamstweaks.vanish.unvanished", "You are no longer vanished.", args);
            }
            case VANISH_VANISHED_OTHER -> {
                return Component.translatable("kamstweaks.vanish.vanished_other", "%s is now vanished.", args);
            }
            case VANISH_UNVANISHED_OTHER -> {
                return Component.translatable("kamstweaks.vanish.unvanished_other", "%s is no longer vanished.", args);
            }
            case VANISH_STATUS_V -> {
                return Component.translatable("kamstweaks.vanish.status.vanished", "You are currently vanished.", args);
            }
            case VANISH_STATUS_U -> {
                return Component.translatable("kamstweaks.vanish.status.unvanished", "You are currently not vanished.", args);
            }
        }
        try {
            throw new IllegalArgumentException("Unknown translation string.");
        } catch (Exception e) {
            Logger.handleException(e);
        }
        return Component.text("Unknown translation string, report this to kam ig: " + key.name());
    }

    private static Map<String, String> genLangMap() {
        Map<String, String> lang = new LinkedHashMap<>();
        for (var enumVal : KTStrings.values()) {
            var comp = (TranslatableComponent) getFor(enumVal);
            lang.put(comp.key(), comp.fallback());
        }
        return lang;
    }

    public static String genJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
        return gson.toJson(genLangMap());
    }

    public static String genTxt() {
        var map = genLangMap();
        var ret = new StringBuilder();
        map.forEach((key, val) -> ret.append(key).append("=").append(val.replace("\n", "\\n")).append("\n"));
        return ret.toString();
    }
}
