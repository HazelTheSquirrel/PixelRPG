# PixelRPG Region System – Funktionsübersicht / Audit

Dieses Dokument ist eine **unabhängige Bestandsaufnahme des aktuell vorhandenen Region-Systems** auf dem Branch `test`.

Es beschreibt ausschließlich, was der vorhandene Code bereits kann. Es fügt keine neuen Befehle, Menüs oder Funktionen hinzu.

---

## 1. Region-Typen

### Globale Region

Jede Welt besitzt eine globale Standardregion.

Eigenschaften:

- gilt für die gesamte Welt
- besitzt keine Polygon-Geometrie
- hat immer Priorität `0`
- kann nicht gelöscht werden
- kann über ihre Flags angepasst werden
- dient als Fallback, wenn an einer Position keine lokale Region ein passendes Flag definiert

Die globale Region wird pro Welt geführt.

### Lokale Polygonregion

Lokale Regionen bestehen aus:

- Welt
- Polygon aus Punkten
- minimaler Höhe (`minY`)
- maximaler Höhe (`maxY`)
- Name
- Region-Typ
- Beschreibung
- Priorität
- optionaler Gilde als Besitzer
- Eintrittsnachricht
- Austrittsnachricht
- Flags
- frei definierbaren Properties
- optionalen feindlichen Mob-Spawnpunkten

Die Polygon-Geometrie wird nicht in einzelne Blöcke aufgelöst.

---

## 2. Regionen erstellen

Der vorhandene Befehl ist:

```text
/pixelrpg region create <name>
```

Danach startet eine temporäre Editor-Session.

Der Region-Editor gibt dem Spieler ein spezielles Stick-Werkzeug.

### Punkte setzen

Mit Rechtsklick auf Blöcke werden Polygonpunkte gesetzt:

```text
P1 → P2 → P3 → P4 → ...
```

Die Punkte müssen in derselben Welt liegen.

Doppelte Punkte werden abgelehnt.

Während der Bearbeitung wird das Polygon für den Spieler mit Partikeln visualisiert.

---

## 3. Polygon prüfen und speichern

### Prüfung

```text
/pixelrpg region finish
```

Dabei wird geprüft, ob aus den gesetzten Punkten eine gültige Region-Geometrie erzeugt werden kann.

Bei einem gültigen Polygon werden unter anderem angezeigt:

- Anzahl der Punkte
- berechnete Fläche

### Speichern

```text
/pixelrpg region confirm
```

Eine Region kann erst nach erfolgreichem `finish` bestätigt werden.

Beim Erstellen werden automatisch die Höhenwerte der Welt verwendet:

- `minY` = minimale Welthöhe
- `maxY` = maximale Welthöhe minus 1

### Abbrechen

```text
/pixelrpg region cancel
```

Die temporäre Session wird verworfen und es wird keine Region verändert.

---

## 4. Region bearbeiten

Das bestehende Region-System besitzt bereits einen Edit-Befehl:

```text
/pixelrpg region edit <region> <feld> <wert>
```

Eine Region kann über ihren Namen oder ihre UUID ausgewählt werden.

Der aktuelle Selector für die globale Region ist:

```text
__global__
```

Alternativ wird auch `global` akzeptiert.

---

## 5. Änderbare Felder lokaler Regionen

Bei einer normalen Polygonregion sind aktuell folgende Felder editierbar:

```text
name
 type
description
priority
enter
leave
flag
guild
unguild
property
```

### Name

```text
/pixelrpg region edit <region> name <neuer name>
```

Ändert den sichtbaren Regionsnamen.

### Typ

```text
/pixelrpg region edit <region> type <typ>
```

Der Wert wird über `RegionType.parse(...)` ausgewertet.

### Beschreibung

```text
/pixelrpg region edit <region> description <beschreibung>
```

### Priorität

```text
/pixelrpg region edit <region> priority <zahl>
```

Die Priorität entscheidet bei überlappenden lokalen Regionen darüber, welche Region für eine Position ausgewählt wird.

