package win.demistorm.mcpositioneditor.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import win.demistorm.mcpositioneditor.ConfigHelper;
import win.demistorm.mcpositioneditor.mixin.GameRendererAccessor;
import win.demistorm.mcpositioneditor.client.VRAbstraction;
import win.demistorm.mcpositioneditor.client.VRDebug;

// Draws the gizmo where the edited item renders
public final class GizmoRenderer {

    private GizmoRenderer() {}

    public static final float AXIS_LENGTH = 1.0f;
    public static final float RING_RADIUS = 0.6f;
    private static final int RING_SEGMENTS = 40;

    private static final int[] AXIS_COLORS = {
        ARGB.colorFromFloat(1.0f, 0.95f, 0.25f, 0.25f),
        ARGB.colorFromFloat(1.0f, 0.30f, 0.95f, 0.30f),
        ARGB.colorFromFloat(1.0f, 0.35f, 0.55f, 1.0f)
    };
    private static final int HOVER_COLOR = ARGB.colorFromFloat(1.0f, 1.0f, 1.0f, 1.0f);

    private static final float GHOST_ALPHA = 0.3f;

    private static final Matrix4f LAST_GIZMO_MATRIX = new Matrix4f();
    private static final Matrix4f LAST_MV = new Matrix4f();
    private static boolean hasGizmo;
    private static int hoveredAxis = -1;

    private static final float[] axisLocalLen = {AXIS_LENGTH, AXIS_LENGTH, AXIS_LENGTH};

    public static final int GRAB_TARGET = 3;
    private static final float GRAB_CUBE_SHRINK = 0.85f;
    private static final float GRAB_LOCAL_SIZE_DEFAULT = 0.35f;
    private static float grabLocalSize = GRAB_LOCAL_SIZE_DEFAULT;

    private static void updateAxisLengthsVR() {
        boolean worldScaleRig = VRAbstraction.isActive() || EditorController.isVrPreviewActive();
        if (!worldScaleRig) {
            axisLocalLen[0] = axisLocalLen[1] = axisLocalLen[2] = AXIS_LENGTH;
            grabLocalSize = GRAB_LOCAL_SIZE_DEFAULT;
            return;
        }
        float target = 0.32f * (VRAbstraction.isActive() ? VRAbstraction.worldScale() : 1.0f);
        Matrix4f m = LAST_GIZMO_MATRIX;
        float colNormSum = 0;
        int sane = 0;
        for (int axis = 0; axis < 3; axis++) {
            float cx = m.get(axis, 0), cy = m.get(axis, 1), cz = m.get(axis, 2);
            float colNorm = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (colNorm < 1.0E-4F) {
                continue;
            }
            colNormSum += colNorm;
            sane++;
            axisLocalLen[axis] = Math.min(AXIS_LENGTH * 100.0f, target / colNorm);
        }
        if (sane > 0) {
            float colNorm = colNormSum / sane;
            float cap = 0.15f * Math.min(axisLocalLen[0], Math.min(axisLocalLen[1], axisLocalLen[2]));
            grabLocalSize = GRAB_CUBE_SHRINK * Math.min(cap, Math.max(0.05f, 0.05f * VRAbstraction.worldScale() / colNorm));
        }
    }

    public static float axisLocalLength(int axis) {
        return axisLocalLen[axis];
    }

    private static final Matrix4f GHOST_POSE = new Matrix4f();
    private static boolean ghostPending;

    private static final java.util.Map<net.minecraft.resources.Identifier, Vector3f> BOUNDS_CENTER =
        new java.util.HashMap<>();

