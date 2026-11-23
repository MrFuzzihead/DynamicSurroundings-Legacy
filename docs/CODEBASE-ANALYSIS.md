# Dynamic Surroundings (Legacy) — codebase review & compatibility plan

**Branch:** `refactor/angelica-notfine-compat`
**Scope:** analysis + the implementation log below. Batches 1–62 are merged into the branch (weather/aurora, relative fog, client-thread safety, NotFine/Angelica compatibility, text-pop-off instancing, the compileOnly Angelica API, validation hardening, config/runtime reload, and the headless test expansion).
**Codebase:** 1.7.10 Forge port of Dynamic Surroundings, 177 production classes / ~14 k LOC, 81 test suites / 328 unit tests, plus a non-shipped in-game test mod.

This document is the working backlog for the refactor branch. It is ordered by risk: the compatibility work with **Angelica** and **NotFine** comes first because two of those conflicts are functional regressions today, not just polish.

---

## 0. Executive summary

### The five things to fix first

| # | Issue | Status | Why it matters |
|---|-------|--------|----------------|
| 1 | `EntityRendererMixin.renderRainSnow` no longer cancelled vanilla | **Fixed (batch 1)** | The pre-mixin transformer inserted a `RETURN` after the DS renderer; the mixin converted that into a plain `HEAD` injection. Vanilla rain/snow rendered **on top of** DS rain, and a custom `IRenderHandler` was invoked **twice**. Both injectors now cancel only when DS owns the frame. |
| 2 | DS and Angelica both cancel `renderRainSnow` / `addRainParticles` at `HEAD` | **Fixed (batch 1)** | Which mod wins used to depend on mixin application order. `WeatherBackend` now has one owner per frame (external provider, Angelica instanced, DS, or vanilla); the DS injectors use `order = 900` and decline when they are not the owner. |
| 3 | Fog ranges are written as absolute values at `EventPriority.LOWEST` (`client/FogEffectHandler.java:76`) | **Fixed (batch 2)** | Overrides NotFine's `TERRAIN_FOG` / `FOG_NEAR_DISTANCE` settings (the exact cause of Angelica issue #2304) and any other mod's fog. Must be applied relative to the fog that vanilla/NotFine already produced. |
| 4 | `SoundManager.update()` is only driven by the biome-sound handler (`client/sound/SoundManager.java:88`) | **Fixed (batch 3)** | When biome sounds are disabled, pending block/step sounds never drain and emitters are never ticked. The handler that drives the queue must be registered unconditionally. |
| 5 | Client packets mutate render/particle state on the Netty thread (`PacketHealthChange`, `PacketAurora`, `PacketRainIntensity`) | **Fixed (batch 3)** | Off-thread `EffectRenderer.addEffect()` and render-state writes are a classic 1.7.10 crash/corruption source. |

### Implementation log

**Batch 1 — weather ownership and aurora lifecycle (172 tests green)**

- Added `compat/Compat`, `compat/angelica/AngelicaBridge` (reads `AngelicaConfig.enableInstancedWeather`) and `compat/notfine/NotFineBridge` (reads `SettingsManager.downfallDistance`). Both bridges are guarded, compileOnly-backed entry points: the optional type references live in small hook classes that are only loaded after the mod-id check, and degrade to the vanilla fallback when the mod is absent.
- Added `client/weather/WeatherBackend` with a pure, unit-tested `resolve(...)` matrix. DS cancels `renderRainSnow`/`addRainParticles` only when it is the owner; otherwise vanilla, an external `IRenderHandler`, or Angelica runs normally.
- Removed the second `world.provider.getWeatherRenderer()` call from `StormRenderer`; a provider is now rendered by exactly one caller.
- DS and splashes now use one effective radius from NotFine's `DOWNFALL_DISTANCE` (falling back to `fancyGraphics ? 10 : 5`). `0` disables downfall rendering.
- Splash dispatch uses `mc.theWorld.provider.dimensionId`, not `EnvironState` (which is `0` before the first tick).
- Moved the aurora out of the `RAIN_SNOW` phase and out of the `WEATHER` mixin gate: `AuroraRenderHandler` draws it from `RenderWorldLastEvent`. The draw is guarded by `auroraEnable` + `DimensionRegistry.hasAuroras` and wrapped in `glPushAttrib`/`glPopAttrib`, so it survives `disableWeatherEffects`, Angelica, and state leaks.
- Fixed aurora identity/range: `AuroraData.hashCode`, `||` in the current-aurora comparison, `volatile currentAurora`, and the server range check now measures against the actual spawn target (`playerZ - offset`) instead of subtracting the offset twice.
- Added optional `after:` relations for Angelica/NotFine/Iris/BOP to `mcmod.info`; the F3 diagnostics now print the active `WeatherBackend.Owner`.

**Batch 2 — relative fog composition (180 tests green)**

- `FogResult` now stores **scale factors** (1.0 = leave the external fog untouched), not absolute distances. The handler reads the current `GL_FOG_START`/`GL_FOG_END` produced by vanilla/NotFine/Angelica and multiplies them, so NotFine's `FOG_NEAR_DISTANCE = 1 %` and `TERRAIN_FOG = false` survive DS fog.
- Added `fog.Respect External Fog` (default `true`) for users who want the old absolute behaviour. When false, the handler uses the vanilla `0.75 / 1.0` geometry as the base.
- Only the linear-fog event (`fogMode >= 0`) is modified; sky fog (`-1`) and the cloud/water/lava paths are left alone.
- `doFog()` now considers every fog option (biome, desert, elevation haze, morning, weather), not just the two biome flags.
- Fixed the morning-fog clamp so `start` can no longer exceed `end`; the `11`-block floor is now expressed in scale space.
- Added 8 fog unit tests: relative-scale semantics, clamping, mode mismatch, vanilla/weather/holistic calculators, and the new config default.

**Batch 3 — client-thread safety (180 tests green)**

- Added `network/ClientPacketHandler`, which hands packet bodies to `Minecraft.func_152344_a` when the handler is on the Netty thread and runs them directly on the client thread. All three packets (`PacketAurora`, `PacketHealthChange`, `PacketRainIntensity`) now mutate client state only from the client thread.
- `SoundManager.update()` moved from the conditional `PlayerSoundEffectHandler` to `ClientEffectHandler.clientTick`, so block/step sounds drain and emitters tick even with `Enable Biome Sounds` off. It runs once per tick, after all effect handlers.
- `DamageEffectHandler.handleEvent` now guards a null client world (possible between a scheduled packet and a world unload).

**Batch 4 — NotFine HUD and void-fog parity (181 tests green)**

- `NotFineBridge` now reads NotFine's `VOID_FOG` tick box directly through the compileOnly `Settings` API. `BiomeFogColorCalculator` skips its own void-fog darkening when the user disabled NotFine's void fog, while blindness/night vision still compose.
- `GuiHUDHandler` filters at the dispatcher: only the non-cancelable `EXPERIENCE` overlay pass is rendered. This is the pass HUDCaching rebuilds, and it avoids Pre/Post/Text/synthetic passes calling the potion list several times per frame.
- `PotionHUD` now null-guards `thePlayer` and wraps its draw in `glPushAttrib`/`glPopAttrib`, so it cannot leak lighting/texture/colour state into the HUD cache or vanilla.
- Still open from this theme: NotFine `CLOUD_HEIGHT` alignment for aurora/haze (DS dimension config currently wins) and per-effect text caching (P9).

**Batch 5 — Angelica `WeatherColumnProvider` bridge (182 tests green)**

- Added `compat/angelica/AngelicaWeatherProvider`. It detects Angelica's `WeatherColumnProvider` interface reflectively, creates a `Proxy` that implements both `IRenderHandler` and that interface, and installs it with `WorldProvider.setWeatherRenderer` on client world load. The previous provider is restored on unload.
- The provider exposes DS's per-biome rain/snow/dust columns to Angelica's instanced weather renderer (layer period/drift/fade, per-column bottom/top, lightmap, scroll and RGBA), mirroring the legacy `StormRenderer` rules. Without Angelica the install path is a no-op.
- `WeatherBackend` recognises the DS-owned provider, so with Angelica's instanced weather disabled the legacy DS renderer still owns the frame; with it enabled Angelica consumes the columns.
- Added a no-op test for the absent-Angelica path.

**Batch 6 — BOP fog, cloud height, Iris aurora phase (184 tests green)**

- Added `Compat.isAngelicaBopFogActive()`. When Angelica's late BOP fog mixin is present, `BiomeRegistry` no longer imports `IBiomeFog` into DS's own biome entries, so BOP colour/density is blended once (by Angelica) instead of twice.
- Added `NotFineBridge.cloudHeight(...)` and `DimensionRegistry` explicit-cloud-height tracking. Aurora height and elevation haze use NotFine's `CLOUD_HEIGHT` unless the dimension config explicitly overrides it.
- Added `compat/angelica/IrisPhaseBridge` (with a compileOnly `IrisPhaseHook`), a typed `WorldRenderingPhase` scope. `AuroraRenderHandler` now draws inside `WorldRenderingPhase.CLOUDS` when Iris is active and restores the previous phase in a `finally` block; without Iris it is a no-op.
- Added three tests: absent Angelica/BOP detection, absent Iris phase bridge, and the NotFine cloud-height fallback.

**Batch 7 — text pop-offs as a post-world overlay (185 tests green)**

- `EntityTextPopOffFX` is now a render-less marker: `renderParticle` does nothing, and the grow/shrink animation moved to `onUpdate`.
- Added `TextPopOffRenderer`, which draws active pop-offs from `RenderWorldLastEvent` with `glPushAttrib`/`glPopAttrib`, no depth-function mutation, and an Iris `PARTICLES` phase scope. It prunes dead/stale-world entries.
- This keeps the font out of Angelica's `ParticleInstancer` capture and out of Iris gbuffer programs.
- Added a unit test that pins the no-op `renderParticle` and the active-list lifecycle.

**Batch 8 — compileOnly Angelica API (185 tests green)**

- Added `compileOnly("com.github.GTNewHorizons:Angelica:2.2.46:dev") { transitive = false }`. The optional references are compile-time only and are not published or bundled.
- Replaced the Angelica weather proxy/reflection with a direct `IRenderHandler + WeatherColumnProvider` nested provider. `AngelicaWeatherProvider` is now a small installer with no Angelica types in its bytecode; `AngelicaWeatherProviderFactory` is only loaded after the mod-id check.
- Replaced the NotFine and Angelica-config reflection handles with guarded hook classes (`NotFineSettingsHook`, `AngelicaConfigHook`). Their direct optional types are loaded only when the corresponding mod id is present.
- Replaced the Iris phase reflection with typed `Iris`/`WorldRenderingPhase` access in `IrisPhaseHook`; `IrisPhaseBridge` only probes for the Iris class and remains loadable without it.
- This also fixed a latent bug in the old proxy: Forge's `IRenderHandler` is an abstract class, not an interface, so `Proxy.newProxyInstance` could never have created the weather provider.

**Batch 9 — Angelica debug-screen diagnostics (186 tests green)**

- Added `compat/angelica/AngelicaDebugEntries` + `AngelicaDebugHook`. When Angelica is present, DS registers a `SCREEN_TEXT` group/entry with `DebugScreenApi` and exposes the active `WeatherBackend.Owner`, dimension, biome, conditions, `Weather.diagnostic()`, and the effective NotFine downfall radius.
- The hook directly uses the compileOnly `api.debug` classes; the guard class has no Angelica types and is a no-op when Angelica is absent.
- This avoids the old `RenderGameOverlayEvent.Text` path for F3 integration and gives compatibility reports a single, versioned debug panel.

**Batch 10 — validation hardening (193 tests green)**

- Extracted the Angelica provider's column builder into `WeatherColumnCollector`, so it can be driven with a fake `WeatherColumnSink` without a client launch.
- Added `WeatherColumnCollectorTest`: rain layer period (32), snow layer period (512), column count/bounds/alpha, and null/zero-input no-ops.
- Extracted `FogEffectHandler.computeRange(...)` and added `FogEffectHandlerTest`: relative scaling preserves NotFine's near-fog base, the `TERRAIN_FOG=false` 16 M range stays effectively off, and empty/inverted/no-op ranges are rejected.
- Re-ran `gameTestServer` after the extraction: **65/65** dedicated-server checks still pass.

**Batch 11 — high-priority, headless-testable data fixes (200 tests green)**

- **B47:** `DimensionEffectData.randomizeRain` now uses `min + rand*(max-min)`. Tests cover a non-zero minimum (`[0.5, 1.0]`), the closed window, and the 1% floor.
- **B87:** `BlockPos` now has coordinate `equals`/`hashCode`; the copy constructor uses getters so `MutableBlockPos.getImmutable()` returns the live coordinates; both `getAllInBox` iterators now terminate and are tested.
- **B88:** `ImmutableColor` now rejects `scale(r,g,b)`, `add`, `blend`, and `mix(Color)` as well as the existing mutators; `Color` has a matching `hashCode`. The palette constants are asserted unchanged.

**Batch 12 — chance, weighted selection, and programmatic sound patterns (210 tests green)**

- Added `WeightedRandom` with guarded `roll`, `pickIndex`, and `pick`. Non-positive chances mean "never"; negative weights are ignored; empty/all-zero tables return no result instead of calling `nextInt(0)` or walking past index 0.
- Replaced the duplicated reverse-cumulative picks in `BiomeRegistry`, `BlockRegistry`, and `ProbabilityWeightsAcoustic`.
- `BlockEffect.trigger` and the block/biome chance checks now use `WeightedRandom.roll`, so a configured `chance = 0` no longer throws.
- `SoundEffect`'s programmatic constructors now compile the `.*` pattern instead of leaving `pattern = null`; `matches()` is safe for effects created in code.
- Added `WeightedRandomTest` (zero/negative chance, negative weights, distribution, empty candidates), `BlockEffectTest`, and `SoundEffectTest`.

**Batch 13 — packet width, config bounds, and double precision (211 tests green)**

- **B43:** `PacketAurora` now serializes `colorSet`/`preset` as `int` instead of `byte`, so future preset/colour IDs cannot silently truncate. The codec test pins the new width.
- **B51:** `ConfigProcessor` uses `-Float.MAX_VALUE` as the no-minimum sentinel. A new test proves an unannotated negative float survives instead of being clamped to `1.4e-45`.
- **B89 (partial):** `MathStuff.sqrt(double)` no longer casts to float; a 1e-15 precision test pins it. The trig/log approximations and absolute-value overflow cases remain in the backlog.

**Batch 14 — footstep delay bounds and wake-up minimum (213 tests green)**

- **B79:** `AcousticsManager.randomDelay` now samples `[a, b]` instead of `[a, a+b]` and clamps equal/inverted windows. The `think()` wake-up time is the actual minimum pending time, not whichever sound was visited last.
- Replaced the remaining duplicated `1.2`/`0.75` literals with the named constants and removed the always-true `USING_*` flags.
- Added `AcousticsManagerTest` for the delay bounds and inverted windows.

**Batch 15 — server no-sky guard, splash view, step block, dust trigger (218 tests green)**

- **B39:** `WorldHandler.updateWeatherBody` now skips `hasNoSky` dimensions unless the DS dimension config explicitly enables weather, matching vanilla's guard while keeping the Nether opt-in.
- **B6 (partial):** the Nether splash height now uses the passed view entity instead of the static `EnvironState` player; the base method still delegates to `World.getPrecipitationHeight` because `range` is only meaningful for the Nether scan.
- **B57:** `DimensionRegistry` probes the Calendar API class without `Loader.isModLoaded` in its static initializer, so unit tests and early class loading no longer depend on FML bootstrap.
- **B65:** step sounds sample `posY - 0.1` (the block under the feet), not `down(2)`.
- **B66:** `Dust.trigger` now requires air above the block, matching the fire/fountain behavior, so dust jets can appear on normal terrain.
- Added `WorldHandlerTest`, `NetherSplashRendererTest`, `BlockEffectHandlerTest`, and `JetEffectDustTest`.

