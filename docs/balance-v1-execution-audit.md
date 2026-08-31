# PixelRPG Balance V1 — Execution Audit

**Basis:** `test` branch at `ede67647a1ef9533b673c2ab23cd7a0f13128f28`
**Working branch:** `balance/v1-foundation`
**Purpose:** Safe execution boundary for the Level 1–99 equipment/combat rebalance.

## 1. Execution rule

No blind numerical rewrite is performed against `test`.

The balance document contains both binding targets and explicitly provisional values. Runtime changes must therefore be introduced only after the complete stat flow has been reconciled from item generation through character attributes, combat damage, monster scaling, set bonuses and abilities.

`test` remains untouched. All balance work is isolated on this branch.

## 2. Confirmed current flow

```text
ItemDefinitionRegistry
    -> ItemService
    -> RPGItemBuilder
    -> PDC item stats
    -> StatEngine
    -> Bukkit/Paper attributes
    -> CombatDamageCalculator / CombatDamageListener
```

Monster scaling independently consumes player level and equipped item levels.

## 3. Critical findings before numerical rebalance

### 3.1 Item growth is currently incompatible with the target curve

`RPGItemBuilder` currently calculates:

```text
progress = (itemLevel - 1) / 98
levelFactor = growthMultiplier ^ progress
```

with `growthMultiplier = 10.0` and an additional random `0.50–1.50` multiplier.

The balance specification instead defines:

```text
progress = (itemLevel - 1) / 98
power = 0.05 + 0.95 * progress^1.35
```

These are materially different systems. Replacing only the JSON growth value cannot produce the specified curve.

### 3.2 Stat budget is not yet implemented

The current generator independently calculates every selected stat from the same level factor and rarity multiplier.

A Legendary/Unique currently receives eight selected stat types from an eight-stat pool. Consequently, an eight-stat roll selects the entire pool.

The target design requires a bounded total item budget so that stat count and total power are separate concepts.

### 3.3 Reach semantics need a single authoritative definition

The balance specification targets `5.0` blocks as total reach while also distinguishing vanilla base values from PixelRPG bonus values.

The current `StatEngine` stores item reach as a direct attribute addition to both block and entity interaction range. Before rebalance, the implementation must decide whether the persisted stat represents:

- PixelRPG bonus reach, or
- final total reach.

The target document currently describes `5.0` as total reach. This must not accidentally become `vanilla + 5.0`.

### 3.4 Crit damage representation must remain consistent

The runtime currently stores `2.0 + PixelRPG bonus` as the effective critical multiplier.

The balance specification defines the PixelRPG value as a percentage bonus over the `2.0x` base. Therefore:

```text
0%   -> 2.00x
50%  -> 2.50x
100% -> 3.00x
```

The runtime cap currently permits a multiplier of `10.0x`, which is inconsistent with the proposed `+100%` target and must be reconciled.

### 3.5 Lifesteal requires end-to-end validation

Lifesteal is applied from actual final damage. Because the target HP cap is 200 LP, even moderate lifesteal becomes materially stronger as attack power and attack frequency increase.

The proposed `8.0%` value must therefore be evaluated together with attack power, critical hits, weapon abilities and monster mitigation rather than tuned independently.

### 3.6 Monster gear scaling currently uses average item level

The current monster scaler derives a gear multiplier from the average level of all equipped items, including the main/off hand and armor slots.

This is not the same as measuring the actual offensive/defensive stat power described by the new stat-budget model.

A numerical item rebalance without revisiting this relationship can cause monsters to scale against an obsolete power proxy.

## 4. Existing safety that should be preserved

The current `StatEngine` already gates item stats by `requiredLevel` before contributing them to character stats.

Generated equipment also writes its item level as its required level.

Weapon abilities independently verify the required level before execution.

These behaviors are preserved as functional invariants during the rebalance.

## 5. Required implementation order

1. Freeze the target mathematical definitions.
2. Introduce one authoritative level-power function.
3. Introduce explicit rarity/slot/stat budgets.
4. Define stat allocation and roll quality without allowing budget overflow.
5. Enforce hard caps centrally at aggregation time.
6. Reconcile reach semantics with Paper attributes.
7. Reconcile critical-damage storage/display semantics.
8. Recalculate monster scaling from the new player-power model.
9. Validate weapon abilities against the new attack-power scale.
10. Validate set bonuses against the same caps/budget philosophy.
11. Build the complete Level 1–99 reference table.
12. Run build/CI validation before any merge toward `test`.

## 6. Non-negotiable invariants

- No changes to `test` during this implementation phase.
- No legacy Minecraft API migration.
- No change to intended feature behavior merely to simplify balancing.
- PixelRPG bonus stats remain zero without active PixelRPG equipment/set bonuses.
- Required-level gating remains authoritative.
- Vanilla base attributes remain intact.
- Hard caps are enforced centrally, not only in item lore.
- Set bonuses cannot bypass stat caps.
- Companion bonuses cannot bypass stat caps.
- Weapon abilities cannot activate below their required level.
- Random rolls cannot create values above the item/stat budget.

## 7. Current decision

The safest first deliverable is the balance foundation branch, not an immediate mass rewrite of `RPGItemBuilder` and combat values.

The repository already contains enough runtime coupling that changing the numbers first would make failures difficult to distinguish between item generation, stat aggregation, monster scaling and combat mitigation.

The next implementation stage should therefore modify the mathematical foundation first, then tune the resulting complete system against Level 1–99 builds and TTK/DTK targets.
