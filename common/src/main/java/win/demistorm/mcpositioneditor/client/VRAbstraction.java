package win.demistorm.mcpositioneditor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.vivecraft.api.client.VRClientAPI;
import org.vivecraft.api.data.VRBodyPart;
import org.vivecraft.api.data.VRPose;
import org.vivecraft.client.network.ClientNetworking;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.gameplay.trackers.BowTracker;
import org.vivecraft.client_vr.render.VivecraftItemRendering;
import org.vivecraft.client_vr.render.VivecraftItemRendering.VivecraftItemTransformType;
import org.vivecraft.data.ViveItems;
import win.demistorm.mcpositioneditor.editor.GizmoRenderer;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

// VR abstraction so adding Visor support later is hopefully simple
public final class VRAbstraction {

    private VRAbstraction() {}

    public static boolean isAvailable() {
        return VivecraftGate.isVivecraftPresent();
    }

    public static boolean isActive() {
        return VivecraftGate.isVivecraftPresent() && Impl.isActive();
    }

    public static void init() {
        if (VivecraftGate.isVivecraftPresent()) {
            Impl.init();
        }
    }

    public static ItemDisplayContext effectiveHeldContext(boolean mainArmRight) {
        if (!VivecraftGate.isVivecraftPresent()) {
            return mainArmRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }
        try {
            return Impl.effectiveHeldContext(mainArmRight);
        } catch (Throwable t) {
            log.debug("VR effective context detection failed, falling back to firstperson", t);
            return mainArmRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }
    }

    public static void pollVrGizmo(GizmoVrSink sink) {
        if (isActive()) {
            try {
                Impl.pollGizmo(sink);
            } catch (Throwable t) {
                log.debug("VR gizmo poll failed", t);
            }
        }
    }

    public static void resetVrInteraction() {
        if (VivecraftGate.isVivecraftPresent()) {
            try {
                Impl.resetInteraction();
            } catch (Throwable t) {
                log.debug("VR interaction reset failed", t);
            }
        }
    }

    public static double[] eyeRenderPosition() {
        return isActive() ? Impl.eyeRenderPosition() : null;
    }

    public static org.joml.Matrix4f eyeViewRotation() {
        return isActive() ? Impl.eyeViewRotation() : null;
    }

    public static void hapticGrab() {
        if (VivecraftGate.isVivecraftPresent()) {
            try {
                Impl.hapticGrab();
            } catch (Throwable ignored) {
            }
        }
    }

    public static void hapticRelease() {
        if (VivecraftGate.isVivecraftPresent()) {
            try {
                Impl.hapticRelease();
            } catch (Throwable ignored) {
            }
        }
    }

