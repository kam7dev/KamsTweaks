package kam.kamsTweaks;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public abstract class Feature implements Listener {
    public abstract void setup();
    public abstract void shutdown();
    public abstract void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands);
    public abstract void loadData();
    public abstract void saveData();
}