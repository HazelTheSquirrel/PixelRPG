# PixelRPG Region System – Konzeptidee

**Status:** Idee / nach dem aktuellen Implementierungsstopp prüfen

## Ziel

Ein eigenes PixelRPG-Region-System als Mischung aus WorldEdit und WorldGuard, jedoch vollständig auf PixelRPG zugeschnitten und nicht auf rechteckige bzw. quadratische Regionen beschränkt.

Das System soll geografische Orte der Welt anhand ihrer tatsächlich definierten Form beschreiben können.

## Grundprinzip

Die Welt hat standardmäßig keine explizit definierte Region. Alles, was keiner PixelRPG-Region zugeordnet ist, gilt automatisch als **Wildnis**.

Eine Region wird über frei gesetzte Eck- bzw. Konturpunkte definiert:

1. Ein spezielles Werkzeug (z. B. ein Stick) wird verwendet.
2. Jeder Klick auf einen Block setzt einen weiteren Punkt der Kontur.
3. Die Punkte bilden eine geschlossene 2D-Polygonkontur auf der X/Z-Ebene.
4. Das System berechnet die Fläche innerhalb dieser Kontur.
5. Diese berechnete Fläche ist die PixelRPG-Region.
6. Der vertikale Bereich kann separat über Min-Y und Max-Y definiert werden.

Es wird **nicht** nur eine Bounding-Box bzw. ein Rechteck gespeichert.

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

Beispiel: Eine Stadt mit kreisförmiger Mauer kann außerhalb der Mauer abgeflogen werden. Entlang der Mauer werden ausreichend viele Punkte gesetzt. Das resultierende Polygon nähert die tatsächliche Form der Stadt an.

## Flächenberechnung

Die Region basiert auf einer klassischen Polygon-Flächenberechnung bzw. Point-in-Polygon-Prüfung.

Für eine Position `(x, z)` wird ermittelt, ob sie innerhalb des definierten Polygons liegt. Liegt sie innerhalb und zusätzlich im definierten Y-Bereich, gehört die Position zur Region.

Die Region soll nicht als Liste jedes einzelnen Blocks gespeichert werden. Primär gespeichert werden die Polygonpunkte sowie der vertikale Bereich und die Regionsdaten.

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

- Name
- Typ
- Beschreibung
- Polygonpunkte
- Min-Y / Max-Y
- Betreten-Nachricht
- Verlassen-Nachricht
- optionale PixelRPG-spezifische Eigenschaften

## Enter / Leave

Das System soll Übergänge zwischen Regionen erkennen.

Beispiel:

> **Willkommen in Silberhain**

Beim Verlassen kann beispielsweise erscheinen:

> **Du verlässt Silberhain**

Damit entsteht eine eigene PixelRPG-Variante der klassischen Enter-/Leave-Regionen, ohne WorldGuard zu benötigen.

Später könnten solche Übergänge zusätzlich für bestehende PixelRPG-Systeme verwendet werden, beispielsweise für Questfortschritt, NPC-/Ortslogik oder andere bereits vorhandene Mechaniken.

## Trennung von Minecraft-Biomen

Minecraft-Biome sollen **nicht** die PixelRPG-Regionen bestimmen.

Ein Minecraft-Biom kann innerhalb weniger Blöcke wechseln, obwohl für den Spieler kein sinnvoller geografischer Ortswechsel stattfindet. Deshalb bleibt die Vanilla-Biome-Logik von der PixelRPG-Ortslogik getrennt.

Beispiel:

- Minecraft: Forest → Birch Forest → Plains
- PixelRPG: weiterhin **Eichenhain**

Minecraft-Biome bleiben für Vanilla-Funktionen zuständig. PixelRPG-Regionen bestimmen die RPG-geografische Bedeutung eines Ortes.

## Architekturgedanke

Das Region-System soll eine zentrale geografische Abstraktion bereitstellen. Andere PixelRPG-Systeme sollen nicht selbst Polygonberechnungen durchführen müssen.

Gedanklich:

`Position -> PixelRPG Region -> Regionseigenschaften`

Dadurch können Quests, NPCs, Gilden, Orte, Bosse oder andere bestehende Systeme später auf dieselbe Regionsebene zugreifen.

## Gültige Geometrie

Die erste technische Prüfung muss sicherstellen, dass nur geeignete, einfache Polygone akzeptiert werden. Selbstüberschneidende Konturen müssen erkannt und abgelehnt oder korrigiert werden können.

## Abgrenzung zum aktuellen Entwicklungsstand

Diese Idee gehört **nicht** zum aktuellen Beta-Implementierungsumfang und darf den bestehenden Implementierungsstopp nicht aufbrechen.

Sie ist als spätere Konzept-/Architekturidee dokumentiert. Eine Implementierung darf erst nach dem Beta-Feedback und nach einer separaten technischen Machbarkeitsprüfung gegen die dann aktuelle Paper-Version begonnen werden.

## Leitgedanke

> Minecraft stellt die Welt bereit. PixelRPG beschreibt, was die tatsächlich gebaute Welt für das RPG bedeutet.

Das System soll daher nicht versuchen, WorldEdit oder WorldGuard vollständig nachzubauen. Es soll nur die für PixelRPG benötigte Kombination aus freier geografischer Markierung, Flächenberechnung und Regionslogik bereitstellen.