**Batch 16 — server rain packet rate and saved-data dirty tracking (220 tests green)**

- **B38:** `ServerEffectHandler` now sends `PacketRainIntensity` only when the intensity changes or on a 40-tick keepalive, instead of every `Phase.START` for every dimension. The decision is a pure, tested helper.
- **B40:** `DimensionEffectDataFile` no longer marks the saved data dirty on every read. `DimensionEffectData` has a dirty callback; its setters and aurora add/clear mark the file, so unchanged data stays clean across autosaves.
- Added `shouldSendRainIntensity` coverage to `ServerEffectHandlerTest` and a new `DimensionEffectDataFileTest` proving repeated reads stay clean while a real setter dirties.

**Batch 17 — console command and legacy blockmap keys (222 tests green)**

- **B45:** `/rain` now resolves the world through `ICommandSender.getEntityWorld()` and replies to the sender, so the server console/RCON can run `/rain status` and the other subcommands. A unit test drives the command through a mocked console sender.
- **B81:** `LegacyCapableBlockMap` now accepts `id:meta` keys in addition to `id^meta` and `id.meta`; the separator choice is a tested helper.
- **B89 (partial):** `MathStuff.abs` uses a proper `< 0` test for the integer/long overloads. The double trig/log approximations and `MIN_VALUE` overflow remain in the backlog.

**Batch 18 — parser and config robustness (229 tests green)**

- **B82 (partial):** `BlockInfo.hashCode` is null-safe and `equals` uses `instanceof`; both the null-block and block-identity/metadata cases are tested. The `BasicBlockMap` regex/threading notes remain.
- **B85:** `AcousticsJsonReader` now throws descriptive `JsonParseException`s for missing `type`/`engineversion`/`contents`, empty sound names, missing basic names, and delayed acoustics without delay bounds. Tests drive the package-private parser directly.
- **B86:** `ConfigProperty.loadStream` treats JSON `null` as an empty map and returns `false` instead of installing a null map; tested.

**Batch 19 — dead code and logging cleanup (229 tests green)**

- **B68:** removed the 21 unused `CONDITION_TOKEN_*`/`CONDITION_SEPARATOR` constants; `getPlayerConditions` remains the single source of literals.
- **B78:** removed the unused `PFReaderQP` class (the port always uses `PFReaderH`).
- **B90 (partial):** removed the unused NBT holder classes and the unused `MIN_INTENSITY`/`MAX_INTENSITY` constants from `DimensionEffectData`, `DimensionEffectDataFile`, and `AuroraData`. The static `DecimalFormat` remains.
- **B95:** `ModLog.error`/`catching` now pass the throwable to Log4j instead of printing a second stack trace.

**Batch 20 — footstep ground detection and inside-cover constants (233 tests green)**

- **B77:** `PFSolver` now skips only a stationary *airborne* player. A player on the ground with `motionY == 0` is no longer treated as having no association, so flat-ground footsteps can resolve. Water/ladder cases remain exempt.
- **B92 (partial):** `PlayerUtils` uses its `RANGE`, `AREA`, and `INSIDE_Y_ADJUST` constants instead of the duplicated `3`/`49`/`2` literals. Unit tests cover open sky and a low ceiling. The 2-block ceiling-height limitation remains.
- Added `PFSolverTest` and `PlayerUtilsTest`.

**Batch 21 — JSON null/stream handling (235 tests green)**

- **B94 (partial):** `JsonUtils.load(InputStream)` returns `null` for a null stream; `load(File)`/`load(String)` fall back to the class default when the JSON body is `null`, so registries no longer receive a null config. Tests cover a null stream and a literal `null` file. `clazz.newInstance()` remains deprecated.

**Batch 22 — blockmap parser shape (237 tests green)**

- **B82 (partial):** replaced the fragile basic-blockmap regex with an anchored `block^meta+substrate` parser and a dedicated `parseKey` helper. Tests cover name-only, meta-only, substrate-only, both, and malformed keys. The mutable lookup key remains.
- Added `BasicBlockMapTest`.

**Batch 23 — null player, malformed footstep packs, and per-tick writes (238 tests green)**

- **B64:** `ClientEffectHandler` requires both a world and a player before dispatching handlers; a null player during respawn/dimension change is covered by `canProcess` and a unit test.
- **B83:** footstep loaders catch runtime parse failures, skip empty acoustic streams, and log malformed JSON through `ModLog` instead of a bare `printStackTrace`.
- **B84:** the `nextStepDistance` force is written only when it actually changes.
- Added `ClientEffectHandlerTest`.

**Batch 24 — rain coordinates and MathStuff accuracy (239 tests green)**

- **B11:** the rain/snow centre column no longer divides by zero; its coordinates are finite and unit-tested.
- **B89 (partial):** double `sin`/`cos`/`tan` delegate to the JDK instead of truncating to the float table; `log` is the JDK implementation. Integer/long `abs` use `< 0`; the `MIN_VALUE` overflow case remains.
- Removed the now-unused `RAD_TO_INDEX`/`DEG_*`/`COS_TO_SIN`/`RAD_TO_DEG`/`DEG_TO_RAD` constants.
- Added `StormRendererCoordinatesTest`; `MathStuffTest` now pins exact double trig/log.

**Batch 25 — survey throttling and immutable effects (241 tests green)**

- **B60:** `BlockRegistry.getEffects` returns a cached unmodifiable view instead of the live list; the weighted sound path no longer allocates a candidate list per call.
- **B70 (partial):** the biome survey dirty check no longer includes `playerY`, so jumping does not re-scan 169 columns. The scan is still per block moved; the throttle/cache remains.
- Added `BiomeSurveyHandlerTest` for Y-insensitivity and dimension/position/biome invalidation.

**Batch 26 — explicit config scrubbing and thread-local blockmap keys (242 tests green)**

- **B53:** `ModOptions` now builds the set of annotated properties and removes only unknown properties. A known property is preserved even if its comment is empty; the scrub helper is unit-tested.
- **B82:** `BasicBlockMap` uses a `ThreadLocal` mutable lookup key instead of one shared instance, so concurrent lookups cannot corrupt each other.
- Added a `ModOptions` scrub test.

**Batch 27 — config types and survey throttling (244 tests green)**

- **B52 (partial):** `ConfigProcessor` now handles `double` and `long` fields in addition to boolean/int/float/String/String[]. A test drives both through `Property`; enum support remains open.
- **B70:** biome surveys are throttled to one per 20 ticks for position movement, while dimension/biome changes remain immediate; Y-only movement still ignored. Tests cover the throttle and invalidation.
- Added config-type and survey-throttle assertions.

**Batch 28 — enum config values and conditional potion scrubbing (248 tests green)**

- **B52 (partial):** `ConfigProcessor` supports enum fields; invalid values log a warning and keep the default. `double`/`long`/enum tests now cover the new branches. A structured validation report remains open.
- **B73 (partial):** `PotionParticleScrubHandler` only writes the potion datawatcher when the colour is non-zero, avoiding a write every tick. The datawatcher hack itself remains until a targeted particle mixin replaces it.
- Added `PotionParticleScrubHandlerTest`.

**Batch 29 — version-check parsing and lifecycle (251 tests green)**

- **B61:** the version check uses `REMOTE_VERSION_FILE`, sets connect/read timeouts, closes its reader with try-with-resources, runs on a daemon thread, and uses the retry/interval constants. The `Loader`-dependent Minecraft version is resolved defensively so headless tests can load the class.
- **B93:** `SoftwareVersion` replaces `assert` validation with explicit `IllegalArgumentException`s, parses versions safely, and orders alpha < beta < release. Tests cover valid/malformed strings and prerelease ordering.
- Added `VersionCheckTest`.

**Batch 30 — sound-list cache and config validation report (252 tests green)**

- **B71 (partial):** `PlayerSoundEffectHandler` caches the weighted biome sound list by `(conditions, BiomeSurveyHandler revision)` and invalidates it when the survey changes.
- **B52:** `ConfigProcessor` exposes the last validation errors; invalid enum values keep their defaults and are reported. Tests cover the report.
- Added the enum-validation assertion.

**Batch 31 — targeted potion particle suppression (251 tests green)**

- **B73:** removed the datawatcher-colour hack. A new client-only `EntityLivingBasePotionMixin` cancels the `mobSpell`/`mobSpellAmbient` particle spawn only for the local player when `suppressPotionParticles` is enabled. The mixin is gated at apply time and also checks the option at runtime.
- Mixin wiring and bytecode-target tests now assert `updatePotionEffects` still calls `World.spawnParticle`.
- Removed the old `PotionParticleScrubHandler`.

**Batch 32 — thread-safe formatting and reflective construction (251 tests green)**

- **B90:** removed the shared `DecimalFormat` from `DimensionEffectData` and `CommandRain`; formatting uses locale-stable `String.format`. The remaining unused/dead-data cleanup is already covered by batch 19.
- **B94:** replaced deprecated `Class.newInstance()` with `getDeclaredConstructor().newInstance()`.
- Existing formatting, command, and JSON tests pass unchanged.

**Batch 33 — ambient sound list caching (252 tests green)**

- **B71:** `PlayerSoundEffectHandler` now caches the combined biome + player ambient list by `(conditions, survey revision, biome-sound flag)`, and caches the player-only list by conditions. `SoundManager.queueAmbientSounds` copies its input instead of mutating the caller's list, so the cached unmodifiable lists are safe to reuse. The spot-sound rolls remain per tick by design.
- Added `SoundManagerQueueTest`, which passes an unmodifiable list and verifies the queue no longer removes entries from it.

**Batch 34 — version source and opt-in (252 tests green)**

- **B61:** version checking is now opt-in (`Enable Online Version Check` defaults to `false`) and the URL is a configurable `Version Check URL`; `VersionCheck` no longer contains a duplicate URL or mod-version literal. The module version is a single `Module.VERSION` constant used by the `@Mod` annotation and the checker. Timeouts, daemon thread, retry constants, and try-with-resources are already in place.
- `ModOptionsTest` pins the disabled default, configurable URL, and restores both statics.

**Batch 35 — GitHub Releases and git-describe versions (253 tests green)**

- **B61:** the default version endpoint is now `https://api.github.com/repos/MrFuzzihead/DynamicSurroundings-Legacy/releases/latest`; the checker accepts GitHub’s `tag_name` JSON and still understands legacy `mcversion:version` lines.
- `Module.VERSION` now comes from the generated `Tags.VERSION` (git describe), so the `@Mod` annotation and checker share one source. `SoftwareVersion` extracts the numeric semver segment from describe strings such as `26.3-5.2.0-70-gc0d4f92f`.
- The update notification links to GitHub Releases instead of CurseForge; the obsolete VersionChecker/Curse IMC registration was removed.
- Tests cover git-describe parsing, GitHub `tag_name`, legacy lines, and missing tags.

**Batch 36 — production logging cleanup (253 tests green)**

- Replaced all 7 production `printStackTrace()` calls with `ModLog` calls:
  - unknown sound type, OpenAL channel query, default biome/block/dimension config loads, per-mod block config load, and version-check interruption.
- `VersionCheck` restores the interrupt flag before logging.
- Production `src/main/java` now has **zero** `System.out`/`System.err` and **zero** `printStackTrace()` calls. The game-test module keeps its stdout markers intentionally.

**Batch 37 — shared weather columns and coarse fog scans (256 tests green)**

- **B9/B10/P2:** added `WeatherColumnCache`, a dimension + 20-tick-bucket cache for biome, precipitation height, temperature, dust/precipitation flags. `StormRenderer` and `StormSplashRenderer` share it; splashes no longer repeat biome lookups.
- **B15/B16/P3:** `BiomeFogRangeCalculator` and `BiomeFogColorCalculator` now key their scans to 4-block coarse cells, so walking within a cell does not rescan 1,681/169 columns. Rain, far-plane, dimension and fog-mode invalidation are retained.
- **B17:** replaced hardcoded `0.9F`/`1681` in the biome fog range calculator with `DUST_FOG_IMPACT` and `(2*DISTANCE+1)^2`.
- Added `WeatherColumnCacheTest` for reuse, bucket expiry, and dimension invalidation.

**Batch 38 — sound engine correctness (261 tests green)**

- **B28 (partial):** `SoundCache` now resolves the `IResourceManager` lazily, uses a `ThreadLocal` 64 KiB buffer, uses a `ConcurrentHashMap`, clears on resource reload via `SoundCacheReloadListener`, reads until EOF instead of trusting `available()`, and logs/falls back to silence instead of throwing `Error`. The cache remains unbounded (no LRU/soft references).
- **B29:** `queueAmbientSounds` deduplicates by sound name and fades removed emitters instead of replacing an equal key.
- **B31:** removed the redundant `containsValue` scan from `isSoundPlaying`; it relies on vanilla's inverse-map check plus the delayed map.
- **B32:** removed the dead `PlayerSound.volume` assignment.
- **B35:** `canFitSound` now accounts for normal + streaming channels via a tested pure overload.
- **B36:** `SoundFixMethods` uses `instanceof ISourceRemoved` with a cleanup fallback.
- Added duplicate-name, channel-capacity, and missing-mixin fallback tests. **B33/P17** are handled in batch 39.

**Batch 39 — bounded sound cache and live OpenAL channel query (263 tests green)**

- **B28/P7:** `SoundCache` uses an access-ordered LRU map bounded to 256 entries, plus the reload clear and `ThreadLocal` buffer. The cache no longer grows with every played ogg.
- **B33/P17:** `configureSound` queries the live `ALCdevice` when a context exists, otherwise opens/closes the device for `ALC_MONO_SOURCES` without creating a temporary AL context. Channel splitting is a pure tested helper.
- **P8:** `SoundEffect` already has the matching `hashCode`; emitter dedupe and the removed `containsValue` scan complete the bookkeeping cleanup.
- Added `SoundManagerChannelTest`.

**Batch 40 — allocation-free block-effect sampling (264 tests green)**

- **B67/P4:** `BlockEffectHandler` reuses a single `MutableBlockPos`, early-outs when the block registry has no effects/sounds, and caches the sample count per configured range. The weighted sound path was already allocation-free.
- Added `checkCountForRange` coverage to `BlockEffectHandlerTest`.

**Batch 41 — server data cache, aurora cap, flat-world sea level (266 tests green)**

- **B41:** `DimensionEffectDataFile` caches the saved-data instance per world in a synchronized `WeakHashMap`, and `ServerEffectHandler` drops it on world unload. Repeated tick reads no longer call `loadItemData`.
- **B44:** aurora spawning is capped at 32 per dimension; `canAddAurora` is tested.
- **B58:** overworld sea level detects `WorldType.FLAT` directly from `WorldInfo`, so it is correct on a dedicated server without the client load event. `DimensionRegistryTest` covers it.
- Updated `DimensionEffectDataFileTest` to assert cache reuse and reload-after-unload.

**Batch 42 — side-safe reload, WAILA, and damage split (267 tests green)**

- **B46:** `/rain reload` now reloads server biome data and sends a `PacketReload` to clients, which schedule `BiomeRegistry`, `BlockRegistry`, and `SoundRegistry` initialization on the client thread. The server command no longer touches client-only `BlockRegistry`.
- **B55:** WAILA registration moved to `ProxyClient`, is only touched when Waila is loaded, and is no longer gated on debug logging.
- **B56:** the common `Proxy` no longer imports client classes. Server damage/heal detection moved to `server/DamageEffectServer` with a common `network/HealthData` DTO; the client keeps only `DamageEffectHandler.handleEvent`.
- Added `PacketReload` codec coverage.

**Batch 43 — copy-on-write biome registry (267 tests green)**

