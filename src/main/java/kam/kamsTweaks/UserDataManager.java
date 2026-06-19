package kam.kamsTweaks;

import org.bukkit.configuration.MemorySection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static kam.kamsTweaks.features.claims.LandClaims.nonNull;

public class UserDataManager extends Feature {
    static Map<UUID, Map<String, Object>> data = new HashMap<>();

    @Override
    public void saveData() {
        var cfg = KamsTweaks.get().getDataConfig();
        data.forEach((uuid, map) -> cfg.set("user-data." + uuid, map));
    }

    @Override
    public void loadData() {
        var cfg = KamsTweaks.get().getDataConfig();
        var uds = cfg.getConfigurationSection("user-data");
        if (uds == null) return;
        for (var key : uds.getKeys(false)) {
            var uuid = UUID.fromString(key);
            data.put(uuid, nonNull(uds.getConfigurationSection(key)).getValues(false));
        }
    }

    public static <T> void set(UUID who, String id, T value) {
        Map<String, Object> ud;
        if (!data.containsKey(who)) {
            ud = new HashMap<>();
            data.put(who, ud);
        } else {
            ud = data.get(who);
        }
        ud.put(id, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(UUID who, String id, T defaultValue) {
        if (!data.containsKey(who)) return defaultValue;
        var ud = data.get(who);
        if (!ud.containsKey(id)) return defaultValue;
        return (T) ud.get(id);
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> getMap(UUID who, String id, Map<String, T> defaultValue) {
        if (!data.containsKey(who)) return defaultValue;
        var ud = data.get(who);
        if (!ud.containsKey(id)) return defaultValue;
        return (Map<String, T>) ((MemorySection) ud.get(id)).getValues(false);
    }
}
