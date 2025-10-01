package kam.kamsTweaks;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ConfigCommand {
    private static final List<Config> configs = new ArrayList<>();

    public static void addConfig(Config config) {
        configs.add(config);
    }

    public abstract static class Config {
        public String name;
        public String configId;
        public String permission;

        public abstract LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command);
    }

    public static class StringConfig extends Config {
        public String[] options;
        public boolean freeType = false;
        public Consumer<String> callback;
        public String default_;

        public StringConfig(String name, String configId, String default_, String[] options) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
            this.options = options;
        }

        public StringConfig(String name, String configId, String default_, String[] options, String permission) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
            this.options = options;
            this.permission = permission;
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                String val = KamsTweaks.getInstance().getConfig().getString(configId, default_);
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
                KamsTweaks.getInstance().getConfig().set(configId, val);
                KamsTweaks.getInstance().saveConfig();
                if (callback != null) callback.accept(val);
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }
    }

    public static class BoolConfig extends Config {
        Consumer<Boolean> callback;
        boolean default_;

        public BoolConfig(String name, String configId, boolean default_) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
        }

        public BoolConfig(String name, String configId, boolean default_, String permission) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
            this.permission = permission;
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                boolean val = KamsTweaks.getInstance().getConfig().getBoolean(configId, default_);
                ctx.getSource().getSender().sendMessage("Current value for " + name + " is " + val + ".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission)).then(Commands.argument("value", BoolArgumentType.bool()).executes(ctx -> {
                Boolean val = ctx.getArgument("value", Boolean.class);
                KamsTweaks.getInstance().getConfig().set(configId, val);
                KamsTweaks.getInstance().saveConfig();
                if (callback != null) callback.accept(val);
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }
    }

    public static class IntegerConfig extends Config {
        Consumer<Integer> callback;
        Integer default_;

        public IntegerConfig(String name, String configId, int default_) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
        }

        public IntegerConfig(String name, String configId, int default_, String permission) {
            this.name = name;
            this.configId = configId;
            this.default_ = default_;
            this.permission = permission;
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> registerSubcommand(LiteralArgumentBuilder<CommandSourceStack> command) {
            return command.then(Commands.literal(name).executes(ctx -> {
                int val = KamsTweaks.getInstance().getConfig().getInt(configId, default_);
                ctx.getSource().getSender().sendMessage("Current value for " + name + " is " + val + ".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission)).then(Commands.argument("value", IntegerArgumentType.integer()).executes(ctx -> {
                Integer val = ctx.getArgument("value", Integer.class);
                KamsTweaks.getInstance().getConfig().set(configId, val);
                KamsTweaks.getInstance().saveConfig();
                ctx.getSource().getSender().sendMessage("Set value of " + name + " to " + val + ".");
                if (callback != null) callback.accept(val);
                ctx.getSource().getSender().sendMessage("Successfully set " + configId + " to \"" + val + "\".");
                return Command.SINGLE_SUCCESS;
            }).requires(sender -> permission == null || sender.getSender().hasPermission(permission))));
        }
    }

    public static void registerCommand(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("config");
        for (var sub : configs) {
            command = sub.registerSubcommand(command);
        }
        commands.registrar().register(Commands.literal("kamstweaks").then(command).then(Commands.literal("save").executes(ctx -> {
            KamsTweaks.getInstance().save();
            ctx.getSource().getSender().sendMessage(Component.text("Saved KamsTweaks.").color(NamedTextColor.GREEN));
            return Command.SINGLE_SUCCESS;
        }).requires(source -> source.getSender().hasPermission("kamstweaks.save"))).build(), List.of("kt"));
    }
}
