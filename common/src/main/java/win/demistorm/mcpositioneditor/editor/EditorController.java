package win.demistorm.mcpositioneditor.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import win.demistorm.mcpositioneditor.ConfigHelper;
import win.demistorm.mcpositioneditor.export.ExportManager;
import win.demistorm.mcpositioneditor.client.VRAbstraction;
import win.demistorm.mcpositioneditor.client.VRDebug;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

public final class EditorController {

    private EditorController() {}

    public enum Mode {
        TRANSLATE, ROTATE
    }

    public static final float MAX_TRANSLATION = 5.0f;
    public static final float MAX_SCALE = 4.0f;
    public static final float SNAP_STEP_DEGREES = 15.0f;
    // Snap step is in JSON/display units (the readout and pack files show value*16)
    public static final float SNAP_STEP_TRANSLATION = 0.25f / 16.0f;

    private static boolean sessionOpen;
    private static ItemStack sessionStack = ItemStack.EMPTY;
    private static Identifier itemId;
    private static Identifier selectedModel;
    private static Identifier activeModel;
    private static ItemDisplayContext ctx = ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    // Virtual context cycle "vr_firstperson_righthand" (same data but the editor draws the Vivecraft arm at a synthetic pose)
    private static boolean vrPreview;
    private static Mode mode = Mode.TRANSLATE;
    private static boolean snapEnabled = true;
    private static ItemDisplayContext vrEffectiveCtx;
    private static boolean ctxObserved;

    private static boolean followActive = true;

    private static final Map<ItemDisplayContext, ItemTransform> WORKING = new EnumMap<>(ItemDisplayContext.class);

    private static final Vector3f ROT_ACCUM = new Vector3f();
    private static boolean rotAccumValid;
    private static Identifier rotAccumModel;
    private static ItemDisplayContext rotAccumCtx;

    private static final Vector3f TRANS_ACCUM = new Vector3f();
    private static boolean transAccumValid;
    private static Identifier transAccumModel;
    private static ItemDisplayContext transAccumCtx;

    private static ItemTransform clipboard;
    private static String clipboardSource;

    public static boolean isSessionOpen() {
        return sessionOpen;
    }

