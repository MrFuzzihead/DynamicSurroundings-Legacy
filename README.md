# Dynamic Surroundings

[![](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![](https://img.shields.io/badge/UniMixins-required-2ea44f)](https://github.com/GTNewHorizons/UniMixins)

A 1.7.10 Forge port of [Dynamic Surroundings](https://github.com/OreCruncher/DynamicSurroundingsFabric): a client-side
mod that alters the fabric of the Minecraft experience by weaving a tapestry of sound and visual effects — biome
soundscapes, footsteps, storms, fog, auroras, and more. It is 100% client side; no server deployment is needed.

Originally written by OreCruncher; this fork keeps the 1.7.10 edition building and running on the GTNH toolchain, with
explicit support for [Angelica](https://github.com/GTNewHorizons/Angelica), NotFine and Iris.

## What it adds

| Kind | Effects |
|---|---|
| Sound | seamless biome soundscapes, footstep acoustics driven by the block underfoot, cave reverb, water and waterfall ambience, block-triggered sounds |
| Weather | client-side rain, snow and desert dust, storm intensity, splash and rain-on-surface sounds, an explicit defer policy for Better Rain / Weather2 |
| Sky | aurora (three styles, including cinematic), biome/time/weather-driven sky fog, morning fog, elevation haze |
| Air | steam and fire jets, misting waterfalls, foam and rings on water |
| HUD | potion HUD with cached text, F3 diagnostics line, optional Waila plugin |
| Misc | text pop-offs, dynamic block effect sampling, `/rain` commands and `/rain reload` |

All of it is configurable, and the weather/fog renderers compose with Angelica's instanced weather and NotFine's fog
settings instead of overwriting them.

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.7.10 |
| Minecraft Forge | 10.13.4.1614 |
| [UniMixins](https://github.com/GTNewHorizons/UniMixins) | required (mixins, early coremod) |
| [Angelica](https://github.com/GTNewHorizons/Angelica) / NotFine / Iris | optional, detected at runtime |
| [Biomes O' Plenty](https://www.curseforge.com/minecraft/mc-mods/biomes-o-plenty) | optional, biome fog colours |
| CalendarAPI, Waila | optional |

The mod ships Java 8 bytecode; Gradle needs a JDK 25 daemon, while the unit tests run on a Java 8 launcher to match
the mod's target.

## Building

Everything is handled by the `com.gtnewhorizons.gtnhconvention` plugin:

```sh
./gradlew build              # compile, unit tests, format check, coverage gate
./gradlew test               # unit tests only
./gradlew jacocoTestReport   # HTML coverage -> build/reports/jacoco/test/html
./gradlew runClient          # dev client (forwards to runClient25)
./gradlew runServer          # dev server (forwards to runServer25)
```

### Tests

| Command | What it runs |
|---|---|
| `./gradlew test` | 334 headless JUnit 4 tests across 83 suites (JUnit 4.13.2 + Mockito 4.11.0, Java 8 launcher) |
| `./gradlew check` | the above plus Spotless, Checkstyle, the JaCoCo coverage floor and the gameTest compile |
| `./gradlew gameTestServer` | boots a real dedicated server, runs 79 in-game assertions, then exits with the result. Also writes an in-game coverage report to `build/reports/jacoco/gameTest/html` |
| `./gradlew clientSmokeTest` | launches the dev client, runs the smoke assertions (weather owner, GL fog state, aurora frames), then exits |

The in-game mod lives in `src/gameTest/java` and is never shipped. It covers what cannot be tested headless: block and
biome registration against the real registries, the packet round trips, NBT persistence, the server-side mixins, and —
in the client smoke run — the Angelica/NotFine ownership and fog contracts.

`docs/CODEBASE-ANALYSIS.md` records the compatibility analysis, the bug/optimization/refactor backlog (every P0–P3 item
is closed, with the deliberately-deferred ones and their reasons listed), the phased refactor plan and the
test-coverage ratchet.

### Configuration

`config/dynamicsurroundings/dynamicsurroundings.cfg`, generated on first launch. The notable categories are `rain`
(weather effects, particle count, sound level, desert dust), `fog` (biome/weather/haze fog, respect external fog),
`sound` (biome sounds, footsteps, block effects) and `compatibility` (`Defer To Better Rain`, `Defer To Weather2` for
letting another weather mod own the frame).

## Contributing

Bug reports and pull requests are welcome. Please read `docs/CODEBASE-ANALYSIS.md` before structural changes: it
records the known limitations and the ordered refactor plan.

If a bug looks like it is inside Angelica, NotFine or Iris rather than this mod, please report it there; the
compatibility layer here is deliberately guarded and typed so the blame is easy to place.

## License

See [LICENSE](LICENSE). Dynamic Surroundings was created by OreCruncher and the legacy edition remains under the same
terms.

## Credit

Dynamic Surroundings was created by OreCruncher. Wirsbo for BetterRain, MrRube for Aurora Rubealis, and Hurricaaane for
Presence Footsteps, whose work the sound and sky effects descend from. This fork is maintained by MrFuzzihead for the
GTNH pack.
