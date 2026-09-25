package win.demistorm.mcpositioneditor.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import win.demistorm.mcpositioneditor.client.commands.Commands;
import win.demistorm.mcpositioneditor.client.MCPositionEditorClient;
import win.demistorm.mcpositioneditor.fabric.FabricEditorKeys;

public final class FabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricEditorKeys.register();
        MCPositionEditorClient.initializeClient();

        // /eip commands registration
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("eip")
                .then(ClientCommandManager.literal("edit").executes(ctx -> Commands.edit()))
                .then(ClientCommandManager.literal("export").executes(ctx -> Commands.export()))
                .then(ClientCommandManager.literal("clear").executes(ctx -> Commands.clear()))));
    }
}
