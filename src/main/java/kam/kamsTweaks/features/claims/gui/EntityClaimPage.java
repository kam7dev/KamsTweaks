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
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import kam.kamsTweaks.features.Names;
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
            var base = DialogBase.builder(Component.text("Entity Claims"));
            var dia = builder.empty().base(base.build());

            List<ActionButton> btns = new ArrayList<>();

            var claim = Claims.get().entityClaims.getClaim(target);
            if (claim != null && claim.getManagementType(who) != Claims.ManagementType.None) {
                var editBtn = ActionButton.builder(Component.text("Edit Claim")).action(DialogAction.customClick((view, audience) -> {
                    new EditPage(who, target).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
                btns.add(editBtn);
            }

            var listBtn = ActionButton.builder(Component.text("List Your Claims")).action(DialogAction.customClick((view, audience) -> {
                Claims.get().entityClaims.listClaims(who);
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
            btns.add(listBtn);

            var deleteBtn = ActionButton.builder(Component.text("Delete ALL of Your Claims")).action(DialogAction.customClick((view, audience) -> {
                new FLAlertLayer(who, Component.text("Delete ALL of your entity claims?"),
                        Component.text("Are you sure you want to ALL of your entity claims?").append(Component.text(" This is irreversible.", NamedTextColor.RED)),
                        Component.text("Yes").color(NamedTextColor.GREEN), Component.text("No").color(NamedTextColor.RED), second -> {
                    if (second) {
                        show();
                    } else {
                        Claims.get().entityClaims.claims.entrySet().removeIf(entry -> entry.getValue().owner != null && who.getUniqueId().equals(entry.getValue().owner.getUniqueId()));
                        who.sendMessage(Component.text("Deleted all of your claims successfully.").color(NamedTextColor.GREEN));
                    }
                }).show();
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
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
                var base = DialogBase.builder(Component.text("Edit Claim: ").append(Names.instance.getEntityRenderedName(target).color(NamedTextColor.GOLD), Component.text(" ("), Component.translatable(target.getType().translationKey()), Component.text(")")));
                if (claim.owner == null || !claim.owner.getUniqueId().equals(who.getUniqueId())) {
                    base.body(List.of(DialogBody.plainMessage(
                            Component.text("Careful! This claim is owned by ").append(
                                    claim.getOwnerName(),
                                    Component.text("!\nYou can only edit this because you are an operator.")
                            ).color(NamedTextColor.RED))));
                }
                var dia = builder.empty().base(base.build());

                var defaultBtn = ActionButton.builder(Component.text("Default Player Permissions")).action(DialogAction.customClick((view, audience) -> {
                    new PermissionPage(who, target, EntityClaimPage.PermMode.DEFAULT).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var entityBtn = ActionButton.builder(Component.text("Default Entity Permissions")).action(DialogAction.customClick((view, audience) -> {
                    new PermissionPage(who, target, EntityClaimPage.PermMode.ENTITY_DEFAULT).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var permBtn = ActionButton.builder(Component.text("Player Permissions")).action(DialogAction.customClick((view, audience) -> {
                    new UserListPage(who, Component.text("Edit Permissions: ").append(Names.instance.getEntityRenderedName(target).color(NamedTextColor.GOLD)), plr -> {
                        new PermissionPage(who, target, plr).show();
                    }).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var settingsBtn = ActionButton.builder(Component.text("Settings")).action(DialogAction.customClick((view, audience) -> {
                    new SettingsPage(who, target).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var deleteBtn = ActionButton.builder(Component.text("Delete")).action(DialogAction.customClick((view, audience) -> {
                    new FLAlertLayer(who, Component.text("Delete claim?"),
                            Component.text("Are you sure you want to delete this entity claim?"),
                            Component.text("Yes").color(NamedTextColor.GREEN), Component.text("No").color(NamedTextColor.RED), second -> {
                        if (second) {
                            show();
                        } else {
                            Claims.get().entityClaims.deleteClaim(claim, who);
                        }
                    }).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

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
                var base = DialogBase.builder(Component.text("Edit Claim Settings: ").append(Names.instance.getEntityRenderedName(target).color(NamedTextColor.GOLD)));

                var aggroBtn = DialogInput.bool("aggro", Component.text("Can Aggro")).initial(claim.config.canAggro).build();

                base.inputs(List.of(aggroBtn));

                var dia = builder.empty().base(base.build());

                var confirmBtn = ActionButton.create(
                        Component.text("Confirm", NamedTextColor.GREEN),
                        Component.text("Click to confirm your changes."),
                        100,
                        DialogAction.customClick((view, audience) -> {
                            var mt = claim.getManagementType(who);
                            if (mt == Claims.ManagementType.None) {
                                who.sendMessage(Component.text("You cannot manage this claim.").color(NamedTextColor.RED));
                                return;
                            } else if (mt == Claims.ManagementType.Op) {
                                KamsTweaks.get().sendToOps(Component.text("[" + who.getName() + ": Edited settings for " + claim.getOwnerUsername() + "'s entity claim]").decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), who);
                                Logger.warn("[Claim management] " + who.getName() + " just deleted " + claim.getOwnerUsername() + "'s entity claim.");
                            }
                            claim.config.canAggro = Objects.requireNonNullElse(view.getBoolean("prio"), false);
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
                who.sendMessage(Component.text("You cannot manage this claim.").color(NamedTextColor.RED));
                return;
            } else if (mt == Claims.ManagementType.Op) {
                KamsTweaks.get().sendToOps(Component.text("[" + who.getName() + ": Edited permissions for " + claim.getOwnerUsername() + "'s entity claim]").decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY), who);
                Logger.warn("[Claim management] " + who.getName() + " just deleted " + claim.getOwnerUsername() + "'s entity claim.");
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
                    case ENTITY -> Component.text("Edit ").append(Names.instance.getEntityRenderedName(entity), Component.text("'s Perms: "), Names.instance.getEntityRenderedName(target).color(NamedTextColor.GOLD));
                    case OFFLINE_PLAYER -> Component.text("Edit ").append(Names.instance.getRenderedName(player, true), Component.text("'s Perms: "), Names.instance.getEntityRenderedName(target).color(NamedTextColor.GOLD));
                    case DEFAULT -> Component.text("Edit Default Player Permissions: ").append(Names.instance.getEntityRenderedName(target).color(NamedTextColor.GOLD));
                    case ENTITY_DEFAULT -> Component.text("Edit Default Entity Permissions: ").append(Names.instance.getEntityRenderedName(target).color(NamedTextColor.GOLD));
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
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) != Claims.OptBool.True);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : EntityClaims.EntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) != Claims.OptBool.True);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(onEntry, offEntry)).build());
                            }
                        }
                        break;
                    case OFFLINE_PLAYER, ENTITY_DEFAULT: {
                        if (isAdvanced) {
                            for (var perm : EntityClaims.AdvancedEntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", Component.text("Default (" + (claim.defaultPerms.getBoolPermission(perm) == Claims.OptBool.True ? "On" : "Off") + ")").color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : EntityClaims.EntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", Component.text("Default (" + (claim.defaultPerms.getBoolPermission(perm) == Claims.OptBool.True ? "On" : "Off") + ")").color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        }
                        break;
                    }
                    case ENTITY: {
                        if (isAdvanced) {
                            for (var perm : EntityClaims.AdvancedEntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", Component.text("Default (" + (claim.defaultEntityPerms.getBoolPermission(perm, claim.defaultPerms) == Claims.OptBool.True ? "On" : "Off") + ")").color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
                                opts.add(DialogInput.singleOption(perm.name(), Component.text(perm.label), List.of(defaultEntry, onEntry, offEntry)).build());
                            }
                        } else {
                            for (var perm : EntityClaims.EntityPermission.values()) {
                                var onEntry = SingleOptionDialogInput.OptionEntry.create("True", Component.text("On").color(NamedTextColor.GREEN), perms.getBoolPermission(perm) == Claims.OptBool.True);
                                var offEntry = SingleOptionDialogInput.OptionEntry.create("False", Component.text("Off").color(NamedTextColor.RED), perms.getBoolPermission(perm) == Claims.OptBool.False);
                                var defaultEntry = SingleOptionDialogInput.OptionEntry.create("Default", Component.text("Default (" + (claim.defaultEntityPerms.getBoolPermission(perm, claim.defaultPerms) == Claims.OptBool.True ? "On" : "Off") + ")").color(NamedTextColor.YELLOW), perms.getBoolPermission(perm) == Claims.OptBool.Default);
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
                                    save(view);
                                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())
                        ),
                        ActionButton.create(
                                Component.text("Discard", NamedTextColor.RED),
                                Component.text("Click to discard your changes."),
                                100,
                                null
                        )/*,
                        isAdvanced ? ActionButton.create(
                                Component.text("Regular Options"),
                                Component.text("Click to edit regular options."),
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
                                Component.text("Advanced Options"),
                                Component.text("Click to edit advanced options."),
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