### Eintrittsnachricht

```text
/pixelrpg region edit <region> enter <nachricht>
```

### Austrittsnachricht

```text
/pixelrpg region edit <region> leave <nachricht>
```

### Gilde zuweisen

```text
/pixelrpg region edit <region> guild <gildenname>
```

Die Gilde wird über den vorhandenen `GuildManager` aufgelöst.

### Gilde entfernen

```text
/pixelrpg region edit <region> unguild
```

### Freie Property setzen

```text
/pixelrpg region edit <region> property <key> <value>
```

Properties sind zusätzliche frei definierbare Schlüssel/Werte der Region.

### Flag setzen

```text
/pixelrpg region edit <region> flag <FLAG> <true|false>
```

Beispiel:

```text
/pixelrpg region edit Spawn flag PVP false
```

---

## 6. Globale Region bearbeiten

Die globale Region wird nicht wie eine Polygonregion verändert.

Sie kann ausschließlich über ihre Flags geändert werden:

```text
/pixelrpg region edit __global__ flag <FLAG> <true|false>
```

Beispiel:

```text
/pixelrpg region edit __global__ flag PVP false
```

Die globale Einstellung gilt für die Welt, in der der Befehl ausgeführt wird.

Andere Eigenschaften der globalen Region wie Name, Polygon, Priorität oder Gildenbesitz sind nicht editierbar.

---

## 7. Aktuelle Region-Flags

Das System kennt aktuell diese Flags:

| Flag | Bedeutung |
|---|---|
| `PVP` | Spieler gegen Spieler |
| `MONSTER_SPAWN` | Feindliche Monster-Spawns |
| `BLOCK_BREAK` | Blöcke abbauen |
| `BLOCK_PLACE` | Blöcke platzieren |
| `FIRE_SPREAD` | Feuer-Ausbreitung |
| `LAVA_FLOW` | Lavafluss |
| `EXPLOSION` | Explosionen allgemein |
| `CREEPER_EXPLOSION` | Creeper-Explosionen |
| `GHAST_FIREBALL` | Ghast-Feuerbälle |
| `ENDERMAN_GRIEF` | Enderman-Griefing |

Flags sind boolesch:

```text
true  = erlaubt
false = verboten
```

Nicht explizit gesetzte Flags einer normalen Region gelten im `PixelRegion`-Modell standardmäßig als `true`.

---

## 8. Flag-Vererbung / Fallback

Das Region-System arbeitet mit einer lokalen Region und der globalen Weltregion.

Bei der Flag-Abfrage wird zuerst die passendste lokale Region betrachtet.

Wenn die lokale Region das Flag **explizit** besitzt, wird ihr Wert verwendet.

Wenn sie das Flag nicht explizit besitzt, wird das Flag der globalen Region der Welt verwendet.

Konzeptuell:

```text
Position
  ↓
lokale Region vorhanden?
  ↓ ja
lokales Flag explizit gesetzt?
  ├─ ja → lokalen Wert verwenden
  └─ nein → globalen Weltwert verwenden

keine lokale Region
  ↓
globalen Weltwert verwenden
```

Dadurch können globale Regeln als Weltstandard dienen und lokale Regionen einzelne Regeln überschreiben.

---

## 9. Region-Abfrage

### Einzelne Region anzeigen

```text
/pixelrpg region info <region>
```

Die Ausgabe enthält bei lokalen Regionen unter anderem:

- Name
- UUID
- Welt
- Typ
- Anzahl der Polygonpunkte
- Höhe
- Fläche
- Priorität
- Gilde
- alle aktuellen Flags

Bei der globalen Region werden unter anderem Welt und globale Regeln angezeigt.

---

## 10. Alle Regionen auflisten

```text
/pixelrpg region list
```

Die Liste zeigt pro Region unter anderem:

- Name
- Typ
- Anzahl der Polygonpunkte
- Priorität

---

## 11. Region löschen

```text
/pixelrpg region delete <region>
```

Eine normale lokale Region kann gelöscht werden.

