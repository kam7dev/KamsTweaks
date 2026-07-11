package kam.kamsTweaks.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import kam.kamsTweaks.KamsTweaks;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.command.ConsoleCommandSender;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class Logger {
    static boolean initialized = false;

    static LogLevel logLevel = LogLevel.WARN;

    public static List<Exception> exceptions = new ArrayList<>();
    static ComponentLogger logger;

    public static void init() {
        if (initialized) return;
        var strLevel = Config.getString("logger.log-level", "info").toUpperCase();
        try {
            logLevel = LogLevel.valueOf(strLevel);
        } catch(Exception e) {
            Logger.error("Your log level ({}) seems to be invalid. Please make sure it's either 'debug', 'info', 'warn', or 'error'.", strLevel);
        }
        var cmd = new Config.StringConfigOption("logger.log-level", "logger.log-level", "info", new String[]{"debug", "info", "warn", "error"}, "kamstweaks.configure");
        cmd.callback = Logger::setLevel;
        Config.addConfig(cmd);
        initialized = true;
        logger = ComponentLogger.logger("KamsTweaks");
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
                Logger.error("Stack trace print requested by {}:\n{}", ctx.getSource().getSender().getName(), sw);
            ctx.getSource().getSender().sendMessage(sw.toString());
            return Command.SINGLE_SUCCESS;
        })).requires(source -> source.getSender().hasPermission("kamstweaks.logger")))
                .then(Commands.literal("clear").executes(ctx -> {
                    Logger.exceptions.clear();
                    ctx.getSource().getSender().sendMessage("Exceptions cleared.");
                    return Command.SINGLE_SUCCESS;
                }).requires(source -> source.getSender().hasPermission("kamstweaks.logger")))
                .then(Commands.literal("throw").executes(ctx -> {
                    try {
                        throw new RuntimeException("Exception throw requested by " + ctx.getSource().getSender().getName());
                    } catch (Exception e) {
                        Logger.handleException(e);
                    }
                    ctx.getSource().getSender().sendMessage("Threw a new exception.");
                    return Command.SINGLE_SUCCESS;
                }).requires(source -> source.getSender().hasPermission("kamstweaks.logger"))));
    }

    public static void debug(String msg) {
        if (logLevel.ordinal() < LogLevel.DEBUG.ordinal()) return;
        logger.debug(msg);
    }
    public static void debug(String format, Object arg) {
        if (logLevel.ordinal() < LogLevel.DEBUG.ordinal()) return;
        logger.debug(format, arg);
    }
    public static void debug(String format, Object arg1, Object arg2) {
        if (logLevel.ordinal() < LogLevel.DEBUG.ordinal()) return;
        logger.debug(format, arg1, arg2);
    }
    public static void debug(String format, Object... arguments) {
        if (logLevel.ordinal() < LogLevel.DEBUG.ordinal()) return;
        logger.debug(format, arguments);
    }


    public static void info(String msg) {
        if (logLevel.ordinal() < LogLevel.INFO.ordinal()) return;
        logger.info(msg);
    }
    public static void info(String format, Object arg) {
        if (logLevel.ordinal() < LogLevel.INFO.ordinal()) return;
        logger.info(format, arg);
    }
    public static void info(String format, Object arg1, Object arg2) {
        if (logLevel.ordinal() < LogLevel.INFO.ordinal()) return;
        logger.info(format, arg1, arg2);
    }
    public static void info(String format, Object... arguments) {
        if (logLevel.ordinal() < LogLevel.INFO.ordinal()) return;
        logger.info(format, arguments);
    }


    public static void warn(String msg) {
        if (logLevel.ordinal() < LogLevel.WARN.ordinal()) return;
        logger.warn(msg);
    }
    public static void warn(String format, Object arg) {
        if (logLevel.ordinal() < LogLevel.WARN.ordinal()) return;
        logger.warn(format, arg);
    }
    public static void warn(String format, Object arg1, Object arg2) {
        if (logLevel.ordinal() < LogLevel.WARN.ordinal()) return;
        logger.warn(format, arg1, arg2);
    }
    public static void warn(String format, Object... arguments) {
        if (logLevel.ordinal() < LogLevel.WARN.ordinal()) return;
        logger.warn(format, arguments);
    }


    public static void error(String msg) {
        logger.error(msg);
    }
    public static void error(String format, Object arg) {
        logger.error(format, arg);
    }
    public static void error(String format, Object arg1, Object arg2) {
        logger.error(format, arg1, arg2);
    }
    public static void error(String format, Object... arguments) {
        logger.error(format, arguments);
    }

    public static void setLevel(String level) {
        try {
            logLevel = LogLevel.valueOf(level.toUpperCase());
        } catch(Exception e) {
            Logger.error("Could not set log level to {} because it's an invalid log level.", level);
        }
    }

    public static void setLevel(LogLevel level) {
        try {
            logLevel = level;
        } catch(Exception e) {
            Logger.error("Could not set log level to {} because it's an invalid log level.", level);
        }
    }

    public static int handleException(String message, Exception e) {
        var where = exceptions.size();
        exceptions.add(e);
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        Logger.error("{}\n{}", message, sw);
        return where;
    }

    public static int handleException(Exception e) {
        return handleException("", e);
    }

    public enum LogLevel {
        ERROR,
        WARN,
        INFO,
        DEBUG
    }
}
