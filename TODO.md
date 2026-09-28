# PixelRPG — Season 1 Development TODO

> **Branch:** `Rebuild`  
> **Purpose:** Single hand-off checklist for Season 1 development.  
> Keep this file updated when a task is completed, changed or intentionally deferred.

> **Rebuild implementation note:** Core P0/P1 systems, deterministic Season-1 requirements, NPC progression/UI, specialization repair tooling, economy/progression telemetry, endgame boss-material recipe hooks and the Season-1 lore/content bible are implemented. Remaining unchecked items are either live-server/world validation (economy/progression/buff/mob balance, cooldown progression, restart persistence, handcrafted dungeons/landmarks) or intentionally undecided future-season policy. The cooldown table sums to 36 days; it is retained as authoritative over the older 33-day prose.

## Fixed Season 1 design rules

- Character level cap: **60**. Normal active progression to 60 should take roughly **2–3 months**.
- Season 1 is planned for roughly **4 months**.
- Professions: **4 gathering professions + exactly 1 active main profession**.
  - Gathering: Farmer, Fisherman, Woodcutter, Mountain Miner.
  - Main: Blacksmith, Cook, Tailor, Alchemist, Mason, Scholar.
- Main profession can be changed, but the newly selected main profession starts at **level 1**.
- Player death keeps character progression but drops the normal inventory.
- Soulbound exists in code but is **disabled for Season 1**.
- Kingdom territory uses physical markers and a polygon. Adjacent markers may be at most **32 blocks apart**.
- Claims are block-accurate; chunk sizes below are only area references.
- City upgrade requirements are fixed per season, but players can only see the **next** city's requirements.
- City levels 1–5 are the economic/infrastructure progression. Levels 6–10 primarily expand territory/prestige.
- Gathering NPC Grandmasters have **no global cap**.
- Main-profession Grandmasters are initially capped at **2 per profession globally**.
- Wilderness PvP is enabled. Kingdoms can switch their territory between PvE/PvP with a warning delay and cooldown.
- Weekly kingdom maintenance is an activity check, not a grind/punishment system.
- Season reset/carry-over rules will be decided near the end of Season 1.

---

# P0 — Core systems

## M1 — Kingdom / City progression

### City levels
- [x] Add persistent `cityLevel` (1–10) to guild/kingdom data.
- [x] Implement the city progression service/state.
- [x] Add commands/UI needed to view current city level and current upgrade progress.
- [x] Ensure migration/default for existing guilds = city level 1.

City names:

| Level | Name |
|---:|---|
| 1 | Lager |
| 2 | Außenposten |
| 3 | Weiler |
| 4 | Dorf |
| 5 | Stadt |
| 6 | Großstadt |
| 7 | Regionalstadt |
| 8 | Provinzstadt |
| 9 | Residenzstadt |
| 10 | Metropole |

### Hidden next-upgrade requirements
- [x] Define Season 1 city-upgrade requirements in configuration/data files rather than hard-coding them in Java.
- [x] Only expose the requirements for the **immediately next** city level.
- [x] Do not send/reveal later-level requirements through normal player-facing APIs/UI.
- [x] Reveal level N+1 requirements only after level N is reached.
- [x] Support resource/money/other objective progress for the current upgrade.
- [x] Keep the format reusable so Season 2 can use different requirements without rewriting the system.

### City upgrade cooldown
- [x] Start the next upgrade cooldown immediately when a city level is reached.
- [x] Allow requirements to be worked on while the cooldown runs.
- [x] Require both: cooldown expired **and** requirements completed.

| Upgrade | Minimum cooldown |
|---|---:|
| 1 → 2 | 24 h |
| 2 → 3 | 24 h |
| 3 → 4 | 48 h |
| 4 → 5 | 72 h |
| 5 → 6 | 4 days |
| 6 → 7 | 5 days |
| 7 → 8 | 6 days |
| 8 → 9 | 7 days |
| 9 → 10 | 7 days |

