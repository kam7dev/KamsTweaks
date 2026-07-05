package kam.kamsTweaks.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.command.ConsoleCommandSender;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class Logger {
    static boolean initialized = false;

    static LogLevel logLevel = LogLevel.WARN;

    public static List<Exception> exceptions = new ArrayList<>();

    public static void init() {
        if (initialized) return;
        try {
            logLevel = LogLevel.valueOf(KamsTweaks.get().getConfig().getString("log-level", "warn").toUpperCase());
        } catch(Exception e) {
            Logger.error("Your log level seems to be invalid. Please make sure it's either 'debug', 'info', 'warn', or 'error'.");
        }
        var cmd = new ConfigCommand.StringConfig("log-level", "log-level", "warn", new String[]{"debug", "info", "warn", "error"}, "kamstweaks.configure");
        cmd.callback = Logger::setLevel;
        ConfigCommand.addConfig(cmd);
        initialized = true;
    }

    public static void saveData() {

    }

    public static void loadData() {

    }

    public static void registerKTSub(LiteralArgumentBuilder<CommandSourceStack> base) {
        base.then(Commands.literal("exceptions").requires(source -> source.getSender().hasPermission("kamstweaks.logger")).executes(ctx -> {
            ctx.getSource().getSender().sendMessage("There are " + Logger.exceptions.size() + " exceptions.");
            return Command.SINGLE_SUCCESS;
        }).then(Commands.literal("print").then(Commands.argument("id", IntegerArgumentType.integer()).executes(ctx -> {
            var id = ctx.getArgument("id", Integer.class);
            if (Logger.exceptions.size() <= id) {
                ctx.getSource().getSender().sendMessage("Exception " + id + " not found.");
                return Command.SINGLE_SUCCESS;
            }
            var e = Logger.exceptions.get(id);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            if (!(ctx.getSource().getSender() instanceof ConsoleCommandSender))
                Logger.error("Stack trace print requested by " + ctx.getSource().getSender().getName() + ":\n" + sw);
            ctx.getSource().getSender().sendMessage(sw.toString());
            return Command.SINGLE_SUCCESS;
        })).requires(source -> source.getSender().hasPermission("kamstweaks.logger")))
                .then(Commands.literal("clear").executes(ctx -> {
                    Logger.exceptions.clear();
                    ctx.getSource().getSender().sendMessage("Exceptions cleared.");
                    return Command.SINGLE_SUCCESS;
                }).requires(source -> source.getSender().hasPermission("kamstweaks.logger"))));
    }

    public static void debug(String message, Object... args) {
        if (logLevel.ordinal() < LogLevel.DEBUG.ordinal()) return;
        StringBuilder msg = new StringBuilder(message);
        for (var obj : args) {
            msg.append(" ").append(obj.toString());
        }
        KamsTweaks.get().getLogger().info(msg.toString());
    }

    public static void info(String message, Object... args) {
        if (logLevel.ordinal() < LogLevel.INFO.ordinal()) return;
        StringBuilder msg = new StringBuilder(message);
        for (var obj : args) {
            msg.append(" ").append(obj.toString());
        }
        KamsTweaks.get().getLogger().info(msg.toString());
    }

    public static void warn(String message, Object... args) {
        if (logLevel.ordinal() < LogLevel.WARN.ordinal()) return;
        StringBuilder msg = new StringBuilder(message);
        for (var obj : args) {
            msg.append(" ").append(obj.toString());
        }
        KamsTweaks.get().getLogger().warning(msg.toString());
    }

    public static void error(String message, Object... args) {
        // error is always less than or equal to logLevel
        StringBuilder msg = new StringBuilder(message);
        for (var obj : args) {
            msg.append(" ").append(obj.toString());
        }
        KamsTweaks.get().getLogger().severe(msg.toString());
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

    public static void handleException(String message, Exception e) {
        exceptions.add(e);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Logger.error(message.isEmpty() ? "" : "\n" + sw);
    }

    public static void handleException(Exception e) {
        handleException("", e);
    }

    public enum LogLevel {
        ERROR,
        WARN,
        INFO,
        DEBUG
    }
}
