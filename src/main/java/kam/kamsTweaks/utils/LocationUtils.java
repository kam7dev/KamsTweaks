package kam.kamsTweaks.utils;

import org.bukkit.Location;

public class LocationUtils {
    public static boolean inBounds(Location target, Location bound1, Location bound2) {
        if (bound1.getWorld() != target.getWorld()) return false;

        int minX = Math.min(bound1.getBlockX(), bound2.getBlockX());
        int maxX = Math.max(bound1.getBlockX(), bound2.getBlockX());
        int minY = Math.min(bound1.getBlockY(), bound2.getBlockY());
        int maxY = Math.max(bound1.getBlockY(), bound2.getBlockY());
        int minZ = Math.min(bound1.getBlockZ(), bound2.getBlockZ());
        int maxZ = Math.max(bound1.getBlockZ(), bound2.getBlockZ());

        return target.getBlockX() >= minX && target.getBlockX() <= maxX
                && target.getBlockY() >= minY && target.getBlockY() <= maxY
                && target.getBlockZ() >= minZ && target.getBlockZ() <= maxZ;
    }
}