    public static void noteModelQuads(net.minecraft.resources.Identifier modelId,
                                       java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads) {
        if (modelId == null || quads == null || quads.isEmpty() || BOUNDS_CENTER.containsKey(modelId)) {
            return;
        }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (net.minecraft.client.renderer.block.model.BakedQuad q : quads) {
            for (int i = 0; i < net.minecraft.client.renderer.block.model.BakedQuad.VERTEX_COUNT; i++) {
                org.joml.Vector3fc p = q.position(i);
                minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
                minY = Math.min(minY, p.y()); maxY = Math.max(maxY, p.y());
                minZ = Math.min(minZ, p.z()); maxZ = Math.max(maxZ, p.z());
            }
        }
        BOUNDS_CENTER.put(modelId, new Vector3f((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f));
    }

    private static Vector3f anchorLocal() {
        if (ConfigHelper.ACTIVE.boundsCenteredGizmos) {
            net.minecraft.resources.Identifier model = EditorController.selectedModel();
            Vector3f c = model == null ? null : BOUNDS_CENTER.get(model);
            if (c != null) {
                return c;
            }
        }
        return new Vector3f(0.5f, 0.5f, 0.5f);
    }

    public static void drawPendingGhost() {
        if (!ghostPending) {
            return;
        }
        ghostPending = false;
        com.mojang.blaze3d.vertex.BufferBuilder builder = com.mojang.blaze3d.vertex.Tesselator.getInstance()
            .begin(GizmoGhostType.GHOST_LINES_TYPE.mode(), GizmoGhostType.GHOST_LINES_TYPE.format());
        if (builder == null) {
            return;
        }
        PoseStack.Pose pose = new PoseStack.Pose();
        pose.pose().set(GHOST_POSE);
        pose.normal().set(new org.joml.Matrix3f(GHOST_POSE));
        if (isRotateMode()) {
            drawRings(builder, pose, GHOST_ALPHA);
        } else {
            drawAxes(builder, pose, GHOST_ALPHA);
        }
        if (VRAbstraction.isActive()) {
            drawGrabCube(builder, pose, GHOST_ALPHA);
        }
        com.mojang.blaze3d.vertex.MeshData mesh = builder.build();
        if (mesh != null) {
            GizmoGhostType.GHOST_LINES_TYPE.draw(mesh);
            mesh.close();
        }
    }

    public static void setHoveredAxis(int axis) {
        hoveredAxis = axis;
    }

    public static int hoveredAxis() {
        return hoveredAxis;
    }

    public static boolean isRotateMode() {
        return EditorController.mode() == EditorController.Mode.ROTATE;
    }

    public static boolean isSnapEnabled() {
        return EditorController.isSnapEnabled();
    }

    public static float snapStepDegrees() {
        return EditorController.SNAP_STEP_DEGREES;
    }

    public static void renderHandGizmo(ItemStack stack, ItemDisplayContext renderCtx, PoseStack poseStack) {
        if (!EditorController.isSessionOpen()) {
            hasGizmo = false;
            return;
        }
        if (!ItemStack.matches(stack, EditorController.sessionStack())) {
            return;
        }
        if (renderCtx != EditorController.context()) {
            if (VRAbstraction.isActive()) {
                EditorController.followRenderCtx(renderCtx);
            }
            if (renderCtx != EditorController.context()) {
                if (VRDebug.gate("gizmo-skip")) {
                    VRDebug.log("gizmo-skip",
                        "renderCtx=", renderCtx, "sessionCtx=", EditorController.context());
                }
                return;
            }
        }

        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.renderer.block.model.ItemTransform working = EditorController.working();

        poseStack.pushPose();
        working.apply(renderCtx.leftHand(), poseStack.last());
        Vector3f anchor = anchorLocal();
        poseStack.translate(anchor.x, anchor.y, anchor.z);

        LAST_GIZMO_MATRIX.set(poseStack.last().pose());
        LAST_MV.set(RenderSystem.getModelViewMatrix());
        hasGizmo = true;
        updateAxisLengthsVR();

        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderTypes.lines());
        if (isRotateMode()) {
            drawRings(consumer, poseStack.last(), 1.0f);
        } else {
            drawAxes(consumer, poseStack.last(), 1.0f);
        }
        if (VRAbstraction.isActive()) {
            drawGrabCube(consumer, poseStack.last(), 1.0f);
        }
        if (VRAbstraction.isActive()) {
            if (VRDebug.gate("gizmo")) {
                Vector3f origin = worldMatrix().transformPosition(new Vector3f(0, 0, 0));
                VRDebug.log("gizmo",
                    "world=", String.format("%.2f/%.2f/%.2f", origin.x(), origin.y(), origin.z()),
                    "rotate=", isRotateMode(), "hovered=", hoveredAxis);
            }
        }

        mc.renderBuffers().bufferSource().endLastBatch();

        GHOST_POSE.set(poseStack.last().pose());
        ghostPending = true;

        poseStack.popPose();

        VRAbstraction.pollVrGizmo(VR_SINK);
    }