    public static boolean openSession() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) {
            return false;
        }
        reanchor(held);
        boolean mainArmRight = mc.player.getMainArm() == HumanoidArm.RIGHT;
        if (VRAbstraction.isActive()) {
            vrEffectiveCtx = VRAbstraction.effectiveHeldContext(mainArmRight);
            ctx = vrEffectiveCtx;
        } else {
            vrEffectiveCtx = null;
            ctx = mainArmRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }
        vrPreview = false;
        VRAbstraction.resetPreviewLatch();
        sessionOpen = true;
        log.info("Editor session open: item={}, model={}, ctx={}", itemId, selectedModel, ctx);
        return true;
    }

    public static void closeSession() {
        sessionOpen = false;
        WORKING.clear();
        rotAccumValid = false;
        transAccumValid = false;
        if (VRAbstraction.isAvailable()) {
            Vector3f rot = VRAbstraction.previewHandRot();
            ConfigHelper.ACTIVE.previewHandRot =
                new float[]{rot.x, rot.y, rot.z};
            ConfigHelper.write(ConfigHelper.ACTIVE);
        }
        OverrideStorage.flush();
    }

    public static void refreshHeldItem() {
        if (!sessionOpen) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            closeSession();
            return;
        }
        ItemStack held = mc.player.getMainHandItem();
        if (!ItemStack.matches(held, sessionStack)) {
            if (held.isEmpty()) {
                closeSession();
            } else {
                reanchor(held);
            }
        }
    }

    private static void reanchor(ItemStack held) {
        sessionStack = held.copy();
        itemId = TransformOverrideManager.itemModelId(sessionStack);
        WORKING.clear();
        activeModel = null;
        followActive = true;
        ctxObserved = false;
        if (itemId != null) {
            List<Identifier> family = TransformOverrideManager.familyOf(itemId);
            selectedModel = family.isEmpty() ? itemId : family.getFirst();
        } else {
            selectedModel = null;
        }
    }

    // renderItem hook reports the context Vivecraft uses
    public static void followRenderCtx(ItemDisplayContext observed) {
        if (!sessionOpen || !followActive || ctxObserved || observed == ctx) {
            return;
        }
        ctxObserved = true;
        VRDebug.log("ctx-follow: ", ctx, " -> ", observed);
        ctx = observed;
        if (VRAbstraction.isActive()) {
            vrEffectiveCtx = observed;
        }
        rotAccumValid = false;
        transAccumValid = false;
        WORKING.clear();
    }

    public static void noteRenderedModel(Identifier modelId, ItemStack stack, ItemDisplayContext renderCtx) {
        if (!sessionOpen || renderCtx != ctx || !ItemStack.matches(stack, sessionStack)) {
            return;
        }
        activeModel = modelId;
        if (selectedModel == null) {
            selectedModel = modelId;
        }
        if (followActive && modelId != null && !modelId.equals(selectedModel)
            && TransformOverrideManager.familyOf(itemId).contains(modelId)) {
            selectedModel = modelId;
            WORKING.clear();
            rotAccumValid = false;
            transAccumValid = false;
        }
    }

    public static ItemTransform resolveLive(Identifier modelId, ItemDisplayContext renderCtx) {
        if (sessionOpen && modelId != null && modelId.equals(selectedModel)) {
            ItemTransform preview = WORKING.get(renderCtx);
            if (preview != null) {
                return preview;
            }
        }
        return TransformOverrideManager.resolveOverride(modelId, renderCtx);
    }

    public static ItemTransform working() {
        if (selectedModel == null) {
            return new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f));
        }
        ItemTransform t = WORKING.get(ctx);
        if (t == null) {
            ItemTransform base = null;
            Map<ItemDisplayContext, ItemTransform> own = TransformOverrideManager.overrides().get(selectedModel);
            if (own != null) {
                base = own.get(ctx);
            }
            if (base == null) {
                base = TransformOverrideManager.resolveOriginal(selectedModel, ctx);
            }
            if (base == ItemTransform.NO_TRANSFORM) {
                base = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0f, 1.0f, 1.0f));
            }
            t = TransformOverrideManager.copy(base);
            WORKING.put(ctx, t);
        }
        return t;
    }

    private static void commit(ItemTransform t) {
        followActive = false;
        ItemTransform prev = WORKING.put(ctx, t);
        if (selectedModel != null) {
            TransformOverrideManager.setOverride(selectedModel, ctx, t);
        }
        if (ConfigHelper.ACTIVE.autoApplyVariants && changed(prev, t)) {
            applyToAllVariants();
        }
    }

    private static boolean changed(ItemTransform a, ItemTransform b) {
        if (a == null) {
            return true;
        }
        return !a.translation().equals(b.translation(), 1.0E-4f)
            || !a.rotation().equals(b.rotation(), 1.0E-4f)
            || !a.scale().equals(b.scale(), 1.0E-4f);
    }

    public static void translateAxis(int axis, float amount) {
        Vector3f d = new Vector3f();
        setComponent(d, axis, amount);
        translateDelta(d);
    }

    public static void translateLocal(float dx, float dy, float dz) {
        translateDelta(new Vector3f(dx, dy, dz));
    }

    private static void translateDelta(org.joml.Vector3fc localDelta) {
        ItemTransform t = working();
        Vector3f dir = new Vector3f(localDelta);
        dir.mul(t.scale());
        Quaternionf rot = eulerQuaternion(t.rotation());
        dir.rotate(rot);
        if (ctx != null && ctx.leftHand()) {
            dir.x = -dir.x;
        }
        Vector3f trans = new Vector3f(t.translation()).add(dir);
        if (snapEnabled) {
            if (!transAccumValid || transAccumModel != selectedModel || transAccumCtx != ctx) {
                TRANS_ACCUM.set(t.translation());
                transAccumModel = selectedModel;
                transAccumCtx = ctx;
                transAccumValid = true;
            }
            TRANS_ACCUM.add(dir);
            clampTranslation(TRANS_ACCUM);
            Vector3f snapped = new Vector3f(
                Math.round(TRANS_ACCUM.x / SNAP_STEP_TRANSLATION) * SNAP_STEP_TRANSLATION,
                Math.round(TRANS_ACCUM.y / SNAP_STEP_TRANSLATION) * SNAP_STEP_TRANSLATION,
                Math.round(TRANS_ACCUM.z / SNAP_STEP_TRANSLATION) * SNAP_STEP_TRANSLATION);
            trans = snapped;
        }
        clampTranslation(trans);
        commit(new ItemTransform(new Vector3f(t.rotation()), trans, new Vector3f(t.scale())));
    }

    private static void clampTranslation(Vector3f trans) {
        trans.set(Mth.clamp(trans.x, -MAX_TRANSLATION, MAX_TRANSLATION),
            Mth.clamp(trans.y, -MAX_TRANSLATION, MAX_TRANSLATION),
            Mth.clamp(trans.z, -MAX_TRANSLATION, MAX_TRANSLATION));
    }

    public static void rotateAxis(int axis, float deltaDeg) {
        ItemTransform t = working();
        if (!rotAccumValid || rotAccumModel != selectedModel || rotAccumCtx != ctx) {
            ROT_ACCUM.set(t.rotation());
            rotAccumModel = selectedModel;
            rotAccumCtx = ctx;
            rotAccumValid = true;
        }
        if (snapEnabled) {
            setComponent(ROT_ACCUM, axis, component(ROT_ACCUM, axis) + deltaDeg);
            float snapped = Math.round(component(ROT_ACCUM, axis) / SNAP_STEP_DEGREES) * SNAP_STEP_DEGREES;
            deltaDeg = snapped - component(t.rotation(), axis);
            if (deltaDeg == 0.0f) {
                return;
            }
        }
        if (ctx != null && ctx.leftHand() && axis != 0) {
            deltaDeg = -deltaDeg;
        }
        Vector3f rot = new Vector3f(t.rotation());
        setComponent(rot, axis, component(rot, axis) + deltaDeg);
        commit(new ItemTransform(rot, new Vector3f(t.translation()), new Vector3f(t.scale())));
    }

    public static void scaleUniform(float delta) {
        ItemTransform t = working();
        float base = (t.scale().x() + t.scale().y() + t.scale().z()) / 3.0f;
        float target = Mth.clamp(base + delta, -MAX_SCALE, MAX_SCALE);
        commit(new ItemTransform(new Vector3f(t.rotation()), new Vector3f(t.translation()),
            new Vector3f(target, target, target)));
    }

    public static void setTranslation(float x, float y, float z) {
        transAccumValid = false;
        ItemTransform t = working();
        Vector3f trans = new Vector3f(
            Mth.clamp(x, -MAX_TRANSLATION, MAX_TRANSLATION),
            Mth.clamp(y, -MAX_TRANSLATION, MAX_TRANSLATION),
            Mth.clamp(z, -MAX_TRANSLATION, MAX_TRANSLATION));
        commit(new ItemTransform(new Vector3f(t.rotation()), trans, new Vector3f(t.scale())));
    }

    public static void setRotation(float x, float y, float z) {
        rotAccumValid = false;
        ItemTransform t = working();
        commit(new ItemTransform(new Vector3f(x, y, z), new Vector3f(t.translation()), new Vector3f(t.scale())));
    }

    public static void setScale(float x, float y, float z) {
        ItemTransform t = working();
        Vector3f scale = new Vector3f(
            Mth.clamp(x, -MAX_SCALE, MAX_SCALE),
            Mth.clamp(y, -MAX_SCALE, MAX_SCALE),
            Mth.clamp(z, -MAX_SCALE, MAX_SCALE));
        commit(new ItemTransform(new Vector3f(t.rotation()), new Vector3f(t.translation()), scale));
    }

    public static void resetWorking() {
        WORKING.remove(ctx);
        rotAccumValid = false;
        transAccumValid = false;
    }

    public static String copyTransform() {
        if (selectedModel == null) {
            return "Nothing to copy";
        }
        clipboard = TransformOverrideManager.copy(working());
        clipboardSource = shortName(selectedModel) + " [" + ctx.getSerializedName() + "]";
        return "Copied " + clipboardSource;
    }

    public static String pasteTransform() {
        if (clipboard == null) {
            return "Clipboard is empty";
        }
        if (selectedModel == null) {
            return "Nothing selected";
        }
        Vector3f trans = new Vector3f(clipboard.translation());
        clampTranslation(trans);
        Vector3f scale = new Vector3f(clipboard.scale());
        scale.set(Mth.clamp(scale.x, -MAX_SCALE, MAX_SCALE),
            Mth.clamp(scale.y, -MAX_SCALE, MAX_SCALE),
            Mth.clamp(scale.z, -MAX_SCALE, MAX_SCALE));
        rotAccumValid = false;
        transAccumValid = false;
        commit(new ItemTransform(new Vector3f(clipboard.rotation()), trans, scale));
        return "Pasted " + clipboardSource;
    }

    public static String saveToProvider() {
        if (selectedModel == null) {
            return "Nothing to save";
        }
        Identifier provider = TransformOverrideManager.providerOf(selectedModel, ctx);
        if (provider == null) {
            return "No provider for " + ctx.getSerializedName();
        }
        TransformOverrideManager.setOverride(provider, ctx, working());
        TransformOverrideManager.clearOverride(selectedModel, ctx);
        resetWorking();
        return "Saved to provider " + shortName(provider) + " [" + ctx.getSerializedName() + "]";
    }

    public static String applyToAllVariants() {
        if (itemId == null) {
            return "No item selected";
        }
        List<Identifier> family = TransformOverrideManager.familyOf(itemId);
        if (family.isEmpty()) {
            return "No variants found";
        }
        ItemTransform t = working();
        for (Identifier m : family) {
            TransformOverrideManager.setOverride(m, ctx, t);
        }
        return "Applied to all " + family.size() + " variants [" + ctx.getSerializedName() + "]";
    }

    public static String resetToDefault() {
        if (selectedModel == null) {
            return "Nothing selected";
        }
        List<String> cleared = new ArrayList<>();
        if (hasOverrideForSelected()) {
            TransformOverrideManager.clearOverride(selectedModel, ctx);
            cleared.add(shortName(selectedModel));
        }
        Identifier provider = providerForSelected();
        Map<ItemDisplayContext, ItemTransform> pov =
            provider == null ? null : TransformOverrideManager.overrides().get(provider);
        if (pov != null && pov.containsKey(ctx)) {
            TransformOverrideManager.clearOverride(provider, ctx);
            cleared.add(shortName(provider));
        }
        resetWorking();
        OverrideStorage.flush();
        if (cleared.isEmpty()) {
            return "Already vanilla [" + ctx.getSerializedName() + "]";
        }
        return "Reset to default: " + String.join(", ", cleared);
    }

    public static String export() {
        OverrideStorage.flush();
        return ExportManager.exportAll();
    }

    // VR hotswitch
    public static void onVrModeChanged() {
        if (!sessionOpen) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean mainArmRight = mc.player != null && mc.player.getMainArm() == HumanoidArm.RIGHT;
        ItemDisplayContext now;
        if (VRAbstraction.isActive()) {
            now = VRAbstraction.effectiveHeldContext(mainArmRight);
            vrEffectiveCtx = now;
        } else {
            vrEffectiveCtx = null;
            now = mainArmRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }
        if (now != ctx) {
            ctx = now;
            WORKING.clear();
            rotAccumValid = false;
            transAccumValid = false;
        }
        vrPreview = false;
        VRAbstraction.resetVrInteraction();
        GizmoRenderer.setHoveredAxis(-1);
        if (mc.screen instanceof EditorScreen panel) {
            panel.resize(panel.width, panel.height);
        }
    }

    public static ItemStack sessionStack() {
        return sessionStack;
    }

    public static Identifier itemId() {
        return itemId;
    }

    public static Identifier selectedModel() {
        return selectedModel;
    }

    public static void setSelectedModel(Identifier model) {
        if (model != null && !model.equals(selectedModel)) {
            followActive = false;
            selectedModel = model;
            WORKING.clear();
            rotAccumValid = false;
            transAccumValid = false;
        }
    }

    public static Identifier activeModel() {
        return activeModel;
    }

    public static ItemDisplayContext context() {
        return ctx;
    }

    public static boolean isVrPreview() {
        return vrPreview;
    }

    public static boolean isVrPreviewActive() {
        return sessionOpen && vrPreview && VRAbstraction.isPreviewUsable() && !VRAbstraction.isActive();
    }

    public static void cycleContext() {
        ItemDisplayContext[] contexts = TransformOverrideManager.EDITOR_CONTEXTS;
        int i = 0;
        for (int j = 0; j < contexts.length; j++) {
            if (contexts[j] == ctx) {
                i = j;
                break;
            }
        }
        if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && !vrPreview
            && VRAbstraction.isPreviewUsable() && !VRAbstraction.isActive()) {
            vrPreview = true;
            followActive = true;
            return;
        }
        vrPreview = false;
        i = Math.floorMod(i + 1, contexts.length);
        ctx = contexts[i];
        followActive = true;
    }

    public static Mode mode() {
        return mode;
    }

    public static void toggleMode() {
        mode = mode == Mode.TRANSLATE ? Mode.ROTATE : Mode.TRANSLATE;
    }

    public static boolean isSnapEnabled() {
        return snapEnabled;
    }

    public static void setSnapEnabled(boolean snap) {
        if (snap && !snapEnabled) {
            transAccumValid = false;
        }
        snapEnabled = snap;
    }

    public static ItemDisplayContext vrEffectiveContext() {
        return vrEffectiveCtx;
    }

    public static List<Identifier> family() {
        return itemId == null ? List.of() : TransformOverrideManager.familyOf(itemId);
    }

    public static Identifier providerForSelected() {
        return selectedModel == null ? null : TransformOverrideManager.providerOf(selectedModel, ctx);
    }

    public static boolean hasOverrideForSelected() {
        Map<ItemDisplayContext, ItemTransform> own = TransformOverrideManager.overrides().get(selectedModel);
        return own != null && own.containsKey(ctx);
    }

    public static Quaternionf eulerQuaternion(org.joml.Vector3fc rot) {
        return new Quaternionf().rotationXYZ(
            (float) Math.toRadians(rot.x()),
            (float) Math.toRadians(rot.y()),
            (float) Math.toRadians(rot.z()));
    }

    private static float component(org.joml.Vector3fc v, int axis) {
        return switch (axis) {
            case 0 -> v.x();
            case 1 -> v.y();
            default -> v.z();
        };
    }

    private static void setComponent(Vector3f v, int axis, float value) {
        switch (axis) {
            case 0 -> v.x = value;
            case 1 -> v.y = value;
            default -> v.z = value;
        }
    }

    public static String shortName(Identifier id) {
        return id == null ? "?" : id.toShortString();
    }
}
