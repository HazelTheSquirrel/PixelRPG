# PixelRPG Region System – Konzeptidee / Roadmap

**Status:** Idee / spätere technische Planung

> Dieses Dokument ist die verbindliche Ideen- und Roadmap-Grundlage für das zukünftige PixelRPG-Region-System. Es beschreibt Ziel, Verhalten und gewünschte Architektur, nicht den aktuellen Implementierungsstand.

## Ziel

Ein eigenes PixelRPG-Region-System als Mischung aus den für PixelRPG benötigten Konzepten von WorldEdit und WorldGuard, jedoch vollständig auf PixelRPG zugeschnitten und **nicht auf rechteckige bzw. quadratische Regionen beschränkt**.

Das System soll geografische Orte der Welt anhand ihrer tatsächlich definierten Form beschreiben können. Die Geometrie wird ausschließlich durch einen Admin festgelegt.

Das System ist **Admin-only**. Spieler und Gilden sollen nicht frei PixelRPG-Regionen erstellen oder deren Geometrie verändern können.

## Grundprinzip

Die Welt hat standardmäßig keine explizit definierte PixelRPG-Region. Alles, was keiner PixelRPG-Region zugeordnet ist, gilt automatisch als **Wildnis**.

Eine Region wird über frei gesetzte Eck- bzw. Konturpunkte definiert:

1. Ein spezielles Werkzeug wird verwendet.
2. Jeder gültige Klick auf einen Block setzt einen weiteren Konturpunkt.
3. Die Punkte werden in ihrer gesetzten Reihenfolge gespeichert.
4. Zwischen jedem Punkt und seinem direkten Nachfolger wird eine Kante dargestellt.
5. Der aktuell letzte Punkt wird während der Erstellung dynamisch mit **P1**, dem ersten Punkt, verbunden.
6. Wird ein neuer Punkt gesetzt, endet die bisherige temporäre Verbindung zum ersten Punkt und der neue letzte Punkt übernimmt diese Verbindung.
7. Beim Abschluss entsteht daraus automatisch die geschlossene Kette `P1 -> P2 -> ... -> Pn -> P1`.
8. Das System validiert die Geometrie.
9. Nur ein gültiges Polygon kann als Region erstellt werden.

Es gibt **keine feste Anzahl an Punkten**. Eine Region kann so viele Punkte besitzen, wie für ihre gewünschte Genauigkeit notwendig sind.

## Geometrie ist nach CREATE unveränderlich

Die Polygon-Geometrie ist ein bewusst unveränderlicher Bestandteil einer bestehenden Region.

Es gibt nur drei grundlegende Vorgänge:

```text
CREATE
→ neue Region inklusive Polygon-Geometrie erstellen

EDIT
→ nur Regionsdaten und Eigenschaften verändern

DELETE
→ komplette Region einschließlich ihrer Geometrie entfernen
```

Die Geometrie selbst kann **nicht** über `EDIT` verändert werden.

Soll sich die tatsächliche Fläche ändern, wird die bestehende Region gelöscht und anschließend als neue Region mit einer neu gesetzten Polygonkontur erstellt.

Dadurch gibt es keinen komplizierten Live-Geometry-Editor und keine Gefahr, dass eine bestehende Region während einer Bearbeitung teilweise oder ungewollt verändert wird.

## Freie Formen

Die Kontur soll beliebige einfache Polygonformen erlauben:

- Rechtecke und Vierecke
- L-, C-, T- und S-Formen
- unregelmäßige Dörfer
- polygonale Städte
- Ruinen
- Festungen
- Straßen bzw. schmale Gebiete
- diagonale Grenzen
- angenäherte Kreisformen
- sonstige durch Punkte beschreibbare einfache Formen

Die Punktanzahl richtet sich ausschließlich nach der gewünschten Genauigkeit.

Beispielsweise kann eine einfache kleine Siedlung mit wenigen Punkten definiert werden, während eine große, unregelmäßige Stadt mit vielen Punkten wesentlich genauer abgebildet werden kann.

