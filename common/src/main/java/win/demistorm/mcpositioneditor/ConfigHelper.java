package win.demistorm.mcpositioneditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

// Client-only editor settings
public final class ConfigHelper {

    public static final class Data {
        public boolean boundsCenteredGizmos = false;
        public boolean autoApplyVariants = false;
        public float[] previewHandRot = {0.0f, 0.0f, 0.0f};
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIGDIR = Path.of("config");
    private static final Path FILE = CONFIGDIR.resolve("easyitempositions-client.json");

    public static final Data ACTIVE = new Data();

    public static void loadOrCreate() {
        Data d = read();
        write(d);
        ACTIVE.boundsCenteredGizmos = d.boundsCenteredGizmos;
        ACTIVE.autoApplyVariants = d.autoApplyVariants;
        if (d.previewHandRot == null || d.previewHandRot.length != 3) {
            d.previewHandRot = new float[]{0.0f, 0.0f, 0.0f};
        }
        ACTIVE.previewHandRot = d.previewHandRot;
        log.info("[Config] boundsCenteredGizmos={}, autoApplyVariants={}",
            ACTIVE.boundsCenteredGizmos, ACTIVE.autoApplyVariants);
    }

    private static Data read() {
        try {
            if (Files.exists(FILE))
                return GSON.fromJson(Files.readString(FILE), Data.class);
        } catch (IOException ignored) {}
        return new Data();
    }

    public static void write(Data d) {
        try {
            Files.createDirectories(CONFIGDIR);
            Files.writeString(FILE, GSON.toJson(d));
        } catch (IOException e) {
            log.error("Unable to write client config!", e);
        }
    }

    private ConfigHelper() {}
}
