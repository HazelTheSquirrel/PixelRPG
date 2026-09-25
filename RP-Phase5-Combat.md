# PixelRPG – RP Phase 5 Combat

**Status:** Phase 5 abgeschlossen  
**Branch:** `test`  
**Referenzbranch:** `main` ausschließlich als Soll-/Vergleichszustand  
**Prüfdatum:** 25.09.2026

## 1. Ziel

Phase 5 behandelt den Combat-Kern gemäß RP.md Abschnitt 68:

- Mob-HP
- Mob-Damage
- Mob-XP
- Loot
- Player Parity
- Biome-Bosse
- World-Bosse

Die vorhandenen Systeme wurden weiterverwendet. Unbelegte Livewerte wie reale Killzeit oder Kills/Stunde wurden nicht erfunden.

## 2. Normale Mobs

Die verbindliche Skalierung kommt aus `data/mob-scaling.json`:

| Parameter | Wert |
|---|---:|
| Basis-HP | 20 + Level × 2,5 |
| Basis-Damage | 2 + Level × 0,15 |
| XP/MaxHealth | 4,0 |
| Player-Parity | 1,00 |
| Gear-Multiplikator | 0,75–1,25 |
| Charakter-/Mob-Level | 1–60 |

### Referenzwerte

| Level | HP | XP | Basis-Damage |
|---:|---:|---:|---:|
| 1 | 22,5 | 90 | 2,15 |
| 10 | 45,0 | 180 | 3,50 |
| 20 | 70,0 | 280 | 5,00 |
| 30 | 95,0 | 380 | 6,50 |
| 40 | 120,0 | 480 | 8,00 |
| 50 | 145,0 | 580 | 9,50 |
| 60 | 170,0 | 680 | 11,00 |

Die XP-Berechnung verwendet die gespeicherte ursprüngliche Maximal-HP des skalierten Mobs. Dadurch wird ein späteres Re-Skalieren nicht mehrfach als XP-Quelle gewertet.

## 3. Player Parity

Aktive RPG-Mobs werden an die höchste aktive Spielerprogression des Kampfes angepasst.

Berücksichtigt werden:

- höchstes aktives Charakterlevel
- durchschnittliches Itemlevel der getragenen relevanten Ausrüstung
- Gear-Multiplikator 0,75–1,25

Die Levelgrenze wurde in Phase 5 technisch auf `Level.MAX_NORMAL_LEVEL` begrenzt. Damit kann das Combat-Scaling nicht mehr versehentlich auf historische Level 61–99 schreiben.

Idle-Mobs werden nicht dauerhaft skaliert. Die bestehende ereignisbasierte Aktivierung und das 5-Sekunden-Kampf-Timeout bleiben erhalten.

## 4. Mob-XP / Boss-Trennung

Ein forensisch relevanter Doppelbelohnungsfehler wurde behoben:

Boss-Entities besitzen `RPGKeys.Boss.bossId()`. Vor Phase 5 wurden sie trotzdem vom normalen `MobExperienceListener` erfasst.

Damit konnte ein Boss neben seiner Boss-XP zusätzlich normale Mob-XP erzeugen.

Phase 5 trennt deshalb:

- normaler Mob → normale Mob-XP
- Biome-Boss → Boss-XP
- World-Boss → World-Boss-XP

Boss-XP wird weiterhin zentral durch `BossManager` verteilt.

## 5. Loot / Economy-Trennung

Der normale `LootDropListener` erzeugt die vorhandene Gildenwährung für normale Hostile-Mob-Kills.

Vor Phase 5 waren Bosses ebenfalls `Monster` und konnten dadurch zusätzlich den normalen Mob-Geldtoken erhalten.

Das ist jetzt getrennt:

- normaler Hostile Mob → normales Mob-Loot / Gildenwährung
- Biome-Boss → Boss-Belohnung
- World-Boss → World-Boss-Belohnung

