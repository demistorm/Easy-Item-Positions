package win.demistorm.mcpositioneditor.mixin.vivecraft;

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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.client.VRAbstraction;
import win.demistorm.mcpositioneditor.editor.FixedOrderCollector;
import win.demistorm.mcpositioneditor.editor.GizmoRenderer;

// VR hands go through Vivecraft's own method, the gizmo hijacks its item submit
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsVrGizmoMixin {

    @ModifyVariable(method = "vivecraft$vrRenderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private SubmitNodeCollector mcpositioneditor$topOrderCollector(SubmitNodeCollector collector) {
        return FixedOrderCollector.wrapIfStorage(collector);
    }

    @Inject(
        method = "vivecraft$vrRenderArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            shift = At.Shift.AFTER),
        require = 0)
    private void mcpositioneditor$vrHandGizmo(PlayerRenderState playerState,
                                              FirstPersonHandsAndItemsRenderState state,
                                              float partialTick, InteractionHand hand, float swingProgress,
                                              ItemStack itemStack, float equippedProgress, PoseStack poseStack,
                                              SubmitNodeCollector collector, int combinedLight, CallbackInfo ci) {
        boolean mainArmRight = playerState.avatarRenderState == null
            || playerState.avatarRenderState.mainArm == HumanoidArm.RIGHT;
        ItemDisplayContext mainCtx = VRAbstraction.effectiveHeldContext(mainArmRight);
        ItemDisplayContext ctx = hand == InteractionHand.MAIN_HAND ? mainCtx : mirror(mainCtx);
        GizmoRenderer.renderHandGizmo(itemStack, ctx, poseStack, collector);
    }

    private static ItemDisplayContext mirror(ItemDisplayContext ctx) {
        return switch (ctx) {
            case FIRST_PERSON_RIGHT_HAND -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            case FIRST_PERSON_LEFT_HAND -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            case THIRD_PERSON_RIGHT_HAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            default -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        };
    }
}
