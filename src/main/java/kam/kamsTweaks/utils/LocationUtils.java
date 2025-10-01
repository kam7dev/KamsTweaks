package kam.kamsTweaks.utils;

import org.bukkit.Location;

import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class LocationUtils {
    public static String serializeBlockPos(Location loc) {
        return loc.getWorld() == null ? "" : loc.getWorld().getUID() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    public static Location deserializeBlockPos(String s) {
        String[] parts = s.split(",");
        UUID worldUuid = UUID.fromString(parts[0]);
        return new Location(
                getServer().getWorld(worldUuid),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3])
        );
    }

    public static String serializeLocation(Location loc) {
        return loc.getWorld() == null ? "" : loc.getWorld().getUID() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    public static Location deserializeLocation(String s) {
        String[] parts = s.split(",");
        UUID worldUuid = UUID.fromString(parts[0]);
        if (parts.length == 4) {
            return new Location(
                    getServer().getWorld(worldUuid),
                    Double.parseDouble(parts[1]) + .5,
                    Double.parseDouble(parts[2]) + .5,
                    Double.parseDouble(parts[3]) + .5
            );
        }
        return new Location(
                getServer().getWorld(worldUuid),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }
}
