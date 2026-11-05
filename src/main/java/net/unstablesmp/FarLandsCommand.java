package net.unstablesmp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * /farlands          -> Start Zone
 * /farlands start    -> Start Zone
 * /farlands floating -> Floating Zone
 *
 * Players land high up with slow falling so they settle onto the terrain instead of
 * taking fall damage or spawning inside an island.
 */
public final class FarLandsCommand {
    private FarLandsCommand() {}

    private static final double START_X = FarLands.EDGE + 100;
    private static final double START_Y = 200;
    private static final double FLOATING_X = FarLands.EDGE + FarLands.WALL_DEPTH + 1_500;
    private static final double FLOATING_Y = 300; // islands top out around Y 253
    private static final int SLOW_FALL_TICKS = 20 * 30;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("farlands")
                .executes(ctx -> go(ctx, "Start Zone", START_X, START_Y))
                .then(CommandManager.literal("start")
                        .executes(ctx -> go(ctx, "Start Zone", START_X, START_Y)))
                .then(CommandManager.literal("floating")
                        .executes(ctx -> go(ctx, "Floating Zone", FLOATING_X, FLOATING_Y))));
    }

    private static int go(CommandContext<ServerCommandSource> ctx, String name, double x, double y)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();

        if (source.getWorld().getRegistryKey() != World.OVERWORLD) {
            source.sendError(Text.literal("The Far Lands are in the Overworld. Go there first."));
            return 0;
        }

        player.requestTeleport(x, y, 0.0);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, SLOW_FALL_TICKS));
        source.sendFeedback(() -> Text.literal("Teleported to the Far Lands: " + name), false);
        return 1;
    }
}
