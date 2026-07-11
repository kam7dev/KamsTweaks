package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;
import kam.kamsTweaks.features.fun.nicknames.Names;
import kam.kamsTweaks.features.claims.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class EntityClaimPage extends GuiLayer {
    Entity target;
    
    void init() {
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(KTStrings.getFor(KTStrings.EC));
            var dia = builder.empty().base(base.build());

            List<ActionButton> btns = new ArrayList<>();

            var claim = Claims.get().entityClaims.getClaim(target);
            if (claim != null && claim.getManagementType(who) != Claims.ManagementType.None) {
                var editBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_EDIT)).action(DialogAction.customClick((view, audience) -> new EditPage(who, target).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
                btns.add(editBtn);
            }

            var listBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_LIST)).action(DialogAction.customClick((view, audience) -> Claims.get().entityClaims.listClaims(who), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
            btns.add(listBtn);

            var deleteBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_DELETE_ALL)).action(DialogAction.customClick((view, audience) -> new FLAlertLayer(who, KTStrings.getFor(KTStrings.CLAIMS_DELETE_ALL_TITLE, KTStrings.getFor(KTStrings.EC)),
                    KTStrings.getFor(KTStrings.CLAIM_DELETE_ALL_CONFIRM, KTStrings.getFor(KTStrings.EC)).append(Component.newline(), KTStrings.getFor(KTStrings.IRREVERSIBLE).color(NamedTextColor.RED)),
                    KTStrings.getFor(KTStrings.YES).color(NamedTextColor.GREEN), KTStrings.getFor(KTStrings.NO).color(NamedTextColor.RED), second -> {
                if (second) {
                    show();
                } else {
                    Claims.get().entityClaims.claims.entrySet().removeIf(entry -> entry.getValue().owner != null && who.getUniqueId().equals(entry.getValue().owner.getUniqueId()));
                    who.sendMessage(KTStrings.getFor(KTStrings.CLAIM_ALL_DELETED, KTStrings.getFor(KTStrings.EC)).color(NamedTextColor.GREEN));
                }
            }).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
            btns.add(deleteBtn);

            dia.type(DialogType.multiAction(btns, null, 1));
        });
    }
    
    public EntityClaimPage(Player who) {
        super(who);
        init();
    }

    public EntityClaimPage(Player who, Entity target) {
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
        Entity target;
        public EditPage(Player who, Entity target) {
            super(who);
            this.target = target;
            dialog = Dialog.create(builder -> {
                var claim = Claims.get().entityClaims.getClaim(target);
                if (claim == null) return;
                var base = DialogBase.builder(KTStrings.getFor(KTStrings.CLAIM_EDIT_TITLE, Names.getEName(target).color(NamedTextColor.GOLD)));
                if (claim.owner == null || !claim.owner.getUniqueId().equals(who.getUniqueId())) {
                    base.body(List.of(DialogBody.plainMessage(KTStrings.getFor(KTStrings.CLAIM_OP_WARNING, claim.getOwnerName()).color(NamedTextColor.RED))));
                }
                var dia = builder.empty().base(base.build());

                var defaultBtn = ActionButton.builder(KTStrings.getFor(KTStrings.PERMS_DEFAULT_PLAYER)).action(DialogAction.customClick((view, audience) -> new PermissionPage(who, target, PermMode.DEFAULT).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var entityBtn = ActionButton.builder(KTStrings.getFor(KTStrings.PERMS_DEFAULT_ENTITY)).action(DialogAction.customClick((view, audience) -> new PermissionPage(who, target, PermMode.ENTITY_DEFAULT).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var permBtn = ActionButton.builder(KTStrings.getFor(KTStrings.PERMS_PLAYER)).action(DialogAction.customClick((view, audience) -> new UserListPage(who, KTStrings.getFor(KTStrings.PERMS_EDIT, Names.getEName(target).color(NamedTextColor.GOLD)), plr -> new PermissionPage(who, target, plr).show()).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var settingsBtn = ActionButton.builder(KTStrings.getFor(KTStrings.SETTINGS)).action(DialogAction.customClick((view, audience) -> new SettingsPage(who, target).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var deleteBtn = ActionButton.builder(KTStrings.getFor(KTStrings.DELETE)).action(DialogAction.customClick((view, audience) -> new FLAlertLayer(who, KTStrings.getFor(KTStrings.CLAIM_DELETE_TITLE, KTStrings.getFor(KTStrings.EC)),
                        KTStrings.getFor(KTStrings.CLAIM_DELETE_CONFIRM, KTStrings.getFor(KTStrings.ENTITY)),
                        KTStrings.getFor(KTStrings.YES).color(NamedTextColor.GREEN), KTStrings.getFor(KTStrings.NO).color(NamedTextColor.RED), second -> {
                    if (second) {
                        show();
                    } else {
                        Claims.get().entityClaims.deleteClaim(claim, who);
                    }
                }).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                dia.type(DialogType.multiAction(List.of(defaultBtn, entityBtn, permBtn, settingsBtn, deleteBtn), null, 1));
            });
        }
    }

    public static class SettingsPage extends GuiLayer {
        Entity target;
        public SettingsPage(Player who, Entity target) {
            super(who);
            this.target = target;
            var claim = Claims.get().entityClaims.getClaim(target);
            if (claim == null) return;
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(KTStrings.getFor(KTStrings.CLAIM_SETTINGS, Names.getEName(target).color(NamedTextColor.GOLD)));
                var aggroBtn = DialogInput.bool("aggro", KTStrings.getFor(KTStrings.EC_AGGRO)).initial(claim.config.canAggro).build();
                var testBtn = DialogInput.bool("test", KTStrings.getFor(KTStrings.PERMS_TEST_MODE)).initial(claim.config.testMode).build();

                base.inputs(List.of(aggroBtn, testBtn));

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
                                KamsTweaks.get().sendToOps(KTStrings.getFor(KTStrings.CLAIM_OP_EDIT_SETTINGS, Component.text(who.getName()), Component.text(claim.getOwnerUsername()), KTStrings.getFor(KTStrings.ENTITY)).decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), who);
                                Logger.warn("[Claim management] " + who.getName() + " just edited settings for " + claim.getOwnerUsername() + "'s entity claim.");
                            }
                            claim.config.canAggro = Objects.requireNonNullElse(view.getBoolean("aggro"), false);
                            claim.config.testMode = Objects.requireNonNullElse(view.getBoolean("test"), false);
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

    public static class PermissionPage extends GuiLayer {
        Entity target;
        EntityClaims.EntityClaim claim;
        EntityClaimPage.PermMode mode;
        OfflinePlayer player;
        Entity entity;
        boolean isAdvanced = false;
        EntityClaims.Permissions perms;

        void save(DialogResponseView view) {
            var mt = claim.getManagementType(who);
            if (mt == Claims.ManagementType.None) {
                who.sendMessage(KTStrings.getFor(KTStrings.CLAIM_CANT_MANAGE).color(NamedTextColor.RED));
                return;
            } else if (mt == Claims.ManagementType.Op) {
                KamsTweaks.get().sendToOps(KTStrings.getFor(KTStrings.CLAIM_OP_EDIT_PERMS, Component.text(who.getName()), Component.text(claim.getOwnerUsername()), KTStrings.getFor(KTStrings.ENTITY)).decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), who);
                Logger.warn("[Claim management] " + who.getName() + " just edited permissions for " + claim.getOwnerUsername() + "'s entity claim.");
            }
            if (isAdvanced) {
                for (var opt : EntityClaims.AdvancedEntityPermission.values()) {
                    perms.setBoolPermission(opt, Claims.OptBool.valueOf(Objects.requireNonNullElse(view.getText(opt.name()), "Default")));
                }
            } else {
                for (var opt : EntityClaims.EntityPermission.values()) {
                    perms.setBoolPermission(opt, Claims.OptBool.valueOf(Objects.requireNonNullElse(view.getText(opt.name()), "Default")));
                }
            }
        }

        void init() {
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(switch(mode) {
                    case ENTITY -> KTStrings.getFor(KTStrings.PERMS_EDIT_ENTITY, Names.getEName(entity), Names.getEName(target).color(NamedTextColor.GOLD));
                    case OFFLINE_PLAYER -> KTStrings.getFor(KTStrings.PERMS_EDIT_ENTITY, Names.getName(player), Names.getEName(target).color(NamedTextColor.GOLD));
                    case DEFAULT -> KTStrings.getFor(KTStrings.PERMS_EDIT_DEFAULT, Names.getEName(target).color(NamedTextColor.GOLD));
                    case ENTITY_DEFAULT -> KTStrings.getFor(KTStrings.PERMS_EDIT_ENTITY_DEFAULT, Names.getEName(target).color(NamedTextColor.GOLD));
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
                    case DEFAULT:
                        if (isAdvanced) {
                            for (var perm : EntityClaims.AdvancedEntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) != Claims.OptBool.True);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : EntityClaims.EntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) != Claims.OptBool.True);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(onEntry, offEntry)).build());
                            }
                        }
                        break;
                    case OFFLINE_PLAYER, ENTITY_DEFAULT: {
                        if (isAdvanced) {
                            for (var perm : EntityClaims.AdvancedEntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", KTStrings.getFor(KTStrings.DEFAULT, KTStrings.getFor(claim.defaultPerms.getBoolPermission(perm) == Claims.OptBool.True ? KTStrings.ON : KTStrings.OFF)).color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : EntityClaims.EntityPermission.values()) {
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
                            for (var perm : EntityClaims.AdvancedEntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", KTStrings.getFor(KTStrings.DEFAULT, KTStrings.getFor(claim.defaultEntityPerms.getBoolPermission(perm, claim.defaultPerms) == Claims.OptBool.True ? KTStrings.ON : KTStrings.OFF)).color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : EntityClaims.EntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", KTStrings.getFor(KTStrings.ON).color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", KTStrings.getFor(KTStrings.OFF).color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", KTStrings.getFor(KTStrings.DEFAULT, KTStrings.getFor(claim.defaultEntityPerms.getBoolPermission(perm, claim.defaultPerms) == Claims.OptBool.True ? KTStrings.ON : KTStrings.OFF)).color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), perm.label, List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        }
                        break;
                    }
                }
                base.inputs(opts);

                var dia = builder.empty().base(base.build());

                dia.type(DialogType.multiAction(List.of(
                        ActionButton.create(
                                KTStrings.getFor(KTStrings.CONFIRM).color( NamedTextColor.GREEN),
                                KTStrings.getFor(KTStrings.CONFIRM_DESC),
                                100,
                                DialogAction.customClick((view, audience) -> save(view), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        ),
                        ActionButton.create(
                                KTStrings.getFor(KTStrings.DISCARD).color(NamedTextColor.RED),
                                KTStrings.getFor(KTStrings.DISCARD_DESC),
                                100,
                                null
                        )/*,
                        isAdvanced ? ActionButton.create(
                                KTStrings.getFor(KTStrings.CLAIM_REGULAR),
                                KTStrings.getFor(KTStrings.CLAIM_REGULAR_DESC),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    save(view);
                                    switch(mode) {
                                        case DEFAULT, ENTITY_DEFAULT -> new EntityClaimPage.PermissionPage(who, target, mode, false).show();
                                        case OFFLINE_PLAYER -> new EntityClaimPage.PermissionPage(who, target, player, false).show();
                                        case ENTITY -> new EntityClaimPage.PermissionPage(who, target, entity, false).show();
                                    }
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        ) : ActionButton.create(
                                KTStrings.getFor(KTStrings.CLAIM_ADVANCED),
                                KTStrings.getFor(KTStrings.CLAIM_ADVANCED_DESC),
                                100,
                                DialogAction.customClick((view, audience) -> {
                                    save(view);
                                    switch(mode) {
                                        case DEFAULT, ENTITY_DEFAULT -> new EntityClaimPage.PermissionPage(who, target, mode, true).show();
                                        case OFFLINE_PLAYER -> new EntityClaimPage.PermissionPage(who, target, player, true).show();
                                        case ENTITY -> new EntityClaimPage.PermissionPage(who, target, entity, true).show();
                                    }
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        )*/
                ), null, 2));
            });
        }

        public PermissionPage(Player who, Entity entity, Entity tgt) {
            super(who);
            this.target = entity;
            this.claim = Claims.get().entityClaims.getClaim(entity);
            this.mode = EntityClaimPage.PermMode.ENTITY;
            this.entity = tgt;
            init();
        }

        public PermissionPage(Player who, Entity entity, OfflinePlayer tgt) {
            super(who);
            this.target = entity;
            this.claim = Claims.get().entityClaims.getClaim(entity);
            this.mode = EntityClaimPage.PermMode.OFFLINE_PLAYER;
            this.player = tgt;
            init();
        }

        public PermissionPage(Player who, Entity entity, EntityClaimPage.PermMode mode) {
            super(who);
            this.target = entity;
            this.claim = Claims.get().entityClaims.getClaim(entity);
            this.mode = mode;
            init();
        }

        public PermissionPage(Player who, Entity entity, Entity tgt, boolean advanced) {
            super(who);
            this.target = entity;
            this.claim = Claims.get().entityClaims.getClaim(entity);
            this.mode = EntityClaimPage.PermMode.ENTITY;
            this.entity = tgt;
            this.isAdvanced = advanced;
            init();
        }

        public PermissionPage(Player who, Entity entity, OfflinePlayer tgt, boolean advanced) {
            super(who);
            this.target = entity;
            this.claim = Claims.get().entityClaims.getClaim(entity);
            this.mode = EntityClaimPage.PermMode.OFFLINE_PLAYER;
            this.player = tgt;
            this.isAdvanced = advanced;
            init();
        }

        public PermissionPage(Player who, Entity entity, EntityClaimPage.PermMode mode, boolean advanced) {
            super(who);
            this.target = entity;
            this.claim = Claims.get().entityClaims.getClaim(entity);
            this.mode = mode;
            this.isAdvanced = advanced;
            init();
        }
    }

    public enum PermMode {
        DEFAULT,
        ENTITY_DEFAULT,
        ENTITY,
        OFFLINE_PLAYER,
    }
}
