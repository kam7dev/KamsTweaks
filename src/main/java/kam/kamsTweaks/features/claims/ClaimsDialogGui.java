package kam.kamsTweaks.features.claims;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class ClaimsDialogGui {
    Claims claims = null;

    public void setup(Claims claims) {
        this.claims = claims;
    }

    @SuppressWarnings("UnstableApiUsage")
    public void openLCPage(Player who, Claims.LandClaim target) {
        List<ActionButton> btns = new ArrayList<>();
        int totalClaims = 0;
        for (var claim : claims.landClaims) {
            if (who.equals(claim.owner)) totalClaims++;
        }
        if (!claims.currentlyClaiming.containsKey(who)) {
            if (totalClaims < KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claims")) {
                btns.add(ActionButton.builder(Component.text("Create a Claim")).action(DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player player) {
                                player.sendMessage(Component.text("Right click the first corner of where you want to claim with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(")").color(NamedTextColor.GOLD)).color(NamedTextColor.GOLD));
                                claims.currentlyClaiming.put(player, new Claims.LandClaim(player, null, null));
                            }
                        },
                        ClickCallback.Options.builder()
                                .uses(ClickCallback.UNLIMITED_USES)
                                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                .build()
                )).build());
            }
        } else {
            btns.add(ActionButton.builder(Component.text("Cancel Claiming")).action(DialogAction.customClick(
                    (view, audience) -> {
                        if (audience instanceof Player player) {
                            player.sendMessage(Component.text("Cancelled claiming land.").color(NamedTextColor.RED));
                            claims.currentlyClaiming.remove(player);
                        }
                    },
                    ClickCallback.Options.builder()
                            .uses(ClickCallback.UNLIMITED_USES)
                            .lifetime(ClickCallback.DEFAULT_LIFETIME)
                            .build()
            )).build());
        }
        if (target != null) {
            btns.add(ActionButton.builder(Component.text("Edit Claim")).action(DialogAction.customClick(
                    (view, audience) -> {
                        if (audience instanceof Player player) {

                        }
                    },
                    ClickCallback.Options.builder()
                            .uses(ClickCallback.UNLIMITED_USES)
                            .lifetime(ClickCallback.DEFAULT_LIFETIME)
                            .build()
            )).build());
        }
        btns.add(ActionButton.builder(Component.text("View All Claims")).action(DialogAction.customClick(
                (view, audience) -> {
                    if (audience instanceof Player player) {
                        for (Claims.LandClaim claim : claims.landClaims) {
                            if (claim.start.getWorld() != player.getWorld()) continue;
                            Color c;
                            if (claim.owner != null && claim.owner.getUniqueId().equals(player.getUniqueId())) {
                                c = Color.GREEN;
                            } else {
                                c = Color.YELLOW;
//                                c = switch (claim.perms.getOrDefault(Bukkit.getServer().getOfflinePlayer(player.getUniqueId()), claim.m_default)) {
//                                    case NONE -> Color.RED;
//                                    case DOORS -> Color.ORANGE;
//                                    case INTERACT -> Color.PURPLE;
//                                    case BLOCKS -> Color.AQUA;
//                                };
                            }
                            showArea(player, claim.start, claim.end, 1, 20 * 10, c);
                            Location l = new Location(claim.start.getWorld(), (claim.start.x() + claim.end.x()) / 2, (claim.start.y() + claim.end.y()) / 2, (claim.start.z() + claim.end.z()) / 2).add(.5, .5, .5);
                            if (l.distance(player.getLocation()) > 100) continue;
                            TextDisplay display = player.getWorld().spawn(l, TextDisplay.class, entity -> {
                                String s;
                                if (claim.owner != null && claim.owner.getUniqueId().equals(player.getUniqueId())) {
                                    s = "You own this claim.";
                                } else {
                                    s = "TODO";
//                                    s = switch (claim.m_perms.getOrDefault(Bukkit.getServer().getOfflinePlayer(player.getUniqueId()), claim.m_default)) {
//                                        case NONE -> "You can only look around.";
//                                        case DOORS -> "You can use doors.";
//                                        case INTERACT -> "You can interact with blocks.";
//                                        case BLOCKS -> "You can interact and manage blocks.";
//                                    };
                                }
                                entity.text(Component.text("Owned by ").append(Component.text(claim.owner == null ? "the server" : claim.owner.getName() == null ? "Unknown player" : claim.owner.getName(), NamedTextColor.GOLD).appendNewline().append(Component.text(s))));
                                entity.setBillboard(Display.Billboard.CENTER);
                                entity.setPersistent(false);
                                for (Player h : Bukkit.getOnlinePlayers()) {
                                    if (!h.equals(player)) {
                                        h.hideEntity(KamsTweaks.getInstance(), entity);
                                    }
                                }
                            });
                            Listener joinListener = new Listener() {
                                @EventHandler
                                public void onPlayerJoin(PlayerJoinEvent event) {
                                    Player joining = event.getPlayer();
                                    if (!joining.equals(player)) {
                                        Bukkit.getScheduler().runTask(KamsTweaks.getInstance(), () -> joining.hideEntity(KamsTweaks.getInstance(), display));
                                    }
                                }
                            };
                            Bukkit.getPluginManager().registerEvents(joinListener, KamsTweaks.getInstance());
                            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(KamsTweaks.getInstance(), () -> {
                                display.remove();
                                HandlerList.unregisterAll(joinListener);
                            }, 20 * 10);
                        }
                    }
                },
                ClickCallback.Options.builder()
                        .uses(ClickCallback.UNLIMITED_USES)
                        .lifetime(ClickCallback.DEFAULT_LIFETIME)
                        .build()
        )).build());
        btns.add(ActionButton.builder(Component.text("List Your Claims")).action(DialogAction.customClick(
                (view, audience) -> {
                    if (audience instanceof Player player) {
                        Component msg = Component.empty();
                        int i = 0;
                        for (Claims.LandClaim claim : claims.landClaims) {
                            if (player.equals(claim.owner)) {
                                i++;
                                msg = msg.append(Component.text("\n"),
                                        Component.text("("),
                                        Component.text(claim.priority).color(NamedTextColor.YELLOW),
                                        Component.text(") "),
                                        Component.text(claim.name).color(NamedTextColor.AQUA),
                                        Component.text(": "),
                                        Component.text(claim.start.getBlockX() + ", " + claim.start.getBlockY() + ", " + claim.start.getBlockZ()).color(NamedTextColor.GREEN),
                                        Component.text(" to "),
                                        Component.text(claim.end.getBlockX() + ", " + claim.end.getBlockY() + ", " + claim.end.getBlockZ()).color(NamedTextColor.GREEN),
                                        Component.text(" in "),
                                        Component.text(claim.start.getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE)
                                );
                            }
                        }

                        player.sendMessage(
                                Component.text("You have ").append(Component.text(i).color(NamedTextColor.GOLD), Component.text(" land claims"), Component.text("."), msg));
                    }
                },
                ClickCallback.Options.builder()
                        .uses(ClickCallback.UNLIMITED_USES)
                        .lifetime(ClickCallback.DEFAULT_LIFETIME)
                        .build()
        )).build());
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Land Claims")).build())
                .type(DialogType.multiAction(btns, null, 1))
        );
        who.showDialog(dialog);
    }

    public void openLCPage(Player who) {
        Claims.LandClaim in = claims.getLandClaim(who.getLocation());
        openLCPage(who, in);
    }

    @SuppressWarnings("UnstableApiUsage")
    public void openMainPage(Player who) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Claims")).build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(Component.text("Land Claims")).action(DialogAction.customClick(
                                (view, audience) -> {
                                    if (audience instanceof Player player) {
                                        openLCPage(player);
                                    }
                                },
                                ClickCallback.Options.builder()
                                        .uses(ClickCallback.UNLIMITED_USES)
                                        .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                        .build()
                        )).build(),
                        ActionButton.builder(Component.text("Entity Claims")).action(DialogAction.customClick(
                                (view, audience) -> {
                                    if (audience instanceof Player player) {

                                    }
                                },
                                ClickCallback.Options.builder()
                                        .uses(ClickCallback.UNLIMITED_USES)
                                        .lifetime(ClickCallback.DEFAULT_LIFETIME)
                                        .build()
                        )).build()
                ), null, 1))
        );
        who.showDialog(dialog);
    }

    public void showArea(
            Player player,
            Location corner1,
            Location corner2,
            double step,
            int durationTicks,
            Color color
    ) {
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX()) + 1;

        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY()) + 1;

        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ()) + 1;

        World world = corner1.getWorld(); // Assumes both corners are in same world

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                try {
                    if (ticks >= durationTicks || !player.isOnline()) {
                        this.cancel();
                        return;
                    }
                    ticks++;

                    var ploc = player.getLocation();
                    if (!world.equals(ploc.getWorld())) return;

                    if (ticks % 7 == 1) {
                        for (double x = minX; x <= maxX; x += step) {
                            for (double y = minY; y <= maxY; y += step) {
                                for (double z = minZ; z <= maxZ; z += step) {
                                    int faces = 0;
                                    if (x == minX || x == maxX) faces++;
                                    if (y == minY || y == maxY) faces++;
                                    if (z == minZ || z == maxZ) faces++;

                                    // Only edges and corners
                                    if (faces >= 2) {
                                        Location loc = new Location(world, x, y, z);
                                        if (ploc.distance(loc) > 100) continue;
                                        player.spawnParticle(Particle.DUST, loc, 0, 0, 0, 0, 0, new Particle.DustOptions(color, 1.0F));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    Logger.error(exception.getMessage());
                }
            }
        }.runTaskTimer(KamsTweaks.getInstance(), 0L, 1L); // Runs every tick
    }
}
