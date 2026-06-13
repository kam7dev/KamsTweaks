package kam.kamsTweaks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

public enum KTStrings {
    // Generic
    PLAYERS_ONLY,
    DISABLED_SINGULAR,
    DISABLED_PLURAL,

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
    NAME_AUTOMOD,
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

    ;

    public static Component getFor(KTStrings key, @NotNull ComponentLike ... args) {
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
                return Component.translatable("kamstweaks.graves.recovery", "You have 10 minutes to recover your grave. You can recover a grave repeatedly, but only %s can be recovered at a time.");
            }
            case GRAVE_NO_ID -> {
                return Component.translatable("kamstweaks.graves.no_id", "You don't have a grave with id %s.");
            }
            case GRAVE_DELETED -> {
                return Component.translatable("kamstweaks.graves.deleted", "Grave deleted successfully.");
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
            case NAME_AUTOMOD -> {
                return Component.translatable("kamstweaks.names.automod", "Nickname by %s was caught by the %s automod: %s", args);
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
        }
        Logger.handleException(new RuntimeException("Unknown translation string."));
        return Component.text("Unknown translation string, report this to kam ig");
    }
}
