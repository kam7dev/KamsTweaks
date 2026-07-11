package kam.kamsTweaks.features.fun.nicknames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.*;
import kam.kamsTweaks.managers.KTStrings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Names extends Feature {
    Map<UUID, Component> data = new HashMap<>();
    public static final String INVISIBLE_REGEX = "[\\u200B-\\u200F\\uFEFF\\u2060]";

    Map<String, NameInterpreter> interpreters = new HashMap<>();

    static Names instance;

    public static final PlainTextComponentSerializer pt = PlainTextComponentSerializer.plainText();
    public static final JSONComponentSerializer json = JSONComponentSerializer.json();

    public static Names get() {
        return instance;
    }

    public Names() {
        instance = this;
    }

    @Override
    public void setup() {
        addInterpreter(new ColorNickNameInterpreter());
        addInterpreter(new MiniMessageInterpreter());

        Config.addConfig(new Config.BoolConfigOption("nicknames.enabled", "nicknames.enabled", true, "kamstweaks.configure"));
    }

    void addInterpreter(NameInterpreter interpreter) {
        interpreters.put(interpreter.getTypeId(), interpreter);
    }

    public void setName(Player player, Component comp) {
        data.put(player.getUniqueId(), comp);
        player.displayName(comp);
        player.playerListName(comp);
    }

    public static OfflinePlayer whois(String name) {
        return whois(name, List.of());
    }

    public static OfflinePlayer whois(String name, List<UUID> ignore) {
        AtomicReference<OfflinePlayer> who = new AtomicReference<>();
        instance.data.forEach((uuid, pair) -> {
            if (ignore.contains(uuid)) return;
            if (Objects.equals(pt.serialize(pair), name)) {
                who.set(Bukkit.getServer().getOfflinePlayer(uuid));
            }
        });
        return who.get();
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
                LiteralArgumentBuilder<CommandSourceStack> whoIsCmd = Commands.literal("whois")
                .requires(source -> source.getSender().hasPermission("kamstweaks.names.nick"))
                .then(Commands.argument("who", StringArgumentType.greedyString()).suggests((ctx, builder) -> {
                    data.forEach((uuid, comp) -> {
                            var ds = pt.serialize(comp);
                            if (ds.toLowerCase().contains(builder.getRemaining().toLowerCase()) || builder.getRemaining().isEmpty())
                                builder.suggest(ds);
                    });
                    return builder.buildFuture();
                }).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("nicknames.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.NAMES)));
                        return Command.SINGLE_SUCCESS;
                    }
                    String name = ctx.getArgument("who", String.class);
                    var who = whois(name);
                    if (who == null) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.NAME_WHOIS_NONE, Component.text(name)));
                    } else {
                        sender.sendMessage(KTStrings.getFor(KTStrings.NAME_WHOIS, getName(who), Component.text(Objects.requireNonNullElse(who.getName(), "Unknown"))));
                    }
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(whoIsCmd.build());
    }

    @Override
    public void loadData() {
        data.clear();
    }

    @Override
    public void saveData() {

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!KamsTweaks.get().getConfig().getBoolean("nicknames.enabled", true)) return;
        var player = event.getPlayer();
        var comp = getName(player);
        player.displayName(comp);
        player.playerListName(comp);
    }

    public static Component getName(@NotNull OfflinePlayer player) {
        var pName = player.getName();
        if (pName == null) pName = "Unknown";
        if (!KamsTweaks.get().getConfig().getBoolean("nicknames.enabled", true)) return Component.text(pName);
        return instance.data.getOrDefault(player.getUniqueId(), Component.text(pName));
    }

    public static Component getName(@NotNull OfflinePlayer player, boolean appendage) {
        if (appendage && instance.data.containsKey(player.getUniqueId())) return getName(player).append(Component.text(" ("), Component.text(Objects.requireNonNullElse(player.getName(), "Unknown")), Component.text(")"));
        return getName(player);
    }

    public static Component getEName(@NotNull Entity entity) {
        if (entity instanceof Player player) return getName(player);
        return entity.name().append(Component.text(" ("), Component.translatable(entity.getType().translationKey()), Component.text(")"));
    }

    public static abstract class NameInterpreter {
        public abstract @Nullable Component interpret(@NonNull ConfigurationSection sect, @NonNull OfflinePlayer who);
        public abstract void save(@NonNull ConfigurationSection sect, @NonNull Component name);
        public abstract @NonNull String getTypeId();
    }
}
