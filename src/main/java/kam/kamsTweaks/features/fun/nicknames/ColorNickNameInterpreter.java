package kam.kamsTweaks.features.fun.nicknames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.fun.nicknames.Names.NameData;
import kam.kamsTweaks.features.moderation.ChatFilter;
import kam.kamsTweaks.managers.KTPerms;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.text.BreakIterator;
import java.util.*;

public class ColorNickNameInterpreter extends Names.NameInterpreter {
    Map<String, List<TextColor>> combos = new HashMap<>();

    final Map<String, List<TextColor>> flags;

    @SuppressWarnings({"unchecked"})
    ColorNickNameInterpreter() {
        Map<String, List<TextColor>> tmp;
        try {
            var clazz = Class.forName("net.kyori.adventure.text.minimessage.tag.standard.PrideTag");
            var field = clazz.getDeclaredField("FLAGS");
            field.setAccessible(true);
            tmp = (Map<String, List<TextColor>>) field.get(null);
        } catch (Exception e) {
            Logger.handleException("Failed to extract flags from adventure:", e);
            tmp = new HashMap<>();
        }
        flags = tmp;
        loadCombos();
    }

    public void loadCombos() {
        combos.clear();
        combos.putAll(flags);
        var sect = KamsTweaks.get().getConfig().getConfigurationSection("nicknames.colors.combos");
        if (sect == null) return;
        for (var key : sect.getKeys(false)) {
            List<TextColor> l = new ArrayList<>();
            for (var color : sect.getStringList(key)) {
                var ntc = NamedTextColor.NAMES.value(color);
                if (ntc != null) {
                    l.add(ntc);
                    continue;
                }
                var col = TextColor.fromHexString(color);
                if (col != null) {
                    l.add(col);
                    continue;
                }
                Logger.error("Combo {} has illegal color: ");
            }
            if (l.isEmpty()) continue;
            combos.put(key, l);
        }
    }