Eine angenäherte kreisförmige Stadt wird nicht als Kreisobjekt gespeichert. Stattdessen werden entlang der tatsächlichen Grenze ausreichend viele Punkte gesetzt, sodass das Polygon die gewünschte Form präzise annähert.

## Dynamische geschlossene Kontur während CREATE

Während der Erstellung existiert eine temporäre Abschlusskante zwischen dem aktuell letzten Punkt und P1.

Beispiel nach P6:

```text
P1 → P2 → P3 → P4 → P5 → P6
↑                              │
└──────────────────────────────┘
```

Wird anschließend P7 gesetzt:

```text
P1 → P2 → P3 → P4 → P5 → P6 → P7
↑                                      │
└──────────────────────────────────────┘
```

Die Verbindung `P6 -> P1` wird dabei nicht als zusätzliche Punktkante behandelt. Sie ist die dynamische Abschlusskante des aktuellen Entwurfs. Mit dem Setzen von P7 wird sie automatisch durch `P6 -> P7` und `P7 -> P1` ersetzt.

Beim finalen CREATE wird die geschlossene Kette aus derselben Punktliste erzeugt:

```text
(P1, P2)
(P2, P3)
(P3, P4)
...
(Pn-1, Pn)
(Pn, P1)
```

Damit spielt es keine Rolle, ob eine Region drei, zehn, fünfzig oder mehr Punkte benötigt.

## Admin-Visualisierung beim Erstellen

Die Erstellung soll dem Admin eine möglichst eindeutige visuelle Rückmeldung geben.

### Eckpunkte

Jeder gesetzte Konturpunkt soll während der laufenden CREATE-Session deutlich sichtbar markiert werden.

Bevorzugte Visualisierung:

- auffälliger Marker am Eckpunkt
- zusätzlich ein sichtbarer Beacon-Strahl oder eine vergleichbar eindeutige vertikale Markierung
- die Visualisierung ist ausschließlich temporär und gehört nicht zur gespeicherten Region

Ziel ist, dass der Admin auf einen Blick erkennt, **wo exakt die gesetzten Eckpunkte liegen**.

### Polygon-Grenze

Die Verbindung zwischen den gesetzten Punkten soll ebenfalls deutlich sichtbar dargestellt werden.

Normale, leicht zu übersehende Partikel allein sind nicht ausreichend. Die Grenze soll so visualisiert werden, dass die Form der geplanten Region auch aus größerer Entfernung unmittelbar erkennbar ist.

Die Visualisierung muss sich bei jedem neuen Punkt aktualisieren:

```text
P1 → P2 → ... → P6 → P1

neuer Punkt P7

P1 → P2 → ... → P6 → P7 → P1
```

Die temporäre Visualisierung wird nach erfolgreichem CREATE entfernt.

## CREATE-Workflow

Der geplante Admin-Workflow:

```text
CREATE starten
    ↓
Konturpunkte setzen
    ↓
Eckpunkte + Grenzlinien live visualisieren
    ↓
Kontur abschließen
    ↓
Polygon validieren
    ↓
Preview / Bestätigung
    ↓
Region speichern
    ↓
temporäre Editor-Visualisierung entfernen
```

Ein ungültiges Polygon darf nicht erstellt werden.

## Gültige Geometrie

Die erste technische Prüfung muss sicherstellen, dass nur geeignete, einfache Polygone akzeptiert werden.

Mindestens zu prüfen sind:

- mindestens drei Punkte
- gültige Koordinaten
- keine ungültigen oder degenerierten Konturen
- keine doppelten bzw. unmittelbar identischen Punkte
- keine Nullfläche
- keine Selbstüberschneidungen
- keine sich kreuzenden Polygonkanten
- gültige geschlossene Kontur

Selbstüberschneidende Konturen müssen abgelehnt werden.

Der Admin soll eine verständliche Fehlermeldung erhalten, damit die Kontur korrigiert werden kann. Da die Geometrie erst bei erfolgreichem CREATE dauerhaft gespeichert wird, bleibt eine bereits vorhandene Region bei einem fehlgeschlagenen neuen CREATE unangetastet.

