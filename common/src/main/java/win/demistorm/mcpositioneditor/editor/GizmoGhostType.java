package win.demistorm.mcpositioneditor.editor;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import win.demistorm.mcpositioneditor.mixin.RenderPipelinesAccessor;
import win.demistorm.mcpositioneditor.mixin.RenderTypeAccessor;

public final class GizmoGhostType {

    // Same uniform layouts as vanilla's lines pipelines, only the depth test differs (see-through ghost)
    private static final RenderPipeline GHOST_LINES = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
        .withBindGroupLayout(BindGroupLayouts.FOG)
        .withVertexShader("core/rendertype_lines")
        .withFragmentShader("core/rendertype_lines")
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
        .withPrimitiveTopology(PrimitiveTopology.LINES)
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withLocation(Identifier.fromNamespaceAndPath("mcpositioneditor", "pipeline/gizmo_ghost_lines"))
        .build();

    public static final RenderType GHOST_LINES_TYPE = RenderTypeAccessor.mcpositioneditor$create(
        "mcpositioneditor_gizmo_ghost",
        RenderSetup.builder(GHOST_LINES).createRenderSetup());

    public static void init() {
        RenderPipelinesAccessor.mcpositioneditor$register(GHOST_LINES);
    }

    private GizmoGhostType() {}
}
