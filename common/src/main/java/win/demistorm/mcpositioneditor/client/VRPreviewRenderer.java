package win.demistorm.mcpositioneditor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.extensions.EntityRenderDispatcherVRExtension;
import org.vivecraft.client_vr.render.VRArmRenderer;
import org.vivecraft.client_vr.render.VRShaders;
import org.vivecraft.client_vr.render.VivecraftItemRendering;
import org.vivecraft.client_vr.render.VivecraftItemRendering.VivecraftItemTransformType;
import win.demistorm.mcpositioneditor.ConfigHelper;
import win.demistorm.mcpositioneditor.editor.GizmoRenderer;

// Renders the Vivecraft arm and item at a synthetic controller pose (VR context option)
public final class VRPreviewRenderer {

    private VRPreviewRenderer() {}

    public static final Vector3f HAND_ROT = new Vector3f(
        ConfigHelper.ACTIVE.vrPreviewHandRot.length == 3 ? ConfigHelper.ACTIVE.vrPreviewHandRot[0] : 0.0f,
        ConfigHelper.ACTIVE.vrPreviewHandRot.length == 3 ? ConfigHelper.ACTIVE.vrPreviewHandRot[1] : 0.0f,
        ConfigHelper.ACTIVE.vrPreviewHandRot.length == 3 ? ConfigHelper.ACTIVE.vrPreviewHandRot[2] : 0.0f);

    // Quest 2/Pro/Plus controller poses (offsets pulled from NullVR.deviceOffsets) (should translate to other controllers pretty close)
    private static final float BEND_G = 37.4f;
    private static final float GRIP_PITCH = 70.0f;
    private static final float STANCE_YAW = 15.0f;
    private static final float FWD_OFFSET = 0.583f;
    private static final float DOWN_OFFSET = 0.20f;
    private static final float SIDE_ANGLE = 20.0f;

    public static Vector3f handRot() {
        return HAND_ROT;
    }

    public static void renderHand(FirstPersonHandsAndItemsRenderer renderer, PlayerRenderState playerState,
                                  FirstPersonHandsAndItemsRenderState handState, ItemStack stack,
                                  PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Minecraft mc = Minecraft.getInstance();
        boolean rightHand = armSide(playerState);

        // Vivecraft's arm type swaps in its own projection uniform (never allocated on desktop)
        GpuBufferSlice projection = RenderSystem.getProjectionMatrixBuffer();
        if (projection != null) {
            VRShaders.UNDISTORTED_PROJ_BUFFER = projection;
        }

        poseStack.pushPose();

        Vector3f dir = new Vector3f(0, 0, -1)
            .rotateY(Mth.DEG_TO_RAD * (rightHand ? -SIDE_ANGLE : SIDE_ANGLE));
        dir.y = 0;
        dir.normalize();
        poseStack.translate(dir.x * FWD_OFFSET, -DOWN_OFFSET, dir.z * FWD_OFFSET);
        poseStack.rotate(new Quaternionf().rotationXYZ(
            Mth.DEG_TO_RAD * (GRIP_PITCH - BEND_G + HAND_ROT.x),
            Mth.DEG_TO_RAD * ((rightHand ? STANCE_YAW : -STANCE_YAW) + HAND_ROT.y),
            Mth.DEG_TO_RAD * HAND_ROT.z));

        renderArm(mc, playerState, rightHand, poseStack, collector, light);

        if (!stack.isEmpty()) {
            poseStack.pushPose();
            VivecraftItemTransformType type = VivecraftItemRendering.getTransformType(stack, mc.player);
            if (type == VivecraftItemTransformType.BOW_ROOMSCALE
                || type == VivecraftItemTransformType.BOW_ROOMSCALE_DRAWING) {
                type = VivecraftItemTransformType.BOW_SEATED;
            }
            VRPreviewState.setGunAngleOverride(BEND_G);
            try {
                VivecraftItemRendering.applyFirstPersonItemTransforms(
                    poseStack, type, true, playerState, 1.0f, 0.0f, stack, InteractionHand.MAIN_HAND);
            } finally {
                VRPreviewState.setGunAngleOverride(null);
            }
            // The held item comes from the pre-extracted hand state now, the gizmo follows it manually
            handState.mainHandRenderState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            GizmoRenderer.renderHandGizmo(stack, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                poseStack, collector);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static boolean armSide(PlayerRenderState playerState) {
        boolean right = playerState.avatarRenderState == null
            || playerState.avatarRenderState.mainArm == HumanoidArm.RIGHT;
        try {
            if (ClientDataHolderVR.getInstance().vrSettings.reverseHands) {
                right = !right;
            }
        } catch (Throwable ignored) {
        }
        return right;
    }

    private static void renderArm(Minecraft mc, PlayerRenderState playerState, boolean rightHand,
                                  PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (playerState.avatarRenderState == null) {
            return;
        }
        VRArmRenderer arm = ((EntityRenderDispatcherVRExtension) mc.getEntityRenderDispatcher())
            .vivecraft$getArmSkinMap().get(playerState.avatarRenderState.skin.model());
        if (arm == null) {
            return;
        }
        boolean slim = playerState.avatarRenderState.skin.model() == PlayerModelType.SLIM;
        float side = rightHand ? -1.0f : 1.0f;
        poseStack.pushPose();
        poseStack.scale(0.4f, 0.4f, 0.4f);
        poseStack.translate(side * (slim ? 0.34375f : 0.375f), 0.0f, 0.75f);
        poseStack.rotate(Axis.XP.rotationDegrees(-90));
        poseStack.rotate(Axis.YP.rotationDegrees(180));
        arm.armAlpha = 1.0f;
        Identifier skin = playerState.avatarRenderState.skin.body().texturePath();
        if (rightHand) {
            arm.renderRightHand(poseStack, collector, light, skin, true);
        } else {
            arm.renderLeftHand(poseStack, collector, light, skin, true);
        }
        poseStack.popPose();
    }
}