    @Override
    public NameData interpret(@NonNull ConfigurationSection sect, @NonNull OfflinePlayer who) {
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
        return new ColorNickNameData(getTypeId(), gradientName(info.first, info.second).hoverEvent(HoverEvent.showText(Component.text(pName))), info.first, info.second);
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> nickCmd = Commands.literal("nick")
                .requires(source -> KTPerms.hasPermission(source, KTPerms.NICKNAME))
                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!Config.getBool("nicknames.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.NAMES)));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        String name = ctx.getArgument("name", String.class).replaceAll(Names.INVISIBLE_REGEX, "");
                        var ml = Config.getInt("nicknames.max-length", 30);
                        if (name.length() > ml) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.NAME_LENGTH, Component.text(ml)));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (name.isBlank()) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.NAME_EMPTY));
                            return Command.SINGLE_SUCCESS;
                        }
                        var res = ChatFilter.instance.isFiltered(name);
                        if (res.first) {
                            ChatFilter.warnStaff(KTStrings.getFor(KTStrings.AUTOMOD_NAME, Component.text(sender.getName()), Component.text(res.second.name), Component.text(name)));
                            sender.sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (Names.whois(name, List.of(player.getUniqueId())) != null) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.NAME_IN_USE));
                            return Command.SINGLE_SUCCESS;
                        }
                        var colors = getNameColors(player.getUniqueId());
                        var comp = renderName(new Pair<>(name, colors)).hoverEvent(HoverEvent.showText(Component.text(player.getName())));
                        Names.setName(player, new ColorNickNameData(getTypeId(), comp, name, colors));
                        sender.sendMessage(KTStrings.getFor(KTStrings.NAME_SET, comp));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/nick")));
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(nickCmd.build());

        LiteralArgumentBuilder<CommandSourceStack> colorCmd = Commands.literal("color")
                .requires(source -> KTPerms.hasPermission(source, KTPerms.NICKNAME_COLORS))
                .then(Commands.argument("colors", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            String input = builder.getInput();
                            List<String> parts = new ArrayList<>(List.of(input.split(" ")));
                            parts.removeFirst();

                            String target;
                            if (!input.endsWith(" ") && !parts.isEmpty()) {
                                target = parts.removeLast().toLowerCase();
                            } else {
                                target = "";
                            }

                            String prefix = String.join(" ", parts);
                            if (!prefix.isEmpty()) prefix += " ";

                            List<String> colors = new ArrayList<>(NamedTextColor.NAMES.keys());
                            colors.sort(Comparator
                                    .comparingInt((String c) -> score(c.toLowerCase(), target))
                                    .thenComparing(String::compareTo)
                            );

                            for (String color : colors) {
                                if (target.isEmpty() || color.toLowerCase().contains(target)) {
                                    builder.suggest(prefix + color.toLowerCase());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            if (!Config.getBool("nicknames.enabled", true)) {
                                sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.NAMES)));
                                return Command.SINGLE_SUCCESS;
                            }
                            if (!Config.getBool("nicknames.color.enabled", true)) {
                                sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.NAME_COLORS)));
                                    return Command.SINGLE_SUCCESS;
                                }
                                Entity executor = ctx.getSource().getExecutor();
                                if (executor instanceof Player player) {
                                    String raw = ctx.getArgument("colors", String.class);
                                    String[] parts = raw.split("\\s+");

                                List<TextColor> colors = new ArrayList<>();
                                List<String> invalid = new ArrayList<>();
                                for (String part : parts) {
                                    var tlc = part.toLowerCase(Locale.ROOT);
                                    TextColor color = NamedTextColor.NAMES.value(tlc);
                                    if (color == null) color = TextColor.fromHexString(part.startsWith("#") ? part : "#" + part);
                                    if (color != null) colors.add(color);
                                    else {
                                        if (combos.containsKey(tlc)) {
                                            colors.addAll(combos.get(tlc));
                                        } else {
                                            invalid.add(part);
                                        }
                                    }
                                }

                                if (!invalid.isEmpty()) {
                                    sender.sendPlainMessage("Invalid color(s): " + String.join(", ", invalid));
                                    return Command.SINGLE_SUCCESS;
                                }

                                var name = getNameText(player.getUniqueId());
                                var comp = renderName(new Pair<>(name == null ? player.getName() : name, colors)).hoverEvent(HoverEvent.showText(Component.text(player.getName())));
                                Names.setName(player, new ColorNickNameData(getTypeId(), comp, name, colors));
                                sender.sendMessage(KTStrings.getFor(KTStrings.NAME_SET, comp));
                                return Command.SINGLE_SUCCESS;
                            }
                            sender.sendMessage("Only players can use /color.");
                            return Command.SINGLE_SUCCESS;
                        })
                );
        commands.registrar().register(colorCmd.build());
    }

    List<TextColor> getNameColors(UUID who) {
        var d = Names.get().data.get(who);
        if (d == null) return null;
        if (!Objects.equals(d.type(), getTypeId())) return null;
        return ((ColorNickNameData) d).colors;
    }

    String getNameText(UUID who) {
        var d = Names.get().data.get(who);
        if (d == null) return null;
        if (!Objects.equals(d.type(), getTypeId())) return null;
        return ((ColorNickNameData) d).baseName;
    }

    @Override
    public void save(@NonNull ConfigurationSection sect, @NonNull NameData nd) {
        if (!(nd instanceof ColorNickNameData data)) {
            Logger.error("Can't parse regular Name Data as a ColorNick name: {}", nd.name());
            return;
        }
        if (data.baseName != null) sect.set("nick", data.baseName);
        if (data.colors != null && !data.colors.isEmpty()) {
            if (data.colors.size() == 1) {
                TextColor c = data.colors.getFirst();
                if (c instanceof NamedTextColor named) {
                    sect.set("color", named.toString());
                } else {
                    sect.set("color", String.format("#%06X", c.value()));
                }
            } else {
                List<String> hexList = new ArrayList<>();
                for (TextColor c : data.colors) {
                    hexList.add(String.format("#%06X", c.value()));
                }
                sect.set("gradient", hexList);
            }
        }
    }

    public static class ColorNickNameData extends NameData {
        final String baseName;
        final List<TextColor> colors;

        public ColorNickNameData(String type, Component name, String baseName, List<TextColor> colors) {
            super(type, name);
            this.baseName = baseName;
            this.colors = colors;
        }
    }

    @Override
    public @NonNull String getTypeId() {
        return "colornick";
    }

    private static int score(String candidate, String target) {
        if (target.isEmpty()) return 0;
        if (candidate.startsWith(target)) return 0;
        if (candidate.contains(target)) return 1;
        return 2;
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
