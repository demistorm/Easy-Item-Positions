package win.demistorm.mcpositioneditor.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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

// Renders the Vivecraft arm and item at a synthetic controller pose (VR context option)
public final class VRPreviewRenderer {

    private VRPreviewRenderer() {}

    public static final Vector3f HAND_ROT = new Vector3f(
        ConfigHelper.ACTIVE.previewHandRot.length == 3 ? ConfigHelper.ACTIVE.previewHandRot[0] : 0.0f,
        ConfigHelper.ACTIVE.previewHandRot.length == 3 ? ConfigHelper.ACTIVE.previewHandRot[1] : 0.0f,
        ConfigHelper.ACTIVE.previewHandRot.length == 3 ? ConfigHelper.ACTIVE.previewHandRot[2] : 0.0f);

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

    public static void renderHand(ItemInHandRenderer renderer, AbstractClientPlayer player,
                                  ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
                                  int light) {
        Minecraft mc = Minecraft.getInstance();
        boolean rightHand = armSide(player);

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
        poseStack.mulPose(new Quaternionf().rotationXYZ(
            Mth.DEG_TO_RAD * (GRIP_PITCH - BEND_G + HAND_ROT.x),
            Mth.DEG_TO_RAD * ((rightHand ? STANCE_YAW : -STANCE_YAW) + HAND_ROT.y),
            Mth.DEG_TO_RAD * HAND_ROT.z));

        renderArm(mc, player, rightHand, poseStack, collector, light);

        if (!stack.isEmpty()) {
            poseStack.pushPose();
            VivecraftItemTransformType type = VivecraftItemRendering.getTransformType(stack, player);
            if (type == VivecraftItemTransformType.BOW_ROOMSCALE
                || type == VivecraftItemTransformType.BOW_ROOMSCALE_DRAWING) {
                type = VivecraftItemTransformType.BOW_SEATED;
            }
            VRPreviewState.setGunAngleOverride(BEND_G);
            try {
                VivecraftItemRendering.applyFirstPersonItemTransforms(
                    poseStack, type, true, player, 1.0f, 0.0f, stack, InteractionHand.MAIN_HAND);
            } finally {
                VRPreviewState.setGunAngleOverride(null);
            }
            // Vivecraft renders first-person items with the right-hand context for both hands (as of 1.3.15)
            renderer.renderItem(player, stack, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                poseStack, collector, light);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static boolean armSide(AbstractClientPlayer player) {
        boolean right = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        try {
            if (ClientDataHolderVR.getInstance().vrSettings.reverseHands) {
                right = !right;
            }
        } catch (Throwable ignored) {
        }
        return right;
    }

    private static void renderArm(Minecraft mc, AbstractClientPlayer player, boolean rightHand,
                                  PoseStack poseStack, SubmitNodeCollector collector, int light) {
        VRArmRenderer arm = ((EntityRenderDispatcherVRExtension) mc.getEntityRenderDispatcher())
            .vivecraft$getArmSkinMap().get(player.getSkin().model());
        if (arm == null) {
            return;
        }
        boolean slim = player.getSkin().model() == PlayerModelType.SLIM;
        float side = rightHand ? -1.0f : 1.0f;
        poseStack.pushPose();
        poseStack.scale(0.4f, 0.4f, 0.4f);
        poseStack.translate(side * (slim ? 0.34375f : 0.375f), 0.0f, 0.75f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        arm.armAlpha = 1.0f;
        Identifier skin = player.getSkin().body().texturePath();
        if (rightHand) {
            arm.renderRightHand(poseStack, collector, light, skin, true);
        } else {
            arm.renderLeftHand(poseStack, collector, light, skin, true);
        }
        poseStack.popPose();
    }
}
