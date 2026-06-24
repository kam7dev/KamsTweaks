package kam.kamsTweaks.features;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PVP extends Feature {
    private final Map<UUID, Boolean> pvp = new HashMap<>();
    public Map<Player, Integer> onCooldown = new HashMap<>();

    @Override
    public void setup() {
        ConfigCommand.addConfig(new ConfigCommand.BoolConfig("player-pvp-toggle.enabled", "player-pvp-toggle.enabled", false, "kamstweaks.configure"));
    }

    void cooldown(Player player) {
        onCooldown.put(player, 15);
        var r = new Runnable() {
            int id = 0;

            @Override
            public void run() {
                int val = onCooldown.get(player) - 1;
                if (val <= 0) {
                    Bukkit.getScheduler().cancelTask(id);
                    onCooldown.remove(player);
                    return;
                }
                onCooldown.put(player, val);
            }
        };

        r.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), r, 20, 20);
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("pvp")
                .then(Commands.literal("on")
                        .executes(ctx -> {
                            if (!KamsTweaks.get().getConfig().getBoolean("player-pvp-toggle.enabled", true)) {
                                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/pvp")));
                                return Command.SINGLE_SUCCESS;
                            }
                            Entity exec = ctx.getSource().getExecutor();
                            if (exec instanceof Player p) {
                                if (onCooldown.containsKey(p)) {
                                    p.sendMessage(KTStrings.getFor(KTStrings.COOLDOWN, Component.text("/pvp"), Component.text(onCooldown.get(p))).color(NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                UUID playerUUID = p.getUniqueId();
                                pvp.put(playerUUID, true);
                                p.sendMessage(KTStrings.getFor(KTStrings.PVP_ENABLE).color(NamedTextColor.GREEN));
                                cooldown(p);
                                return Command.SINGLE_SUCCESS;
                            }
                            ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("off")
                        .executes(ctx -> {
                            if (!KamsTweaks.get().getConfig().getBoolean("player-pvp-toggle.enabled", true)) {
                                ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/pvp")));
                                return Command.SINGLE_SUCCESS;
                            }
                            Entity exec = ctx.getSource().getExecutor();
                            if (exec instanceof Player p) {
                                if (onCooldown.containsKey(p)) {
                                    p.sendMessage(KTStrings.getFor(KTStrings.COOLDOWN, Component.text("/pvp"), Component.text(onCooldown.get(p))).color(NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                UUID playerUUID = p.getUniqueId();
                                pvp.put(playerUUID, false);
                                p.sendMessage(KTStrings.getFor(KTStrings.PVP_DISABLE).color(NamedTextColor.RED));
                                cooldown(p);
                                return Command.SINGLE_SUCCESS;
                            }
                            ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                            return Command.SINGLE_SUCCESS;
                        }))
                .executes(ctx -> {
                    if (!KamsTweaks.get().getConfig().getBoolean("player-pvp-toggle.enabled", true)) {
                        ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.DISABLED_SINGULAR, Component.text("/pvp")));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity exec = ctx.getSource().getExecutor();
                    if (exec instanceof Player p) {
                        UUID playerUUID = p.getUniqueId();
                        if (pvp.getOrDefault(playerUUID, true)) {
                            p.sendMessage(KTStrings.getFor(KTStrings.PVP_STATUS_ENABLED).color(NamedTextColor.GOLD));
                        } else {
                            p.sendMessage(KTStrings.getFor(KTStrings.PVP_STATUS_DISABLED).color(NamedTextColor.GOLD));
                        }
                        return Command.SINGLE_SUCCESS;
                    }
                    ctx.getSource().getSender().sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY));
                    return Command.SINGLE_SUCCESS;
                }).build());
    }

    @Override
    public void loadData() {
        pvp.clear();
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        if (config.contains("player-pvp.enabled")) {
            for (String key : Objects.requireNonNull(config.getConfigurationSection("player-pvp.enabled")).getKeys(false)) {
                try {
                    pvp.put(UUID.fromString(key), config.getBoolean("player-pvp.enabled." + key));
                } catch (Exception e) {
                    Logger.handleException(e);
                }
            }
        }
    }

    @Override
    public void saveData() {
        FileConfiguration config = KamsTweaks.get().getDataConfig();
        config.set("player-pvp", null);
        pvp.forEach((uuid, en) -> {
            if (en != null) config.set("player-pvp.enabled." + uuid, en);
        });
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!KamsTweaks.get().getConfig().getBoolean("player-pvp-toggle.enabled", true))
            return;
        if (e.getEntity() instanceof Player target && e.getDamageSource().getCausingEntity() instanceof Player causer) {
            if (!pvp.getOrDefault(target.getUniqueId(), true)) {
                e.setCancelled(true);
                causer.sendMessage(KTStrings.getFor(KTStrings.PVP_TARGET_DISABLED, Names.instance.getRenderedName(target)).color(NamedTextColor.RED));
            } else if (!pvp.getOrDefault(causer.getUniqueId(), true)) {
                e.setCancelled(true);
                causer.sendMessage(KTStrings.getFor(KTStrings.PVP_YOU_DISABLED).color(NamedTextColor.RED));
            }
        }
    }
}
