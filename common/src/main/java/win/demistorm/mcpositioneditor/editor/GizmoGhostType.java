package win.demistorm.mcpositioneditor.editor;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import win.demistorm.mcpositioneditor.mixin.RenderPipelinesAccessor;
import win.demistorm.mcpositioneditor.mixin.RenderTypeAccessor;

public final class GizmoGhostType {

    private static final RenderPipeline GHOST_LINES = RenderPipeline.builder()
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("Fog", UniformType.UNIFORM_BUFFER)
        .withUniform("Globals", UniformType.UNIFORM_BUFFER)
        .withVertexShader("core/rendertype_lines")
        .withFragmentShader("core/rendertype_lines")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withLocation(Identifier.fromNamespaceAndPath("mcpositioneditor", "pipeline/gizmo_ghost_lines"))
        .build();

    public static final RenderType GHOST_LINES_TYPE = RenderTypeAccessor.mcpositioneditor$create(
        "mcpositioneditor_gizmo_ghost",
        RenderSetup.builder(GHOST_LINES)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup());

    public static void init() {
        RenderPipelinesAccessor.mcpositioneditor$register(GHOST_LINES);
    }

    private GizmoGhostType() {}
}
