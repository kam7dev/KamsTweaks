package kam.kamsTweaks.features;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Names implements Listener {
    Map<UUID, Pair<String, List<TextColor>>> data = new HashMap<>();

    public Component gradientName(String name, List<TextColor> colors) {
        Component res = Component.empty();
        if (name.isEmpty() || colors.isEmpty()) {
            return Component.text(name);
        }
        int totalSteps = name.length() - 1;
        int totalColors = colors.size() - 1;
        for (int i = 0; i < name.length(); i++) {
            double progress = (double) i / totalSteps;
            TextColor color = getTextColor(colors, progress, totalColors);
            res = res.append(Component.text(name.charAt(i)).color(color));
        }
        return res;
    }

    private static @NotNull TextColor getTextColor(List<TextColor> colors, double progress, int totalColors) {
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

    private Component renderName(Pair<String, List<TextColor>> info) {
        if (info.second == null || info.second.isEmpty()) {
            return Component.text(info.first);
        }
        if (info.second.size() == 1) {
            return Component.text(info.first).color(info.second.getFirst());
        }
        return gradientName(info.first, info.second);
    }

    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("nicknames.enabled", "nicknames.enabled", true, "kamstweaks.configure"));
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("name-colors.enabled", "name-colors.enabled", true, "kamstweaks.configure"));
    }

    public void loadNames() {
        data.clear();
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
        if (config.contains("names")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("names")).getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(key);

                    String rawNick = config.getString("names." + key + ".nick", Bukkit.getServer().getOfflinePlayer(owner).getName());
                    String nick = (rawNick == null ? "" : rawNick.replaceAll("[^a-zA-Z0-9\\-_. ,()\\[\\]{}:;\"'!?+&$~`/]", ""));

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

                    if (nick.isBlank() && colors.isEmpty()) continue;

                    Pair<String, List<TextColor>> info = new Pair<>(
                            nick.isBlank() ? Bukkit.getServer().getOfflinePlayer(owner).getName() : nick,
                            colors
                    );
                    data.put(owner, info);
                } catch (Exception e) {
                    Logger.warn("Failed to load name for " + key + ": " + e.getMessage());
                }
            }
        }
    }

    public void saveNames() {
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
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
        if (!data.containsKey(event.getPlayer().getUniqueId())) return;
        var info = data.get(event.getPlayer().getUniqueId());
        Component comp = renderName(info);
        event.getPlayer().displayName(comp);
        event.getPlayer().playerListName(comp);
    }

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
                        Pair<String, List<TextColor>> info;
                        String name = ctx.getArgument("name", String.class);
                        if (name.length() > 20) {
                            sender.sendPlainMessage("Nicknames cannot be longer than 20 characters.");
                            return Command.SINGLE_SUCCESS;
                        }
                        if (name.isBlank()) {
                            sender.sendPlainMessage("Nicknames cannot be empty.");
                            return Command.SINGLE_SUCCESS;
                        }
                        if (!name.matches("[a-zA-Z0-9\\-_. ,()\\[\\]{}:;\"'!?+&$~`/]*")) {
                            sender.sendPlainMessage("Nicknames can only contain letters, numbers, spaces, and some symbols.");
                            return Command.SINGLE_SUCCESS;
                        }
                        AtomicBoolean ret = new AtomicBoolean(false);
                        data.forEach((uuid, pair) -> {
                            if (!uuid.equals(player.getUniqueId()) && Objects.equals(pair.first, name)) {
                                sender.sendPlainMessage("Someone already has that nickname.");
                                ret.set(true);
                            }
                        });
                        if (ret.get()) return Command.SINGLE_SUCCESS;

                        if (data.containsKey(player.getUniqueId())) {
                            info = data.get(player.getUniqueId());
                            info.first = name;
                        } else {
                            info = new Pair<>(name, new ArrayList<>());
                        }
                        data.put(player.getUniqueId(), info);

                        Component comp = renderName(info);
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
                            for (String color : NamedTextColor.NAMES.keys()) {
                                builder.suggest(color.toLowerCase());
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
                                        Logger.info(part);
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

                                Pair<String, List<TextColor>> info =
                                        data.getOrDefault(player.getUniqueId(), new Pair<>(player.getName(), new ArrayList<>()));
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
                    data.forEach((uuid, pair) -> builder.suggest(pair.first));
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
}
