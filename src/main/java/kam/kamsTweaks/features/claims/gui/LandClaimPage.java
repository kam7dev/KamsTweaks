package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.*;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.*;
import io.papermc.paper.registry.data.dialog.body.*;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.*;
import kam.kamsTweaks.utils.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.features.moderation.ChatFilter;
import kam.kamsTweaks.features.fun.Names;
import kam.kamsTweaks.features.claims.Claims;
import kam.kamsTweaks.features.claims.LandClaims.*;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class LandClaimPage extends GuiLayer {
    LandClaim target;

    void init() {
        if (target == null) {
            this.target = Claims.get().landClaims.getClaim(who.getLocation());
        }
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(KTStrings.getFor(KTStrings.LC));
            var dia = builder.empty().base(base.build());

            var btns = new ArrayList<ActionButton>();

            int totalClaims = 0;
            for (var claim : Claims.get().landClaims.claims) {
                if (claim.owner != null && who.getUniqueId().equals(claim.owner.getUniqueId()))
                    totalClaims += claim.slots;
            }

            if (totalClaims < KamsTweaks.get().getConfig().getInt("land-claims.max-claims", 30) && !Claims.get().landClaims.currentlyClaiming.containsKey(who)) {
                var createBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_CREATE)).action(DialogAction.customClick((view, audience) -> Claims.get().landClaims.startClaiming(who, true), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
                btns.add(createBtn);
            }

            if (Claims.get().landClaims.currentlyClaiming.containsKey(who)) {
                var createBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_CANCEL)).action(DialogAction.customClick((view, audience) -> Claims.get().landClaims.stopClaiming(who), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
                btns.add(createBtn);
            }

            if (target != null && target.getManagementType(who) != Claims.ManagementType.None) {
                var editBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_EDIT)).action(DialogAction.customClick((view, audience) -> new EditPage(who, target).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
                btns.add(editBtn);
            }

            var viewBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_VIEW_ALL)).action(DialogAction.customClick((view, audience) -> {
                Claims.get().landClaims.showClaims(who);
                who.sendMessage(KTStrings.getFor(KTStrings.CLAIM_HIGHLIGHTED));
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
            btns.add(viewBtn);

            var listBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_LIST)).action(DialogAction.customClick((view, audience) -> Claims.get().landClaims.listClaims(who), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
            btns.add(listBtn);

            var deleteBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_DELETE_ALL)).action(DialogAction.customClick((view, audience) -> new FLAlertLayer(who, KTStrings.getFor(KTStrings.CLAIMS_DELETE_ALL_TITLE, KTStrings.getFor(KTStrings.LC)),
                    KTStrings.getFor(KTStrings.CLAIM_DELETE_ALL_CONFIRM, KTStrings.getFor(KTStrings.LC)).append(Component.newline(), KTStrings.getFor(KTStrings.IRREVERSIBLE).color(NamedTextColor.RED)),
                    KTStrings.getFor(KTStrings.YES).color(NamedTextColor.GREEN), KTStrings.getFor(KTStrings.NO).color(NamedTextColor.RED), second -> {
                if (second) {
                    show();
                } else {
                    Claims.get().landClaims.claims.removeIf(claim -> claim.owner == who);
                    who.sendMessage(KTStrings.getFor(KTStrings.CLAIM_ALL_DELETED, KTStrings.getFor(KTStrings.LC)).color(NamedTextColor.GREEN));
                }
            }).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
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
                var base = DialogBase.builder(KTStrings.getFor(KTStrings.CLAIM_EDIT_TITLE, Component.text(claim.config.name).color(NamedTextColor.GOLD)));
                if (claim.owner == null || !claim.owner.getUniqueId().equals(who.getUniqueId())) {
                    base.body(List.of(DialogBody.plainMessage(KTStrings.getFor(KTStrings.CLAIM_OP_WARNING, claim.getOwnerName()).color(NamedTextColor.RED))));
                }
                var dia = builder.empty().base(base.build());

                var defaultBtn = ActionButton.builder(KTStrings.getFor(KTStrings.PERMS_DEFAULT_PLAYER))
                        .action(DialogAction.customClick((view, audience) -> new PermissionPage(who, claim, PermMode.DEFAULT).show(),
                                ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var entityBtn = ActionButton.builder(KTStrings.getFor(KTStrings.PERMS_DEFAULT_ENTITY))
                        .action(DialogAction.customClick((view, audience) -> new PermissionPage(who, claim, PermMode.ENTITY_DEFAULT).show(),
                                ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var permBtn = ActionButton.builder(KTStrings.getFor(KTStrings.PERMS_PLAYER))
                        .action(DialogAction.customClick((view, audience) -> new UserListPage(who, KTStrings.getFor(KTStrings.PERMS_EDIT,
                                        Component.text(claim.config.name).color(NamedTextColor.GOLD)),plr -> new PermissionPage(who, claim, plr).show()).show(),
                                ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var settingsBtn = ActionButton.builder(KTStrings.getFor(KTStrings.SETTINGS))
                        .action(DialogAction.customClick((view, audience) -> new SettingsPage(who, claim).show(),
                                ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var deleteBtn = ActionButton.builder(KTStrings.getFor(KTStrings.DELETE)).action(DialogAction.customClick((view, audience) -> new FLAlertLayer(who, KTStrings.getFor(KTStrings.CLAIM_DELETE_TITLE, KTStrings.getFor(KTStrings.LC)),
                        KTStrings.getFor(KTStrings.CLAIM_DELETE_CONFIRM, KTStrings.getFor(KTStrings.LAND)),
                        KTStrings.getFor(KTStrings.YES).color(NamedTextColor.GREEN), KTStrings.getFor(KTStrings.NO).color(NamedTextColor.RED), second -> {
                    if (second) {
                        show();
                    } else {
                        Claims.get().landClaims.deleteClaim(claim, who);
                    }
                }).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                dia.type(DialogType.multiAction(List.of(defaultBtn, entityBtn, permBtn, settingsBtn, deleteBtn), null, 1));
            });
        }
    }

    public static class SettingsPage extends GuiLayer {
        LandClaim claim;

        public SettingsPage(Player who, LandClaim claim) {
            super(who);
            this.claim = claim;
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(KTStrings.getFor(KTStrings.CLAIM_SETTINGS, Component.text(claim.config.name).color(NamedTextColor.GOLD)));

                var nameInp = DialogInput.text("name", KTStrings.getFor(KTStrings.NAME)).initial(claim.config.name).build();
                var prioDrag = DialogInput.numberRange("prio", KTStrings.getFor(KTStrings.PRIORITY), -100, 100).step(1f).initial(claim.config.priority.floatValue()).build();
                var testBox = DialogInput.bool("test", KTStrings.getFor(KTStrings.PERMS_TEST_MODE)).initial(claim.config.testMode).build();
                var pvpBox = DialogInput.bool("pvp", KTStrings.getFor(KTStrings.PVP)).initial(claim.config.pvp).build();
                base.inputs(List.of(nameInp, prioDrag, testBox, pvpBox));

                var dia = builder.empty().base(base.build());

                var confirmBtn = ActionButton.create(
                        KTStrings.getFor(KTStrings.CONFIRM).color(NamedTextColor.GREEN),
                        KTStrings.getFor(KTStrings.CONFIRM_DESC),
                        100,
                        DialogAction.customClick((view, audience) -> {
                            var mt = claim.getManagementType(who);
                            if (mt == Claims.ManagementType.None) {
                                who.sendMessage(KTStrings.getFor(KTStrings.CLAIM_CANT_MANAGE).color(NamedTextColor.RED));
                                return;
                            } else if (mt == Claims.ManagementType.Op) {
                                KamsTweaks.get().sendToOps(KTStrings.getFor(KTStrings.CLAIM_OP_EDIT_SETTINGS, Component.text(who.getName()), Component.text(claim.getOwnerUsername()), KTStrings.getFor(KTStrings.LAND)).decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), who);
                                Logger.warn("[Claim management] " + who.getName() + " just edited settings for " + claim.getOwnerUsername() + "'s land claim.");
                            }
                            claim.config.priority = Objects.requireNonNullElse(view.getFloat("prio"), 0).intValue();
                            claim.config.testMode = Objects.requireNonNullElse(view.getBoolean("test"), false);
                            if (Objects.requireNonNullElse(view.getBoolean("pvp"), false)) {
                                for (Player player : claim.getPlayers()) {
                                    player.sendMessage(KTStrings.getFor(KTStrings.LC_PVP_ENABLE_WARN, Component.text(5).color(NamedTextColor.RED)).color(NamedTextColor.YELLOW));
                                }
                                Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
                                    claim.config.pvp = true;
                                    for (Player player : claim.getPlayers()) {
                                        player.sendMessage(KTStrings.getFor(KTStrings.LC_PVP_ENABLE).color(NamedTextColor.GREEN));
                                    }
                                }, 20 * 5);
                            } else {
                                claim.config.pvp = false;
                                for (Player player : claim.getPlayers()) {
                                    player.sendMessage(KTStrings.getFor(KTStrings.LC_PVP_DISABLE).color(NamedTextColor.GREEN));
                                }
                            }

                            var name = Objects.requireNonNullElse(view.getText("name"), "Unnamed Claim");
                            var res = ChatFilter.instance.isFiltered(name);
                            if (res.first) {
                                ChatFilter.warnStaff(KTStrings.getFor(KTStrings.AUTOMOD_NAME, Component.text(who.getName()), Component.text(res.second.name), Component.text(name)));
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
                                KTStrings.getFor(KTStrings.DISCARD).color(NamedTextColor.RED),
                                KTStrings.getFor(KTStrings.DISCARD_DESC),
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

        void save(DialogResponseView view) {
            var mt = claim.getManagementType(who);
            if (mt == Claims.ManagementType.None) {
                who.sendMessage(KTStrings.getFor(KTStrings.CLAIM_CANT_MANAGE).color(NamedTextColor.RED));
                return;
            } else if (mt == Claims.ManagementType.Op) {
                KamsTweaks.get().sendToOps(KTStrings.getFor(KTStrings.CLAIM_OP_EDIT_PERMS, Component.text(who.getName()), Component.text(claim.getOwnerUsername()), KTStrings.getFor(KTStrings.LAND)).decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), who);
                Logger.warn("[Claim management] " + who.getName() + " just edited permissions for " + claim.getOwnerUsername() + "'s land claim.");
            }
            if (isAdvanced) {
                for (var opt : AdvancedLandPermission.values()) {
                    perms.setBoolPermission(opt, Claims.OptBool.valueOf(Objects.requireNonNullElse(view.getText(opt.name()), "Default")));
                }
            } else {
                for (var opt : LandPermission.values()) {
                    perms.setBoolPermission(opt, Claims.OptBool.valueOf(Objects.requireNonNullElse(view.getText(opt.name()), "Default")));
                }
            }
            perms.trusted = Objects.requireNonNullElse(view.getBoolean("trusted"), true);
        }

        void init() {
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(switch (mode) {
                    case ENTITY ->
                            KTStrings.getFor(KTStrings.PERMS_EDIT_ENTITY, Names.instance.getEntityRenderedName(entity), Component.text(claim.config.name).color(NamedTextColor.GOLD));
                    case OFFLINE_PLAYER ->
                            KTStrings.getFor(KTStrings.PERMS_EDIT_ENTITY, Names.instance.getRenderedName(player, true), Component.text(claim.config.name).color(NamedTextColor.GOLD));
                    case DEFAULT ->
                            KTStrings.getFor(KTStrings.PERMS_EDIT_DEFAULT, Component.text(claim.config.name).color(NamedTextColor.GOLD));
                    case ENTITY_DEFAULT ->
                            KTStrings.getFor(KTStrings.PERMS_EDIT_ENTITY_DEFAULT, Component.text(claim.config.name).color(NamedTextColor.GOLD));
                });

                var l = new ArrayList<>();

                perms = switch (mode) {
                    case ENTITY -> claim.getPerms(entity.getUniqueId());
                    case OFFLINE_PLAYER -> claim.getPerms(player.getUniqueId());
                    case DEFAULT -> claim.defaultPerms;
                    case ENTITY_DEFAULT -> claim.defaultEntityPerms;
                };

                List<DialogInput> opts = new ArrayList<>();
                switch (mode) {
                    case DEFAULT:
                        if (isAdvanced) {
                            for (var perm : AdvancedLandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) != Claims.OptBool.True);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : LandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) != Claims.OptBool.True);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(onEntry, offEntry)).build());
                            }
                        }
                        break;
                    case OFFLINE_PLAYER, ENTITY_DEFAULT: {
                        if (isAdvanced) {
                            for (var perm : AdvancedLandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", KTStrings.getFor(KTStrings.DEFAULT, KTStrings.getFor(claim.defaultPerms.getBoolPermission(perm) == Claims.OptBool.True ? KTStrings.ON : KTStrings.OFF)).color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : LandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", KTStrings.getFor(KTStrings.DEFAULT, KTStrings.getFor(claim.defaultPerms.getBoolPermission(perm) == Claims.OptBool.True ? KTStrings.ON : KTStrings.OFF)).color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        }
                        break;
                    }
                    case ENTITY: {
                        if (isAdvanced) {
                            for (var perm : AdvancedLandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", KTStrings.getFor(KTStrings.DEFAULT, KTStrings.getFor(claim.defaultEntityPerms.getBoolPermission(perm, claim.defaultPerms) == Claims.OptBool.True ? KTStrings.ON : KTStrings.OFF)).color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : LandPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", KTStrings.getFor(KTStrings.DEFAULT, KTStrings.getFor(claim.defaultEntityPerms.getBoolPermission(perm, claim.defaultPerms) == Claims.OptBool.True ? KTStrings.ON : KTStrings.OFF)).color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        }
                        break;
                    }
                }
                if (mode == PermMode.OFFLINE_PLAYER && (claim.owner == null || !player.getUniqueId().equals(claim.owner.getUniqueId()))) {
                    opts.add(DialogInput.bool("trusted", KTStrings.getFor(KTStrings.TRUSTED)).initial(perms.trusted).build());
                }
                base.inputs(opts);

                var dia = builder.empty().base(base.build());

                dia.type(DialogType.multiAction(List.of(
                        ActionButton.create(
                                KTStrings.getFor(KTStrings.CONFIRM).color(NamedTextColor.GREEN),
                                KTStrings.getFor(KTStrings.CONFIRM_DESC),
                                100,
                                DialogAction.customClick((view, audience) -> save(view), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        ),
                        ActionButton.create(
                                KTStrings.getFor(KTStrings.DISCARD).color(NamedTextColor.RED),
                                KTStrings.getFor(KTStrings.DISCARD_DESC),
                                100,
                                null
                        ),
                        isAdvanced ? ActionButton.create(
                                KTStrings.getFor(KTStrings.CLAIM_REGULAR),
                                KTStrings.getFor(KTStrings.CLAIM_REGULAR_DESC),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    save(view);
                                    switch (mode) {
                                        case DEFAULT, ENTITY_DEFAULT ->
                                                new PermissionPage(who, claim, mode, false).show();
                                        case OFFLINE_PLAYER -> new PermissionPage(who, claim, player, false).show();
                                        case ENTITY -> new PermissionPage(who, claim, entity, false).show();
                                    }
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        ) : ActionButton.create(
                                KTStrings.getFor(KTStrings.CLAIM_ADVANCED),
                                KTStrings.getFor(KTStrings.CLAIM_ADVANCED_DESC),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    save(view);
                                    switch (mode) {
                                        case DEFAULT, ENTITY_DEFAULT ->
                                                new PermissionPage(who, claim, mode, true).show();
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