## Flächenberechnung

Die Region basiert auf einer klassischen Polygon-Flächenberechnung bzw. Point-in-Polygon-Prüfung.

Für eine Position `(x, z)` wird ermittelt, ob sie innerhalb des definierten Polygons liegt. Liegt sie innerhalb und zusätzlich im definierten Y-Bereich, gehört die Position zur Region.

Die Region soll **nicht** als Liste jedes einzelnen Blocks gespeichert werden. Primär gespeichert werden die Polygonpunkte sowie der vertikale Bereich und die Regionsdaten.

## X/Z-Geometrie und Y-Bereich

Die eigentliche Kontur ist eine 2D-Geometrie auf der X/Z-Ebene.

Der vertikale Bereich wird davon getrennt behandelt:

```text
Polygon.contains(x, z)
        AND
minY <= y <= maxY
```

Dadurch kann dieselbe X/Z-Fläche beispielsweise unterschiedliche vertikale PixelRPG-Orte enthalten.

Beispiel:

```text
Y 60–120 → Stadt
Y 20–59  → Katakomben
```

Die Geometrie bleibt dabei unabhängig von der eigentlichen Regionslogik.

## Region als Weltbeschreibung

Die Region ist nicht ausschließlich ein Schutzgebiet. Sie beschreibt, **was ein geografischer Ort für PixelRPG bedeutet**.

Beispiele für Regionstypen:

- Wildnis (Default)
- Dorf
- Stadt
- Gildenstadt
- Ruine
- Festung
- Dungeon
- Gefahrengebiet
- Bossgebiet
- sonstige PixelRPG-Orte

Eine Region könnte beispielsweise enthalten:

- eindeutige Region-ID
- Name
- Typ
- Beschreibung
- Polygonpunkte
- Min-Y / Max-Y
- Owner
- Betreten-Nachricht
- Verlassen-Nachricht
- Flags
- optionale PixelRPG-spezifische Eigenschaften

## EDIT – nur Regionsdaten, niemals die Geometrie

`EDIT` dient ausschließlich der Änderung der Daten einer bereits existierenden Region.

Beispielsweise:

- Name
- Typ
- Beschreibung
- Owner
- Gilde
- Flags
- Enter-Text
- Leave-Text
- sonstige Properties

Die Polygonpunkte und damit die tatsächliche geografische Fläche bleiben unverändert.

Soll die Fläche verändert werden, gilt ausschließlich:

```text
DELETE alte Region
CREATE neue Region
```

## DELETE

`DELETE` entfernt die vollständige Regiondefinition.

Dabei werden insbesondere entfernt:

- Region-ID / Zuordnung
- Polygon-Geometrie
- Y-Bereich
- Owner
- Flags
- Nachrichten
- weitere Regionsdaten

Nach dem Löschen gilt der entsprechende Bereich wieder als Wildnis, sofern dort keine andere gültige PixelRPG-Region greift.

## Region-Flags

Ein zentraler Bestandteil der Region-Idee sind **WorldGuard-inspirierte Flags**, jedoch ausschließlich als PixelRPG-eigene Funktionalität.

Die Region kann damit Regeln für ihren geografischen Bereich definieren.

Beispiele:

- `PVP = false`
- `MONSTER_SPAWN = false`
- `BLOCK_BREAK = ...`
- `BLOCK_PLACE = ...`
- weitere PixelRPG-spezifische Regeln

Die Flags gehören zur Region und werden über `EDIT` verändert.

Wichtig: PixelRPG soll nicht versuchen, WorldGuard vollständig nachzubauen. Es werden nur die für PixelRPG tatsächlich benötigten Regeln umgesetzt.

## Gilden und Ownership

Eine Region kann einem Owner bzw. einer Gilde zugeordnet werden.

Beispiel:

```text
Region: Silberhain
Typ: Gildenstadt
Owner: Gilde XYZ

Flags:
PVP = false
MONSTER_SPAWN = false
```

Damit beschreibt die Region gleichzeitig:

