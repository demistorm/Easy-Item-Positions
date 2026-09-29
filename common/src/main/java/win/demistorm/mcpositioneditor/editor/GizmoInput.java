package win.demistorm.mcpositioneditor.editor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import win.demistorm.mcpositioneditor.client.VRAbstraction;

// Flatscreen gizmo interaction
public final class GizmoInput {

    private GizmoInput() {}

    private static final double PICK_RADIUS_PX = 16.0;
    private static final float TRANSLATE_RATE = 1.2f;
    private static final float ROTATE_RATE = 90.0f;

    private static int grabbedAxis = -1;
    private static double lastX, lastY;
    private static double grabAngle;
    private static double[] grabCenter;
    private static boolean lastSpaceDown;
    private static long lastSpaceTap;

    public static boolean isDragging() {
        return grabbedAxis >= 0;
    }

    public static int pick(double winX, double winY) {
        if (!GizmoRenderer.hasGizmo() || !EditorController.isSessionOpen()) {
            return -1;
        }
        int best = -1;
        double bestDist = PICK_RADIUS_PX;
        if (GizmoRenderer.isRotateMode()) {
            for (int axis = 0; axis < 3; axis++) {
                double d = ringDistance(axis, winX, winY);
                if (d >= 0 && d < bestDist) {
                    bestDist = d;
                    best = axis;
                }
            }
        } else {
            for (int axis = 0; axis < 3; axis++) {
                double d = axisDistance(axis, winX, winY);
                if (d >= 0 && d < bestDist) {
                    bestDist = d;
                    best = axis;
                }
            }
        }
        return best;
    }

    private static double axisDistance(int axis, double winX, double winY) {
        org.joml.Vector3f[] seg = GizmoRenderer.axisEndpointsLocal(axis);
        double[] px = GizmoRenderer.projectSegmentToScreen(seg[0], seg[1]);
        if (px == null) {
            return -1;
        }
        return pointSegmentDistance(winX, winY, new Vector2(px[0], px[1]), new Vector2(px[2], px[3]));
    }

    private static double ringDistance(int axis, double winX, double winY) {
        org.joml.Vector3f normal = GizmoRenderer.axisVector(axis);
        org.joml.Vector3f u = new org.joml.Vector3f(0, 1, 0);
        if (Math.abs(normal.y()) > 0.9f) {
            u.set(1, 0, 0);
        }
        org.joml.Vector3f v = new org.joml.Vector3f(normal).cross(u).normalize().mul(GizmoRenderer.ringRadiusLocal());
        u = new org.joml.Vector3f(normal).cross(v).normalize().mul(GizmoRenderer.ringRadiusLocal());
        double best = -1;
        final int samples = 20;
        org.joml.Vector3f prev = new org.joml.Vector3f(v);
        for (int s = 1; s <= samples; s++) {
            double ang = 2.0 * Math.PI * s / samples;
            org.joml.Vector3f next = new org.joml.Vector3f(v).mul((float) Math.cos(ang))
                .add(new org.joml.Vector3f(u).mul((float) Math.sin(ang)));
            double[] seg = GizmoRenderer.projectSegmentToScreen(prev, next);
            if (seg != null) {
                double d = pointSegmentDistance(winX, winY, new Vector2(seg[0], seg[1]), new Vector2(seg[2], seg[3]));
                if (best < 0 || d < best) {
                    best = d;
                }
            }
            prev = next;
        }
        return best;
    }

    private static double pointSegmentDistance(double px, double py, Vector2 a, Vector2 b) {
        double abx = b.x - a.x, aby = b.y - a.y;
        double apx = px - a.x, apy = py - a.y;
        double lenSq = abx * abx + aby * aby;
        double t = lenSq < 1.0E-9 ? 0 : Math.clamp((apx * abx + apy * aby) / lenSq, 0, 1);
        double cx = a.x + abx * t, cy = a.y + aby * t;
        return Math.hypot(px - cx, py - cy);
    }