    private static void drawAxes(VertexConsumer consumer, PoseStack.Pose pose, float alphaMul) {
        for (int axis = 0; axis < 3; axis++) {
            Vector3f dir = axisVector(axis);
            Vector3f end = new Vector3f(dir).mul(axisLocalLen[axis]);
            int color = dim(axis == hoveredAxis ? HOVER_COLOR : AXIS_COLORS[axis], alphaMul);
            line(consumer, pose, new Vector3f(0, 0, 0), end, color);
            Vector3f side = new Vector3f(0, 1, 0);
            if (Math.abs(dir.y()) > 0.9f) {
                side.set(1, 0, 0);
            }
            Vector3f tip1 = new Vector3f(end).sub(new Vector3f(side).mul(0.1f)).sub(new Vector3f(dir).mul(0.12f));
            Vector3f tip2 = new Vector3f(end).add(new Vector3f(side).mul(0.1f)).sub(new Vector3f(dir).mul(0.12f));
            line(consumer, pose, tip1, end, color);
            line(consumer, pose, tip2, end, color);
        }
    }

    private static void drawRings(VertexConsumer consumer, PoseStack.Pose pose, float alphaMul) {
        for (int axis = 0; axis < 3; axis++) {
            int color = dim(axis == hoveredAxis ? HOVER_COLOR : AXIS_COLORS[axis], alphaMul);
            Vector3f a = axisVector(axis);
            Vector3f u = new Vector3f(0, 1, 0);
            if (Math.abs(a.y()) > 0.9f) {
                u.set(1, 0, 0);
            }
            Vector3f v = new Vector3f(a).cross(u).normalize().mul(RING_RADIUS);
            u = new Vector3f(a).cross(v).normalize().mul(RING_RADIUS);
            Vector3f prev = new Vector3f(v);
            for (int s = 1; s <= RING_SEGMENTS; s++) {
                double ang = 2.0 * Math.PI * s / RING_SEGMENTS;
                Vector3f next = new Vector3f(v).mul((float) Math.cos(ang)).add(new Vector3f(u).mul((float) Math.sin(ang)));
                line(consumer, pose, prev, next, color);
                prev = next;
            }
        }
    }

    private static void drawGrabCube(VertexConsumer consumer, PoseStack.Pose pose, float alphaMul) {
        float s = grabLocalSize;
        for (int axis = 0; axis < 3; axis++) {
            int color = dim(hoveredAxis == GRAB_TARGET ? HOVER_COLOR : AXIS_COLORS[axis], alphaMul);
            for (int u = 0; u < 2; u++) {
                for (int v = 0; v < 2; v++) {
                    float cu = (u * 2 - 1) * s;
                    float cv = (v * 2 - 1) * s;
                    Vector3f from = new Vector3f();
                    Vector3f to = new Vector3f();
                    switch (axis) {
                        case 0 -> { from.set(-s, cu, cv); to.set(s, cu, cv); }
                        case 1 -> { from.set(cu, -s, cv); to.set(cu, s, cv); }
                        default -> { from.set(cu, cv, -s); to.set(cu, cv, s); }
                    }
                    line(consumer, pose, from, to, color);
                }
            }
        }
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose, Vector3f from, Vector3f to, int color) {
        Vector3f normal = new Vector3f(to).sub(from).normalize();
        consumer.addVertex(pose, from.x(), from.y(), from.z()).setColor(color).setNormal(pose, normal).setLineWidth(3.0f);
        consumer.addVertex(pose, to.x(), to.y(), to.z()).setColor(color).setNormal(pose, normal).setLineWidth(3.0f);
    }

