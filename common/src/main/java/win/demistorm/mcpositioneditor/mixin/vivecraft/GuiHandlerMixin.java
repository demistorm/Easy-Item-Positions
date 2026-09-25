package win.demistorm.mcpositioneditor.mixin.vivecraft;

import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.client.VRAbstraction;
import win.demistorm.mcpositioneditor.client.VRDebug;

// Log-only (onScreenChanged is where Vivecraft places the floating GUI quad)
@Mixin(targets = "org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler", remap = false)
public abstract class GuiHandlerMixin {

    @Shadow
    public static Vector3f GUI_POS_ROOM;

    @Shadow
    public static float GUI_SCALE;

    @Inject(method = "onScreenChanged(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("TAIL"))
    private static void mcpositioneditor$logGuiPos(Screen previousGuiScreen, Screen newScreen, boolean unpressKeys, CallbackInfo ci) {
        if (VRAbstraction.isActive() && newScreen != null) {
            VRDebug.log("gui-pos",
                "screen=", newScreen.getClass().getSimpleName(),
                "pos=", GUI_POS_ROOM == null ? "null"
                    : String.format("%.2f/%.2f/%.2f", GUI_POS_ROOM.x, GUI_POS_ROOM.y, GUI_POS_ROOM.z),
                "scale=", GUI_SCALE);
        }
    }
}
