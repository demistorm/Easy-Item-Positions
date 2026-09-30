package win.demistorm.mcpositioneditor.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import win.demistorm.mcpositioneditor.ConfigHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public final class ConfigScreen extends Screen {

    private static final String SPRITES_ROOT = "textures/gui/sprites";
    private static final String ART_FOLDER = SPRITES_ROOT + "/art";
    private static final Pattern VALID_SPRITE_PATH = Pattern.compile("[a-z0-9/._-]+");
    private static final Component CREDIT = Component.literal("Some fun mod icons/art from the folks on Discord :D");
    private static final int CREDIT_COLOR = ARGB.colorFromFloat(1.0f, 0.65f, 0.65f, 0.65f);
    private static final int TOGGLE_WIDTH = 170;

    // Gui atlas sprite ids (files at textures/gui/sprites/path.png)
    private record Splash(Identifier texture, int width, int height) {}

    private final Screen parent;
    private final Splash splash;
    private final Minecraft client = Minecraft.getInstance();
    private boolean boundsCenteredValue = ConfigHelper.ACTIVE.boundsCenteredGizmos;
    private boolean autoApplyValue = ConfigHelper.ACTIVE.autoApplyVariants;

    // Art panel on the left, toggles on the right
    private int artX, artY, artW, artH;
    private int toggleX, toggleY;

    private ConfigScreen(Screen parent) {
        super(Component.literal("Easy Item Positions Settings"));
        this.parent = parent;
        List<Splash> pool = discoverSplashes();
        this.splash = pool.isEmpty() ? null : pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    // Any png added in the art folder joins the rotation automatically
    private static List<Splash> discoverSplashes() {
        Minecraft mc = Minecraft.getInstance();
        TextureAtlas guiAtlas = mc.getAtlasManager().getAtlasOrThrow(Identifier.withDefaultNamespace("gui"));
        List<Splash> found = new ArrayList<>();
        mc.getResourceManager()
            .listResources(ART_FOLDER, id -> id.getPath().endsWith(".png")
                && VALID_SPRITE_PATH.matcher(id.getPath()).matches())
            .keySet().stream().sorted()
            .forEach(file -> {
                String path = file.getPath();
                Identifier spriteId = Identifier.fromNamespaceAndPath(file.getNamespace(),
                    path.substring(SPRITES_ROOT.length() + 1, path.length() - 4));
                TextureAtlasSprite sprite = guiAtlas.getSprite(spriteId);
                found.add(new Splash(spriteId, sprite.contents().width(), sprite.contents().height()));
            });
        return found;
    }

    public static ConfigScreen create(Screen parent) {
        return new ConfigScreen(parent);
    }

    private void layout() {
        int top = 44;
        int maxH = Math.max(60, height - 104);
        int maxW = Math.max(80, width / 2 - 24);
        if (splash == null) {
            artW = 0;
            artH = 0;
        } else {
            float scale = Math.min((float) maxW / splash.width(), (float) maxH / splash.height());
            artW = Math.round(splash.width() * scale);
            artH = Math.round(splash.height() * scale);
        }
        artX = 24 + (maxW - artW) / 2;
        artY = top + (maxH - artH) / 2;
        int regionLeft = artX + artW + 16;
        toggleX = (regionLeft + width - 16) / 2 - TOGGLE_WIDTH / 2;
        toggleY = artY + artH / 2 - 25;
    }

    @Override
    protected void init() {
        layout();
        String boundsPrefix = "Bounds-centered Gizmos: ";
        addRenderableWidget(
                Button.builder(
                                Component.literal(boundsPrefix + (boundsCenteredValue ? "ON" : "OFF")),
                                btn -> {
                                    boundsCenteredValue = !boundsCenteredValue;
                                    btn.setMessage(Component.literal(
                                            boundsPrefix + (boundsCenteredValue ? "ON" : "OFF")));
                                })
                        .bounds(toggleX, toggleY, TOGGLE_WIDTH, 20)
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
                        .bounds(toggleX, toggleY + 30, TOGGLE_WIDTH, 20)
                        .tooltip(Tooltip.create(Component.literal(
                                "Automatically applies edits to all item variants (may be useful for animated items, food eating, etc). Alternative to the save dropdown option")))
                        .build());

        addRenderableWidget(
                Button.builder(Component.literal("Done"),
                                btn -> {
                                    ConfigHelper.ACTIVE.boundsCenteredGizmos = boundsCenteredValue;
                                    ConfigHelper.ACTIVE.autoApplyVariants = autoApplyValue;
                                    ConfigHelper.write();
                                    client.gui.setScreen(parent);
                                })
                        .bounds(width / 2 - 100, height - 27, 200, 20)
                        .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Art renders first so buttons render aren't stuck behind
        drawArt(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
    }

    private void drawArt(GuiGraphicsExtractor context) {
        if (splash == null) {
            return;
        }
        context.blitSprite(RenderPipelines.GUI_TEXTURED, splash.texture(), artX, artY, artW, artH);
        int creditY = Math.min(artY + artH + 4, height - 38);
        int halfW = font.width(CREDIT) * 3 / 8;
        int cx = Math.max(halfW + 4, Math.min(artX + artW / 2, width - halfW - 4));
        context.pose().pushMatrix();
        context.pose().translate(cx, creditY);
        context.pose().scale(0.75F, 0.75F);
        context.text(font, CREDIT, -font.width(CREDIT) / 2, 0, CREDIT_COLOR);
        context.pose().popMatrix();
    }
}
