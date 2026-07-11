package kam.kamsTweaks.features.fun.nicknames;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class MiniMessageInterpreter extends Names.NameInterpreter{
    MiniMessage mm = MiniMessage.builder().tags(TagResolver.builder()
            .resolver(StandardTags.color())
            .resolver(StandardTags.decorations())
            .resolver(StandardTags.shadowColor())
            .resolver(StandardTags.reset())
            .resolver(StandardTags.gradient())
            .resolver(StandardTags.rainbow())
            .resolver(StandardTags.pride())
            .build()).build();

    @Override
    public Component interpret(@NonNull ConfigurationSection sect, @NonNull OfflinePlayer who) {
        var name = sect.getString("name");
        if (name == null) return null;
        return Component.text().append(mm.deserialize(name)).build().hoverEvent(HoverEvent.showText(Component.text(Objects.requireNonNullElse(who.getName(), "Unknown"))));
    }

    @Override
    public void save(@NonNull ConfigurationSection sect, @NonNull Component name) {
        sect.set("mm", mm.serialize(name));
    }

    @Override
    public @NonNull String getTypeId() {
        return "minimessage";
    }
}
