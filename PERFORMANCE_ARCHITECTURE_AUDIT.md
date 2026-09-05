# PixelRPG – Performance & Event-Driven Architecture Audit

**Branch:** `test`  
**Audit basis:** commit `6ea0d826511227c6dec0f29d2813ca86c0348be2`  
**Target:** Java 25 + Paper 26.2  
**Primary goals:** Performance, stability, deterministic behavior, clean architecture, and removal of genuinely dead/unreferenced legacy code.

> This document is an optimization plan, not a behavior redesign. Player-visible behavior, results, timings that are part of gameplay, presentation, interactions, balancing, and existing features are treated as invariants. Internal architecture may be changed aggressively when that improves performance or stability.

## 1. Optimization principles

1. **Event-driven first:** If state changes because of a known event, update only the affected object/player/region instead of periodically scanning everything.
2. **Dirty-state second:** When a state can change repeatedly before it needs to be consumed, coalesce it with a keyed dirty set/map and process once.
3. **Time-driven only where time is the actual input:** Combat cooldowns, boss phases, respawn delays, playtime, and other genuinely temporal mechanics may remain scheduled, but their scheduled work must be local and cheap.
4. **No global scans for local changes:** A player changing equipment must not cause all players/companions/entities to be recalculated.
5. **No repeated derivation:** Cache immutable/configuration data and derived state; invalidate it only when its dependencies change.
6. **No unnecessary allocations on hot paths:** Avoid creating collections, streams, Components, ItemStacks, locations, UUID collections, and database objects inside frequent loops unless required.
7. **No synchronous I/O on gameplay paths:** Database, filesystem, HTTP, skin resolution, and other blocking operations must be isolated from the server thread.
8. **Bound all queues:** Dirty/event queues must be coalescing and bounded by the number of affected keys, not by the number of repeated events.
9. **Lifecycle ownership:** Every task, listener, cache, executor, and subscription must have one clear owner and deterministic shutdown behavior.
10. **Delete only proven dead code:** A class/field/method is removed only after repository-wide reference analysis confirms that it is not part of registration, reflection/configuration, API exposure, serialization, or another runtime path.

## 2. Current architecture observations

The branch already contains the beginnings of the desired architecture. `core/WakeScheduler.java` exists as a keyed one-shot/coalescing scheduler, while `AsyncFileWriter.java` and `LifecycleCoordinator.java` centralize lifecycle/persistence concerns. fileciteturn544file0L2-L2

The quest package is already split into event listeners, state/repository classes, navigation, and a passive-check task. In particular, `QuestMobKillListener`, `QuestNavigationLifecycleListener`, `QuestNavigationService`, and `QuestPassiveCheckTask` indicate a mixed event-driven + scheduled architecture that should be pushed further toward dependency-triggered invalidation. fileciteturn551file0L2-L2

The companion package contains separate combat, equipment, experience, and follow components. `CompanionFollowTask` is therefore a high-value candidate for converting repeatedly derived state into dirty-state updates while keeping the actual follow movement behavior unchanged. fileciteturn553file0L2-L2

## 3. Priority roadmap

### P0 – highest performance impact / lowest behavioral risk

- Convert recurring global scans into per-player/per-entity/per-region dirty processing.
- Remove repeated equipment/stat/quest derivation from tick loops.
- Coalesce scoreboard, companion, quest, scaling, navigation, and persistence invalidations.
- Eliminate synchronous database/file/network work from gameplay events.
- Introduce local spatial indexes wherever code currently searches all players/entities/spawn points.
- Make caches dependency-aware instead of time-based where possible.
- Audit every scheduled task for actual required frequency and replace fixed-rate polling with event + wake-up scheduling.

### P1 – stability and scalability

- Enforce lifecycle ownership for every executor/task/listener.
- Make shutdown flushes bounded, ordered, and failure-tolerant.
- Make database writes idempotent and coalesced.
- Prevent duplicate event processing and duplicate scheduled work.
- Add explicit invalidation contracts between domain state and presentation consumers.
- Reduce object churn and repeated Bukkit/Paper lookups in hot paths.

### P2 – architecture cleanup

