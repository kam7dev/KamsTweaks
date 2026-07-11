package kam.kamsTweaks.features.moderation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import kam.kamsTweaks.ext.SRVHelper;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.features.fun.nicknames.Names;
import kam.kamsTweaks.managers.KTStrings;
import kam.kamsTweaks.utils.Pair;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;

import io.papermc.paper.event.player.AsyncChatEvent;
import kam.kamsTweaks.KamsTweaks;
import kam.kamsTweaks.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(fetchUrl)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                KamsTweaks.get().getDataConfig().set("filter-cache." + name, response.body());
                str = response.body();
            } catch (Exception e) {
                Logger.error("Failed to load updated filter for " + name + ". Exception printed below.");
                Logger.handleException(e);
                str = KamsTweaks.get().getDataConfig().getString("filter-cache." + name, "");
            }
            List<String> terms = Arrays.stream(str.split("[,\n]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
            String regex = ("\\b(" + String.join("|", terms) + ")\\b").replace("*", ".*");
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
    }

    List<Filter> filters = new ArrayList<>();

    public void setup() {
        instance = this;
        var cfg = KamsTweaks.get().getConfig();
        if (cfg.contains("chat-filters")) {
            for (var str : Objects.requireNonNull(cfg.getConfigurationSection("chat-filters")).getKeys(false)) {
                Filter filter;
                if (cfg.getBoolean("chat-filters." + str + ".fetch.enabled", false)) {
                    filter = new Filter(str, cfg.getString("chat-filters." + str + ".message", "Your message contains words banned by the server."), cfg.getString("chat-filters." + str + ".fetch.link", ""));
                    filter.fetch();
                } else {
                    List<String> terms = Arrays.stream(cfg.getString("chat-filters." + str + ".regex", "").split("[,\n]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
                    String regex = ("\\b(" + String.join("|", terms) + ")\\b").replace("*", ".*");
                    if (cfg.getString("chat-filters." + str + ".regex", "").isEmpty()) return;
                    filter = new Filter(str, cfg.getString("chat-filters." + str + ".message", "Your message contains words banned by the server."), Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                }
                filter.enabled = cfg.getBoolean("chat-filters." + str + ".enabled", true);
                filters.add(filter);
            }
        }
    }

    public Pair<Boolean, Filter> isFiltered(String str) {
        for (var filter : filters) {
            if (!filter.enabled) continue;
            var m = filter.pattern.matcher(str);
            if (m.find()) {
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent e) {
        var str = Names.pt.serialize(e.message());
        var res = isFiltered(str);
        if (res.first) {
            warnStaff(KTStrings.getFor(KTStrings.AUTOMOD_CHAT, Component.text(e.getPlayer().getName()), Component.text(res.second.name), Component.text(str)));
            e.setCancelled(true);
            e.getPlayer().sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onSign(SignChangeEvent e) {
        var comps = Component.text("");
        var f = true;
        for (var line : e.lines()) {
            var s = Names.pt.serialize(line);
            if (s.isBlank()) continue;
            if (!f) comps = comps.appendNewline();
            comps = comps.append(line);
            f = false;
        }
        var str = Names.pt.serialize(comps);
        var res = isFiltered(str);
        if (res.first) {
            warnStaff(KTStrings.getFor(KTStrings.AUTOMOD_SIGN, Component.text(e.getPlayer().getName()), Component.text(res.second.name), Component.text(str)));
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
                var str = Names.pt.serialize(item.displayName());
                var res = isFiltered(str);
                if (res.first) {
                    warnStaff(KTStrings.getFor(KTStrings.AUTOMOD_NAME, Component.text(e.getWhoClicked().getName()), Component.text(res.second.name), Component.text(str)));
                    e.setCancelled(true);
                    e.getWhoClicked().sendMessage(Component.text(res.second.message).color(NamedTextColor.RED));
                }
            }
        }
    }

    public static void warnStaff(Component message) {
        Logger.warn(Names.pt.serialize(message));
        KamsTweaks.get().sendToOps(message.color(NamedTextColor.RED));
        SRVHelper.messageAutomodChannel(PlainTextComponentSerializer.plainText().serialize(message));
    }
}
