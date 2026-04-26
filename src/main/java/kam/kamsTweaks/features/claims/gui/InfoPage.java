package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.*;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.*;
import io.papermc.paper.registry.data.dialog.body.*;
import io.papermc.paper.registry.data.dialog.type.*;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.*;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class InfoPage extends GuiLayer {
    public InfoPage(Player who, GuiLayer next) {
        super(who);
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(Component.text("Claim Tool Info"));
            base.body(List.of(DialogBody.plainMessage(Component.text("""
                Welcome to the Claim Tool! This explanation will guide you through its usage.
                
                To create a land claim, you must select an area by right-clicking two different blocks (or the same block twice if you want). Claims do not extend upward or downward infinitely, so to claim a tall build you need to select the lowest point and the highest point as well.
                
                You can also claim mobs, armor stands, boats, etc. These are entity claims.
                
                You are able to edit your claims to set the default permissions (which apply to all players) and give specific permissions for specific players, or set the behavior of the claimed entity. You can also change the name of your land claims and set their priority.
                
                Server operators are able to edit or delete your claims at any time if they need to."""), 400)));
            var dia = builder.empty().base(base.build());

            // back button
            dia.type(DialogType.notice(ActionButton.builder(Component.text("Close")).action(DialogAction.customClick((view, audience) -> {
                next.show();
            }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build()));
        });
    }
}