- Separate domain state changes from presentation refreshes.
- Introduce small domain events for meaningful state changes rather than generic "refresh everything" calls.
- Remove dead compatibility code, obsolete helpers, unused fields, and unreachable services after reference verification.
- Consolidate duplicate cache/index implementations.
- Keep all periodic work that is truly temporal, but move all derived-state refreshes out of those timers.

## 4. Event-Driven / Dirty-State Architecture

### 4.1 Target model

Use the following pipeline:

```text
Domain change
    -> domain event
    -> affected key(s) marked dirty
    -> coalescing queue/set
    -> one local refresh
    -> presentation/state cache updated
```

Examples:

```text
Player equipment changed
    -> PlayerEquipmentChangedEvent
    -> player UUID dirty in stat/scaling/scoreboard consumers
    -> one recalculation
```

```text
Companion equipment changed
    -> CompanionEquipmentChangedEvent
    -> companion UUID dirty
    -> derived combat/follow state refreshed once
```

```text
Quest progress changed
    -> QuestProgressChangedEvent
    -> affected player + quest dirty
    -> navigation/UI/progress consumers refresh only that player
```

### 4.2 Required properties

- Events must carry enough information to identify the affected scope.
- Consumers must never perform a global scan merely because they received a local event.
- Repeated events before consumption must collapse to one refresh.
- Refresh methods must be idempotent.
- Events must not retain live `Player`, `Entity`, or `World` objects longer than necessary.
- Prefer UUID/identifier + immutable value snapshots over storing live server objects in queues.
- Presentation refreshes must happen on the correct server thread/context.

### 4.3 Dirty queues

Recommended primitive:

```text
Set<UUID> dirtyPlayers
Set<UUID> dirtyCompanions
Set<SpawnPointKey> dirtySpawnPoints
Map<UUID, DirtyFlags> dirtyPlayerState
```

Use bit flags/enums for multiple independent consumers where useful, e.g.:

```text
STATS | SCOREBOARD | NAVIGATION | QUEST_UI | SCALING
```

This prevents five separate events from causing five independent refreshes during one gameplay action.

### 4.4 WakeScheduler

`WakeScheduler` should become the standard mechanism for keyed deferred work where a delayed wake-up is required. It should not be used to disguise polling. The correct pattern is **event -> mark dirty -> wake affected key** rather than **wake every key forever**.

## 5. Scoreboard

**Priority: P0**

Current architecture already contains guild-state/version invalidation work. Push this to per-player dirty state.

### Improvements

- Rebuild only the scoreboard state whose dependency changed.
- Guild membership/currency/name/role changes should invalidate affected guild members only.
- Player stat/level/money changes should dirty only that player's relevant entries.
- Avoid rebuilding all lines when one value changes.
- Cache immutable/static scoreboard text.
- Do not recreate Objective/Team/entries when their effective value is unchanged.
- Keep the existing invisible entry-key behavior exactly as-is.

### Result

A single player's change becomes O(1) affected-player work instead of O(P) global refresh work.

## 6. Companion system

**Priority: P0**

`CompanionFollowTask.java`, equipment, combat, and experience are separate components already. fileciteturn553file0L2-L2

### Follow

Current/recurrent follow calculations should be split into:

- event-driven target/state invalidation;
- minimal movement/update work only while the companion actually needs movement.

Events that should dirty a companion:

- owner movement/world change;
- companion spawn/despawn;
- teleport/respawn;
- companion state change;
- combat state change;
- equipment change.

Do **not** change follow distance, movement feel, teleport thresholds, or timing if those are part of current behavior. Only avoid recalculating unchanged inputs.

### Equipment

Replace repeated equipment hashing/derivation with explicit dirty marking from equipment mutation paths. Keep a cached immutable derived combat/stat snapshot.

### Combat

Cache companion-derived damage/stat data until one of its dependencies changes. Do not recompute static definition data per attack.

## 7. Mob scaling

**Priority: P0**

Scaling should be dependency-driven.

Dependencies normally include:

- mob/player association;
- player level;
- effective player equipment/stat multiplier;
- mob base definition.

Cache the effective snapshot and apply attributes only if the effective snapshot changed.

Never scan every managed mob because one player's equipment changed.

## 8. NPC look / interaction

**Priority: P0**

Movement is the natural trigger. Keep block-change filtering and spatial locality.