Damit werden Boss-Gold und normales Mob-Gold nicht mehr unbeabsichtigt kombiniert.

## 6. Player Combat

Der vorhandene Combat-Kern bleibt erhalten:

- deterministischer Basis-Schaden
- Attack Power
- Crit Chance
- Crit Damage
- Armor-Mitigation
- Lifesteal
- Projektil-Angriffe
- registrierte RPG-Spieler
- Schutz nicht registrierter Spieler vor RPG-Monstern
- Boss-Hit-Cap

Die Schadensberechnung nutzt weiterhin Adventure Components für Feedback und keine Legacy-Chat-API.

## 7. Biome-Bosse

Die 26 vorhandenen Biome-Bosse bleiben eigenständige Weltbegegnungen.

Level:

`1, 4, 6, 9, 11, 14, 16, 19, 21, 24, 26, 29, 31, 34, 36, 39, 41, 44, 46, 49, 51, 54, 56, 58, 59, 60`

Sie verwenden:

- normales Mob-Level-Basismodell
- boss-spezifische HP-/Damage-Multiplikatoren
- Biome-Bindung
- eigene Attack Patterns
- Bossbar
- eigene Boss-XP
- eigene Boss-Goldbelohnung
- eigene Lootdefinition

Biome-Bosse bleiben damit von World-Boss-Events getrennt.

## 8. World-Bosse

Die sechs vorhandenen World-Bosse bleiben getrennt:

| Boss | Level | XP | Gold |
|---|---:|---:|---:|
| rift_colossus | 40 | 6.000 | 2.500 |
| storm_lord | 44 | 7.000 | 3.000 |
| abyss_lord | 48 | 8.000 | 3.400 |
| soul_devourer | 52 | 9.000 | 3.800 |
| end_harbinger | 56 | 10.000 | 4.200 |
| ancient_world_warden | 60 | 12.000 | 5.000 |

Sie behalten:

- Phasen
- Attack Patterns
- Summons
- Enrage-Phasen
- Bossbar
- Damage Contribution
- Event-Loot
- World-Boss-Cleanup

World-Bosse sind damit weiterhin Eventcontent und keine normale Mob-Grindquelle.

## 9. Loot-Integrität

Boss-Custom-Items mit `pixelrpg:` bleiben bewusst aus der normalen Boss-Lootausgabe ausgeschlossen.

Das bestehende System verwendet dafür die vorhandenen Boss-Reward-/Item-Systeme und verhindert, dass historische Custom-Lootdefinitionen unkontrolliert über die normale Material-Lootpipeline ausgegeben werden.

## 10. Nicht künstlich geänderte Werte

Folgende Werte wurden nicht ohne Laufzeitmessung neu erfunden:

- Killzeit
- Kills/Stunde
- reale XP/Stunde
- reale Gold/Stunde
- reale Spawnrate
- reale Spieler-DPS
- reale Boss-Kampfdauer

Diese Werte hängen vom tatsächlichen Serverbetrieb ab und bleiben daher als Messpunkte für das Abschlussaudit bestehen.

## 11. Phase-5-Ergebnis

| Punkt | Status |
|---|---|
| Mob-XP geprüft | ERFÜLLT |
| Mob-HP geprüft | ERFÜLLT |
| Mob-Damage geprüft | ERFÜLLT |
| Player-Parity geprüft | ERFÜLLT |
| Level-Cap 1–60 im Combat | ERFÜLLT |
| Mob-/Boss-XP getrennt | ERFÜLLT |
| Mob-/Boss-Loot getrennt | ERFÜLLT |
| Biome-Bosse separat | ERFÜLLT |
| World-Bosse separat | ERFÜLLT |
| Boss-Phasen erhalten | ERFÜLLT |
| Boss-Loot erhalten | ERFÜLLT |
| Legacy-API eingeführt | NEIN |
| main verändert | NEIN |
| test verändert | JA |

**Phase 5 ist damit technisch abgeschlossen.**
