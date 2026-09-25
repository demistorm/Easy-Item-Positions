package win.demistorm.mcpositioneditor.mixin.vivecraft;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.ClientDataHolderVR;
import win.demistorm.mcpositioneditor.editor.EditorScreen;

// Any open screen forces Vivecraft's gray menu hands, so treat EditorScreen as having "no screen" for this only
@Mixin(targets = "org.vivecraft.client_vr.gameplay.VRPlayer", remap = false)
public abstract class VRPlayerMixin {

    @Inject(method = "preRender", at = @At("TAIL"))
    private void mcpositioneditor$keepRealHandsInEditor(float partialTick, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof EditorScreen) {
            ClientDataHolderVR dh = ClientDataHolderVR.getInstance();
            dh.menuHandMain = false;
            dh.menuHandOff = false;
        }
    }
}