- Ignore same-block movement when the visual result cannot change.
- Index NPCs spatially by chunk/region.
- Process only NPCs in the affected neighborhood.
- Deduplicate NPCs when movement crosses overlapping lookup areas.
- Avoid all-NPC iteration on every movement event.

Native Paper 26.2 interaction/dialog APIs remain the source of truth; no legacy interaction implementation should be introduced for optimization.

## 9. Region spawning

**Priority: P0**

The existing region/spawn-point model is a strong candidate for fully local processing.

### Target

```text
Player enters/moves into area
    -> affected chunks
    -> indexed spawn points
    -> mark only those points dirty
```

And:

```text
Managed mob dies
    -> exact spawn point
    -> schedule only that point's respawn check
```

Do not retain a global `all spawn points every N ticks` loop merely for convenience.

Keep temporal respawn delays, but schedule them per spawn point.

## 10. Quest system

`QuestPassiveCheckTask` is the main architectural smell in the package: passive checks are often polling derived state that can be invalidated by explicit changes. fileciteturn551file0L2-L2

### Convert to event-driven checks

Potential triggers:

- inventory changed;
- item acquired/removed;
- equipment changed;
- location/world changed;
- level/stat changed;
- mob killed;
- profession state changed;
- quest accepted/completed;
- relevant global event state changed.

Only truly time-dependent objectives should remain scheduled.

`QuestInventoryTracker` should consume inventory changes instead of requiring broad rescans. `QuestMobKillListener` already represents the desired direction. fileciteturn551file0L2-L2

### Navigation

`QuestNavigationService` should cache the current navigation target and invalidate only when:

- quest target changes;
- quest progress changes;
- player world changes;
- relevant target location changes;
- player crosses the spatial threshold at which presentation needs updating.

Avoid synchronous repeated location/target discovery from generic movement/timer code.

## 11. Boss system

**Priority: P0/P1**

Bosses contain inherently temporal behavior, but not every boss operation is temporal.

Separate:

- boss phase/cooldown timers – temporal;
- damage contribution changes – event-driven;
- loot state – event-driven;
- boss death/cleanup – event-driven;
- participant tracking – event-driven;
- definition/config data – immutable/cacheable.

Never scan all active bosses on every player event. Resolve the affected boss from entity UUID/managed-state indexes.

Boss attack patterns should avoid reconstructing immutable configuration objects per execution.

## 12. Combat

**Priority: P0**

Combat is inherently hot-path code.

- Keep active combat state indexed by UUID/entity ID.
- Avoid repeated scans of all combatants.
- Cache derived stats between dependency changes.
- Use event-driven enter/exit state transitions.
- Keep only actual cooldown timers scheduled.
- Avoid database calls from combat events.
- Avoid allocation-heavy logging/messages on every hit unless debugging is enabled.
- Deduplicate repeated target/attacker resolution.

The public combat API already exposes explicit combat enter/exit events; use those boundaries to drive consumers instead of independent polling.

## 13. Economy / money / guild bank

**Priority: P1**

Money and guild-bank state should be memory-first during active gameplay, with dirty persistence.

### Target

```text
transaction
  -> mutate in-memory state atomically
  -> mark affected player/guild dirty
  -> persist asynchronously/coalesced
```

Do not perform a database round trip for every UI refresh or repeated read.

Guild bank inventory changes should invalidate only the guild-bank viewer/session that needs an update, not every open GUI.

Transactions must remain atomic and must never expose partially persisted state.

## 14. Database / persistence

**Priority: P0**

This is one of the most important scalability areas.

### Rules

- Reads required during gameplay should come from in-memory state after initial load.
- Writes should be coalesced by player/guild/entity key.
- Multiple mutations before a flush should produce one persistence operation where safe.
- Never block the main server thread on JDBC.
- Bound the asynchronous executor/queue.
- Use HikariCP efficiently: one pool, prepared statements, sensible pool size, no per-operation pool creation.
- Avoid repeatedly opening/closing connections for related writes.
- Flush dirty state at controlled intervals and on lifecycle boundaries.
- On shutdown, stop accepting new work, drain pending writes, then close resources.
- Preserve transactional semantics for multi-table state.

