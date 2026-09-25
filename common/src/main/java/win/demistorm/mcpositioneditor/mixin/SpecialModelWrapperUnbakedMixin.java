package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import win.demistorm.mcpositioneditor.editor.TransformOverrideManager;

// Baked special wrappers don't know their base id either (registered at bake time so the render path can tag them)
@Mixin(SpecialModelWrapper.Unbaked.class)
public abstract class SpecialModelWrapperUnbakedMixin {

    @Shadow
    public abstract Identifier base();

    @Inject(method = "bake", at = @At("TAIL"))
    private void mcpositioneditor$registerWrapper(ItemModel.BakingContext bakingContext,
                                                  CallbackInfoReturnable<ItemModel> cir) {
        if (cir.getReturnValue() instanceof SpecialModelWrapper<?> wrapper) {
            TransformOverrideManager.registerSpecialWrapper(wrapper, base());
        }
    }
}
