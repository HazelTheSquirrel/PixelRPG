# PixelRPG – RP Phase 1 – Charakterprogression

**Status:** Phase 1 abgeschlossen  
**Branch:** `test`  
**Referenz:** `main` ausschließlich als Soll-/Vergleichszustand  
**Prüfstand:** 25.09.2026

---

## 1. Ziel der Phase

Phase 1 aus `RP.md`:

- Level 1–60 finalisieren
- XP-Kurve validieren
- Meilensteine definieren
- XP-Quellen abstimmen
- Levelzeit bestimmen

Es wurden keine neuen Balancewerte erfunden. Die bestehende XP-Kurve wurde als verbindliche Baseline mathematisch validiert und die Laufzeitmessung für reale Levelzeiten instrumentiert.

---

## 2. Verbindliche Charaktergrenzen

| Eigenschaft | Wert |
|---|---:|
| Mindestlevel | 1 |
| normales Maximallevel | 60 |
| reserviertes Level | 61 |
| kumulative XP für Level 60 | 3.379.400 |
| XP oberhalb Level 60 | nicht mehr progressionswirksam |

**Wichtig:** Spielerprofile werden jetzt bei der Charakter-XP auf die Level-60-Schwelle begrenzt. Dadurch kann die numerische Charakterprogression nicht unbemerkt über Level 60 hinaus anwachsen.

---

## 3. Validierte XP-Kurve

Die bestehende Tabelle in `core/Level.java` bleibt die verbindliche Kurve.

| Level | kumulative XP |
|---:|---:|
| 1 | 0 |
| 10 | 27.600 |
| 20 | 159.100 |
| 30 | 440.800 |
| 40 | 977.700 |
| 50 | 1.921.500 |
| 60 | 3.379.400 |

XP-Bedarf zwischen den Haupttiers:

| Abschnitt | zusätzliche XP |
|---|---:|
| 1 → 10 | 27.600 |
| 10 → 20 | 131.500 |
| 20 → 30 | 281.700 |
| 30 → 40 | 536.900 |
| 40 → 50 | 943.800 |
| 50 → 60 | 1.457.900 |

Damit ist die Kurve nicht linear und erzeugt bewusst größere langfristige Levelabschnitte.

---

## 4. Charakter-Meilensteine

Die sechs Haupttiers aus `RP.md` werden als Charakter-Meilensteine verwendet:

| Meilenstein | Charakterlevel | Thema |
|---|---:|---|
| I | 1–10 | Starter |
| II | 11–20 | Kupfer |
| III | 21–30 | Eisen |
| IV | 31–40 | Diamant |
| V | 41–50 | Netherite |
| VI | 51–60 | Custom-Endgame |

Diese Tiers sind keine Regionen- oder Quest-Gates.

---

## 5. XP-Quellen

Aktuell dokumentierte Charakter-XP-Quellen:

1. normale Monster
2. Quests
3. Storykapitel
4. Biome-Bosse
5. World-Bosse

Berufsaktivitäten bleiben primär Berufs-XP und werden nicht automatisch zu Charakter-XP.

### Monster

Die aktuelle Baseline bleibt:

`XP = MaxHealth × xpPerMaxHealth`

Aktueller Multiplikator:

`xpPerMaxHealth = 4,0`

Beispiele:

| Mob-Level | ungefähr XP |
|---:|---:|
| 1 | 90 |
| 10 | 180 |
| 20 | 280 |
| 30 | 380 |
| 40 | 480 |
| 50 | 580 |
| 60 | 680 |

Party-XP erhält aktuell den dokumentierten +20-%-Gesamtbonus vor der Verteilung.

### Quests

Die vorhandenen Quest-XP bleiben Bestandteil der Charakterprogression.

Die Questdatenbasis wird nicht in Phase 1 pauschal neu skaliert. Die spätere Questphase bewertet Dauer, Reise, Kampf, Dialoganteil und Reward einzeln.

### Story

Story-XP bleibt ein eigener Contentpfad. Story soll Charakterentwicklung unterstützen, aber nicht die einzige Route zum Level 60 darstellen.

