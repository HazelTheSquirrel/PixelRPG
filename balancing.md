# PixelRPG – Aktuelle Balancing-Baseline

Stand: 2026-08-22

Dieses Dokument beschreibt ausschließlich den aktuellen Balancing-Stand. Historische Testwerte und verworfene Systeme werden nicht mehr als aktuelle Referenz geführt.

## Grundprinzip

PixelRPG verwendet eine kontrollierte, deterministische Progression.

Keine zufällige Kernstat-Streuung als versteckter Multiplikator. Keine Mana-Ressource. Keine Blessings. Keine Curses. Keine Gems, Runes oder Sockets.

Die Hauptprogression lautet:

```text
Spieler-Level
    ↓
Attribute / Klasse
    ↓
Equipment
    ↓
Finale Stats
    ↓
Combat / Mob Scaling
    ↓
XP / Loot / Progression
```

## Spieler-Level

Normale Progression: **Level 1–99**.

Level 100 ist reserviert und wird durch das aktuelle Level-System nicht erreicht.

Die XP-Kurve ist bis Level 79 WotLK-inspiriert. Ab Level 80 beginnt eine kontrollierte Endgame-Kurve:

- 80 → 81: ca. `1.7M` zusätzlich
- weitere Level-Inkremente wachsen um `3.5%`
- Level 99 liegt bewusst weit unter der früheren extremen Endgame-Explosion

Die Levelkurve ist damit inspiriert von WotLK, aber nicht 1:1 als WotLK-Kopie gedacht.

## Attribute

Aktive Punktwerte aus `data/attributes.json`:

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

Die Klassenwerte stammen aus `data/class-balance.json`.

| Klasse | Armor | Health | Speed | Melee | Ranged | Spell | Heal | Crit | Crit Damage |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Warrior | 5.0 | 10.0 | 0 | 1.05 | 0.95 | 0.90 | 1.00 | 0 | 1.00 |
| Ranger | 0 | 3.0 | 0.0025 | 0.95 | 1.05 | 1.00 | 1.00 | 3 | 1.00 |
| Rogue | 0 | 0 | 0.002 | 1.00 | 1.00 | 1.00 | 1.00 | 5 | 1.15 |
| Healer | 3.0 | 10.0 | 0 | 0.95 | 0.95 | 1.00 | 1.15 | 0 | 1.00 |
| Mage | -2.0 | -2.0 | 0 | 0.85 | 1.00 | 1.15 | 1.00 | 0 | 1.00 |

## Items

Aktive Datenquelle: `data/item-scaling.json`.

```text
Growth multiplier = 10.0
Weapon damage base = 3.0
Weapon crit base = 0.5
Armor base = 0.7
Health base = 1.2
Tool efficiency base = 1.0
```

Die Levelskalierung wächst deterministisch von Level 1 bis 99.

Rarity multipliziert die resultierenden Item-Werte:

| Rarity | Multiplikator |
|---|---:|
| Common | `1.00` |
| Uncommon | `1.10` |
| Rare | `1.22` |
| Epic | `1.38` |
| Legendary | `1.60` |
| Unique | `1.60` |

Unique ist nicht als normale Zufallsrarität gedacht.

## Weapon-Abilities

Waffenfähigkeiten besitzen aktuell:

- Rechtsklick als Trigger
- waffenbezogenen Cooldown
- keine Mana-Kosten
- keine globale Mana-Ressource

Die vorhandenen Ability-Implementierungen sind noch fest im `WeaponAbilityEngine` definiert. Das ist bewusst kein neues datengetriebenes Ability-Framework.

Bogen-/Schussmechaniken bleiben ein späterer Designpunkt und werden hier nicht künstlich vorweggenommen.

## Mob Scaling

Aktive Baseline aus `config.yml` / `data/mob-scaling.json`:

- HP pro Level: `5.0`
- Damage pro Level: `0.30`
- Player-Parity-Multiplier: `1.05`
- XP pro Max-Health: `4.0`

| Dimension | HP | Damage | Base Level |
|---|---:|---:|---:|
| Overworld | 1.00 | 1.00 | 1 |
| Nether | 1.15 | 1.10 | 50 |
| The End | 1.30 | 1.20 | 75 |

Der wichtigste offene Balance-Test ist die Interaktion zwischen diesem Scaling und Vanilla-Spielern in derselben Welt.

## Companion

Companion-Combat ist aktuell bewusst deaktiviert.

`companion-stats.json` enthält die spätere Rarity-Basis:

| Rarity | Health | Damage | Speed |
|---|---:|---:|---:|
| Common | 0.70 | 0.70 | 0.95 |
| Uncommon | 0.82 | 0.82 | 0.98 |
| Rare | 0.95 | 0.95 | 1.00 |
| Epic | 1.08 | 1.08 | 1.02 |
| Legendary | 1.22 | 1.18 | 1.04 |
| Unique | 1.00 | 1.00 | 1.00 |

Diese Werte sind für die spätere Combat-Phase vorbereitet und derzeit nicht als aktive Companion-Kampfwerte zu behandeln.

## Bewusst nicht vorhanden

- Mana
- Mana-Regeneration
- Mana-Kosten
- Mana-HUD
- Blessings
- Curses
- Gems
- Runes
- Sockets
- alte F–S-Ranks

## Aktueller Status

Das Balancing ist strukturell definiert, aber noch nicht final durch reale Servertests validiert.

Deshalb werden aktuell keine weiteren Einzelwerte auf Verdacht angepasst.

```text
Build
 ↓
Server-Test
 ↓
Messwerte
 ↓
Abweichung feststellen
 ↓
gezielt korrigieren
```

Keine neuen Systeme, bevor diese Baseline nicht praktisch validiert wurde.