    public static void hapticTick() {
        if (VivecraftGate.isVivecraftPresent()) {
            try {
                Impl.hapticTick();
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean previewBroken;

    public static boolean isPreviewUsable() {
        return VivecraftGate.isVivecraftPresent() && !previewBroken;
    }

    public static void resetPreviewLatch() {
        previewBroken = false;
    }

    public static org.joml.Vector3f previewHandRot() {
        return VRPreviewRenderer.handRot();
    }

    public static void renderPreviewHand(net.minecraft.client.renderer.ItemInHandRenderer renderer,
                                         net.minecraft.client.player.AbstractClientPlayer player,
                                         net.minecraft.world.item.ItemStack stack,
                                         com.mojang.blaze3d.vertex.PoseStack poseStack,
                                         net.minecraft.client.renderer.SubmitNodeCollector collector,
                                         int light) {
        try {
            VRPreviewRenderer.renderHand(renderer, player, stack, poseStack, collector, light);
        } catch (Throwable t) {
            previewBroken = true;
            log.error("VR preview hand rendering failed, disabling for this session", t);
        }
    }

    public static void setEditorScreenActive(boolean active) {
        if (VivecraftGate.isVivecraftPresent()) {
            Impl.setEditorScreenActive(active);
        }
    }

    public interface GizmoVrSink {
        void vrHover(int axisIndex, double distance);

        void vrDragAxis(int axisIndex, double worldDelta);

        void vrDragRotate(int axisIndex, double worldAngleDelta);

        void vrDragMove(double dx, double dy, double dz);
    }

    public static boolean isSeated() {
        return VivecraftGate.isVivecraftPresent() && Impl.isSeated();
    }

    public static float worldScale() {
        return VivecraftGate.isVivecraftPresent() ? Impl.worldScale() : 1.0f;
    }

    private static double pointPolylineDistance(double px, double py, double pz, double[][] pts) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < pts.length; i++) {
            double[] a = pts[i];
            double[] b = pts[(i + 1) % pts.length];
            best = Math.min(best, pointSegmentDistance(px, py, pz, a[0], a[1], a[2], b[0], b[1], b[2]));
        }
        return best;
    }

    private static double rayPolylineDistance(double px, double py, double pz,
                                              double qx, double qy, double qz, double[][] pts) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < pts.length; i++) {
            double[] a = pts[i];
            double[] b = pts[(i + 1) % pts.length];
            best = Math.min(best, segSegDistance(px, py, pz, qx, qy, qz, a[0], a[1], a[2], b[0], b[1], b[2]));
        }
        return best;
    }

    private static double segSegDistance(double p1x, double p1y, double p1z,
                                         double q1x, double q1y, double q1z,
                                         double p2x, double p2y, double p2z,
                                         double q2x, double q2y, double q2z) {
        double d1x = q1x - p1x, d1y = q1y - p1y, d1z = q1z - p1z;
        double d2x = q2x - p2x, d2y = q2y - p2y, d2z = q2z - p2z;
        double rx = p1x - p2x, ry = p1y - p2y, rz = p1z - p2z;
        double a = d1x * d1x + d1y * d1y + d1z * d1z;
        double e = d2x * d2x + d2y * d2y + d2z * d2z;
        double f = d2x * rx + d2y * ry + d2z * rz;
        final double eps = 1.0E-9;
        double s, t;
        if (a <= eps && e <= eps) {
            return Math.sqrt(rx * rx + ry * ry + rz * rz);
        }
        if (a <= eps) {
            s = 0;
            t = Math.clamp(f / e, 0, 1);
        } else {
            double c = d1x * rx + d1y * ry + d1z * rz;
            if (e <= eps) {
                t = 0;
                s = Math.clamp(-c / a, 0, 1);
            } else {
                double b = d1x * d2x + d1y * d2y + d1z * d2z;
                double denom = a * e - b * b;
                s = denom > eps ? Math.clamp((b * f - c * e) / denom, 0, 1) : 0;
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = Math.clamp(-c / a, 0, 1);
                } else if (t > 1) {
                    t = 1;
                    s = Math.clamp((b - c) / a, 0, 1);
                }
            }
        }
        double c1x = d1x * s + rx - d2x * t;
        double c1y = d1y * s + ry - d2y * t;
        double c1z = d1z * s + rz - d2z * t;
        return Math.sqrt(c1x * c1x + c1y * c1y + c1z * c1z);
    }

    static double pointSegmentDistance(double px, double py, double pz,
                                       double ax, double ay, double az,
                                       double bx, double by, double bz) {
        Vector3d ab = new Vector3d(bx - ax, by - ay, bz - az);
        Vector3d ap = new Vector3d(px - ax, py - ay, pz - az);
        double lenSq = ab.lengthSquared();
        double t = lenSq < 1.0E-9 ? 0 : Math.clamp(ap.dot(ab) / lenSq, 0, 1);
        Vector3d closest = ab.mul(t).add(ap);
        return closest.length();
    }

    private static double angleAroundAxis(double cx, double cy, double cz,
                                          double nx, double ny, double nz,
                                          double px, double py, double pz) {
        Vector3d v = new Vector3d(px - cx, py - cy, pz - cz);
        Vector3d n = new Vector3d(nx, ny, nz).normalize();
        Vector3d ref = new Vector3d(0, 1, 0);
        if (Math.abs(n.dot(ref)) > 0.9) {
            ref.set(1, 0, 0);
        }
        Vector3d side = new Vector3d(n).cross(ref).normalize();
        Vector3d up = new Vector3d(side).cross(n).normalize();
        double x = v.dot(side);
        double y = v.dot(up);
        return Math.atan2(y, x);
    }

    private static double wrapDegrees(double rad) {
        while (rad > Math.PI) {
            rad -= 2 * Math.PI;
        }
        while (rad < -Math.PI) {
            rad += 2 * Math.PI;
        }
        return rad;
    }

    private static final class Impl {

        private static boolean initialized;
        private static double lastHandX, lastHandY, lastHandZ;
        private static boolean haveLastHand;
        private static double snapAccum;
        private static boolean wasGrab;
        private static int grabbedAxis = -1;
        private static int stickyAxis = -1;
        private static boolean stickyRotateMode;

        static void init() {
            if (!initialized) {
                initialized = true;
            }
        }

        static boolean isActive() {
            try {
                return VRClientAPI.instance().isVRActive();
            } catch (Throwable t) {
                return false;
            }
        }

        static boolean isSeated() {
            return VRClientAPI.instance().isSeated();
        }

        static float worldScale() {
            float scale = VRClientAPI.instance().getWorldScale();
            return scale > 0.0f ? scale : 1.0f;
        }

        static ItemDisplayContext effectiveHeldContext(boolean mainArmRight) {
            Minecraft mc = Minecraft.getInstance();
            ItemStack main = mc.player != null ? mc.player.getMainHandItem() : ItemStack.EMPTY;
            VivecraftItemTransformType type = VivecraftItemRendering.getTransformType(main, mc.player);
            boolean custom = (main.has(DataComponents.CUSTOM_MODEL_DATA) || hasNonDefaultItemModel(main))
                && type != VivecraftItemTransformType.CROSSBOW
                && type != VivecraftItemTransformType.SPEAR
                && type != VivecraftItemTransformType.SHIELD;
            boolean isBow = BowTracker.isBow(main)
                && ClientDataHolderVR.getInstance().bowTracker.isActive((LocalPlayer) mc.player);
            boolean thirdPerson = ViveItems.isClimbingClaws(main)
                || (!isBow && (ClientNetworking.isThirdPersonItems()
                    || (custom && ClientNetworking.isThirdPersonItemsCustom())));
            if (thirdPerson) {
                if (ClientDataHolderVR.getInstance().vrSettings.reverseHands) {
                    mainArmRight = !mainArmRight;
                }
                return mainArmRight ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            }
            return mainArmRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }

        private static boolean hasNonDefaultItemModel(ItemStack stack) {
            return stack.getComponents() instanceof PatchedDataComponentMap patched
                && patched.hasNonDefault(DataComponents.ITEM_MODEL);
        }

        static void pollGizmo(GizmoVrSink sink) {
            VRPose pose;
            try {
                pose = ClientDataHolderVR.getInstance().vrPlayer.vrdata_world_render.asVRPose();
            } catch (Throwable t) {
                return;
            }
            if (pose == null) {
                return;
            }
            org.vivecraft.api.data.VRBodyPartData hand = pose.getHand(InteractionHand.OFF_HAND);
            if (hand == null) {
                return;
            }

            double gx = hand.getPos().x;
            double gy = hand.getPos().y;
            double gz = hand.getPos().z;
            double hx = gx, hy = gy, hz = gz;
            double rx = gx, ry = gy, rz = gz;
            boolean hasRay = false;
            net.minecraft.world.phys.Vec3 dir = hand.getDir();
            if (dir != null) {
                double tip = 0.03 * worldScale();
                hx += dir.x * tip;
                hy += dir.y * tip;
                hz += dir.z * tip;
                double reach = 1.5 * worldScale();
                rx = gx + dir.x * reach;
                ry = gy + dir.y * reach;
                rz = gz + dir.z * reach;
                hasRay = true;
            }
            boolean rotate = GizmoRenderer.isRotateMode();
            double threshold;
            double worldLen = 0;
            if (rotate) {
                threshold = 0.10 * worldScale();
            } else {
                double[] seg0 = GizmoRenderer.getAxisWorldSegment(0, 0.0f);
                if (seg0 != null) {
                    worldLen = Math.sqrt(Math.pow(seg0[3] - seg0[0], 2)
                        + Math.pow(seg0[4] - seg0[1], 2) + Math.pow(seg0[5] - seg0[2], 2));
                }
                double lo = 0.06 * worldScale();
                double hi = 0.15 * worldScale();
                threshold = Math.max(lo, Math.min(hi, worldLen * 0.35));
            }
            double grabFallbackRange = 0.25 * worldScale();
            int bestAxis = -1;
            double bestDist = Double.MAX_VALUE;
            double[] dists = new double[4];
            double[] rayDists = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                double dist;
                double rayDist = Double.MAX_VALUE;
                if (rotate) {
                    double[][] ring = GizmoRenderer.getRingWorldSamples(axis);
                    dist = ring == null ? Double.MAX_VALUE : pointPolylineDistance(hx, hy, hz, ring);
                    if (ring != null && hasRay) {
                        rayDist = rayPolylineDistance(gx, gy, gz, rx, ry, rz, ring);
                    }
                } else {
                    double[] seg = GizmoRenderer.getAxisWorldSegment(axis, 0.2f);
                    if (seg == null) {
                        dist = Double.MAX_VALUE;
                    } else {
                        dist = pointSegmentDistance(hx, hy, hz, seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
                        if (hasRay) {
                            rayDist = segSegDistance(gx, gy, gz, rx, ry, rz,
                                seg[0], seg[1], seg[2], seg[3], seg[4], seg[5]);
                        }
                    }
                }
                rayDists[axis] = rayDist;
                dist = Math.min(dist, rayDist);
                dists[axis] = dist;
                if (dist < bestDist) {
                    bestDist = dist;
                    bestAxis = axis;
                }
            }

            double grabThreshold = Double.MAX_VALUE;
            double[] cube = GizmoRenderer.getGrabWorld();
            dists[3] = Double.MAX_VALUE;
            boolean touchingGrab = false;
            if (cube != null) {
                double sd = Math.sqrt(Math.pow(hx - cube[0], 2) + Math.pow(hy - cube[1], 2)
                    + Math.pow(hz - cube[2], 2)) - cube[3];
                dists[3] = sd;
                grabThreshold = cube[3] * 0.75;
                touchingGrab = sd < grabThreshold;
                if (touchingGrab) {
                    bestAxis = GizmoRenderer.GRAB_TARGET;
                    bestDist = sd;
                }
            }

            if (stickyRotateMode != rotate) {
                stickyRotateMode = rotate;
                stickyAxis = -1;
            }
            if (touchingGrab) {
                stickyAxis = GizmoRenderer.GRAB_TARGET;
            } else if (stickyAxis >= 0) {
                double stick = stickyAxis == GizmoRenderer.GRAB_TARGET ? 1.0 : rotate ? 1.05 : 1.5;
                double thr = stickyAxis == GizmoRenderer.GRAB_TARGET ? grabThreshold : threshold;
                if (dists[stickyAxis] > thr * stick) {
                    stickyAxis = -1;
                }
            } else if (bestAxis >= 0 && bestDist < (bestAxis == GizmoRenderer.GRAB_TARGET ? grabThreshold : threshold)) {
                stickyAxis = bestAxis;
            }
            boolean hovering = stickyAxis >= 0;

            if (VRDebug.gate("hover") && grabbedAxis < 0) {
                VRDebug.log("hover",
                    "tip=", String.format("%.2f/%.2f/%.2f", hx, hy, hz),
                    "len=", String.format("%.2f", worldLen),
                    "dists=", String.format("%.2f/%.2f/%.2f", dists[0], dists[1], dists[2]),
                    "ray=", String.format("%.2f/%.2f/%.2f", rayDists[0], rayDists[1], rayDists[2]),
                    "thr=", String.format("%.2f", threshold),
                    "hover=", stickyAxis);
            }

            boolean grabHeld = grabKeyDown();
            if (grabHeld && !wasGrab) {
                int grabAxis = -1;
                String source = "hover";
                if (hovering) {
                    grabAxis = stickyAxis;
                } else if (bestAxis >= 0 && bestAxis != GizmoRenderer.GRAB_TARGET
                    && bestDist < grabFallbackRange) {
                    grabAxis = bestAxis;
                    source = "fallback";
                }
                if (grabAxis >= 0) {
                    grabbedAxis = grabAxis;
                    hapticGrab();
                    haveLastHand = false;
                    snapAccum = 0;
                    VRDebug.log("grab", "axis=", grabAxis, "src=", source,
                        "dist=", String.format("%.2f", dists[grabAxis]));
                } else if (VRDebug.gate("grab-miss")) {
                    VRDebug.log("grab-miss",
                        "dists=", String.format("%.2f/%.2f/%.2f", dists[0], dists[1], dists[2]),
                        "fallbackRange=", String.format("%.2f", grabFallbackRange));
                }
            } else if (!grabHeld && wasGrab) {
                if (grabbedAxis >= 0) {
                    hapticRelease();
                    VRDebug.log("grab-release", "axis=", grabbedAxis);
                }
                grabbedAxis = -1;
            }
            wasGrab = grabHeld;

            if (grabbedAxis >= 0) {
                sink.vrHover(grabbedAxis, 0);
            } else {
                sink.vrHover(stickyAxis, stickyAxis >= 0 ? dists[stickyAxis] : -1);
            }

            if (grabbedAxis >= 0) {
                if (!haveLastHand) {
                    lastHandX = hx;
                    lastHandY = hy;
                    lastHandZ = hz;
                    haveLastHand = true;
                }

                if (grabbedAxis == GizmoRenderer.GRAB_TARGET) {
                    double dx = hx - lastHandX, dy = hy - lastHandY, dz = hz - lastHandZ;
                    sink.vrDragMove(dx, dy, dz);
                } else if (GizmoRenderer.isRotateMode()) {
                    double[] ring = GizmoRenderer.getRingWorldPlane(grabbedAxis);
                    if (ring != null) {
                        double cx = ring[0], cy = ring[1], cz = ring[2];
                        double nx = ring[3], ny = ring[4], nz = ring[5];
                        double a1 = angleAroundAxis(cx, cy, cz, nx, ny, nz, lastHandX, lastHandY, lastHandZ);
                        double a2 = angleAroundAxis(cx, cy, cz, nx, ny, nz, hx, hy, hz);
                        double delta = -wrapDegrees(a2 - a1);
                        sink.vrDragRotate(grabbedAxis, delta);
                        if (GizmoRenderer.isSnapEnabled()) {
                            snapAccum += delta;
                            double step = Math.toRadians(GizmoRenderer.snapStepDegrees());
                            if (Math.abs(snapAccum) >= step) {
                                hapticTick();
                                snapAccum = 0;
                            }
                        }
                    }
                } else {
                    double[] seg = GizmoRenderer.getAxisWorldSegment(grabbedAxis);
                    if (seg != null) {
                        double dx = hx - lastHandX, dy = hy - lastHandY, dz = hz - lastHandZ;
                        double ax = seg[3] - seg[0], ay = seg[4] - seg[1], az = seg[5] - seg[2];
                        double len = Math.sqrt(ax * ax + ay * ay + az * az);
                        if (len > 1.0E-6) {
                            sink.vrDragAxis(grabbedAxis, (dx * ax + dy * ay + dz * az) / len);
                        }
                    }
                }

                lastHandX = hx;
                lastHandY = hy;
                lastHandZ = hz;
            }
        }

        static void resetInteraction() {
            grabbedAxis = -1;
            wasGrab = false;
            stickyAxis = -1;
            haveLastHand = false;
        }

        private static boolean grabKeyDown() {
            if (EditorKeys.isGrabDown()) {
                return true;
            }
            return offhandTriggerDown();
        }

        // Cannot inject SteamVR default bindings (vivecraft.key.teleport is the offhand trigger in stock bindings, hence why I poll it)
        private static Boolean offhandTriggerCache;

        private static boolean offhandTriggerDown() {
            if (EditorKeys.isGrabCustom()) {
                return false;
            }
            try {
                ClientDataHolderVR dh = ClientDataHolderVR.getInstance();
                org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction teleport =
                    dh.vr.getInputActionByName("ingame/in/vivecraft.key.teleport");
                if (teleport == null) {
                    teleport = dh.vr.getInputActionByName("/actions/ingame/in/vivecraft.key.teleport");
                }
                if (teleport != null && teleport.isEnabled() && teleport.isButtonPressed()) {
                    if (offhandTriggerCache == null) {
                        offhandTriggerCache = true;
                        VRDebug.log("grab-default", "using offhand trigger (teleport action)");
                    }
                    return true;
                }
                offhandTriggerCache = null;
                return false;
            } catch (Throwable t) {
                return false;
            }
        }

        // Vivecraft suspends the INGAME set while a screen is open
        private static Boolean savedIngameBindingsInGui;

        static void setEditorScreenActive(boolean active) {
            try {
                ClientDataHolderVR dh = ClientDataHolderVR.getInstance();
                if (dh == null || dh.vrSettings == null) {
                    return;
                }
                if (active) {
                    if (savedIngameBindingsInGui == null) {
                        savedIngameBindingsInGui = dh.vrSettings.ingameBindingsInGui;
                        dh.vrSettings.ingameBindingsInGui = true;
                        VRDebug.log("vrsession", "ingame bindings live for editor session (was ", savedIngameBindingsInGui + ")");
                    }
                } else if (savedIngameBindingsInGui != null) {
                    dh.vrSettings.ingameBindingsInGui = savedIngameBindingsInGui;
                    savedIngameBindingsInGui = null;
                    VRDebug.log("vrsession", "ingame bindings restored");
                }
            } catch (Throwable ignored) {
            }
        }

        static void hapticGrab() {
            VRClientAPI.instance().triggerHapticPulse(VRBodyPart.fromInteractionHand(InteractionHand.OFF_HAND), 0.2f);
        }

        static void hapticRelease() {
            VRClientAPI.instance().triggerHapticPulse(VRBodyPart.fromInteractionHand(InteractionHand.OFF_HAND), 0.5f);
        }

        static void hapticTick() {
            VRClientAPI.instance().triggerHapticPulse(VRBodyPart.fromInteractionHand(InteractionHand.OFF_HAND), 0.1f);
        }

        // Vivecraft subtracts this eye position when building the hand-item pose
        static double[] eyeRenderPosition() {
            try {
                ClientDataHolderVR dh = ClientDataHolderVR.getInstance();
                Vec3 eye = dh.vrPlayer.vrdata_world_render.getEye(dh.currentPass).getPosition();
                return new double[]{eye.x, eye.y, eye.z};
            } catch (Throwable t) {
                return null;
            }
        }

        static org.joml.Matrix4f eyeViewRotation() {
            try {
                ClientDataHolderVR dh = ClientDataHolderVR.getInstance();
                return new org.joml.Matrix4f(
                    org.vivecraft.client_vr.render.helpers.RenderHelper.getVRModelView(dh.currentPass));
            } catch (Throwable t) {
                return null;
            }
        }
    }
}
