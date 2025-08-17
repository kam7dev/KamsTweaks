package kam.kamsTweaks.features;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Names implements Listener {
    Map<UUID, Pair<String, TextColor>> data = new HashMap<>();

    public void init() {

    }

    public void loadNames() {
        data.clear();
        FileConfiguration config = KamsTweaks.getInstance().getGeneralConfig();
        if (config.contains("names")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("names")).getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(key);
                    String nick = config.getString("names." + key + ".nick", Bukkit.getServer().getOfflinePlayer(owner).getName()).replaceAll("[^a-zA-Z0-9\\-_. ,()\\[\\]{}:;\"'!?+&$~`/]", "");;
		            if (nick.isBlank()) continue;
                    String colorStr = config.getString("names." + key + ".color");
                    TextColor color = null;
                    if (colorStr != null) {
                        color = NamedTextColor.NAMES.value(colorStr);
                        if (color == null) {
                            if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
                            color = TextColor.fromHexString(colorStr);
                        }
                    }
                    if (color == null) continue;
                    Pair<String, TextColor> info = new Pair<>(nick, color);
                    data.put(owner, info);
                } catch (Exception e) {
                    Logger.warn(e.getMessage());
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
            if (pair.second != null) config.set(path + ".color", pair.second.toString());
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
        LiteralArgumentBuilder<CommandSourceStack> nickCmd = Commands.literal("nick")
                .requires(source -> source.getSender().hasPermission("kamstweaks.names.nick"))
                .then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("nicknames", true)) {
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

        LiteralArgumentBuilder<CommandSourceStack> colorCmd = Commands.literal("color")
                .requires(source -> source.getSender().hasPermission("kamstweaks.names.color"))
                .then(Commands.argument("color", StringArgumentType.string()).suggests((ctx, builder) -> {
            for (String color : NamedTextColor.NAMES.keys()) {
                builder.suggest(color.toLowerCase());
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            CommandSender sender = ctx.getSource().getSender();
            if (!KamsTweaks.getInstance().getConfig().getBoolean("name-colors", true)) {
                sender.sendPlainMessage("Name colors are disabled.");
                return Command.SINGLE_SUCCESS;
            }
            Entity executor = ctx.getSource().getExecutor();
            if (executor instanceof Player player) {
                Pair<String, TextColor> info;
                String colorStr = ctx.getArgument("color", String.class).toLowerCase();
                TextColor color = NamedTextColor.NAMES.value(colorStr);
                if (color == null) {
                    if (!colorStr.startsWith("#")) colorStr = "#" + colorStr;
                    color = TextColor.fromHexString(colorStr);
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
                } else {
                    sender.sendMessage(Component.text(colorStr).append(Component.text(" is an invalid color.")));
                    return Command.SINGLE_SUCCESS;
                }
                player.displayName(comp);
                sender.sendMessage(Component.text("Your name is now ").append(Component.text(colorStr).color(info.second)).append(Component.text(".")));
                return Command.SINGLE_SUCCESS;
            }
            sender.sendMessage("Only players can use /color.");
            return Command.SINGLE_SUCCESS;
        }));
        commands.registrar().register(colorCmd.build());
    }
}
