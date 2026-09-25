package win.demistorm.mcpositioneditor.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import win.demistorm.mcpositioneditor.MCPositionEditor;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

@Mod(MCPositionEditor.MOD_ID)
public final class MCPositionEditorNeoForge {

    public MCPositionEditorNeoForge(IEventBus modEventBus) {
        log.info("Easy Item Positions (NEOFORGE) starting!");
        MCPositionEditor.initialize();

        if (FMLEnvironment.getDist().isClient()) {
            NeoClient.doClientSetup();
            NeoForgeConfigScreen.register();
        }

        log.info("Easy Item Positions (NEOFORGE) initialization complete!");
    }
}
