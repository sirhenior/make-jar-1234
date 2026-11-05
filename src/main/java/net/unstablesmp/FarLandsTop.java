package net.unstablesmp;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/**
 * The flat "top" of the Far Lands: long parallel stripes running along the far axis,
 * stepped up and down by a few blocks, almost all grass with a rare small snow patch.
 */
public final class FarLandsTop {
    private FarLandsTop() {}

    // ---- tuning ----
    /** Average height of the plateau. */
    public static final int BASE_Y = 110;
    /** How fast stripes change across their width (higher = narrower stripes). */
    private static final double ACROSS_FREQ = 0.15;
    /** How fast stripes drift along their length (lower = longer stripes). */
    private static final double ALONG_FREQ = 0.0006;
    /** Total step range of the stripes in blocks (+/- half of this). */
    private static final double STEP_RANGE = 6.0;
    /** Chance that a patch is snow instead of grass (very rare: one or two glitched patches). */
    private static final double SNOW_CHANCE = 0.0003;
    /** Dirt patches are gone: the top is grass with the odd snow patch. */
    private static final double DIRT_CHANCE = 0.0;
    /** Patch size: width across the stripes, length along them. */
    private static final int PATCH_ACROSS = 3;
    private static final int PATCH_ALONG = 3;

    private static final BlockState GRASS = Blocks.GRASS_BLOCK.getDefaultState();
    private static final BlockState DIRT = Blocks.DIRT.getDefaultState();
    private static final BlockState SNOW = Blocks.SNOW_BLOCK.getDefaultState();

    public static int surfaceY(long seed, int x, int z, boolean xFar) {
        int across = xFar ? z : x;
        int along = xFar ? x : z;
        double n = value2(seed, across * ACROSS_FREQ, along * ALONG_FREQ) * 0.75
                + value2(seed ^ 0x2545F4914F6CDD1DL, across * ACROSS_FREQ * 3.0, along * ALONG_FREQ * 4.0) * 0.25;
        return BASE_Y + (int) Math.floor((n - 0.5) * STEP_RANGE);
    }

    public static BlockState topBlock(long seed, int x, int z, boolean xFar) {
        int across = xFar ? z : x;
        int along = xFar ? x : z;
        double r = FarLandsNoise.hash(seed ^ 0x7A3B1C5D9E2F4A61L,
                Math.floorDiv(across, PATCH_ACROSS), Math.floorDiv(along, PATCH_ALONG), 7);
        if (r < SNOW_CHANCE) return SNOW;
        if (r < SNOW_CHANCE + DIRT_CHANCE) return DIRT;
        return GRASS;
    }

    private static double value2(long seed, double a, double b) {
        int a0 = (int) Math.floor(a), b0 = (int) Math.floor(b);
        double ta = smooth(a - a0), tb = smooth(b - b0);
        double c00 = FarLandsNoise.hash(seed, a0, b0, 0), c10 = FarLandsNoise.hash(seed, a0 + 1, b0, 0);
        double c01 = FarLandsNoise.hash(seed, a0, b0 + 1, 0), c11 = FarLandsNoise.hash(seed, a0 + 1, b0 + 1, 0);
        double top = c00 + ta * (c10 - c00);
        double bot = c01 + ta * (c11 - c01);
        return top + tb * (bot - top);
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }
}
