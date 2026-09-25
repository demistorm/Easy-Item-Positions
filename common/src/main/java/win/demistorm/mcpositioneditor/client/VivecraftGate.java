package win.demistorm.mcpositioneditor.client;

// Reflection-only Vivecraft present gate
public final class VivecraftGate {

    private VivecraftGate() {}

    private static final boolean VIVECRAFT_PRESENT = checkPresent();

    private static boolean checkPresent() {
        try {
            Class.forName("org.vivecraft.api.VRAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isVivecraftPresent() {
        return VIVECRAFT_PRESENT;
    }

    public static boolean isVRActive() {
        if (!VIVECRAFT_PRESENT) {
            return false;
        }
        try {
            Class<?> api = Class.forName("org.vivecraft.api.client.VRClientAPI");
            Object instance = api.getMethod("instance").invoke(null);
            return (boolean) api.getMethod("isVRActive").invoke(instance);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
