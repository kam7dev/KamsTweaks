package kam.kamsTweaks.features.claims;

import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.Button;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.BooleanDialogInput;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.provider.ChestDialogViewProvider;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ClaimsDialogGui {
    Claims claims = null;

    public void setup(Claims claims) {
        this.claims = claims;
    }


    public void openLCPage(Player who, Claims.LandClaim target) {
        List<ActionButton> btns = new ArrayList<>();
        int totalClaims = 0;
        for (var claim : claims.landClaims) {
            if (who.equals(claim.owner)) totalClaims++;
        }
        if (!claims.currentlyClaiming.containsKey(who)) {
            if (totalClaims < KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claims")) {
                btns.add(ActionButton.builder(Component.text("Create a Claim")).action(DialogAction.customClick((view, audience) -> {
                    if (audience instanceof Player player) {
                        player.sendMessage(Component.text("Right click the first corner of where you want to claim with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(")").color(NamedTextColor.GOLD)).color(NamedTextColor.GOLD));
                        claims.currentlyClaiming.put(player, new Claims.LandClaim(player, null, null));
                    }
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
            }
        } else {
            btns.add(ActionButton.builder(Component.text("Cancel Claiming")).action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player player) {
                    player.sendMessage(Component.text("Cancelled claiming land.").color(NamedTextColor.RED));
                    claims.currentlyClaiming.remove(player);
                }
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        if (target != null) {
            btns.add(ActionButton.builder(Component.text("Edit Claim")).action(DialogAction.customClick((view, audience) -> {
                if (audience instanceof Player player) {
                    openEditLandClaimPage(player, target);
                }
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        btns.add(ActionButton.builder(Component.text("View All Claims")).action(DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {
                player.sendMessage(Component.text("Nearby claims are being highlighted."));
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
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        btns.add(ActionButton.builder(Component.text("List Your Claims")).action(DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {
                Component msg = Component.empty();
                int i = 0;
                for (Claims.LandClaim claim : claims.landClaims) {
                    if (player.equals(claim.owner)) {
                        i++;
                        msg = msg.append(Component.text("\n"), Component.text("("), Component.text(claim.priority).color(NamedTextColor.YELLOW), Component.text(") "), Component.text(claim.name).color(NamedTextColor.AQUA), Component.text(": "), Component.text(claim.start.getBlockX() + ", " + claim.start.getBlockY() + ", " + claim.start.getBlockZ()).color(NamedTextColor.GREEN), Component.text(" to "), Component.text(claim.end.getBlockX() + ", " + claim.end.getBlockY() + ", " + claim.end.getBlockZ()).color(NamedTextColor.GREEN), Component.text(" in "), Component.text(claim.start.getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE));
                    }
                }

                player.sendMessage(Component.text("You have ").append(Component.text(i).color(NamedTextColor.GOLD), Component.text(" land claims"), Component.text("."), msg));
            }
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        btns.add(ActionButton.builder(Component.text("Delete ALL of Your Claims")).action(DialogAction.customClick((view, audience) -> {
            audience.showDialog(Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Are you sure you want to delete ALL of your land claims?")).build()).type(DialogType.confirmation(ActionButton.builder(Component.text("Yes, delete them!")).action(DialogAction.customClick((view2, audience2) -> {
                if (audience instanceof Player player) {
                    claims.landClaims.removeIf(claim -> claim.owner.equals(player));
                    player.sendMessage(Component.text("Successfully deleted your land claims."));
                }
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build(), ActionButton.builder(Component.text("No, don't delete them!")).build()))));
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Land Claims")).build()).type(DialogType.multiAction(btns, null, 1)));
        who.showDialog(dialog);
    }

    public void openLCPage(Player who) {
        Claims.LandClaim in = claims.getLandClaim(who.getLocation(), true);
        openLCPage(who, in);
    }


    public void openMainPage(Player who) {
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Claims")).build()).type(DialogType.multiAction(List.of(ActionButton.builder(Component.text("Land Claims")).action(DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {
                openLCPage(player);
            }
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build(), ActionButton.builder(Component.text("Entity Claims")).action(DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {

            }
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build()), null, 1)));
        who.showDialog(dialog);
    }

    public void showArea(Player player, Location corner1, Location corner2, double step, int durationTicks, Color color) {
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


    public void openCreateECPage(Player who, Entity entity) {
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Claim ").append(entity.name(), Component.text(" ("), Component.translatable(entity.getType().translationKey()), Component.text(")?"))).build()).type(DialogType.confirmation(ActionButton.builder(Component.text("Yes")).action(DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {
                if (claims.entityClaims.containsKey(entity.getUniqueId())) {
                    player.sendMessage(Component.text("Sorry! This entity is already claimed!").color(NamedTextColor.RED));
                } else {
                    claims.entityClaims.put(entity.getUniqueId(), new Claims.EntityClaim(player));
                    entity.setFireTicks(0);
                    ((Mob) entity).setTarget(null);
                    player.sendMessage(Component.text("Claimed ").append(entity.name(), Component.text(" successfully.")));
                }
            }
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build(), ActionButton.builder(Component.text("No")).build())));
        who.showDialog(dialog);
    }

    public void openECPage(Player who, Claims.EntityClaim target) {

    }

    public void openECPage(Player who) {
        openECPage(who, (Claims.EntityClaim) null);
    }

    public void openECPage(Player who, Entity entity) {
        if (claims.entityClaims.containsKey(entity.getUniqueId())) {
            openECPage(who, claims.entityClaims.get(entity.getUniqueId()));
        } else {
            openCreateECPage(who, entity);
        }
    }

    public void openEditClaimPermissionsPage(Player who, Claims.LandClaim claim, OfflinePlayer target) {
        who.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit ").append(target == null ? Component.text("Default Permissions") : Component.text("Permissions for ").append(target.getPlayer() == null ? Component.text(target.getName() == null ? "Unknown Player" : target.getName()) : target.getPlayer().displayName())))
                        .inputs(List.of(
                                DialogInput.singleOption("interactions", Component.text("Interactions"), List.of(
                                        SingleOptionDialogInput.OptionEntry.create("none", Component.text("None").color(NamedTextColor.RED), !claim.hasPermission(target, Claims.ClaimPermission.INTERACT_DOOR) && !claim.hasPermission(target, Claims.ClaimPermission.INTERACT_BLOCK)),
                                        SingleOptionDialogInput.OptionEntry.create("doors", Component.text("Doors").color(NamedTextColor.GOLD), claim.hasPermission(target, Claims.ClaimPermission.INTERACT_DOOR) && !claim.hasPermission(target, Claims.ClaimPermission.INTERACT_BLOCK)),
                                        SingleOptionDialogInput.OptionEntry.create("all", Component.text("Blocks").color(NamedTextColor.DARK_PURPLE), claim.hasPermission(target, Claims.ClaimPermission.INTERACT_BLOCK))
                                )).build(),
                                DialogInput.bool("place", Component.text("Block Place"), claim.hasPermission(target, Claims.ClaimPermission.BLOCK_PLACE), "on", "off"),
                                DialogInput.bool("break", Component.text("Block Break"), claim.hasPermission(target, Claims.ClaimPermission.BLOCK_BREAK), "on", "off")
                        )).build()
                )
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Confirm", NamedTextColor.GREEN),
                                Component.text("Click to confirm your changes."),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    List<Claims.ClaimPermission> list = target == null ? claim.defaults : claim.perms.get(target);
                                    list.clear();
                                    if (Boolean.TRUE.equals(view.getBoolean("place"))) {
                                        list.add(Claims.ClaimPermission.BLOCK_PLACE);
                                    }
                                    if (Boolean.TRUE.equals(view.getBoolean("break"))) {
                                        list.add(Claims.ClaimPermission.BLOCK_BREAK);
                                    }
                                    if (view.getText("interactions") != null) {
                                        switch(view.getText("interactions")) {
                                            case "doors" -> list.add(Claims.ClaimPermission.INTERACT_DOOR);
                                            case "all" -> list.add(Claims.ClaimPermission.INTERACT_BLOCK);
                                            case null, default -> {}
                                        }
                                    }
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        ),
                        ActionButton.create(
                                Component.text("Discard", NamedTextColor.RED),
                                Component.text("Click to discard your changes."),
                                100,
                                null
                        )
                ))
        ));
    }


    public void openEditLandClaimPage(Player who, Claims.LandClaim claim) {
        List<ActionButton> btns = new ArrayList<>();
        btns.add(ActionButton.builder(Component.text("Default Permissions")).action(DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {
                openEditClaimPermissionsPage(who, claim, null);
            }
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());

        btns.add(ActionButton.builder(Component.text("Player Permissions")).action(DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {

            }
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());

        btns.add(ActionButton.builder(Component.text("Settings")).action(DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player player) {

            }
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Land Claims")).build()).type(DialogType.multiAction(btns, null, 1)));
        who.showDialog(dialog);
    }
}
