package win.demistorm.mcpositioneditor.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import win.demistorm.mcpositioneditor.client.commands.Commands;
import win.demistorm.mcpositioneditor.MCPositionEditor;
import win.demistorm.mcpositioneditor.client.MCPositionEditorClient;

// NeoForge client setup (keymappings and the /eip client command)
@EventBusSubscriber(modid = MCPositionEditor.MOD_ID, value = Dist.CLIENT)
public final class NeoClient {

    private NeoClient() {}

    public static void doClientSetup() {
        NeoForgeEditorKeys.install();
        MCPositionEditorClient.initializeClient();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(NeoForgeEditorKeys.TOGGLE);
        event.register(NeoForgeEditorKeys.GRAB);
    }

    // Register /eip commands
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(net.minecraft.commands.Commands.literal("eip")
            .then(net.minecraft.commands.Commands.literal("edit").executes(ctx -> Commands.edit()))
            .then(net.minecraft.commands.Commands.literal("export").executes(ctx -> Commands.export()))
            .then(net.minecraft.commands.Commands.literal("clear").executes(ctx -> Commands.clear())));
    }
}
