# PixelRPG Region System – Funktionsübersicht

Dieses Dokument beschreibt den aktuellen Region-Stand auf `test`. Das System bleibt auf der bestehenden Polygon-/Globalregion-Architektur aufgebaut; neue Funktionen werden darin integriert.

## 1. Regionen

### Globale Region

Jede Welt besitzt eine globale Standardregion.

- gilt für die gesamte Welt
- keine Polygongeometrie
- Priorität `0`
- nicht löschbar
- besitzt die globalen Standard-Flags der Welt
- dient als Flag-Fallback für lokale Regionen
- kein Besitzer und keine Mitglieder

### Lokale Polygonregion

Eine lokale Region besitzt:

- Welt
- Polygon
- `minY` / `maxY`
- Name
- Typ
- Beschreibung
- Priorität
- Besitzer
- Mitglieder
- Enter-Titel
- Leave-Titel
- Flags
- freie Properties
- optionale feindliche Spawnpunkte

Die Polygongeometrie wird nicht in Blocklisten aufgelöst.

---

## 2. Erstellung

Die bestehende Admin-Erstellung bleibt erhalten:

```text
/pixelrpg region create <name>
/pixelrpg region finish
/pixelrpg region confirm
/pixelrpg region cancel
```

Die Polygonpunkte werden weiterhin mit dem vorhandenen Editor-Werkzeug gesetzt und validiert. Die Geometrie und die vorhandene Spawnpunkt-Funktion bleiben unverändert.

---

## 3. WorldGuard-artige Besitzstruktur

Die bisherige Gilden-Zuordnung einer Region entfällt vollständig.

Stattdessen besitzt jede lokale Region optional:

```text
owner: <UUID>
members:
  - <UUID>
  - <UUID>
```

Spieler werden dauerhaft über UUID gespeichert.

### Besitzer setzen

Nur Administratoren:

```text
/pixelrpg region owner <region> <spieler|UUID>
/pixelrpg region owner <region> none
```

Beim Setzen eines neuen Besitzers wird dieser automatisch aus der Mitgliederliste entfernt.

### Mitglieder verwalten

Besitzer und Administratoren:

```text
/pixelrpg region member add <region> <spieler|UUID>
/pixelrpg region member remove <region> <spieler|UUID>
```

Der Besitzer kann damit selbst Spieler hinzufügen und entfernen, ohne dass dafür die Gildenlogik verwendet wird.

Mitglieder sind momentan eine eigenständige Zugriffs-/Verwaltungsstruktur. Die vorhandenen Region-Flags gelten weiterhin für die Region als Ganzes; eine separate Flag-Gruppenlogik wird nicht künstlich eingeführt.

---

## 4. Region-Info

Die Info-Funktion ist an der WorldGuard-Idee orientiert:

```text
/pixelrpg region info
/pixelrpg region info <region>
```

Ohne Regionsnamen wird bei einem Spieler die Region an der aktuellen Position angezeigt.

Die Ausgabe enthält unter anderem:

- Name
- UUID
- Welt
- Typ
- Polygonpunkte
- Fläche
- Höhenbereich
- Priorität
- Besitzer
- Mitglieder
- Beschreibung
- alle effektiven Flags

Bei lokalen Regionen wird zusätzlich angezeigt, ob ein Flag vom globalen Weltwert geerbt wird.

---

## 5. Flag-System

Die vorhandene Flag-Architektur bleibt bestehen und wird als zentraler Region-Mechanismus verwendet.

Flags können sowohl auf der **globalen Weltregion** als auch auf **lokalen Polygonregionen** gesetzt werden.

Aktuell vorhandene Flags:

