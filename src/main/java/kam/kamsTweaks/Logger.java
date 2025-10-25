package kam.kamsTweaks;

import java.util.ArrayList;
import java.util.List;

public class Logger {
    static boolean inited = false;

    static LogLevel logLevel = LogLevel.WARN;

    public static List<Exception> excs = new ArrayList<>();

    public static void init() {
        if (inited) return;
        try {
            logLevel = LogLevel.valueOf(KamsTweaks.getInstance().getConfig().getString("log-level", "warn").toUpperCase());
        } catch(Exception e) {
            Logger.error("Your log level seems to be invalid. Please make sure it's either 'debug', 'info', 'warn', or 'error'.");
        }
        var cmd = new ConfigCommand.StringConfig("log-level", "log-level", "warn", new String[]{"debug", "info", "warn", "error"}, "kamstweaks.configure");
        cmd.callback = Logger::setLevel;
        ConfigCommand.addConfig(cmd);
        inited = true;
    }

    public static void debug(String message) {
        if (logLevel.ordinal() >= LogLevel.DEBUG.ordinal())
            KamsTweaks.getInstance().getLogger().info(message);
    }

    public static void info(String message) {
        if (logLevel.ordinal() >= LogLevel.INFO.ordinal())
            KamsTweaks.getInstance().getLogger().info(message);
    }

    public static void warn(String message) {
        if (logLevel.ordinal() >= LogLevel.WARN.ordinal())
            KamsTweaks.getInstance().getLogger().warning(message);
    }

    public static void error(String message) {
        // error is always less than or equal to logLevel
        KamsTweaks.getInstance().getLogger().severe(message);
    }

    public static void setLevel(String level) {
        try {
            logLevel = LogLevel.valueOf(level.toUpperCase());
        } catch(Exception e) {
            Logger.error("Could not set log level to " + level + " because it's an invalid log level.");
        }
    }

    public static void setLevel(LogLevel level) {
        try {
            logLevel = level;
        } catch(Exception e) {
            Logger.error("Could not set log level to " + level + " because it's an invalid log level.");
        }
    }

    public enum LogLevel {
        ERROR,
        WARN,
        INFO,
        DEBUG
    }
}
