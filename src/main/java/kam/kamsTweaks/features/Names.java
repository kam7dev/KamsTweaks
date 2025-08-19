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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Names implements Listener {
    Map<UUID, Pair<String, TextColor>> data = new HashMap<>();

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

                    String colorStr = config.getString("names." + key + ".color");
                    TextColor color = null;
                    if (colorStr != null) {
                        color = NamedTextColor.NAMES.value(colorStr.toLowerCase(Locale.ROOT));
                        if (color == null) {
                            if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
                            color = TextColor.fromHexString(colorStr);
                        }
                    }
                    if (nick.isBlank() && color == null) continue;

                    Pair<String, TextColor> info = new Pair<>(nick.isBlank() ? Bukkit.getServer().getOfflinePlayer(owner).getName() : nick, color);
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
            if (pair.second != null) {
                if (pair.second instanceof NamedTextColor named) {
                    config.set(path + ".color", named.toString());
                } else {
                    config.set(path + ".color", String.format("#%06X", pair.second.value()));
                }
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!data.containsKey(event.getPlayer().getUniqueId())) return;
        var info = data.get(event.getPlayer().getUniqueId());
        Component comp = Component.text(info.first);
        if (info.second != null) {
            comp = comp.color(info.second);
        }
        event.getPlayer().displayName(comp);
    }

    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> nickCmd = Commands.literal("nick").requires(source -> source.getSender().hasPermission("kamstweaks.names.nick")).then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("nicknames.enabled", true)) {
                sender.sendPlainMessage("Nicknames are disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                Pair<String, TextColor> info;
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
                    info = new Pair<>(name, null);
                }
                data.put(player.getUniqueId(), info);

                Component comp = Component.text(info.first);
                if (info.second != null) {
                    comp = comp.color(info.second);
                }
                player.displayName(comp);
                sender.sendMessage(Component.text("Your nickname is now ").append(Component.text(info.first)).append(Component.text(".")));
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /nick.");
            return Command.SINGLE_SUCCESS;
        }));
        commands.registrar().register(nickCmd.build());

        LiteralArgumentBuilder<CommandSourceStack> colorCmd = Commands.literal("color").requires(source -> source.getSender().hasPermission("kamstweaks.names.color")).then(Commands.argument("color", StringArgumentType.string()).suggests((ctx, builder) -> {
            for (String color : NamedTextColor.NAMES.keys()) {
                builder.suggest(color.toLowerCase());
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("name-colors.enabled", true)) {
                sender.sendPlainMessage("Name colors are disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                Pair<String, TextColor> info;
                String colorStr = ctx.getArgument("color", String.class).toLowerCase(Locale.ROOT);
                TextColor color = NamedTextColor.NAMES.value(colorStr);
                if (color == null) {
                    if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
                    color = TextColor.fromHexString(colorStr);
                }
                if (color == null) {
                    sender.sendMessage(Component.text(colorStr).append(Component.text(" is an invalid color.")));
                    return Command.SINGLE_SUCCESS;
                }

                if (data.containsKey(player.getUniqueId())) {
                    info = data.get(player.getUniqueId());
                    info.second = color;
                } else {
                    info = new Pair<>(player.getName(), color);
                }
                data.put(player.getUniqueId(), info);

                Component comp = Component.text(info.first);
                if (info.second != null) {
                    comp = comp.color(info.second);
                }
                player.displayName(comp);
                sender.sendMessage(Component.text("Your name is now ").append(Component.text(info.first).color(info.second)).append(Component.text(".")));
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /color.");
            return Command.SINGLE_SUCCESS;
        }));
        commands.registrar().register(colorCmd.build());

        LiteralArgumentBuilder<CommandSourceStack> whoIsCmd = Commands.literal("whois").requires(source -> source.getSender().hasPermission("kamstweaks.names.nick")).then(Commands.argument("who", StringArgumentType.word()).suggests((ctx, builder) -> {
            data.forEach((uuid, pair) -> {
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
}
