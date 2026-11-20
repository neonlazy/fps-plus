package it.hiddenmultikeys;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import it.hiddenmultikeys.mixin.KeyMappingAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Config + lookup dei tasti extra associati ad ogni azione (KeyMapping). */
public final class ExtraBindings {
    public static final int MAX_EXTRA = 4;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hidden_multikeys.json");

    /** id azione (KeyMapping#getName) -> tasti extra */
    private static final Map<String, List<InputConstants.Key>> EXTRA = new HashMap<>();
    /** tasti attualmente premuti (tracciati dal mixin su KeyMapping.set) */
    private static final Set<InputConstants.Key> HELD = new HashSet<>();
    private static Map<InputConstants.Key, List<KeyMapping>> lookup = null;

    private ExtraBindings() {}

    public static void load() {
        EXTRA.clear();
        if (!Files.exists(FILE)) return;
        try (Reader r = Files.newBufferedReader(FILE)) {
            Map<String, List<String>> raw = GSON.fromJson(r, new TypeToken<Map<String, List<String>>>() {}.getType());
            if (raw == null) return;
            raw.forEach((id, names) -> {
                List<InputConstants.Key> keys = new ArrayList<>();
                for (String n : names) {
                    try {
                        InputConstants.Key k = InputConstants.getKey(n);
                        if (!keys.contains(k) && keys.size() < MAX_EXTRA) keys.add(k);
                    } catch (RuntimeException ignored) {
                    }
                }
                if (!keys.isEmpty()) EXTRA.put(id, keys);
            });
        } catch (Exception e) {
            System.err.println("[hidden_multikeys] impossibile leggere la config: " + e);
        }
        lookup = null;
    }

    public static void save() {
        Map<String, List<String>> raw = new TreeMap<>();
        EXTRA.forEach((id, keys) -> {
            if (!keys.isEmpty()) raw.put(id, keys.stream().map(InputConstants.Key::getName).toList());
        });
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(raw, w);
        } catch (Exception e) {
            System.err.println("[hidden_multikeys] impossibile salvare la config: " + e);
        }
        lookup = null;
    }

    /** Lista modificabile dei tasti extra di un'azione. */
    public static List<InputConstants.Key> list(KeyMapping km) {
        return EXTRA.computeIfAbsent(km.getName(), k -> new ArrayList<>());
    }

    public static List<InputConstants.Key> view(KeyMapping km) {
        return EXTRA.getOrDefault(km.getName(), List.of());
    }

    /** Azioni che hanno {@code key} come tasto extra. */
    public static List<KeyMapping> bindingsFor(InputConstants.Key key) {
        if (lookup == null) rebuild();
        if (lookup == null) return List.of();
        return lookup.getOrDefault(key, List.of());
    }

    private static void rebuild() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        Map<InputConstants.Key, List<KeyMapping>> m = new HashMap<>();
        for (KeyMapping km : mc.options.keyMappings) {
            for (InputConstants.Key k : EXTRA.getOrDefault(km.getName(), List.of())) {
                m.computeIfAbsent(k, x -> new ArrayList<>()).add(km);
            }
        }
        lookup = m;
    }

    public static Set<InputConstants.Key> held() {
        return HELD;
    }

    /** Vero se il tasto principale o uno qualsiasi degli extra è premuto. */
    public static boolean anyDown(KeyMapping km) {
        InputConstants.Key primary = ((KeyMappingAccessor) km).mkb$getKey();
        if (HELD.contains(primary)) return true;
        for (InputConstants.Key k : view(km)) if (HELD.contains(k)) return true;
        return false;
    }

    /** Azioni con almeno un tasto extra. */
    public static List<KeyMapping> withExtras() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return List.of();
        List<KeyMapping> out = new ArrayList<>();
        for (KeyMapping km : mc.options.keyMappings) if (!view(km).isEmpty()) out.add(km);
        return out;
    }
}
