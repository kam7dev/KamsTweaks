package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.*;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.*;
import io.papermc.paper.registry.data.dialog.body.*;
import io.papermc.paper.registry.data.dialog.type.*;
import kam.kamsTweaks.utils.KTStrings;
import net.kyori.adventure.text.event.*;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class InfoPage extends GuiLayer {
    public InfoPage(Player who, GuiLayer next) {
        super(who);
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(KTStrings.getFor(KTStrings.CLAIM_TOOL_INFO));
            base.body(List.of(DialogBody.plainMessage(KTStrings.getFor(KTStrings.CLAIM_DESC), 400)));
            var dia = builder.empty().base(base.build());

            dia.type(DialogType.notice(ActionButton.builder(KTStrings.getFor(KTStrings.CLOSE)).action(DialogAction.customClick((view, audience) -> next.show(), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build()));
        });
    }
}
