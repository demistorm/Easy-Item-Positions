package win.demistorm.mcpositioneditor.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.client.VRAbstraction;
import win.demistorm.mcpositioneditor.editor.EditorController;
import win.demistorm.mcpositioneditor.editor.GizmoRenderer;

// In-hand render path
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {

    // The item render itself is inline now so need to anchor to the state submit instead of a method tail
    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
        shift = At.Shift.AFTER))
    private void mcpositioneditor$handGizmo(PlayerRenderState playerState,
                                            FirstPersonHandsAndItemsRenderState state,
                                            float partialTick, float xRot, InteractionHand hand,
                                            float swingProgress, ItemStack itemStack,
                                            float equippedProgress, PoseStack poseStack,
                                            SubmitNodeCollector submitNodeCollector, int combinedLight,
                                            CallbackInfo ci) {
        GizmoRenderer.renderHandGizmo(itemStack, handContext(playerState, hand), poseStack, submitNodeCollector);
    }

    // Preview replaces the vanilla hand with Vivecraft's arm at a synthetic pose
    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void mcpositioneditor$vrPreviewHand(PlayerRenderState playerState,
                                                FirstPersonHandsAndItemsRenderState state,
                                                float partialTick, float xRot, InteractionHand hand,
                                                float swingProgress, ItemStack itemStack,
                                                float equippedProgress, PoseStack poseStack,
                                                SubmitNodeCollector submitNodeCollector, int combinedLight,
                                                CallbackInfo ci) {
        if (!EditorController.isVrPreviewActive() || state.isScoping) {
            return;
        }
        ci.cancel();
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }
        VRAbstraction.renderPreviewHand((FirstPersonHandsAndItemsRenderer) (Object) this, playerState, state,
            itemStack, poseStack, submitNodeCollector, combinedLight);
    }

    // Mirrors how vanilla picks the baked context during extraction
    private static ItemDisplayContext handContext(PlayerRenderState playerState, InteractionHand hand) {
        boolean mainArmRight = playerState.avatarRenderState == null
            || playerState.avatarRenderState.mainArm == HumanoidArm.RIGHT;
        boolean right = (hand == InteractionHand.MAIN_HAND) == mainArmRight;
        return right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }
}