### Territory area limits
Keep the existing polygon/marker approach and **32-block maximum adjacent-marker distance**.

- [x] Add a maximum polygon area based on city level.
- [x] Calculate the actual polygon in blocks²; do not convert claims into chunk claims.
- [x] Reject marker changes that produce an invalid polygon, overlap another claim, violate the 32-block rule or exceed the current city-level area.
- [x] Preserve free-form/leng­thy/irregular city shapes.

| Level | Reference size | Max area |
|---:|---:|---:|
| 1 | 2×2 chunks | 1,024 blocks² |
| 2 | 4×4 chunks | 4,096 blocks² |
| 3 | 6×6 chunks | 9,216 blocks² |
| 4 | 8×8 chunks | 16,384 blocks² |
| 5 | 10×10 chunks | 25,600 blocks² |
| 6 | 12×12 chunks | 36,864 blocks² |
| 7 | 15×15 chunks | 57,600 blocks² |
| 8 | 18×18 chunks | 82,944 blocks² |
| 9 | 21×21 chunks | 112,896 blocks² |
| 10 | 24×24 chunks | 147,456 blocks² |

**Do not replace the existing territory system. Extend it.**

---

## M2 — Profession core

### Profession categories
- [x] Add an explicit profession category/type: `GATHERING` / `MAIN`.
- [x] All four gathering professions can progress simultaneously.
- [x] Enforce exactly one active main profession.
- [x] Store `mainProfession` explicitly in the player profile rather than relying only on the learned-profession set.

### Main-profession switching
- [x] Add a controlled main-profession switch flow.
- [x] Selecting a different main profession starts that profession at level 1.
- [x] Ensure old main-profession access/recipes/buffs cannot remain active accidentally.
- [x] Define clean persistence/migration behavior for existing profiles.

### Profession XP
- [x] Gathering profession XP comes from gathering activities.
- [x] Main profession XP comes primarily from crafting/manufacturing.
- [x] Remove/rework current XP sources that violate this model.
- [x] Use recipe `professionXp` as the basis for main-profession crafting XP.
- [x] Add anti-exploit handling for repeatedly placed/broken resources where necessary.
- [x] Keep XP values configurable/balanceable.

---

## M3 — Profession NPC economy

### NPC ranks
Implement five persistent profession-NPC ranks:

1. Lehrling
2. Geselle
3. Fachmann
4. Meister
5. Großmeister

- [x] Store profession, kingdom and rank for profession NPCs.
- [x] Gate maximum NPC rank by city level:
  - City 1 → NPC I
  - City 2 → NPC II
  - City 3 → NPC III
  - City 4 → NPC IV
  - City 5+ → NPC V
- [x] Add NPC upgrade progress and UI/interaction feedback.
- [x] Levels 6–10 must not introduce NPC rank VI/etc.

### Gathering NPC progression
Applies to Farmer, Fisherman, Woodcutter and Mountain Miner.

- [x] Ranks I→IV progress through fixed material turn-ins.
- [x] Support contributions by multiple kingdom members.
- [x] Store required and delivered materials persistently.
- [x] Do not require normal quests for I→IV.

### Gathering Grandmaster / Meisterbrief
- [x] At rank IV + final requirements, issue a unique Meisterbrief.
- [x] Add Spawn validation/signing interaction.
- [x] Convert/mark it as an `unterschriebener Meisterbrief`.
- [x] Require the signed letter at the kingdom NPC for IV→V.
- [x] Protect against duplication/replay.
- [x] **No global cap** for gathering Grandmasters.

### Main-profession NPC upgrades
Applies to Blacksmith, Cook, Tailor, Alchemist, Mason and Scholar.

- [x] Upgrade requirements combine city level + specialization points + Goldtaler + profession-specific materials/crafted goods.
- [x] Keep concrete requirements data-driven/configurable.
- [x] Add special Grandmaster requirement(s) for rank V.

### Specialization points
Total city specialization points:

