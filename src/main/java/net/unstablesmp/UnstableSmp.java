package net.unstablesmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

public class UnstableSmp implements ModInitializer {
    public static final String MOD_ID = "unstablesmp";

    @Override
    public void onInitialize() {
        ServerChunkEvents.CHUNK_LOAD.register(FarLands::onChunkLoad);
        CommandRegistrationCallback.EVENT.register(FarLandsCommand::register);
    }
}
