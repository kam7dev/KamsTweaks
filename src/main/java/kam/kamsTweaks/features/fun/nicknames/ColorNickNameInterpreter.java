package kam.kamsTweaks.features.fun.nicknames;

import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ColorNickNameInterpreter extends Names.NameInterpreter{
    @Override
    public Component interpret(@NonNull ConfigurationSection sect, @NonNull OfflinePlayer who) {
        var pName = who.getName();
        if (pName == null) pName = "Unknown";
        String nick = sect.getString("nick", pName);

        List<TextColor> colors = new ArrayList<>();
        List<String> gradientList = sect.getStringList("gradient");
        if (!gradientList.isEmpty()) {
            for (String s : gradientList) {
                if (!s.startsWith("#")) s = "#" + s;
                TextColor col = TextColor.fromHexString(s);
                if (col != null) colors.add(col);
            }
        } else {
            String colorStr = sect.getString("color");
            if (colorStr != null) {
                TextColor color = NamedTextColor.NAMES.value(colorStr.toLowerCase(Locale.ROOT));
                if (color == null) {
                    if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
                    color = TextColor.fromHexString(colorStr);
                }
                if (color != null) colors.add(color);
            }
        }

        if ((nick.isBlank()) && colors.isEmpty()) return null;

        Pair<String, List<TextColor>> info = new Pair<>(
                (nick.isBlank()) ? pName : nick,
                colors
        );
        return Component.text().append(renderName(info)).build().hoverEvent(HoverEvent.showText(Component.text(pName)));
    }

    @Override
    public void save(@NonNull ConfigurationSection sect, @NonNull Component name) {
//        sect.set("mm", mm.serialize(name));
        // TODO
    }

    @Override
    public @NonNull String getTypeId() {
        return "colornick";
    }

    private static List<String> splitGraphemes(String input) {
        BreakIterator iter = BreakIterator.getCharacterInstance(Locale.ROOT);
        iter.setText(input);
        List<String> result = new ArrayList<>();
        int start = iter.first();
        for (int end = iter.next(); end != BreakIterator.DONE; start = end, end = iter.next()) {
            result.add(input.substring(start, end));
        }
        return result;
    }

    public static Component gradientName(String name, List<TextColor> colors) {
        Component res = Component.empty();
        if (name.isEmpty() || colors.isEmpty()) {
            return Component.text(name);
        }

        List<String> graphemes = splitGraphemes(name);

        int totalSteps = graphemes.size() - 1;
        int totalColors = colors.size() - 1;

        for (int i = 0; i < graphemes.size(); i++) {
            double progress = totalSteps == 0 ? 0 : (double) i / totalSteps;
            TextColor color = getTextColor(colors, progress, totalColors);
            res = res.append(Component.text(graphemes.get(i)).color(color));
        }
        return res;
    }

    private static TextColor getTextColor(List<TextColor> colors, double progress, int totalColors) {
        double scaled = progress * totalColors;
        int idx = (int) Math.floor(scaled);
        double blend = scaled - idx;

        TextColor a = colors.get(idx);
        TextColor b = colors.get(Math.min(idx + 1, totalColors));

        int r = (int) Math.round(a.red() * (1 - blend) + b.red() * blend);
        int g = (int) Math.round(a.green() * (1 - blend) + b.green() * blend);
        int bCol = (int) Math.round(a.blue() * (1 - blend) + b.blue() * blend);

        return TextColor.color(r, g, bCol);
    }

    public static Component renderName(Pair<String, List<TextColor>> info) {
        if (info.second == null || info.second.isEmpty()) {
            return Component.text(info.first);
        }
        if (info.second.size() == 1) {
            return Component.text(info.first).color(info.second.getFirst());
        }
        return gradientName(info.first, info.second);
    }
}