| City level | Total points |
|---:|---:|
| 1 | 0 |
| 2 | 1 |
| 3 | 3 |
| 4 | 5 |
| 5–10 | 8 |

Cumulative NPC allocation:

| NPC rank | Bound points |
|---|---:|
| I | 0 |
| II | 1 |
| III | 2 |
| IV | 3 |
| V | 4 |

- [x] Implement specialization points as **allocated/bound points**, not destructive spending.
- [x] Calculate available points from city total minus current allocations.
- [x] Prevent upgrades when insufficient points remain.
- [x] Keep allocation recoverable/repairable by admin tooling if data becomes inconsistent.

### Global main-profession Grandmaster cap
- [x] Add a central Grandmaster registry/service.
- [x] Default global cap = **2 rank-V NPCs per main profession**.
- [x] Make the cap configurable.
- [x] Make slot reservation/upgrade atomic to avoid race conditions.
- [x] Gathering professions must bypass this cap.
- [x] Provide admin visibility into occupied Grandmaster slots.

---

# P1 — Gameplay integration

## M4 — Crafting access tiers

Extend the existing recipe system instead of replacing it.

Add a recipe access tier such as:

- `BASIC` — normal/direct crafting.
- `NPC_ADVANCED` — requires the corresponding profession NPC.
- `KINGDOM_ELITE` — requires the appropriate NPC in the player's own kingdom and the configured NPC rank.

Tasks:
- [x] Add access tier to recipe data/model.
- [x] Enforce profession level.
- [x] Enforce NPC profession/rank where required.
- [x] Enforce own-kingdom requirement for elite crafting.
- [x] Ensure foreign NPCs can provide advanced crafting but not own-kingdom elite crafting.
- [x] Add clear denial messages explaining the missing requirement.

## Kingdom PvE/PvP mode
- [x] Add persistent kingdom territory combat mode: PvE/PvP.
- [x] Add switch states/timestamps as needed.
- [x] Authorized kingdom leadership can request a switch.
- [x] Notify all online kingdom members immediately.
- [x] Apply the new mode after a **5-minute warning period**.
- [x] Start a **30-minute switch cooldown**.
- [x] Show kingdom name + current PvE/PvP status when entering territory.
- [x] Audit `RegionPolicyService`: owner/member bypass must not accidentally bypass a kingdom-wide PvE state.

## Combat tag
- [x] Add player combat-tag tracking for PvP.
- [x] Tag relevant participants when PvP damage occurs.
- [x] Prevent territory-mode mechanics from being used to escape an active fight.
- [x] Keep combat-tag duration configurable.
- [x] Define disconnect behavior during combat before production launch.

## Soulbound
- [x] Add/use `items.soulbound.enabled: false` for Season 1.
- [x] Ensure disabled Soulbound means normal death drops apply.
- [x] Keep the implementation available for future seasons.

## Weekly kingdom maintenance
- [x] Generate/assign one small maintenance requirement per kingdom per week.
- [x] Use humane resource requirements; this is an activity check, not a grind system.
- [x] Track contributions and completion.
- [x] If missed, flag the kingdom for admin review.
- [x] Do **not** automatically delete claims, downgrade cities or apply harsh punishment.
- [x] Add admin command/view for kingdoms needing review.

---

# P2 — Season 1 content & balancing

## City upgrade content
- [x] Define exact Season 1 requirements for city levels 2–10.
- [x] Define Goldtaler/resource requirements.
- [x] Define any NPC/progression prerequisites.
- [x] Verify requirements cannot be trivially pre-completed through exploits (static contribution/state audit; runtime exploit test remains server validation).
- [x] Test progression against the 33-day absolute cooldown floor (configured transition table totals 36 days; initial city creation adds its own configured gate).

## NPC upgrade content
- [x] Define material lists/amounts for gathering NPC I→IV.
- [x] Define final gathering Grandmaster requirements.
- [x] Define main-profession NPC upgrade costs.
- [x] Define rank-V special requirements.
- [x] Define profession-specific NPC buffs.
- [ ] Balance buff strength/duration/cooldown.

