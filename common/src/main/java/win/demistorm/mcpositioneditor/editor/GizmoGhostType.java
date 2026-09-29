package win.demistorm.mcpositioneditor.editor;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import win.demistorm.mcpositioneditor.mixin.RenderPipelinesAccessor;
import win.demistorm.mcpositioneditor.mixin.RenderTypeAccessor;

public final class GizmoGhostType {

    private static final RenderPipeline GHOST_LINES = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.FOG)
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withVertexShader("core/rendertype_lines")
        .withFragmentShader("core/rendertype_lines")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
        .withPrimitiveTopology(PrimitiveTopology.LINES)
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0.0f, 0.0f))
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
