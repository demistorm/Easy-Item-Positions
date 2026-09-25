package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.editor.EditorController;
import win.demistorm.mcpositioneditor.editor.EditorRenderContext;

// Point where a display transform lands on the render layer (overrides here preview instantly everywhere)
@Mixin(ModelRenderProperties.class)
public abstract class ModelRenderPropertiesMixin {

    @Shadow
    public abstract boolean usesBlockLight();

    @Shadow
    public abstract TextureAtlasSprite particleIcon();

    @Inject(method = "applyToLayer", at = @At("HEAD"), cancellable = true)
    private void mcpositioneditor$applyOverride(ItemStackRenderState.LayerRenderState layer,
                                                ItemDisplayContext itemDisplayContext,
                                                CallbackInfo ci) {
        Identifier modelId = EditorRenderContext.currentModel();
        if (modelId == null) {
            return;
        }
        ItemTransform override = EditorController.resolveLive(modelId, itemDisplayContext);
        if (override != null) {
            layer.setUsesBlockLight(this.usesBlockLight());
            layer.setParticleIcon(this.particleIcon());
            layer.setTransform(override);
            ci.cancel();
        }
    }
}
