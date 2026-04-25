package kam.kamsTweaks.features;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.*;
import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.text.BreakIterator;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Names extends Feature {
    Map<UUID, Component> data = new HashMap<>();
    public static final String INVIS_REGEX = "[\\u200B-\\u200F\\uFEFF\\u2060]";

    public static Names instance;

    PlainTextComponentSerializer pt = PlainTextComponentSerializer.plainText();
    MiniMessage mm = MiniMessage.builder().tags(TagResolver.builder()
            .resolver(StandardTags.color())
            .resolver(StandardTags.decorations())
            .resolver(StandardTags.shadowColor())
            .resolver(StandardTags.reset())
            .resolver(StandardTags.gradient())
            .resolver(StandardTags.rainbow())
            .resolver(StandardTags.pride())
            .build()).build();

    public Names() {
        instance = this;
    }

    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("nicknames.enabled", "nicknames.enabled", true, "kamstweaks.configure"));
    }

    public static class Compat {
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

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> nickCmd = Commands.literal("nick")
                .requires(source -> source.getSender().hasPermission("kamstweaks.names.nick"))
                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("nicknames.enabled", true)) {
                        sender.sendPlainMessage("Nicknames are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        String name = ctx.getArgument("name", String.class).replaceAll(INVIS_REGEX, "");
                        Component comp = Component.text().append(mm.deserialize(name)).build().hoverEvent(HoverEvent.showText(Component.text(player.getName())));
                        var plain = pt.serialize(comp);
                        if (plain.length() > 30) {
                            sender.sendPlainMessage("Nicknames cannot be longer than 30 characters.");
                            return Command.SINGLE_SUCCESS;
                        }
                        if (plain.isBlank()) {
                            sender.sendPlainMessage("Nicknames cannot be empty.");
                            return Command.SINGLE_SUCCESS;
                        }
                        var res = ChatFilter.instance.isFiltered(plain);
                        if (res.first) {
                            ChatFilter.warnStaff("Nickname by " + sender.getName() + " was caught by the " + res.second.name + " automod: " + plain);
                            sender.sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        AtomicBoolean ret = new AtomicBoolean(false);
                        data.forEach((uuid, other) -> {
                            if (!uuid.equals(player.getUniqueId()) && Objects.equals(plain, pt.serialize(other))) {
                                sender.sendPlainMessage("Someone already has that nickname.");
                                ret.set(true);
                            }
                        });
                        if (ret.get()) return Command.SINGLE_SUCCESS;
                        data.put(player.getUniqueId(), comp);
                        player.displayName(comp);
                        player.playerListName(comp);
                        sender.sendMessage(Component.text("Your nickname is now ").append(comp).append(Component.text(".")));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage("Only players can use /nick.");
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(nickCmd.build());

        LiteralArgumentBuilder<CommandSourceStack> whoIsCmd = Commands.literal("whois")
                .requires(source -> source.getSender().hasPermission("kamstweaks.names.nick"))
                .then(Commands.argument("who", StringArgumentType.greedyString()).suggests((ctx, builder) -> {
                    data.forEach((uuid, comp) -> {
                            var ds = pt.serialize(comp);
                            if (ds.contains(builder.getRemaining().toLowerCase()) || builder.getRemaining().isEmpty())
                                builder.suggest(ds);
                    });
                    return builder.buildFuture();
                }).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("nicknames.enabled", true)) {
                        sender.sendPlainMessage("Nicknames are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    String name = ctx.getArgument("who", String.class);
                    AtomicReference<OfflinePlayer> who = new AtomicReference<>();
                    data.forEach((uuid, pair) -> {
                        if (Objects.equals(pt.serialize(pair), name)) {
                            who.set(Bukkit.getServer().getOfflinePlayer(uuid));
                        }
                    });
                    if (who.get() == null) {
                        sender.sendMessage("No one is using the nickname " + name + ".");
                    } else {
                        sender.sendMessage(getRenderedName(who.get()) + " is " + who.get().getName() + ".");
                    }
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(whoIsCmd.build());
    }

    @Override
    public void loadData() {
        data.clear();
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        if (config.contains("names-v2")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("names-v2")).getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(key);
                    var pName = Bukkit.getServer().getOfflinePlayer(owner).getName();
                    if (pName == null) pName = "Unknown";
                    Component nick = Component.text().append(mm.deserialize(Objects.requireNonNull(config.getString("names-v2." + key, Bukkit.getServer().getOfflinePlayer(owner).getName())))).build().hoverEvent(HoverEvent.showText(Component.text(pName)));
                    if ((pt.serialize(nick).isBlank())) continue;
                    data.put(owner, nick);
                } catch (Exception e) {
                    Logger.error("Failed to load name for " + key + ". Exception printed below.");
                    Logger.handleException(e);
                }
            }
        } else if (config.contains("names")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("names")).getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(key);

                    var pName = Bukkit.getServer().getOfflinePlayer(owner).getName();
                    if (pName == null) pName = "Unknown";
                    String nick = config.getString("names." + key + ".nick", pName);

                    List<TextColor> colors = new ArrayList<>();
                    List<String> gradientList = config.getStringList("names." + key + ".gradient");
                    if (!gradientList.isEmpty()) {
                        for (String s : gradientList) {
                            if (!s.startsWith("#")) s = "#" + s;
                            TextColor col = TextColor.fromHexString(s);
                            if (col != null) colors.add(col);
                        }
                    } else {
                        String colorStr = config.getString("names." + key + ".color");
                        if (colorStr != null) {
                            TextColor color = NamedTextColor.NAMES.value(colorStr.toLowerCase(Locale.ROOT));
                            if (color == null) {
                                if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
                                color = TextColor.fromHexString(colorStr);
                            }
                            if (color != null) colors.add(color);
                        }
                    }

                    if ((nick.isBlank()) && colors.isEmpty()) continue;

                    Pair<String, List<TextColor>> info = new Pair<>(
                            (nick.isBlank()) ? pName : nick,
                            colors
                    );
                    data.put(owner, Component.text().append(Compat.renderName(info)).build().hoverEvent(HoverEvent.showText(Component.text(pName))));
                } catch (Exception e) {
                    Logger.warn("Failed to load name for " + key + ". Exception printed below.");
                    Logger.handleException(e);
                }
            }
        }
    }

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        config.set("names-v2", null);

        data.forEach((uuid, comp) -> {
            if (comp != null) config.set("names-v2." + uuid, mm.serialize(comp));
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!KamsTweaks.get().getConfig().getBoolean("nicknames.enabled", true)) return;
        var player = event.getPlayer();
        var comp = getRenderedName(player);
        player.displayName(comp);
        player.playerListName(comp);
    }

    public Component getRenderedName(@NotNull OfflinePlayer player) {
        var pName = player.getName();
        if (pName == null) pName = "Unknown";
        if (!KamsTweaks.get().getConfig().getBoolean("nicknames.enabled", true)) return Component.text(pName);
        return data.getOrDefault(player.getUniqueId(), Component.text(pName));
    }
}
