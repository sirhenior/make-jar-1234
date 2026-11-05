# Unstable SMP - Far Lands

Past 12,550,820 blocks from 0,0 (X or Z), newly loaded chunks become Far Lands: a jagged wall for ~19k blocks, then a flat striped grass plateau (rare snow + dirt patches). Tuning: FarLandsTop.java.

**Floating Zone** (12,570,000 to 12,575,000): the plateau with floating grass islands above it and copper/oxidized copper chains hanging from some of them. Islands fade in over the first 1,500 blocks. Tuning: `FloatingIslands.java`.

## Build the jar
**GitHub (no setup):** push this folder to a GitHub repo, open Actions > build > latest run > download the `unstable-smp` artifact. Use the jar WITHOUT `-sources`.

**Locally:** install Java 21 and Gradle 9.2+, then run `gradle build`. Jar is in `build/libs/`.

## Install
Put the jar and Fabric API (0.141.3+1.21.11) in `mods/`. Players do not need to install anything on a dedicated server.

## Test
`/farlands` (Start Zone) or `/farlands floating` (Floating Zone). Lands you high up with 30s of slow falling.

Manual coordinates:
`/tp @s 12550900 120 0` (Start Zone) or `/tp @s 12571000 200 0` (Floating Zone) (or any coordinate past 12,550,820 on X or Z).
Tuning constants: `FarLands.java` (EDGE, VOID_DISTANCE) and `FarLandsNoise.java` (frequencies, threshold).
