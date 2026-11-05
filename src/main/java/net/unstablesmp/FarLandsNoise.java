package net.unstablesmp;

/**
 * Value noise tuned to look like the old Far Lands: along the "far" axis the noise
 * barely changes (so terrain is extruded into enormous walls), while the other two
 * axes change fast (so the walls are jagged and shard-like).
 */
public final class FarLandsNoise {
    private FarLandsNoise() {}

    /** Positive = solid block. */
    public static double density(long seed, int x, int y, int z, boolean xFar, double corruption) {
        double fx = xFar ? 0.0009 : 0.045;
        double fz = xFar ? 0.045 : 0.0009;
        double fy = 0.12;

        double n = fbm(seed, x * fx, y * fy, z * fz);   // roughly 0..1
        double heightBias = (64.0 - y) / 160.0;          // lower = more solid
        double threshold = 0.5 + corruption * 0.45;      // more void further out
        return n + heightBias - threshold;
    }

    private static double fbm(long seed, double x, double y, double z) {
        double sum = 0, amp = 1, norm = 0;
        for (int i = 0; i < 4; i++) {
            sum += amp * value(seed + i * 0x9E3779B97F4A7C15L, x, y, z);
            norm += amp;
            x *= 2; y *= 2; z *= 2;
            amp *= 0.5;
        }
        return sum / norm;
    }

    private static double value(long seed, double x, double y, double z) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
        double tx = smooth(x - x0), ty = smooth(y - y0), tz = smooth(z - z0);

        double c000 = hash(seed, x0, y0, z0),         c100 = hash(seed, x0 + 1, y0, z0);
        double c010 = hash(seed, x0, y0 + 1, z0),     c110 = hash(seed, x0 + 1, y0 + 1, z0);
        double c001 = hash(seed, x0, y0, z0 + 1),     c101 = hash(seed, x0 + 1, y0, z0 + 1);
        double c011 = hash(seed, x0, y0 + 1, z0 + 1), c111 = hash(seed, x0 + 1, y0 + 1, z0 + 1);

        double x00 = lerp(tx, c000, c100), x10 = lerp(tx, c010, c110);
        double x01 = lerp(tx, c001, c101), x11 = lerp(tx, c011, c111);
        return lerp(tz, lerp(ty, x00, x10), lerp(ty, x01, x11));
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }

    /** Deterministic hash to [0, 1). */
    public static double hash(long seed, int x, int y, int z) {
        long h = seed ^ (x * 0x632BE59BD9B4E019L) ^ (y * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * (1.0 / (1L << 53));
    }
}
