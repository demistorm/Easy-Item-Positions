package win.demistorm.mcpositioneditor.mixin.vivecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import win.demistorm.mcpositioneditor.editor.EditorScreen;
import win.demistorm.mcpositioneditor.client.VRDebug;

// Hide EditorScreen from the touch hotbar's mc.screen reads
@Mixin(targets = "org.vivecraft.client_vr.gameplay.interact_modules.InteractiveHotbarModule")
public abstract class InteractiveHotbarModuleMixin {

    private static boolean mcpositioneditor$fired;

    @ModifyExpressionValue(
        method = {"isActive", "renderDebug"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;")
    )
    private Screen mcpositioneditor$hotbarDuringEditor(Screen original) {
        if (original instanceof EditorScreen) {
            if (!mcpositioneditor$fired) {
                mcpositioneditor$fired = true;
                VRDebug.log("vr-hotbar", "touch hotbar live during editor session");
            }
            return null;
        }
        return original;
    }
}
