package kam.kamsTweaks.ext;


import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SRVHelper {
    public static void sendJoin(Player who, String message) {
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) return;
        DiscordSRV.getPlugin().sendJoinMessage(who, message);
    }

    public static void sendLeave(Player who, String message) {
        if (!Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) return;
        DiscordSRV.getPlugin().sendLeaveMessage(who, message);
    }
}
