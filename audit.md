# PixelRPG – System-Audit / Cleanup-Stand

Stand: 2026-08-22

Dieser Audit betrachtet den aktuellen `main`-Stand nach dem Cleanup-Durchlauf. Es wurden keine neuen Gameplay-Systeme eingeführt.

## Erledigt

### Entfernte Alt-Systeme

- Blessings vollständig entfernt.
- Curses vollständig entfernt.
- Blessing-/Curse-Typen und PDC-Felder entfernt.
- Blessing-/Curse-Loot-Chancen aus dem Item-System entfernt.
- Mana vollständig aus dem Gameplay-Code entfernt.
- Mana aus `PlayerProfile` entfernt.
- Mana aus `StatEngine` entfernt.
- Mana aus Weapon-Abilities entfernt.
- Mana-PDC-Key entfernt.
- Mana aus YAML-Persistenz entfernt.
- Mana aus MySQL-Persistenz und dem neuen Schema entfernt.
- Mana-Regenerations-Task entfernt.
- Mana-Lifecycle aus dem Plugin entfernt.
- Alte Rank-/Gem-/Rune-/Socket-Pfade sind im aktuellen Repository-Suchstand nicht mehr vorhanden.

### Weapon-Abilities

- Aktivierung ist jetzt tatsächlich **Rechtsklick**.
- Shift ist nicht mehr Voraussetzung.
- Item-Lore und Listener entsprechen demselben Design.
- Cooldown bleibt waffenbezogen.
- Keine Mana-Kosten.

### Balance-Datenquellen

`attributes.json` ist die aktive Attribut-Baseline. Die Java-Fallbackwerte wurden auf dieselben Werte synchronisiert.

`class-balance.json` ist die aktive Klassen-Baseline. Die Java-Fallbackformeln wurden auf denselben Stand synchronisiert.

`item-scaling.json` ist die aktive Item-Wachstumskurve:

- Growth multiplier: `10.0`
- Weapon damage base: `3.0`
- Weapon crit base: `0.5`
- Armor base: `0.7`
- Health base: `1.2`
- Tool efficiency base: `1.0`

Die Item-Rarity-Multiplikatoren sind:

- Common `1.00`
- Uncommon `1.10`
- Rare `1.22`
- Epic `1.38`
- Legendary `1.60`
- Unique `1.60`

## Architektur-Befund

PixelRPG ist inzwischen kein reiner Feature-Prototyp mehr. Vorhanden sind unter anderem:

- Player/Profile/Persistenz
- Guild-Registrierung
- Level/XP
- Attribute
- Klassen
- Items/Rarity/Item-Level
- Combat
- Mob-Level-Scaling
- Loot
- Weapon-Abilities
- NPCs
- native Dialoge
- Quests
- Berufe/Crafting
- Economy
- Party
- Story
- Travel
- Companions
- Bosse/World-Bosse
- Statistics/Scoreboard
- API/Event-Schicht

Die nächste Phase ist deshalb weiterhin **Validierung und Polish**, nicht Feature-Ausbau.

## Vanilla-/PixelRPG-Koexistenz

Die Registrierung eines Spielers ist die zentrale Grenze für RPG-Systeme. Viele relevante Systeme prüfen bereits den registrierten PixelRPG-Zustand.

Der wichtigste noch zu testende Mischfall bleibt Mob Scaling:

```text
Vanilla-Spieler
      ↓
selbe Welt / selbe Entity
      ↑
PixelRPG-Spieler
```

Das Scaling verändert eine Entity für einen RPG-Kampf. Das ist konzeptionell zulässig, muss aber im echten Serverbetrieb geprüft werden, damit Vanilla-Spieler nicht unerwartet in RPG-Mechaniken hineingezogen werden.

## Native Dialoge

Native Minecraft-/Paper-Dialoge sind inzwischen ein echter Bestandteil der NPC-Architektur. NPC-Verhalten wie Empfang, Schmied, Quest, Shop, Travel, Story und Bank greifen darauf zurück.

Inventory-GUIs existieren weiterhin und dürfen nicht pauschal gelöscht werden. Sie müssen einzeln als Runtime, Admin-Tool, Inventaroperation oder Legacy klassifiziert werden.

## Persistenz

YAML und MySQL bilden weiterhin zwei Repository-Implementierungen für PlayerProfile. Die bestehende Async-Save-/Load-Struktur bleibt erhalten.

Alte MySQL-Datenbanken können noch historische Mana-Spalten besitzen. Der aktuelle Code liest und schreibt diese Felder nicht mehr. Das automatische Löschen alter Produktionsspalten ist bewusst nicht Teil des Cleanup-Schrittes, um bestehende Datenbanken nicht destruktiv zu verändern.

## Dokumentation

`balancing.md` enthält weiterhin historische Analyse und muss noch als aktuelle Master-Baseline bereinigt werden. Die tatsächlichen aktuellen Werte stehen inzwischen konsistent in den JSON-Daten und den synchronisierten Java-Fallbacks.

Die Roadmap bleibt die funktionale Referenz. Es werden in dieser Phase keine neuen Roadmap-Features hinzugefügt.

## Technischer Plattformstand

Der Repository-Stand muss weiterhin gegen die verbindliche Zielplattform geprüft werden:

- Java 25
- Paperweight `2.0.0-beta.21`
- Paper 26.x
- Zielplattform: Paper 26.2
- Mojang-Mappings
- `paper-plugin.yml`

Ein tatsächlicher Build gegen Paper 26.2 muss noch separat validiert werden.

## Noch offen – ausschließlich Cleanup / Validation

### P0

1. Build gegen Paper 26.2 erfolgreich durchführen.
2. Vanilla-/PixelRPG-Mischtests für Mob Scaling durchführen.
3. Persistenz nach Entfernen von Mana mit YAML und bestehender MySQL-Datenbank testen.

### P1

4. `balancing.md` auf den tatsächlichen aktuellen Stand synchronisieren.
5. Inventory-GUIs einzeln klassifizieren.
6. PDC-Keys auf weitere tote/historische Felder prüfen.
7. Quest-/Companion-Daten gegen die tatsächliche Runtime-Verwendung prüfen.
8. Datenquellen weiterhin auf genau eine aktuelle Wahrheit pro System reduzieren.

### P2

9. End-to-End-Test: Registrierung → NPC → Dialog → Quest → Combat → XP → Loot/Reward.
10. Persistenztest über Server-Neustart.
11. Level-/Scaling-Testmatrix für die relevanten Levelstufen.
12. Abschließender Voll-Audit nach den Tests.

## Grundsatz für die weitere Arbeit

Keine neuen Features, solange diese Cleanup- und Validierungsphase nicht abgeschlossen ist.

PixelRPG soll Minecraft erweitern, nicht Minecraft ersetzen. Jede Bereinigung muss deshalb die Vanilla-/PixelRPG-Koexistenz und die native Minecraft-/Paper-Mechanik als oberste Leitlinie behalten.
