package kam.kamsTweaks.ext;


import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import io.papermc.paper.event.player.AsyncChatEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.fun.Names;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SRVHelper implements SlashCommandProvider, Listener {
    public static SRVHelper instance;
    public static void sendJoin(Player who, String message) {
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) return;
        DiscordSRV.getPlugin().sendJoinMessage(who, message);
    }

    public static void sendLeave(Player who, String message) {
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) return;
        DiscordSRV.getPlugin().sendLeaveMessage(who, message);
    }

    public static void init() {
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) return;
        instance = new SRVHelper();
        Bukkit.getPluginManager().registerEvents(instance, KamsTweaks.get());
        DiscordSRV.api.subscribe(instance);
        DiscordSRV.api.addSlashCommandProvider(instance);
    }

    @Subscribe(priority = ListenerPriority.NORMAL)
    public void handleWhois(DiscordGuildMessagePreProcessEvent event) {
        if (event.getMessage().getContentRaw().startsWith("whois ")) {
            event.setCancelled(true);
            var name = event.getMessage().getContentRaw().substring(6);
            var who = Names.instance.whois(name);
            if (who == null) {
                event.getMessage().reply("No one is using the nickname " + name + ".").queue();
            } else {
                event.getMessage().reply(name + " is " + who.getName() + ".").queue();
            }
        }
    }

    String lastSender = "";
    String message = "";
    int inARow = 0;

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void handleUb3rB0t(DiscordGuildMessagePreProcessEvent event) {
        if (event.getAuthor().isBot()) return;
        var plain = event.getMessage().getContentDisplay();
        if (lastSender.equals(event.getAuthor().getName())) return;
        lastSender = event.getAuthor().getName();
        if (!message.equals(plain)) {
            message = plain;
            inARow  = 1;
            return;
        }
        inARow++;
        if (inARow >= 3) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
                Bukkit.getServer().sendMessage(Component.text("<LTSMP> ").append(Component.text(message)));
                DiscordSRV.getPlugin().getMainTextChannel().sendMessage(message).queue();
            }, 10);
            inARow = 0;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void handleUb3rB0t(AsyncChatEvent event) {
        var plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (lastSender.equals(event.getPlayer().getName())) return;
        lastSender = event.getPlayer().getName();
        if (!message.equals(plain)) {
            message = plain;
            inARow  = 1;
            return;
        }
        inARow++;
        if (inARow >= 3) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(KamsTweaks.get(), () -> {
                Bukkit.getServer().sendMessage(Component.text("<LTSMP> ").append(Component.text(message)));
                DiscordSRV.getPlugin().getMainTextChannel().sendMessage(message).queue();
            }, 10);
            inARow  = 0;
        }
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(List.of(new PluginSlashCommand(KamsTweaks.get(), new CommandData("whois", "Find who is using a name.").addOption(OptionType.STRING, "name", "Who to find"))));
    }

    @SlashCommand(path = "whois")
    public void whoisCommand(SlashCommandEvent event) {
        var opt = event.getOption("name");
        if (opt == null) return;
        var name = opt.getAsString();
        var who = Names.instance.whois(name);
        if (who == null) {
            event.reply("No one is using the nickname " + name + ".").queue();
        } else {
            event.reply(name + " is " + who.getName() + ".").queue();
        }
    }
}
