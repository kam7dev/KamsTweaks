package kam.kamsTweaks.features.claims.gui;

import io.papermc.paper.dialog.Dialog;
import kam.kamsTweaks.Logger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class GuiLayer {
    protected Dialog dialog;
    protected Player who;
    protected GuiLayer(Player who) {
        this.who = who;
    }
    public void show() {
        if (who == null) return;
        if (dialog == null) {
            Logger.error("Could not show dialog " + this.getClass().getName() + " for " + who.getName() + ": No dialog found.");
            return;
        }
        who.showDialog(dialog);
    }

    protected static Player getPlayer(Audience audience) {
        var uuid = audience.get(Identity.UUID);
        return uuid.map(Bukkit::getPlayer).orElse(null);
    }
}