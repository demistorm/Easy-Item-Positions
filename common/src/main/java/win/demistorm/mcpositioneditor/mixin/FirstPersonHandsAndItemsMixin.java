package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.editor.EditorRenderContext;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void mcpositioneditor$beginHandExtract(LocalPlayer player, float partialTicks,
                                                   FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        EditorRenderContext.pushHandPath();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void mcpositioneditor$endHandExtract(LocalPlayer player, float partialTicks,
                                                 FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        EditorRenderContext.popHandPath();
    }
}
