package win.demistorm.mcpositioneditor.client;

import java.util.HashMap;
import java.util.Map;

import static win.demistorm.mcpositioneditor.MCPositionEditor.debugMode;
import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

// VR diagnostics with per-tag rate limiting (only active when debugMode is true)
public final class VRDebug {

    private VRDebug() {}

    private static final Map<String, Long> LAST = new HashMap<>();

    public static boolean gate(String tag) {
        if (!debugMode) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = LAST.get(tag);
        if (last == null || now - last >= 1000) {
            LAST.put(tag, now);
            return true;
        }
        return false;
    }

    public static void log(String message) {
        if (!debugMode) {
            return;
        }
        log.info("[VRDBG] {}", message);
    }

    public static void log(String tag, Object... parts) {
        if (!debugMode) {
            return;
        }
        StringBuilder sb = new StringBuilder(tag);
        for (Object p : parts) {
            sb.append(' ').append(p);
        }
        log(sb.toString());
    }
}
