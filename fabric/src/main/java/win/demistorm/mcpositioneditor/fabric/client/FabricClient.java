package win.demistorm.mcpositioneditor.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
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
            dispatcher.register(ClientCommands.literal("eip")
                .then(ClientCommands.literal("edit").executes(ctx -> Commands.edit()))
                .then(ClientCommands.literal("export").executes(ctx -> Commands.export()))
                .then(ClientCommands.literal("clear").executes(ctx -> Commands.clear()))));
    }
}
