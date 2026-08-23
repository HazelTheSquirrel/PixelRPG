# PixelRPG – Aktuelle Balancing-Baseline

Stand: 2026-08-23

> Fachreferenz für das aktuelle Balancing. Der technische Gesamtstatus steht in `audit.md`.

## Grundprinzip

PixelRPG verwendet kontrollierte, deterministische Progression.

Nicht Bestandteil der aktuellen Progression:

- Mana
- Blessings
- Curses
- Gems
- Runes
- Sockets
- alte F–S-Ranks

Hauptprogression:

```text
Spieler-Level → Attribute/Klasse → Equipment → finale Stats → Combat/Mob Scaling → XP/Loot
```

## Spieler-Level

- reguläre Progression: **Level 1–99**
- Level 100 ist reserviert bzw. Endgame
- XP-Kurve bis Level 79 WotLK-inspiriert
- ab Level 80 kontrollierte Endgame-Kurve
- keine weitere Anpassung ohne reale Testdaten

## Attribute

Aktive Quelle: `src/main/resources/data/attributes.json`.

| Attribut | Effekt pro Punkt |
|---|---:|
| Vitality | `+2 HP` |
| Agility | `+0.0025` Bewegungsgeschwindigkeit |
| Agility | `+0.20%` Crit Chance |
| Precision | `+0.75` Bonus Damage |
| Range | `+0.10` Block Interaction Range |
| Range | `+0.10` Entity Interaction Range |
| Toughness | `+1 Armor` |

Kosten:

- Base: `30.0`
- Multiplikator: `1.08`
- Klassenrabatt für Primärattribute: `0.75`
- Soulview: `500.0`
- Elytra Permit: `750.0`

## Klassen

Aktive Quelle: `data/class-balance.json`.

| Klasse | Armor | Health | Speed | Melee | Ranged | Spell | Heal | Crit | Crit Damage |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Warrior | 5.0 | 10.0 | 0 | 1.05 | 0.95 | 0.90 | 1.00 | 0 | 1.00 |
| Ranger | 0 | 3.0 | 0.0025 | 0.95 | 1.05 | 1.00 | 1.00 | 3 | 1.00 |
| Rogue | 0 | 0 | 0.002 | 1.00 | 1.00 | 1.00 | 1.00 | 5 | 1.15 |
| Healer | 3.0 | 10.0 | 0 | 0.95 | 0.95 | 1.00 | 1.15 | 0 | 1.00 |
| Mage | -2.0 | -2.0 | 0 | 0.85 | 1.00 | 1.15 | 1.00 | 0 | 1.00 |

## Items

Aktive Quelle: `data/item-scaling.json`.

```text
Growth multiplier = 10.0
Weapon damage base = 3.0
Weapon crit base = 0.5
Armor base = 0.7
Health base = 1.2
Tool efficiency base = 1.0
```

Rarity-Multiplikatoren:

| Rarity | Multiplikator |
|---|---:|
| Common | `1.00` |
| Uncommon | `1.10` |
| Rare | `1.22` |
| Epic | `1.38` |
| Legendary | `1.60` |
| Unique | `1.60` |

Unique ist keine normale Zufallsrarität.

## Weapon-Abilities

- Rechtsklick als Trigger
- waffenbezogener Cooldown
- keine Mana-Kosten
- keine globale Mana-Ressource

Die vorhandenen Fähigkeiten bleiben zunächst bewusst im bestehenden `WeaponAbilityEngine`-Modell. Kein neues datengetriebenes Ability-Framework auf Verdacht.

## Mob Scaling

Aktive Baseline:

- HP pro Level: `5.0`
- Damage pro Level: `0.30`
- Player-Parity-Multiplier: `1.05`
- XP pro Max-Health: `4.0`

| Dimension | HP | Damage | Base Level |
|---|---:|---:|---:|
| Overworld | 1.00 | 1.00 | 1 |
| Nether | 1.15 | 1.10 | 50 |
| The End | 1.30 | 1.20 | 75 |

**Offener Test:** Verhalten bei gleichzeitig registrierten PixelRPG-Spielern und Vanilla-Spielern in derselben Welt.

## Companion

Companion-Combat ist aktuell nicht als aktive Kernprogression zu behandeln.

Die vorbereiteten Rarity-Basiswerte in `companion-stats.json` bleiben Datenbasis für die spätere Combat-Phase.

## Aktueller Status

Die Baseline ist strukturell festgelegt, aber noch nicht final durch reale Servertests validiert.

```text
Build → Server-Test → Messwerte → Abweichung → gezielte Korrektur
```

**Keine Balancing-Werte auf Verdacht verändern.**
