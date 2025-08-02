package kam.kamsTweaks.features.teleportation;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.jetbrains.annotations.NotNull;

public class SetHome {
    TeleportationHandler handler;
    public void init(TeleportationHandler handler) {
        this.handler = handler;
    }

    public void saveHomes() {

    }

    public void loadHomes() {

    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {

    }

    public static class Home {}
}
