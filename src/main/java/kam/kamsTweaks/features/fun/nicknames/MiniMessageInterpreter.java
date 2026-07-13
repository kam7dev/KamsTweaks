package kam.kamsTweaks.features.fun.nicknames;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.features.moderation.ChatFilter;
import kam.kamsTweaks.managers.KTPerms;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import kam.kamsTweaks.features.fun.nicknames.Names.NameData;

import java.util.List;
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
    public NameData interpret(@NonNull ConfigurationSection sect, @NonNull OfflinePlayer who) {
        var name = sect.getString("name");
        if (name == null) return null;
        return new NameData(getTypeId(), Component.text().append(mm.deserialize(name)).build().hoverEvent(HoverEvent.showText(Component.text(Objects.requireNonNullElse(who.getName(), "Unknown")))));
    }

    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> nickCmd = Commands.literal("mmnick")
                .requires(source -> KTPerms.hasPermission(source, KTPerms.NICKNAME_MINIMESSAGE))
                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!Config.getBool("nicknames.enabled", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.NAMES)));
                        return Command.SINGLE_SUCCESS;
                    }
                    if (!Config.getBool("nicknames.allow-minimessage", true)) {
                        sender.sendMessage(KTStrings.getFor(KTStrings.DISABLED_PLURAL, KTStrings.getFor(KTStrings.NAME_MINIMESSAGE)));
                        return Command.SINGLE_SUCCESS;
                    }
                    Entity executor = ctx.getSource().getExecutor();
                    if (executor instanceof Player player) {
                        String name = ctx.getArgument("name", String.class).replaceAll(Names.INVISIBLE_REGEX, "");
                        Component comp = Component.text().append(mm.deserialize(name)).build().hoverEvent(HoverEvent.showText(Component.text(player.getName())));
                        var plain = Names.pt.serialize(comp);
                        var ml = Config.getInt("nicknames.max-length", 30);
                        if (plain.length() > ml) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.NAME_LENGTH, Component.text(ml)));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (plain.isBlank()) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.NAME_EMPTY));
                            return Command.SINGLE_SUCCESS;
                        }
                        var res = ChatFilter.instance.isFiltered(plain);
                        if (res.first) {
                            ChatFilter.warnStaff(KTStrings.getFor(KTStrings.AUTOMOD_NAME, Component.text(sender.getName()), Component.text(res.second.name), Component.text(plain)));
                            sender.sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }
                        if (Names.whois(plain, List.of(player.getUniqueId())) != null) {
                            sender.sendMessage(KTStrings.getFor(KTStrings.NAME_IN_USE));
                            return Command.SINGLE_SUCCESS;
                        }
                        Names.setName(player, new NameData(getTypeId(), comp));
                        sender.sendMessage(KTStrings.getFor(KTStrings.NAME_SET, comp));
                        return Command.SINGLE_SUCCESS;
                    }
                    sender.sendMessage(KTStrings.getFor(KTStrings.PLAYERS_ONLY, Component.text("/nick")));
                    return Command.SINGLE_SUCCESS;
                }));
        commands.registrar().register(nickCmd.build());
    }

    @Override
    public void save(@NonNull ConfigurationSection sect, @NonNull NameData nd) {
        sect.set("mm", mm.serialize(nd.name()));
    }

    @Override
    public @NonNull String getTypeId() {
        return "minimessage";
    }
}
