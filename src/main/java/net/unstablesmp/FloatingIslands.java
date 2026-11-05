package net.unstablesmp;

/**
 * Floating Zone islands. Pure math (no Minecraft classes): every column asks
 * "is there an island here, and from which Y to which Y?" using only the seed and
 * its X/Z, so chunks always line up no matter what order they load in.
 *
 * The world is cut into CELL x CELL squares. Each square may hold one island with a
 * hashed centre, radius, altitude and thickness. Islands have a flat-ish top and a
 * tapered underside, and some columns hang a long chain from the underside.
 */
public final class FloatingIslands {
    private FloatingIslands() {}

    // ---- tuning ----
    /** Size of one island cell. Must stay > 2 * MAX_RADIUS so a 3x3 cell check is enough. */
    private static final int CELL = 96;
    /** Chance a cell has an island once the zone is fully ramped up. */
    private static final double MAX_CHANCE = 0.80;
    private static final int MIN_RADIUS = 14, MAX_RADIUS = 46;
    /** Island altitude (Y of the island's widest layer). */
    private static final int MIN_Y = 150, MAX_Y = 250;
    /** How far the underside hangs below the altitude at the island centre. */
    private static final int MIN_DEPTH = 10, MAX_DEPTH = 38;
    /** How far the top rises above the altitude at the island centre. */
    private static final int DOME = 3;
    /** Chance that an island column hangs a chain, and how long chains get. */
    private static final double CHAIN_CHANCE = 0.006;
    private static final int MIN_CHAIN = 8, MAX_CHAIN = 80;

    private static final long SALT_ISLAND = 0x51F15EEDCAFE1234L;
    private static final long SALT_CHAIN = 0x3C6EF372FE94F82BL;

    /** Y range of an island at one column. bottom <= top. */
    public record Column(int top, int bottom, double edge) {}

    /**
     * @param density 0..1, how many islands exist (ramps up from the zone's start)
     * @return the island column here, or null if the sky is empty
     */
    public static Column column(long seed, int x, int z, double density) {
        if (density <= 0) return null;

        int cx0 = Math.floorDiv(x, CELL);
        int cz0 = Math.floorDiv(z, CELL);

        Column best = null;
        double bestU = 0;

        for (int cx = cx0 - 1; cx <= cx0 + 1; cx++) {
            for (int cz = cz0 - 1; cz <= cz0 + 1; cz++) {
                if (FarLandsNoise.hash(seed ^ SALT_ISLAND, cx, cz, 1) >= MAX_CHANCE * density) continue;

                double centerX = cx * (double) CELL + FarLandsNoise.hash(seed ^ SALT_ISLAND, cx, cz, 2) * CELL;
                double centerZ = cz * (double) CELL + FarLandsNoise.hash(seed ^ SALT_ISLAND, cx, cz, 3) * CELL;
                double radius = MIN_RADIUS + FarLandsNoise.hash(seed ^ SALT_ISLAND, cx, cz, 4) * (MAX_RADIUS - MIN_RADIUS);

                double dx = x + 0.5 - centerX;
                double dz = z + 0.5 - centerZ;
                double t = Math.sqrt(dx * dx + dz * dz) / radius;
                if (t >= 1.0) continue;

                double u = 1.0 - t * t; // 1 at the centre, 0 at the rim
                if (u <= bestU) continue;

                int altitude = MIN_Y + (int) (FarLandsNoise.hash(seed ^ SALT_ISLAND, cx, cz, 5) * (MAX_Y - MIN_Y));
                int depth = MIN_DEPTH + (int) (FarLandsNoise.hash(seed ^ SALT_ISLAND, cx, cz, 6) * (MAX_DEPTH - MIN_DEPTH));

                int top = altitude + (int) Math.round(u * DOME);
                int bottom = altitude - (int) Math.round(u * depth);
                bestU = u;
                best = new Column(top, bottom, t);
            }
        }
        return best;
    }

    /** Length of the chain hanging from the island at this column, or 0 for none. */
    public static int chainLength(long seed, int x, int z) {
        if (FarLandsNoise.hash(seed ^ SALT_CHAIN, x, z, 1) >= CHAIN_CHANCE) return 0;
        return MIN_CHAIN + (int) (FarLandsNoise.hash(seed ^ SALT_CHAIN, x, z, 2) * (MAX_CHAIN - MIN_CHAIN));
    }

    /** 0..1 roll used to pick which chain variant (copper / oxidized) a column gets. */
    public static double chainRoll(long seed, int x, int z) {
        return FarLandsNoise.hash(seed ^ SALT_CHAIN, x, z, 3);
    }

    /** True if the underside block here should be moss instead of stone. */
    public static boolean mossy(long seed, int x, int y, int z) {
        return FarLandsNoise.hash(seed ^ SALT_ISLAND, Math.floorDiv(x, 3), y, Math.floorDiv(z, 3)) < 0.22;
    }
}
