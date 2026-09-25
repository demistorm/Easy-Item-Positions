package win.demistorm.mcpositioneditor.editor;

import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class EditorRenderContext {

    private EditorRenderContext() {}

    private static final ThreadLocal<Identifier> CURRENT_MODEL = new ThreadLocal<>();
    private static final ThreadLocal<Integer> HAND_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static void beginModel(Identifier modelId, ItemStack stack, ItemDisplayContext ctx) {
        CURRENT_MODEL.set(modelId);
        if (modelId != null && inHandPath()) {
            EditorController.noteRenderedModel(modelId, stack, ctx);
        }
    }

    public static void endModel() {
        CURRENT_MODEL.remove();
    }

    public static Identifier currentModel() {
        return CURRENT_MODEL.get();
    }

    public static void pushHandPath() {
        HAND_DEPTH.set(HAND_DEPTH.get() + 1);
    }

    public static void popHandPath() {
        HAND_DEPTH.set(Math.max(0, HAND_DEPTH.get() - 1));
    }

    public static boolean inHandPath() {
        return HAND_DEPTH.get() > 0;
    }

    public static Identifier idOf(BlockModelWrapper wrapper) {
        return TransformOverrideManager.wrapperId(wrapper);
    }
}