## 15. AsyncFileWriter

`AsyncFileWriter` exists and should remain the single owner of asynchronous file persistence rather than allowing feature-specific ad-hoc executors. fileciteturn544file0L2-L2

Required properties:

- bounded/coalesced writes;
- atomic replacement where appropriate;
- no lost writes during shutdown;
- deterministic executor shutdown;
- no main-thread blocking;
- no duplicate queued snapshots for the same key.

## 16. Playtime tracking

Elapsed playtime is genuinely time-dependent, but persistence is not.

Use:

```text
join -> establish start timestamp
runtime -> derive elapsed time when needed
meaningful state change/flush interval -> dirty
quit/shutdown -> final flush
```

Avoid writing player playtime continuously or creating database work from every tick.

## 17. Inventory / equipment / item systems

**Priority: P0/P1**

### Event-driven invalidation

Inventory/equipment mutation events should publish narrow changes:

- slot changed;
- item identity changed;
- quantity changed;
- equipment slot changed.

Consumers should subscribe only to what they need.

Examples:

```text
EquipmentChanged
 -> stat cache dirty
 -> scaling dirty if relevant
 -> scoreboard dirty if displayed
 -> companion dirty if companion equipment is affected
```

Avoid each consumer independently comparing the entire inventory/equipment set.

## 18. GUI

**Priority: P1**

GUI rendering should be reactive, not polling.

- Keep per-viewer state.
- Dirty only the viewer whose backing state changed.
- Re-render only changed slots/sections when safe.
- Cache static ItemStacks/components where immutable.
- Do not query the database during inventory click/render paths.
- Close/invalidate GUI sessions on player lifecycle events.
- Avoid retaining Player references in long-lived GUI registries.

## 19. Skin / HTTP services

**Priority: P1**

External skin resolution is inherently I/O-bound.

- Resolve asynchronously.
- Cache successful and negative results with controlled TTLs.
- Deduplicate concurrent requests by lookup key.
- Never perform URL/HTTP work on the server thread.
- Keep SSRF and HTTPS restrictions.
- Bound response size and URL length.
- Cancel/ignore obsolete requests when a player/entity no longer needs the result.

## 20. Memory and allocation optimization

Audit all hot paths for:

- `new Location(...)` in loops;
- repeated `distance()` where `distanceSquared()` is sufficient;
- temporary `List`/`Set` creation;
- streams/lambdas in tick/event-heavy paths;
- repeated `UUID` conversion/parsing;
- repeated config lookups;
- repeated ItemStack cloning;
- repeated Component construction;
- repeated registry lookups;
- repeated entity type/class checks.

Prefer immutable cached definitions and primitive/simple keys where appropriate.

Do not optimize blindly at the cost of readability; optimize measured/high-frequency paths first.

## 21. Spatial indexing

Any subsystem that repeatedly answers "what is near this location?" should use an index.

Candidates:

- NPCs;
- region spawn points;
- navigation targets;
- managed bosses;
- companions;
- other plugin-owned entities.

Preferred key:

```text
world + chunkX + chunkZ
```

Maintain the index on registration/unregistration/world changes. Never rebuild it during a hot-path query.

## 22. Event fan-out control

Event-driven architecture can become slower than polling if every event triggers many consumers independently.

Therefore:

- use typed domain events;
- keep payloads small;
- coalesce dirty keys;
- process each key once per server cycle where appropriate;
- use dirty flags for multiple dependent views;
- avoid event chains that synchronously trigger ten downstream recalculations.

The target is **one source mutation -> one coalesced refresh per affected subsystem**, not "one mutation -> many immediate refreshes".

## 23. Lifecycle / stability

`LifecycleCoordinator` should be the central contract for startup/shutdown ownership. fileciteturn544file0L2-L2

Every subsystem should define:

```text
start()
  -> register listeners
  -> initialize indexes/caches
  -> start only required temporal schedulers

stop()
  -> stop accepting work
  -> cancel wakes/tasks
  -> flush dirty state
  -> close async resources
  -> unregister/clear state
```

No orphan executors, no tasks surviving plugin shutdown, and no asynchronous callback touching unloaded state.

## 24. Dead-code / legacy cleanup procedure

Dead code must be removed deliberately, not by filename intuition.

