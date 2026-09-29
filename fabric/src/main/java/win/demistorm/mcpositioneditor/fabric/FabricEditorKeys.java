package win.demistorm.mcpositioneditor.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import win.demistorm.mcpositioneditor.client.EditorKeys;

// Fabric editor keybinds (grab unbound by default)
public final class FabricEditorKeys {

    private FabricEditorKeys() {}

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("mcpositioneditor", "editor")
    );

    private static KeyMapping toggle;
    private static KeyMapping grab;

    public static void register() {
        toggle = new KeyMapping(EditorKeys.TOGGLE_KEY_NAME, InputConstants.KEY_P, CATEGORY);
        grab = new KeyMapping(EditorKeys.GRAB_KEY_NAME, 0, CATEGORY);
        KeyMappingHelper.registerKeyMapping(toggle);
        KeyMappingHelper.registerKeyMapping(grab);

        EditorKeys.install(new EditorKeys.Impl() {
            @Override
            public boolean isGrabDown() {
                return !grab.isDefault() && grab.isDown();
            }

            @Override
            public boolean isGrabCustom() {
                return !grab.isDefault();
            }

            @Override
            public int consumeToggleClicks() {
                int clicks = 0;
                while (toggle.consumeClick()) {
                    clicks++;
                }
                return clicks;
            }
        });
    }
}