- **wo** Silberhain liegt → Polygon
- **was** Silberhain ist → RegionType / Properties
- **wem** Silberhain zugeordnet ist → Owner
- **was dort gilt** → Flags

Die Gilde erhält dadurch nicht automatisch das Recht, die PixelRPG-Region selbst zu verändern. Die Region-Geometrie bleibt Admin-only.

## Gilden-Claims als separates Konzept

Ein möglicher späterer Bestandteil ist ein separates Gilden-Claim-System.

Dabei soll ein Claim nicht mit der Polygon-Region verwechselt werden.

```text
Gilden-Claim
→ Besitz / Verwaltungsbereich

PixelRPG-Region
→ geografische Bedeutung des Ortes

Region-Flags
→ Regeln innerhalb des Ortes
```

Ein Gilden-Claim könnte beispielsweise auf Minecraft-Chunks basieren, während die eigentliche Stadt weiterhin als präzises Polygon definiert wird.

Damit kann eine Gilde einen zusammenhängenden Verwaltungsbereich besitzen, während die tatsächliche Stadtgrenze unabhängig davon eine frei definierte Polygonform besitzt.

## Erweiterung von Gildenstädten / Bauabschnitte

Langfristig soll das System auch kontrollierte Stadterweiterungen unterstützen können.

Beispiel:

```text
Bestehende Region
Silberhain

        ↓

Gilde plant Erweiterung
        ↓
Bauabschnitt markieren
        ↓
Admin prüft
        ↓
Genehmigung
        ↓
Bau wird fertiggestellt
```

Der Bauabschnitt kann als eigenes, temporäres geografisches Objekt betrachtet werden. Nach Fertigstellung kann der Admin die bestehende Region gemäß dem gewünschten Endzustand neu definieren.

Da die Geometrie unveränderlich ist, wird die bestehende Region bei einer tatsächlichen Grenzänderung gelöscht und anschließend mit der neuen vollständigen Kontur neu erstellt.

Dadurch bleibt jederzeit klar, welche Geometrie aktuell tatsächlich als Region gilt.

## Enter / Leave

Das System soll Übergänge zwischen Regionen erkennen.

Beispiel:

> **Willkommen in Silberhain**

Beim Verlassen kann beispielsweise erscheinen:

> **Du verlässt Silberhain**

Die Übergangserkennung soll nicht nur für Nachrichten dienen. Sie kann später als zentrale Information für bestehende PixelRPG-Systeme genutzt werden.

Beispiele:

- Questfortschritt
- NPC-/Ortslogik
- Dungeon-Logik
- Bossgebiete
- Gildenlogik
- weitere ortsabhängige Mechaniken

## Überlappende Regionen

Das System muss definieren können, wie mit geometrischen Überschneidungen umgegangen wird.

Eine mögliche spätere Regel ist eine explizite Regionspriorität:

```text
niedrige Priorität
    ↓
höhere Priorität
```

Beispiel:

```text
Stadt        priority 10
Dungeon      priority 50
Bossgebiet   priority 100
```

Damit kann beispielsweise ein Dungeon geometrisch innerhalb einer Stadt liegen und trotzdem als Dungeon erkannt werden.

Die konkrete Prioritäts- und Überlappungslogik muss vor der Implementierung abschließend festgelegt werden.

## Trennung von Minecraft-Biomen

Minecraft-Biome sollen **nicht** die PixelRPG-Regionen bestimmen.

Ein Minecraft-Biom kann innerhalb weniger Blöcke wechseln, obwohl für den Spieler kein sinnvoller geografischer Ortswechsel stattfindet. Deshalb bleibt die Vanilla-Biome-Logik von der PixelRPG-Ortslogik getrennt.

Beispiel:

- Minecraft: Forest → Birch Forest → Plains
- PixelRPG: weiterhin **Eichenhain**

Minecraft-Biome bleiben für Vanilla-Funktionen zuständig. PixelRPG-Regionen bestimmen die RPG-geografische Bedeutung eines Ortes.

## Zentrale Architektur

Das Region-System soll eine zentrale geografische Abstraktion bereitstellen. Andere PixelRPG-Systeme sollen **nicht selbst Polygonberechnungen durchführen müssen**.

