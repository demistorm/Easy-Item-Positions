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
                mc.player.sendOverlayMessage(Component.literal("Hold an item to edit its position"));
            }
        });
        return 1;
    }

    public static int export() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            String result = EditorController.export();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("[EIP] " + result));
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
                mc.player.sendSystemMessage(Component.literal("[EIP] Cleared all saved overrides"));
            }
        });
        return 1;
    }
}
