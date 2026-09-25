package win.demistorm.mcpositioneditor.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import win.demistorm.mcpositioneditor.editor.EditorController;
import win.demistorm.mcpositioneditor.editor.EditorScreen;
import win.demistorm.mcpositioneditor.editor.OverrideStorage;
import win.demistorm.mcpositioneditor.editor.TransformOverrideManager;

// The /eip command handlers
public final class Commands {

    private Commands() {}

    public static int edit() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (EditorController.openSession()) {
                mc.setScreen(new EditorScreen());
            } else if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("Hold an item to edit its position"), true);
            }
        });
        return 1;
    }

    public static int export() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            String result = EditorController.export();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("[EIP] " + result), false);
            }
        });
        return 1;
    }

    public static int clear() {
        TransformOverrideManager.clearAll();
        OverrideStorage.flush();
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("[EIP] Cleared all saved overrides"), false);
            }
        });
        return 1;
    }
}
