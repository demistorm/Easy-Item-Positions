package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CuboidItemModelWrapper.class)
public interface BlockModelWrapperAccessor {
    @Accessor("itemQuads")
    ItemQuads mcpositioneditor$itemQuads();

    @Accessor("transformation")
    Matrix4fc mcpositioneditor$transformation();
}
