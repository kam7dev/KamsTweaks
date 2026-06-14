package kam.kamsTweaks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

public enum KTStrings {
    // Generic
    PLAYERS_ONLY,
    DISABLED_SINGULAR,
    DISABLED_PLURAL,

    // Automod
    AUTOMOD_NAME,
    AUTOMOD_ANVIL,
    AUTOMOD_SIGN,
    AUTOMOD_CHAT,
    AUTOMOD_CLAIM,

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

    // Names
    NAMES,
    NAME_LENGTH,
    NAME_EMPTY,
    NAME_IN_USE,
    NAME_SET,
    NAME_WHOIS,
    NAME_WHOIS_NONE,

    // PVP Toggle
    PVP_ENABLE,
    PVP_DISABLE,
    PVP_STATUS_ENABLED,
    PVP_STATUS_DISABLED,
    PVP_YOU_DISABLED,
    PVP_TARGET_DISABLED,

    // Silk Spawners
    SPAWNER,

    // /hat
    SLASHHAT_BINDING,
    SLASHHAT_EQUIP,

    // TP
    TP_IMMUNITY_LOST, //
    TP_CANCEL, //
    TP_CANCEL_DAMAGE, //
    TP_CANCEL_DEATH, //
    TP_CANCEL_TP, //
    TP_CANCEL_MOVE, //
    TP_CANCEL_OTHER_DAMAGE, //
    TP_CANCEL_OTHER_LEFT, //
    TP_CANCEL_OTHER_DEATH,
    TP_CANCEL_OTHER_TP, //
    TP_CANCEL_OTHER_MOVE, //
    TP_CANCEL_PASSENGER, //
    TP_CANCEL_PASSENGER_INFO, //
    TP_ALREADY_TELEPORTING,
    TP_COOLDOWN,
    TP_OTHER_COOLDOWN,

    WARPS,
    WARP_NOT_EXIST,
    WARP_CREATED,
    WARP_DELETED,
    WARP_LIST,
    WARP_COUNT,
    WARP_INFO,
    TP_TO_WARP,

    BACK_INFO,
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
    TPA_CANCEL
    ;

    public static Component getFor(KTStrings key, @NotNull ComponentLike... args) {
        switch (key) {
            case PLAYERS_ONLY -> {
                return Component.translatable("kamstweaks.commands.players_only", "Only players can use %s.", args);
            }
            case DISABLED_SINGULAR -> {
                return Component.translatable("kamstweaks.commands.disabled_singular", "%s is disabled.", args);
            }
            case DISABLED_PLURAL -> {
                return Component.translatable("kamstweaks.commands.disabled_plural", "%s are disabled.", args);
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

            case PVP_ENABLE -> {
                return Component.translatable("kamstweaks.pvp.enable", "PVP is now enabled.", args);
            }
            case PVP_DISABLE -> {
                return Component.translatable("kamstweaks.pvp.disable", "PVP is now disabled.", args);
            }
            case PVP_STATUS_ENABLED -> {
                return Component.translatable("kamstweaks.pvp.status_enabled", "PVP is currently enabled.", args);
            }
            case PVP_STATUS_DISABLED -> {
                return Component.translatable("kamstweaks.pvp.status_disabled", "PVP is currently disabled.", args);
            }
            case PVP_YOU_DISABLED -> {
                return Component.translatable("kamstweaks.pvp.you_disabled", "You have PVP disabled.", args);
            }
            case PVP_TARGET_DISABLED -> {
                return Component.translatable("kamstweaks.pvp.target_disabled", "%s has PVP disabled.", args);
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

            case BACK_INFO -> {
                return Component.translatable("kamstweaks.tp.back.info", "Return to your death location with %s.", args);
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
                return Component.translatable("kamstweaks.tp.homes.to", "Teleporting to spawn in %s seconds, please do not move.", args);
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
        }
        try {
            throw new RuntimeException("Unknown translation string.");
        } catch (Exception e) {
            Logger.handleException(e);
        }
        return Component.text("Unknown translation string, report this to kam ig");
    }
}