    private static int dim(int color, float alphaMul) {
        if (alphaMul >= 1.0f) {
            return color;
        }
        int a = Math.round(((color >>> 24) & 0xFF) * alphaMul);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static Vector3f axisVector(int axis) {
        return switch (axis) {
            case 0 -> new Vector3f(1, 0, 0);
            case 1 -> new Vector3f(0, 1, 0);
            default -> new Vector3f(0, 0, 1);
        };
    }

    public static Vector3f[] axisEndpointsLocal(int axis) {
        return axisEndpointsLocal(axis, 0.0f);
    }

    public static Vector3f[] axisEndpointsLocal(int axis, float startFraction) {
        return new Vector3f[]{
            axisVector(axis).mul(axisLocalLen[axis] * startFraction),
            axisVector(axis).mul(axisLocalLen[axis])};
    }

    public static float ringRadiusLocal() {
        return RING_RADIUS;
    }

    public static double[] getAxisWorldSegment(int axis) {
        return getAxisWorldSegment(axis, 0.0f);
    }

    public static double[] getAxisWorldSegment(int axis, float startFraction) {
        return worldSegment(axisEndpointsLocal(axis, startFraction));
    }

    public static double[] getRingWorldPlane(int axis) {
        if (!hasGizmo) {
            return null;
        }
        Matrix4f world = worldMatrix();
        Vector3f center = world.transformPosition(new Vector3f(0, 0, 0));
        Vector3f normal = world.transformDirection(axisVector(axis));
        return new double[]{center.x(), center.y(), center.z(), normal.x(), normal.y(), normal.z()};
    }

    public static double[] getGrabWorld() {
        if (!hasGizmo || !VRAbstraction.isActive()) {
            return null;
        }
        Matrix4f world = worldMatrix();
        Vector3f center = world.transformPosition(new Vector3f(0, 0, 0));
        float r = 0;
        for (int axis = 0; axis < 3; axis++) {
            Vector3f p = world.transformPosition(new Vector3f(axisVector(axis)).mul(grabLocalSize));
            r += p.distance(center);
        }
        return new double[]{center.x(), center.y(), center.z(), r / 3.0f};
    }

    public static double[][] getRingWorldSamples(int axis) {
        if (!hasGizmo) {
            return null;
        }
        Matrix4f world = worldMatrix();
        Vector3f a = axisVector(axis);
        Vector3f u = new Vector3f(0, 1, 0);
        if (Math.abs(a.y()) > 0.9f) {
            u.set(1, 0, 0);
        }
        Vector3f v = new Vector3f(a).cross(u).normalize().mul(RING_RADIUS);
        u = new Vector3f(a).cross(v).normalize().mul(RING_RADIUS);
        double[][] pts = new double[RING_SEGMENTS][];
        for (int s = 0; s < RING_SEGMENTS; s++) {
            double ang = 2.0 * Math.PI * s / RING_SEGMENTS;
            Vector3f p = new Vector3f(v).mul((float) Math.cos(ang)).add(new Vector3f(u).mul((float) Math.sin(ang)));
            Vector3f w = world.transformPosition(p);
            pts[s] = new double[]{w.x(), w.y(), w.z()};
        }
        return pts;
    }

    private static double[] worldSegment(Vector3f[] localPts) {
        if (!hasGizmo) {
            return null;
        }
        Matrix4f world = worldMatrix();
        Vector3f a = world.transformPosition(new Vector3f(localPts[0]));
        Vector3f b = world.transformPosition(new Vector3f(localPts[1]));
        return new double[]{a.x(), a.y(), a.z(), b.x(), b.y(), b.z()};
    }

    private static Matrix4f worldMatrix() {
        if (VRAbstraction.isActive()) {
            double[] eye = VRAbstraction.eyeRenderPosition();
            Matrix4f world = new Matrix4f();
            if (eye != null) {
                world.translate(new Vector3f((float) eye[0], (float) eye[1], (float) eye[2]));
            }
            return world.mul(LAST_GIZMO_MATRIX);
        }
        Matrix4f world = new Matrix4f();
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.Camera cam = mc.gameRenderer.getMainCamera();
        world.translate(new Vector3f(cam.position().toVector3f()));
        world.rotate(new org.joml.Quaternionf(cam.rotation()));
        world.mul(LAST_GIZMO_MATRIX);
        return world;
    }

    public static double[] projectToScreen(Vector3f localPoint) {
        if (!hasGizmo) {
            return null;
        }
        double[] dims = screenDims();
        if (dims == null) {
            return null;
        }
        org.joml.Vector4f clip = gizmoClipSpace(localPoint, dims[2]);
        if (clip.w <= CLIP_W_EPSILON) {
            return null;
        }
        return clipToWindow(clip, dims[0], dims[1]);
    }

    public static double[] projectSegmentToScreen(Vector3f aLocal, Vector3f bLocal) {
        if (!hasGizmo) {
            return null;
        }
        double[] dims = screenDims();
        if (dims == null) {
            return null;
        }
        org.joml.Vector4f a = gizmoClipSpace(aLocal, dims[2]);
        org.joml.Vector4f b = gizmoClipSpace(bLocal, dims[2]);
        boolean aBehind = a.w <= CLIP_W_EPSILON;
        boolean bBehind = b.w <= CLIP_W_EPSILON;
        if (aBehind && bBehind) {
            return null;
        }
        if (aBehind || bBehind) {
            org.joml.Vector4f behind = aBehind ? a : b;
            org.joml.Vector4f front = aBehind ? b : a;
            float t = (CLIP_W_EPSILON - behind.w) / (front.w - behind.w);
            behind.set(
                behind.x + (front.x - behind.x) * t,
                behind.y + (front.y - behind.y) * t,
                behind.z + (front.z - behind.z) * t,
                behind.w + (front.w - behind.w) * t);
        }
        double[] pa = clipToWindow(a, dims[0], dims[1]);
        double[] pb = clipToWindow(b, dims[0], dims[1]);
        return new double[]{pa[0], pa[1], pb[0], pb[1]};
    }

    private static final float CLIP_W_EPSILON = 1.0E-4f;

    private static double[] screenDims() {
        Minecraft mc = Minecraft.getInstance();
        double width = mc.getWindow().getScreenWidth();
        double height = mc.getWindow().getScreenHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new double[]{width, height, width / height};
    }

    private static org.joml.Vector4f gizmoClipSpace(Vector3f localPoint, double aspect) {
        Minecraft mc = Minecraft.getInstance();
        GameRenderer gr = mc.gameRenderer;
        float partial = gr.getMainCamera().getPartialTickTime();
        float fov = ((GameRendererAccessor) gr).mcpositioneditor$getFov(gr.getMainCamera(), partial, false);
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(fov), (float) aspect, 0.05f, 100.0f);
        Vector3f local = LAST_GIZMO_MATRIX.transformPosition(new Vector3f(localPoint));
        return proj.transform(LAST_MV.transform(
            new org.joml.Vector4f(local.x(), local.y(), local.z(), 1.0f)));
    }

