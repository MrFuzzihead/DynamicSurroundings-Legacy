# Dynamic Surroundings (Legacy) — codebase review & compatibility plan

**Branch:** `refactor/angelica-notfine-compat`
**Scope:** analysis + the implementation log below. Batches 1–10 (weather/aurora, relative fog, client-thread safety, NotFine HUD/void fog, the Angelica provider bridge, BOP/cloud/phase compatibility, text-pop-off instancing, the compileOnly Angelica API, Angelica debug entries, and the validation hardening) are merged into the branch.
**Codebase:** 1.7.10 Forge port of Dynamic Surroundings, 177 production classes / ~14 k LOC, 37 test suites / 200 unit tests, plus a non-shipped in-game test mod.

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

**What remains:** run the real client compatibility matrix (Angelica on/off, NotFine near=1 %, Iris pack), then the remaining non-compat findings (B9–B11, B15–B17, B38–B40, B48/B50, B64–B65, B73) and the optional per-effect HUD text cache (P9).

### The three compatibility moves that unlock the most value

1. **A weather backend abstraction** with three owners: vanilla/external `IRenderHandler`, DS legacy renderer, and Angelica's instanced weather. DS mixins ask the backend before cancelling. **Done in batch 1.**
2. **An Angelica `WeatherColumnProvider` bridge** so DS's per-biome rain/dust/snow is fed into Angelica's instanced pipeline instead of competing with it. Angelica's API already exists (`com.gtnewhorizons.angelica.api.weather`, `apiVersion = 1`). **Done in batch 5; now uses a compileOnly Angelica reference (batch 8) instead of a reflection proxy.**
3. **Relative fog + a NotFine setting bridge** (`jss.notfine.core.SettingsManager.downfallDistance`, `Settings.TERRAIN_FOG`, `Settings.FOG_NEAR_DISTANCE`) — reflective, optional, no compile dependency. **Done in batches 1–2 for downfall and fog range.**

### Coverage reality

The JaCoCo floor measures **~48 % lines / ~29 % branches**, but that denominator **excludes `client/**` and the generated `Tags` class** (`addon.gradle.kts`). The client package is **113 of 177 classes and 8,659 of 13,969 LOC (62 %)**. Effective whole-mod coverage is therefore closer to ~7–10 %. The floor is honest about the measured subset but misleading as a project metric; see §8.

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
| B6 | **H** | `client/weather/StormSplashRenderer.java:90-91`, `NetherSplashRenderer.java:30` | The base `getPrecipitationHeight` ignores its `range` parameter; the nether override uses `EnvironState.getPlayer()` instead of the passed view entity. |
| B7 | **M** | `client/weather/Weather.java:119` | `level = (float)(level + 0.01)` is dead — the parameter is never used afterwards. Remove or use it in the threshold comparisons. |
| B8 | **M** | `client/weather/Weather.java:25,111-131` | `intensity`/`intensityLevel`/texture statics were mutated from the packet handler (Netty) while the render thread read them. **Fixed in batch 3:** `PacketRainIntensity` schedules on the client thread. |
| B9 | **M** | `client/weather/StormRenderer.java:90-100` | 441 columns × `getBiomeGenForCoords` + temperature + precipitation + light per frame; no cache. Share a 20-tick column cache with `StormSplashRenderer`. |
| B10 | **M** | `client/weather/StormSplashRenderer.java:100-160` | Up to 100 attempts/frame, each with world lookups; no column cache; `new Color` per column. |
| B11 | **L** | `client/weather/StormRenderer.java:247-257` | `RAIN_X_COORDS/RAIN_Y_COORDS` divide by zero at the centre column (i=j=16, NaN vertices). Vanilla has the same quirk, but the copied code could clamp to a minimum radius. |

