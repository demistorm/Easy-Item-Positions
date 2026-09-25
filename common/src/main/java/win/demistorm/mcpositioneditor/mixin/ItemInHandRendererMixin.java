package win.demistorm.mcpositioneditor.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.editor.EditorController;
import win.demistorm.mcpositioneditor.editor.EditorRenderContext;
import win.demistorm.mcpositioneditor.editor.GizmoRenderer;
import win.demistorm.mcpositioneditor.client.VRAbstraction;

// In-hand render path (flags model tracking and adds the gizmo in right after the item)
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void mcpositioneditor$beginHand(LivingEntity livingEntity, ItemStack itemStack,
                                             ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                                             SubmitNodeCollector submitNodeCollector, int i, CallbackInfo ci) {
        EditorRenderContext.pushHandPath();
    }

    @Inject(method = "renderItem", at = @At("TAIL"))
    private void mcpositioneditor$endHand(LivingEntity livingEntity, ItemStack itemStack,
                                          ItemDisplayContext itemDisplayContext, PoseStack poseStack,
                                          SubmitNodeCollector submitNodeCollector, int i, CallbackInfo ci) {
        EditorRenderContext.popHandPath();
        GizmoRenderer.renderHandGizmo(itemStack, itemDisplayContext, poseStack);
    }

    // Preview replaces the vanilla hand with Vivecraft's arm at a synthetic pose (mainhand only, offhand hides, scoping stays vanilla)
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void mcpositioneditor$vrPreviewHand(AbstractClientPlayer player, float partialTick, float pitch,
                                                InteractionHand hand, float swingProgress, ItemStack itemStack,
                                                float equippedProgress, PoseStack poseStack,
                                                SubmitNodeCollector submitNodeCollector, int combinedLight,
                                                CallbackInfo ci) {
        if (!EditorController.isVrPreviewActive() || player.isScoping()) {
            return;
        }
        ci.cancel();
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        VRAbstraction.renderPreviewHand((ItemInHandRenderer) (Object) this, player, itemStack,
            poseStack, submitNodeCollector, combinedLight);
    }
}
