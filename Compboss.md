# PixelRPG – Companions, Bosses & Mounts

Stand: 2026-08-23

> Arbeits-/Designreferenz für Companions, Bosse und Reittiere. Der technische Gesamtstatus steht in `audit.md`.

## Aktueller Stand

Das Companion-Grundsystem und das Boss-System sind bereits im Code vorhanden. Diese Datei ist deshalb **keine reine Zukunftsplanung mehr**.

Aktuell gilt:

- Companion-Besitz und Grundverwaltung vorhanden.
- Companion-Equipment vorhanden.
- Mannequin-Companions vorhanden.
- Companion-Follow vorhanden.
- Companion-Combat bleibt bis zur Stabilisierung des Basissystems zurückgestellt.
- Boss-Grundsystem, Phasen, Damage Contribution und Loot-Strukturen vorhanden.
- Mount-System ist noch kein abgeschlossener Gameplay-Bereich.

## Verbindliche Companion-Regel

Companions sind Runtime-Entities und keine persistenten Welt-NPCs.

```text
Spieler besitzt Companion
        ↓
Spieler ruft ihn
        ↓
Spawn
        ↓
Follow / vorhandene Runtime-Funktionen
        ↓
Logout
        ↓
Companion verschwindet
        ↓
Login
        ↓
kein automatischer Restore
        ↓
Spieler ruft ihn erneut
```

Der erneute Ruf nach Login muss zuverlässig funktionieren.

## Companion-Raritäten

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Unique-Companions bleiben Admin-vergeben und können feste Namen/Skins besitzen.

## Companion-Design

Für jeden Companion müssen langfristig definiert sein:

- Entity
- Seltenheit
- Basiswerte
- Level 1–99
- eigene XP
- XP-Kurve
- Skalierung
- passive Fähigkeiten
- Cooldowns
- Reichweite
- Verhalten
- Follow-Distanz
- Größe/Scale
- Spawn-/Despawn-Verhalten
- Freischaltung
- Quest-/Bossquelle
- Datenkonfiguration
- Teststatus

Aktive Companion-Fähigkeiten werden erst nach Stabilisierung des passiven Basissystems betrachtet.

## Reittiere

Erste Designziele:

1. Pig – Boden
2. Bee – Flug
3. Skeleton Horse – Boden
4. Strider – Nether/Lava
5. Goat – Charge
6. Frog – Sprung
7. Turtle – langsam/defensiv

Das Mount-System ist noch nicht als fertig zu betrachten.

## Boss-System

Das Boss-System besitzt bereits Definitionen, aktive Bosse, Phasen, Loot, Contribution, Angriffsmuster und Spawnlogik.

Für jeden Boss müssen später geprüft werden:

- Entity/Variante
- Dimension
- Tier
- Spawnbedingungen
- Größe/Scale
- Werte
- Bewegung
- Angriffe
- Phasen
- HP-Schwellen
- Cooldowns
- Reichweiten
- Status-Effekte
- `block_break`
- Umgebungseinfluss
- Loot
- Companion-Belohnung
- Mount-Belohnung
- Teststatus

Vanilla-Sonderbosse wie der Ender Dragon bleiben zunächst vom normalen Boss-Pool getrennt.

## Grundpool

Die vorhandenen Boss-/Companion-Kandidaten und Reittierideen bleiben als Designpool bestehen. Einzelne Werte und Fähigkeiten werden erst bei der tatsächlichen Implementierung bzw. Balancevalidierung verbindlich.

## Nächste Arbeiten

Die Reihenfolge wird nicht mehr hier verwaltet. Offene Aufgaben stehen in `audit.md`.

Insbesondere:

1. Companion-Ruf nach Login testen.
2. Spawn/Skin/Equipment/Follow/Despawn testen.
3. Companion-Daten gegen Runtime-Verwendung prüfen.
4. Boss-Lifecycle end-to-end testen.
5. Mount-System erst nach Bedarf weiter ausbauen.
