package win.demistorm.mcpositioneditor.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;
import win.demistorm.mcpositioneditor.editor.OverrideStorage;
import win.demistorm.mcpositioneditor.editor.TransformOverrideManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

// Dumps the saved override map into a standalone resource pack
public final class ExportManager {

    private ExportManager() {}

    public static final String PACK_DIR_NAME = "EasyItemPositions";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static String exportAll() {
        Map<Identifier, Map<ItemDisplayContext, net.minecraft.client.resources.model.cuboid.ItemTransform>> overrides =
            TransformOverrideManager.overrides();
        if (overrides.isEmpty()) {
            return "Nothing to export - save some transforms first";
        }

        Minecraft mc = Minecraft.getInstance();
        Path packRoot = new File(mc.gameDirectory, "resourcepacks").toPath().resolve(PACK_DIR_NAME);
        int written = 0;
        int failed = 0;
        List<Identifier> exported = new ArrayList<>();

        try {
            Files.createDirectories(packRoot);
            writePackMeta(packRoot);
        } catch (IOException e) {
            log.error("Could not create export pack at {}", packRoot, e);
            return "Export failed: " + e.getMessage();
        }

        for (Map.Entry<Identifier, Map<ItemDisplayContext, net.minecraft.client.resources.model.cuboid.ItemTransform>> entry : overrides.entrySet()) {
            Identifier modelId = entry.getKey();
            try {
                JsonObject root = readOriginalModel(mc, modelId);
                JsonObject display = root.has("display") && root.get("display").isJsonObject()
                    ? root.getAsJsonObject("display") : new JsonObject();

                for (Map.Entry<ItemDisplayContext, net.minecraft.client.resources.model.cuboid.ItemTransform> ctxEntry : entry.getValue().entrySet()) {
                    display.add(ctxEntry.getKey().getSerializedName(), serialize(ctxEntry.getValue()));
                }
                root.add("display", display);

                Path target = packRoot.resolve("assets").resolve(modelId.getNamespace())
                    .resolve("models").resolve(modelId.getPath() + ".json");
                Files.createDirectories(target.getParent());
                Files.writeString(target, GSON.toJson(root), StandardCharsets.UTF_8);
                written++;
                exported.add(modelId);
            } catch (Exception e) {
                failed++;
                log.error("Failed exporting model {}", modelId, e);
            }
        }

        TransformOverrideManager.removeOverrides(exported);
        OverrideStorage.flush();

        String result = "Exported " + written + " model(s) to resourcepacks/" + PACK_DIR_NAME;
        if (!exported.isEmpty()) {
            result += ", cleared " + exported.size() + " local override(s)";
        }
        if (failed > 0) {
            result += " (" + failed + " failed, kept locally)";
        }
        result += " - enable the pack, then F3+T";
        log.info(result);
        return result;
    }

    private static void writePackMeta(Path packRoot) throws IOException {
        Path meta = packRoot.resolve("pack.mcmeta");
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 69);
        pack.addProperty("description", "Easy Item Positions transform overrides");
        pack.addProperty("min_format", 69);
        pack.addProperty("max_format", 99);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        Files.writeString(meta, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static JsonObject readOriginalModel(Minecraft mc, Identifier modelId) throws IOException {
        Identifier resource = Identifier.fromNamespaceAndPath(modelId.getNamespace(), "models/" + modelId.getPath() + ".json");
        Resource res = mc.getResourceManager().getResourceOrThrow(resource);
        try (InputStream in = res.open()) {
            JsonElement element = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        }
        return new JsonObject();
    }

    private static JsonObject serialize(net.minecraft.client.resources.model.cuboid.ItemTransform t) {
        JsonObject obj = new JsonObject();
        obj.add("rotation", vec(t.rotation(), 1.0f));
        obj.add("translation", vec(t.translation(), 16.0f));
        obj.add("scale", vec(t.scale(), 1.0f));
        return obj;
    }

    private static JsonArray vec(org.joml.Vector3fc v, float mul) {
        JsonArray arr = new JsonArray(3);
        arr.add(round(v.x() * mul));
        arr.add(round(v.y() * mul));
        arr.add(round(v.z() * mul));
        return arr;
    }

    private static float round(float f) {
        return Math.round(f * 1000.0f) / 1000.0f;
    }
}
