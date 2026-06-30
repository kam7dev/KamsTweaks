package kam.kamsTweaks.gameplay;

import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.utils.ConfigCommand;
import kam.kamsTweaks.utils.KTStrings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

public class DragonFightLock extends Feature {
    @Override
    public void setup() {
        var cmd = new ConfigCommand.BoolConfig("dragon-fight.enabled", "dragon-fight." + Bukkit.getWorlds().get(2).getUID(), false, "kamstweaks.configure");
        cmd.config = KamsTweaks.get().getDataConfig();
        ConfigCommand.addConfig(cmd);
    }

    @EventHandler
    public void onAttack(EntityDamageEvent event) {
        var world = event.getEntity().getWorld();
        if (KamsTweaks.get().getDataConfig().getBoolean("dragon-fight." + world.getUID(), false)) return;
        if (event.getEntity() instanceof EnderDragon dragon) {
            event.setCancelled(true);
            if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                player.sendMessage(KTStrings.getFor(KTStrings.DRAGON_TOGGLE, Component.text("/kt config dragon-fight.enabled true").clickEvent(ClickEvent.suggestCommand("/kt config dragon-fight.enabled true")).color(NamedTextColor.YELLOW)));
            }
        } else if (event.getEntity() instanceof EnderCrystal crystal) {
            if (world.getEnderDragonBattle() != null && world.getEnderDragonBattle().getHealingCrystals().contains(crystal)) {
                event.setCancelled(true);
                if (event.getDamageSource().getCausingEntity() instanceof Player player) {
                    player.sendMessage(KTStrings.getFor(KTStrings.DRAGON_TOGGLE, Component.text("/kt config dragon-fight.enabled true").clickEvent(ClickEvent.suggestCommand("/kt config dragon-fight.enabled true")).color(NamedTextColor.YELLOW)));
                }
            }
        }
    }
}
