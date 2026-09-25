package win.demistorm.mcpositioneditor.mixin.vivecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.editor.EditorScreen;
import win.demistorm.mcpositioneditor.client.VRAbstraction;
import win.demistorm.mcpositioneditor.client.VRDebug;

// Vivecraft cancels the HUD hotbar while a screen is open, so null mc.screen just around Gui.render (it renders later)
@Mixin(Gui.class)
public abstract class GuiVRHotbarMixin {

    @Unique
    private static Screen mcpositioneditor$savedScreen;
    @Unique
    private static boolean mcpositioneditor$window;

    @Unique
    private static boolean mcpositioneditor$fired;

    @Inject(method = "render", at = @At("HEAD"))
    private void mcpositioneditor$showHudDuringEditor(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof EditorScreen && VRAbstraction.isActive()) {
            mcpositioneditor$savedScreen = mc.screen;
            mcpositioneditor$window = true;
            mc.screen = null;
            if (!mcpositioneditor$fired) {
                mcpositioneditor$fired = true;
                VRDebug.log("vr-hotbar-vis", "HUD hotbar rendering during editor session");
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void mcpositioneditor$restoreHudAfterEditor(CallbackInfo ci) {
        if (mcpositioneditor$window) {
            Minecraft.getInstance().screen = mcpositioneditor$savedScreen;
            mcpositioneditor$window = false;
            mcpositioneditor$savedScreen = null;
        }
    }
}
