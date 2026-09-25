package win.demistorm.mcpositioneditor.fabric.client;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import win.demistorm.mcpositioneditor.client.config.ConfigScreen;

// ModMenu integration for Fabric
public class FabricConfigScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::create;
    }
}
