package win.demistorm.mcpositioneditor.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import win.demistorm.mcpositioneditor.MCPositionEditor;

import static win.demistorm.mcpositioneditor.MCPositionEditor.log;

@Mod(MCPositionEditor.MOD_ID)
public final class MCPositionEditorForge {

    public MCPositionEditorForge() {
        log.info("Easy Item Positions (FORGE) starting!");
        MCPositionEditor.initialize();

        if (FMLEnvironment.dist.isClient()) {
            ForgeClient.doClientSetup();
        }

        log.info("Easy Item Positions (FORGE) initialization complete!");
    }
}
