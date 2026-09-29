package win.demistorm.mcpositioneditor.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import win.demistorm.mcpositioneditor.client.VRAbstraction;
import win.demistorm.mcpositioneditor.client.VRDebug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

// Editor UI (default opens with "P")
public class EditorScreen extends Screen {

    private static final int TEXT_COLOR = ARGB.colorFromFloat(1.0f, 0.92f, 0.92f, 0.92f);
    private static final int HINT_COLOR = ARGB.colorFromFloat(1.0f, 0.65f, 0.72f, 0.78f);
    private static final int ACCENT_COLOR = ARGB.colorFromFloat(1.0f, 0.55f, 0.85f, 0.55f);

    private static final int COL_W = 113;
    private static final int COL_GAP = 6;
    private static final int PANEL_W = COL_W * 2 + COL_GAP;
    private static final int BTN_WIDE = 88;
    private static final int BTN_SMALL = 44;

    private static final int READOUT_W = 218;

    private static boolean hasFocusedEditBox;

    private Button modeBtn;
    private Button contextBtn;
    private Button saveBtn;
    private Button closeBtn;
    private Button snapBtn;
    private final List<Button> saveMenuButtons = new ArrayList<>();
    private boolean saveMenuOpen;

    private final EditBox[] rotFields = new EditBox[3];
    private final EditBox[] transFields = new EditBox[3];
    private final EditBox[] scaleFields = new EditBox[3];
    private final List<Button> variantButtons = new ArrayList<>();
    private int readoutX;
    private int readoutY;
    private Identifier lastStripModel;
    private int stripBaseY = -1;

    private int panelX = 6;

    // Vivecraft's GUI quad auto-scales to 1280x720 so compact mode trims what doesn't fit
    private boolean compact;

    public EditorScreen() {
        super(Component.literal("Easy Item Positions"));
    }

    public static boolean hasFocusedEditBox() {
        return hasFocusedEditBox;
    }

    @Override
    protected void init() {
        hasFocusedEditBox = false;
        GizmoInput.resetKeyTracking();
        compact = this.height < 320;
        panelX = VRAbstraction.isActive() ? Math.max(6, (this.width - PANEL_W) / 2) : 6;
        int left = panelX;
        int y = 4;

        int mid = left + BTN_WIDE + COL_GAP;
        int rightCol = mid + BTN_SMALL + COL_GAP;
        modeBtn = addRenderableWidget(Button.builder(modeLabel(), b -> {
            EditorController.toggleMode();
            b.setMessage(modeLabel());
        }).bounds(left, y, BTN_WIDE, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Copy"), b -> {
            status(EditorController.copyTransform());
        }).bounds(mid, y, BTN_SMALL, 16).build());

        snapBtn = addRenderableWidget(Button.builder(snapLabel(), b -> {
            EditorController.setSnapEnabled(!EditorController.isSnapEnabled());
            b.setMessage(snapLabel());
        }).bounds(rightCol, y, BTN_WIDE, 16).build());

        y += 18;
        contextBtn = addRenderableWidget(Button.builder(contextLabel(), b -> {
            EditorController.cycleContext();
            refreshFieldValues(true);
        }).bounds(left, y, PANEL_W, 16).build());

        y += 18;
        saveBtn = addRenderableWidget(Button.builder(Component.literal("Save \u25be"), b -> {
            saveMenuOpen = !saveMenuOpen;
            updateSaveMenuVisibility();
        }).bounds(left, y, BTN_WIDE, 16).build());

        addRenderableWidget(Button.builder(Component.literal("Paste"), b -> {
            status(EditorController.pasteTransform());
        }).bounds(mid, y, BTN_SMALL, 16).build());