- **B59:** `BiomeRegistry.initialize()` now builds a fresh biome map and alias map, publishes the new registry snapshot under `INIT_LOCK`, and posts the reload event afterward. Reads use the volatile snapshot without locking.
- After the first initialization, an unknown biome gets a default entry instead of triggering a full rescan under the lock.
- Aliases are no longer cleared at the end of initialization.
- Existing biome registry tests remain green.

**Batch 44 — HUD text cache and environment null guards (268 tests green)**

- **B72/P9:** `PotionHUD` now uses `PotionTextCache`, caching the potion name per potion/amplifier and the duration string per displayed second. Added `PotionTextCacheTest`.
- **B69 (partial):** `EnvironState.getPlayer/getWorld/isPlayer/isPlayerMoving` now tolerate a missing client player instead of dereferencing null. The broader static-state refactor remains.
- **B76:** `craftSoundThrottle` starts at `Integer.MIN_VALUE`, so the first craft is not tick-counter dependent.

**Batch 45 — weather light cache and deterministic weather RNG (270 tests green)**

- **B9 residual:** `WeatherColumnCache.Column` now caches the sky light per column/height; `StormRenderer` no longer calls `getLightBrightnessForSkyBlocks` for every column frame. Added a cache-hit test.
- **B10:** the storm renderer reuses immutable white for the non-nether case and only allocates a `Color` copy when nether dust tint is needed.
- **P12:** `WorldHandler` has package-private `nextRainInterval`/`nextThunderInterval` overloads accepting an RNG; the tick path uses the shared RNG, tests use a mock. `STRENGTH_ADJUST` is now used for the strength steps.
- **P3/P13/P14/P15/P18/P10** remain.

**Batch 46 — fog cache retry cooldown (270 tests green)**

- **P3 (partial):** both biome fog calculators keep their cached result for 20 ticks when a chunk is not loaded and retry after the cooldown, instead of rescanning every frame. Also fixed the color calculator's inverted `blockExists` flag, which previously forced a scan almost every frame.
- **B16:** the color scan now uses the same coarse/cached behavior and only marks itself dirty for missing chunks.

**Batch 47 — biome-mix fog invalidation (272 tests green)**

- **P3:** fog caches now use a cheap centre-plus-cardinal biome signature. They reuse the cached scan while the signature is unchanged, even if the player moves within the same biome mix; the 20-tick missing-chunk cooldown remains.
- Added `FogBiomeSignatureTest` for stable and changing mixes.

**Batch 48 — dimension condition cache (273 tests green)**

- **P15 (partial):** `DimensionRegistry.getConditions` caches the built condition string by dimension, day/night, rain and season; repeated ticks reuse it. Player-condition string caching/snapshot refactor remains.
- Added a condition-cache equality test to `DimensionRegistryTest`.

**Batch 49 — dead-code and identity cleanup (274 tests green)**

- **B7:** removed the dead `level += 0.01F` adjustment in `Weather.setIntensity`.
- **B24:** removed the unused Aurora constants/literals.
- **B62:** removed the handler-less `Module` FML-bus registration; `Module.VERSION` remains the single version source.
- **B63:** corrected the fog config comments.
- **B89:** integer/long `abs(MIN_VALUE)` now saturates to `MAX_VALUE` instead of returning a negative value; tested.
- **B91:** documented column-based `AuroraData` identity and added `sameColumn`; seed/color updates no longer obscure the intended identity.
- **B92:** `isReallyInside` now uses `World.canBlockSeeTheSky` over the survey area, so a tall roofed room counts as inside; tests updated.

**Batch 50 — config save ordering (274 tests green)**

- **B54 (partial):** the config GUI now saves the synthetic sound lists and config before calling `super.actionPerformed`, so a closing screen cannot save stale values. The broader runtime-reload/no-restart behavior remains.

**Batch 51 — sound asset validation (275 tests green)**

- Added `SoundAssetValidationTest`, which parses `sounds.json` and asserts every bundled `dynamicsurroundings:` variant has a matching `.ogg` resource. This catches missing/renamed audio assets headlessly.
- This begins the golden-data portion of the §8.2 harness backlog.

**Batch 52 — config sound references (276 tests green)**

- Added `ConfigSoundReferenceTest`: every `dynamicsurroundings:` sound referenced by the shipped biome and block JSON must exist as a key in `sounds.json`. This catches config/audio drift without launching the game.

**Batch 53 — runtime config reload (276 tests green)**

- **B54 (partial):** after saving the config GUI, `ClientEffectHandler.reloadRuntime()` now rebuilds biome/block/dimension/sound registries and refreshes the fog theme. The save still happens before `super.actionPerformed`. Options that are inherently restart-bound (static final sound tables, handler registration) still need a restart.

**Batch 54 — player condition cache (276 tests green)**

- **P15 (partial):** `EnvironState.getPlayerConditions` now builds a bitmask of all player conditions and caches the resulting string by mask + temperature category. Repeated ticks avoid rebuilding the string.
- The broader `EnvironmentSnapshot` refactor remains.

**Batch 55 — config processor coverage (279 tests green)**

- Extended `ConfigProcessorTest` with annotated integer/float clamping, restart/world-restart flags, hidden options, and comma-split `String[]` defaults. Covered in `ConfigProcessorTest` (7 tests total).

**Batch 56 — shipped JSON validation (280 tests green)**

- Added `ShippedJsonValidationTest`: every `.json` under `assets/dynamicsurroundings` is parsed with Gson, so a broken data/resource file fails the headless build.

**Batch 57 — P1 fully-automatable backlog (313 tests green)**

- **R1 — coverage metric fixed.** `jacocoClassDirs()` now excludes only GL/audio/client-bound classes (renderers, handlers, config GUI, HUD, WAILA, particle instancing, sound cache/manager, biome-grid fog scans) instead of all of `client/**`. The measured subset is now **152/177 classes (86 %)**, with **56.3 % line / 40.8 % branch / 69.7 % class** coverage. Global floors rose to **50 % line / 35 % branch**, plus per-package floors for `util`, `data`, `client.weather`, `client.aurora`, `client.sound`, `client.fog`, footsteps, and config properties.
- **R2 — game-test report guard.** `jacocoGameTestReport` now reports against `build/classes/java/main` (the classes the game test actually loaded) instead of a missing RFB dump. New `verifyGameTestReport` fails `check`/`gameTestServer` when `gameTest.exec` exists but the XML has no counters.
- **R3 — remaining pure/client-logic tests.** Added `AuroraTest`, `NodeTest`, `WeatherTest`, `PFReaderHTest`, `PlayerSoundTest`, `SpotSoundTest`, `ProbabilityWeightsAcousticTest`, and `MorningAndHazeFogTest`. The Weather test exposed **B96**: `setIntensity(0)` was a no-op while the state was `VANILLA`, so `NONE` was unreachable from a fresh state; fixed.
- **R4 — ForgeDictionary cache.** Extracted `OreLookupCache`; repeated `ForgeDictionary.initialize` calls reuse per-ore `ItemStack` lists, with `invalidate()` for ore-dictionary changes.
- **R5 — P15 residual regex work.** `BiomeRegistry` compiles each config entry’s biome regex once per reload and reuses it for every biome/alias instead of calling `Pattern.matches` per lookup.
- **Verification:** `./gradlew check` green; `gameTestServer` **65/65**; `verifyGameTestReport` green.

**Batch 58 — R6 game-test data/network/persistence checks (313 unit tests, 79/79 game checks)**

- Added `checkShippedDimensionEntries`: `dimensions.json` must have entries, every entry needs an id or name, ids must be unique, and configured ids must be registered.
- Added `checkShippedBlockIdsResolve`: every block id in `blocks.json` must resolve through the live `GameData` registry and not be air.
- Added `checkRainPacketDecision`: first-send, send-on-change, unchanged suppression, and 40-tick keepalive are asserted against the real `ServerEffectHandler` method.
- Added `checkDimensionDataPersistence`: sets a rain window/intensity, round-trips the attached `DimensionEffectDataFile` through NBT, and reads the restored values back.
- `ServerEffectHandler.shouldSendRainIntensity` and `DimensionEffectDataFile.getData` are now public for the game-test harness.
- **R6 complete.**

**Batch 59 — R9 snapshot and R10 restart contract (316 unit tests, 79/79 game checks)**

- **R9 — `EnvironmentSnapshot`.** Added an immutable per-tick snapshot (`EnvironmentSnapshot`) with the player/world, biome, dimension, conditions, temperature, inside/freezing/humid/dry flags, tick, ground/moving state. `ClientEffectHandler` builds it once before dispatching handlers; `EnvironState` getters now delegate to the published snapshot. Fog, block effects, ambient/player sounds, biome survey, aurora, damage effects, footsteps and Angelica diagnostics read the snapshot instead of a set of mutable static fields. Added `EnvironmentSnapshotTest`.
- **R10 — restart contract.** Added `@RestartRequired` to `blockedSounds` (the last init-time option without it) and a `ModOptionsTest` that pins every handler/static-table option to the annotation. The config UI now consistently tells users that these options need a restart; runtime reload still handles the registry/fog options.
- Coverage is now **57.1 % line / 40.8 % branch / 70.1 % class** over **154/177 classes**.

**Batch 60 — P2 fully-automatable cleanup (322 unit tests, 79/79 game checks)**

- **M1 — mixin hardening.** Added `MixinClassAuditTest`: every class in the `mixins` package must be `@Mixin` or listed in `@TransformerExclusions`, and every mixin field must be `@Unique` or `@Shadow`. Annotated the weather-injector constant `@Unique`.
- **M2 — clashing class names.** Renamed `util.Color` → `RgbColor` (and `ImmutableColor` → `ImmutableRgbColor`), and `client.sound.SoundManager` → `DynamicSoundManager`. References, tests and mixin hooks updated; no Minecraft or `java.awt` name collision remains.
- **M3 — rolling block samples.** `BlockEffectHandler` now samples half the configured volume per tick (`sampleCountForRange`), with a tested minimum of one sample.
- **M4 — dead code/assets.** Removed six unreferenced ogg files, the empty `variator.json` and its loader path, duplicate `MC_BLACK`/`MC_WHITE`, the public `RenderWeather.register` entry point, and made `WailaHandler` an explicit singleton.
- **M5 — golden/schema data tests.** Added strict duplicate-key detection for every shipped JSON file; runtime block-id resolution remains covered by R6.
- **M6 — property loops.** Added seeded loops for `RgbColor` integer round trips, `BlockPos` long serialization, `MathStuff` trig accuracy, and `SoftwareVersion` ordering.
- Coverage is now **56.4 % line / 40.6 % branch / 70.3 % class** over **155/177 classes (88 %)**.

**Batch 61 — M7 registry lifecycle (325 unit tests, 79/79 game checks)**

- Added `RegistryManager` with named `Reloadable` entries and `reload(...)`/`reloadAll()`.
- Biome, dimension and sound registries are registered centrally; the client registers `blocks` during client init.
- Server post-init, client runtime reload, `PacketReload`, and `/rain reload` now call the manager instead of repeating the registry list.
- A failing registry is logged without preventing the remaining registries from reloading; covered by `RegistryManagerTest`.

**Batch 62 — P2 design/refactor completion (328 unit tests, 79/79 game checks)**

- **M9 — config schema.** Added `ConfigSchema`: category comments, order and restart flags now come from one declarative table instead of repeated `setCategory*` calls, and unknown properties are logged before scrubbing. `ConfigSchemaTest` covers metadata and unknown-option detection.
- **M10 — resource loading.** Added `ResourceLoader.forEachPack` for the shared open/consume/close lifecycle and refactored the footsteps block map, primitive map and acoustics loaders onto it. `ResourceLoaderTest` covers success, null streams and per-pack failures.
- **M11 — build hygiene.** Moved the test/game-test/JaCoCo harness from `addon.gradle.kts` into `gradle/test-harness.gradle.kts`, applied from `addon.gradle.kts`. Root `build.gradle.kts` and `settings.gradle.kts` are unchanged.
- Coverage is now **56.7 % line / 41.3 % branch / 71.1 % class** over **159/177 classes (90 %)**.

**What remains:** the headless correctness backlog is exhausted. The release gate is the real client compatibility matrix; the authoritative remaining-work register is §11.

### The three compatibility moves that unlock the most value

1. **A weather backend abstraction** with three owners: vanilla/external `IRenderHandler`, DS legacy renderer, and Angelica's instanced weather. DS mixins ask the backend before cancelling. **Done in batch 1.**
2. **An Angelica `WeatherColumnProvider` bridge** so DS's per-biome rain/dust/snow is fed into Angelica's instanced pipeline instead of competing with it. Angelica's API already exists (`com.gtnewhorizons.angelica.api.weather`, `apiVersion = 1`). **Done in batch 5; now uses a compileOnly Angelica reference (batch 8) instead of a reflection proxy.**
3. **Relative fog + a NotFine setting bridge** (`jss.notfine.core.SettingsManager.downfallDistance`, `Settings.TERRAIN_FOG`, `Settings.FOG_NEAR_DISTANCE`) — reflective, optional, no compile dependency. **Done in batches 1–2 for downfall and fog range.**

### Coverage reality

The JaCoCo floor now measures **56.7 % lines / 41.3 % branches / 71.1 % classes** over **159 of 177 classes (90 %)**, after batch 57 narrowed the exclusion from all of `client/**` to GL/audio/client-bound classes (`addon.gradle.kts`). The floor is a regression guard plus per-package floors, not a target; see §8.

---

## 1. Codebase snapshot

### 1.1 Layout

```
src/main/java/com/mrfuzzihead/dynamicsurroundings
├── Module / ModOptions / ModLog / VersionCheck      bootstrap, config, logging
├── client/                                          ~62 % of LOC, excluded from coverage
│   ├── EnvironStateHandler, ClientEffectHandler     per-tick god-object + handler fan-out
│   ├── fog/                                         fog colour + range calculators
│   ├── weather/                                     storm overlay, splash particles, weather state
│   ├── aurora/                                      aurora geometry + immediate-mode renderer
│   ├── fx/, fx/particle/                            block jets and text pop-offs
│   ├── sound/, sound/cache/, sound/fix/             emitters, spot sounds, sound cache, paulscode fixes
│   ├── footsteps/                                   Presence-Footsteps-derived engine
│   ├── gui/, hud/, waila/                           config GUI, potion HUD, WAILA
├── data/                                            biome/block/dimension registries, saved data
├── server/                                          weather state machine, aurora spawning
├── network/                                         3 packets
├── mixins/ (early)                                  EntityRenderer, World, WorldServer, SoundManager, accessors, paulscode
└── core/SoundManagerSyncTransformer                 the one remaining class transformer
```

### 1.2 Test harness (already good)

- JUnit 4 + Mockito (inline) on a Java 8 toolchain, JaCoCo 0.8.16, coverage floor on `check`.
- `src/gameTest` — a separate non-shipped `@Mod` that boots a dedicated server at `FMLServerStartedEvent`, runs ~70 assertions and exits with a status (`GameTestMod`, `ServerGameTests`).
- Mixin safety tests: wiring annotations, injection-point bytecode, refmap contents, generated config presence. This is a strong foundation and should be kept.
- What is missing is **client/render logic coverage** and **optional-mod integration coverage**.

### 1.3 Measured coverage (unit run, excluding client)

| Counter | Covered | Missed | Ratio |
|---|---:|---:|---:|
| Line | 1,017 | 1,089 | 48.3 % |
| Branch | 237 | 568 | 29.4 % |
| Instruction | 5,022 | 4,628 | 52.0 % |

---

## 2. Angelica / NotFine compatibility (highest priority)

### 2.1 How the three mods meet