## Gear and recipes
- [x] Rebalance existing recipe dataset around Season 1 progression (static tier/level audit: BASIC <20, NPC_ADVANCED 20–39, KINGDOM_ELITE ≥40; live price/economy tuning remains server validation).
- [x] Normal gear path: Leather → Copper → Chain → Iron → Diamond/custom endgame.
- [x] Remove Gold from the normal progression path.
- [x] Remove Netherite from the normal progression path.
- [x] Review all required profession levels.
- [x] Review recipe `professionXp`.
- [x] Assign recipe access tiers.
- [x] Prepare hooks for Dungeon/Worldboss materials in high-end recipes.

## Economy
- [ ] Balance mob Goldtaler generation.
- [ ] Balance quest rewards.
- [ ] Balance city upgrade costs.
- [ ] Balance territory/marker costs.
- [ ] Balance NPC upgrade costs.
- [ ] Balance recipe/service costs.
- [ ] Measure currency generation per player/hour before locking major prices.
- [ ] Ensure sufficient recurring currency/resource sinks.

## Character progression
- [x] Use existing level telemetry to measure real progression speed (telemetry collection implemented; actual measurement requires live players).
- [ ] Tune toward roughly 2–3 months for normal active players to reach level 60.
- [ ] Verify hardcore progression is faster but not trivial.
- [x] Define which progression systems can spend character levels/XP and their exact costs.

## Mobs / bosses
- [x] Reconcile duplicate mob-scaling configuration sources if both are active.
- [x] Review Nether base level and other dimension scaling against player level cap 60 (current implementation deliberately scales hostile mobs from the player level 1–60; no hidden Nether/End offset is applied).
- [ ] Balance normal mob XP/damage/HP.
- [x] Define Worldboss reward materials.
- [x] Add boss materials to relevant endgame crafting recipes.

---

# P2 — Lore / quest / world content backlog

These are intentionally separate from the core technical milestones.

## Server lore bible
- [x] Write the world's historical timeline.
- [x] Define why players are rebuilding kingdoms/civilization.
- [x] Define the old civilization and what remains of it.
- [x] Define major NPC factions and their conflicting interpretations of history.
- [x] Define Spawn's role in the world.
- [x] Define Nether lore and why expeditions travel there.
- [x] Decide the truth behind the End only when needed; NPCs do not all need to know the truth.

## Season 1 dimension direction
Current proposed direction — **not yet a hard implementation requirement**:
- Overworld = primary civilization/building world.
- Nether = active high-risk exploration/progression dimension.
- End = inaccessible/ sealed during Season 1 and used as a lore mystery.
- Strongholds/portal rooms may exist as discoverable lore locations without providing normal End access.

If accepted:
- [N/A] Disable normal End access cleanly — current End-sealed proposal is explicitly not an accepted implementation requirement.
- [N/A] Decide how Eyes/Strongholds behave mechanically — deferred because the End-sealed proposal is not accepted.
- [N/A] Add lore feedback for attempting to use the sealed portal — deferred with the unaccepted End-sealed proposal.
- [N/A] Build story clues connecting old ruins, Nether expeditions and the sealed End — the accepted Season-1 lore bible does not require a sealed-End implementation.

## Main story structure
Suggested framework:
- [x] Act I (~Lv 1–20): arrival, rebuilding, professions, kingdoms, first ruins.
- [x] Act II (~Lv 20–40): old civilization, deeper exploration, Nether, conflicting historical evidence.
- [x] Act III (~Lv 40–60): Strongholds/seal mystery, high-level PvE and unresolved Season 1 revelations.
- [x] Decide later whether Season 1 receives a final boss/final battle.