    private static double[] clipToWindow(org.joml.Vector4f clip, double width, double height) {
        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        return new double[]{(ndcX * 0.5 + 0.5) * width, (0.5 - ndcY * 0.5) * height};
    }

    public static boolean hasGizmo() {
        return hasGizmo;
    }

    private static final VRAbstraction.GizmoVrSink VR_SINK = new VRAbstraction.GizmoVrSink() {
        @Override
        public void vrHover(int axisIndex, double distance) {
            setHoveredAxis(axisIndex);
        }

        @Override
        public void vrDragAxis(int axisIndex, double worldDelta) {
            double[] seg = getAxisWorldSegment(axisIndex);
            double worldLen = 0.01;
            if (seg != null) {
                worldLen = Math.sqrt(Math.pow(seg[3] - seg[0], 2) + Math.pow(seg[4] - seg[1], 2) + Math.pow(seg[5] - seg[2], 2));
            }
            if (worldLen < 1.0E-4) {
                return;
            }
            float modelUnits = (float) (worldDelta * axisLocalLength(axisIndex) / worldLen);
            EditorController.translateAxis(axisIndex, modelUnits);
        }

        @Override
        public void vrDragRotate(int axisIndex, double worldAngleDelta) {
            EditorController.rotateAxis(axisIndex, (float) Math.toDegrees(worldAngleDelta));
        }

        @Override
        public void vrDragMove(double dx, double dy, double dz) {
            Vector3f local = new Vector3f();
            for (int axis = 0; axis < 3; axis++) {
                double[] seg = getAxisWorldSegment(axis);
                if (seg == null) {
                    return;
                }
                double ax = seg[3] - seg[0], ay = seg[4] - seg[1], az = seg[5] - seg[2];
                double len = Math.sqrt(ax * ax + ay * ay + az * az);
                if (len < 1.0E-6) {
                    return;
                }
                double along = (dx * ax + dy * ay + dz * az) / len;
                float modelUnits = (float) (along * axisLocalLength(axis) / len);
                switch (axis) {
                    case 0 -> local.x = modelUnits;
                    case 1 -> local.y = modelUnits;
                    default -> local.z = modelUnits;
                }
            }
            EditorController.translateLocal(local.x, local.y, local.z);
        }
    };

    public static void reset() {
        hasGizmo = false;
        hoveredAxis = -1;
    }

}
