package win.demistorm.mcpositioneditor.neoforge;

import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.ModLoadingContext;
import win.demistorm.mcpositioneditor.client.config.ConfigScreen;

// NeoForge config screen registration
public final class NeoForgeConfigScreen {

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
            () -> (minecraft, screen) -> ConfigScreen.create(screen));
    }

    private NeoForgeConfigScreen() {}
}
