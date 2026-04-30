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
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class FLAlertLayer extends GuiLayer {
    public void init(Component title, Component body, ActionButton first, ActionButton second) {
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(title);
            base.body(List.of(DialogBody.plainMessage(body, 400)));
            var dia = builder.empty().base(base.build());
            dia.type(DialogType.confirmation(first, second));
        });
    }
    public FLAlertLayer(Player who, Component title, Component body, ActionButton first, ActionButton second) {
        super(who);
        init(title, body, first, second);
    }
    public FLAlertLayer(Player who, Component title, Component body, Component first, Component second, Consumer<Boolean> callback) {
        super(who);
        var firstBtn = ActionButton.builder(first).action(DialogAction.customClick((view, audience) -> {
            callback.accept(false);
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
        var secondBtn = ActionButton.builder(second).action(DialogAction.customClick((view, audience) -> {
            callback.accept(true);
        }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build())).build();
        init(title, body, firstBtn, secondBtn);
    }
}
