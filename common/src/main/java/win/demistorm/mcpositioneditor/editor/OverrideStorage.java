package win.demistorm.mcpositioneditor.editor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

// Persists saved overrides to config/easyitempositions.json (survives restarts so not everything has to be exported to resourcepacks))
public final class OverrideStorage {

    private OverrideStorage() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long FLUSH_INTERVAL_MS = 5000;

    private static boolean loaded;
    private static boolean dirty;
    private static long lastFlushMs;
    private static boolean suppressDirty;

    private static Path configFile() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        return configDir.toPath().resolve("easyitempositions.json");
    }

    public static void markDirty() {
        if (!suppressDirty) {
            dirty = true;
        }
    }

    public static void tickFlush() {
        ensureLoaded();
        if (dirty && System.currentTimeMillis() - lastFlushMs >= FLUSH_INTERVAL_MS) {
            flush();
        }
    }

    public static void flush() {
        ensureLoaded();
        try {
            Path file = configFile();
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            JsonObject models = new JsonObject();
            for (Map.Entry<Identifier, Map<ItemDisplayContext, ItemTransform>> e
                : TransformOverrideManager.overrides().entrySet()) {
                JsonObject contexts = new JsonObject();
                for (Map.Entry<ItemDisplayContext, ItemTransform> c : e.getValue().entrySet()) {
                    contexts.add(c.getKey().getSerializedName(), serialize(c.getValue()));
                }
                models.add(e.getKey().toString(), contexts);
            }
            root.add("models", models);
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
            dirty = false;
            lastFlushMs = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Failed writing override config", e);
        }
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        Path file;
        try {
            file = configFile();
        } catch (Throwable t) {
            return;
        }
        loaded = true;
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonElement element = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject models = element.getAsJsonObject().getAsJsonObject("models");
            if (models == null) {
                return;
            }
            int restored = 0;
            for (Map.Entry<String, JsonElement> me : models.entrySet()) {
                Identifier modelId = Identifier.tryParse(me.getKey());
                if (modelId == null || !me.getValue().isJsonObject()) {
                    log.warn("Skipping bad model entry {} in override config", me.getKey());
                    continue;
                }
                for (Map.Entry<String, JsonElement> ce : me.getValue().getAsJsonObject().entrySet()) {
                    ItemDisplayContext ctx = contextByName(ce.getKey());
                    if (ctx == null || !ce.getValue().isJsonObject()) {
                        log.warn("Skipping bad context {}.{} in override config", me.getKey(), ce.getKey());
                        continue;
                    }
                    ItemTransform t = deserialize(ce.getValue().getAsJsonObject());
                    if (t != null) {
                        suppressDirty = true;
                        TransformOverrideManager.setOverride(modelId, ctx, t);
                        suppressDirty = false;
                        restored++;
                    }
                }
            }
            if (restored > 0) {
                log.info("Restored {} saved override(s) from config", restored);
            }
        } catch (Exception e) {
            log.error("Failed reading override config", e);
        }
    }

    private static ItemDisplayContext contextByName(String name) {
        for (ItemDisplayContext ctx : ItemDisplayContext.values()) {
            if (ctx.getSerializedName().equals(name)) {
                return ctx;
            }
        }
        return null;
    }

    // Translation stays in internal model units (the x16 JSON convention only applies to exports)
    private static JsonObject serialize(ItemTransform t) {
        JsonObject obj = new JsonObject();
        obj.add("rotation", vec(t.rotation(), 1.0f));
        obj.add("translation", vec(t.translation(), 1.0f));
        obj.add("scale", vec(t.scale(), 1.0f));
        return obj;
    }

    private static ItemTransform deserialize(JsonObject obj) {
        Vector3f rot = vec(obj, "rotation");
        Vector3f trans = vec(obj, "translation");
        Vector3f scale = vec(obj, "scale");
        if (rot == null || trans == null || scale == null) {
            return null;
        }
        trans.set(Mth.clamp(trans.x, -EditorController.MAX_TRANSLATION, EditorController.MAX_TRANSLATION),
            Mth.clamp(trans.y, -EditorController.MAX_TRANSLATION, EditorController.MAX_TRANSLATION),
            Mth.clamp(trans.z, -EditorController.MAX_TRANSLATION, EditorController.MAX_TRANSLATION));
        scale.set(Mth.clamp(scale.x, -EditorController.MAX_SCALE, EditorController.MAX_SCALE),
            Mth.clamp(scale.y, -EditorController.MAX_SCALE, EditorController.MAX_SCALE),
            Mth.clamp(scale.z, -EditorController.MAX_SCALE, EditorController.MAX_SCALE));
        return new ItemTransform(rot, trans, scale);
    }

    private static com.google.gson.JsonArray vec(org.joml.Vector3fc v, float mul) {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray(3);
        arr.add(round(v.x() * mul));
        arr.add(round(v.y() * mul));
        arr.add(round(v.z() * mul));
        return arr;
    }

    private static Vector3f vec(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray() || obj.getAsJsonArray(key).size() != 3) {
            return null;
        }
        com.google.gson.JsonArray a = obj.getAsJsonArray(key);
        try {
            return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static float round(float f) {
        return Math.round(f * 10000.0f) / 10000.0f;
    }
}
