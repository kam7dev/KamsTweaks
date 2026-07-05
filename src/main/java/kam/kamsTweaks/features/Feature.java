package kam.kamsTweaks.features;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public abstract class Feature implements Listener {
    public void setup() {}
    public void shutdown() {}
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {}
    public void registerKTSub(LiteralArgumentBuilder<CommandSourceStack> base) {}
    public void loadData() {}
    public void saveData() {}
}