| Surface | Angelica | NotFine (bundled in Angelica) | Dynamic Surroundings |
|---|---|---|---|
| `EntityRenderer.renderRainSnow(F)` | `MixinEntityRenderer_Weather` injects at `HEAD`, cancels when `WeatherRenderer.render()` handles it | `MixinEntityRenderer$RenderRainSnow` redirects `fancyGraphics` and the weather radius byte | `EntityRendererMixin` injects at `HEAD`, **does not cancel** |
| `EntityRenderer.addRainParticles()` | same mixin, cancels when `WeatherRenderer.spawnRainParticles()` handles it | — | `EntityRendererMixin` injects at `HEAD`, cancels |
| Fog range (`setupFog`) | `GLSMFog` tracks state; `AngelicaFogService`/`GLState` expose it read-only | `MixinEntityRenderer` `@ModifyArg` on `glFogf` ordinals 14/15, driven by `TERRAIN_FOG`/`FOG_NEAR_DISTANCE` | `FogEffectHandler` writes absolute `glFogf(2915/2916)` at `LOWEST` |
| Fog colour | `FogHelper` captures `fogColor*` at `updateFogColor` RETURN; Iris/terrain use it | — | `FogColors` event at `HIGHEST` (composes correctly) |
| Particles | `ParticleInstancer`/`ParticleRunSplitter` batches `renderParticle` calls, including unknown `EntityFX` classes | `MixinEffectRenderer` depth-mask redirect; void-particle toggle | custom `EntityTextPopOffFX` calls `FontRenderer` from inside `renderParticle` |
| HUD | `HUDCaching` renders the overlay into a cached framebuffer at `hudCachingFPS` | HUD toggles | `GuiHUDHandler` renders on **every** `RenderGameOverlayEvent`; `PotionHUD` on `EXPERIENCE` |
| Weather data | `WeatherColumns` cache + instanced draws; provider API | `DOWNFALL_DISTANCE` (0/5/10/15) | `StormRenderer` recomputes 441 columns/frame; ignores `downfallDistance` |
| BOP fog | `BopFogBlend`, `FogColourGrid` | `MixinFogHandler` | `BiomeRegistry` reads `IBiomeFog` per biome and blends again |
| Sound | no `SoundManager` hooks found | — | `SoundManagerMixin` + `SoundManagerSyncTransformer` |
| Mixins | early configs, no explicit priority on the weather mixin | early toggles | early configs, no explicit priority on the weather mixin |

### 2.2 Conflict A — both mods own the weather renderer (critical)

**Evidence**

- `client/weather/StormRenderer.java:51` explicitly delegates to `world.provider.getWeatherRenderer()`, and `EntityRenderer.renderRainSnow` *also* delegates and returns (`build/rfg/minecraft-src/.../EntityRenderer.java:1554`). Because DS does not cancel, the provider is invoked twice.
- `EntityRendererMixin.java:30` is a plain `HEAD` injection. The old `PatchEntityRenderer` inserted `RenderWeather.renderRainSnow(...)` **and `RETURN`** at the head (`git show HEAD~2:.../asm/PatchEntityRenderer.java`). The mixin conversion dropped the cancel → **vanilla draws rain on top of DS rain**.
- Angelica's `MixinEntityRenderer_Weather` is also `@Inject(HEAD, cancellable = true)`. Mixin applies multiple HEAD injectors; only the first `cancel()` in execution order matters. Which one is first depends on config/mixin ordering (both mods use early configs with no explicit priority for these methods).

**Consequences (depending on order)**

- DS renders rain + vanilla renders rain (double overlay, double fill cost).
- DS and Angelica both render weather (or one never runs).
- A third-party `IRenderHandler` is rendered twice.
- `disableWeatherEffects` removes the whole weather mixin, which also removes the **aurora** (see 2.4).

**Recommended fix**

Introduce a single decision point, e.g. `client/weather/WeatherBackend`:

```java
enum Owner { EXTERNAL_PROVIDER, ANGELICA_INSTANCED, DYNAMIC_SURROUNDINGS, VANILLA }
static Owner owner(WorldClient world) { ... }
static boolean isDsOwner(WorldClient world) { return owner(world) == DYNAMIC_SURROUNDINGS; }
```

- `EXTERNAL_PROVIDER` if `world.provider.getWeatherRenderer() != null` and it is not DS.
- `ANGELICA_INSTANCED` if Angelica is loaded, `enableInstancedWeather` is true, and NotFine's `downfallDistance != 0` (mirror `WeatherRenderer.render`'s own gate).
- Otherwise `DYNAMIC_SURROUNDINGS` (or `VANILLA` when `disableWeatherEffects`).
- Both DS mixins:
  ```java
  @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true)
  private void ds$renderRainSnow(float pt, CallbackInfo ci) {
      if (!WeatherBackend.isDsOwner(mc.theWorld)) return; // let Angelica/vanilla/provider own it
      RenderWeather.renderRainSnow((EntityRenderer)(Object)this, pt);
      ci.cancel();                                      // never double-render
  }
  ```
