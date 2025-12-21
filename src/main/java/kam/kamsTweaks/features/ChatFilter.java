package kam.kamsTweaks.features;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import kam.kamsTweaks.utils.Pair;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import kam.kamsTweaks.ConfigCommand;
import kam.kamsTweaks.Feature;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatFilter extends Feature {

    public static ChatFilter instance;

    public static class Filter {
        public String name;
        public boolean enabled = true;
        public Pattern pattern = null;
        public String message;
        public boolean fetched = false;
        public String fetchUrl = "";
        public Filter(String name, String message, Pattern regex) {
            this.name = name;
            this.message = message;
            this.pattern = regex;
        }
        public Filter(String name, String message, String fetchUrl) {
            this.name = name;
            this.message = message;
            this.fetched = true;
            this.fetchUrl = fetchUrl;
        }

        public void fetch() {
            if (!fetched) return;
            String str;
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fetchUrl)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                KamsTweaks.getInstance().getDataConfig().set("filter-cache." + name, response.body());
                str = response.body();
            } catch (Exception e) {
                Logger.error("Failed to load updated filter for " + name + "!");
                e.printStackTrace();
                str = KamsTweaks.getInstance().getDataConfig().getString("filter-cache." + name, "");
            }
            List<String> terms = Arrays.stream(str.split("[,\n]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            String regex = ("\\b(" + String.join("|", terms) + ")\\b").replace("*", ".*");
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
    }

    List<Filter> filters = new ArrayList<>();

    public void setup() {
        instance = this;
        var cfg = KamsTweaks.getInstance().getConfig();
        if (cfg.contains("filters")) {
            for (var str : Objects.requireNonNull(cfg.getConfigurationSection("filters")).getKeys(false)) {
                Filter filter;
                if (cfg.getBoolean("filters." + str + ".fetch.enabled", false)) {
                    filter = new Filter(str, cfg.getString("filters." + str + ".message", "Your message contains words banned by the server."), cfg.getString("filters." + str + ".fetch.link", ""));
                    filter.fetch();
                } else {
                    filter = new Filter(str, cfg.getString("filters." + str + ".message", "Your message contains words banned by the server."), Pattern.compile(cfg.getString("filters." + str + ".fetch.regex", ""), Pattern.CASE_INSENSITIVE));
                }
                filter.enabled = cfg.getBoolean("filters." + str + ".enabled", true);
                filters.add(filter);
            }
        }
        String str;
        try {
            ConfigCommand.addConfig(new ConfigCommand.BoolConfig("filter.brainrot", "filter.brainrot", true, "kamstweaks.configure"));
            ConfigCommand.addConfig(new ConfigCommand.BoolConfig("filter.slur", "filter.slur", true, "kamstweaks.configure"));
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://extras.km7dev.me/slurFilter")).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            KamsTweaks.getInstance().getDataConfig().set("slur-filter", response.body());
            str = response.body();
        } catch (Exception e) {
            Logger.error("Failed to load updated slur filter!");
            e.printStackTrace();
            str = KamsTweaks.getInstance().getDataConfig().getString("slur-filter", "");
        }
        List<String> terms = Arrays.stream(str.split("[,\n]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        String regex = ("\\b(" + String.join("|", terms) + ")\\b").replace("*", ".*");
    }

    public void shutdown() {
    }

    public void registerCommands(ReloadableRegistrarEvent<@NotNull Commands> commands) {
    }

    public void loadData() {
    }

    public void saveData() {
    }

    PlainTextComponentSerializer ser = PlainTextComponentSerializer.plainText();

    public Pair<Boolean, Filter> isFiltered(String str) {
        for (var filter : filters) {
            if (!filter.enabled) continue;
            if (filter.pattern.matcher(str).find()) {
                return new Pair<>(true, filter);
            }
        }
        return new Pair<>(false, null);
    }

    public Pair<Boolean, Filter> isFiltered(String str, List<String> ignore) {
        for (var filter : filters) {
            if (!filter.enabled || ignore.contains(filter.name)) continue;
            if (filter.pattern.matcher(str).find()) {
                return new Pair<>(true, filter);
            }
        }
        return new Pair<>(false, null);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        var str = ser.serialize(e.message());
        var res = isFiltered(str);
        if (res.first) {
            Logger.warn("Message by " + e.getPlayer().getName() + " was caught by the " + res.second.name + " automod: " + str);
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onSign(SignChangeEvent e) {
        var comps = Component.text("");
        var f = true;
        for (var line : e.lines()) {
            var s = ser.serialize(line);
            if (s.isBlank()) continue;
            if (!f) comps = comps.appendNewline();
            comps = comps.append(line);
            f = false;
        }
        var str = ser.serialize(comps);
        var res = isFiltered(str);
        if (res.first) {
            Logger.warn("Sign by " + e.getPlayer().getName() + " was caught by the " + res.second.name + " automod: " + str);
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onAnvilRename(InventoryClickEvent e) {
        if (e.getClickedInventory() instanceof AnvilInventory inv) {
            if (e.getSlot() == 2) {
                var item = inv.getResult();
                if (item == null) return;
                var str = ser.serialize(item.displayName());
                var res = isFiltered(str);
                if (res.first) {
                    Logger.warn("Anvil rename by " + e.getWhoClicked().getName() + " was caught by the " + res.second.name + " automod: " + str);
                    e.setCancelled(true);
                    e.getWhoClicked().sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
                }
            }
        }
    }
}
