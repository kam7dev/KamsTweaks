package kam.kamsTweaks.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Config {
    private static final List<ConfigOption> configs = new ArrayList<>();
    public static void addConfig(ConfigOption config) {
        configs.add(config);
    }

    public static boolean getBool(String id, boolean defaultValue) {
        return KamsTweaks.get().getConfig().getBoolean(id, defaultValue);
    }

    public static int getInt(String id, int defaultValue) {
        return KamsTweaks.get().getConfig().getInt(id, defaultValue);
    }

    public static long getLong(String id, long defaultValue) {
        return KamsTweaks.get().getConfig().getLong(id, defaultValue);
    }

    public static String getString(String id, String defaultValue) {
        return KamsTweaks.get().getConfig().getString(id, defaultValue);
    }

    public abstract static class ConfigOption {
        public String name;
        public String configId;
        public String permission;
        public boolean requiresRestart;
        public FileConfiguration config = KamsTweaks.get().getConfig();

        public abstract LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command);
    }

    public static class StringConfigOption extends ConfigOption {
        public String[] options;
        public boolean freeType = false;
        public Consumer<String> callback;
        public String default_;

        public StringConfigOption(String name, String configId, String default_, String[] options, boolean requiresRestart, String permission) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
            this.options = options;
            this.requiresRestart = requiresRestart;
            this.permission = permission;
        }
        public StringConfigOption(String name, String configId, String default_, String[] options, boolean requiresRestart) {
            this(name, configId, default_, options, requiresRestart, null);
        }
        public StringConfigOption(String name, String configId, String default_, String[] options, String permission) {
            this(name, configId, default_, options, false, permission);
        }
        public StringConfigOption(String name, String configId, String default_, String[] options) {
            this(name, configId, default_, options, false, null);
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                String val = config.getString(configId, default_);
                ctx.getSource().getSender().sendMessage("Current value for " + name + " is " + val + ".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission)).then(Commands.argument("value", StringArgumentType.word()).suggests((ctx, builder) -> {
                for (var option : options) {
                    builder.suggest(option);
                }
                return builder.buildFuture();
            }).executes(ctx -> {
                String val = ctx.getArgument("value", String.class).toLowerCase();
                if (!List.of(options).contains(val) && !freeType) {
                    ctx.getSource().getSender().sendMessage("Invalid value. Valid options are " + String.join(", ", List.of(options)) + ".");
                    return Command.SINGLE_SUCCESS;
                }
                config.set(configId, val);
                KamsTweaks.get().saveConfig();
                if (callback != null) callback.accept(val);
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }
    }

    public static class BoolConfigOption extends ConfigOption {
        Consumer<Boolean> callback;
        boolean default_;

        public BoolConfigOption(String name, String configId, boolean default_, boolean requiresRestart, String permission) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
            this.requiresRestart = requiresRestart;
            this.permission = permission;
        }
        public BoolConfigOption(String name, String configId, boolean default_, boolean requiresRestart) {
            this(name, configId, default_, requiresRestart, null);
        }
        public BoolConfigOption(String name, String configId, boolean default_, String permission) {
            this(name, configId, default_, false, permission);
        }
        public BoolConfigOption(String name, String configId, boolean default_) {
            this(name, configId, default_, false, null);
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                boolean val = config.getBoolean(configId, default_);
                ctx.getSource().getSender().sendMessage("Current value for " + name + " is " + val + ".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission)).then(Commands.argument("value", BoolArgumentType.bool()).executes(ctx -> {
                Boolean val = ctx.getArgument("value", Boolean.class);
                config.set(configId, val);
                KamsTweaks.get().saveConfig();
                if (callback != null) callback.accept(val);
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }
    }

    public static class IntConfigOption extends ConfigOption {
        Consumer<Integer> callback;
        Integer default_;

        public IntConfigOption(String name, String configId, int default_, boolean requiresRestart, String permission) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
            this.requiresRestart = requiresRestart;
            this.permission = permission;
        }
        public IntConfigOption(String name, String configId, int default_, boolean requiresRestart) {
            this(name, configId, default_, requiresRestart, null);
        }
        public IntConfigOption(String name, String configId, int default_, String permission) {
            this(name, configId, default_, false, permission);
        }
        public IntConfigOption(String name, String configId, int default_) {
            this(name, configId, default_, false, null);
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                int val = config.getInt(configId, default_);
                ctx.getSource().getSender().sendMessage("Current value for " + name + " is " + val + ".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission)).then(Commands.argument("value", IntegerArgumentType.integer()).executes(ctx -> {
                Integer val = ctx.getArgument("value", Integer.class);
                config.set(configId, val);
                KamsTweaks.get().save();
                ctx.getSource().getSender().sendMessage("Set value of " + name + " to " + val + ".");
                if (callback != null) callback.accept(val);
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }
    }

    public static void registerKTSub(LiteralArgumentBuilder<CommandSourceStack> base) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("config");
        for (var sub : configs) {
            command = sub.registerSubcommand(command);
        }
        base.then(command);
    }
}
