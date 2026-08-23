# PixelRPG – Quest & Companion Roadmap

Stand: 2026-08-23

> Arbeits-/Designreferenz für Quest- und Companion-Systeme. Der technische Gesamtstatus und die aktuelle Aufgabenpriorität stehen in `audit.md`.

## Aktueller Stand

Das Quest-System und das Companion-Grundsystem sind bereits implementiert. Diese Datei beschreibt weiterhin die verbindlichen Designregeln und die geplante inhaltliche Richtung, ist aber **keine aktuelle Bug-/Aufgabenliste**.

### Verbindliche Designregeln

- Open-World statt künstlicher Level-Gebiete.
- Keine Reputation.
- Keine Titel.
- Maximal 5 aktive Quests als aktueller Designwert.
- Questketten statt sinnloser Filler-Quests.
- NPCs und echte Orte stehen im Mittelpunkt.
- Recovery Compass dient als Richtungshelfer, nicht als vollständiges GPS.
- Quests und veränderliche Inhalte sollen datengetrieben bleiben.
- Companion unterstützt den Spieler und ersetzt ihn nicht.
- Companion-Combat wird erst nach Stabilisierung des Grundsystems weiter aktiviert.

## Spielerprogression

```text
Level 1 → 99
Level 100 → Endgame/Grind
```

Questprogression orientiert sich an Levelbereichen, ohne Levelgebiete als Zugangssperre zu verwenden.

## Questtypen

Aktive/grundsätzlich vorgesehene Typen:

- `HUNT`
- `COLLECT`
- `TALK_TO_NPC`
- `ESCORT`
- `REACH_LOCATION`
- `GLOBAL_EVENT`

Keine neuen Questtypen als reine Enum-Platzhalter. Jeder neue Typ braucht vollständige Progressionslogik.

## Quest-Navigation

Questziele sollen verständlich beschrieben werden. Koordinaten dürfen intern gespeichert werden, ersetzen aber nicht die Welt-/NPC-Beschreibung.

Recovery Compass bleibt als richtungsbasierte Hilfe vorgesehen.

## Datengetriebene Quests

Primäre Datenquelle im Repository:

`src/main/resources/data/quests.json`

Veränderliche Questdaten sollen nicht unnötig im Java-Code verteilt werden.

## Companion-Grundsatz

Companions besitzen eigene Progression und sind Runtime-Entities.

**Wichtig:** Logout/Login bedeutet nicht automatischen Companion-Restore. Der Spieler ruft seinen Companion nach dem Login erneut.

## Beispiele / Inhaltsplanung

Die bisherigen Questbeispiele und Questketten bleiben als Designmaterial erhalten. Sie werden nicht als erledigte Implementierung betrachtet, solange die tatsächlichen Daten und Runtime-Abläufe nicht getestet wurden.

## Status

**Design:** weitgehend definiert.

**Runtime:** Grundsystem vorhanden.

**Aktuelle Aufgabe:** Validierung der tatsächlichen Quest-/Companion-Runtime gegen die definierten Regeln.

Konkrete offene Arbeiten werden ausschließlich in `audit.md` geführt.
