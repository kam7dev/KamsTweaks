package kam.kamsTweaks;

import com.eduardomcb.discord.webhook.WebhookClient;
import com.eduardomcb.discord.webhook.WebhookManager;
import com.eduardomcb.discord.webhook.models.Message;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Logger {
    static WebhookManager webhook;
    static boolean inited = false;
    public static void init() {
        if (inited) return;
        inited = true;
        String url = KamsTweaks.getInstance().getConfig().getString("dev-webhook", "");
        if (url.isEmpty()) return;
        webhook = new WebhookManager().setChannelUrl(url);
	webhook.setListener(new WebhookClient.Callback() {
		@Override
		public void onSuccess(String response) {
		        
  		}

    		@Override
    		public void onFailure(int statusCode, String errorMessage) {
        		KamsTweaks.getInstance().getLogger().warning("Failed to send to dev webhook! Code: " + statusCode + ", error: " + errorMessage);
    		}
	});
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("kamslogger")
                .requires(sender -> sender.getSender().getName().equals("km7dev"))
                .then(Commands.literal("set-url").then(Commands.argument("url", StringArgumentType.string()).executes(ctx -> {
                    String url = ctx.getArgument("url", String.class);
                    KamsTweaks.getInstance().getConfig().set("dev-webhook", url);
                    KamsTweaks.getInstance().saveConfig();
                    if (webhook == null) {
                        webhook = new WebhookManager().setChannelUrl(url);
			webhook.setListener(new WebhookClient.Callback() {
				@Override                                                                       public void onSuccess(String response) {

				}

				@Override
				public void onFailure(int statusCode, String errorMessage) {
					KamsTweaks.getInstance().getLogger().warning("Failed to send to dev webhook! Code: " + statusCode + ", error: " + errorMessage);
				}
			});
                    } else {
			webhook.setChannelUrl(url);
                    }
                    ctx.getSource().getSender().sendMessage("Successfully set webhook url.");
                    return Command.SINGLE_SUCCESS;
		}))).then(Commands.literal("Log").executes(ctx -> {
                    Logger.info("Test log");
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("Warn").executes(ctx -> {
                    Logger.warn("Test warn");
                    return Command.SINGLE_SUCCESS;
                })).then(Commands.literal("Error").executes(ctx -> {
                    Logger.error("Test error");
                    return Command.SINGLE_SUCCESS;
                }));
        LiteralCommandNode<CommandSourceStack> buildCommand = command.build();
        commands.registrar().register(buildCommand);
    }

    static void sendToHook(String message) {
        if (webhook != null) {
            if (message.length() > 1000) {

                for (int i = 0; i < message.length(); i += 1000) {
                    String chunk = message.substring(i, Math.min(message.length(), i + 1000));
                    webhook.setMessage(new Message().setContent(chunk).setUsername("KamsTweaks").setAvatarUrl("https://raw.githubusercontent.com/Kingminer7/pixel-art/refs/heads/main/pfp/pfp-transparent.png"));
		    webhook.exec();
                }
            } else {
		webhook.setMessage(new Message().setContent(message).setUsername("KamsTweaks").setAvatarUrl("https://raw.githubusercontent.com/Kingminer7/pixe  l-art/refs/heads/main/pfp/pfp-transparent.png"));
		webhook.exec();
            }
        }
    }

    public static void info(String message) {
        KamsTweaks.getInstance().getLogger().info(message);
        sendToHook(message);
    }

    public static void warn(String message) {
        KamsTweaks.getInstance().getLogger().warning(message);
        sendToHook(message);
    }

    public static void error(String message) {
        KamsTweaks.getInstance().getLogger().severe(message);
        sendToHook(message);
    }
}
