package kam.kamsTweaks.utils;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import kam.kamsTweaks.features.Feature;
import kam.kamsTweaks.KamsTweaks;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class UserDataManager extends Feature {
    static class Entry {
        String type;
        JsonElement value;
    }

    static Map<UUID, Map<String, Entry>> data = new HashMap<>();

    private static File file;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final Type TYPE = new TypeToken<Map<UUID, Map<String, Entry>>>() {}.getType();

    @Override
    public void setup() {
        file = new File(KamsTweaks.get().getDataFolder(),  "user-data.json");
    }

    @Override
    public void saveData() {
        try {
            Path target = file.toPath();
            Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");

            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
                writer.flush();
            }

            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(Exception e) {
            Logger.handleException(e);
        }
    }

    @Override
    public void loadData() {
        if (file == null || !file.exists()) {
            data = new HashMap<>();
            return;
        }

        try(Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            data = GSON.fromJson(reader, TYPE);
            if (data == null) data = new HashMap<>();
        } catch (Exception e) {
            Logger.handleException(e);
        }
    }

    public static <T> T get(UUID id, String key, T defaultValue, Class<T> clazz) {
        if (id == null || key == null || clazz == null) return defaultValue;

        Map<String, Entry> inner = data.get(id);
        if (inner == null) return defaultValue;

        Entry e = inner.get(key);
        if (e == null || e.type == null || e.value == null) return defaultValue;

        String name = clazz.getName();
        if (!name.equals(e.type)) throw new IllegalArgumentException(key + " is not of type " + name);

        return GSON.fromJson(e.value, clazz);
    }

    public static <T> void put(UUID id, String key, T value, Class<T> clazz) {
        if (id == null || key == null || clazz == null) return;

        data.computeIfAbsent(id, k -> new HashMap<>());

        if (value == null) {
            data.get(id).remove(key);
            return;
        }

        Entry e = new Entry();
        e.type = clazz.getName();
        e.value = GSON.toJsonTree(value);
        data.get(id).put(key, e);
    }

    public static void erase(UUID id, String key) {
        if (id == null || key == null) return;
        if (!data.containsKey(id)) return;
        data.get(id).remove(key);
    }

    // some easier helpers
    // adding as i need em
    public static boolean get(UUID id, String key, boolean defaultValue) {
        return get(id, key, defaultValue, Boolean.class);
    }

    public static void put(UUID id, String key, boolean value) {
        put(id, key, value, Boolean.class);
    }

    static String stackToString(ItemStack stack) {
        if (stack.isEmpty()) return "_EMPTY_ITEM_STACK_";
        return Base64.getEncoder().encodeToString(stack.serializeAsBytes());
    }

    static ItemStack stringToStack(String string) {
        if (string.equals("_EMPTY_ITEM_STACK_")) return ItemStack.empty();
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(string));
    }

    public static ItemStack getItemStack(UUID id, String key, ItemStack defaultValue) {
        var str = get(id, key, "_DEFAULT_", String.class);
        if (str.equals("_DEFAULT_")) return defaultValue;
        return stringToStack(str);
    }

    public static void putItemStack(UUID id, String key, ItemStack value) {
        put(id, key, stackToString(value), String.class);
    }
}
