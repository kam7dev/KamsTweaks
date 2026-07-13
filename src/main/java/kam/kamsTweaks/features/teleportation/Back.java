package kam.kamsTweaks.features.teleportation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.features.gameplay.Graves;
import kam.kamsTweaks.managers.KTPerms;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.features.gameplay.PVP;
import kam.kamsTweaks.utils.LocationUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class Back extends Feature {
    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("back")
                .requires(source -> KTPerms.hasPermission(source, KTPerms.TP_BACK))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!KamsTweaks.get().getConfig().getBoolean("teleportation.back.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/back")));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        if (PVP.instance.inCombat.containsKey(player.getUniqueId())) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.PVP_BLOCK_COMMAND).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        var handler = TeleportFeatures.get();
                        if (handler.teleportations.containsKey(player)) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.TP_ALREADY_TELEPORTING).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (handler.onCooldown.containsKey(player)) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.TP_COOLDOWN, Component.text(handler.onCooldown.get(player))).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (!handler.locations.containsKey(player.getUniqueId())) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.BACK_NO_RECENT).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        int time = KamsTweaks.get().getConfig().getInt("teleportation.timer");
                        sender.sendMessage(KTStrings.getFor(KTStrings.TP_TO_BACK, Component.text(time).color(NamedTextColor.RED)).color(NamedTextColor.GOLD));
                        handler.scheduleTeleport(player, handler.locations.get(player.getUniqueId()), time);
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/back")).color(NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                });
        LiteralCommandNode<CommandSourceStack> buildCommand = command.build();
        commands.registrar().register(buildCommand);
    }

    @Override
    public void loadData() {
        var tpf = TeleportFeatures.get();
        tpf.locations.clear();
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        if (config.contains("back-locs")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("back-locs")).getKeys(false)) {
                try {
                    UUID owner = UUID.fromString(key);
                    String locStr = config.getString("back-locs." + key);
                    assert locStr != null;
                    Location loc = LocationUtils.deserializeLocation(locStr);
                    if (loc.getWorld() == null) continue;
                    tpf.locations.put(owner, loc);
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
    }

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        config.set("back-locs", null);

        TeleportFeatures.get().locations.forEach((uuid, loc) -> {
            if (loc != null) config.set("back-locs." + uuid, LocationUtils.serializeLocation(loc));
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("teleportation.back.on-death", false)) return;
        Player player = e.getPlayer();
        TeleportFeatures.get().locations.put(player.getUniqueId(), Graves.checkLocation(player.getLocation()));
        player.sendMessage(KTStrings.getFor(KTStrings.BACK_INFO, Component.text("/back").color(NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/back"))).color(NamedTextColor.GOLD));
    }
}