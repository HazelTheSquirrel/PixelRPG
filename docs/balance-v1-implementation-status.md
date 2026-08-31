# PixelRPG Balance V1 — Implementation Status

## Scope

This branch now contains the first runtime implementation of the balance foundation described by `balance.md`.

`test` remains untouched.

## Implemented

### 1. Central level-power curve

Generated combat equipment no longer uses the historical `growthMultiplier ^ progress` calculation.

The authoritative curve is:

```text
progress = (itemLevel - 1) / 98
power = 0.05 + 0.95 * progress^1.35
```

Reference points:

| Level | Power |
|---:|---:|
| 1 | 5.0% |
| 10 | 8.8% |
| 20 | 15.4% |
| 30 | 23.4% |
| 40 | 32.4% |
| 50 | 42.3% |
| 60 | 52.9% |
| 70 | 64.2% |
| 80 | 76.0% |
| 90 | 88.4% |
| 99 | 100.0% |

### 2. Shared stat budget

Each generated combat item receives:

```text
Item Level Power
× Rarity Budget
= Item Budget
```

The selected stat lines divide that budget equally. A quality roll ranges from 75% to 100% of the allocated share and therefore cannot inflate an item beyond its budget.

Rarity budgets currently used by this implementation:

```text
Common      0.35
Uncommon    0.50
Rare        0.68
Epic        0.84
Legendary   1.00
Unique      1.00
```

These are implementation baselines, not a claim that final production tuning is complete.

### 3. Category-aware stat pools

Weapons no longer roll armor-only stats indiscriminately, armor no longer receives attack-power rolls by accident, and shields use a smaller defensive pool.

The system still supports multiple build directions because stats such as HP, reach, crit and lifesteal may overlap where the category can reasonably use them.

### 4. Runtime hard caps

The final aggregated values are centrally bounded to:

```text
HP bonus          180 LP  -> 200 LP total
Armor             200
Movement Speed     30.0 %
Reach               5.0 blocks total target
Crit Chance       100.0 %
Crit Damage Bonus 100.0 % -> 3.00x effective crit
Lifesteal            8.0 %
Attack Power        60.0
```

Reach is stored as a PixelRPG attribute bonus and therefore uses a 0.5-block maximum against the standard 4.5-block interaction range. This prevents the documented 5.0-block total target from becoming an unintended 8.0-block range.

### 5. Crit-damage semantics

The public character-stat view now reports the PixelRPG crit-damage bonus rather than the effective multiplier.

The combat calculation continues to use the effective multiplier:

```text
2.00x base + 0–100% PixelRPG bonus
= 2.00x–3.00x effective crit multiplier
```

### 6. Monster gear scaling

The monster scaler no longer derives its gear multiplier from average item level.

It now consumes the player's final active `CachedStats` through a bounded `PlayerPowerIndex`.

The index normalizes the eight core combat stats against their balance caps and maps the result to:

```text
0% effective gear power  -> 0.75x mob gear multiplier
100% effective gear power -> 1.25x mob gear multiplier
```

Player level remains the level axis for the monster's base HP/damage curve.

## Preserved invariants

- Required-level gating remains authoritative.
- Items can remain present in equipment while their PixelRPG stats are inactive below the required level.
- Set pieces are only counted when their required level is met.
- Weapon abilities retain their required-level check.
- Vanilla base attributes are not replaced by PixelRPG bonus values.
- Hard caps are applied at runtime after item and set aggregation.
- `test` was not modified.

## Explicit validation boundary

This branch has not been declared production-balanced solely from formulas.

The repository environment available during this implementation did not expose a runnable Paper 26.2 test server, and the GitHub repository currently has no workflow run associated with the implementation commits. Therefore the following remain mandatory before merging toward `test`:

1. Compile with Java 25 and Paper 26.2.
2. Generate representative Level 1–99 equipment.
3. Test worst/average/best quality rolls.
4. Test defensive, offensive and hybrid builds.
5. Verify the five-block total interaction range on Paper 26.2.
6. Measure normal-mob TTK and player DTK at the documented level checkpoints.
7. Validate weapon abilities against the new 60 Attack Power ceiling.
8. Validate set bonuses and companion bonuses against the centralized caps.
9. Verify the character profile uses the same `CachedStats` semantics.
10. Run a complete build/CI check before merge.

## Decision

The branch implements the mathematical foundation without pretending that server-side TTK/DTK tuning has already been empirically proven.

The next tuning pass should use measured combat results rather than further guessing at multipliers.
