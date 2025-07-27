package kam.kamsTweaks;

public class Logger {
    static boolean inited = false;

    public static void init() {
        if (inited) return;
        inited = true;
    }

    public static void info(String message) {
        KamsTweaks.getInstance().getLogger().info(message);
    }

    public static void warn(String message) {
        KamsTweaks.getInstance().getLogger().warning(message);
    }

    public static void error(String message) {
        KamsTweaks.getInstance().getLogger().severe(message);
    }
}
