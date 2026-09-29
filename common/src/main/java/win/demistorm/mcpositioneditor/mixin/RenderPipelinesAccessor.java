package win.demistorm.mcpositioneditor.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {

    @Invoker("register")
    static RenderPipeline mcpositioneditor$register(RenderPipeline pipeline) {
        throw new AssertionError();
    }
}