- `StormRenderer.render` should stop calling `getWeatherRenderer()` itself (vanilla already calls it when we don't cancel), or `WeatherBackend` should call it and then DS cancels. Pick one owner, not both.
- The same rule applies to `addRainParticles`: when Angelica is active, neither DS mixin cancels, so Angelica owns the frame whether its injection was applied before or after DS's.
- Add `priority` to the DS mixin only if a deterministic order is still needed; runtime ownership is preferable to priority.

### 2.3 Conflict B — fog range override breaks NotFine (critical user-visible)

**Evidence**

- `client/FogEffectHandler.java:76-77` writes absolute `glFogf(GL_FOG_START/END)` at `EventPriority.LOWEST`, *after* vanilla `setupFog` and NotFine's `@ModifyArg` changes.
- NotFine's settings (Angelica `jss/notfine/core/Settings.java`):
  - `TERRAIN_FOG` (false → `glFogf` near = `15 * 1024 * 1024`, far = `16 * 1024 * 1024`)
  - `FOG_NEAR_DISTANCE` (1–100 %, default 75; near = `farPlaneDistance * pct * 0.01 - 1`)
- `RenderFogEvent` only exposes `fogMode` and `farPlaneDistance` (Forge 1.7.10), so DS cannot see the current start/end from the event and re-computes them from `farPlaneDistance`. Result: DS re-enables fog when the user disabled it, and ignores the near-distance percentage. This is Angelica issue **#2304** ("Dynamic Surroundings disables fog settings"), with screenshots at 1 % near distance.

**Implemented in batch 2** (the points below are the design that shipped):

**Recommended fix (composes with every fog mod)**

1. In `fogRenderEvent`, read the current state that vanilla/NotFine already produced:
   ```java
   float baseStart = GL11.glGetFloat(GL11.GL_FOG_START);
   float baseEnd   = GL11.glGetFloat(GL11.GL_FOG_END);
   ```
   This works in vanilla and in Angelica: `GLSMQueries.glGetFloat` returns the tracked `fogState` for `GL_FOG_START/END/DENSITY` (`glsm/.../GLSMQueries.java:379-381`), and `GL11` calls are redirected by Angelica's transformer.
2. Change `FogResult`/calculators to produce **relative scales** (`startScale`, `endScale`, or `densityScale`) instead of absolute distances, and apply:
   ```java
   GL11.glFogf(GL_FOG_START, baseStart * result.getStartScale());
   GL11.glFogf(GL_FOG_END,   baseEnd   * result.getEndScale());
   ```
   Scaling preserves NotFine's 1 % near distance, preserves `TERRAIN_FOG = false` (16 M × 0.4 is still effectively infinite), and preserves the vanilla 0.75/0.25 geometry.
3. Only modify `GL_LINEAR` fog; use `GL_FOG_DENSITY` for `GL_EXP/GL_EXP2` if DS ever wants those modes. Do not touch water/lava (already skipped).
4. Add `fog.respectExternalFog` (default `true`). When false, keep today's absolute behaviour for users who want DS to own fog.
5. Optional NotFine bridge (compileOnly Angelica/NotFine, guarded hook):
   - If `Loader.isModLoaded("notfine")`, read `jss.notfine.core.SettingsManager.downfallDistance` and `Settings.TERRAIN_FOG` / `FOG_NEAR_DISTANCE`.
   - This is also needed for `StormRenderer` (see 2.6).

**Tests to pin:** `FogResult` scale semantics; a mock `RenderFogEvent` + `GL11` static mock asserting that DS never increases `end` beyond the base; a compatibility game test that sets NotFine near=1 % and asserts the final fog start is ≈1 % of far.

### 2.4 Conflict C — aurora is rendered inside the weather phase (and dies with weather disabled)

**Evidence**

- `client/RenderWeather.java:34-37` registers `AuroraRenderer` in the same list as `StormRenderer`; both are invoked from `renderRainSnow`.
- Angelica wraps `renderRainSnow` in `WorldRenderingPhase.RAIN_SNOW` (`mixins/early/shaders/MixinEntityRenderer.java:140`). DS's aurora therefore draws into the **rain/snow gbuffer** with shaders. That is the wrong program: with Iris it can be invisible, tinted like rain, or write to the wrong buffer.
- If Angelica's `WeatherRenderer.render` cancels `renderRainSnow` (instanced weather default on), DS's `HEAD` injection may never run → **aurora disappears**.
- If `ModOptions.disableWeatherEffects` is true, the whole `WEATHER` mixin group is not applied, so `RenderWeather` is never called → **aurora disappears** even though `auroraEnable` is true.
- `AuroraRenderer` has an obvious leftover: `GL11.glScaled(3.5, 25.0, 3.5)` after `tess.draw()` (`AuroraRenderer.java:137`), plus unused `lowY`/`lowY2`.

**Recommended fix**

1. Separate the render paths: `RenderWeather.renderStorm(...)` and `AuroraRenderer.renderAurora(...)`, and stop putting the aurora in the weather list.
2. Render the aurora from `EntityRenderer.renderWorld` at the `renderRainSnow` call **with `shift = AFTER`** (works whether or not anyone cancelled weather), or via `RenderWorldLastEvent`. Do not render it inside the `RAIN_SNOW` phase.
3. **Implemented in batch 6:** `IrisPhaseBridge` sets `WorldRenderingPhase.CLOUDS` around the aurora draw and restores the previous phase; it is a no-op without Iris.
4. Wrap the draw in `GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)` / `glPopAttrib`, or use Angelica's `GLSMAttribStack` if present, so the immediate-mode state changes (`glDisable(GL_TEXTURE_2D)`, shade model, blend, depth mask, cull) cannot leak into terrain/HUD/other mods. Prefer `GLStateManager`-tracked calls if a compile dep is acceptable; otherwise the redirected `GL11` calls are equivalent under Angelica.
5. Move the aurora out of the `WEATHER` mixin gate so `disableWeatherEffects` leaves it working; gate it on `auroraEnable` + `DimensionRegistry.hasAuroras`.

### 2.5 Conflict D — immediate-mode text particles vs instancing and shaders (high)

**Evidence**

- `client/fx/particle/EntityTextPopOffFX.java:59-89` calls `FontRenderer.drawStringWithShadow` from inside `EntityFX.renderParticle(Tessellator, ...)` and ignores the passed `Tessellator`. It also changes `glDepthFunc`, `glDepthMask`, `glDisable(GL_LIGHTING)`, `glEnable(GL_TEXTURE_2D)` and only partially restores.
- Angelica's `ParticleInstancer` captures unknown `EntityFX` classes and calls `fx.renderParticle(CAPTURE, ...)` inside the batched particle pass (`rendering/particles/ParticleInstancer.java`). Drawing the font there:
  - produces zero captured vertices (the text bypasses the capture tessellator),
  - binds the font texture and changes state in the middle of a deferred/batched pass,
  - under Iris runs inside a gbuffer program, so the text is shaded as whatever pass is active.
- The `glDepthFunc(519)` then `glDepthFunc(515)` sequence is not a save/restore; it leaves `GL_LEQUAL` instead of the default `GL_LESS`.

**Recommended fix**

- **Implemented in batch 7:** `EntityTextPopOffFX` is a render-less marker and `TextPopOffRenderer` draws all active pop-offs in a dedicated `RenderWorldLastEvent` pass with:
  - `glPushAttrib/glPopAttrib` (or `PushMatrix` + explicit state save),
  - `glDisable(GL_DEPTH_TEST)` for `shouldOnTop` instead of changing the depth *function*,
  - a depth-sorted or distance-scaled draw,
  - a guard that defers under `ParticleInstancer`/Iris to avoid re-entering the particle pass.
- Alternatively implement the text as real textured quads built into the supplied `Tessellator`; that is the only version that batches correctly, but requires a glyph atlas.
- Add a test that renders a text pop-off with a mocked `Tessellator` and asserts that the `GL11` calls are balanced (or, better, that the overlay list contains the pop-off and no GL calls happen during `renderParticle`).

### 2.6 Conflict E — NotFine settings ignored (medium)

| NotFine setting | DS behaviour | Fix |
|---|---|---|
| `DOWNFALL_DISTANCE` (`SettingsManager.downfallDistance`, 0/5/10/15) | `StormRenderer.java:77` used `mc.gameSettings.fancyGraphics ? 10 : 5`; splashes used hard-coded `RANGE=10` | **Fixed in batch 1:** one reflected radius for quads and splashes; `0` disables downfall. |
| `TERRAIN_FOG` / `FOG_NEAR_DISTANCE` | Overridden by absolute `glFogf` | **Fixed in batch 2:** relative fog preserves both settings. |
| `VOID_FOG` | `BiomeFogColorCalculator.applyPlayerEffects` independently applies `getVoidFogYFactor` to the biome portion | **Fixed in batch 4:** `NotFineBridge.isVoidFogEnabled()` gates DS's term. |
| `CLOUD_HEIGHT` | `DimensionRegistry` cloud height is used for aurora height/haze | **Fixed in batch 6:** NotFine `CLOUD_HEIGHT` is used for aurora/haze unless the dimension config explicitly overrides it. |
| `MODE_CLOUDS`, `MODE_WATER`, `MODE_LEAVES`, `MODE_SKY` | no direct conflict | Add to the compat diagnostics, no action |

**Bridge design:** `compat/notfine/NotFineCompat` with a `boolean available`, cached field/method handles, and `IBridge` fallback (`NONE`), mirroring the existing `BOPFogIntegration` pattern. Load it only when `Loader.isModLoaded("notfine")`, never reference `jss.notfine.*` from a class that is loaded unconditionally. Add a unit test that the bridge is a no-op when the class is absent and that it caches the lookup.

### 2.7 Conflict F — HUD caching (low/medium)

- `GuiHUDHandler.onRenderExperienceBar` (`client/hud/GuiHUDHandler.java:36`) forwards **every** `RenderGameOverlayEvent` (including `Pre`/`Post`/`Text` and the synthetic events HUDCaching constructs) to every overlay. `PotionHUD` filters by `event.type == EXPERIENCE && !event.isCancelable()`, which happens to work but is fragile and does unnecessary work several times per frame.
- Fix: filter in the dispatcher (`if (event.type != ElementType.EXPERIENCE || event instanceof RenderGameOverlayEvent.Pre) return;`) and render from `RenderGameOverlayEvent.Post(EXPERIENCE)`. That is inside the cached pass and refreshes at `hudCachingFPS` (≥ 20 by default), which is fine for second-resolution potion timers.
- Null-guard `Minecraft.getMinecraft().thePlayer` (`PotionHUD.java:39`) and restore `glColor`/`glDisable(GL_LIGHTING)` state (the cache restores state in Angelica, but vanilla does not).
- `PotionHUD` calls `I18n.format`/`getStringWidth` every frame; cache per `PotionEffect` and invalidate on the displayed second.

### 2.8 Conflict G — BOP fog is blended twice (medium)

- DS reads BOP's `IBiomeFog` (`data/compat/BOPFogCompat.java`) and adds the biome colour/density to its own `BiomeFogColorCalculator`/`BiomeFogRangeCalculator`.
- Angelica has its own BOP fog pipeline (`compat/bop/BopFogBlend`, `FogColourGrid`, late `MixinFogHandlerBiomeCache`), which also blends BOP fog. With both installed and biome fog enabled, the BOP colour/density is applied twice (or two different blends compete).
- **Fixed in batch 6:** `Compat.isAngelicaBopFogActive()` gates the `BiomeRegistry` import; DS keeps its BOP import only when Angelica's late BOP fog mixin is not present.
- The existing `BOPFogCompatTest` still pins the non-Angelica bridge; a full Angelica+BOP client run remains in the matrix.

### 2.9 Conflict H — mixin robustness and ordering (medium)

- No `priority` on either side's weather mixin; the runtime ownership check above is the real fix, but add `priority = 900` to DS's weather mixin so it applies *before* Angelica's (lower priority applies first). Then DS's runtime check decides whether to cancel; Angelica's later HEAD injection runs only if DS declined. Document the invariant.
- `Mixins.java` puts `Mixins` and `EarlyMixinsLoader` in the `mixins` package and therefore needs `@TransformerExclusions` for them. Any new non-mixin helper added to that package will be rejected by Mixin. Either move the catalogue/loader to `core/mixins` (cleaner) or add a test that every non-`@Mixin` class under `mixins` is listed in `@TransformerExclusions`.
- `WorldMixin` uses `remap = false` for `updateWeatherBody`; this is correct — it is a Forge-added method with no SRG name (verified against `build/rfg/minecraft-src/.../World.java` and `methods.csv`). Keep the existing refmap test that asserts the name stays out of the refmap; add the same assertion for any new Forge-added targets.
- `SoundManagerMixin`'s `@ModifyConstant(intValue = 20)` modifies **all** 20 constants in `playSound`, not just the play-time one. The injection-point test should assert exactly one occurrence; if a mapping update adds another `20`, the mixin would silently change it.
- `EntityRendererAccessor` duplicates an accessor Angelica also adds (different method names, so no conflict), but keep the prefix convention to avoid future collisions.

### 2.10 Recommended compatibility architecture

```
compat/
├── Compat.java                 minimal facade: isAngelica(), isNotFine(), isIris(), isBop()
├── angelica/
│   ├── AngelicaBridge.java     guarded: enableInstancedWeather
│   ├── AngelicaWeatherProvider.java   installer/factory, no Angelica types in its bytecode
│   ├── AngelicaWeatherProviderFactory.java   direct IRenderHandler + WeatherColumnProvider
│   └── AngelicaDebugEntries.java      optional DebugScreenApi registration (batch 9)
├── notfine/
│   ├── NotFineBridge.java      guarded entry point
│   └── NotFineSettingsHook.java   direct SettingsManager/Settings references
└── bop/
    └── BopFogBridge.java       existing BOPFogIntegration, gated on Angelica presence
```

Rules:

1. **No hard dependency** on Angelica/NotFine. Every class that names their types is loaded only behind `Loader.isModLoaded(...)`, using the existing `IBOPFog.NONE` fallback pattern.
2. **Prefer public APIs** where they exist: Angelica's `api.weather`, `api.glstate.GLState`, `api.debug`, `api.GLRedirects`; Iris's `IrisApi`.
3. **No `Class.forName` in static initializers** of classes loaded on both sides; resolve lazily and cache the result.
4. **Declare optional relations** in `mcmod.info`: `after:angelica`, `after:notfine`, `after:iris`, `after:biomesoplenty` (already `UniMixins` required). Do not use `required-after`.
5. **Diagnostics**: add a `DiagnosticEvent` section listing detected mods, versions, backend owner, effective fog start/end, and NotFine settings. This turns future compatibility reports into one screenshot.

### 2.11 Compatibility test matrix

| Test | How | What it catches |
|---|---|---|
| No Angelica/NotFine | existing unit + dedicated game test | regressions in the base path |
| Angelica, instanced weather **on** | extra `runClient` with Angelica as `devOnlyNonPublishable`; assert the weather backend owner is `ANGELICA_INSTANCED` and DS is not cancelling | double render / lost weather / lost aurora |
| Angelica, instanced weather **off** | same, toggle in config | DS fallback still renders |
| NotFine near fog 1 % and terrain fog off | set the option in the NotFine config before launch; read `GL_FOG_START/END` via `GLState` and assert | Angelica #2304 |
| Iris shader pack | Complementary/Euphoria as the GTNH target; screenshot aurora at night + rain | aurora in the wrong gbuffer / text particles |
| BOP + Angelica | BOP fog biome | double fog blend |
| Mixin order | run with Angelica's mixin config before/after DS (or add priority) | order-dependent cancel |

Because Angelica is large, the client matrix can be a **separate CI job** that only boots, joins a flat world, forces rain/night via `/rain 100` + `/time set 18000`, renders N frames, and checks `glGetError()` + the absence of Mixin errors in the log. It does not need screenshots to be useful.

---

## 3. Bugs

Severity: **C**ritical (crash/corruption/major feature loss), **H**igh (wrong visible behaviour), **M**edium, **L**ow.

### 3.1 Weather & rendering

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B1 | **C** | `mixins/early/EntityRendererMixin.java` | `renderRainSnow` doubled vanilla rain and providers. Old transformer inserted `RETURN`; the mixin did not cancel. **Fixed in batch 1:** cancel after DS renders, only when DS is the weather owner. |
| B2 | **C** | `client/weather/StormRenderer.java:51`, `mixins/early/EntityRendererMixin.java` | DS called `getWeatherRenderer()` itself, then vanilla called it again. **Fixed in batch 1:** the provider is now rendered only by the owner selected by `WeatherBackend`. |
| B3 | **H** | `mixins/early/EntityRendererMixin.java` (whole `WEATHER` group) | `disableWeatherEffects` also disabled the aurora. **Fixed in batch 1:** `AuroraRenderHandler` now draws from `RenderWorldLastEvent`, outside the weather mixin gate. |
| B4 | **H** | `client/weather/StormRenderer.java:77` | NotFine `DOWNFALL_DISTANCE` was ignored; weather radius was `fancyGraphics ? 10 : 5`, and splashes always used 10. **Fixed in batch 1:** one effective radius from `NotFineBridge` is used by storm quads and splashes; `0` disables downfall. |
| B5 | **H** | `client/weather/StormSplashRenderer.java:43-51` | Dispatch used `EnvironState.getDimensionId()` (0 before the first tick) instead of `mc.theWorld.provider.dimensionId`; the wrong dimension renderer could be used on the first frame. **Fixed in batch 1.** |
| B6 | **H** | `client/weather/StormSplashRenderer.java:90-91`, `NetherSplashRenderer.java:30` | The base `getPrecipitationHeight` ignores its `range` parameter; the nether override used `EnvironState.getPlayer()` instead of the passed view entity. **Fixed in batch 15 for the nether path:** it receives the view entity; the base remains a vanilla delegate. |
| B7 | **M** | `client/weather/Weather.java:119` | Dead `level += 0.01` removed. **Fixed in batch 49.** |
| B8 | **M** | `client/weather/Weather.java:25,111-131` | `intensity`/`intensityLevel`/texture statics were mutated from the packet handler (Netty) while the render thread read them. **Fixed in batch 3:** `PacketRainIntensity` schedules on the client thread. |
| B9 | **M** | `client/weather/StormRenderer.java:90-100` | 441 columns × biome/temperature/precipitation per frame; no cache. **Fixed in batches 37+45:** shared 20-tick `WeatherColumnCache` with cached lightmap values. |
| B10 | **M** | `client/weather/StormSplashRenderer.java:100-160` | Up to 100 attempts/frame with repeated world lookups. **Fixed in batches 37+45:** biome/height/temperature/light values come from the shared weather cache; non-nether column colors reuse immutable white. |
| B11 | **L** | `client/weather/StormRenderer.java:247-257` | `RAIN_X_COORDS/RAIN_Y_COORDS` divided by zero at the centre column (NaN vertices). **Fixed in batch 24:** minimum radius clamp and a finite-centre test. |
| B96 | **L** | `client/weather/Weather.java:99-116` | `setIntensity(0)` was a no-op while the state was `VANILLA` because the level was already `0`, so `NONE` could not be reached from a fresh state. **Fixed in batch 57:** the state is reclassified when it is `VANILLA`, even if the numeric level is unchanged; tested. |

### 3.2 Fog

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B12 | **C** | `client/FogEffectHandler.java:76-77` | Absolute fog override broke NotFine `TERRAIN_FOG`/`FOG_NEAR_DISTANCE` (Angelica #2304). **Fixed in batch 2:** relative scales applied to the current GL fog; `Respect External Fog` opt-out. |
| B13 | **H** | `client/FogEffectHandler.java:42-44` | `doFog()` was `enableBiomeFog || allowDesertFog`; elevation haze, morning fog and weather fog were ignored when those two were off. **Fixed in batch 2:** all fog options are considered. |
| B14 | **M** | `client/fog/MorningFogRangeCalculator.java:39` | `clamp(start - shift*2, 11, newEnd)` could return `11` when `newEnd < 11`, producing `start > end`. **Fixed in batch 2:** the floor is scaled and `FogResult` clamps `start <= end`. |
| B15 | **M** | `client/fog/BiomeFogRangeCalculator.java:47-80` | 41×41 = 1,681 biome lookups whenever the player moves one block (or the rain/far-plane changes). Cache on a coarser grid (e.g., 4-block cells) or only re-scan when the biome mix changes. |
| B16 | **M** | `client/fog/BiomeFogColorCalculator.java:60-79` | Same per-block 169-column rescan. **Fixed in batches 4+37+46:** NotFine `VOID_FOG` gating, 4-block coarse scan keying, and missing-chunk retry cooldown. `applyPlayerEffects` remains a local reimplementation. |
| B17 | **L** | `client/fog/BiomeFogRangeCalculator.java:13-15` | `DUST_FOG_IMPACT`/`weightMixed` were unused literals. **Fixed in batch 37:** named constants and a derived grid weight. |
| B18 | **L** | `client/fog/HolisticFogRangeCalculator.java:30-37` | `Math.min` of starts/ends could produce an invalid pair. **Fixed in batch 2:** `FogResult` clamps `start <= end` and the handler checks `isValid`. |

### 3.3 Aurora

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B19 | **H** | `client/AuroraEffectHandler.java:72` | `currentAurora.posX != ad.posX && currentAurora.posZ != ad.posZ` should be `||`. A new aurora with the same X but a different Z kept the old one. **Fixed in batch 1.** |
| B20 | **H** | `data/AuroraData.java:59` | `equals` but **no `hashCode`** → `HashSet<AuroraData>` used identity hashes, so duplicates were never deduplicated. Server could accumulate auroras every 100 ticks and broadcast all of them. **Fixed in batch 1:** `hashCode` on dimension/x/z; capping the per-dimension set is still open (B44). |
| B21 | **H** | `server/ServerEffectHandler.java:60-61` | `isAuroraInRange` subtracted `-auroraSpawnOffset` from a `posZ` that already contains the offset → the Z delta was doubled (`-2 * spawnOffset`). **Fixed in batch 1:** it now measures against the actual spawn target (`playerZ - offset`) and uses `MIN_AURORA_DISTANCE_SQ`. |
| B22 | **M** | `client/AuroraEffectHandler.java:27-75` | `currentAurora` was written outside the `synchronized (auroras)` block and read by the renderer; `addAurora` could run on the Netty thread. **Fixed in batches 1+3:** `currentAurora` is `volatile`, selection is locked, and `PacketAurora` is scheduled on the client thread. |
| B23 | **M** | `client/aurora/AuroraRenderer.java:137` | `GL11.glScaled(3.5, 25.0, 3.5)` after `tess.draw()` was a leftover; `lowY`/`lowY2` were unused. **Fixed in batch 1.** |
| B24 | **M** | `client/aurora/Aurora.java:17-22` | Unused constants removed. **Fixed in batch 49.** |
| B25 | **M** | §2.4 | Aurora is drawn inside `RAIN_SNOW` and is disabled with weather. |
| B26 | **L** | `client/aurora/AuroraRenderer.java:80-146` | Immediate-mode state was restored by enabling cull/lighting/texture unconditionally rather than restoring the previous state. **Fixed in batch 1:** `glPushAttrib(GL_ALL_ATTRIB_BITS)` / `glPopAttrib`. |

### 3.4 Sound

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B27 | **C** | `client/sound/SoundManager.java:88` | `SoundManager.update()` was only called from `PlayerSoundEffectHandler.process`, which is registered only when `enableBiomeSounds` is true. With biome sounds off, pending block/step sounds never flushed and emitters never ticked. **Fixed in batch 3:** `ClientEffectHandler.clientTick` updates the queue unconditionally. |
| B28 | **H** | `client/sound/cache/SoundCache.java:25-27,48` | Static manager/buffer, no reload invalidation, `Error` on failure, unreliable `available()`. **Fixed in batches 38+39:** lazy manager, `ThreadLocal` buffer, bounded LRU cache, reload listener, EOF read, and logger/silence fallback. |
| B29 | **H** | `client/sound/SoundManager.java:84` | Equal emitter keys could be replaced without fading. **Fixed in batch 38:** dedupe by sound name and fade removed emitters. |
| B30 | **M** | `client/sound/SoundManager.java:67-84` | `queueAmbientSounds` is O(n²) and mutates `sounds` while iterating (safe only because of the immediate `break`). Use maps/sets. |
| B31 | **M** | `client/sound/SoundManager.java:161-167` | `isSoundPlaying` scanned the whole playing map. **Fixed in batch 38:** vanilla inverse-map check + delayed map; no `containsValue`. |
| B32 | **M** | `client/sound/PlayerSound.java:30-33` | The first `volume` assignment was dead. **Fixed in batch 38.** |
| B33 | **M** | `client/sound/SoundManager.java:240-275` | `configureSound()` created/destroyed a temporary OpenAL context just to query channels. **Fixed in batch 39:** live device query, or a short device-only open/close; pure channel-splitting helper tested. |
| B34 | **M** | `client/sound/SoundEffect.java:55` | The no-pattern constructor left `pattern = null`; `matches()` NPE'd for programmatically created effects. **Fixed in batch 12:** the `.*` pattern is compiled in the string constructors. |
| B35 | **M** | `client/sound/SoundManager.java:116-118` | `canFitSound` ignored streaming channels. **Fixed in batch 38:** normal + streaming capacity, tested via a pure overload. |
| B36 | **L** | `client/sound/fix/SoundFixMethods.java` | The `ISourceRemoved` cast assumed the mixin applied. **Fixed in batch 38:** `instanceof` fallback to direct cleanup; tested. |

### 3.5 Network & server

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B37 | **C** | `network/PacketHealthChange.java`, `network/PacketAurora.java`, `network/PacketRainIntensity.java` | The handlers called `EffectRenderer.addEffect`, `AuroraEffectHandler`, and `Weather.setIntensity` directly on the Netty thread. **Fixed in batch 3:** all three use `ClientPacketHandler.schedule` (`Minecraft.func_152344_a`). |
| B38 | **H** | `server/ServerEffectHandler.java:52` | Sent `PacketRainIntensity` every `Phase.START` for every dimension. **Fixed in batch 16:** sent on change or a 40-tick keepalive, with a tested pure decision function. |
| B39 | **H** | `server/WorldHandler.java:28-31` | The replacement for `World.updateWeatherBody` dropped vanilla's `!provider.hasNoSky` guard, so the state machine ran in the End and other no-sky dimensions. **Fixed in batch 15:** it skips no-sky worlds unless `DimensionRegistry.hasWeather` opts them in, so the Nether config still works. |
| B40 | **M** | `data/DimensionEffectDataFile.java:31` | `getFile()` marked the saved data dirty on every access, so every autosave rewrote it. **Fixed in batch 16:** dirty callbacks on the real setters and aurora add/clear; repeated reads stay clean. |
| B41 | **M** | `data/DimensionEffectDataFile.java:23-33` | `world.loadItemData` ran on every tick. **Fixed in batch 41:** synchronized `WeakHashMap` cache cleared on world unload; tested. |
| B42 | **M** | `server/ServerEffectHandler.java:30,60` | `MIN_AURORA_DISTANCE_SQ`, `CHECK_INTERVAL`, `RESET` are unused; the literals `160000L`, `100`, `-10` are duplicated in `Network`/`DimensionEffectData`. |
| B43 | **M** | `network/PacketAurora.java:41-47` | `colorSet`/`preset` were serialized as bytes but held as ints. **Fixed in batch 13:** both are written/read as `int`; the codec test pins the width. |
| B44 | **M** | `server/ServerEffectHandler.java:90-95` | Aurora creation could grow without bound. **Fixed in batch 41:** 32-per-dimension cap with a tested `canAddAurora` guard; hash/range fixes from batches 1/41 handle dedupe. |
| B45 | **L** | `commands/CommandRain.java:60` | `getCommandSenderAsPlayer` meant the server console could not run `/rain status`. **Fixed in batch 17:** `ICommandSender.getEntityWorld()` and sender replies; tested with a mocked console. |
| B46 | **L** | `commands/CommandRain.java:81-86` | `/rain reload` touched client-only `BlockRegistry` and never refreshed clients. **Fixed in batch 42:** server reload + `PacketReload` to client; client-thread registry rebuild. |
| B47 | **H** | `data/DimensionEffectData.java:66-77` | `randomizeRain` ignored `minIntensity`: with `delta = max-min` and `mid = delta/2`, `rand*mid + rand*mid` produced `[0, delta]`, then clamped to `0.01`. **Fixed in batch 11:** `min + rand*(max-min)`, with tests for a non-zero minimum and the 1% floor. |

### 3.6 Registries, config and bootstrap

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B48 | **H** | `data/BiomeRegistry.java:167`, `data/BlockRegistry.java:79,86`, `client/fx/BlockEffect.java:31` | `random.nextInt(chance)` threw `IllegalArgumentException` when the configured chance was `0`. **Fixed in batch 12:** all call sites use `WeightedRandom.roll`, where `chance <= 0` means never. |
| B49 | **M** | `data/BiomeRegistry.java:187`, `data/BlockRegistry.java:69`, `client/footsteps/engine/implem/ProbabilityWeightsAcoustic.java:42` | The reverse-cumulative pick was duplicated and unguarded: negative weights could walk past index 0 and an empty table called `nextInt(0)`. **Fixed in batch 12:** one `WeightedRandom` helper, negative weights ignored, empty tables return no pick, distribution and guard tested. |
| B50 | **H** | `data/config/BiomeConfig.java:43`, `data/config/BlockConfig.java:26,41` | The JSON models accept any `Integer` for `spotSoundChance`/`chance`/`stepChance`. **Fixed in batch 12 at the use site:** zero/negative values are treated as "never" rather than reaching `nextInt`; no ambiguous clamping to "always". |
| B51 | **M** | `util/ConfigProcessor.java:58` | `Float.MIN_VALUE` as the "no minimum" sentinel clamped unannotated floats to `1.4e-45`. **Fixed in batch 13:** `-Float.MAX_VALUE`, with a negative-value config test. |
| B52 | **M** | `util/ConfigProcessor.java:19-101` | No support for `double`/`long`/enums beyond `String[]`; parse failures were logged and ignored. **Fixed in batches 27+28+30:** `double`/`long`/enum support, invalid-value fallback, and a last-errors validation report are tested. |
| B53 | **M** | `ModOptions.java:405-425` | `scrubCategory` removed any property with an empty comment. **Fixed in batch 26:** only unknown (non-annotated) properties are removed; known comment-less properties survive, with a unit test. |
| B54 | **M** | `client/gui/DynamicSurroundingsConfigGui.java:92-96` | Synthetic sound/config saves ran after `super.actionPerformed`. **Fixed in batches 50+53+59:** saves happen first, client registries/fog reload at runtime, and every static-final/handler-registration option now declares `RestartRequired` in the config UI. |
| B55 | **M** | `client/waila/WailaHandler.java:119-121` | WAILA registration was debug-gated and loaded from the common proxy. **Fixed in batch 42:** registered from `ProxyClient` only when Waila is loaded; debug gate removed. |
| B56 | **M** | `proxy/Proxy.java:31-36` | Common proxy imported client classes and initialized `DamageEffectHandler` on the server. **Fixed in batch 42:** common proxy has no client imports, server damage detection is `DamageEffectServer`, and the DTO is `network/HealthData`; client-only registries stay in `ProxyClient`. |
| B57 | **M** | `data/DimensionRegistry.java:27` | `CALENDAR_API = Loader.isModLoaded("CalendarAPI")` in a static initializer forced FML bootstrap in tests. **Fixed in batch 15:** a safe class probe, no `Loader` call. |
| B58 | **M** | `data/DimensionRegistry.java:277-305` | Flat-world sea level depended on a client-only load event. **Fixed in batch 41:** `WorldInfo.getTerrainType() == FLAT` is checked directly; tested. |
| B59 | **M** | `data/BiomeRegistry.java:118-146` | A miss rescaled the whole registry under `synchronized (registry)`, and aliases were cleared after initialization. **Fixed in batch 43:** copy-on-write snapshot, volatile reads, miss default entries after first init, and persistent aliases. |
| B60 | **M** | `data/BlockRegistry.java:28-35` | `getEffects` returned the internal mutable list and `getRandomSound` allocated a candidate list per call. **Fixed in batch 25:** cached unmodifiable view and `WeightedRandom.pick` without the candidate list. |
| B61 | **M** | `VersionCheck.java:24,30-34,40-46` | The check used a literal URL, no timeouts, a non-daemon thread, duplicated retry constants, and was enabled by default. **Fixed in batches 29+34+35:** GitHub Releases API, git-describe version source, JSON/legacy parsing, timeouts, daemon thread, named constants, configurable URL, and opt-in default. |
| B62 | **L** | `Module.java:24-34` | Version sources consolidated through `Module.VERSION`/`Tags.VERSION`; handler-less FML-bus registration removed. **Fixed in batch 49.** |
| B63 | **L** | `ModOptions.java:86,89,93` | Copy-pasted comments corrected. **Fixed in batch 49.** |

### 3.7 Client effects, environment and HUD

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B64 | **H** | `client/ClientEffectHandler.java:78-86` | `world` was checked but `player` was not; a null player during respawn/dimension change reached every handler. **Fixed in batch 23:** `canProcess(world, player)` and a unit test. |
| B65 | **H** | `client/fx/BlockEffectHandler.java:64-72` | Step sound sampled `playerPos.down(2)`, i.e. dirt under grass. **Fixed in batch 15:** `posY - 0.1`, with a position test. |
| B66 | **H** | `client/fx/JetEffect.java:83-89` | `Dust.trigger` required `world.isAirBlock(x, y-1, z)`, so dust only spawned on floating blocks. **Fixed in batch 15:** it now requires air above, like fire/fountain; tested both directions. |
| B67 | **H** | `client/fx/BlockEffectHandler.java:40-62` | ~1,000 `BlockPos` allocations and repeated work per sample. **Fixed in batch 40:** one mutable position, cached sample count, registry early-out, and allocation-free weighted sound selection. |
| B68 | **M** | `client/EnvironStateHandler.java:205-225` | 21 `CONDITION_TOKEN_*` constants and `CONDITION_SEPARATOR` were unused. **Fixed in batch 19:** removed; `getPlayerConditions` is the single literal source. |
| B69 | **M** | `client/EnvironStateHandler.java:188-470` | `EnvironState` is a static mutable god-object. **Fixed in batches 44+59:** null-safe accessors plus an immutable per-tick `EnvironmentSnapshot`; the static mutable state is gone. |
| B70 | **M** | `client/BiomeSurveyHandler.java:57-70` | The dirty check included `playerY` and re-scanned per block moved. **Fixed in batches 25+27:** Y is ignored and position movement is throttled to one scan per 20 ticks; dimension/biome changes are immediate. |
| B71 | **M** | `client/PlayerSoundEffectHandler.java:42-63` | Every tick allocated a map/list and re-ran regex matching per sound. **Fixed in batches 30+33:** weighted biome, player, and combined ambient lists are cached; `queueAmbientSounds` no longer mutates the caller list. Per-tick spot rolls remain by design. |
| B72 | **M** | `client/hud/PotionHUD.java:30-100` | Null player, no state restore, per-frame formatting. **Fixed in batches 4+44:** dispatcher filter, null guard, `glPushAttrib`/`glPopAttrib`, and `PotionTextCache`. |
| B73 | **M** | `client/PotionParticleScrubHandler.java:8-11` | Wrote the potion liquid colour datawatcher every tick. **Fixed in batch 31:** removed; a client-only mixin cancels the local player `mobSpell` spawn when enabled. |
| B74 | **M** | `client/fx/particle/EntityTextPopOffFX.java:59-89` | Immediate-mode font draw and depth-func state leak; see §2.5. **Fixed in batch 7:** render-less marker + post-world renderer with pushed attributes and an Iris `PARTICLES` phase. |
| B75 | **L** | `client/hud/GuiHUDHandler.java:36-40` | The dispatcher forwards all overlay phases to all overlays; filter by `ElementType` in the dispatcher. |
| B76 | **L** | `client/EnvironStateHandler.java:104-109` | `craftSoundThrottle` made the first craft depend on the tick counter. **Fixed in batch 44:** initialized to `Integer.MIN_VALUE`. |

### 3.8 Footsteps / sound engine

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B77 | **H** | `client/footsteps/game/system/PFSolver.java:49` | `Math.abs(motionY) < 0.02` returned null for every grounded player with zero vertical motion, suppressing flat-ground footsteps. **Fixed in batch 20:** only stationary airborne players are skipped; ground/water/ladder cases are tested. |
| B78 | **M** | `client/footsteps/game/system/PFReaderQP.java:50-85` | Dead branches and unused fields in a class never instantiated. **Fixed in batch 19:** class removed. |
| B79 | **M** | `client/footsteps/mcpackage/implem/AcousticsManager.java:126-135` | `newMinimum` was assigned every remaining sound instead of the minimum, and `randAB` sampled `[a, a+b]` instead of `[a, b]`. **Fixed in batch 14:** `Math.min` for the wake-up and a bounded `randomDelay`, with tests. |
| B80 | **M** | `client/footsteps/mcpackage/implem/AcousticsManager.java:40-43` | `USING_LATENESS`, `USING_EARLYNESS`, `LATENESS_THRESHOLD_DIVIDER`, `EARLYNESS_THRESHOLD_POW` unused; the literals are repeated. |
| B81 | **M** | `client/footsteps/mcpackage/implem/LegacyCapableBlockMap.java:15-35` | Only `^` and `.` were treated as legacy separators; `id:meta` keys failed. **Fixed in batch 17:** `:`, `^`, and `.` are accepted; `1:0`, `1^0`, `1.0`, and bare IDs are tested. |
| B82 | **M** | `client/footsteps/mcpackage/implem/BasicBlockMap.java:97-120` | The regex is fragile; `BlockInfo.hashCode` NPE'd on a null block and `equals` cast without `instanceof`; `getBlockMap` reuses a mutable key. **Fixed in batches 18+22+26:** null-safe `BlockInfo` hash/`instanceof` equality, an anchored `parseKey` parser, and a `ThreadLocal` lookup key. The regex/key work is covered by tests. |
| B83 | **M** | `client/footsteps/game/system/Footsteps.java:74-166` | Empty streams threw `NoSuchElementException` and malformed JSON was swallowed. **Fixed in batch 23:** empty streams are skipped, loaders catch runtime parse failures, and `AcousticsJsonReader` logs through `ModLog`. |
| B84 | **L** | `client/footsteps/game/system/Footsteps.java:180-195` | `nextStepDistance = Integer.MAX_VALUE` was written every tick. **Fixed in batch 23:** write only when the value changes. |
| B85 | **L** | `client/footsteps/parsers/AcousticsJsonReader.java:45-100` | Missing fields/empty sound names/delayed bounds produced NPEs or IndexOutOfBounds. **Fixed in batch 18:** descriptive `JsonParseException`s and direct parser tests. |
| B86 | **L** | `client/footsteps/util/property/simple/ConfigProperty.java:100-110` | `JsonUtils.load(..., Map.class)` returns `null` for a JSON `null`, and the unchecked cast hid it. **Fixed in batch 18:** null becomes an empty map and `loadStream` returns false. |

### 3.9 Utilities / data

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B87 | **H** | `compat/BlockPos.java:145-220` | No `equals`/`hashCode`; the box iterators compared with `equals` and never terminated; `MutableBlockPos.getImmutable()` copied the base class's private fields (always 0). **Fixed in batch 11:** coordinate `equals`/`hashCode`, getter-based copy construction, and tests for both box iterators and mutable immutability. |
| B88 | **H** | `util/Color.java:222-250` | `ImmutableColor` overrode `scale`, `mix` and `adjust` but not `add`, `blend`, or `scale(r,g,b)`, so `Color.RED.add(...)` could mutate a shared static constant. `Color` had `equals` but no `hashCode`. **Fixed in batch 11:** every mutator is rejected and the hash contract is tested. |
| B89 | **M** | `util/MathStuff.java:139-144` | Precision and `MIN_VALUE` issues. **Fixed in batches 13+17+24+49:** sqrt/double trig/log use the JDK and `abs(MIN_VALUE)` saturates to `MAX_VALUE`. |
| B90 | **M** | `data/DimensionEffectData.java:30-32,88-95` | `NBT` inner classes and `MIN_INTENSITY`/`MAX_INTENSITY` were unused; `FORMATTER` was not thread-safe. **Fixed in batches 19+32:** dead data removed and formatting uses `String.format`. |
| B91 | **M** | `data/AuroraData.java:59-67` | Identity semantics were undocumented. **Fixed in batch 49:** column identity is documented, `hashCode` matches, and `sameColumn` is tested. |
| B92 | **M** | `util/PlayerUtils.java:20-24,60-84` | `isReallyInside` used a 2-block ceiling heuristic. **Fixed in batches 20+49:** uses `World.canBlockSeeTheSky` over the survey area; tall roofed rooms count as inside. |
| B93 | **M** | `VersionCheck.java:157-210` | `assert` validation was disabled in production and `compareTo` ignored alpha/beta. **Fixed in batch 29:** explicit `IllegalArgumentException`s, alpha < beta < release ordering, and parse/compare tests. |
| B94 | **L** | `util/JsonUtils.java:40-75` | `load(InputStream)` could return `null`; `clazz.newInstance()` was deprecated. **Fixed in batches 21+32:** null stream/JSON null fallbacks and `getDeclaredConstructor().newInstance()`; tested. |
| B95 | **L** | `ModLog.java` | `error` printed a second stack trace and did not pass the throwable to Log4j; `catching` printed twice. **Fixed in batch 19:** both delegate to Log4j with the throwable. |

### 3.10 Exact line references for the top items

- `src/main/java/com/mrfuzzihead/dynamicsurroundings/mixins/early/EntityRendererMixin.java:30`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/client/FogEffectHandler.java:42,76`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/data/BiomeRegistry.java:167,187`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/data/BlockRegistry.java:69,79,86`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/client/footsteps/engine/implem/ProbabilityWeightsAcoustic.java:42`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/data/DimensionEffectData.java:73`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/network/PacketHealthChange.java:33`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/server/ServerEffectHandler.java:52,60`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/client/sound/SoundManager.java:84,88`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/client/AuroraEffectHandler.java:72`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/client/fx/BlockEffectHandler.java:68`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/client/fx/JetEffect.java:83`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/util/ConfigProcessor.java:58`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/util/Color.java:222`
- `src/main/java/com/mrfuzzihead/dynamicsurroundings/compat/BlockPos.java:145`

---

## 4. Performance work

Ordered by expected win. Each item is independent and testable.

| # | Hotspot | Current cost | Fix |
|---|---|---|---|
| P1 | `ServerEffectHandler` rain packet | 20 packets/s/dimension, always | Send only on change (compare last sent value), on join, and at a low keep-alive rate (e.g., 20 ticks). |
| P2 | `StormRenderer` + `StormSplashRenderer` world lookups | 441 columns × (biome + temperature + precipitation + light) per frame, up to 100 more per frame for splashes | **Done in batch 37:** shared `WeatherColumnCache` keyed by column/dimension/20-tick bucket; light remains uncached. |
| P3 | `BiomeFogRangeCalculator` | 1,681 biome lookups on every block move / rain / far-plane change | **Done in batches 37+46+47:** 4-block cells, 20-tick missing-chunk retry, and biome-mix signature invalidation. |
| P4 | `BlockEffectHandler` | ~1,000 samples/tick × 2 block reads + allocations + weighted-list allocation | Mutable `BlockPos`, one `getBlock`+metadata read, allocation-free weighted pick, and an early-out when no block effects/sounds are configured. Optionally sample 1/2 or 1/4 of the volume per tick on a rolling schedule. |
| P5 | `PlayerSoundEffectHandler` | New maps/lists/scaled effects every tick; regex per sound | Cache the weighted sound list per (conditions, survey revision); reuse lists; precompile `SoundEffect` patterns. |
| P6 | `BiomeSurveyHandler` | 169 biome lookups per block moved (including Y changes) | Drop Y from the key, throttle to 20 ticks/area change, skip if the player hasn't moved a block. |
| P7 | `SoundCache` | Unbounded retention of every played ogg; 64 KiB static buffer | **Done in batches 38+39:** `ThreadLocal` buffer, reload clear, and a 256-entry LRU. |
| P8 | `SoundManager` | O(n²) `containsValue`/`contains`/`remove` per tick | **Done in batches 38+39:** removed `containsValue`, emitter dedupe by name, and `SoundEffect.hashCode`. |
| P9 | `PotionHUD` | `I18n.format`, `getStringWidth`, string building every frame | Cache per `PotionEffect` and refresh when the displayed second changes. |
| P10 | Config GUI | Builds 570 Boolean + 570 Integer properties on open | Virtualize/lazy-load the sound lists, or move per-sound volume to a separate searchable screen. |
| P11 | `DimensionEffectDataFile` | `loadItemData` + `markDirty` per tick per world | Cache per world; mark dirty on mutation. |
| P12 | `WorldHandler` | Static `XorShiftRandom` shared, `DimensionEffectDataFile` per tick | Fine, but pass an RNG or a `WeatherTicker` for deterministic tests; cache the data. |
| P13 | `Aurora` | Per-frame trig over ~3 × 128 nodes | Precompute the band template per seed; only update offsets when `auroraAnimate`; use `MathStuff` (already). |
| P14 | `AuroraRenderer` | Immediate-mode + attribute state churn | A small static VBO / `Tessellator` buffer with the geometry rebuilt only when the node set changes; batch all three bands into one draw. |
| P15 | `EnvironState.tick` | Regex per biome + string concat per tick | **Done in batches 48+54+57:** dimension/player condition strings are cached, and biome config regexes are compiled once per reload. |
| P16 | `VersionCheck` | Blocking HTTP on a non-daemon thread | Timeouts, daemon thread, optional. |
| P17 | `SoundManager.configureSound` | AL create/destroy | **Done in batch 39:** live-device query with a device-only fallback. |
| P18 | `ForgeDictionary` | Repeated ore-dictionary lookups on every reload | **Done in batch 57:** `OreLookupCache` reuses per-ore `ItemStack` lists; `invalidate()` clears them when the ore dictionary changes. |

---

## 5. Refactors (structure, testability, maintainability)

1. **Split the per-tick environment state.** **Done in batch 59:** immutable `EnvironmentSnapshot` built once per tick; `EnvironState` delegates to it and the fog/weather/sound/block/footstep consumers read it.
2. **One weather backend.** `WeatherBackend` owns provider/angelica/DS/vanilla selection and exposes `renderOverlay`, `spawnSplashes`, `ownsWeather`. `RenderWeather` becomes an ordered list of DS renderers used only by the DS backend. See §2.2.
3. **One fog result type with a mode.** `FogResult { mode: ABSOLUTE | SCALE, start, end, density }`; calculators return SCALE by default. This is the enabler for NotFine/Angelica compatibility and for tests.
4. **One weighted random helper.** `WeightedRandom.pick(List<T>, ToIntFunction<T>, Random)` used by all three call sites; statistical tests with a fixed seed.
5. **Side-safe proxies.** `Proxy` should not import `client.*`. Move `DamageEffectHandler.initialize`, `WailaHandler.register` and `SoundRegistry` client bits into `ProxyClient`; create a `ServerPacketSender` for the health/rain packets. `DamageEffectHandler` should be split into `client/…/DamageEffectClient` and `server/…/DamageEffectServer`.
6. **Rename the clashing classes.** **Done in batch 60:** `DynamicSoundManager` and `RgbColor`; no Minecraft or `java.awt` name collision remains.
7. **Registry lifecycle.** **Done in batches 42+61:** `RegistryManager` handles named init/reload, server reload broadcasts `PacketReload`, and client reload rebuilds fog/sound.
8. **Config schema.** **Done in batch 62:** `ConfigSchema` owns category metadata and unknown-option reporting; `ConfigProcessor` remains the compatibility bridge.
9. **Resource loading.** **Done in batch 62:** `ResourceLoader` centralizes the resource-pack stream lifecycle and error isolation; the four footsteps copies are gone.
10. **Mixins.** **Done in batches 1+60:** weather ownership/order checks, an exclusion test for the mixin package, and a `@Unique`/`@Shadow` field audit.
11. **Immutable value types.** `Color`, `AuroraData` (or document identity), `FogResult`, `AuroraPreset` (already immutable), `SoundEffect` (make `pattern` non-null).
12. **Deterministic time/random.** Inject a `Clock`/`Random` into `AcousticsManager`, `WorldHandler`, `AuroraEffectHandler`, `EnvironState` so tests can run without sleeping or seeding global random state.
13. **Logging.** Use `ModLog`/Log4j parameterized calls and pass throwables; replace `printStackTrace` (10 occurrences) and empty `catch` blocks in resource loaders with `ModLog.warn(..., e)`.
14. **Build hygiene.** **Done in batch 62:** the test/game-test/JaCoCo harness lives in `gradle/test-harness.gradle.kts`, applied from `addon.gradle.kts`.

---

## 6. Dead code / cleanup

Confirmed unused (or production-dead) items to remove after the refactors land:

- **`BlockPos`**: `getAllInBox`, `getAllInBoxMutable`, `getImmutable`, `fromLong` (only tests use it), `ORIGIN` maybe; the class has no `equals`/`hashCode` and the test documents the broken iterators.
- **`EnvironStateHandler`**: 21 `CONDITION_TOKEN_*` constants + `CONDITION_SEPARATOR`; `CONDITION_TOKEN_MINECART/HORSE/BOAT/PIG` also duplicated as literals.
- **`DimensionEffectData`**: `NBT` inner class, `MIN_INTENSITY`, `MAX_INTENSITY`; `DimensionEffectDataFile.NBT`; `AuroraData.NBT`.
- **`Aurora`**: `ANGLE1`, `FADE_LIMIT`, `AURORA_SPEED`, `AURORA_AMPLITUDE`, `AURORA_WAVELENGTH`; **`AuroraRenderer`**: `lowY`, `lowY2`, the `glScaled(3.5, 25, 3.5)` line.
- **`PFReaderQP`**: `USE_FUNCTION`, the `if (2 == …)` branches and unused locals. The class is never instantiated (`Footsteps` always uses `PFReaderH`); either wire a config for quadrupeds or delete it.
- **`AcousticsManager`**: `USING_LATENESS`, `USING_EARLYNESS`, `LATENESS_THRESHOLD_DIVIDER`, `EARLYNESS_THRESHOLD_POW`.
- **`PlayerUtils`**: `INSIDE_Y_ADJUST`, `RANGE`, `AREA` (literals used).
- **`BiomeSurveyHandler`**: `BIOME_SURVEY_RANGE` (literal 6).
- **`ServerEffectHandler`**: `MIN_AURORA_DISTANCE_SQ`, `CHECK_INTERVAL`, `RESET`.
- **`WorldHandler`**: `STRENGTH_ADJUST` (literal 0.01F).
- **`VersionCheck`**: `VERSION_CHECK_RETRIES`, `VERSION_CHECK_INTERVAL`, `REMOTE_VERSION_FILE` (literal URL), `CURSE_PROJECT_NAME`, `MOD_NAME_TEMPLATE` (literals in `register`).
- **`MathStuff`**: `RAD_TO_INDEX`, `DEG_FULL`, `DEG_TO_INDEX`, `COS_TO_SIN`, `RAD_TO_DEG`, `DEG_TO_RAD`, `PI_F`, `E_F` (verify test usage before removing public constants).
- **`ModOptions`**: many `CONFIG_*` constants are only used by the GUI; prune or keep as the schema. `SOUND_QUEUE_SLACK`/`AGE_THRESHOLD_TICKS` are used as literals.
- **`StormSplashRenderer`**: `PARTICLE_SOUND_CHANCE` (literal 20), `RANGE` (literal 10); `GENERATOR` is used.
- **`BiomeFogRangeCalculator`**: `DUST_FOG_IMPACT` (literal 0.9F).
- **`RgbColor`**: duplicate `MC_BLACK`/`MC_WHITE` removed in batch 60; verify any remaining unused palette constants before pruning.
- **Assets**: the six unreferenced ogg files were removed in batch 60.
- **`variator.json` / variator loader**: removed in batch 60; `NormalVariator` defaults remain.
- **`RenderWeather.register`**: removed in batch 60; the static list is populated directly.
- **`WailaHandler`**: made an explicit singleton in batch 60.
- **`Module`**: self-registration on the FML bus removed in batch 49.

---

## 7. Features worth adding (after stability)

1. **Angelica instanced-weather provider** — the flagship compat feature: DS's per-biome rain/dust/snow becomes a `WeatherColumnProvider`, so Angelica keeps its 1-draw instanced pipeline and DS keeps biome awareness. Needs `WeatherColumnCache` + the bridge in §2.10.
2. **NotFine settings bridge + compat page** — surface the effective `downfallDistance`, terrain fog state and detected versions in the DS config GUI; a "Compatibility" category with `respectExternalFog`, `deferWeatherToAngelica`, `useAngelicaWeatherProvider`, `bopFogSource`.
3. **Angelica debug entries** — register DS's diagnostics (dimension, weather intensity, fog range/color, aurora, sound counts) with `DebugScreenApi` when present, instead of the `RenderGameOverlayEvent.Text` debug list. Better F3 integration and no HUD-cache interaction.
4. **`/ds` command** — `status`, `compat`, `weather set <0-100>`, `aurora spawn`, `fog debug`, `reload` (broadcast to clients). The existing `/rain` is server-only and player-only.
5. **Fog disable** — users repeatedly ask for a global fog off switch; add `fog.enable` and/or let NotFine's `TERRAIN_FOG=false` be authoritative.
6. **Weather smoothing/cross-fade** — the intensity state machine currently jumps between NONE/CALM/LIGHT/NORMAL/HEAVY based on thresholds; add hysteresis or a fade to avoid texture/sound pops.
7. **Aurora quality/visibility options** — per-shader toggle, max auroras, "only when looking north", and a client-side debug wireframe for the node mesh.
8. **Footstep pack authoring** — a `pf_pack.json` validator command and a `/ds footstep debug` overlay showing association, substrate and acoustic chosen.
9. **Sound cache controls** — a config for cache size/disable, and a diagnostic for cache hits/misses.
10. **Optional Better Rain/Weather2 integration** — the weather ownership backend makes it easy to add a "defer to other weather mods" policy.

---

## 8. Test coverage plan

### 8.1 Fix the metric first

**Implemented in batch 57.** The floor now covers 159/177 classes (90 %) instead of excluding all client code: renderers, handlers, GUI/HUD, audio-cache and biome-grid scans are the only exclusions. Global floors are 50 % line / 35 % branch, with per-package floors for the logic-heavy packages. The original plan was:

1. **Narrow the JaCoCo exclusion** from `client/**` to the actual GL/audio-bound classes, e.g. `client/**Renderer*`, `client/**Handler*` (only those that touch `GL11`/`Tessellator`), `client/gui/**`, `client/hud/**`, `client/fx/particle/**`, `client/waila/**`, `client/sound/cache/**` (or leave the cache testable with mocks). Include pure classes such as `FogResult`, `Aurora`/`Node` geometry, `Weather` (after splitting the state from Minecraft), `SoundEffect`, `ConfigProperty`, `AuroraPreset`, `ColorPair`, `MathStuff`, `PlayerUtils` (after extracting a world-access interface), `BiomeRegistry`/`BlockRegistry` weighted pick (after extraction).
2. **Better: move pure logic to a `core/`/`logic/` package** (no `client` in the name) so the coverage configuration stays simple and the code is obviously side-neutral. E.g. `logic/fog/FogResult`, `logic/aurora/AuroraGeometry`, `logic/weather/WeatherState`, `logic/sound/WeightedRandom`.
3. **Add a `jacocoGameTestReport` sanity check** — the current report is empty; fail the build when `gameTest.exec` exists but the XML has no counters. This makes the in-game number trustworthy.
4. **Ratchet per package** — set floors for `util`, `data`, `logic` and the total, and raise them as tests land.

### 8.2 Harness additions

- **Static mocking** (`mockito-inline`) for `Minecraft`, `GL11`, `Tessellator`, `OpenGlHelper` in a `ClientTestSupport` helper, so client-side classes that mainly compute can be tested without a GL context.
- **Fake world/player builders**: a small `FakeWorld` implementing the handful of `World` methods used by fog/weather/survey (or Mockito deep stubs), plus `FakePlayer` with pos/motion/biome/potion state.
- **Deterministic clock/random** injection (see refactor 12), plus `@Rule ExpectedException` where appropriate.
- **Golden JSON tests** for the shipped data files: every configured sound event exists in `sounds.json`; every `sound` name referenced in `data/**` resolves; every `blocks.json` block id resolves at runtime (game test); every `dimensions.json` dimension key is valid; no duplicate keys.
- **Property-based tests** for `MathStuff` vs `Math`, `Color` round trips, `BlockPos` serialization, and `SoftwareVersion` comparisons (quickcheck-style loops are enough).
- **Mixin tests**: extend `MixinInjectionPointTest` to assert exactly one `20` constant, the `renderRainSnow` cancel, the new `renderWorld` aurora hook, and the `@ModifyArg`/`@Redirect` targets used by the compat bridges. Add a test that every `Mixins` entry class exists, is annotated `@Mixin`, and is either in `@TransformerExclusions` or a real mixin.
- **Game test additions**: `DimensionEffectDataFile` persistence round trip across save/reload; rain packet send-on-change (capture `Network` traffic with a fake network or count `SimpleNetworkWrapper` sends); aurora dedupe/cap; `WorldHandler` no-sky gate; `PlayerSleepHandler`; `/rain` console path; `Weather.setIntensity` state machine driven by packets.
- **Client smoke test**: a `-PclientTest` run that launches the client, opens a flat world, forces rain/night, runs 200 frames, and asserts no `glGetError`, no exception in `latest.log`, and the expected phase ownership. This is the only way to catch the Angelica/Iris integration issues automatically.

### 8.3 Test backlog by module

| Area | Tests to add | Priority | Notes |
|---|---|---|---|
| Weighted selection | Fixed-seed distribution for `[1, 100]`, `[10, 1]`, single entry; negative/zero weight guards; assert no AIOOBE | **P1** | The current algorithm is correct for positive weights; the test pins it and the extraction adds validation |
| `DimensionEffectData.randomizeRain` | `min=0.5, max=1.0` → always ≥ 0.5; `min=max`; `min=0,max=0.01` floor | **P0** | Current test misses the bug |
| `FogResult` / fog calculators | Scale semantics; `MorningFog` start≤end; `Haze` boundaries; `WeatherFog` rain=0/1; `BiomeFog` cache invalidation | **P0** | Needs `FogResult` refactor |
| `EntityRendererMixin` behaviour | Assert `renderRainSnow` is cancellable and cancels; ownership matrix (DS/Angelica/provider) | **P0** | Add a pure `WeatherBackend` test |
| `Color` | `ImmutableColor` rejects every mutator; `hashCode` contract; `rgb` bounds; `blend`/`luminance` | **P1** | |
| `BlockPos` | Either delete the iterators or test termination + `equals`/`hashCode` + `getImmutable` | **P1** | Documented broken |
| `ConfigProcessor` | Every type; min/max clamps; negative float with no annotation; restart flags; hidden; unknown option | **P1** | |
| `SoundCache` | <64 KiB cached, ≥64 KiB streams, missing resource → silence, loader invalidation, concurrent load, fallback URL | **P1** | Mock `IResourceManager` |
| `SoundManager` | Duplicate sound-name replacement, queue/age, capacity, `update` called with biome sounds off | **P1** | |
| `Packet*` | Round trip, wrong dimension, byte truncation, off-thread handling | **P1** | |
| `AuroraData`/`AuroraPreset`/`ColorPair` | `HashSet` dedupe, identity, clamping, deterministic seed | **P1** | |
| `Aurora`/`Node` geometry | Node count/length per preset, band count option, symmetry, fade alpha, moon factor | **P1** | Extract geometry from GL |
| `Weather` state machine | Thresholds, `VANILLA` sentinel, texture selection, volume, `setIntensity` edge values | **P1** | Needs Minecraft mock |
| `PlayerUtils` | Water/river/ocean/deep, underground/space/clouds boundaries, inside coverage | **P2** | |
| `BiomeRegistry` | Regex/alias matching, reset semantics, weighted spot sounds, zero chance | **P2** | |
| `BlockRegistry` | Reset flags, weighted sounds, zero chance, unknown block | **P2** | |
| `BasicBlockMap`/`LegacyCapableBlockMap` | `1:0`, `1^0`, `1.0`, macros, missing blocks, substrate | **P2** | |
| `AcousticsManager` | Pending scheduling (min), late/early tolerance, `randAB` bounds | **P2** | Inject clock |
| `ProbabilityWeightsAcoustic` | Distribution, odd array, negative weights | **P2** | |
| `PFReaderH` | Cadence, jump/land, immobile, stair/water, motionY gate | **P2** | Needs fake player |
| `PlayerSound`/`SpotSound` | Fade, volume scale, age, location | **P2** | |
| `ConfigProperty`/`JsonUtils` | Missing/type/empty/null/invalid JSON, modId sanitisation | **P2** | |
| `MathStuff` | Compare to `Math` over ranges; `sqrt(double)` precision; MIN_VALUE abs; atan2 error | **P2** | |
| `VersionCheck` | Parse alpha/beta/short/bad; compare; thread timeouts | **P3** | |
| `CommandRain` | Console sender, missing args, clamps, reload broadcast | **P3** | |
| `ServerEffectHandler` | Send-on-change, join packet, per-dimension counters | **P2** | Extract a `RainBroadcaster` |
| `WorldHandler` | No-sky gate, intensity window, deterministic RNG | **P2** | Extract `WeatherTicker` |
| HUD | Potion HUD filter, null player, state restore | **P3** | Minecraft mock |

### 8.4 What not to unit-test

Do not try to unit-test `GL11`/`Tessellator` draw calls directly. Extract the math (quad coordinates, UVs, colors, brightness, scales) into pure functions/records and test those; leave the actual draw to the client smoke test/game test. This is also better for performance because the extracted functions can be benchmarked.

---

## 9. Suggested roadmap for this branch

**Phase 0 — safety net (PR 1)**
- Fix the coverage metric configuration + add the missing high-value unit tests (weighted selection, `randomizeRain`, `Color`, `ConfigProcessor`, `SoundCache`, packets).
- Add the client smoke/game test skeleton and the empty-report guard.

**Phase 1 — regressions and stability (PR 2–4)**
- Fix B1/B2 (cancel weather + single provider owner), B3 (aurora not gated on weather), B19/B20/B21 (aurora identity/range), B37 (off-thread client packets), B27 (sound update), B38/B40 (server packet/dirty).

**Phase 2 — compat layer (PR 5–7)**
- `compat/Compatibility`, `NotFineBridge`, `AngelicaBridge`, `WeatherBackend`; relative fog; NotFine downfall distance; BOP fog source; diagnostics.
- Angelica `WeatherColumnProvider` bridge + the shared weather column cache.
- Iris phase for the aurora; text pop-off overlay.

**Phase 3 — performance (PR 8–10)**
- P1/P2/P3/P4/P5 (network, weather cache, fog cache, block effects, sound lists).
- Sound cache bound/reload; config GUI virtualisation.

**Phase 4 — refactors/cleanup (PR 11+)**
- Environment snapshot, registry lifecycle, side-safe proxies, renames, dead-code removal, version single-source.

Keep each PR small and keep the game test + new compat job green. The compatibility job should be allowed to be non-blocking for the first few PRs, then promoted to required once the ownership/fog fixes land.

---

## 10. Appendix — references used

- Angelica (master): `src/main/java/com/gtnewhorizons/angelica/api/weather/*`, `render/WeatherRenderer.java`, `api/glstate/GLState.java`, `mixins/early/angelica/rendering/MixinEntityRenderer_Weather.java`, `mixins/early/notfine/toggle/MixinEntityRenderer.java`, `mixins/early/notfine/toggle/MixinEntityRenderer$RenderRainSnow.java`, `glsm/.../GLSMQueries.java`, `glsm/.../GLSMFog.java`, `client/rendering/AngelicaFogService.java`, `rendering/particles/ParticleInstancer.java`, `hudcaching/HUDCaching.java`, `jss/notfine/core/Settings.java`, `jss/notfine/core/SettingsManager.java`.
- Angelica issue #2304 “Dynamic Surroundings disables fog settings”.
- Forge 1.7.10 decompiled sources (`build/rfg/minecraft-src/...`): `EntityRenderer.renderRainSnow/setupFog/updateFogColor`, `World.updateWeatherBody`, `WorldProvider.resetRainAndThunder`, `EntityViewRenderEvent`.
- This repository’s `addon.gradle.kts`, `.github/workflows/game-test.yml`, `src/test`, `src/gameTest`, and `git show HEAD~2:.../asm/PatchEntityRenderer.java` for the transformer semantics.

---

## 11. Remaining work register (after batch 56)

**Baseline:** `./gradlew check` green with **328 unit tests / 81 suites**; `gameTestServer` **79/79**; production code has no `System.out`/`System.err`/`printStackTrace`. **No known headless-testable correctness bug remains.**

This register is authoritative where an earlier table still describes a finding as open even though a later batch fixed it (notably B15, B25, B30, B75, B80 and P1/P5/P6/P9/P11/P12/P16). The dead-code list in §6 also predates batches 11–49 and must be re-verified item by item rather than removed wholesale.

**Priority:** **P0** = release blocker; **P1** = should land before release if possible; **P2** = schedule after release; **P3** = opportunistic/cleanup.
**Severity:** **C**ritical / **H**igh / **M**edium / **L**ow, as defined in §3.
**Automation:**
- **A — headless:** unit/static/golden/bytecode tests or a normal `check` run; no game launch.
- **B — scriptable client/game harness:** needs a client or dedicated server launch, but assertions are mechanical (log, GL error, owner, packet, saved data).
- **C — manual/visual:** needs a human to inspect fog, aurora, shaders, or UI feel; screenshots/logs can support but not replace it.
- **D — design/refactor:** no direct behavioural oracle; acceptance is review, compile, existing tests, and targeted benchmarks.

### 11.1 P0 — release blockers

No P0 item is fully headless: the remaining release risk is integration behaviour, not a known unit-level defect.

#### Needs a client/game harness (B)

| ID | Item | Sev | Notes |
|---|---|---|---|
| C1 | **Client smoke harness** | H | Launch a flat world, force rain/night, run ~200 frames, assert no `glGetError`, no exception/Mixin error in `latest.log`, and the expected `WeatherBackend.Owner`. This is the automation half of the matrix and should be a separate CI job. |
| C2 | **Automate the ownership/fog assertions** | H | In the smoke harness, assert DS is not cancelling when Angelica owns weather, and read effective `GL_FOG_START/END` when NotFine near=1 % / `TERRAIN_FOG=false`. |

#### Manual/visual (C)

| ID | Item | Sev | Notes |
|---|---|---|---|
| C3 | **Real compatibility matrix** | H | The only remaining release gate: Angelica instanced weather **on/off**, NotFine near fog **1 %** and terrain fog off, an Iris shader pack (aurora + rain + text pop-offs), BOP + Angelica fog, and both mixin orders. Compare against vanilla/Angelica screenshots and logs. |
| C4 | **Config GUI behaviour after save** | M | Verify B54's runtime reload actually refreshes biome/block/dimension/sound/fog without a world reload, and that restart-bound options are clearly marked. Static-final sound tables and handler registration still require a restart. |

### 11.2 P1 — high priority

**R1–R10 are complete in batches 57–59.** No P1 items remain.

#### Fully automatable (A)

| ID | Item | Sev | Notes |
|---|---|---|---|
| R1 | **Fix the coverage metric** | M | **Complete (batch 57):** exclusions narrowed to GL/audio/client-bound classes; 152/177 classes measured; 50 % line / 35 % branch global floors plus per-package floors. |
| R2 | **Guard the game-test report** | M | **Complete (batch 57):** `verifyGameTestReport` fails `check`/`gameTestServer` when `gameTest.exec` exists but the XML has no counters; the report now uses `build/classes/java/main`. |
| R3 | **Remaining pure/client-logic tests** | M | **Complete (batch 57):** added Aurora/Node, Weather state, PFReaderH, PlayerSound/SpotSound, ProbabilityWeightsAcoustic, and morning/haze fog tests; Weather `NONE` bug fixed (B96). |
| R4 | **ForgeDictionary cache (P18)** | L | **Complete (batch 57):** `OreLookupCache` reuses per-ore lists across reloads; `invalidate()` clears the cache. |
| R5 | **P15 residual regex work** | L | **Complete (batch 57):** biome config regexes are compiled once per reload and reused for every biome/alias. |

#### Needs a client/game harness (B)

| ID | Item | Sev | Notes |
|---|---|---|---|
| R6 | **Game-test additions** | M | **Complete (batch 58):** dimensions/blocks data checks, rain first-send/change/keepalive decisions, and `DimensionEffectDataFile` NBT persistence; game test is 79/79. |

#### Manual/visual (C)

| ID | Item | Sev | Notes |
|---|---|---|---|
| R7 | **Config GUI virtualisation (P10)** | M | Opening the GUI still builds ~570 Boolean + ~570 Integer properties. Lazy-load sound lists or move per-sound volume to a searchable sub-screen. Needs UI/perf judgement. |
| R8 | **Aurora template + VBO (P13/P14)** | M | Precompute the band template per seed, animate only offsets, and replace immediate-mode draws with a small rebuilt-on-change buffer. Verify visually under Iris and with `auroraAnimate` off. |

#### Design/refactor (D)

| ID | Item | Sev | Notes |
|---|---|---|---|
| R9 | **`EnvironmentSnapshot` refactor (B69)** | M | **Complete (batch 59):** immutable snapshot built once per client tick; `EnvironState` delegates to it and fog/weather/sound/block/footstep consumers read it; tested. |
| R10 | **Finish B54's runtime contract** | M | **Complete (batch 59):** every init-time/handler-registration option now declares `RestartRequired` (including `blockedSounds`), so the config UI reports it consistently; `ModOptionsTest` pins the list. |

### 11.3 P2 — medium priority

**M1–M11 are complete in batches 60–62.** No P2 items remain.

#### Fully automatable (A)

| ID | Item | Sev | Notes |
|---|---|---|---|
| M1 | **Mixin hardening (§2.9/#10)** | L | **Complete (batch 60):** `MixinClassAuditTest` enforces `@Mixin`/`@TransformerExclusions` and `@Unique`/`@Shadow` fields; weather `order=900` remains. |
| M2 | **Rename clashing classes (#6)** | L | **Complete (batch 60):** `util.Color` → `RgbColor`, `client.sound.SoundManager` → `DynamicSoundManager`; all references and tests updated. |
| M3 | **P4 optional rolling block sampling** | L | **Complete (batch 60):** block effects sample half the volume per tick via a tested count helper, with a minimum of one sample. |
| M4 | **Dead-code/assets sweep (§6)** | L | **Complete (batch 60):** removed unused oggs/variator path, duplicate color constants, public `RenderWeather.register`, and made `WailaHandler` an explicit singleton. |
| M5 | **Golden/schema data tests** | L | **Complete (batch 60):** strict duplicate-key validation for all shipped JSON; runtime block-id resolution is covered by R6. |
| M6 | **Property-based loops** | L | **Complete (batch 60):** seeded loops for `RgbColor`, `BlockPos`, `MathStuff` trig, and `SoftwareVersion` ordering. |

#### Needs a client/game harness (B)

| ID | Item | Sev | Notes |
|---|---|---|---|
| M7 | **Registry lifecycle abstraction (#7)** | M | **Complete (batch 61):** `RegistryManager` owns named reloads, with block registration on the client; proxy init, `PacketReload`, `/rain reload`, and runtime reload all use it. |

#### Manual/visual (C)

| ID | Item | Sev | Notes |
|---|---|---|---|
| M8 | **Compatibility diagnostics expansion** | L | Show detected mod versions, effective fog start/end, NotFine settings, and backend owner in one place (debug screen already has the owner/dimension/biome/weather subset). Useful for future reports, not a blocker. |

#### Design/refactor (D)

| ID | Item | Sev | Notes |
|---|---|---|---|
| M9 | **Config schema (#8)** | M | **Complete (batch 62):** `ConfigSchema` centralizes category comments/order/flags and logs unknown options before scrubbing; field annotations remain the compatibility bridge. |
| M10 | **ResourceLoader consolidation (#9)** | M | **Complete (batch 62):** `ResourceLoader.forEachPack` owns the open/consume/close lifecycle; block map, primitive map and acoustics use it. |
| M11 | **Build hygiene (#14)** | L | **Complete (batch 62):** harness moved to `gradle/test-harness.gradle.kts`, applied from `addon.gradle.kts`; root build/settings scripts unchanged. |

### 11.4 P3 — low priority / opportunistic

#### Fully automatable (A)

| ID | Item | Sev | Notes |
|---|---|---|---|
| L1 | **Unused asset cleanup** | L | **Complete (batch 60):** the six unreferenced ogg files and empty `variator.json` were removed. |
| L2 | **Small dead-code removals** | L | **Complete (batch 60):** duplicate color constants, `RenderWeather.register`, and the `WailaHandler` singleton path were cleaned up. |
| L3 | **Comment/doc drift** | L | Several §3/§4/§6 rows are historical; mark fixed rows consistently so the next audit does not re-open closed work. |

#### Design/refactor (D)

| ID | Item | Sev | Notes |
|---|---|---|---|
| L4 | **Optional Better Rain/Weather2 defer policy** | L | `WeatherBackend` makes a "defer to another weather mod" owner feasible, but this is a feature, not a stability fix. |
| L5 | **Aurora quality/visibility options** | L | Per-shader toggle, max auroras, look-direction gating, debug wireframe. Wait until the P0 matrix is green. |

### 11.5 Recommended order

1. **P0:** build the client smoke harness (C1/C2), then run the full manual matrix (C3). Fix anything it finds before release.
2. **P1:** complete — R1–R10 are done.
3. **P2:** M1–M11 are done; only the optional diagnostics expansion (M8) remains.
4. **P3:** opportunistic cleanup and feature work (L1–L5).
