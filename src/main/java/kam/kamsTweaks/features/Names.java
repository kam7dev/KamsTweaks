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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

    public Names() {
        instance = this;
    }

    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("nicknames.enabled", "nicknames.enabled", true, "kamstweaks.configure"));
    }

    @Override
    public void shutdown() {

    }

    private static int score(String candidate, String target) {
        if (target.isEmpty()) return 0;
        if (candidate.startsWith(target)) return 0;
        if (candidate.contains(target)) return 1;
        return 2;
    }

    PlainTextComponentSerializer pt = PlainTextComponentSerializer.plainText();

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> nickCmd = Commands.literal("nick")
                .requires(source -> source.getSender().hasPermission("kamstweaks.names.nick"))
                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("nicknames.enabled", true)) {
                        sender.sendPlainMessage("Nicknames are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        String name = ctx.getArgument("name", String.class).replaceAll(INVIS_REGEX, "");
                        Component comp = MiniMessage.miniMessage().deserialize(name);
                        var plain = pt.serialize(comp);
                        if (plain.length() > 20) {
                            sender.sendPlainMessage("Nicknames cannot be longer than 20 characters.");
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

        LiteralArgumentBuilder<CommandSourceStack> colorCmd = Commands.literal("color")
                .requires(source -> source.getSender().hasPermission("kamstweaks.names.color"))
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
                            if (!KamsTweaks.getInstance().getConfig().getBoolean("name-colors.enabled", true)) {
                                sender.sendPlainMessage("Name colors are disabled.");
                                return Command.SINGLE_SUCCESS;
                            }
                            Entity executor = ctx.getSource().getExecutor();
                            if (executor instanceof Player player) {
                                String raw = ctx.getArgument("colors", String.class);
                                String[] parts = raw.split("\\s+");

                                List<TextColor> colors = new ArrayList<>();
                                List<String> invalid = new ArrayList<>();
                                for (String part : parts) {
                                    TextColor color = NamedTextColor.NAMES.value(part.toLowerCase(Locale.ROOT));
                                    if (color == null) color = TextColor.fromHexString(part.startsWith("#") ? part : "#" + part);
                                    if (color != null) colors.add(color);
                                    else {
                                        switch(part.toLowerCase()) {
                                            case "pride": {
                                                colors.add(NamedTextColor.RED);
                                                colors.add(NamedTextColor.GOLD);
                                                colors.add(NamedTextColor.YELLOW);
                                                colors.add(NamedTextColor.GREEN);
                                                colors.add(NamedTextColor.BLUE);
                                                colors.add(NamedTextColor.DARK_PURPLE);
                                                break;
                                            }
                                            case "lesbian": {
                                                colors.add(TextColor.fromHexString("#D52D00"));
                                                colors.add(TextColor.fromHexString("#EF7627"));
                                                colors.add(TextColor.fromHexString("#FF9A56"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#D162A4"));
                                                colors.add(TextColor.fromHexString("#B55690"));
                                                colors.add(TextColor.fromHexString("#A30262"));
                                                break;
                                            }
                                            case "gay": {
                                                colors.add(TextColor.fromHexString("#078D70"));
                                                colors.add(TextColor.fromHexString("#26CEAA"));
                                                colors.add(TextColor.fromHexString("#98E8C1"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#7BADE2"));
                                                colors.add(TextColor.fromHexString("#5049CC"));
                                                colors.add(TextColor.fromHexString("#3D1A78"));
                                                break;
                                            }
                                            case "bi", "bisexual": {
                                                colors.add(TextColor.fromHexString("#D60270"));
                                                colors.add(TextColor.fromHexString("#9B4F96"));
                                                colors.add(TextColor.fromHexString("#0038A8"));
                                                break;
                                            }
                                            case "trans": {
                                                colors.add(TextColor.fromHexString("#55CDFC"));
                                                colors.add(TextColor.fromHexString("#F7A8B8"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#F7A8B8"));
                                                colors.add(TextColor.fromHexString("#55CDFC"));
                                                break;
                                            }
                                            case "pan": {
                                                colors.add(TextColor.fromHexString("#FF1B8D"));
                                                colors.add(TextColor.fromHexString("#FFD800"));
                                                colors.add(TextColor.fromHexString("#1BB3FF"));
                                                break;
                                            }
                                            case "nonbinary", "nb": {
                                                colors.add(TextColor.fromHexString("#FFF430"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#9C59D1"));
                                                colors.add(TextColor.fromHexString("#000000"));
                                                break;
                                            }
                                            case "genderfluid": {
                                                colors.add(TextColor.fromHexString("#FF75A2"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#BE18D6"));
                                                colors.add(TextColor.fromHexString("#000000"));
                                                colors.add(TextColor.fromHexString("#333EBD"));
                                                break;
                                            }
                                            case "genderqueer": {
                                                colors.add(TextColor.fromHexString("#B57EDC"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#4A8123"));
                                                break;
                                            }
                                            case "intersex": {
                                                colors.add(TextColor.fromHexString("#FFD800"));
                                                colors.add(TextColor.fromHexString("#7902AA"));
                                                break;
                                            }
                                            case "asexual": {
                                                colors.add(TextColor.fromHexString("#000000"));
                                                colors.add(TextColor.fromHexString("#A3A3A3"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#800080"));
                                                break;
                                            }
                                            case "aromantic": {
                                                colors.add(TextColor.fromHexString("#3DA542"));
                                                colors.add(TextColor.fromHexString("#A7D379"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#A9A9A9"));
                                                colors.add(TextColor.fromHexString("#000000"));
                                                break;
                                            }
                                            case "demisexual": {
                                                colors.add(TextColor.fromHexString("#000000"));
                                                colors.add(TextColor.fromHexString("#A4A4A4"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#800080"));
                                                break;
                                            }
                                            case "demiboy": {
                                                colors.add(TextColor.fromHexString("#7F7F7F"));
                                                colors.add(TextColor.fromHexString("#9AD9EB"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#9AD9EB"));
                                                colors.add(TextColor.fromHexString("#7F7F7F"));
                                                break;
                                            }
                                            case "demigirl": {
                                                colors.add(TextColor.fromHexString("#7F7F7F"));
                                                colors.add(TextColor.fromHexString("#FBA9B0"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#FBA9B0"));
                                                colors.add(TextColor.fromHexString("#7F7F7F"));
                                                break;
                                            }
                                            case "polysexual": {
                                                colors.add(TextColor.fromHexString("#F61CB9"));
                                                colors.add(TextColor.fromHexString("#07D569"));
                                                colors.add(TextColor.fromHexString("#1C92F6"));
                                                break;
                                            }
                                            case "bigender": {
                                                colors.add(TextColor.fromHexString("#C479A2"));
                                                colors.add(TextColor.fromHexString("#EDA5CD"));
                                                colors.add(TextColor.fromHexString("#D6C7E8"));
                                                colors.add(TextColor.fromHexString("#FFFFFF"));
                                                colors.add(TextColor.fromHexString("#D6C7E8"));
                                                colors.add(TextColor.fromHexString("#9AC7E8"));
                                                colors.add(TextColor.fromHexString("#6D82D1"));
                                                break;
                                            }
                                            case "geode": {
                                                colors.add(TextColor.fromHexString("#F4D48E"));
                                                colors.add(TextColor.fromHexString("#F5AE7D"));
                                                colors.add(TextColor.fromHexString("#EC897C"));
                                                colors.add(TextColor.fromHexString("#D56985"));
                                                colors.add(TextColor.fromHexString("#AD5492"));
                                                colors.add(TextColor.fromHexString("#714A9A"));
                                                break;
                                            }
                                            default: {
                                                invalid.add(part);
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (!invalid.isEmpty()) {
                                    sender.sendPlainMessage("Invalid color(s): " + String.join(", ", invalid));
                                    return Command.SINGLE_SUCCESS;
                                }

                                Pair<String, List<TextColor>> info = data.getOrDefault(player.getUniqueId(), new Pair<>(player.getName(), new ArrayList<>()));
                                info.second = colors;
                                data.put(player.getUniqueId(), info);

                                Component comp = renderName(info);
                                player.displayName(comp);
                                player.playerListName(comp);

                                sender.sendMessage(Component.text("Your name is now ").append(comp).append(Component.text(".")));
                                return Command.SINGLE_SUCCESS;
                            }
                            sender.sendMessage("Only players can use /color.");
                            return Command.SINGLE_SUCCESS;
                        })
                );
        commands.registrar().register(colorCmd.build());

        LiteralArgumentBuilder<CommandSourceStack> whoIsCmd = Commands.literal("whois")
                .requires(source -> source.getSender().hasPermission("kamstweaks.names.nick"))
                .then(Commands.argument("who", StringArgumentType.greedyString()).suggests((ctx, builder) -> {
                    data.forEach((uuid, pair) -> {
                            if (pair.first.contains(builder.getRemaining().toLowerCase()) || builder.getRemaining().isEmpty())
                                builder.suggest(pair.first);
                    });
                    return builder.buildFuture();
                }).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.getInstance().getConfig().getBoolean("nicknames.enabled", true)) {
                        sender.sendPlainMessage("Nicknames are disabled.");
                        return Command.SINGLE_SUCCESS;
                    }
                    String name = ctx.getArgument("who", String.class);
                    AtomicReference<OfflinePlayer> who = new AtomicReference<>();
                    data.forEach((uuid, pair) -> {
                        if (Objects.equals(pair.first, name)) {
                            who.set(Bukkit.getServer().getOfflinePlayer(uuid));
                        }
                    });
                    if (who.get() == null) {
                        sender.sendMessage("No one is using the nickname " + name + ".");
                    } else {
                        sender.sendMessage(name + " is " + who.get().getName() + ".");
                    }
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(whoIsCmd.build());
    }

    @Override
    public void loadData() {
        data.clear();
        FileConfiguration config = KamsTweaks.getInstance().getDataConfig();
        if (config.contains("names")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("names")).getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(key);

                    String nick = config.getString("names." + key + ".nick", Bukkit.getServer().getOfflinePlayer(owner).getName());

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

                    if ((nick == null || nick.isBlank()) && colors.isEmpty()) continue;

                    Pair<String, List<TextColor>> info = new Pair<>(
                            (nick == null || nick.isBlank()) ? Bukkit.getServer().getOfflinePlayer(owner).getName() : nick,
                            colors
                    );
                    data.put(owner, info);
                } catch (Exception e) {
                    Logger.excs.add(e);
                    Logger.warn("Failed to load name for " + key + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.getInstance().getDataConfig();
        config.set("names", null);

        data.forEach((uuid, pair) -> {
            String path = "names." + uuid;
            if (pair.first != null) config.set(path + ".nick", pair.first);
            if (pair.second != null && !pair.second.isEmpty()) {
                if (pair.second.size() == 1) {
                    TextColor c = pair.second.getFirst();
                    if (c instanceof NamedTextColor named) {
                        config.set(path + ".color", named.toString());
                    } else {
                        config.set(path + ".color", String.format("#%06X", c.value()));
                    }
                } else {
                    List<String> hexList = new ArrayList<>();
                    for (TextColor c : pair.second) {
                        hexList.add(String.format("#%06X", c.value()));
                    }
                    config.set(path + ".gradient", hexList);
                }
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        Pair<String, List<TextColor>> info =
                data.getOrDefault(player.getUniqueId(), new Pair<>(player.getName(), new ArrayList<>()));
        Component comp = renderName(info);
        player.displayName(comp);
        player.playerListName(comp);
    }

    public Component getRenderedName(OfflinePlayer player) {
        return renderName(data.getOrDefault(player.getUniqueId(), new Pair<>(player.getName(), new ArrayList<>())));
    }
}