For every candidate:

1. Search repository-wide references.
2. Check Java references/imports.
3. Check `paper-plugin.yml` registration.
4. Check configuration keys and serialized class names.
5. Check reflection/string-based lookups.
6. Check command/event registration.
7. Check API exposure.
8. Check tests/build verification scripts.
9. Check startup wiring in `PixelRPGPlugin`/bootstrap.
10. Delete only after all paths are proven unused.

### Candidates to audit first

- classes not referenced from startup wiring;
- duplicate managers/services superseded by newer implementations;
- compatibility helpers for removed Minecraft/Paper versions;
- old polling tasks replaced by event-driven listeners;
- unused fields left after cache/index refactors;
- obsolete APIs whose implementation is no longer registered;
- dead configuration keys;
- stale documentation describing removed architecture.

Do **not** delete public API classes solely because internal code does not use them; first determine whether they are intentionally exposed to other plugins.

## 25. What must NOT be changed for optimization

The following are behavioral invariants unless a separate feature request explicitly changes them:

- player-visible results;
- GUI appearance and behavior;
- scoreboard presentation;
- NPC/companion visual behavior;
- combat outcomes;
- boss mechanics;
- quest semantics;
- item/economy semantics;
- guild-bank semantics;
- spawn/respawn gameplay behavior;
- configured gameplay timing where timing itself is part of the feature;
- current Paper 26.2 interaction/dialog behavior.

Optimization means changing **how** the same result is produced, not changing **what** the player experiences.

## 26. Verification strategy

Every optimization batch must be validated with:

### Functional equivalence

- same player-visible state;
- same event outcomes;
- same economy/item mutations;
- same quest progression;
- same companion behavior;
- same boss/spawn lifecycle.

### Performance

Measure before/after:

- main-thread time per event;
- scheduler task time;
- number of entities/players inspected;
- allocations on hot paths;
- database operations per player/action;
- queue depth;
- cache hit rate;
- number of GUI/scoreboard refreshes;
- event fan-out;
- tick-time contribution under realistic player counts.

### Stability

- startup/shutdown;
- reload-like lifecycle paths where supported by the architecture;
- player join/quit;
- world change/teleport;
- entity death/removal;
- database outage/reconnect;
- file write failure;
- HTTP timeout/failure;
- concurrent rapid state changes.

### Build verification

The existing Gradle verification tasks must remain enabled. `check` must continue to execute all existing forbidden-API, legacy-NMS/CraftBukkit, ChatColor, live-server-reference, relocation, and JDBC service-descriptor checks.

## 27. Recommended implementation order

1. **Inventory/equipment -> dirty event infrastructure**
2. **Scoreboard -> per-player dirty refresh**
3. **Companion -> event-driven derived state + local movement work**
4. **Quest passive checks -> dependency events**
5. **Mob scaling -> dependency snapshots**
6. **NPC/spatial indexes -> local movement processing**
7. **Region spawning -> per-point wake scheduling**
8. **Combat -> indexed active state + cached derived stats**
9. **Boss -> event-driven state + local timers**
10. **GUI -> viewer-local dirty rendering**
11. **Persistence -> dirty snapshots + coalesced async writes**
12. **Playtime -> time-derived state + dirty persistence**
13. **Skin/HTTP -> async deduplicated cache**
14. **Repository-wide dead-code/reference audit**
15. **Final allocation/hot-path pass**
16. **Load/stress verification and regression comparison**

## 28. Definition of done

The optimization phase is complete only when:

- global polling has been removed wherever a reliable event exists;
- genuinely temporal work remains local and bounded;
- repeated changes are coalesced;
- derived state is cached and invalidated by dependencies;
- database/file/network I/O is asynchronous and controlled;
- spatial queries are indexed;
- lifecycle ownership is deterministic;
- dead/unreachable legacy code is removed after reference verification;
- no player-visible behavior has changed;
- existing build verification remains intact;
- `test` builds successfully and runtime behavior is regression-tested;
- performance measurements demonstrate lower main-thread work and lower unnecessary operation counts.

---

**Status:** Audit/roadmap created from the current `test` branch. Implementation is intentionally separate from this document so the optimization work can be executed in measured, behavior-preserving batches.
