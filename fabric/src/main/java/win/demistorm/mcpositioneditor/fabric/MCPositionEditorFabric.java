package win.demistorm.mcpositioneditor.fabric;

import net.fabricmc.api.ModInitializer;
import win.demistorm.mcpositioneditor.MCPositionEditor;

public final class MCPositionEditorFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MCPositionEditor.initialize();
    }
}
