package win.demistorm.mcpositioneditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import win.demistorm.mcpositioneditor.editor.OverrideStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

// Client-only editor settings (stored in the settings section of easyitempositions.json)
public final class ConfigHelper {

    public static final class Data {
        public boolean boundsCenteredGizmos = false;
        public boolean autoApplyVariants = false;
        public float[] vrPreviewHandRot = {0.0f, 0.0f, 0.0f};
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path FILE = Path.of("config", "easyitempositions.json");

    public static final Data ACTIVE = new Data();

    public static void loadOrCreate() {
        JsonObject root = readRoot();
        if (root == null) {
            root = new JsonObject();
            root.add("models", new JsonObject());
        }
        Data d;
        try {
            d = GSON.fromJson(root.get("settings"), Data.class);
        } catch (Exception e) {
            d = null;
        }
        if (d == null) {
            d = new Data();
        }
        if (d.vrPreviewHandRot == null || d.vrPreviewHandRot.length != 3) {
            d.vrPreviewHandRot = new float[]{0.0f, 0.0f, 0.0f};
        }
        ACTIVE.boundsCenteredGizmos = d.boundsCenteredGizmos;
        ACTIVE.autoApplyVariants = d.autoApplyVariants;
        ACTIVE.vrPreviewHandRot = d.vrPreviewHandRot;
        // Rewriting on load sneaks new fields into old files and creates file if absent
        root.addProperty("version", 2);
        root.add("settings", GSON.toJsonTree(ACTIVE));
        writeRoot(root);
        log.info("[Config] boundsCenteredGizmos={}, autoApplyVariants={}",
            ACTIVE.boundsCenteredGizmos, ACTIVE.autoApplyVariants);
    }


    public static void write() {
        OverrideStorage.flush();
    }

    private static JsonObject readRoot() {
        try {
            JsonElement el = JsonParser.parseString(Files.readString(FILE));
            if (el.isJsonObject()) {
                return el.getAsJsonObject();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void writeRoot(JsonObject root) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root));
        } catch (IOException e) {
            log.error("Unable to write client config!", e);
        }
    }

    private ConfigHelper() {}
}
