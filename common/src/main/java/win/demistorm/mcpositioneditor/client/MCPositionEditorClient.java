package win.demistorm.mcpositioneditor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import win.demistorm.mcpositioneditor.ConfigHelper;
import win.demistorm.mcpositioneditor.editor.EditorController;
import win.demistorm.mcpositioneditor.editor.EditorScreen;
import win.demistorm.mcpositioneditor.editor.GizmoGhostType;
import win.demistorm.mcpositioneditor.editor.OverrideStorage;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

public class MCPositionEditorClient {

    public static void initializeClient() {
        log.info("Easy Item Positions (CLIENT) starting!");

        ConfigHelper.loadOrCreate();

        GizmoGhostType.init();

        VRAbstraction.init();
        log.info("Vivecraft present: {}, VR active: {}", VRAbstraction.isAvailable(), VRAbstraction.isActive());
    }

    public static void onClientTick(Minecraft mc) {
        OverrideStorage.tickFlush();

        if (mc.player == null) {
            if (EditorController.isSessionOpen()) {
                EditorController.closeSession();
            }
            return;
        }

        int clicks = EditorKeys.consumeToggleClicks();
        for (int i = 0; i < clicks; i++) {
            String held = mc.player.getMainHandItem().isEmpty()
                ? "empty" : String.valueOf(mc.player.getMainHandItem().getItem());
            if (EditorController.isSessionOpen() || mc.screen instanceof EditorScreen) {
                VRDebug.log("toggle", "click, closing session (hand=", held + ")");
                if (mc.screen instanceof EditorScreen) {
                    mc.setScreen(null);
                }
                EditorController.closeSession();
            } else if (EditorController.openSession()) {
                VRDebug.log("toggle", "click, opening editor (hand=", held + ")");
                mc.setScreen(new EditorScreen());
            } else {
                VRDebug.log("toggle", "click REJECTED, hand=", held, "- hold an item first");
                mc.player.sendOverlayMessage(Component.literal("Hold an item to edit its position"));
            }
        }

        VRAbstraction.setEditorScreenActive(mc.screen instanceof EditorScreen);

        boolean vrActive = VRAbstraction.isActive();
        if (vrActive != lastVrActive) {
            lastVrActive = vrActive;
            if (EditorController.isSessionOpen()) {
                EditorController.onVrModeChanged();
                VRDebug.log("vrsession",
                    "hotswitch to ", vrActive ? "VR" : "flatscreen", " mid-session - re-anchored");
            }
        }

        if (EditorController.isSessionOpen() && VRAbstraction.isActive() && mc.screen == null) {
            if (VRDebug.gate("self-heal")) {
                VRDebug.log("self-heal", "re-attaching EditorScreen");
            }
            mc.setScreen(new EditorScreen());
        }
    }

    private static boolean lastVrActive;
}