        closeBtn = addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
            .bounds(rightCol, y, BTN_WIDE, 16).build());

        saveMenuButtons.clear();
        int sy = y + 18;
        saveMenuButtons.add(addRenderableWidget(Button.builder(Component.literal("Save to Provider"), b -> {
            status(EditorController.saveToProvider());
        }).bounds(left, sy, PANEL_W, 15).build()));
        saveMenuButtons.add(addRenderableWidget(Button.builder(Component.literal("Apply to All Variants"), b -> {
            status(EditorController.applyToAllVariants());
        }).bounds(left, sy + 16, PANEL_W, 15).build()));
        saveMenuButtons.add(addRenderableWidget(Button.builder(Component.literal("Export Resourcepack"), b -> {
            status(EditorController.export());
        }).bounds(left, sy + 32, PANEL_W, 15).build()));
        saveMenuButtons.add(addRenderableWidget(Button.builder(Component.literal("Reset to Default"), b -> {
            status(EditorController.resetToDefault());
        }).bounds(left, sy + 48, PANEL_W, 15).build()));
        updateSaveMenuVisibility();

        int ry = sy + 67;
        readoutX = left + (PANEL_W - READOUT_W) / 2;
        readoutY = ry;
        addFields(rotFields, readoutX, ry, "rot");
        addFields(transFields, readoutX, ry + 21, "pos");
        addFields(scaleFields, readoutX, ry + 42, "scl");

        // Translation always shows JSON x16 (same units as the exported pack and vanilla)
        addRenderableWidget(Button.builder(Component.literal("-"), b -> EditorController.scaleUniform(-0.05f))
            .bounds(readoutX + 184, ry + 42, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> EditorController.scaleUniform(0.05f))
            .bounds(readoutX + 202, ry + 42, 16, 16).build());

        stripBaseY = ry + 64;
        rebuildVariantStrip(left, stripBaseY);
        refreshFieldValues(true);
    }

    private void addFields(EditBox[] fields, int x, int y, String label) {
        int fx = x + 38;
        for (int i = 0; i < 3; i++) {
            EditBox box = new NumericEditBox(this.font, fx, y, 34, 16, Component.literal(label + i));
            box.setMaxLength(12);
            fields[i] = addRenderableWidget(box);
            fx += 36;
        }
        addRenderableWidget(Button.builder(Component.literal("Set"), b -> applyFields(label))
            .bounds(fx, y, 34, 16).build());
    }

    private void rebuildVariantStrip(int x, int y) {
        variantButtons.forEach(this::removeWidget);
        variantButtons.clear();
        List<Identifier> family = EditorController.family();
        if (family.size() < 2) {
            return;
        }
        int max = compact ? Math.max(0, (this.height - 58 - y) / 16) : 10;
        List<Identifier> shown = family.size() > max ? family.subList(0, max) : family;
        for (Identifier model : shown) {
            Button b = Button.builder(Component.literal(variantLabel(model)), btn -> {
                EditorController.setSelectedModel(model);
                refreshFieldValues(true);
            }).bounds(x, y, 166, 15).build();
            variantButtons.add(addRenderableWidget(b));
            y += 16;
        }
    }

    private String variantLabel(Identifier model) {
        String name = model.getPath();
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        String prefix = "  ";
        if (model.equals(EditorController.activeModel())) {
            prefix = "\u25cf ";
        }
        if (model.equals(EditorController.selectedModel())) {
            prefix = "\u25b6 ";
        }
        return prefix + name;
    }

    private void updateSaveMenuVisibility() {
        saveMenuButtons.forEach(b -> b.visible = saveMenuOpen);
    }

    private Component modeLabel() {
        return Component.literal("Mode: " + (EditorController.mode() == EditorController.Mode.TRANSLATE ? "Move" : "Rotate"));
    }

    private Component contextLabel() {
        String name = EditorController.context().getSerializedName();
        if (EditorController.isVrPreview()) {
            name = "vr_" + name;
        }
        return Component.literal("Context: " + name + " \u25b8");
    }

    private Component snapLabel() {
        return Component.literal("Snap: " + (EditorController.isSnapEnabled() ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        double dt = lastRenderNanos > 0 ? (System.nanoTime() - lastRenderNanos) / 1.0E9 : 0.016;
        lastRenderNanos = System.nanoTime();

        EditorController.refreshHeldItem();
        if (!EditorController.isSessionOpen()) {
            onClose();
            return;
        }

        if (VRAbstraction.isActive() && VRDebug.gate("screen")) {
            String mcScreen = Minecraft.getInstance().screen == null
                ? "null" : Minecraft.getInstance().screen.getClass().getSimpleName();
            VRDebug.log("screen",
                "w/h=", this.width + "x" + this.height, "compact=", compact, "panelX=", panelX,
                "children=", this.children().size(), "mc.screen=", mcScreen);
        }

        modeBtn.setMessage(modeLabel());
        contextBtn.setMessage(contextLabel());
        snapBtn.setMessage(snapLabel());

        Identifier currentSel = EditorController.selectedModel();
        if (!java.util.Objects.equals(currentSel, lastStripModel)) {
            lastStripModel = currentSel;
            int stripX = panelX + 6;
            int stripY = stripBaseY;
            if (stripY >= 0) {
                rebuildVariantStrip(stripX, stripY);
            }
        }
        for (int i = 0; i < variantButtons.size() && i < EditorController.family().size(); i++) {
            variantButtons.get(i).setMessage(Component.literal(variantLabel(EditorController.family().get(i))));
        }

        boolean anyFocus = false;
        anyFocus |= refreshFieldValues(false);
        for (EditBox[] fields : new EditBox[][]{rotFields, transFields, scaleFields}) {
            for (EditBox f : fields) {
                if (f.isFocused()) {
                    anyFocus = true;
                }
            }
        }
        hasFocusedEditBox = anyFocus;

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int ry = readoutY;
        guiGraphics.text(this.font, "rot\u00b0", readoutX + 4, ry + 4, TEXT_COLOR);
        guiGraphics.text(this.font, "pos", readoutX + 4, ry + 25, TEXT_COLOR);
        guiGraphics.text(this.font, "scale", readoutX + 4, ry + 46, TEXT_COLOR);

        int hintY = this.height - 44;
        Identifier provider = EditorController.providerForSelected();
        String inherited = provider == null
            ? "no provider for this context"
            : "inherited from: " + EditorController.shortName(provider);
        int inheritedY = stripBaseY + variantButtons.size() * 16 + 2;
        if (compact) {
            inheritedY = Math.min(inheritedY, hintY - 12);
        }
        guiGraphics.text(this.font, inherited, panelX + 2, inheritedY, HINT_COLOR);

        Identifier sel = EditorController.selectedModel();
        guiGraphics.text(this.font, "editing: " + EditorController.shortName(sel), panelX + 2, hintY, ACCENT_COLOR);
        ItemDisplayContext vrCtx = EditorController.vrEffectiveContext();
        String live;
        if (EditorController.isVrPreviewActive()) {
            org.joml.Vector3f hr = VRAbstraction.previewHandRot();
            live = String.format("VR preview hand - Space+WASDQE rotates - rot=%.0f/%.0f/%.0f",
                hr.x, hr.y, hr.z);
        } else if (vrCtx != null) {
            live = "VR live context: " + vrCtx.getSerializedName();
        } else {
            live = "live context: " + EditorController.context().getSerializedName();
        }
        guiGraphics.text(this.font, live + (VRAbstraction.isActive() ? "  [VR]" : ""), panelX + 2, hintY + 10, HINT_COLOR);

        GizmoInput.updateHover();
        GizmoInput.tickKeyboard(dt);
    }

    private long lastRenderNanos;

    private boolean refreshFieldValues(boolean force) {
        boolean anyFocused = force;
        if (!force) {
            for (EditBox[] fields : new EditBox[][]{rotFields, transFields, scaleFields}) {
                for (EditBox f : fields) {
                    if (f != null && f.isFocused()) {
                        anyFocused = true;
                    }
                }
            }
            if (anyFocused) {
                return true;
            }
        }
        var t = EditorController.working();
        for (int i = 0; i < 3; i++) {
            setIfNotNull(rotFields[i], fmt(t.rotation(), i, 1.0f));
            setIfNotNull(transFields[i], fmt(t.translation(), i, 16.0f));
            setIfNotNull(scaleFields[i], fmt(t.scale(), i, 1.0f));
        }
        return anyFocused;
    }

    private static void setIfNotNull(EditBox box, String value) {
        if (box != null && !box.isFocused()) {
            box.setValue(value);
        }
    }

    private static String fmt(org.joml.Vector3fc v, int i, float mul) {
        return String.format(Locale.ROOT, "%.2f", component(v, i) * mul);
    }

    private static float component(org.joml.Vector3fc v, int i) {
        return switch (i) {
            case 0 -> v.x();
            case 1 -> v.y();
            default -> v.z();
        };
    }

    private void applyFields(String label) {
        try {
            switch (label) {
                case "rot" -> EditorController.setRotation(
                    parse(rotFields[0]), parse(rotFields[1]), parse(rotFields[2]));
                case "pos" -> {
                    EditorController.setTranslation(
                        parse(transFields[0]) / 16.0f, parse(transFields[1]) / 16.0f, parse(transFields[2]) / 16.0f);
                }
                default -> EditorController.setScale(
                    parse(scaleFields[0]), parse(scaleFields[1]), parse(scaleFields[2]));
            }
            status("Set " + label + " values");
        } catch (NumberFormatException e) {
            status("Bad number in " + label);
        }
        refreshFieldValues(true);
    }

    private static float parse(EditBox box) {
        String s = box.getValue().trim();
        return s.isEmpty() || s.equals("-") ? 0.0f : Float.parseFloat(s);
    }

    private void status(String message) {
        if (message != null && minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.literal(message));
        }
        log.info("Editor: {}", message == null ? "" : message);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int i, int j, float f) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (Math.abs(scrollY) > 0.0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                int slot = mc.player.getInventory().getSelectedSlot();
                slot = Math.floorMod(slot + (scrollY > 0 ? -1 : 1), 9);
                mc.player.getInventory().setSelectedSlot(slot);
                return true;
            }
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!hasFocusedEditBox) {
            if (keyEvent.isCopy()) {
                status(EditorController.copyTransform());
                return true;
            }
            if (keyEvent.isPaste()) {
                status(EditorController.pasteTransform());
                return true;
            }
        }
        int key = keyEvent.input();
        if (key == 32 && !hasFocusedEditBox && EditorController.isVrPreviewActive()) {
            return true;
        }
        int digit = -1;
        if (key >= 49 && key <= 57) {
            digit = key - 48;
        } else if (key >= 321 && key <= 329) {
            digit = key - 320;
        }
        if (digit > 0 && !hasFocusedEditBox) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getInventory().setSelectedSlot(digit - 1);
                return true;
            }
        }
        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && GizmoInput.mouseClicked()) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        GizmoInput.mouseDragged();
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        GizmoInput.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        EditorController.closeSession();
        GizmoRenderer.reset();
        super.onClose();
    }

    @Override
    public void removed() {
        hasFocusedEditBox = false;
    }
}
