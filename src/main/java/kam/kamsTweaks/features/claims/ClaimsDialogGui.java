package kam.kamsTweaks.features.claims;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

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
            if (claim.owner.equals(who)) totalClaims++;
        }
        if (!claims.currentlyClaiming.contains(who)) {
            if (totalClaims < KamsTweaks.getInstance().getConfig().getInt("land-claims.max-claims")) {
                btns.add(ActionButton.builder(Component.text("Create a Claim")).action(DialogAction.customClick(
                        (view, audience) -> {
                            if (audience instanceof Player player) {
                                player.sendMessage(Component.text("Right click the first corner of where you want to claim with your claim tool. (If you lost it, run ").append(Component.text("/claims get-tool").clickEvent(ClickEvent.runCommand("claims get-tool")).color(NamedTextColor.YELLOW).decorate(TextDecoration.UNDERLINED), Component.text(")").color(NamedTextColor.GOLD)).color(NamedTextColor.GOLD));
                                claims.currentlyClaiming.add(player);
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
                            if (claim.owner.equals(player)) {
                                i++;
                                msg = msg.append(Component.text("\n"),
                                        Component.text(claim.name).color(NamedTextColor.AQUA),
                                        Component.text(": "),
                                        Component.text(claim.start.getBlockX() + ", " + claim.start.getBlockY() + ", " + claim.start.getBlockZ()).color(NamedTextColor.GREEN),
                                        Component.text(" in "),
                                        Component.text(claim.start.getWorld().getName()).color(NamedTextColor.LIGHT_PURPLE));
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
}
