package kam.kamsTweaks.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.managers.KTPerms;
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

    public static BoolOption.Builder bool(String id, boolean defaultValue) {
        return new BoolOption.Builder().configId(id).name(id).defaultValue(defaultValue);
    }

    public static BoolOption.Builder bool(String id) {
        return new BoolOption.Builder().configId(id).name(id);
    }

    public static BoolOption.Builder bool() {
        return new BoolOption.Builder();
    }

    public static IntOption.Builder integer() {
        return new IntOption.Builder();
    }

    public static IntOption.Builder integer(String id, int defaultValue) {
        return new IntOption.Builder().configId(id).name(id).defaultValue(defaultValue);
    }

    public static IntOption.Builder integer(String id) {
        return new IntOption.Builder().configId(id).name(id);
    }

    public static StringOption.Builder string() {
        return new StringOption.Builder();
    }

    public static StringOption.Builder string(String id, String defaultValue) {
        return new StringOption.Builder().configId(id).name(id).defaultValue(defaultValue);
    }

    public static StringOption.Builder string(String id, String defaultValue, String[] options) {
        return new StringOption.Builder().configId(id).name(id).defaultValue(defaultValue).options(options);
    }

    public static StringOption.Builder string(String id) {
        return new StringOption.Builder().configId(id).name(id);
    }

    public abstract static class ConfigOption {
        public String name;
        public String configId;
        public String permission = "kamstweaks.configure";
        public boolean requiresRestart = false;
        public FileConfiguration config = KamsTweaks.get().getConfig();

        public abstract LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command);

        ConfigOption() {}

        ConfigOption(Builder<?> builder) {
            this.name = builder.name;
            this.configId = builder.configId;
            if (builder.permission != null) this.permission = builder.permission;
            this.requiresRestart = builder.requiresRestart;
            if (builder.config != null) this.config = builder.config;
        }

        public void add() {
            addConfig(this);
        }

        @SuppressWarnings("unchecked")
        protected static class Builder<Class extends Builder<Class>> {
            String name;
            String configId;
            public String permission = "kamstweaks.configure";
            boolean requiresRestart = false;
            public FileConfiguration config = KamsTweaks.get().getConfig();

            public Class name(String name) {
                this.name = name;
                return (Class) this;
            }

            public Class configId(String configId) {
                this.configId = configId;
                if (this.name == null) this.name =  configId;
                return (Class) this;
            }

            public Class permission(String permission) {
                this.permission = permission;
                return (Class) this;
            }

            public Class permission(KTPerms permission) {
                this.permission = permission.id;
                return (Class) this;
            }

            public Class requiresRestart(boolean requiresRestart) {
                this.requiresRestart = requiresRestart;
                return (Class) this;
            }

            public Class config(FileConfiguration config) {
                this.config = config;
                return (Class) this;
            }
        }
    }

    public static class BoolOption extends ConfigOption {
        public boolean defaultValue = false;
        public Consumer<Boolean> callback;

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                boolean val = config.getBoolean(configId, defaultValue);
                ctx.getSource().getSender().sendMessage("Current value for " + name + " is " + val + ".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission)).then(Commands.argument("value", BoolArgumentType.bool()).executes(ctx -> {
                Boolean val = ctx.getArgument("value", Boolean.class);
                config.set(configId, val);
                KamsTweaks.get().saveConfig();
                if (callback != null) callback.accept(val);
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\"." + (requiresRestart ? " This option requires a restart to take effect." : ""));
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }

        BoolOption() {}

        BoolOption(Builder builder) {
            super(builder);
            this.defaultValue = builder.defaultValue;
            this.callback = builder.callback;
        }

        public static class Builder extends ConfigOption.Builder<Builder> {
            boolean defaultValue = false;
            Consumer<Boolean> callback;

            public Builder defaultValue(boolean defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            public Builder callback(Consumer<Boolean> callback) {
                this.callback = callback;
                return this;
            }

            public BoolOption build() {
                return new BoolOption(this);
            }
        }
    }

    public static class IntOption extends ConfigOption {
        public int defaultValue = 0;
        public Consumer<Integer> callback;

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                int val = config.getInt(configId, defaultValue);
                ctx.getSource().getSender().sendMessage("Current value for " + name + " is " + val + ".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission)).then(Commands.argument("value", IntegerArgumentType.integer()).executes(ctx -> {
                Integer val = ctx.getArgument("value", Integer.class);
                config.set(configId, val);
                KamsTweaks.get().save();
                ctx.getSource().getSender().sendMessage("Set value of " + name + " to " + val + ".");
                if (callback != null) callback.accept(val);
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\"." + (requiresRestart ? " This option requires a restart to take effect." : ""));
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }

        IntOption() {}

        IntOption(Builder builder) {
            super(builder);
            this.defaultValue = builder.defaultValue;
            this.callback = builder.callback;
        }

        public static class Builder extends ConfigOption.Builder<Builder> {
            int defaultValue = 0;
            Consumer<Integer> callback;

            public Builder defaultValue(int defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            public Builder callback(Consumer<Integer> callback) {
                this.callback = callback;
                return this;
            }

            public IntOption build() {
                return new IntOption(this);
            }
        }
    }

    public static class StringOption extends ConfigOption {
        public String[] options = new String[]{};
        public boolean freeType = false;
        public String defaultValue = "";
        public Consumer<String> callback;

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                String val = config.getString(configId, defaultValue);
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
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\"." + (requiresRestart ? " This option requires a restart to take effect." : ""));
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }

        StringOption() {}

        StringOption(Builder builder) {
            super(builder);
            this.options = builder.options;
            this.freeType = builder.freeType;
            this.defaultValue = builder.defaultValue;
            this.callback = builder.callback;
        }

        public static class Builder extends ConfigOption.Builder<Builder> {
            public String[] options = new String[]{};
            public boolean freeType = false;
            String defaultValue = "";
            Consumer<String> callback;

            public Builder options(String[] options) {
                this.options = options;
                return this;
            }
            public Builder freeType(boolean freeType) {
                this.freeType = freeType;
                return this;
            }

            public Builder defaultValue(String defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            public Builder callback(Consumer<String> callback) {
                this.callback = callback;
                return this;
            }

            public StringOption build() {
                return new StringOption(this);
            }
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
