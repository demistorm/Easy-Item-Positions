package win.demistorm.mcpositioneditor.mixin.vivecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import win.demistorm.mcpositioneditor.client.VRAbstraction;
import win.demistorm.mcpositioneditor.client.VRDebug;
import win.demistorm.mcpositioneditor.editor.EditorScreen;

// Vivecraft cancels the HUD hotbar while a screen is open so hide the screen field only around the hotbar extract
@Mixin(Gui.class)
public abstract class GuiVRHotbarMixin {

    @Shadow
    @Nullable
    private Screen screen;

    @Unique
    private static boolean mcpositioneditor$fired;

    // Restore in a finally, the screen extract a few lines further down needs the real field back
    @WrapOperation(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void mcpositioneditor$showHudDuringEditor(Hud hud, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                                      Operation<Void> original) {
        if (screen instanceof EditorScreen && VRAbstraction.isActive()) {
            Screen saved = screen;
            screen = null;
            try {
                if (!mcpositioneditor$fired) {
                    mcpositioneditor$fired = true;
                    VRDebug.log("vr-hotbar-vis", "HUD hotbar rendering during editor session");
                }
                original.call(hud, graphics, deltaTracker);
            } finally {
                screen = saved;
            }
        } else {
            original.call(hud, graphics, deltaTracker);
        }
    }
}
