package kam.kamsTweaks.features.fun;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.math.Rotations;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.features.Feature;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class VirtualInsanity extends Feature {
    @Override
    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        commands.registrar().register(Commands.literal("jamiroquai").executes(ctx -> {
            var exec = ctx.getSource().getExecutor();
            if (!(exec instanceof Player plr)) return Command.SINGLE_SUCCESS;
            float yaw = (plr.getLocation().getYaw() % 360 + 360) % 360;
            var rad = -Math.toRadians(yaw);
            var loc = plr.getLocation().add(Math.sin(rad) * 10, 0, Math.cos(rad) * 10).addRotation(246, 0);
            var stand = plr.getWorld().spawn(loc, ArmorStand.class);
            stand.teleport(loc);
            stand.setMarker(true);
            stand.setHeadRotations(Rotations.ofDegrees(0, 305, 0));
            stand.setLeftLegRotations(Rotations.ofDegrees(352, 0, 354));
            stand.setLeftArmRotations(Rotations.ofDegrees(16, 50,  0));
            stand.setRightArmRotations(Rotations.ofDegrees(275 ,300 ,344));
            stand.setArms(true);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.setItem(EquipmentSlot.HEAD, new ItemStack(Material.PLAYER_HEAD));
            stand.setPersistent(false);
            stand.setRemoveWhenFarAway(true);
            for (var player : Bukkit.getOnlinePlayers()) {
                if (plr.getUniqueId().equals(player.getUniqueId())) continue;
                player.hideEntity(KamsTweaks.get(), stand);
            }
            stand.setDisabledSlots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
            plr.playSound(Sound.sound(Key.key("kamstweaks", "ooooooooo"), Sound.Source.MASTER, 1, 1));
            var ref = new Object() {
                int id = 0;
                int i = 88;
                final float div = 10f / i;
            };
            ref.id = Bukkit.getScheduler().scheduleSyncRepeatingTask(KamsTweaks.get(), () -> {
                var newLoc = stand.getLocation().add(-Math.sin(rad) * ref.div, 0, -Math.cos(rad) * ref.div);
                stand.teleport(newLoc);
                ref.i--;
                if (ref.i == 0) {
                    Bukkit.getScheduler().cancelTask(ref.id);
                    stand.remove();
                }
            }, 1, 1);

            // Head:[0f,305f,0f]
            // LeftLeg:[352f,0f,354f]
            // LeftArm:[16f,50f,0f]
            // RightArm:[275f,300f,344f]

            return Command.SINGLE_SUCCESS;
        }).build());
    }
}
