package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.*;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.*;
import io.papermc.paper.registry.data.dialog.body.*;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.*;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.ChatFilter;
import kam.kamsTweaks.features.claims.Claims;
import kam.kamsTweaks.features.claims.LandClaims.*;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class LandClaimPage extends GuiLayer {
    public LandClaimPage(Player who) {
        super(who);
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(Component.text("Land Claims"));
            var dia = builder.empty().base(base.build());

            var createBtn = ActionButton.builder(Component.text("Create a Claim")).action(DialogAction.customClick((view, audience) -> {

            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

            var editBtn = ActionButton.builder(Component.text("Edit Claim")).action(DialogAction.customClick((view, audience) -> {
                new EditPage(who, Claims.get().landClaims.claims.getFirst()).show();
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

            var viewBtn = ActionButton.builder(Component.text("View All Claims")).action(DialogAction.customClick((view, audience) -> {

            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

            var listBtn = ActionButton.builder(Component.text("List Your Claims")).action(DialogAction.customClick((view, audience) -> {

            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

            var deleteBtn = ActionButton.builder(Component.text("Delete ALL of Your Claims")).action(DialogAction.customClick((view, audience) -> {

            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

            dia.type(DialogType.multiAction(List.of(createBtn, editBtn, viewBtn, listBtn, deleteBtn), null, 1));
        });
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
                var base = DialogBase.builder(Component.text("Edit Claim:\n").append(Component.text(claim.config.name).color(NamedTextColor.GOLD)));
                if (claim.owner == null || claim.owner.getUniqueId() != who.getUniqueId()) {
                    base.body(List.of(DialogBody.plainMessage(
                            Component.text("Careful! This claim is owned by ").append(
                                    claim.getOwnerName(),
                                    Component.text("!\nYou can only edit this because you are an operator.")
                            ).color(NamedTextColor.RED))));
                }
                var dia = builder.empty().base(base.build());

                var editBtn = ActionButton.builder(Component.text("Permissions")).action(DialogAction.customClick((view, audience) -> {

                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var settingsBtn = ActionButton.builder(Component.text("Settings")).action(DialogAction.customClick((view, audience) -> {
                    new SettingsPage(who, claim).show();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                var deleteBtn = ActionButton.builder(Component.text("Delete")).action(DialogAction.customClick((view, audience) -> {

                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

                dia.type(DialogType.multiAction(List.of(editBtn, settingsBtn, deleteBtn), null, 1));
            });
        }
    }

    public static class SettingsPage extends GuiLayer {
        LandClaim claim;
        public SettingsPage(Player who, LandClaim claim) {
            super(who);
            this.claim = claim;
            dialog = Dialog.create(builder -> {
                var base = DialogBase.builder(Component.text("Edit Claim Settings:\n").append(Component.text(claim.config.name).color(NamedTextColor.GOLD)));

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
}
