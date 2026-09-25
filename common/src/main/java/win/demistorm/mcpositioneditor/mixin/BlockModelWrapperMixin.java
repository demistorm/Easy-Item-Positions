package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.editor.EditorController;
import win.demistorm.mcpositioneditor.editor.EditorRenderContext;
import win.demistorm.mcpositioneditor.editor.GizmoRenderer;

// Tag which model is being resolved so ModelRenderPropertiesMixin knows whose transform to override
@Mixin(BlockModelWrapper.class)
public abstract class BlockModelWrapperMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void mcpositioneditor$beginModel(ItemStackRenderState itemStackRenderState, ItemStack itemStack,
                                             ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext,
                                             @Nullable ClientLevel clientLevel, @Nullable ItemOwner itemOwner, int i,
                                             CallbackInfo ci) {
        Identifier id =
            EditorRenderContext.idOf((BlockModelWrapper) (Object) this);
        EditorRenderContext.beginModel(id, itemStack, itemDisplayContext);
        if (EditorController.isSessionOpen()) {
            GizmoRenderer.noteModelQuads(
                id, ((BlockModelWrapperAccessor) (Object) this).mcpositioneditor$quads());
        }
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void mcpositioneditor$endModel(ItemStackRenderState itemStackRenderState, ItemStack itemStack,
                                           ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext,
                                           @Nullable ClientLevel clientLevel, @Nullable ItemOwner itemOwner, int i,
                                           CallbackInfo ci) {
        EditorRenderContext.endModel();
    }
}
