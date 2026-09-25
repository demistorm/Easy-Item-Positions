package win.demistorm.mcpositioneditor.forge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import win.demistorm.mcpositioneditor.client.commands.Commands;
import win.demistorm.mcpositioneditor.MCPositionEditor;
import win.demistorm.mcpositioneditor.client.MCPositionEditorClient;

// Forge client setup (keymappings and the /eip client command)
@Mod.EventBusSubscriber(modid = MCPositionEditor.MOD_ID, value = Dist.CLIENT)
public final class ForgeClient {

    private ForgeClient() {}

    public static void doClientSetup() {
        ForgeEditorKeys.install();
        MCPositionEditorClient.initializeClient();
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ForgeEditorKeys.TOGGLE);
        event.register(ForgeEditorKeys.GRAB);
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