### 3.2 Fog

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B12 | **C** | `client/FogEffectHandler.java:76-77` | Absolute fog override broke NotFine `TERRAIN_FOG`/`FOG_NEAR_DISTANCE` (Angelica #2304). **Fixed in batch 2:** relative scales applied to the current GL fog; `Respect External Fog` opt-out. |
| B13 | **H** | `client/FogEffectHandler.java:42-44` | `doFog()` was `enableBiomeFog || allowDesertFog`; elevation haze, morning fog and weather fog were ignored when those two were off. **Fixed in batch 2:** all fog options are considered. |
| B14 | **M** | `client/fog/MorningFogRangeCalculator.java:39` | `clamp(start - shift*2, 11, newEnd)` could return `11` when `newEnd < 11`, producing `start > end`. **Fixed in batch 2:** the floor is scaled and `FogResult` clamps `start <= end`. |
| B15 | **M** | `client/fog/BiomeFogRangeCalculator.java:47-80` | 41×41 = 1,681 biome lookups whenever the player moves one block (or the rain/far-plane changes). Cache on a coarser grid (e.g., 4-block cells) or only re-scan when the biome mix changes. |
| B16 | **M** | `client/fog/BiomeFogColorCalculator.java:60-79` | Same per-block 169-column rescan; `applyPlayerEffects` re-implements void fog/blindness/night vision. **Partially fixed in batch 4:** NotFine's `VOID_FOG` now gates the DS term; the scan/caching remains (P3). |
| B17 | **L** | `client/fog/BiomeFogRangeCalculator.java:13-15` | `DUST_FOG_IMPACT` unused (literal `0.9F`); `weightMixed` hardcoded 1681 instead of `(2*DISTANCE+1)^2`. |
| B18 | **L** | `client/fog/HolisticFogRangeCalculator.java:30-37` | `Math.min` of starts/ends could produce an invalid pair. **Fixed in batch 2:** `FogResult` clamps `start <= end` and the handler checks `isValid`. |

### 3.3 Aurora

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B19 | **H** | `client/AuroraEffectHandler.java:72` | `currentAurora.posX != ad.posX && currentAurora.posZ != ad.posZ` should be `||`. A new aurora with the same X but a different Z kept the old one. **Fixed in batch 1.** |
| B20 | **H** | `data/AuroraData.java:59` | `equals` but **no `hashCode`** → `HashSet<AuroraData>` used identity hashes, so duplicates were never deduplicated. Server could accumulate auroras every 100 ticks and broadcast all of them. **Fixed in batch 1:** `hashCode` on dimension/x/z; capping the per-dimension set is still open (B44). |
| B21 | **H** | `server/ServerEffectHandler.java:60-61` | `isAuroraInRange` subtracted `-auroraSpawnOffset` from a `posZ` that already contains the offset → the Z delta was doubled (`-2 * spawnOffset`). **Fixed in batch 1:** it now measures against the actual spawn target (`playerZ - offset`) and uses `MIN_AURORA_DISTANCE_SQ`. |
| B22 | **M** | `client/AuroraEffectHandler.java:27-75` | `currentAurora` was written outside the `synchronized (auroras)` block and read by the renderer; `addAurora` could run on the Netty thread. **Fixed in batches 1+3:** `currentAurora` is `volatile`, selection is locked, and `PacketAurora` is scheduled on the client thread. |
| B23 | **M** | `client/aurora/AuroraRenderer.java:137` | `GL11.glScaled(3.5, 25.0, 3.5)` after `tess.draw()` was a leftover; `lowY`/`lowY2` were unused. **Fixed in batch 1.** |
| B24 | **M** | `client/aurora/Aurora.java:17-22` | `ANGLE1`, `FADE_LIMIT`, `AURORA_SPEED`, `AURORA_AMPLITUDE`, `AURORA_WAVELENGTH` unused; `cycle += 0.75F` and `18.0F`/`8.0F` literals remain. Remove or wire them. |
| B25 | **M** | §2.4 | Aurora is drawn inside `RAIN_SNOW` and is disabled with weather. |
| B26 | **L** | `client/aurora/AuroraRenderer.java:80-146` | Immediate-mode state was restored by enabling cull/lighting/texture unconditionally rather than restoring the previous state. **Fixed in batch 1:** `glPushAttrib(GL_ALL_ATTRIB_BITS)` / `glPopAttrib`. |

### 3.4 Sound

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B27 | **C** | `client/sound/SoundManager.java:88` | `SoundManager.update()` was only called from `PlayerSoundEffectHandler.process`, which is registered only when `enableBiomeSounds` is true. With biome sounds off, pending block/step sounds never flushed and emitters never ticked. **Fixed in batch 3:** `ClientEffectHandler.clientTick` updates the queue unconditionally. |
| B28 | **H** | `client/sound/cache/SoundCache.java:25-27,48` | Static `IResourceManager` captured once; shared static 64 KiB `BUFFER` read from the sound thread; cache never invalidated on resource reload; `throw new Error(...)` on failure; `available()` is not a reliable size check. Use a per-call buffer or `ThreadLocal`, resolve the manager lazily, implement `IResourceManagerReloadListener`, fall back to the vanilla URL, and bound the cache (LRU/soft refs). |
| B29 | **H** | `client/sound/SoundManager.java:84` | `emitters.put(sound, new Emitter(sound))` replaces an equal key (same sound name) without fading the old emitter → a playing sound can be orphaned and never stopped. Handle duplicates by name explicitly. |
| B30 | **M** | `client/sound/SoundManager.java:67-84` | `queueAmbientSounds` is O(n²) and mutates `sounds` while iterating (safe only because of the immediate `break`). Use maps/sets. |
| B31 | **M** | `client/sound/SoundManager.java:161-167` | `isSoundPlaying` does `containsValue` on the whole playing map for every emitter every tick; O(n²). Maintain a set of playing sounds/ids. |
| B32 | **M** | `client/sound/PlayerSound.java:30-33` | `this.volume = sound.volume; ... this.volume = 0.002F;` — the first assignment is dead. |
| B33 | **M** | `client/sound/SoundManager.java:240-275` | `configureSound()` creates and destroys a temporary OpenAL context just to query the channel count; can disturb audio and is expensive. Cache the count from the live context, or use `ALC10.alcGetInteger` after the real device exists. Magic `4112`. |
| B34 | **M** | `client/sound/SoundEffect.java:55` | The no-pattern constructor leaves `pattern = null`; `matches()` NPEs for programmatically created effects. Compile `.*` or guard. `equals` compares only the sound name (intentional for emitters, but document it). |
| B35 | **M** | `client/sound/SoundManager.java:116-118` | `canFitSound()` counts all playing sounds but compares to `normalChannels - 6` and ignores streaming channels; use the normal-channel map or the manager's own bookkeeping. |
| B36 | **L** | `client/sound/fix/SoundFixMethods.java` | The paulscode `ISourceRemoved` cast assumes the `Source` mixin always applied. If the mixin fails (e.g., a SoundSystem version change), this throws `ClassCastException`. Add `instanceof` fallback and a log. |

### 3.5 Network & server

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B37 | **C** | `network/PacketHealthChange.java`, `network/PacketAurora.java`, `network/PacketRainIntensity.java` | The handlers called `EffectRenderer.addEffect`, `AuroraEffectHandler`, and `Weather.setIntensity` directly on the Netty thread. **Fixed in batch 3:** all three use `ClientPacketHandler.schedule` (`Minecraft.func_152344_a`). |
| B38 | **H** | `server/ServerEffectHandler.java:52` | Sends a `PacketRainIntensity` every `Phase.START` for every dimension (20 packets/s/dimension, even with no players), although the value changes only when rain starts/stops. Send on change, on player join, and at a low keep-alive rate at most. |
| B39 | **H** | `server/WorldHandler.java:28-31` | The replacement for `World.updateWeatherBody` dropped vanilla's `if (!provider.hasNoSky)` guard, so the state machine runs in the Nether and the End too. `WorldServerMulti` installs a `DerivedWorldInfo` whose setters are no-ops (`DerivedWorldInfo.setRaining/setRainTime`), so those dimensions read the overworld's rain flags but mutate their own `rainingStrength`, and the End still randomizes `DimensionEffectData`. Gate on `DimensionRegistry.hasWeather(world)` (config-driven, so the Nether can still opt in) and skip `hasNoSky` worlds that are configured weatherless. |
| B40 | **M** | `data/DimensionEffectDataFile.java:31` | `getFile()` calls `markDirty()` on **every** access; `get()` is called from `ServerEffectHandler` every tick and `WorldHandler` every tick. The data is always dirty, so every autosave rewrites it even when nothing changed. Mark dirty in the setters (`setRainIntensity`, aurora add/remove) instead, and cache the `WorldSavedData` per world. |
| B41 | **M** | `data/DimensionEffectDataFile.java:23-33` | `world.loadItemData` on every tick for every world is an unnecessary map lookup; cache the file in a `WeakHashMap<World, DimensionEffectDataFile>` cleared on `WorldEvent.Unload`. |
| B42 | **M** | `server/ServerEffectHandler.java:30,60` | `MIN_AURORA_DISTANCE_SQ`, `CHECK_INTERVAL`, `RESET` are unused; the literals `160000L`, `100`, `-10` are duplicated in `Network`/`DimensionEffectData`. |
| B43 | **M** | `network/PacketAurora.java:41-47` | `colorSet`/`preset` are serialized as bytes but held as ints. Current IDs are < 128, but adding presets/colours will silently truncate. Use `writeByte`+validate or `writeInt`. |
| B44 | **M** | `server/ServerEffectHandler.java:90-95` | A new aurora is created for a player whenever `!isAuroraInRange`; with the broken range check (B21) and no `hashCode` (B20) this can grow without bound. Cap per dimension/player and dedupe. |
| B45 | **L** | `commands/CommandRain.java:60` | `getCommandSenderAsPlayer` means the server console cannot run `/rain status`. Use `sender.getEntityWorld()` (and `ICommandSender`) so it works from console/RCON. |
| B46 | **L** | `commands/CommandRain.java:81-86` | `/rain reload` calls `BiomeRegistry.initialize()` and `BlockRegistry.initialize()` from the server command. `BlockRegistry` (and its `MCHelper`/`JetEffect`/`SoundManager` dependencies) is effectively client-only, so this is both useless and a potential dedicated-server `NoClassDefFoundError`/`NoSuchMethodError`; the client also keeps its old biome/block/fog/sound data. Add a client reload packet and keep server/client reload paths separate. |
| B47 | **H** | `data/DimensionEffectData.java:66-77` | `randomizeRain` ignored `minIntensity`: with `delta = max-min` and `mid = delta/2`, `rand*mid + rand*mid` produced `[0, delta]`, then clamped to `0.01`. **Fixed in batch 11:** `min + rand*(max-min)`, with tests for a non-zero minimum and the 1% floor. |

### 3.6 Registries, config and bootstrap

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B48 | **H** | `data/BiomeRegistry.java:167`, `data/BlockRegistry.java:79,86`, `client/fx/BlockEffect.java:31` | `random.nextInt(chance)`/`nextInt(spotSoundChance)` throws `IllegalArgumentException` when the configured chance is `0`. Config allows 0 (`@MinMaxInt(min = 0)` for effect chance; block chance has no min annotation). Guard `chance <= 0` (disable) at load and at use. |
| B49 | **M** | `data/BiomeRegistry.java:187`, `data/BlockRegistry.java:69`, `client/footsteps/engine/implem/ProbabilityWeightsAcoustic.java:42` | The reverse-cumulative weighted pick is **correct for positive weights**, but it is duplicated three times and is not guarded: a negative weight makes the loop walk past index 0 (`ArrayIndexOutOfBoundsException`), and a zero/negative `totalWeight` gives `nextInt(0)` or a wrong pick. Extract one `WeightedRandom` helper, validate weights at load, and test the distribution (`[10,1]` → 10/11 vs 1/11) plus the negative-weight guard. |
| B50 | **H** | `data/config/BiomeConfig.java:43`, `data/config/BlockConfig.java:26,41` | The JSON config models accept any `Integer` for `spotSoundChance`/`chance`/`stepChance`; there is no validation (0 or negative) before the value reaches `nextInt` (B48). Validate at load and clamp to `>= 1` or disable the feature. |
| B51 | **M** | `util/ConfigProcessor.java:58` | `Float.MIN_VALUE` as the "no minimum" sentinel means any float property without `@MinMaxFloat` is clamped to `1.4e-45`; negative and zero values silently become ~0. Use `-Float.MAX_VALUE`. All current float options happen to carry the annotation, so this is latent. |
| B52 | **M** | `util/ConfigProcessor.java:19-101` | No support for `double`/`long`/enums/lists beyond `String[]`; every parse failure is logged and ignored (a typo leaves a static at its previous value). Add a validation report and a unit test per type. |
| B53 | **M** | `ModOptions.java:405-425` | `scrubCategory` removes any property with an empty comment. A legitimately comment-less entry is deleted on every load. Use an explicit `removedOptions` list instead of comment emptiness. |
| B54 | **M** | `client/gui/DynamicSurroundingsConfigGui.java:92-96` | `super.actionPerformed` runs before the synthetic sound lists are saved; the screen may already be closing/saving. Save the synthetic categories first, then call `super`. Applying the config also does not re-run `BlockRegistry`/`BiomeRegistry`/`SoundRegistry` initialization or refresh `EnvironStateHandler` static finals, so several non-restart options appear to do nothing until a world reload. Either make them `@RestartRequired` or add a `reloadRuntime()` path. |
| B55 | **M** | `client/waila/WailaHandler.java:119-121` | WAILA registration is gated on `enableDebugLogging`; the integration is off by default. Remove the gate. Also the class is loaded from the common `Proxy.init` on a dedicated server; the `@Optional.Method` annotations are incomplete and `gatherText` has no annotation. Split the WAILA client code into `ProxyClient`, or guard the whole class with `@SideOnly(Side.CLIENT)` + optional interface handling. |
| B56 | **M** | `proxy/Proxy.java:31-36` | Common proxy imports/loads client classes (`DamageEffectHandler`, `WailaHandler`) and calls `DamageEffectHandler.initialize()` on the server. `DamageEffectHandler.handleEvent` is `@SideOnly(CLIENT)` but the class is not; a production dedicated server that resolves the method can `NoClassDefFoundError`. The same side leakage exists in `BlockRegistry` (client `MCHelper`/`JetEffect`/`SoundManager` references called from `CommandRain`). Move client init to `ProxyClient`, keep a server-only handler for the packet sender, and make `BlockRegistry` explicitly client-only. |
| B57 | **M** | `data/DimensionRegistry.java:27` | `CALENDAR_API = Loader.isModLoaded("CalendarAPI")` in a static initializer. Class initialization order determines the result; it is also the reason tests need FML bootstrap. Resolve lazily and cache. |
| B58 | **M** | `data/DimensionRegistry.java:277-305` | `getData` uses a static cache keyed only by dimension id; `isFlatWorld` is set only when dimension 0 loads and only from the client event. Flat-world sea level is wrong on a dedicated server. |
| B59 | **M** | `data/BiomeRegistry.java:118-146` | A miss calls `initialize()` (full rescan, regex recompile, event post) while holding `synchronized (registry)`; on a busy server this is a global stall and can recurse through listeners. Make the registry copy-on-write and publish an immutable snapshot. `initialize` also clears `biomeAliases` at the end, so aliases cannot be queried later. |
| B60 | **M** | `data/BlockRegistry.java:28-35` | `getEffects` returns the internal mutable list; `getRandomSound` allocates a new `ArrayList` on every call (called ~1,000×/tick from `BlockEffectHandler`). Return an immutable list and avoid per-call allocation. |
| B61 | **M** | `VersionCheck.java:24,30-34,40-46` | Stale OreCruncher/BetterRain URL; `REMOTE_VERSION_FILE` unused in `versionCheck` (literal URL); no connect/read timeout; non-daemon thread; 3 retries × 10 s can delay shutdown. The mod version is hardcoded in three places. Use the generated `Tags.VERSION`, a daemon thread with timeouts, and make the check opt-in/disableable. |
| B62 | **L** | `Module.java:24-34` | `version = "1.0.6.4"`, `VERSION`, `mcmod.info` `${modVersion}`, and `VersionCheck.modVersion` are four independent version sources. Use `Tags.VERSION` once. `Module` registers itself on the FML bus but has no `@SubscribeEvent` methods. |
| B63 | **L** | `ModOptions.java:86,89,93` | Copy-pasted comments ("Enable morning fog" for `enableWeatherFog`). Cosmetic but confusing in the GUI. |

### 3.7 Client effects, environment and HUD

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B64 | **H** | `client/ClientEffectHandler.java:78-86` | `world != null` is checked but `player` is not; during respawn/dimension change `thePlayer` can be null while the world is set, and every handler dereferences it. Guard `player != null`. |
| B65 | **H** | `client/fx/BlockEffectHandler.java:64-72` | Step sound samples `playerPos.down(2)`, i.e. the block **below the support block** (dirt under grass), not the block under the feet. Use `boundingBox.minY - 0.1` (like `PFSolver`) or `down(1)` with an epsilon. |
| B66 | **H** | `client/fx/JetEffect.java:83-89` | `Dust.trigger` requires `world.isAirBlock(x, y-1, z)`, so dust only spawns on floating dirt/sand. The intended condition is almost certainly the opposite (solid below and/or air above). This likely means the desert dust jets never appear in normal terrain. Verify in game and fix. |
| B67 | **H** | `client/fx/BlockEffectHandler.java:40-62` | ~1,000 `BlockPos` allocations + two block lookups per sample per tick (plus `new ArrayList` in `BlockRegistry.getRandomSound`) → GC pressure and client tick cost. Reuse a mutable pos, read block+metadata once, and avoid allocation in the weighted pick. |
| B68 | **M** | `client/EnvironStateHandler.java:205-225` | 21 `CONDITION_TOKEN_*` constants and `CONDITION_SEPARATOR` are declared but never used; `getPlayerConditions` appends literals. Remove the constants or use them. |
| B69 | **M** | `client/EnvironStateHandler.java:188-470` | `EnvironState` is a static mutable god-object. `getPlayer()` can return null, `getWorld()` dereferences it, `isPlayerMoving` dereferences the static `player` without a null check, and `tick` is not re-entrant. Replace with an immutable per-tick `EnvironmentSnapshot` passed to consumers. |
| B70 | **M** | `client/BiomeSurveyHandler.java:57-70` | The survey dirty check includes `playerY`, so jumping re-scans 169 columns; it also re-scans for every block moved. Drop Y, throttle to ~20 ticks or a biome/area change, and cache the last survey. |
| B71 | **M** | `client/PlayerSoundEffectHandler.java:42-63` | Every tick allocates a `TObjectIntHashMap`, a `List`, and a scaled `SoundEffect` per sound, and re-runs regex matching for every sound. Cache the weighted sound list per (conditions, survey revision) and only rebuild when the biome mix changes. |
| B72 | **M** | `client/hud/PotionHUD.java:30-100` | Null `thePlayer`; no state restore; re-formats strings/widths every frame; rendered on every `RenderGameOverlayEvent`. **Partially fixed in batch 4:** dispatcher filter, null guard and `glPushAttrib`/`glPopAttrib`; per-effect text caching remains (P9). |
| B73 | **M** | `client/PotionParticleScrubHandler.java:8-11` | Writes `dataWatcher.updateObject(7, 0)` (the potion liquid colour, not the particle flag) every tick to suppress particles. It also removes the colour other client code may read and is a global datawatcher hack that can fight other mods. Prefer a mixin/targeted redirect on the particle spawn path, and only update when the value actually changes. |
| B74 | **M** | `client/fx/particle/EntityTextPopOffFX.java:59-89` | Immediate-mode font draw and depth-func state leak; see §2.5. **Fixed in batch 7:** render-less marker + post-world renderer with pushed attributes and an Iris `PARTICLES` phase. |
| B75 | **L** | `client/hud/GuiHUDHandler.java:36-40` | The dispatcher forwards all overlay phases to all overlays; filter by `ElementType` in the dispatcher. |
| B76 | **L** | `client/EnvironStateHandler.java:104-109` | `craftSoundThrottle` initial value makes the first craft test depend on the tick counter; use `Integer.MIN_VALUE` or a boolean. |

### 3.8 Footsteps / sound engine

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B77 | **H** | `client/footsteps/game/system/PFSolver.java:49` | `if (Math.abs(ply.motionY) < 0.02) return null;` — verify against the original Presence Footsteps. If a player standing/walking on flat ground has `motionY == 0` at the client tick, this suppresses every footstep. Add a game test with a walking player and count `playAssociation` calls. |
| B78 | **M** | `client/footsteps/game/system/PFReaderQP.java:50-85` | `if (2 == 2)`, `if (2 == 1)`, `if (2 == 0)` are dead branches; `USE_FUNCTION`, `overallMultiplier`, `ndm` are unused. `PFReaderQP` is also never instantiated (the port always uses `PFReaderH`), so the whole class is dead unless a config is added. |
| B79 | **M** | `client/footsteps/mcpackage/implem/AcousticsManager.java:126-135` | `newMinimum` is assigned **every** remaining sound instead of the minimum; the wake-up time can be later than the earliest pending sound, delaying sounds. Compute `Math.min`. Also `randAB` uses `a + rng.nextInt((int)b + 1)`, which ignores the lower bound for the range (max is `a+b`, not `b`) and can overflow. |
| B80 | **M** | `client/footsteps/mcpackage/implem/AcousticsManager.java:40-43` | `USING_LATENESS`, `USING_EARLYNESS`, `LATENESS_THRESHOLD_DIVIDER`, `EARLYNESS_THRESHOLD_POW` unused; the literals are repeated. |
| B81 | **M** | `client/footsteps/mcpackage/implem/LegacyCapableBlockMap.java:15-35` | Only `^` (94) and `.` (46) are treated as legacy separators; `id:meta` keys (YouTube-era blockmaps) go to `super.register` with the numeric string and fail to resolve. Add `:` to the separator search and a test with `1:0`, `1^0`, `1.0`. |
| B82 | **M** | `client/footsteps/mcpackage/implem/BasicBlockMap.java:97-120` | The regex `([^:]+:[^^+]+)\^?(\d+)?\+?(\w+)?` is fragile (`[^^+]` negates `^` and `+`, not what the comment intends); `BlockInfo.hashCode` NPEs on a null block and `equals` casts without `instanceof`; `getBlockMap` reuses a mutable key (not thread-safe). Add parser tests. |
| B83 | **M** | `client/footsteps/game/system/Footsteps.java:74-166` | Each loader catches `IOException` but `Scanner.next()` on an empty stream throws `NoSuchElementException` (not caught) and `AcousticsJsonReader` swallows all parse errors, so a malformed pack silently disables all footsteps. Validate and log. |
| B84 | **L** | `client/footsteps/game/system/Footsteps.java:180-195` | Setting `nextStepDistance = Integer.MAX_VALUE` every tick is a write per tick; do it once when the handler is enabled. |
| B85 | **L** | `client/footsteps/parsers/AcousticsJsonReader.java:45-100` | `json.get("type")`/`get("engineversion")` NPE on missing fields; `setupSoundName` does `charAt(0)` on a possibly empty string; `delayed` assumes `delay_min`/`delay_max` exist. Throw a descriptive `JsonParseException` instead of relying on the outer `printStackTrace`. |
| B86 | **L** | `client/footsteps/util/property/simple/ConfigProperty.java:100-110` | `JsonUtils.load(..., Map.class)` returns `null` for a JSON `null`; the unchecked cast hides it; values must all be strings (numbers throw). Add validation. |

### 3.9 Utilities / data

| ID | Sev | File:line | Finding |
|---|---|---|---|
| B87 | **H** | `compat/BlockPos.java:145-220` | No `equals`/`hashCode`; the box iterators compared with `equals` and never terminated; `MutableBlockPos.getImmutable()` copied the base class's private fields (always 0). **Fixed in batch 11:** coordinate `equals`/`hashCode`, getter-based copy construction, and tests for both box iterators and mutable immutability. |
| B88 | **H** | `util/Color.java:222-250` | `ImmutableColor` overrode `scale`, `mix` and `adjust` but not `add`, `blend`, or `scale(r,g,b)`, so `Color.RED.add(...)` could mutate a shared static constant. `Color` had `equals` but no `hashCode`. **Fixed in batch 11:** every mutator is rejected and the hash contract is tested. |
| B89 | **M** | `util/MathStuff.java:139-144` | `sqrt(double)` returns `(float) Math.sqrt(value)` (precision loss); `sin/cos(double)` truncate to float; `log` is an approximation used for `nextGaussian`, which biases the distribution. `abs(Long.MIN_VALUE)` / `abs(Integer.MIN_VALUE)` overflow; `RAD_TO_INDEX`, `DEG_FULL`, `DEG_TO_INDEX`, `COS_TO_SIN`, `RAD_TO_DEG`, `DEG_TO_RAD` unused (some only by tests). Use `Math` for accuracy where it matters. |
| B90 | **M** | `data/DimensionEffectData.java:30-32,88-95` | `NBT` inner classes in `DimensionEffectData`, `DimensionEffectDataFile` and `AuroraData` are unused; `MIN_INTENSITY`/`MAX_INTENSITY` unused; `FORMATTER` (static `DecimalFormat`) is not thread-safe. |
| B91 | **M** | `data/AuroraData.java:59-67` | No `hashCode` (see B20). `equals` ignores seed/color/preset, so `HashSet` dedupe by column would drop updates; decide the intended identity and document it. |
| B92 | **M** | `util/PlayerUtils.java:20-24,60-84` | `INSIDE_Y_ADJUST`, `RANGE`, `AREA` unused (literals 3/3/49); `isReallyInside` only counts ceilings within 2 blocks, so a 5-block-high room is "outside". Use `world.canBlockSeeTheSky`/heightmap or a configurable coverage test. Add unit tests with mocked worlds. |
| B93 | **M** | `VersionCheck.java:157-210` | `assert` is used for input validation (`numComponents >= 3`), which is disabled in production; `compareTo` ignores alpha/beta. Add proper parse/compare tests. |
| B94 | **L** | `util/JsonUtils.java:40-75` | `load(InputStream)` can return `null` (JSON `null`); `clazz.newInstance()` is deprecated; the stream is not closed by the method (documented by callers). Add null checks and try-with-resources. |
| B95 | **L** | `ModLog.java` | `error` prints the stack trace and logs only the message (the throwable is not passed to Log4j); `catching` prints twice. Use parameterized logging and pass the throwable. |

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
| P2 | `StormRenderer` + `StormSplashRenderer` world lookups | 441 columns × (biome + temperature + precipitation + light) per frame, up to 100 more per frame for splashes | One shared `WeatherColumnCache` keyed by (x, z, dimension, 20-tick bucket) and block-exists invalidation. Angelica's `WeatherColumns` is a good reference. |
| P3 | `BiomeFogRangeCalculator` | 1,681 biome lookups on every block move / rain / far-plane change | Coarse grid (4×4 cells), rescan only when the biome mix changes, keep the old result on chunk-not-loaded. |
| P4 | `BlockEffectHandler` | ~1,000 samples/tick × 2 block reads + allocations + weighted-list allocation | Mutable `BlockPos`, one `getBlock`+metadata read, allocation-free weighted pick, and an early-out when no block effects/sounds are configured. Optionally sample 1/2 or 1/4 of the volume per tick on a rolling schedule. |
| P5 | `PlayerSoundEffectHandler` | New maps/lists/scaled effects every tick; regex per sound | Cache the weighted sound list per (conditions, survey revision); reuse lists; precompile `SoundEffect` patterns. |
| P6 | `BiomeSurveyHandler` | 169 biome lookups per block moved (including Y changes) | Drop Y from the key, throttle to 20 ticks/area change, skip if the player hasn't moved a block. |
| P7 | `SoundCache` | Unbounded retention of every played ogg; 64 KiB static buffer | LRU/soft-reference bound, per-call or `ThreadLocal` buffer, clear on resource reload. |
| P8 | `SoundManager` | O(n²) `containsValue`/`contains`/`remove` per tick | `Map<ResourceLocation, ISound>`/set-based bookkeeping; maintain a playing-id set. |
| P9 | `PotionHUD` | `I18n.format`, `getStringWidth`, string building every frame | Cache per `PotionEffect` and refresh when the displayed second changes. |
| P10 | Config GUI | Builds 570 Boolean + 570 Integer properties on open | Virtualize/lazy-load the sound lists, or move per-sound volume to a separate searchable screen. |
| P11 | `DimensionEffectDataFile` | `loadItemData` + `markDirty` per tick per world | Cache per world; mark dirty on mutation. |
| P12 | `WorldHandler` | Static `XorShiftRandom` shared, `DimensionEffectDataFile` per tick | Fine, but pass an RNG or a `WeatherTicker` for deterministic tests; cache the data. |
| P13 | `Aurora` | Per-frame trig over ~3 × 128 nodes | Precompute the band template per seed; only update offsets when `auroraAnimate`; use `MathStuff` (already). |
| P14 | `AuroraRenderer` | Immediate-mode + attribute state churn | A small static VBO / `Tessellator` buffer with the geometry rebuilt only when the node set changes; batch all three bands into one draw. |
| P15 | `EnvironState.tick` | Regex per biome + string concat per tick | Precompute per-biome match sets, cache the conditions string by its inputs, use a `StringBuilder`/enum token set. |
| P16 | `VersionCheck` | Blocking HTTP on a non-daemon thread | Timeouts, daemon thread, optional. |
| P17 | `SoundManager.configureSound` | AL create/destroy | Query after the real context exists; cache. |
| P18 | `ForgeDictionary` | Up to 300 `IResourcePack` probes on reload | Reload only on demand; cache the found packs. |

---

## 5. Refactors (structure, testability, maintainability)

1. **Split the per-tick environment state.** `EnvironState` is 20 static fields + 20 static predicates. Create an immutable `EnvironmentSnapshot` (dimension, biome, conditions, inside/freezing/humid/dry, riding, player refs) built once per tick and passed to fog/weather/sound/block effects. This removes hidden temporal coupling (handler registration order) and makes all consumers unit-testable.
2. **One weather backend.** `WeatherBackend` owns provider/angelica/DS/vanilla selection and exposes `renderOverlay`, `spawnSplashes`, `ownsWeather`. `RenderWeather` becomes an ordered list of DS renderers used only by the DS backend. See §2.2.
3. **One fog result type with a mode.** `FogResult { mode: ABSOLUTE | SCALE, start, end, density }`; calculators return SCALE by default. This is the enabler for NotFine/Angelica compatibility and for tests.
4. **One weighted random helper.** `WeightedRandom.pick(List<T>, ToIntFunction<T>, Random)` used by all three call sites; statistical tests with a fixed seed.
5. **Side-safe proxies.** `Proxy` should not import `client.*`. Move `DamageEffectHandler.initialize`, `WailaHandler.register` and `SoundRegistry` client bits into `ProxyClient`; create a `ServerPacketSender` for the health/rain packets. `DamageEffectHandler` should be split into `client/…/DamageEffectClient` and `server/…/DamageEffectServer`.
6. **Rename the clashing classes.** `client.sound.SoundManager` vs `net.minecraft.client.audio.SoundManager`; `util.Color` vs `java.awt.Color`; `network.Network` is fine. `DynamicSoundManager` / `RgbColor` reduce import noise and grep ambiguity.
7. **Registry lifecycle.** `BiomeRegistry`, `BlockRegistry`, `SoundRegistry`, `DimensionRegistry` share a `Reloadable` interface + a `RegistryManager` that handles init/reload/publish. Server reload broadcasts a client packet; client reload rebuilds fog/sound. This also fixes `/rain reload` (B46).
8. **Config schema.** Replace the 422-line `ModOptions` with an annotated schema per module and generated category/sort/flags; add a validation pass that reports unknown options instead of silently dropping them. Keep the current `ConfigProcessor` for compatibility but fix `Float.MIN_VALUE`.
9. **Resource loading.** One `ResourceLoader`/`JsonConfigLoader` with strict parsing, error aggregation and a `reload` lifecycle; the footsteps engine currently has four near-identical try/catch/close blocks.
10. **Mixins.** Move `Mixins`/`EarlyMixinsLoader` out of the `mixins` package (or add an exclusion test); add `priority` to compat-sensitive mixins; add a `WeatherOwnership` check to both weather mixins; add `@Unique` to every injected field (pauls code already uses the duck interface).
11. **Immutable value types.** `Color`, `AuroraData` (or document identity), `FogResult`, `AuroraPreset` (already immutable), `SoundEffect` (make `pattern` non-null).
12. **Deterministic time/random.** Inject a `Clock`/`Random` into `AcousticsManager`, `WorldHandler`, `AuroraEffectHandler`, `EnvironState` so tests can run without sleeping or seeding global random state.
13. **Logging.** Use `ModLog`/Log4j parameterized calls and pass throwables; replace `printStackTrace` (10 occurrences) and empty `catch` blocks in resource loaders with `ModLog.warn(..., e)`.
14. **Build hygiene.** Consider moving the test/game-test/JaCoCo setup from `addon.gradle.kts` into a small convention plugin; it is ~250 lines of Kotlin DSL inside a mod project. Not required for the refactor, but it will make the compat CI job easier.

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
- **`Color`**: duplicate `MC_BLACK`==`BLACK`, several unused named colours; `transitionTo` (check), `add`, `blend`, `adjust` may be unused.
- **Assets**: 6 unreferenced ogg files (`ambient/steve/bowpull2-4`, `ambient/underground/rockfall1-3`). Keep or add to a pack; remove to shrink the jar.
- **`variator.json` is empty** — the variator loader is effectively a no-op; either populate it or remove the loader path.
- **`RenderWeather.register`** is only called from its own static initializer; make it private or an instance list.
- **`Namespace`**: `WailaHandler` constructor registers to the event bus every time it is instantiated; the class is only instantiated in `callbackRegister`, so it is effectively a singleton. Make it explicit.
- **`Module`** self-registration on the FML bus (no handlers).

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

The current floor is measured over a subset that excludes 62 % of the code. Do not simply lower the floor; change what is measured:

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
- Fix B1/B2 (cancel weather + single provider owner), B3 (aurora not gated on weather), B19/B20/B21 (aurora identity/range), B37 (off-thread client packets), B27 (sound update), B38/B39/B40 (server weather/packet/dirty), B48/B50 (chance guards + config validation), B64/B65 (null player + step block).

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