| Flag | Bedeutung |
|---|---|
| `PVP` | PvP erlauben/verbieten |
| `MONSTER_SPAWN` | natürliche feindliche Monster-Spawns erlauben/verbieten |
| `BLOCK_BREAK` | Blockabbau erlauben/verbieten |
| `BLOCK_PLACE` | Blockplatzierung erlauben/verbieten |
| `FIRE_SPREAD` | Feuerausbreitung erlauben/verbieten |
| `LAVA_FLOW` | Lavafluss erlauben/verbieten |
| `EXPLOSION` | Explosionen erlauben/verbieten |
| `CREEPER_EXPLOSION` | Creeper-Explosionen erlauben/verbieten |
| `GHAST_FIREBALL` | Ghast-Feuerbälle erlauben/verbieten |
| `ENDERMAN_GRIEF` | Enderman-Griefing erlauben/verbieten |

Die Flagwerte sind binär:

```text
AN  = true
AUS = false
```

Die vorhandenen Event-/Policy-Adapter verwenden weiterhin `RegionManager.hasFlag(...)`. Dadurch bleibt die Gameplay-Logik zentralisiert.

---

## 6. Flag-Vererbung

Lokale Regionen können ein Flag explizit überschreiben. Wenn ein lokales Flag nicht gesetzt wurde, fällt die Abfrage auf die globale Region der Welt zurück.

```text
Position
  ↓
passende lokale Region?
  ↓ ja
lokales Flag explizit gesetzt?
  ├─ ja → lokalen Wert verwenden
  └─ nein → globalen Weltwert verwenden

keine lokale Region
  ↓
globalen Weltwert verwenden
```

Damit kann die globale Region als Weltstandard verwendet werden, während einzelne Regionen nur die Regeln überschreiben, die sie tatsächlich ändern sollen.

---

## 7. Neues Flag-Menü

Für die Flag-Bearbeitung gibt es ein eigenständiges natives Paper-Dialogmenü:

```text
/pixelrpg region flags
/pixelrpg region flags <region>
```

Ohne Region wird die Region an der aktuellen Spielerposition verwendet.

Das Menü verwendet ausschließlich das aktuelle Paper-26.x-Dialog-System.

Jedes vorhandene Flag erscheint als eigener Button:

```text
✓ PvP – AN
✕ PvP – AUS
```

Ein Klick schaltet den aktuellen Wert sofort um.

Bei einem lokalen Flag, das noch nicht explizit gesetzt wurde, zeigt das Menü den geerbten globalen Wert an:

```text
✓ PvP – AN • geerbt
```

Der erste Klick erzeugt daraus einen expliziten lokalen Wert. Nach jeder Änderung wird das Menü mit dem aktuellen Zustand erneut geöffnet. Die Änderung wird sofort über die bestehende Regionspersistenz gespeichert.

### Wer darf Flags ändern?

- globale Region: Administratoren
- lokale Region: Administratoren oder Besitzer
- normale Mitglieder: keine Flag-Verwaltung

---

## 8. Enter-/Leave-Titel

Die vorhandene Enter-/Leave-Mechanik bleibt bestehen:

```text
/pixelrpg region edit <region> enter <titel>
/pixelrpg region edit <region> leave <titel>
```

Administratoren und der Besitzer einer lokalen Region dürfen diese beiden Titel ändern.

Beim Betreten bzw. Verlassen einer Region werden die vorhandenen Titel weiterhin über den `RegionTransitionService` angezeigt.

---

## 9. Weitere Regionsbearbeitung

Administratoren behalten die vollständige bestehende Bearbeitung:

```text
/pixelrpg region edit <region> name <wert>
/pixelrpg region edit <region> type <wert>
/pixelrpg region edit <region> description <wert>
/pixelrpg region edit <region> priority <wert>
/pixelrpg region edit <region> enter <wert>
/pixelrpg region edit <region> leave <wert>
/pixelrpg region edit <region> flag <FLAG> <true|false>
/pixelrpg region edit <region> property <key> <value>
```

Besitzer dürfen bei lokalen Regionen gezielt:

- Flags ändern
- Enter-Titel ändern
- Leave-Titel ändern
- Mitglieder verwalten

Die Polygongeometrie selbst wird durch die Besitzerlogik nicht verändert.

---

