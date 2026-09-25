package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.editor.EditorRenderContext;
import win.demistorm.mcpositioneditor.editor.TransformOverrideManager;

// Same model tagging as BlockModelWrapperMixin but for special renderers (trident, shield, etc), whose display transform come from the base model
@Mixin(SpecialModelWrapper.class)
public abstract class SpecialModelWrapperMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void mcpositioneditor$beginModel(ItemStackRenderState itemStackRenderState, ItemStack itemStack,
                                              ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext,
                                              @Nullable ClientLevel clientLevel, @Nullable ItemOwner itemOwner, int i,
                                              CallbackInfo ci) {
        EditorRenderContext.beginModel(
            TransformOverrideManager.specialWrapperId((SpecialModelWrapper<?>) (Object) this), itemStack, itemDisplayContext);
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void mcpositioneditor$endModel(ItemStackRenderState itemStackRenderState, ItemStack itemStack,
                                            ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext,
                                            @Nullable ClientLevel clientLevel, @Nullable ItemOwner itemOwner, int i,
                                            CallbackInfo ci) {
        EditorRenderContext.endModel();
    }
}
