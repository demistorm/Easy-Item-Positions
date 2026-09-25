package win.demistorm.mcpositioneditor.client;

// Synthetic gun angle handed to VivecraftItemRenderingMixin (mixin members can't be called from mod code)
public final class VRPreviewState {

    private VRPreviewState() {}

    private static volatile Float gunAngleOverride;

    public static void setGunAngleOverride(Float degrees) {
        gunAngleOverride = degrees;
    }

    public static Float gunAngleOverride() {
        return gunAngleOverride;
    }
}
