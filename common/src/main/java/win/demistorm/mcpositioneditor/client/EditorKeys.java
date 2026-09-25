package win.demistorm.mcpositioneditor.client;

// Loader-generic key state (KeyMappings are in the loader modules)
public final class EditorKeys {

    private EditorKeys() {}

    public static final String TOGGLE_KEY_NAME = "key.mcpositioneditor.toggle_editor";
    public static final String GRAB_KEY_NAME = "key.mcpositioneditor.grab_gizmo";

    public interface Impl {
        boolean isGrabDown();

        boolean isGrabCustom();

        int consumeToggleClicks();
    }

    private static Impl impl = new Impl() {
        @Override
        public boolean isGrabDown() {
            return false;
        }

        @Override
        public boolean isGrabCustom() {
            return false;
        }

        @Override
        public int consumeToggleClicks() {
            return 0;
        }
    };

    public static void install(Impl loaderImpl) {
        impl = loaderImpl;
    }

    public static boolean isGrabDown() {
        return impl.isGrabDown();
    }

    public static boolean isGrabCustom() {
        return impl.isGrabCustom();
    }

    public static int consumeToggleClicks() {
        return impl.consumeToggleClicks();
    }
}
