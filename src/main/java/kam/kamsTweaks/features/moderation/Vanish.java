package kam.kamsTweaks.features.moderation;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.ext.SRVHelper;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.Config;
import kam.kamsTweaks.utils.UserDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

@SuppressWarnings("deprecation")
public class Vanish extends Feature {

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("vanish").requires(source -> source.getSender().isOp())
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    if (!(ctx.getSource().getExecutor() instanceof Player plr)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/vanish")).color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (!Config.getBool("staff.vanish.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/vanish")).color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    setVanished(plr, sender, true);
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("targets", ArgumentTypes.playerProfiles()).requires(source -> source.getSender().isOp())
                        .executes(ctx -> {
                            var targets = ctx.getArgument("targets", PlayerProfileListResolver.class).resolve(ctx.getSource());
                            if (targets.isEmpty()) return Command.SINGLE_SUCCESS;
                            var sender = ctx.getSource().getSender();
                            if (!Config.getBool("staff.vanish.enabled", true)) {
                                sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/vanish")).color(NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            StringBuilder who = new StringBuilder();
                            for (var target : targets) {
                                if (target.getId() == null) continue;
                                if (!who.isEmpty()) who.append(",");
                                who.append(target.getName());
                                setVanished(Bukkit.getOfflinePlayer(target.getId()), sender, true);
                            }
                            var built = who.toString();
                            if (!built.equals(sender.getName())) sender.sendMessage(KTStrings.getFor(KTStrings.VANISH_VANISHED_OTHER, Component.text(built)).color(NamedTextColor.GOLD));
                            return Command.SINGLE_SUCCESS;
                        })).build());
        commands.registrar().register(Commands.literal("unvanish").requires(source -> source.getSender().isOp())
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    if (!(ctx.getSource().getExecutor() instanceof Player plr)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/unvanish")).color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (!Config.getBool("staff.unvanish.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/vanish")).color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    setVanished(plr, sender, false);
                    return Command.SINGLE_SUCCESS;
                }).then(Commands.argument("targets", ArgumentTypes.playerProfiles()).requires(source -> source.getSender().isOp())
                        .executes(ctx -> {
                            var targets = ctx.getArgument("targets", PlayerProfileListResolver.class).resolve(ctx.getSource());
                            if (targets.isEmpty()) return Command.SINGLE_SUCCESS;
                            var sender = ctx.getSource().getSender();
                            StringBuilder who = new StringBuilder();
                            for (var target : targets) {
                                if (target.getId() == null) continue;
                                if (!who.isEmpty()) who.append(",");
                                who.append(target.getName());
                                setVanished(Bukkit.getOfflinePlayer(target.getId()), sender, false);
                            }
                            var built = who.toString();
                            if (!built.equals(sender.getName())) sender.sendMessage(KTStrings.getFor(KTStrings.VANISH_UNVANISHED_OTHER, Component.text(built).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
                            return Command.SINGLE_SUCCESS;
                        })).build());

        commands.registrar().register(Commands.literal("vanished").requires(source -> source.getSender().isOp())
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    if (!(ctx.getSource().getExecutor() instanceof Player plr)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/vanish")).color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(UserDataManager.get(plr.getUniqueId(), "vanished", false) ? KTStrings.VANISH_STATUS_V : KTStrings.VANISH_STATUS_U));
                    return Command.SINGLE_SUCCESS;
                }).build());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!Config.getBool("staff.vanish.enabled", true)) return;
        var plr = e.getPlayer();
        if (UserDataManager.get(plr.getUniqueId(), "vanished", false)) {
            plr.sendMessage(KTStrings.getFor(KTStrings.VANISH_STATUS_V).color(NamedTextColor.GOLD));
            plr.setMetadata("vanished", new FixedMetadataValue(KamsTweaks.get(), true));
            e.joinMessage(null);
            for (var p : Bukkit.getOnlinePlayers()) {
                p.hidePlayer(KamsTweaks.get(), plr);
            }
        }
        for (var p : Bukkit.getOnlinePlayers()) {
            if (UserDataManager.get(p.getUniqueId(), "vanished", false)) plr.hidePlayer(KamsTweaks.get(), p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!Config.getBool("staff.vanish.enabled", true)) return;
        if (UserDataManager.get(event.getPlayer().getUniqueId(), "vanished", false)) {
            event.setCancelled(true);
            Bukkit.broadcast(Component.text("[Server] ").append(event.message()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!Config.getBool("staff.vanish.enabled", true)) return;
        var plr = e.getPlayer();
        if (UserDataManager.get(plr.getUniqueId(), "vanished", false)) {
            plr.removeMetadata("vanished", KamsTweaks.get());
            e.quitMessage(null);
            for (var p : Bukkit.getOnlinePlayers()) {
                p.hidePlayer(KamsTweaks.get(), plr);
            }
        }
    }

    public void setVanished(@NonNull OfflinePlayer who, CommandSender sender, boolean isVanished) {
        if (!Config.getBool("staff.vanish.enabled", true)) return;
        assert who.getName() != null;
        if (UserDataManager.get(who.getUniqueId(), "vanished", false) == isVanished) return;
        UserDataManager.put(who.getUniqueId(), "vanished", isVanished);
        List<Player> ignore = new ArrayList<>();
        if (sender instanceof Player p) ignore.add(p);
        if (isVanished) {
            KamsTweaks.get().sendToOps(KTStrings.getFor(KTStrings.VANISH_OP_VANISHED, Component.text(sender.getName()), Component.text(who.getName())).decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), ignore);
            if (who.isOnline()) {
                var plr = who.getPlayer();
                assert plr != null;
                plr.setMetadata("vanished", new FixedMetadataValue(KamsTweaks.get(), true));

                Bukkit.getServer().broadcast(Component.translatable("multiplayer.player.left").arguments(Component.text(who.getName())).color(NamedTextColor.YELLOW));
                SRVHelper.sendLeave(plr, "left the server");

                for (var p : Bukkit.getOnlinePlayers()) {
                    p.hidePlayer(KamsTweaks.get(), plr);
                }
                plr.sendMessage(KTStrings.getFor(KTStrings.VANISH_VANISHED).color(NamedTextColor.GOLD));
            }
        } else {
            KamsTweaks.get().sendToOps(KTStrings.getFor(KTStrings.VANISH_OP_UNVANISHED, Component.text(sender.getName()), Component.text(who.getName())).decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), ignore);
            if (who.isOnline()) {
                var plr = who.getPlayer();
                assert plr != null;
                plr.removeMetadata("vanished", KamsTweaks.get());

                Bukkit.getServer().broadcast(Component.translatable("multiplayer.player.joined").arguments(Component.text(who.getName())).color(NamedTextColor.YELLOW));
                SRVHelper.sendJoin(plr, "joined the server");
                for (var p : Bukkit.getOnlinePlayers()) {
                    p.showPlayer(KamsTweaks.get(), plr);
                }
                plr.sendMessage(KTStrings.getFor(KTStrings.VANISH_UNVANISHED).color(NamedTextColor.GOLD));
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!Config.getBool("staff.vanish.enabled", true)) return;
        var split = event.getMessage().split(" ");
        switch (split[0]) {
            case "/tell", "/w", "/msg": {
                if (split.length < 3) return;
                var p = Bukkit.getPlayer(split[1]);
                if (p != null && UserDataManager.get(p.getUniqueId(), "vanished", false)) {
                    event.getPlayer().sendMessage(Component.translatable("argument.entity.notfound.player").color(NamedTextColor.RED));
                    event.setCancelled(true);
                }
            }
            case null, default: break;
        }
    }
}
