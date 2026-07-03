package kam.kamsTweaks.ext;


import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.fun.Names;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SRVHelper implements SlashCommandProvider {
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
        DiscordSRV.api.subscribe(instance);
        DiscordSRV.api.addSlashCommandProvider(instance);
    }

    @Subscribe
    public void on(DiscordGuildMessagePreProcessEvent event) {
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
