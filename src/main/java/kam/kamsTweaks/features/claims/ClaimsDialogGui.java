package kam.kamsTweaks.features.claims;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.features.Names;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"UnstableApiUsage", "CodeBlock2Expr"})
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
        if (!claims.currentlyClaiming.containsKey(who) && who.hasPermission("kamstweaks.claims.claim")) {
            if (totalClaims < KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claims", 30)) {
                btns.add(ActionButton.builder(Component.text("Create a Claim")).action(DialogAction.customClick((view, audience) -> {
                    who.sendMessage(Component.text("Right click the first corner of where you want to claim with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(")").color(NamedTextColor.GOLD)).color(NamedTextColor.GOLD));
                    claims.currentlyClaiming.put(who, new Claims.LandClaim(who, null, null));
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
            }
        } else if (who.hasPermission("kamstweaks.claims.claim")) {
            btns.add(ActionButton.builder(Component.text("Cancel Claiming")).action(DialogAction.customClick((view, audience) -> {
                who.sendMessage(Component.text("Cancelled claiming land.").color(NamedTextColor.RED));
                claims.currentlyClaiming.remove(who);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        if (target != null && (target.owner.getUniqueId().equals(who.getUniqueId()) || who.hasPermission("kamstweaks.claims.manage"))) {
            btns.add(ActionButton.builder(Component.text("Edit Claim")).action(DialogAction.customClick((view, audience) -> {
                openEditLandClaimPage(who, target);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        btns.add(ActionButton.builder(Component.text("View All Claims")).action(DialogAction.customClick((view, audience) -> {
            who.sendMessage(Component.text("Nearby claims are being highlighted."));
            for (Claims.LandClaim claim : claims.landClaims) {
                if (claim.start.getWorld() != who.getWorld()) continue;
                Color c;
                if (claim.owner != null && claim.owner.getUniqueId().equals(who.getUniqueId())) {
                    c = Color.GREEN;
                } else {
                    if (claim.hasPermission(who, Claims.ClaimPermission.BLOCK_BREAK) && claim.hasPermission(who, Claims.ClaimPermission.BLOCK_PLACE) && claim.hasPermission(who, Claims.ClaimPermission.INTERACT_BLOCK)) {
                        c = Color.AQUA;
                    } else if (claim.hasPermission(who, Claims.ClaimPermission.BLOCK_BREAK) || claim.hasPermission(who, Claims.ClaimPermission.BLOCK_PLACE)) {
                        c = Color.FUCHSIA;
                    } else if (claim.hasPermission(who, Claims.ClaimPermission.INTERACT_BLOCK)) {
                        c = Color.PURPLE;
                    } else if (claim.hasPermission(who, Claims.ClaimPermission.INTERACT_DOOR)) {
                        c = Color.ORANGE;
                    } else {
                        c = Color.RED;
                    }
                }
                showArea(who, claim.start, claim.end, 1, 20 * 10, c);
                Location l = new Location(claim.start.getWorld(), (claim.start.x() + claim.end.x()) / 2, (claim.start.y() + claim.end.y()) / 2, (claim.start.z() + claim.end.z()) / 2).add(.5, .5, .5);
                if (l.distance(who.getLocation()) > 100) continue;
                TextDisplay display = who.getWorld().spawn(l, TextDisplay.class, entity -> {
                    String s;
                    if (claim.owner != null && claim.owner.getUniqueId().equals(who.getUniqueId())) {
                        s = "You own this claim.";
                    } else {
                        List<String> perms = new ArrayList<>();
                        if (claim.hasPermission(who, Claims.ClaimPermission.INTERACT_BLOCK)) {
                            perms.add("interact with blocks");
                        } else if (claim.hasPermission(who, Claims.ClaimPermission.INTERACT_DOOR)) {
                            perms.add("interact with doors");
                        } else {
                            perms.add("not interact");
                        }
                        if (claim.hasPermission(who, Claims.ClaimPermission.BLOCK_BREAK)) {
                            perms.add("break blocks");
                        }
                        if (claim.hasPermission(who, Claims.ClaimPermission.BLOCK_PLACE)) {
                            perms.add("place blocks");
                        }
                        s = switch (perms.size()) {
                            case 1 -> "You can " + perms.getFirst() + ".";
                            case 2 -> "You can " + perms.getFirst() + " and " + perms.get(1) + ".";
                            default -> "You can " + String.join(", ", perms.subList(0, perms.size() - 1))
                                    + ", and " + perms.getLast() + ".";
                        };
                    }
                    entity.text(Component.text("Owned by ").append(claim.owner == null ? Component.text("the server", NamedTextColor.GOLD) : Names.instance.getRenderedName(claim.owner)).appendNewline().append(Component.text(s)));
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setPersistent(false);
                    for (Player h : Bukkit.getOnlinePlayers()) {
                        if (!h.equals(who)) {
                            h.hideEntity(KamsTweaks.getInstance(), entity);
                        }
                    }
                });
                Listener joinListener = new Listener() {
                    @EventHandler
                    public void onPlayerJoin(PlayerJoinEvent event) {
                        Player joining = event.getPlayer();
                        if (!joining.equals(who)) {
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
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        btns.add(ActionButton.builder(Component.text("List Your Claims")).action(DialogAction.customClick((view, audience) -> {
            Component msg = Component.empty();
            int i = 0;
            for (Claims.LandClaim claim : claims.landClaims) {
                if (who.equals(claim.owner)) {
                    i++;
                    msg = msg.append(Component.text("\n"), Component.text("("), Component.text(claim.priority).color(NamedTextColor.YELLOW), Component.text(") "), Component.text(claim.name).color(NamedTextColor.AQUA), Component.text(": "), Component.text(claim.start.getBlockX() + ", " + claim.start.getBlockY() + ", " + claim.start.getBlockZ()).color(NamedTextColor.GREEN), Component.text(" to "), Component.text(claim.end.getBlockX() + ", " + claim.end.getBlockY() + ", " + claim.end.getBlockZ()).color(NamedTextColor.GREEN), Component.text(" in "), Component.text(claim.start.getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE));
                }
            }

            who.sendMessage(Component.text("You have ").append(Component.text(i).color(NamedTextColor.GOLD), Component.text(" land claims"), Component.text("."), msg));
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        btns.add(ActionButton.builder(Component.text("Delete ALL of Your Claims")).action(DialogAction.customClick((view, audience) -> {
            audience.showDialog(Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Are you sure you want to delete ALL of your land claims?")).build()).type(DialogType.confirmation(ActionButton.builder(Component.text("Yes, delete them!")).action(DialogAction.customClick((view2, audience2) -> {
                claims.landClaims.removeIf(claim -> claim.owner.equals(who));
                who.sendMessage(Component.text("Successfully deleted your land claims."));
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
            openLCPage(who);
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build(), ActionButton.builder(Component.text("Entity Claims")).action(DialogAction.customClick((view, audience) -> {
            openECPage(who);
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
                    Logger.excs.add(exception);
                    Logger.error(exception.getMessage());
                }
            }
        }.runTaskTimer(KamsTweaks.getInstance(), 0L, 1L); // Runs every tick
    }


    public void openCreateECPage(Player who, Entity entity) {
        if (!claims.isClaimable(entity)) {
            who.sendMessage(Component.text("You can't claim this!").color(NamedTextColor.RED));
            return;
        }
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Claim ").append(entity.name(), Component.text(" ("), Component.translatable(entity.getType().translationKey()), Component.text(")?"))).build()).type(DialogType.confirmation(ActionButton.builder(Component.text("Yes")).action(DialogAction.customClick((view, audience) -> {
                if (claims.entityClaims.containsKey(entity.getUniqueId())) {
                    who.sendMessage(Component.text("Sorry! This entity is already claimed!").color(NamedTextColor.RED));
                } else {
                    claims.entityClaims.put(entity.getUniqueId(), new Claims.EntityClaim(who, entity.getUniqueId()));
                    entity.setFireTicks(0);
                    ((Mob) entity).setTarget(null);
                    entity.setPersistent(true);
                    ((LivingEntity) entity).setRemoveWhenFarAway(false);
                    who.sendMessage(Component.text("Claimed ").append(entity.name(), Component.text(" successfully.")));
                }
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build(), ActionButton.builder(Component.text("No")).build())));
        who.showDialog(dialog);
    }

    @SuppressWarnings("unused") // while i havent finished it yet
    public void openECPage(Player who, Claims.EntityClaim target) {
        List<ActionButton> btns = new ArrayList<>();
        if (target != null && (target.owner.getUniqueId().equals(who.getUniqueId()) || who.hasPermission("kamstweaks.claims.manage"))) {
            btns.add(ActionButton.builder(Component.text("Edit Claim")).action(DialogAction.customClick((view, audience) -> {
                openEditEntityClaimPage(who, target);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        btns.add(ActionButton.builder(Component.text("List Your Claims")).action(DialogAction.customClick((view, audience) -> {
            var ref = new Object() {
                Component msg = Component.empty();
                int i = 0;
            };
            claims.entityClaims.forEach((uuid, claim) -> {
                if (who.getUniqueId().equals(claim.owner.getUniqueId())) {
                    var entity = Bukkit.getEntity(uuid);
                    if (entity != null) {
                        ref.i++;
                        ref.msg = ref.msg.append(Component.text("\n"), entity.name().color(NamedTextColor.AQUA), Component.text(" ("), Component.translatable(entity.getType().translationKey()), Component.text("): "), Component.text(entity.getLocation().getBlockX() + ", " + entity.getLocation().getBlockY() + ", " + entity.getLocation().getBlockZ()).color(NamedTextColor.GREEN), Component.text(" in "), Component.text(entity.getLocation().getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE));
                    }
                }
            });

            who.sendMessage(Component.text("You have ").append(Component.text(ref.i).color(NamedTextColor.GOLD), Component.text(" entity claims"), Component.text("."), ref.msg));
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        btns.add(ActionButton.builder(Component.text("Delete ALL of Your Claims")).action(DialogAction.customClick((view, audience) -> {
            audience.showDialog(Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Are you sure you want to delete ALL of your entity claims?")).build()).type(DialogType.confirmation(ActionButton.builder(Component.text("Yes, delete them!")).action(DialogAction.customClick((view2, audience2) -> {
                List<UUID> toRem = new ArrayList<>();
                claims.entityClaims.forEach((uuid, claim) -> {
                    if (claim.owner.getUniqueId().equals(who.getUniqueId())) {
                        toRem.add(uuid);
                    }
                });
                for (var uuid : toRem) {
                    claims.entityClaims.remove(uuid);
                }
                who.sendMessage(Component.text("Successfully deleted your entity claims."));
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build(), ActionButton.builder(Component.text("No, don't delete them!")).build()))));
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Entity Claims")).build()).type(DialogType.multiAction(btns, null, 1)));
        who.showDialog(dialog);
    }

    public void openEditEntityClaimPage(Player who, Claims.EntityClaim claim) {
        List<ActionButton> btns = new ArrayList<>();
        if (claim.owner.getUniqueId().equals(who.getUniqueId())) {
            btns.add(ActionButton.builder(Component.text("Default Permissions")).action(DialogAction.customClick((view, audience) -> {
                openEditECPermissionsPage(who, claim, null);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());

            btns.add(ActionButton.builder(Component.text("Player Permissions")).action(DialogAction.customClick((view, audience) -> {
                openECSelectTargetPage(who, claim);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());

            btns.add(ActionButton.builder(Component.text("Settings")).action(DialogAction.customClick((view, audience) -> {
                openEditECSettingsPage(who, claim);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        btns.add(ActionButton.builder(Component.text("Delete Claim")).action(DialogAction.customClick((view, audience) -> {
            audience.showDialog(Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Are you sure you want to delete this entity claim?")).build()).type(DialogType.confirmation(ActionButton.builder(Component.text("Yes, delete it!")).action(DialogAction.customClick((view2, audience2) -> {
                if (!who.getUniqueId().equals(claim.owner.getUniqueId())) {
                    claims.entityClaims.remove(claim.entity);
                    Logger.warn("[Claim management] " + who.getName() + " just deleted " + claim.owner.getName() + "'s entity claim!");
                    Bukkit.getServer().sendMessage(Component.text("[Land Claims] " + who.getName() + " just deleted " + claim.owner.getName() + "'s entity claim!"));
                    Plugin dsPlugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
                    if (dsPlugin != null && dsPlugin.isEnabled()) {
                        try {
                            Class<?> dsClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
                            Object dsInstance = dsClass.getMethod("getPlugin").invoke(null);

                            Method getChannel = dsClass.getMethod("getDestinationTextChannelForGameChannelName", String.class);
                            Object channel = getChannel.invoke(dsInstance, "global");

                            if (channel != null) {
                                Method sendMessage = channel.getClass().getMethod("sendMessage", CharSequence.class);
                                Object action = sendMessage.invoke(channel, "<t:" + System.currentTimeMillis() / 1000 + ":R> <@!1254538148755537971> ‼️" + (claim.owner.getName() != null ? claim.owner.getName() : "the server").toUpperCase() + "'S ENTITY CLAIM WAS DELETED BY " + who.getName().toUpperCase() + ", SEND A PIPEBOMB TO THEIR DOORSTEP! ‼️ ⚠️ 🔥");
                                action.getClass().getMethod("queue").invoke(action);
                            }
                        } catch (Exception e) {
                            Logger.excs.add(e);
                            Logger.error("Failed to send message to discord: " + e.getMessage());
                        }
                    }

                    return;
                }
                claims.entityClaims.remove(claim.entity);
                who.sendMessage(Component.text("Successfully deleted your entity claim."));
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build(), ActionButton.builder(Component.text("No, don't delete it!")).build()))));
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Entity Claims: ").append(Bukkit.getEntity(claim.entity) != null ? Objects.requireNonNull(Bukkit.getEntity(claim.entity)).name() : Component.text("Missing"))).build()).type(DialogType.multiAction(btns, null, 1)));
        who.showDialog(dialog);
    }

    public void openECPage(Player who) {
        var entity = who.getTargetEntity(5);
        if (entity != null && claims.entityClaims.containsKey(entity.getUniqueId())) {
            openECPage(who, claims.entityClaims.get(entity.getUniqueId()));
        } else {
            openECPage(who, (Claims.EntityClaim) null);
        }
    }

    public void openECPage(Player who, Entity entity) {
        if (claims.entityClaims.containsKey(entity.getUniqueId())) {
            openECPage(who, claims.entityClaims.get(entity.getUniqueId()));
        } else {
            openCreateECPage(who, entity);
        }
    }

    public void openEditLCPermissionsPage(Player who, Claims.LandClaim claim, OfflinePlayer target) {
        List<DialogInput> btns = new ArrayList<>(List.of(
                DialogInput.singleOption("interactions", Component.text("Interactions"), List.of(
                        SingleOptionDialogInput.OptionEntry.create("none", Component.text("None").color(NamedTextColor.RED), !claim.hasPermission(target, Claims.ClaimPermission.INTERACT_DOOR) && !claim.hasPermission(target, Claims.ClaimPermission.INTERACT_BLOCK)),
                        SingleOptionDialogInput.OptionEntry.create("doors", Component.text("Doors").color(NamedTextColor.GOLD), claim.hasPermission(target, Claims.ClaimPermission.INTERACT_DOOR) && !claim.hasPermission(target, Claims.ClaimPermission.INTERACT_BLOCK)),
                        SingleOptionDialogInput.OptionEntry.create("all", Component.text("Blocks").color(NamedTextColor.DARK_PURPLE), claim.hasPermission(target, Claims.ClaimPermission.INTERACT_BLOCK))
                )).build(),
                DialogInput.bool("place", Component.text("Block Place"), claim.hasPermission(target, Claims.ClaimPermission.BLOCK_PLACE), "on", "off"),
                DialogInput.bool("break", Component.text("Block Break"), claim.hasPermission(target, Claims.ClaimPermission.BLOCK_BREAK), "on", "off")
        ));
        if (target != null) {
            btns.add(DialogInput.bool("default", Component.text("Follow Default Permission (overwrites other options)"), claim.hasPermission(target, Claims.ClaimPermission.DEFAULT), "on", "off"));
        }

        who.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit ").append(target == null ? Component.text("Default Permissions") : Component.text("Permissions for ").append(Names.instance.getRenderedName(target))))
                        .inputs(btns).build()
                )
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Confirm", NamedTextColor.GREEN),
                                Component.text("Click to confirm your changes."),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    List<Claims.ClaimPermission> list;
                                    if (target == null) {
                                        list = claim.defaults;
                                    } else {
                                        if (!claim.perms.containsKey(target)) claim.perms.put(target, new ArrayList<>());
                                        list = claim.perms.get(target);
                                    }
                                    list.clear();
                                    if (Boolean.TRUE.equals(view.getBoolean("default"))) {
                                        list.add(Claims.ClaimPermission.DEFAULT);
                                    } else {
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

    void openLCSelectTargetPage(Player who, Claims.LandClaim claim) {
        List<ActionButton> plrs = new ArrayList<>();
        for (var plr : Bukkit.getServer().getOfflinePlayers()) {
            if (plr == claim.owner || plr.getName() == null) continue;
            Component comp = Component.text(plr.getName());
            if (plr.getPlayer() != null) {
                comp = plr.getPlayer().displayName().append(Component.text("("), comp, Component.text(")"));
            }
            plrs.add(ActionButton.builder(comp).action(DialogAction.customClick((view, audience) -> {
                openEditLCPermissionsPage(who, claim, plr);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Select a player to edit")).build()).type(DialogType.multiAction(plrs, null, 3)));
        who.showDialog(dialog);
    }


    public void openEditLandClaimPage(Player who, Claims.LandClaim claim) {
        List<ActionButton> btns = new ArrayList<>();
        if (claim.owner.getUniqueId().equals(who.getUniqueId())) {
            btns.add(ActionButton.builder(Component.text("Default Permissions")).action(DialogAction.customClick((view, audience) -> {
                openEditLCPermissionsPage(who, claim, null);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());

            btns.add(ActionButton.builder(Component.text("Player Permissions")).action(DialogAction.customClick((view, audience) -> {
                openLCSelectTargetPage(who, claim);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());

            btns.add(ActionButton.builder(Component.text("Settings")).action(DialogAction.customClick((view, audience) -> {
                openEditLCSettingsPage(who, claim);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        btns.add(ActionButton.builder(Component.text("Delete Claim")).action(DialogAction.customClick((view, audience) -> {
                audience.showDialog(Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Are you sure you want to delete your land claim (").append(Component.text(claim.name).color(NamedTextColor.GOLD), Component.text(")?"))).build()).type(DialogType.confirmation(ActionButton.builder(Component.text("Yes, delete it!")).action(DialogAction.customClick((view2, audience2) -> {
                    if (!who.getUniqueId().equals(claim.owner.getUniqueId())) {
                        claims.landClaims.remove(claim);
                        Logger.warn("[Claim management] " + who.getName() + " just deleted " + claim.owner.getName() + "'s land claim!");
                        Bukkit.getServer().sendMessage(Component.text("[Land Claims] " + who.getName() + " just deleted " + claim.owner.getName() + "'s land claim!"));
                        Plugin dsPlugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
                        if (dsPlugin != null && dsPlugin.isEnabled()) {
                            try {
                                Class<?> dsClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
                                Object dsInstance = dsClass.getMethod("getPlugin").invoke(null);

                                Method getChannel = dsClass.getMethod("getDestinationTextChannelForGameChannelName", String.class);
                                Object channel = getChannel.invoke(dsInstance, "global");

                                if (channel != null) {
                                    Method sendMessage = channel.getClass().getMethod("sendMessage", CharSequence.class);
                                    Object action = sendMessage.invoke(channel, "<t:" + System.currentTimeMillis() / 1000 + ":R> <@!1254538148755537971> ‼️" + (claim.owner.getName() != null ? claim.owner.getName() : "the server").toUpperCase() + "'S LAND CLAIM WAS DELETED BY " + who.getName().toUpperCase() + ", SEND A PIPEBOMB TO THEIR DOORSTEP! ‼️ ⚠️ 🔥");
                                    action.getClass().getMethod("queue").invoke(action);
                                }
                            } catch (Exception e) {
                                Logger.excs.add(e);
                                Logger.error("Failed to send message to discord: " + e.getMessage());
                            }
                        }

                        return;
                    }
                    claims.landClaims.remove(claim);
                    who.sendMessage(Component.text("Successfully deleted your land claim."));
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build(), ActionButton.builder(Component.text("No, don't delete it!")).build()))));
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Land Claims: " + claim.name)).build()).type(DialogType.multiAction(btns, null, 1)));
        who.showDialog(dialog);
    }

    public void openEditLCSettingsPage(Player who, Claims.LandClaim claim) {
        who.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit Claim Settings"))
                        .inputs(List.of(
                                DialogInput.text("name", Component.text("Name")).initial(claim.name).build(),
                                DialogInput.numberRange("prio", Component.text("Priority"), -100, 100).step(1f).initial(claim.priority.floatValue()).build()
                        )).build()
                )
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Confirm", NamedTextColor.GREEN),
                                Component.text("Click to confirm your changes."),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    claim.name = view.getText("name");
                                    // had to do a variable cause intellij wasnt doing assert right 💀
                                    // https://cdn.discordapp.com/attachments/1357431808421007410/1421905060543336539/image.png
                                    var prioF = view.getFloat("prio");
                                    assert prioF != null;
                                    claim.priority = prioF.intValue();
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

    public void openEditECSettingsPage(Player who, Claims.EntityClaim claim) {
        who.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit Claim Settings"))
                        .inputs(List.of(

                                DialogInput.bool("aggro", Component.text("Can Target Players")).initial(claim.canAggro).build()
                        )).build()
                )
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Confirm", NamedTextColor.GREEN),
                                Component.text("Click to confirm your changes."),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    claim.canAggro = Boolean.TRUE.equals(view.getBoolean("aggro"));
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

    void openECSelectTargetPage(Player who, Claims.EntityClaim claim) {
        List<ActionButton> plrs = new ArrayList<>();
        for (var plr : Bukkit.getServer().getOfflinePlayers()) {
            if (plr == claim.owner || plr.getName() == null) continue;
            Component comp = Component.text(plr.getName());
            if (plr.getPlayer() != null) {
                comp = plr.getPlayer().displayName().append(Component.text("("), comp, Component.text(")"));
            }
            plrs.add(ActionButton.builder(comp).action(DialogAction.customClick((view, audience) -> {
                openEditECPermissionsPage(who, claim, plr);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build());
        }
        Dialog dialog = Dialog.create(builder -> builder.empty().base(DialogBase.builder(Component.text("Select a player to edit")).build()).type(DialogType.multiAction(plrs, null, 3)));
        who.showDialog(dialog);
    }

    public void openEditECPermissionsPage(Player who, Claims.EntityClaim claim, OfflinePlayer target) {
        List<DialogInput> btns = new ArrayList<>(List.of(
                DialogInput.bool("interact", Component.text("Interact with Entity"), claim.hasPermission(target, Claims.ClaimPermission.INTERACT_ENTITY), "on", "off"),
                DialogInput.bool("damage", Component.text("Damage Entity"), claim.hasPermission(target, Claims.ClaimPermission.DAMAGE_ENTITY), "on", "off")
        ));
        if (target != null) {
            btns.add(DialogInput.bool("default", Component.text("Follow Default Permission (overwrites other options)"), claim.hasPermission(target, Claims.ClaimPermission.DEFAULT), "on", "off"));
        }

        who.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Edit ").append(target == null ? Component.text("Default Permissions") : Component.text("Permissions for ").append(Names.instance.getRenderedName(target))))
                        .inputs(btns).build()
                )
                .type(DialogType.confirmation(
                        ActionButton.create(
                                Component.text("Confirm", NamedTextColor.GREEN),
                                Component.text("Click to confirm your changes."),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    List<Claims.ClaimPermission> list;
                                    if (target == null) {
                                        list = claim.defaults;
                                    } else {
                                        if (!claim.perms.containsKey(target)) claim.perms.put(target, new ArrayList<>());
                                        list = claim.perms.get(target);
                                    }
                                    list.clear();
                                    if (Boolean.TRUE.equals(view.getBoolean("default"))) {
                                        list.add(Claims.ClaimPermission.DEFAULT);
                                    } else {
                                        if (Boolean.TRUE.equals(view.getBoolean("interact"))) {
                                            list.add(Claims.ClaimPermission.INTERACT_ENTITY);
                                        }
                                        if (Boolean.TRUE.equals(view.getBoolean("damage"))) {
                                            list.add(Claims.ClaimPermission.DAMAGE_ENTITY);
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
}
