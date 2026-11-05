package net.unstablesmp;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Rewrites chunks past EDGE blocks from 0,0 (on either axis) into Far Lands terrain.
 * Already-converted chunks are recognised by a reinforced deepslate block in their
 * bottom-corner column, so player changes are never overwritten on later loads.
 *
 * Zones so far:
 *  - WALL:     first WALL_DEPTH blocks past the edge, jagged extruded shards (Start Zone).
 *  - FLOATING: the flat striped plateau with floating islands and hanging chains above it
 *              (see FloatingIslands). Starts at 12,570,000, islands fade in over RAMP blocks.
 *  - TOP:      everything after, the plain plateau (placeholder until the next zone is built).
 */
public final class FarLands {
    /** Where the Far Lands begin, in blocks from 0,0 on the X or Z axis. */
    public static final int EDGE = 12_550_820;
    /** Blocks past the edge where the wall ends and the top begins (Floating zone starts at 12,570,000). */
    public static final int WALL_DEPTH = 19_180;
    /** Blocks past the edge where the Floating Zone ends (Spikes zone starts at 12,575,000). */
    public static final int FLOATING_END = 24_180;
    /** Islands fade in over this many blocks so the plateau transitions into the zone. */
    private static final int RAMP = 1_500;
    /** Above this Y the wall terrain is always air. */
    private static final int MAX_WALL_Y = 144;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState STONE = Blocks.STONE.getDefaultState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.getDefaultState();
    private static final BlockState DIRT = Blocks.DIRT.getDefaultState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.getDefaultState();
    private static final BlockState SENTINEL = Blocks.REINFORCED_DEEPSLATE.getDefaultState();
    private static final BlockState MOSS = Blocks.MOSS_BLOCK.getDefaultState();
    // Looked up by id so this compiles regardless of how the chain blocks are named in the mappings.
    private static final BlockState CHAIN_COPPER = blockById("copper_chain");
    private static final BlockState CHAIN_OXIDIZED = blockById("oxidized_copper_chain");

    private FarLands() {}

    public static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        if (world.getRegistryKey() != World.OVERWORLD) return;

        ChunkPos cp = chunk.getPos();
        int startX = cp.getStartX();
        int startZ = cp.getStartZ();

        int farthest = Math.max(
                Math.max(Math.abs(startX), Math.abs(startX + 15)),
                Math.max(Math.abs(startZ), Math.abs(startZ + 15)));
        if (farthest < EDGE) return;

        int bottomY = world.getBottomY();
        int topY = bottomY + world.getHeight(); // exclusive

        BlockPos.Mutable pos = new BlockPos.Mutable();

        if (chunk.getBlockState(pos.set(startX, bottomY, startZ)).isOf(Blocks.REINFORCED_DEEPSLATE)) return;

        for (BlockPos bePos : new java.util.ArrayList<>(chunk.getBlockEntityPositions())) {
            chunk.removeBlockEntity(bePos);
        }

        long seed = world.getSeed();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = startX + lx;
                int z = startZ + lz;
                int ax = Math.abs(x);
                int az = Math.abs(z);
                int past = Math.max(ax, az) - EDGE;
                if (past < 0) continue;

                boolean xFar = ax >= az;
                if (past >= FLOATING_END) {
                    fillTop(chunk, pos, seed, x, z, xFar, bottomY, topY);
                } else if (past >= WALL_DEPTH) {
                    fillFloating(chunk, pos, seed, x, z, xFar, bottomY, topY, past - WALL_DEPTH);
                } else {
                    fillWall(chunk, pos, seed, x, z, xFar, bottomY, topY);
                }
            }
        }

        chunk.setBlockState(pos.set(startX, bottomY, startZ), SENTINEL, 0);
    }

    private static void fillTop(WorldChunk chunk, BlockPos.Mutable pos, long seed,
                                int x, int z, boolean xFar, int bottomY, int topY) {
        int surface = FarLandsTop.surfaceY(seed, x, z, xFar);
        BlockState top = FarLandsTop.topBlock(seed, x, z, xFar);

        for (int y = topY - 1; y > bottomY; y--) {
            BlockState target;
            if (y > surface) target = AIR;
            else if (y == surface) target = top;
            else if (y >= surface - 3) target = DIRT;
            else target = y < 0 ? DEEPSLATE : STONE;
            place(chunk, pos, x, y, z, target);
        }
    }

    /** Plateau on the ground, floating islands above it, chains hanging from some islands. */
    private static void fillFloating(WorldChunk chunk, BlockPos.Mutable pos, long seed,
                                     int x, int z, boolean xFar, int bottomY, int topY, int into) {
        int surface = FarLandsTop.surfaceY(seed, x, z, xFar);
        BlockState top = FarLandsTop.topBlock(seed, x, z, xFar);

        double density = Math.min(1.0, into / (double) RAMP);
        FloatingIslands.Column island = FloatingIslands.column(seed, x, z, density);

        int chainBottom = Integer.MIN_VALUE; // lowest Y of this column's chain (if any)
        BlockState chain = null;
        if (island != null) {
            int len = FloatingIslands.chainLength(seed, x, z);
            if (len > 0) {
                chainBottom = Math.max(island.bottom() - len, surface + 1);
                chain = FloatingIslands.chainRoll(seed, x, z) < 0.5 ? CHAIN_COPPER : CHAIN_OXIDIZED;
            }
        }

        for (int y = topY - 1; y > bottomY; y--) {
            BlockState target;
            if (y > surface) target = AIR;
            else if (y == surface) target = top;
            else if (y >= surface - 3) target = DIRT;
            else target = y < 0 ? DEEPSLATE : STONE;

            if (island != null) {
                if (y <= island.top() && y >= island.bottom()) {
                    if (y == island.top()) target = GRASS;
                    else if (y >= island.top() - 3) target = DIRT;
                    else if (y <= island.bottom() + 1 && FloatingIslands.mossy(seed, x, y, z)) target = MOSS;
                    else target = STONE;
                } else if (chain != null && y < island.bottom() && y >= chainBottom) {
                    target = chain;
                }
            }
            place(chunk, pos, x, y, z, target);
        }
    }

    private static BlockState blockById(String path) {
        return Registries.BLOCK.get(Identifier.of("minecraft", path)).getDefaultState();
    }

    private static void fillWall(WorldChunk chunk, BlockPos.Mutable pos, long seed,
                                 int x, int z, boolean xFar, int bottomY, int topY) {
        boolean airAbove = true;
        int depth = 0;

        for (int y = topY - 1; y > bottomY; y--) {
            boolean solid = y <= MAX_WALL_Y
                    && FarLandsNoise.density(seed, x, y, z, xFar, 0.0) > 0;

            BlockState target;
            if (!solid) {
                target = AIR;
                airAbove = true;
                depth = 0;
            } else {
                if (airAbove) depth = 0; else depth++;
                airAbove = false;
                if (depth == 0) target = GRASS;
                else if (depth <= 3) target = DIRT;
                else target = y < 0 ? DEEPSLATE : STONE;
            }
            place(chunk, pos, x, y, z, target);
        }
    }

    private static void place(WorldChunk chunk, BlockPos.Mutable pos, int x, int y, int z, BlockState target) {
        pos.set(x, y, z);
        if (chunk.getBlockState(pos) != target) {
            chunk.setBlockState(pos, target, 0);
        }
    }
}