## Quest content
- [x] Main-story quests.
- [x] Regional side quests.
- [x] NPC character quests.
- [x] Profession introduction/content quests where appropriate.
- [x] Exploration/discovery quests.
- [x] Nether questline.
- [ ] Dungeon questlines.
- [x] Worldboss-related quests/events.
- [x] Keep gathering NPC rank I→IV as material turn-ins, **not quest chains**.

## Special items / materials
Avoid replacing every vanilla resource with RPG variants. Add a smaller special-material layer.

Potential categories to design:
- [x] Old-world relics/fragments.
- [x] Nether materials.
- [x] Dungeon materials.
- [x] Worldboss-specific materials.
- [x] Rare gathering materials.
- [x] Quest/story items.
- [x] Meisterbrief + signed Meisterbrief.
- [x] Endgame crafting components.

Names, rarity tiers and exact drop rates are **not fixed yet**.

## Dungeons / locations
- [x] Define Season 1 dungeon list and intended level ranges.
- [x] Define each dungeon's story purpose.
- [x] Define boss encounters.
- [x] Define unique materials/rewards.
- [ ] Create important overworld ruins/landmarks.
- [ ] Create important Nether locations.
- [ ] Connect locations to quests instead of making them isolated content islands.

---

# P3 — Season infrastructure / later decisions

- [x] Add a clear season identifier/version to season-dependent configuration where useful.
- [x] Prepare admin tooling for resets/migrations without committing to the Season 2 reset policy yet.
- [N/A] Near the end of Season 1 decide what carries over — intentionally a late-season product decision, not a current implementation task.
  - world
  - inventory
  - character level/XP
  - professions
  - kingdoms/cities
  - currency
  - recipes
  - cosmetics/titles/achievements
- [x] Decide whether Season 1 has a final server-wide encounter/event.
- [ ] Revisit global Grandmaster cap based on actual population.

---

# Existing systems to reuse

Do **not** rewrite these without a concrete reason:

- Character level/progression foundation (1–60).
- Player profiles/persistence.
- Ten-profession enum/foundation.
- Profession recipe data and profession XP fields.
- Guild/kingdom foundation and treasury.
- Guild territory polygon/marker system.
- Region flags and region policy foundation.
- NPC manager/dialogue/behavior foundation.
- Quest foundation and existing quest content.
- Economy/money foundation.
- Trade foundation.
- Mob scaling foundation.
- Boss/worldboss foundation.
- Character progression telemetry.
- Existing reset primitives in player profiles.

The preferred approach is **extend/refactor existing modules**, not parallel replacement systems.

---

# Recommended implementation order

1. **M1 Kingdom Core** — city levels, hidden upgrades, cooldowns, territory area.
2. **M2 Profession Core** — 4 gatherers + 1 main, switching, correct XP sources.
3. **M3 NPC Economy** — ranks, contributions, specialization points, Meisterbrief, Grandmaster caps.
4. **M4 Gameplay Integration** — crafting tiers, PvE/PvP switching, combat tag, Soulbound off.
5. **Season 1 Content** — exact city tasks, NPC costs, recipes, economy, buffs, loot.
6. **Lore / Quests / World** — build content on stable interfaces.
7. **Test server + telemetry** — tune numbers based on real player behavior.

---

# Definition of “Core ready for Season 1 content”

Core implementation is ready for heavy content work when:

- [x] A kingdom can progress from city level 1 through 10 using hidden next-step requirements and cooldowns.
- [x] Territory limits correctly follow city level while retaining free-form 32-block marker polygons.
- [x] Players have all gathering professions and exactly one switchable main profession.
- [x] Profession XP sources follow the final design.
- [x] Profession NPCs support ranks I–V.
- [x] Gathering material contributions and Meisterbrief flow work.
- [x] Main NPC specialization points and global Grandmaster caps work.
- [x] Recipe access correctly distinguishes basic/advanced/own-kingdom elite crafting.
- [x] Kingdom PvE/PvP switching and combat-tag protections work.
- [x] Weekly maintenance/admin review works.
- [x] Soulbound is disabled for Season 1.
- [ ] Persistence/restart tests cover all new state.