## 10. Räumliche Auflösung

Die bestehende Chunk-basierte Indexierung bleibt erhalten.

Für eine Position werden passende Chunk-Kandidaten gesucht, die Polygongeometrie und der Höhenbereich geprüft und bei mehreren Treffern die Region mit der höchsten Priorität verwendet. Gibt es keinen lokalen Treffer, wird die globale Weltregion verwendet.

Die neue Besitzer-/Flaglogik sitzt damit auf dem vorhandenen RegionManager und ersetzt nicht dessen räumliche Architektur.

---

## 11. Spawnpunkte

Die vorhandene Spawnpunkt-Infrastruktur bleibt unverändert:

- Spawnpunkte für feindliche Mobs können während der Region-Erstellung gesetzt werden.
- Spawnpunkte werden separat indexiert.
- Explizite Spawnpunkte können unabhängig von der normalen Monster-Spawn-Flaglogik behandelt werden.
- Die vorhandene `RegionSpawnService`-Logik bleibt erhalten.

---

## 12. Speicherung

Regionen werden weiterhin in:

```text
plugins/PixelRPG/regions.yml
```

gespeichert.

Das Regionsformat wurde auf Version `5` angehoben.

Lokale Regionen speichern nun unter anderem:

```text
regions.<uuid>.world
regions.<uuid>.name
regions.<uuid>.type
regions.<uuid>.description
regions.<uuid>.min-y
regions.<uuid>.max-y
regions.<uuid>.priority
regions.<uuid>.owner
regions.<uuid>.members
regions.<uuid>.points
regions.<uuid>.flags
regions.<uuid>.properties
regions.<uuid>.spawn-points
regions.<uuid>.enter-message
regions.<uuid>.leave-message
```

Die alten Felder

```text
owner-guild-id
owner-guild-name
```

werden nicht mehr verwendet.

Die globalen Flags bleiben getrennt unter:

```text
global-regions.<world>.flags
```

Die bestehende atomare und sequenzielle asynchrone Persistenz bleibt erhalten. Ältere Regionsdateien werden beim Laden auf das neue Format migriert.

---

## 13. Befehlsübersicht

### Allgemein

```text
/pixelrpg region info
/pixelrpg region info <region>
/pixelrpg region flags
/pixelrpg region flags <region>
```

### Besitzer / Mitglieder

```text
/pixelrpg region owner <region> <spieler|UUID>
/pixelrpg region owner <region> none
/pixelrpg region member add <region> <spieler|UUID>
/pixelrpg region member remove <region> <spieler|UUID>
```

### Bestehende Administration

```text
/pixelrpg region create <name>
/pixelrpg region finish
/pixelrpg region confirm
/pixelrpg region cancel
/pixelrpg region delete <region>
/pixelrpg region list
/pixelrpg region edit <region> <feld> <wert>
```

Es wurde kein zusätzliches paralleles Region-System eingeführt. Die neuen Funktionen hängen direkt am vorhandenen `PixelRegion`-/`RegionManager`-/`RegionRepository`-System.

---

## 14. Architekturprinzip

```text
PixelRegion
 ├─ Owner
 ├─ Members
 ├─ Flags
 ├─ Enter/Leave
 └─ bestehende Geometrie

RegionManager
 ├─ globale Region
 ├─ lokale Regionen
 ├─ Flag-Fallback
 ├─ Owner/Member-Mutationen
 └─ bestehende Persistenzkette

RegionSubCommand
 ├─ Info
 ├─ Owner
 ├─ Member
 ├─ Flags
 └─ bestehende Admin-Befehle

RegionFlagDialogService
 └─ natives Paper-Dialog-Menü für Flag-Toggles

RegionPolicyService
 └─ bestehende Gameplay-Entscheidungen über RegionManager.hasFlag(...)
```

Damit bleibt das bestehende Region-System die zentrale Grundlage. Es gibt keine zweite Region-Engine und keine parallele Guild-Ownership-Logik mehr.
