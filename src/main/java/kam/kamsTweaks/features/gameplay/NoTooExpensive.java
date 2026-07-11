package kam.kamsTweaks.features.gameplay;

import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareAnvilEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NoTooExpensive extends Feature {
    List<UUID> hasMessaged = new ArrayList<>();

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        var view = event.getView();
        if (!Config.getBool("no-too-expensive.enabled", true)) return;
        view.setMaximumRepairCost(Config.getInt("no-too-expensive.new-limit",  Integer.MAX_VALUE));
        if (!Config.getBool("no-too-expensive.use-modified-curve", true)) return;
        if (view.getRepairCost() > 39) {
            var old = view.getRepairCost();
            view.setRepairCost((int) (33 + (Math.log(view.getRepairCost()) * 2)));
            event.getViewers().forEach(v -> {
                if (v instanceof Player player) {
                    if (hasMessaged.contains(player.getUniqueId())) return;
                    hasMessaged.add(player.getUniqueId());
                    player.sendMessage(KTStrings.getFor(KTStrings.TOO_EXPENSIVE_FIX, Component.text("Anvil Too Expensive Fix").decorate(TextDecoration.UNDERLINED).color(NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl("https://modrinth.com/mod/anvil-too-expensive-fix"))).color(NamedTextColor.YELLOW));
                }
            });
        }
    }
}
