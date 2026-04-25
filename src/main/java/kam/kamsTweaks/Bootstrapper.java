package kam.kamsTweaks;

import io.papermc.paper.datapack.DatapackRegistrar;
import io.papermc.paper.plugin.bootstrap.*;
import io.papermc.paper.plugin.lifecycle.event.*;
import io.papermc.paper.plugin.lifecycle.event.types.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class Bootstrapper implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        final LifecycleEventManager<@NotNull BootstrapContext> manager = context.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            DatapackRegistrar registrar = event.registrar();
            try {
                final URI uri = Objects.requireNonNull(
                        Bootstrapper.class.getResource("/datapack")
                ).toURI();
                registrar.discoverPack(uri, "kamstweaks");
            } catch (final Exception e) {
                Logger.handleException(e);
            }
        });
    }
}