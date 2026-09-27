# PixelRPG – RP Phase 6 Quest / Story

**Status:** Phase 6 technisch abgeschlossen  
**Branch:** `test`  
**Referenzbranch:** `main` ausschließlich als Soll-/Vergleichszustand  
**Prüfstand:** 27.09.2026

## 1. Ziel

Phase 6 behandelt gemäß `RP.md` Abschnitt 69:

- Quest-XP
- Levelanforderungen
- Story-XP
- Rewards
- Follow-ups
- Weltanbindung

Die vorhandene Quest-/Story-Architektur wurde weiterverwendet.

## 2. Forensischer Ausgangsbefund

Die Questdaten enthalten insgesamt **149 eindeutige Questdefinitionen**:

| Quelle | Definitionen |
|---|---:|
| quests_v2.json | 36 |
| quests_content_expansion_01.json | 46 |
| quests_expansion_02.json | 19 |
| quests_story.json | 19 |
| quests_world_expansion.json | 29 |
| **Gesamt** | **149** |

Vor Phase 6 lagen **52 Definitionen** über dem verbindlichen Charakter-Maximum von Level 60.

Die bestehende `QuestRepository`-Validierung verwirft normale Quests außerhalb `Level.MIN_LEVEL..Level.MAX_NORMAL_LEVEL`. Dadurch waren effektiv nur 97 der 149 Definitionen als normale Runtime-Quests ladbar.

Es wurden keine Questdefinitionen gelöscht.

## 3. Levelanforderungen

Alle 149 Questdefinitionen liegen jetzt bei:

- `recommendedLevel`: 1–60
- `categoryLevel`: 1–60
- `rules.normalLevelMax`: 60

Die 52 historischen Level-61–99-Werte wurden auf das bestehende Endgame-Limit 60 begrenzt.

Damit bleibt der vorhandene Content erhalten und wird nicht durch einen nicht mehr existierenden Levelbereich unzugänglich.

Die Repository-Validierung bleibt zusätzlich als technische Schutzschicht erhalten.

## 4. Quest-XP

Die bestehenden Quest-XP-Werte wurden nicht künstlich neu skaliert.

Die vorhandenen Rewards bleiben Bestandteil der Contentbasis. Die Charakter-XP wird weiterhin beim Questabschluss über den bestehenden `PlayerProfileManager` vergeben.

Damit wurde in Phase 6 bewusst nur die nicht mehr gültige Level-Gating-Lücke behoben, nicht die XP-Kurve ohne Laufzeitmessung neu erfunden.

## 5. Rewards

Die vorhandene Reward-Pipeline bleibt erhalten:

- Geld
- Charakter-XP
- Vanilla-Materialien
- PixelRPG-Items
- Begleiter

Die bestehende Item-Validierung und Item-Erzeugung in `QuestManager` bleiben unverändert.

## 6. Follow-ups / Voraussetzungen

Die Questdaten besitzen weiterhin eine zentrale Prerequisite-/Follow-up-Struktur.

Die beiden expliziten Follow-up-Ketten der aktuellen Datenbasis bleiben erhalten:

- `intro_first_delivery` → `intro_zombie_watch`
- `intro_zombie_watch` → `bee_nest`

Zusätzlich werden vorhandene `prerequisites` und `requirements.previousQuest` weiterhin durch `QuestRepository` zusammengeführt und auf unbekannte Referenzen sowie Zyklen geprüft.

Der forensische Referenzcheck ergab keine fehlenden Quest-Referenzen und keine doppelten Quest-IDs.

## 7. Story

Die Story besteht weiterhin aus **20 Kampagnenkapiteln** inklusive dem initialen Lore-Kapitel.

Die Story verwendet:

- persistierten Story-Fortschritt im `PlayerProfile`
- lineare Kapitelreihenfolge
- Level-Gating
- Story-Quest-Verknüpfung
- NPC-Verknüpfung
- Struktur-/Weltbezug
- Kapitel-XP

Die 19 eigentlichen Story-Quests bleiben erhalten.

### Story-Level

Historische Story-Anforderungen über Level 60 wurden ebenfalls auf Level 60 begrenzt.

Betroffen waren:

- `under_the_stone`
- `campaign_fortress`
- `campaign_bastion_remnant`
- `campaign_stronghold`
- `campaign_dragon`
- `campaign_end_city`
- `after_the_end`

Damit endet die numerische Story-Anforderung ebenfalls bei Level 60.

## 8. Story-XP

Kapitel-XP und Quest-XP bleiben getrennte bestehende Belohnungskomponenten.

Die vorhandene Reihenfolge bleibt:

1. Quest wird abgeschlossen.
2. Quest-Belohnungen werden vergeben.
3. Story-Kapitel wird abgeschlossen.
4. Kapitel-XP wird vergeben.

Es wurde keine neue XP-Quelle erfunden.

## 9. Weltanbindung

Die bestehende Story-Kampagne nutzt reale Minecraft-Strukturen als Anker, unter anderem:

- Villages
- Shipwrecks
- Desert/Jungle/Swamp-Strukturen
- Ocean Ruins
- Monument
- Trail Ruins
- Mineshaft
- Pillager Outpost
- Mansion
- Ruined Portal
- Ancient City
- Nether Fortress
- Bastion Remnant
- Stronghold
- End City

Die vorhandene `QuestCoordinateResolver`-/Navigation-Pipeline bleibt dafür zuständig.

Die Story wird dadurch nicht zu einer isolierten Menü-Kampagne, sondern bleibt an die Welt gebunden.

## 10. Runtime-Sicherheit

Die bestehende Trennung bleibt erhalten:

- normale Quests → normaler Quest-Giver-Pfad
- Story-Quests → zentrale Reception-/Story-Rezeption
- Story-Fortschritt → persistiert im Spielerprofil
- Quest-Ziele → persistierte Navigationsziele
- Questabschluss → bestehendes `QuestCompletedEvent`

Die bestehende Validierung gegen ungültige Questtypen, Items, Entitytypen, Navigation und Levelbereiche bleibt erhalten.

## 11. Technische Änderung

Zusätzlich zur Datenbereinigung wurde `StoryManager` auf das zentrale Levelmodell umgestellt:

- keine eigene historische 1–99-Grenze mehr
- Story-Level werden mit `Level.MIN_LEVEL` / `Level.MAX_NORMAL_LEVEL` begrenzt
- Story-Migrationsversion auf 8 erhöht
- vorhandene v7-Bundled-Kampagne wird bei Bedarf auf die aktuelle Kampagne migriert

Damit bleibt auch eine bereits vorhandene `story.yml` aus dem vorherigen Bundled-Stand kompatibel.

## 12. Phase-6-Ergebnis

| Punkt | Status |
|---|---|
| 149 Questdefinitionen erhalten | ERFÜLLT |
| Alle Quest-IDs eindeutig | ERFÜLLT |
| Quest-Level 1–60 | ERFÜLLT |
| Quest-XP-Pipeline geprüft | ERFÜLLT |
| Rewards erhalten | ERFÜLLT |
| Follow-ups geprüft | ERFÜLLT |
| Prerequisite-Referenzen geprüft | ERFÜLLT |
| Story-Kapitel erhalten | ERFÜLLT |
| Story-Level 1–60 | ERFÜLLT |
| Story-XP erhalten | ERFÜLLT |
| Welt-/Strukturanbindung erhalten | ERFÜLLT |
| Runtime-Validierung erhalten | ERFÜLLT |
| Legacy-API eingeführt | NEIN |
| main verändert | NEIN |
| test verändert | JA |

**Phase 6 ist technisch abgeschlossen.**