    public static void updateHover() {
        if (VRAbstraction.isActive()) {
            return;
        }
        if (isDragging()) {
            GizmoRenderer.setHoveredAxis(grabbedAxis);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();
        GizmoRenderer.setHoveredAxis(pick(x, y));
    }

    public static boolean mouseClicked() {
        if (!GizmoRenderer.hasGizmo() || VRAbstraction.isActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();
        int axis = pick(x, y);
        if (axis < 0) {
            return false;
        }
        grabbedAxis = axis;
        lastX = x;
        lastY = y;
        if (GizmoRenderer.isRotateMode()) {
            grabCenter = GizmoRenderer.projectToScreen(new org.joml.Vector3f(0, 0, 0));
            grabAngle = grabCenter == null ? 0 : Math.atan2(y - grabCenter[1], x - grabCenter[0]);
        }
        return true;
    }

    public static void mouseDragged() {
        if (grabbedAxis < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();
        int axis = grabbedAxis;

        if (GizmoRenderer.isRotateMode()) {
            if (grabCenter != null) {
                double ang = Math.atan2(y - grabCenter[1], x - grabCenter[0]);
                // IEEEremainder keeps the delta in -180..180
                double delta = Math.toDegrees(Math.IEEEremainder(ang - grabAngle, 2.0 * Math.PI));
                if (axis != 0) {
                    delta = -delta;
                }
                grabAngle = ang;
                EditorController.rotateAxis(axis, (float) delta);
            }
        } else {
            org.joml.Vector3f[] endPts = GizmoRenderer.axisEndpointsLocal(axis);
            double[] seg = GizmoRenderer.projectSegmentToScreen(endPts[0], endPts[1]);
            if (seg != null) {
                double abx = seg[2] - seg[0], aby = seg[3] - seg[1];
                double len = Math.hypot(abx, aby);
                if (len > 4.0) {
                    double dx = x - lastX, dy = y - lastY;
                    double along = (dx * abx + dy * aby) / len;
                    EditorController.translateAxis(axis, (float) (along / len * GizmoRenderer.AXIS_LENGTH));
                }
            }
        }
        lastX = x;
        lastY = y;
    }

    public static void mouseReleased() {
        if (grabbedAxis >= 0) {
            grabbedAxis = -1;
            grabCenter = null;
            Minecraft mc = Minecraft.getInstance();
            GizmoRenderer.setHoveredAxis(pick(mc.mouseHandler.xpos(), mc.mouseHandler.ypos()));
        }
    }

    // Keys only count as held after an observed release (doesn't go crazy when one is glitched down)
    private static final java.util.Set<Integer> seenUp = new java.util.HashSet<>();
    private static boolean wasWindowActive = true;

    public static void resetKeyTracking() {
        seenUp.clear();
        wasWindowActive = Minecraft.getInstance().isWindowActive();
    }

    private static boolean safeDown(int key) {
        boolean down = rawDown(key);
        if (!down) {
            seenUp.add(key);
            return false;
        }
        return seenUp.contains(key);
    }

    public static void tickKeyboard(double dtSeconds) {
        Minecraft mc = Minecraft.getInstance();
        boolean active = mc.isWindowActive();
        if (!active) {
            // When unfocused, SDL keeps serving the last key state (why seenUp is needed)
            seenUp.clear();
            wasWindowActive = false;
            return;
        }
        if (!wasWindowActive) {
            seenUp.clear();
            wasWindowActive = true;
        }
        if (!EditorController.isSessionOpen() || EditorScreen.hasFocusedEditBox()) {
            return;
        }
        boolean shift = safeDown(InputConstants.KEY_LSHIFT) || safeDown(InputConstants.KEY_RSHIFT);
        boolean ctrl = safeDown(InputConstants.KEY_LCONTROL) || safeDown(InputConstants.KEY_RCONTROL);
        boolean space = safeDown(InputConstants.KEY_SPACE);
        float mult = shift ? 0.2f : ctrl ? 3.0f : 1.0f;
        double dt = Math.min(dtSeconds, 0.1);

        if (EditorController.isVrPreviewActive()) {
            long now = System.nanoTime() / 1_000_000L;
            if (space && !lastSpaceDown) {
                if (now - lastSpaceTap < 1000L) {
                    VRAbstraction.previewHandRot().set(0.0f, 0.0f, 0.0f);
                    lastSpaceTap = 0L;
                } else {
                    lastSpaceTap = now;
                }
            }
            lastSpaceDown = space;
        }

        boolean w = safeDown(InputConstants.KEY_W);
        boolean s = safeDown(InputConstants.KEY_S);
        boolean a = safeDown(InputConstants.KEY_A);
        boolean d = safeDown(InputConstants.KEY_D);
        boolean q = safeDown(InputConstants.KEY_Q);
        boolean e = safeDown(InputConstants.KEY_E);

        if (!(w || s || a || d || q || e)) {
            return;
        }

        if (space && EditorController.isVrPreviewActive()) {
            org.joml.Vector3f r = VRAbstraction.previewHandRot();
            float step = ROTATE_RATE * mult * (float) dt;
            if (w) r.x -= step;
            if (s) r.x += step;
            if (a) r.z += step;
            if (d) r.z -= step;
            if (q) r.y -= step;
            if (e) r.y += step;
            r.set(Math.max(-360.0f, Math.min(360.0f, r.x)),
                Math.max(-360.0f, Math.min(360.0f, r.y)),
                Math.max(-360.0f, Math.min(360.0f, r.z)));
            return;
        }

        if (GizmoRenderer.isRotateMode()) {
            if (w) EditorController.rotateAxis(0, -ROTATE_RATE * mult * (float) dt);
            if (s) EditorController.rotateAxis(0, ROTATE_RATE * mult * (float) dt);
            if (a) EditorController.rotateAxis(2, ROTATE_RATE * mult * (float) dt);
            if (d) EditorController.rotateAxis(2, -ROTATE_RATE * mult * (float) dt);
            if (q) EditorController.rotateAxis(1, -ROTATE_RATE * mult * (float) dt);
            if (e) EditorController.rotateAxis(1, ROTATE_RATE * mult * (float) dt);
        } else {
            if (w) EditorController.translateAxis(0, -TRANSLATE_RATE * mult * (float) dt);
            if (s) EditorController.translateAxis(0, TRANSLATE_RATE * mult * (float) dt);
            if (a) EditorController.translateAxis(2, TRANSLATE_RATE * mult * (float) dt);
            if (d) EditorController.translateAxis(2, -TRANSLATE_RATE * mult * (float) dt);
            if (q) EditorController.translateAxis(1, -TRANSLATE_RATE * mult * (float) dt);
            if (e) EditorController.translateAxis(1, TRANSLATE_RATE * mult * (float) dt);
        }
    }

    private static boolean rawDown(int key) {
        return key != -1 && InputConstants.isKeyDown(key);
    }

    private record Vector2(double x, double y) {}
}
