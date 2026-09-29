package win.demistorm.mcpositioneditor.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import win.demistorm.mcpositioneditor.ConfigHelper;

public final class ConfigScreen extends Screen {

    private final Screen parent;
    private final Minecraft client = Minecraft.getInstance();
    private boolean boundsCenteredValue = ConfigHelper.ACTIVE.boundsCenteredGizmos;
    private boolean autoApplyValue = ConfigHelper.ACTIVE.autoApplyVariants;

    private ConfigScreen(Screen parent) {
        super(Component.literal("Easy Item Positions Settings"));
        this.parent = parent;
    }

    public static ConfigScreen create(Screen parent) {
        return new ConfigScreen(parent);
    }

    private int toggleWidth(String prefix) {
        int textW = Math.max(font.width(prefix + "ON"), font.width(prefix + "OFF"));
        return textW + 16;
    }

    @Override
    protected void init() {
        String boundsPrefix = "Bounds-centered Gizmos: ";
        int toggleW = toggleWidth(boundsPrefix);
        addRenderableWidget(
                Button.builder(
                                Component.literal(boundsPrefix + (boundsCenteredValue ? "ON" : "OFF")),
                                btn -> {
                                    boundsCenteredValue = !boundsCenteredValue;
                                    btn.setMessage(Component.literal(
                                            boundsPrefix + (boundsCenteredValue ? "ON" : "OFF")));
                                })
                        .bounds(width / 2 - toggleW / 2, height / 4 + 24, toggleW, 20)
                        .tooltip(Tooltip.create(Component.literal(
                                """
                                OFF: Renders gizmo at .json models' pivot point (default, actual transforms always apply to pivot point regardless of this setting)
                                ON: Tries to render gizmo at model's center, however keep in mind that edits always apply to the model's .json pivot point regardless (can be easier to manipulate items where pivot points are offcentered significantly)
                                """)))
                        .build());

        String variantsPrefix = "Auto Apply to Variants: ";
        addRenderableWidget(
                Button.builder(
                                Component.literal(variantsPrefix + (autoApplyValue ? "ON" : "OFF")),
                                btn -> {
                                    autoApplyValue = !autoApplyValue;
                                    btn.setMessage(Component.literal(
                                            variantsPrefix + (autoApplyValue ? "ON" : "OFF")));
                                })
                        .bounds(width / 2 - toggleW / 2, height / 4 + 54, toggleW, 20)
                        .tooltip(Tooltip.create(Component.literal(
                                "Automatically applies edits to all item variants (may be useful for animated items, food eating, etc). Alternative to the save dropdown option")))
                        .build());

        addRenderableWidget(
                Button.builder(Component.literal("Done"),
                                btn -> {
                                    ConfigHelper.ACTIVE.boundsCenteredGizmos = boundsCenteredValue;
                                    ConfigHelper.ACTIVE.autoApplyVariants = autoApplyValue;
                                    ConfigHelper.write(ConfigHelper.ACTIVE);
                                    client.setScreen(parent);
                                })
                        .bounds(width / 2 - 100, height - 27, 200, 20)
                        .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
    }
}
