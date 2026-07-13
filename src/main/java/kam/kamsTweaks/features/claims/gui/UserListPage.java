package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import kam.kamsTweaks.features.fun.nicknames.Names;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class UserListPage extends GuiLayer {
    public UserListPage(Player who, Component title, Consumer<OfflinePlayer> callback) {
        super(who);
        dialog = Dialog.create(builder -> {
            var base = DialogBase.builder(title);

            var plrs = Bukkit.getOfflinePlayers();
            Arrays.sort(plrs, Comparator.comparing(OfflinePlayer::getName, Comparator.nullsLast(String::compareTo)));
            List<ActionButton> btns = new ArrayList<>();
            for (var plr : plrs) {
                var name = Names.getName(plr, true);
                var btn = ActionButton.builder(name);
                btn.action(DialogAction.customClick((view, audience) -> callback.accept(plr), ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).lifetime(ClickCallback.DEFAULT_LIFETIME).build()));
                btns.add(btn.build());
            }

            var dia = builder.empty().base(base.build());
            dia.type(DialogType.multiAction(btns, null, 3));
        });
    }
}