Gedanklich:

```text
Position
   ↓
RegionService
   ↓
PixelRPG Region
   ↓
Regionseigenschaften / Flags / Owner
```

Dadurch können Quests, NPCs, Gilden, Orte, Bosse oder andere bestehende Systeme später auf dieselbe Regionsebene zugreifen.

## Geometrie und Runtime strikt trennen

Die Geometrie soll als eigene technische Abstraktion behandelt werden.

Gedanklich:

```text
RegionGeometry
    ↓
Polygon

PixelRegion
    ↓
Geometry + Metadaten + Flags + Properties

RegionService
    ↓
Lookup + Runtime + Transition-Erkennung
```

Die Runtime soll nicht wissen müssen, wie ein Polygon editiert wurde. Der Editor wiederum soll nicht für die eigentliche Regionslogik verantwortlich sein.

## Karte / Map-System

Langfristig soll PixelRPG eine eigene Kartenansicht erhalten können.

Die Karte ist dabei **nicht die Quelle der Region-Geometrie**, sondern eine Visualisierung der bereits vorhandenen Daten.

Mögliche Darstellung:

- tatsächliche Weltkarte
- Chunk-Raster als Orientierung
- PixelRPG-Regionengrenzen
- Gilden-Claims
- Städte
- Dungeons
- weitere relevante Orte
- spätere Bauvorhaben

Das Region-System bleibt die Single Source of Truth. Die Karte liest die Geometrie aus dem Region-System und stellt sie dar.

## Mögliche Karten-/Claim-Idee

Eine spätere `M`-Karte könnte Spielern eine direkte Übersicht über die Welt geben.

Für Gilden könnte sie zusätzlich als Grundlage für die Auswahl von Claim-Flächen dienen.

Dabei bleiben zwei Dinge getrennt:

```text
Karte
→ Visualisierung / Interaktion

Region
→ präzise geografische PixelRPG-Geometrie

Gilden-Claim
→ Besitz / Verwaltung
```

Ein Gilden-Claim kann auf Minecraft-Chunks basieren, während die PixelRPG-Region weiterhin eine beliebig präzise Polygonkontur besitzt.

## Keine Blocklisten

Das System soll niemals versuchen, eine Region als Liste aller enthaltenen Blöcke zu speichern.

Nicht gewünscht:

```text
Block 1
Block 2
Block 3
...
Block 500000
```

Gewünscht:

```text
Polygonpunkte
+ MinY
+ MaxY
+ Regionsdaten
```

Die Zugehörigkeit einer Position wird zur Laufzeit geometrisch bestimmt.

## Performance / Lookup

Eine naive Implementierung würde bei jeder Positionsprüfung alle Regionen der Welt prüfen. Für kleine Mengen kann dies zunächst ausreichen, soll aber nicht die endgültige Architektur vorgeben.

Langfristig soll die Region-Suche über eine geeignete räumliche Vorfilterung optimiert werden.

Beispielsweise kann ein grober Chunk-/Bounding-Index dazu dienen, nur mögliche Kandidaten für eine Point-in-Polygon-Prüfung zu betrachten.

Wichtig bleibt:

```text
Index = Optimierung
Polygon = Wahrheit
```

Der Index darf niemals die eigentliche Region-Geometrie ersetzen.

## MultiPolygon als spätere Erweiterungsmöglichkeit

Die erste Implementierung kann auf einfache Polygone beschränkt werden.

Die Architektur sollte jedoch nicht unnötig verhindern, dass später mehrere getrennte Polygonflächen zu einer logischen Region zusammengefasst werden können.

Beispiel:

```text
Silberhain
 ├── Polygon A
 ├── Polygon B
 └── Polygon C
```

Das ist eine spätere Erweiterung und kein notwendiger Bestandteil der ersten Version.

## Abgrenzung zu WorldEdit / WorldGuard

PixelRPG soll **kein vollständiger Ersatz** für WorldEdit oder WorldGuard werden.

Übernommen werden lediglich Konzepte, die für PixelRPG sinnvoll sind:

