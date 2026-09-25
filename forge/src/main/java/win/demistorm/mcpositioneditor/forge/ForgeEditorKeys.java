package win.demistorm.mcpositioneditor.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import win.demistorm.mcpositioneditor.client.EditorKeys;

// Forge editor keybinds (grab unbound by default)
public final class ForgeEditorKeys {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("mcpositioneditor", "editor")
    );

    public static final KeyMapping TOGGLE = new KeyMapping(EditorKeys.TOGGLE_KEY_NAME, GLFW.GLFW_KEY_P, CATEGORY);
    public static final KeyMapping GRAB = new KeyMapping(EditorKeys.GRAB_KEY_NAME, -1, CATEGORY);

    public static void install() {
        EditorKeys.install(new EditorKeys.Impl() {
            @Override
            public boolean isGrabDown() {
                return !GRAB.isDefault() && GRAB.isDown();
            }

            @Override
            public boolean isGrabCustom() {
                return !GRAB.isDefault();
            }

            @Override
            public int consumeToggleClicks() {
                int clicks = 0;
                while (TOGGLE.consumeClick()) {
                    clicks++;
                }
                return clicks;
            }
        });
    }
}
