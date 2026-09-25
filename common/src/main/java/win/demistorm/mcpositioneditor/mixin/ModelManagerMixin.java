package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import win.demistorm.mcpositioneditor.editor.TransformOverrideManager;

import java.util.Map;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin {

    @Inject(method = "discoverModelDependencies", at = @At("HEAD"))
    private static void mcpositioneditor$captureMetadata(Map<Identifier, UnbakedModel> map,
                                                         BlockStateModelLoader.LoadedModels loadedModels,
                                                         ClientItemInfoLoader.LoadedClientInfos loadedClientInfos,
                                                         CallbackInfoReturnable<?> cir) {
        TransformOverrideManager.captureMetadata(map, loadedClientInfos);
    }
}