```text
WorldEdit-Idee
→ freie geografische Definition

WorldGuard-Idee
→ Flags / Regeln

PixelRPG
→ beides als eigene, schlanke RPG-Infrastruktur
```

Die Region soll vor allem beantworten:

> Was ist dieser geografische Bereich für PixelRPG und welche Regeln gelten dort?

## Architekturgedanke

Das System soll die geografische Ebene zentralisieren.

```text
Minecraft Welt
      ↓
PixelRPG Geometrie
      ↓
PixelRPG Region
      ↓
Owner / Flags / Properties
      ↓
Quests / NPCs / Gilden / Dungeons / Bosse / Karte
```

Kein nachgelagertes System soll eigene, voneinander abweichende Regionsberechnungen implementieren.

## Geplanter Roadmap-Ablauf

### Phase 1 – Geometrie-Grundlage

- Punktmodell definieren
- Polygonmodell definieren
- X/Z-Geometrie
- Min-Y / Max-Y
- Point-in-Polygon
- Flächenberechnung
- Polygonvalidierung
- Selbstüberschneidungen erkennen

### Phase 2 – Admin CREATE

- CREATE-Session
- Konturpunkte setzen
- beliebige Punktanzahl
- dynamische `Pn -> P1`-Verbindung
- Eckpunktvisualisierung
- deutliche Grenzvisualisierung
- Preview
- Validierung
- endgültiges Speichern

### Phase 3 – Region Runtime

- RegionRegistry
- RegionService
- Positions-Lookup
- Wildnis als Default
- Y-Bereich
- mögliche Prioritätslogik

### Phase 4 – Region EDIT / DELETE

- Metadaten bearbeiten
- Owner bearbeiten
- Flags bearbeiten
- Enter-/Leave-Texte bearbeiten
- Properties bearbeiten
- vollständiges DELETE
- Geometrie bleibt über EDIT unangetastet

### Phase 5 – Enter / Leave

- Regionswechsel erkennen
- ENTER-Event
- LEAVE-Event
- Integration mit weiteren PixelRPG-Systemen

### Phase 6 – Flags

- PixelRPG-Flag-Modell
- Flag-Auswertung
- PVP
- Monster-Spawn
- weitere benötigte Regeln
- Prioritäten bei konkurrierenden Regeln definieren

### Phase 7 – Gildenintegration

- Owner/Gilden-Zuordnung
- Gilden-Claims als separates Konzept
- Claim- und Regionslogik sauber trennen

### Phase 8 – Karte

- Weltkarten-Grundlage
- Chunk-Raster als Orientierung
- Regionengrenzen darstellen
- Gilden-Claims darstellen
- spätere Interaktion mit Gilden-Claims

### Phase 9 – Erweiterungen

- Bauvorhaben
- kontrollierte Stadterweiterungen
- MultiPolygon
- räumlicher Index
- weitere ortsabhängige PixelRPG-Systeme

## Abgrenzung zum aktuellen Entwicklungsstand

Diese Idee gehört **nicht** zum aktuellen Beta-Implementierungsumfang und darf einen bestehenden Implementierungsstopp nicht aufbrechen.

Sie ist als spätere Konzept-/Architekturidee dokumentiert. Eine Implementierung darf erst nach dem Beta-Feedback und nach einer separaten technischen Machbarkeitsprüfung gegen die dann aktuelle Paper-Version begonnen werden.

Vor der tatsächlichen Implementierung müssen insbesondere die aktuellen Paper-26.x-APIs und die Integration in die dann bestehende Central Content Pipeline geprüft werden.

## Leitgedanke

> Minecraft stellt die Welt bereit. PixelRPG beschreibt, was die tatsächlich gebaute Welt für das RPG bedeutet.

Das System soll nicht versuchen, WorldEdit oder WorldGuard vollständig nachzubauen. Es soll die für PixelRPG benötigte Kombination aus **freier geografischer Markierung, präziser Polygon-Geometrie, Regionslogik, Flags und späterer Karten-/Gildenintegration** bereitstellen.
