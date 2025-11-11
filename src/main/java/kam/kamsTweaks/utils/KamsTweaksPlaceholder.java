package kam.kamsTweaks.utils;

import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.Names;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class KamsTweaksPlaceholder extends PlaceholderExpansion {

    @Override
    @NotNull
    public String getAuthor() {
        return "km7dev";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "kamstweaks";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @NotNull
    public String getVersion() {
        return KamsTweaks.getInstance().getPluginMeta().getVersion();
    }


    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equals("nickname")) {
            return LegacyComponentSerializer.legacySection().serialize(Names.instance.getRenderedName(player));
        }
        return null;
    }
}
