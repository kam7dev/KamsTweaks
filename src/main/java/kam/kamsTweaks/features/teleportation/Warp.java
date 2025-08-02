package kam.kamsTweaks.features.teleportation;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.jetbrains.annotations.NotNull;

public class Warp {
    TeleportationHandler handler;
    public void init(TeleportationHandler handler) {
        this.handler = handler;
    }

    public void saveWarps() {

    }

    public void loadWarps() {

    }

    @SuppressWarnings("UnstableApiUsage")
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {

    }
}
