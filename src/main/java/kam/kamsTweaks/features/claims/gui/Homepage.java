package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.*;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.*;
import io.papermc.paper.registry.data.dialog.type.*;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.event.*;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class Homepage extends GuiLayer {
    public Homepage(Player who) {
        super(who);
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(KTStrings.getFor(KTStrings.CLAIMS));
            var dia = builder.empty().base(base.build());

            var lcBtn = ActionButton.builder(KTStrings.getFor(KTStrings.LC)).action(DialogAction.customClick((view, audience) -> new LandClaimPage(who).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

            var ecBtn = ActionButton.builder(KTStrings.getFor(KTStrings.EC)).action(DialogAction.customClick((view, audience) -> new EntityClaimPage(who).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

            var infoBtn = ActionButton.builder(KTStrings.getFor(KTStrings.CLAIM_TOOL_INFO)).action(DialogAction.customClick((view, audience) -> new InfoPage(who, this).show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();

            dia.type(DialogType.multiAction(List.of(lcBtn, ecBtn, infoBtn), null, 1));
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
}
