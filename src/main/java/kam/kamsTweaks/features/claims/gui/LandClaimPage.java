package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.*;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.*;
import io.papermc.paper.registry.data.dialog.body.*;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.*;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.features.ChatFilter;
import kam.kamsTweaks.features.Names;
import kam.kamsTweaks.features.claims.Claims;
import kam.kamsTweaks.features.claims.LandClaims;
import kam.kamsTweaks.features.claims.LandClaims.*;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class LandClaimPage extends GuiLayer {
    LandClaim target;
    void init() {
        if (target == null) {
            this.target = Claims.get().landClaims.getClaim(who.getLocation());
        }
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(Component.text("Land Claims"));
            var dia = builder.empty().base(base.build());

            var btns = new ArrayList<ActionButton>();

            int totalClaims = 0;
            for (var claim : Claims.get().landClaims.claims) {
                if (claim.owner != null && who.getUniqueId().equals(claim.owner.getUniqueId())) totalClaims += claim.claimCount;
            }

            if (totalClaims < KamsTweaks.get().getConfig().getInt("land-claims.max-claims", 30)) {
                var createBtn = ActionButton.builder(Component.text("Create a Claim")).action(DialogAction.customClick((view, audience) -> {
                    Claims.get().landClaims.startClaiming(who);
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
                btns.add(createBtn);
            }

            if (target != null) {
                if ((target.owner != null && target.owner.getUniqueId().equals(who.getUniqueId())) || who.hasPermission("kamstweaks.claims.manage")) {
                    var editBtn = ActionButton.builder(Component.text("Edit Claim")).action(DialogAction.customClick((view, audience) -> {
                        new EditPage(who, target).show();
                    }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
                    btns.add(editBtn);
                }
            }

            var viewBtn = ActionButton.builder(Component.text("View All Claims")).action(DialogAction.customClick((view, audience) -> {
                for (var claim : Claims.get().landClaims.claims) {
                    Color c;
                    if (claim.owner != null && claim.owner.getUniqueId().equals(who.getUniqueId())) {
                        c = Color.GREEN;
                    } else {
                        if (claim.hasPermission(who, LandPermission.BLOCK_BREAK) && claim.hasPermission(who, LandPermission.BLOCK_PLACE) && claim.hasPermission(who, LandPermission.BLOCK_INTERACT)) {
                            c = Color.AQUA;
                        } else if (claim.hasPermission(who, LandPermission.BLOCK_BREAK) || claim.hasPermission(who, LandPermission.BLOCK_PLACE)) {
                            c = Color.FUCHSIA;
                        } else if (claim.hasPermission(who, LandPermission.BLOCK_INTERACT)) {
                            c = Color.PURPLE;
                        } else if (claim.hasPermission(who, LandPermission.DOOR_INTERACT)) {
                            c = Color.ORANGE;
                        } else {
                            c = Color.RED;
                        }
                    }
                    LandClaims.showArea(who, claim.start, claim.end, 1, 200, c);
                }
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
            btns.add(viewBtn);

            var listBtn = ActionButton.builder(Component.text("List Your Claims")).action(DialogAction.customClick((view, audience) -> {

            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
            btns.add(listBtn);

            var deleteBtn = ActionButton.builder(Component.text("Delete ALL of Your Claims")).action(DialogAction.customClick((view, audience) -> {

            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
            btns.add(deleteBtn);

            dia.type(DialogType.multiAction(btns, null, 1));
        });
    }

    public LandClaimPage(Player who) {
        super(who);
        init();
    }

    public LandClaimPage(Player who, LandClaim target) {
        super(who);
        this.target = target;
        init();
    }

    @Override
    public void show() {
        if (!KamsTweaks.get().getDataConfig().contains("shown." + who.getUniqueId())) {
            KamsTweaks.get().getDataConfig().set("shown." + who.getUniqueId(), true);
            new InfoPage(who, this).show();
            return;
        }
        super.show();
    }

    public static class EditPage extends GuiLayer {
        LandClaim claim;
        public EditPage(Player who, LandClaim claim) {
            super(who);
            this.claim = claim;
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(Component.text("Edit Claim: ").append(Component.text(claim.config.name).color(NamedTextColor.GOLD)));
                if (claim.owner == null || !claim.owner.getUniqueId().equals(who.getUniqueId())) {
                    base.body(List.of(DialogBody.plainMessage(
                            Component.text("Careful! This claim is owned by ").append(
                                    claim.getOwnerName(),
                                    Component.text("!\nYou can only edit this because you are an operator.")
                            ).color(NamedTextColor.RED))));
                }
                var dia = builder.empty().base(base.build());

                var defaultBtn = ActionButton.builder(Component.text("Default Player Permissions")).action(DialogAction.customClick((view, audience) -> {
                    new PermissionPage(who, claim, PermMode.DEFAULT).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var entityBtn = ActionButton.builder(Component.text("Default Entity Permissions")).action(DialogAction.customClick((view, audience) -> {
                    new PermissionPage(who, claim, PermMode.ENTITY_DEFAULT).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var permBtn = ActionButton.builder(Component.text("Player Permissions")).action(DialogAction.customClick((view, audience) -> {
                    new UserListPage(who, claim).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var settingsBtn = ActionButton.builder(Component.text("Settings")).action(DialogAction.customClick((view, audience) -> {
                    new SettingsPage(who, claim).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var deleteBtn = ActionButton.builder(Component.text("Delete")).action(DialogAction.customClick((view, audience) -> {

                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                dia.type(DialogType.multiAction(List.of(defaultBtn, entityBtn, permBtn, settingsBtn, deleteBtn), null, 1));
            });
        }
    }

    public static class UserListPage extends GuiLayer {
        LandClaim claim;
        public UserListPage(Player who, LandClaim claim) {
            super(who);
            this.claim = claim;
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(Component.text("Edit Permissions: ").append(Component.text(claim.config.name).color(NamedTextColor.GOLD)));

                var plrs = Bukkit.getOfflinePlayers();
                Arrays.sort(plrs, Comparator.comparing(OfflinePlayer::getName, Comparator.nullsLast(String::compareTo)));
                List<ActionButton> btns = new ArrayList<>();
                for (var plr : plrs) {
                    var name = Names.instance.getRenderedName(plr, true);
                    var btn = ActionButton.builder(name);
                    btn.action(DialogAction.customClick((view, audience) -> {
                        new PermissionPage(who, claim, plr).show();
                    }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build()));
                    btns.add(btn.build());
                }

                var dia = builder.empty().base(base.build());
                dia.type(DialogType.multiAction(btns, null, 3));
            });
        }
    }

    public static class SettingsPage extends GuiLayer {
        LandClaim claim;
        public SettingsPage(Player who, LandClaim claim) {
            super(who);
            this.claim = claim;
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(Component.text("Edit Claim Settings: ").append(Component.text(claim.config.name).color(NamedTextColor.GOLD)));

                var nameInp = DialogInput.text("name", Component.text("Name")).initial(claim.config.name).build();
                var prioDrag = DialogInput.numberRange("prio", Component.text("Priority"), -100, 100).step(1f).initial(claim.config.priority.floatValue()).build();
                // TODO
//                var treeBox = DialogInput.bool("tree", Component.text("Trees Grow")).initial(claim.config.treesGrow).build();
//                var grassBox = DialogInput.bool("grass", Component.text("Grass Spreads")).initial(claim.config.grassSpread).build();
                base.inputs(List.of(nameInp, prioDrag/*, treeBox, grassBox*/));

                var dia = builder.empty().base(base.build());

                var confirmBtn = ActionButton.create(
                        Component.text("Confirm", NamedTextColor.GREEN),
                        Component.text("Click to confirm your changes."),
                        100,
                        DialogAction.customClick((view, audience) -> {
                            claim.config.priority = Objects.requireNonNullElse(view.getFloat("prio"), 0).intValue();
//                            claim.config.treesGrow = Objects.requireNonNullElse(view.getBoolean("tree"), true);
//                            claim.config.grassSpread = Objects.requireNonNullElse(view.getBoolean("grass"), true);
                            // TODO

                            var name = Objects.requireNonNullElse(view.getText("name"), "Unnamed Claim");
                            var res = ChatFilter.instance.isFiltered(name);
                            if (res.first) {
                                ChatFilter.warnStaff("Claim rename by " + who.getName() + " was caught by the " + res.second.name + " automod: " + name);
                                show();
                                who.sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
                                return;
                            }
                            claim.config.name = name;
                        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                );

                dia.type(DialogType.confirmation(
                        confirmBtn,
                        ActionButton.create(
                                Component.text("Discard", NamedTextColor.RED),
                                Component.text("Click to discard your changes."),
                                100,
                                null
                        )
                ));
            });
        }
    }

    public enum PermMode {
        DEFAULT,
        ENTITY_DEFAULT,
        ENTITY,
        OFFLINE_PLAYER,
    }

    public static class PermissionPage extends GuiLayer {
        LandClaim claim;
        PermMode mode;
        OfflinePlayer player;
        Entity entity;
        boolean isAdvanced = false;
        Permissions perms;

        void init() {
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(switch(mode) {
                    case ENTITY -> Component.text("Edit ").append(Names.instance.getEntityRenderedName(entity), Component.text("'s Perms: "), Component.text(claim.config.name).color(NamedTextColor.GOLD));
                    case OFFLINE_PLAYER -> Component.text("Edit ").append(Names.instance.getRenderedName(player, true), Component.text("'s Perms: "), Component.text(claim.config.name).color(NamedTextColor.GOLD));
                    case DEFAULT -> Component.text("Edit Default Player Permissions: ").append(Component.text(claim.config.name).color(NamedTextColor.GOLD));
                    case ENTITY_DEFAULT -> Component.text("Edit Default Entity Permissions: ").append(Component.text(claim.config.name).color(NamedTextColor.GOLD));
                });

                var l = new ArrayList<>();

                perms = switch(mode) {
                    case ENTITY -> claim.getPerms(entity.getUniqueId());
                    case OFFLINE_PLAYER -> claim.getPerms(player.getUniqueId());
                    case DEFAULT -> claim.defaultPerms;
                    case ENTITY_DEFAULT -> claim.defaultEntityPerms;
                };

                List<DialogInput> opts = new ArrayList<>();
                switch(mode) {
                    case DEFAULT, ENTITY_DEFAULT:
                        if (isAdvanced) {
                            for (var perm : AdvancedLandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("TRUE", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.TRUE);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("FALSE", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) != Claims.OptBool.TRUE);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : LandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("TRUE", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.TRUE);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("FALSE", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) != Claims.OptBool.TRUE);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(onEntry, offEntry)).build());
                            }
                        }
                        break;
                    case OFFLINE_PLAYER: {
                        if (isAdvanced) {
                            for (var perm : AdvancedLandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("TRUE", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.TRUE);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("FALSE", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.FALSE);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("DEFAULT", Component.text("Default (" + (claim.defaultPerms.getBoolPermission(perm) == Claims.OptBool.TRUE ? "On" : "Off") + ")").color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.DEFAULT);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : LandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("TRUE", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.TRUE);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("FALSE", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.FALSE);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("DEFAULT", Component.text("Default (" + (claim.defaultPerms.getBoolPermission(perm) == Claims.OptBool.TRUE ? "On" : "Off") + ")").color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.DEFAULT);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        }
                        break;
                    }
                    case ENTITY: {
                        if (isAdvanced) {
                            for (var perm : AdvancedLandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("TRUE", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.TRUE);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("FALSE", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.FALSE);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("DEFAULT", Component.text("Default (" + (claim.defaultEntityPerms.getBoolPermission(perm) == Claims.OptBool.TRUE ? "On" : "Off") + ")").color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.DEFAULT);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : LandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("TRUE", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.TRUE);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("FALSE", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.FALSE);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("DEFAULT", Component.text("Default (" + (claim.defaultEntityPerms.getBoolPermission(perm) == Claims.OptBool.TRUE ? "On" : "Off") + ")").color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.DEFAULT);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        }
                        break;
                    }
                }
                base.inputs(opts);

                var dia = builder.empty().base(base.build());

                dia.type(DialogType.multiAction(List.of(
                        ActionButton.create(
                                Component.text("Confirm", NamedTextColor.GREEN),
                                Component.text("Click to confirm your changes."),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    if (isAdvanced) {
                                        for (var opt : AdvancedLandPermission.values()) {
                                            perms.setBoolPermission(opt, Claims.OptBool.valueOf(Objects.requireNonNullElse(view.getText(opt.name()), "DEFAULT")));
                                        }
                                    } else {
                                        for (var opt : LandPermission.values()) {
                                            perms.setBoolPermission(opt, Claims.OptBool.valueOf(Objects.requireNonNullElse(view.getText(opt.name()), "DEFAULT")));
                                        }
                                    }
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        ),
                        ActionButton.create(
                                Component.text("Discard", NamedTextColor.RED),
                                Component.text("Click to discard your changes."),
                                100,
                                null
                        ),
                        isAdvanced ? ActionButton.create(
                                Component.text("Regular Options"),
                                Component.text("Click to edit regular options."),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    switch(mode) {
                                        case DEFAULT, ENTITY_DEFAULT -> new PermissionPage(who, claim, mode, false).show();
                                        case OFFLINE_PLAYER -> new PermissionPage(who, claim, player, false).show();
                                        case ENTITY -> new PermissionPage(who, claim, entity, false).show();
                                    }
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        ) : ActionButton.create(
                                Component.text("Advanced Options"),
                                Component.text("Click to edit advanced options."),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    switch(mode) {
                                        case DEFAULT, ENTITY_DEFAULT -> new PermissionPage(who, claim, mode, true).show();
                                        case OFFLINE_PLAYER -> new PermissionPage(who, claim, player, true).show();
                                        case ENTITY -> new PermissionPage(who, claim, entity, true).show();
                                    }
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        )
                ), null, 2));
            });
        }

        public PermissionPage(Player who, LandClaim claim, Entity target) {
            super(who);
            this.claim = claim;
            this.mode = PermMode.ENTITY;
            this.entity = target;
            init();
        }

        public PermissionPage(Player who, LandClaim claim, OfflinePlayer target) {
            super(who);
            this.claim = claim;
            this.mode = PermMode.OFFLINE_PLAYER;
            this.player = target;
            init();
        }

        public PermissionPage(Player who, LandClaim claim, PermMode mode) {
            super(who);
            this.claim = claim;
            this.mode = mode;
            init();
        }

        public PermissionPage(Player who, LandClaim claim, Entity target, boolean advanced) {
            super(who);
            this.claim = claim;
            this.mode = PermMode.ENTITY;
            this.entity = target;
            this.isAdvanced = advanced;
            init();
        }

        public PermissionPage(Player who, LandClaim claim, OfflinePlayer target, boolean advanced) {
            super(who);
            this.claim = claim;
            this.mode = PermMode.OFFLINE_PLAYER;
            this.player = target;
            this.isAdvanced = advanced;
            init();
        }

        public PermissionPage(Player who, LandClaim claim, PermMode mode, boolean advanced) {
            super(who);
            this.claim = claim;
            this.mode = mode;
            this.isAdvanced = advanced;
            init();
        }
    }
}
