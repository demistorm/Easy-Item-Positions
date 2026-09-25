package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.editor.GizmoRenderer;

// The ghost gizmo must draw after endBatch flushes (drawing earlier gets covered by item model)
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class MultiBufferSourceMixin {

    @Inject(method = "endBatch()V", at = @At("TAIL"))
    private void mcpositioneditor$afterFlush(CallbackInfo ci) {
        GizmoRenderer.drawPendingGhost();
    }
}