Die globale Region kann ausdrücklich **nicht** gelöscht werden.

Beim Löschen wird auch der räumliche Index der Region bereinigt.

---

## 12. Räumliche Auflösung

Regionen werden über einen Chunk-basierten Index gefunden.

Für eine Position wird nicht jede Region der Welt geprüft.

Das System ermittelt zunächst den betroffenen Chunk und betrachtet die dort registrierten Kandidaten.

Anschließend wird geprüft, ob die Position tatsächlich innerhalb der Polygongeometrie und des Höhenbereichs liegt.

Bei mehreren passenden lokalen Regionen gewinnt die Region mit der höchsten Priorität.

Bei gleicher Priorität wird die UUID zur deterministischen Sortierung herangezogen.

Wenn keine lokale Region passt, wird die globale Weltregion verwendet.

---

## 13. Höhenbereich

Lokale Regionen besitzen:

```text
minY
maxY
```

Eine Position muss gleichzeitig:

1. im richtigen Chunk/Kandidatenbereich liegen,
2. innerhalb des Polygons liegen,
3. zwischen `minY` und `maxY` liegen.

Die globale Region ist dagegen für die gesamte Höhe der Welt gültig.

---

## 14. Spawnpunkte für feindliche Mobs

Während einer laufenden Region-Erstellung kann zusätzlich ein Spawn-Editor verwendet werden.

Der vorhandene Region-Editor besitzt dafür ein Pfeil-Werkzeug.

Die Spawnpunkte werden mit einem bestimmten feindlichen Mob-Typ verknüpft.

Die Region muss anschließend wie gewohnt mit:

```text
/pixelrpg region finish
/pixelrpg region confirm
```

abgeschlossen werden.

### Zulässige Mob-Typen

Der Code prüft den Bukkit-`EntityType` und akzeptiert nur Entity-Typen, deren Entity-Klasse `Monster` implementiert.

Der eingegebene Name wird normalisiert, unter anderem werden Groß-/Kleinschreibung und `-`/`_` berücksichtigt.

Beispielhafte Form:

```text
zombie
skeleton
creeper
spider
```

Entscheidend ist die tatsächliche `EntityType`-/`Monster`-Prüfung des Codes, nicht eine separat gepflegte Liste.

### Spawnpunkt setzen

Der Spawn-Editor setzt den Punkt auf die Blockposition und speichert eine eigene Y-Position oberhalb des angeklickten Blocks.

Spawnpunkte werden separat räumlich indexiert.

---

## 15. Spawnpunkte abfragen

Der `RegionManager` kann Spawnpunkte in einem Chunk-Radius um eine Position suchen.

Zusätzlich kann geprüft werden, ob eine konkrete Position ein expliziter Spawnpunkt für einen bestimmten feindlichen Mob ist.

Damit besitzt das Region-System neben der Polygonprüfung auch eine eigene Spawnpunkt-Infrastruktur.

---

## 16. Speicherung

Die Regionen werden in:

```text
plugins/PixelRPG/regions.yml
```

gespeichert.

Das Format besitzt aktuell:

```text
format-version: 4
```

Gespeichert werden unter anderem:

```text
regions.<uuid>.world
regions.<uuid>.name
regions.<uuid>.type
regions.<uuid>.description
regions.<uuid>.min-y
regions.<uuid>.max-y
regions.<uuid>.priority
regions.<uuid>.owner-guild-id
regions.<uuid>.owner-guild-name
regions.<uuid>.points
regions.<uuid>.flags
regions.<uuid>.properties
regions.<uuid>.spawn-points
regions.<uuid>.enter-message
regions.<uuid>.leave-message
```

Globale Weltregeln liegen getrennt unter:

```text
global-regions.<world>.flags
```

---

## 17. Speicherung ist atomar aufgebaut

Das Repository schreibt zunächst in eine temporäre Datei und versucht anschließend einen atomaren Move auf die eigentliche `regions.yml`.

Falls ein atomarer Move vom Dateisystem nicht unterstützt wird, wird auf einen normalen Replace-Move zurückgefallen.

