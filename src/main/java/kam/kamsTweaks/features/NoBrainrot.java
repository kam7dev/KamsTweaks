package kam.kamsTweaks.features;

import java.util.regex.Pattern;

import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class NoBrainrot extends Feature {
  String pattern = "(?i)" + // case-insensitive
      ".*:shushing_face: :deaf_man:.*|.*:shushing_face::deaf_man:.*|" +
      ".*461n 9055.*|.*4d1n r0ss.*|.*5c1b1di.*|.*5k1b1dy.*|" +
      ".*a6in ross.*|.*adin ross.*|.*admin ross.*|.*baby gronk.*|" +
      ".*bb gr0nk.*|.*bb gronk.*|.*BOMBARDINO CROCODILO.*|.*bopboompow.*|" +
      ".*f4num.*|.*fanum.*|.*fire in the hole.*|.*g00n1ng.*|.*g00ning.*|" +
      ".*g74tt.*|.*gedagedi.*|.*gooning.*|.*gy4tt.*|.*gyatt.*|" +
      ".*hawk 2.*|.*hawk tua.*|.*hawk tuah.*|.*hawk two.*|.*kai cenat.*|" +
      ".*low taper fade.*|.*m3w.*|.*m4xx1ng.*|.*m4xxing.*|.*maxx1ng.*|.*maxxing.*|" +
      ".*mewer.*|.*mewing.*|.*quandale dingle.*|.*r177.*|.*r1zz.*|.*rizz.*|" +
      ".*sc1b1d1.*|.*sc1b1dy.*|.*scibidi.*|.*scibidy.*|.*sigma.*|" +
      ".*six.*seven.*|.*sk1b1.*|.*sk1bi.*|.*skbidi.*|.*skibi.*|" +
      ".*skibiddi.*|.*skibide.*|.*skibidi.*|.*skibidy.*|.*skibity.*|" +
      ".*skybidi.*|.*tralalero.*|.*tuah.*|.*tuff.*|.*tung.*|.*tung sahur.*|" +
      ".*unohio.*|.*water on the hill.*";

  Pattern p = Pattern.compile(pattern);

  public void setup() {
    ConfigCommand
        .addConfig(new ConfigCommand.BoolConfig("filter.enabled", "filter.enabled", true, "kamstweaks.configure"));
  }

  public void shutdown() {
  }

  public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
  }

  public void loadData() {
  }

  public void saveData() {
  }

  PlainTextComponentSerializer ser = PlainTextComponentSerializer.plainText();

  @EventHandler
  void onChat(AsyncChatEvent e) {
    if (!KamsTweaks.getInstance().getConfig().getBoolean("filter.enabled", true))
      return;
    var str = ser.serialize(e.message());
    if (p.matcher(str).find()) {
      Logger.warn(e.getPlayer().getName() + " was caught sending brainrot: " + str);
      e.setCancelled(true);
      e.getPlayer().sendMessage(Component.text("please take your brainrot somewhere else (5 minute timeout for bypass)")
          .color(NamedTextColor.RED));
    }
  }
}
