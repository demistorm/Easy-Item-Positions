package win.demistorm.mcpositioneditor.mixin.vivecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.vivecraft.client_vr.provider.MCVR;
import win.demistorm.mcpositioneditor.client.VRPreviewState;

// Feeds the preview a synthetic gun angle (the live read goes through DH.vr, null without a VR session)
@Mixin(targets = "org.vivecraft.client_vr.render.VivecraftItemRendering", remap = false)
public abstract class VivecraftItemRenderingMixin {

    @WrapOperation(
        method = "applyFirstPersonItemTransforms",
        at = @At(value = "INVOKE", target = "Lorg/vivecraft/client_vr/provider/MCVR;getGunAngle()F"))
    private static float mcpositioneditor$previewGunAngle(MCVR mcvr, Operation<Float> original) {
        Float override = VRPreviewState.gunAngleOverride();
        return override != null ? override : original.call(mcvr);
    }
}