### Bosse

Biome-Bosse und World-Bosse bleiben getrennte XP-Quellen.

World-Bosse bleiben Eventcontent und dürfen keine notwendige Level-60-Progressionsquelle werden.

---

## 6. Reale Levelzeit – Messung

Eine statische Repositoryanalyse kann keine ehrliche XP/Stunde oder Levelzeit erzeugen. Deshalb wurde die Charakterprogression um persistente Laufzeit-Telemetrie ergänzt.

Neue Klasse:

`src/main/java/de/pixelrpg/rpg/progression/CharacterProgressionTelemetry.java`

Sie misst pro Charakterlevel die tatsächlich verbrachte aktive Sessionzeit und schreibt sie in die bestehenden Player-Statistiken:

`rp.character.level.<level>.time-millis`

Die Messung:

- startet beim registrierten Spielerbeitritt,
- bleibt über Sessions hinweg kumulierbar,
- wird beim Levelwechsel abgeschlossen,
- wird beim Quit abgeschlossen,
- wird beim Plugin-Shutdown abgeschlossen,
- verändert keine XP-Werte,
- erzeugt keinen zusätzlichen Progressionsvorteil.

Damit kann später aus realen Spielsessions bestimmt werden:

- Zeit bis Level 10
- Zeit bis Level 20
- Zeit bis Level 30
- Zeit bis Level 40
- Zeit bis Level 50
- Zeit bis Level 60

und zusätzlich die Zeit pro einzelnem Level.

---

## 7. Warum keine künstliche Levelzeit eingetragen wurde

Eine Zahl wie „Level 20 dauert 8 Stunden“ wäre ohne reale Messdaten erfunden.

Die Phase fordert ausdrücklich:

`XP benötigt / reale XP pro Stunde = reale Levelzeit`

Daher wird keine theoretische XP/Stunde als reale Messung ausgegeben.

Die Infrastruktur ist jetzt vorhanden; die tatsächlichen Werte entstehen aus echten Spieler-Sessions.

---

## 8. XP-Cap

Die Charakter-XP wird jetzt auf:

`3.379.400 XP`

begrenzt.

Damit gilt:

- Level 60 bleibt das numerische Charakterende.
- Quests/Bosse/Mobs können weiterhin technisch XP-Rewards besitzen.
- Nach Erreichen von Level 60 entsteht daraus kein weiterer numerischer Charakterfortschritt.
- Es gibt keine versteckte Level-61-Progression.

---

## 9. Phase-1-Änderungen

Geändert auf `test`:

- `PlayerProfile`: Charakter-XP wird hart auf die Level-60-Schwelle begrenzt.
- `CharacterProgressionTelemetry`: reale Levelzeitmessung eingeführt.
- `PixelRPGPlugin`: Telemetrie registriert und sauber beim Shutdown beendet.

Nicht geändert:

- bestehende XP-Tabelle
- Mob-XP-Multiplikator
- Quest-XP-Werte
- Story-XP-Werte
- Boss-XP-Werte
- XP-Partybonus
- Profession-XP
- Itemwerte
- Economywerte

---

## 10. Definition of Done – Phase 1

| Punkt | Status |
|---|---|
| Level 1–60 konsistent | ERFÜLLT |
| Level 60 als numerisches Ende | ERFÜLLT |
| XP-Kurve mathematisch validiert | ERFÜLLT |
| XP-Cap implementiert | ERFÜLLT |
| Hauptmeilensteine definiert | ERFÜLLT |
| alle aktuellen Charakter-XP-Quellen dokumentiert | ERFÜLLT |
| reale Levelzeit messbar | ERFÜLLT |
| XP-Kurve ohne unbelegte Balanceänderung | ERFÜLLT |
| main verändert | NEIN |
| test verändert | JA |

### Ergebnis

**Phase 1 ist technisch abgeschlossen.**

Die verbleibenden realen Levelzeitwerte sind keine Implementierungslücke: Sie benötigen echte Spielzeitdaten. Die Messung dafür ist jetzt persistent vorbereitet und kann in der späteren Balanceauswertung verwendet werden.
