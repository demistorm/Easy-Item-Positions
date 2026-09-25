package win.demistorm.mcpositioneditor.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.mcpositioneditor.client.MCPositionEditorClient;
import win.demistorm.mcpositioneditor.editor.OverrideStorage;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void mcpositioneditor$onClientTick(CallbackInfo ci) {
        MCPositionEditorClient.onClientTick((Minecraft) (Object) this);
    }

    @Inject(method = "stop", at = @At("TAIL"))
    private void mcpositioneditor$onStop(CallbackInfo ci) {
        OverrideStorage.flush();
    }
}