Damit wird vermieden, die eigentliche Regionsdatei direkt während des Schreibens zu überschreiben.

---

## 18. Asynchrone Persistenz

Der `RegionManager` besitzt einen eigenen Persistence-Executor:

```text
PixelRPG-RegionIO
```

Region-Snapshots werden sequenziell in eine Persistence-Chain eingereiht.

Das System trennt dadurch die Erfassung des aktuellen Serverzustands von der eigentlichen Dateischreiboperation.

---

## 19. Migration

Das Regionsformat besitzt eine Versionsnummer.

Beim Laden älterer Formate kann das Repository eine Migration markieren.

Der aktuelle Stand ist:

```text
CURRENT_FORMAT_VERSION = 4
```

Nach erkannter Migration wird der aktuelle Zustand wieder gespeichert.

Auch ältere Flag-Daten werden beim Laden entsprechend der vorhandenen Formatversion berücksichtigt.

---

## 20. Tab-Completion

Der vorhandene Region-Befehl bietet Tab-Completion für:

### Hauptbefehle

```text
create
finish
confirm
cancel
delete
info
edit
list
```

### Edit-Felder

```text
name
type
description
priority
enter
leave
flag
guild
unguild
property
```

### Flags

Alle Werte aus `RegionFlag` werden für die Flag-Bearbeitung angeboten.

Für den booleschen Wert stehen zur Auswahl:

```text
true
false
```

---

## 21. Aktuelle vollständige Befehlsübersicht

```text
/pixelrpg region create <name>
/pixelrpg region finish
/pixelrpg region confirm
/pixelrpg region cancel
/pixelrpg region delete <region>
/pixelrpg region info <region>
/pixelrpg region list
/pixelrpg region edit <region> <feld> <wert>
```

Das ist die vorhandene Region-Befehlsstruktur. Für die Bearbeitung wird **kein zusätzlicher Region-Befehl benötigt**.

---

## 22. Kurzfassung der Möglichkeiten

| Funktion | Vorhanden |
|---|:---:|
| Globale Region pro Welt | ✓ |
| Globale Flags ändern | ✓ |
| Polygonregion erstellen | ✓ |
| Polygonpunkte setzen | ✓ |
| Polygon live visualisieren | ✓ |
| Polygon validieren | ✓ |
| Region speichern | ✓ |
| Region löschen | ✓ |
| Region nach Name suchen | ✓ |
| Region nach UUID suchen | ✓ |
| Region-Informationen anzeigen | ✓ |
| Regionen auflisten | ✓ |
| Regionname ändern | ✓ |
| Regiontyp ändern | ✓ |
| Beschreibung ändern | ✓ |
| Priorität ändern | ✓ |
| Enter-Nachricht ändern | ✓ |
| Leave-Nachricht ändern | ✓ |
| Gilde zuweisen | ✓ |
| Gilde entfernen | ✓ |
| Freie Properties | ✓ |
| Region-Flags ändern | ✓ |
| Flag-Fallback auf globale Region | ✓ |
| Höhenbereich | ✓ |
| Chunk-basierter Spatial Index | ✓ |
| Feindliche Mob-Spawnpunkte | ✓ |
| Spawnpunkte räumlich indexieren | ✓ |
| Explizite Spawnpunktprüfung | ✓ |
| Atomare Speicherung | ✓ |
| Asynchrone Region-Persistenz | ✓ |
| Versionsmigration | ✓ |
| Tab-Completion | ✓ |

---

## 23. Abgrenzung

Dieses Dokument ist bewusst nur eine **Bestandsaufnahme**.

Es behauptet keine Funktionen, die im aktuellen Code nicht vorhanden sind, und definiert keine zukünftigen Features.

Insbesondere gibt es hier **keinen zusätzlichen Edit-Befehl und kein zusätzliches Region-Menü**. Die vorhandene Befehlsstruktur unter `/pixelrpg region ...` ist die Grundlage des aktuellen Systems.
