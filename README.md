# PixelRPG

**PixelRPG** ist das eigenständige MMORPG-/RPG-Plugin des **Pixel-Servers**.  
Es bildet die zentrale RPG-Schicht des Servers und verbindet Charakterprogression, Quests, Story, NPCs, Berufe, Crafting, Wirtschaft, Gruppen, Gilden, Begleiter, Bosse und Dialoge zu einem gemeinsamen Spielsystem.

> **Projektstatus:** Aktiver Entwicklungsstand  
> **Plugin-Version:** 1.0.0  
> **Plattform:** Paper 26.2  
> **Java:** 25  
> **Mappings:** Mojang-Mappings

---

## Inhaltsverzeichnis

- [Über PixelRPG](#über-pixelrpg)
- [Technische Basis](#technische-basis)
- [Spielsysteme](#spielsysteme)
  - [Charakter & Progression](#charakter--progression)
  - [Quests](#quests)
  - [Story-Kampagne](#story-kampagne)
  - [NPC-System](#npc-system)
  - [Dialoge & Menüs](#dialoge--menüs)
  - [Berufe & Crafting](#berufe--crafting)
  - [Items & Ausrüstung](#items--ausrüstung)
  - [Kampf & Statistiken](#kampf--statistiken)
  - [Bosse](#bosse)
  - [Party](#party)
  - [Gilden](#gilden)
  - [Begleiter](#begleiter)
  - [Wirtschaft](#wirtschaft)
  - [Reisen](#reisen)
- [Story-Fortschritt](#story-fortschritt)
- [Datenhaltung](#datenhaltung)
- [NPC-Typen](#npc-typen)
- [Befehle](#befehle)
- [Berechtigungen](#berechtigungen)
- [Konfiguration](#konfiguration)
- [Projektstruktur](#projektstruktur)
- [Entwicklung](#entwicklung)
- [Build & Verifikation](#build--verifikation)
- [Grundsätze](#grundsätze)

---

## Über PixelRPG

PixelRPG ist kein einzelnes Minispiel, sondern die RPG-Kernschicht des Pixel-Servers.

Das Plugin übernimmt unter anderem:

- persistente Spielerprofile
- Level- und Charakterprogression
- RPG-Statistiken
- Quests und Questfortschritt
- eine zusammenhängende Story-Kampagne
- Story-NPCs und NPC-Interaktionen
- native Dialoge
- Berufe und Berufsstufen
- freischaltbare Crafting-Rezepte
- eigene Progressionsausrüstung
- Währung, Bank und Shops
- Party-System
- Gilden-System inklusive Gildenbank
- Begleiter
- Reise-/Travel-NPCs
- Biome-Bosse und Weltbosse
- Beute, Erfahrung und Boss-Drops
- serverseitige Administration und Debugging

Die Systeme sind miteinander verbunden. Beispielsweise können Quests Storyfortschritt auslösen, Story-NPCs abhängig vom persönlichen Fortschritt sichtbar werden und Progressionsausrüstung über Berufe, Crafting und Boss-Beute miteinander verknüpft sein.

---

## Technische Basis

PixelRPG wird ausschließlich für die aktuelle Projektbasis entwickelt:

| Komponente | Version / Basis |
|---|---|
| Minecraft/Paper | **Paper 26.2** |
| Paper API | **Paper 26.x** |
| Java | **Java 25** |
| Mappings | **Mojang-Mappings** |
| Paperweight Userdev | **2.0.0-beta.21** |
| Paper Dev Bundle | **26.2.build.121-stable** |
| Gson | **2.13.1** |
| HikariCP | **7.0.2** |
| MySQL Connector/J | **9.7.0** |
| Build-System | **Gradle** |
| Shadow | **9.6.1** |

Das Plugin verwendet das aktuelle **Paper-Plugin-System** mit `paper-plugin.yml`.

Legacy-Bukkit-/CraftBukkit-/Legacy-NMS-Techniken gehören ausdrücklich nicht zur technischen Basis des Projekts.

---

# Spielsysteme

## Charakter & Progression

Spieler besitzen ein persistentes PixelRPG-Profil. Darin werden unter anderem RPG-Fortschritt und Storyfortschritt verwaltet.

Die Charakterentwicklung bildet die Grundlage für die übrigen Systeme:

- Level
- RPG-Statistiken
- Ausrüstung
- Quests
- Story
- Berufe
- Begleiter
- soziale Systeme
- Boss- und Content-Progression

Das System ist auf langfristige Progression bis **Level 99** ausgelegt.

---

## Quests

PixelRPG besitzt ein eigenes Quest-System mit zentralem Quest-Repository und Quest-Manager.

Quests können unter anderem:

- Fortschritt speichern
- Ziele verfolgen
- Belohnungen vergeben
- mit der Story verbunden sein
- Spieler- und Gruppenfortschritt berücksichtigen
- über NPCs bzw. Dialoge gestartet oder fortgeführt werden

Aktuelle Questdaten liegen unter:

`src/main/resources/data/quests/`

Wichtige Questdaten:

- `quests_v2.json`
- `quests_story.json`
- `quests_additional.json`
- `quests_crafting_orders.json`

Das Quest-System ist damit datengetrieben und kann über die Ressourcen erweitert werden, ohne die komplette Questlogik neu zu implementieren.

---

## Story-Kampagne

PixelRPG enthält eine eigenständige Story-Kampagne mit einer linearen Hauptprogression von **Level 1 bis 99**.

Die Kampagne ist in Kapitel und Handlungsabschnitte gegliedert. Kapitel können an bestimmte Minecraft-Strukturen gekoppelt sein.

Aktuell umfasst die Kampagnendefinition **64 mögliche Kapitelplätze**; die lineare Hauptkampagne nutzt derzeit die Ordnungen **0 bis 19**.

Beispiele für Story-Schauplätze:

- Dörfer
- Schiffswracks
- Wüstenpyramiden
- Dschungeltempel
- Sumpfhütten
- Ozeanruinen
- Ozeanmonumente
- Trail Ruins
- Minenschächte
- Plünderer-Außenposten
- Woodland Mansions
- Ruined Portals
- Ancient Cities
- Netherfestungen
- Bastionen
- Strongholds
- End
- End Cities

Die Kampagne endet nicht einfach mit einem einzelnen Bosskampf. Der Story-Fortschritt verbindet die verschiedenen Dimensionen und Strukturen zu einer zusammenhängenden Server-Lore.

Die Storydaten befinden sich in:

`src/main/resources/data/story_campaign.yml`

Zusätzlich existieren Story-Komponenten für:

- Story-Manager
- Story-NPC-Dialoge
- Story-NPC-Sichtbarkeit
- Story-Trigger
- Story-Locations
- Story-Archivierung

### Pixel-Archiv

Abgeschlossene Story-Kapitel können im Pixel-Archiv erneut gelesen werden.

Das Archiv dient der persönlichen Story-Historie des Spielers und verändert beim Lesen bereits abgeschlossener Kapitel nicht den aktuellen Storyfortschritt.

---

## NPC-System

PixelRPG besitzt ein eigenes persistentes NPC-System.

NPCs werden mit eindeutiger ID, Typ, Position, Name und optionalen Zusatzdaten verwaltet. NPCs werden in der NPC-Datenhaltung gespeichert und bei geladenen Chunks wiederhergestellt.

Das System unterstützt unter anderem:

- persistente NPC-Positionen
- NPC-Typen
- NPC-Verhalten
- Dialoge
- Interaktionen
- Berufe
- Shops
- Reisen
- Story-Interaktionen
- individuelle Sichtbarkeit
- Skin-Verwaltung
- Chunk-basiertes Spawning

### Story-NPC-Sichtbarkeit

Story-NPCs werden nicht einfach dauerhaft für jeden Spieler sichtbar gemacht.

PixelRPG besitzt eine eigene Sichtbarkeitslogik, mit der Story-NPCs abhängig vom persönlichen Storyfortschritt pro Spieler ein- bzw. ausgeblendet werden können.

Dadurch können Story-NPCs bereits in der Welt existieren, ohne dass sie für Spieler außerhalb des relevanten Storyfortschritts sichtbar sein müssen.

### Travel-NPCs

Travel-NPCs sind normale, persistente PixelRPG-NPCs und werden aktuell über die vorhandene NPC-Datenhaltung platziert.

Eine automatische Erkennung von Dörfern und Glocken zur automatischen Travel-NPC-Erzeugung ist derzeit **nicht Bestandteil des Systems**.

---

## Dialoge & Menüs

PixelRPG verwendet das native Dialog-System der aktuellen Paper-26.x-Plattform.

Dazu gehören unter anderem:

- Charakterprofil
- aktive Quests
- Party
- Gilde
- Begleiter
- Berufe
- NPC-Dialoge
- Story-Dialoge
- Reise-Dialoge
- Quest-Dialoge
- Bestätigungsdialoge

Das zentrale Spieler-Menü ist über die native Quick-Actions-Integration erreichbar.

Die Dialoge sind nicht als klassisches Inventar-GUI aufgebaut, sondern nutzen das aktuelle Paper-/Minecraft-Dialogsystem.

---

## Berufe & Crafting

PixelRPG besitzt ein eigenes Berufssystem mit Berufsstufen und freischaltbaren Rezepten.

Crafting-Rezepte können abhängig von der Berufsstufe freigeschaltet werden und besitzen unter anderem:

- benötigten Beruf
- erforderliche Berufsstufe
- Materialkosten
- optionale Itemkosten
- Freischaltpreis
- Seltenheit
- Ergebnis
- Item-ID

Die Rezeptdaten liegen unter:

`src/main/resources/data/recipes/crafting-recipes.json`

Damit können Berufe langfristig erweitert werden, ohne die grundlegende Crafting-Engine zu verändern.

---

## Items & Ausrüstung

PixelRPG verwendet eigene Item-Definitionen und eigene PixelRPG-Item-IDs.

Neben normalen Minecraft-Materialien existieren damit servereigene Items, beispielsweise für:

- Waffen
- Rüstungen
- Boss-Beute
- Progressionsgegenstände
- Questgegenstände
- Währungen bzw. wirtschaftliche Systeme
- Crafting

Die zentrale Itemdefinition befindet sich unter:

`src/main/resources/data/item-definitions.json`

### Progressionsausrüstung

Das Plugin enthält eine eigene Ausrüstungslinie, die an Level, Berufe, Crafting und teilweise Boss-Beute gekoppelt ist.

Die Progression umfasst unter anderem Ausrüstungsstufen rund um:

- Eisen
- Kupfer
- Kettenrüstung
- Gold
- Diamant
- Netherit

Boss-spezifische Gegenstände können zusätzlich als benötigte Materialien für höherstufige Ausrüstung verwendet werden.

---

## Kampf & Statistiken

PixelRPG besitzt ein eigenes Stat-System und eine eigene Kampflogik.

Zu den berechneten RPG-Werten gehören unter anderem:

- maximale Lebenspunkte
- Rüstung
- kritische Trefferchance
- kritischer Schaden
- Lifesteal
- Reichweite
- Angriffskraft

Die Werte werden aus dem Spielerzustand bzw. der aktuellen Ausrüstung berechnet und zentral verwaltet.

### Mob-Skalierung

Normale Mobs können abhängig von verschiedenen Faktoren skaliert werden.

Die Konfiguration berücksichtigt unter anderem:

- Level
- maximale Lebenspunkte
- Schaden
- Spielerparität
- Dimension
- Erfahrung

Für die drei Dimensionen sind eigene Basisskalierungen definiert:

- Oberwelt
- Nether
- Ende

Dadurch bleibt die RPG-Progression auch außerhalb von speziell definierten Bosskämpfen relevant.

---

## Bosse

PixelRPG besitzt ein eigenes Boss-System.

Es gibt zwei grundlegende Bossarten:

### Biome-Bosse

Biome-Bosse werden bestimmten Biomen zugeordnet und können dort als besondere Gegner erscheinen.

Das System enthält eine umfangreiche Auswahl an Bossdefinitionen mit:

- eigenem Namen
- Entity-Typ
- Level
- Lebensmultiplikator
- Schadensmultiplikator
- Skalierung
- Angriffsmustern
- Biomen
- Beute
- Geldbelohnung
- Erfahrung
- eigenen Boss-Items

### Weltbosse

Weltbosse besitzen zusätzlich ein Phasensystem.

Aktuell sind unter anderem Weltboss-Konzepte wie:

- Der Risskoloss
- Der Sturmherrscher
- Der Abgrundfürst
- Der Seelenverschlinger
- Der Endbote
- Der Uralte Weltenwächter

definiert.

Weltboss-Phasen können beispielsweise:

- Angriffsmuster verändern
- das Angriffstempo verändern
- zusätzliche Gegner beschwören
- einen Enrage-Zustand auslösen
- neue Kampfansagen ausgeben

Bossdaten werden zentral über das Boss-Repository geladen und validiert.

---

## Party

PixelRPG besitzt ein eigenes Party-System.

Unterstützt werden unter anderem:

- Party erstellen
- Spieler einladen
- Einladungen annehmen
- Party verlassen
- Spieler entfernen
- Party-Leitung übertragen
- Party auflösen
- Party-Informationen anzeigen
- Party-Dialog
- Quest-Sharing innerhalb einer definierten Reichweite

Die konfigurierte Quest-Share-Reichweite beträgt derzeit **24 Blöcke**.

Party-Funktionen sind sowohl über das Dialogsystem als auch über Befehle erreichbar.

---

## Gilden

PixelRPG besitzt ein eigenes Gilden-System.

Dazu gehören:

- Gilde gründen
- Gildenverwaltung
- Gildeninteraktionen
- Gildenbank
- Gildenbezogene Dialoge

Die Gildenfunktionen sind in das zentrale Spieler-Menü und das Dialogsystem integriert.

---

## Begleiter

PixelRPG enthält ein eigenes Begleiter-System.

Begleiter können über das zentrale Spieler-Menü verwaltet werden.

Die Begleiterlogik ist als eigener Service aufgebaut und kann damit unabhängig von den übrigen Dialog- und Quest-Systemen erweitert werden.

---

## Wirtschaft

PixelRPG besitzt eigene wirtschaftliche Systeme.

Dazu gehören unter anderem:

- PixelRPG-Währung
- Bank
- Shops
- Geldbelohnungen
- Questbelohnungen
- Bossbelohnungen
- Gildenbank
- kaufbare Rezeptfreischaltungen
- Soulbound-Mechanik

Die Währung besitzt eine konfigurierte maximale Stackgröße von **99**.

### Soulbound

Für bestimmte Gegenstände existiert eine Soulbound-Mechanik.

Aktuell ist dafür unter anderem eine Levelvoraussetzung und ein konfigurierbarer Preis vorgesehen.

---

## Reisen

Travel-NPCs ermöglichen Reise-/Transportinteraktionen über das PixelRPG-Dialogsystem.

Die Reise-Funktion ist als eigenes System mit `TravelDialog` und `TravelBehavior` integriert.

Travel-NPCs werden derzeit nicht automatisch anhand von Dörfern oder Glocken erzeugt, sondern über die bestehende NPC-Verwaltung platziert.

---

# Story-Fortschritt

Die Story ist bewusst persönlich pro Spieler.

Das bedeutet:

- Storyfortschritt wird im Spielerprofil gespeichert.
- Story-NPC-Sichtbarkeit kann pro Spieler unterschiedlich sein.
- Bereits abgeschlossene Kapitel können archiviert und erneut gelesen werden.
- Story-Trigger können Fortschritt auslösen.
- Strukturen können als Story-Ziele dienen.
- Die Kampagne kann mit Questfortschritt gekoppelt werden.

Die Welt muss dadurch nicht für jeden Spieler denselben Storyzustand besitzen.

---

# Datenhaltung

PixelRPG unterstützt eine konfigurierbare Datenhaltung.

Standardmäßig ist in der aktuellen Konfiguration YAML eingestellt:

`storage.type: YAML`

Zusätzlich ist MySQL-Unterstützung vorgesehen.

### YAML

YAML eignet sich für eine einfache Serverinstallation und lokale Datenhaltung.

### MySQL

Für größere Serverumgebungen kann die Datenhaltung über MySQL erfolgen.

Die Verbindung wird über HikariCP verwaltet.

Konfiguration:

`src/main/resources/config.yml`

Beispielbereiche:

- `storage`
- `storage.mysql`
- `statistics`
- `economy`
- `combat`
- `mob-scaling`
- `npc`
- `quests`
- `bosses`
- `story`
- `scoreboard`

**Wichtig:** Das mitgelieferte Passwort `CHANGE_ME` ist lediglich ein Platzhalter und muss vor einem produktiven MySQL-Betrieb ersetzt werden.

---

# NPC-Typen

Das NPC-System unterscheidet verschiedene funktionale NPC-Typen.

Je nach Typ können unterschiedliche Interaktionen bzw. Verhaltensweisen verwendet werden, beispielsweise:

- Empfang / Rathaus
- Quest-NPC
- Story-NPC
- Travel-NPC
- Shop-NPC
- Banker
- Berufstrainer
- weitere spezialisierte NPC-Funktionen

Die konkrete Verhaltensebene wird über eigene Behavior-Klassen umgesetzt.

---

# Befehle

Der zentrale Befehl ist:

`/pixelrpg`

Ohne Unterbefehl zeigt PixelRPG die verfügbaren Unterbefehle und deren Beschreibung an.

Aktuelle Spieler-/Systembereiche umfassen unter anderem:

| Unterbefehl | Zweck |
|---|---|
| `/pixelrpg party` | Party-Dialog öffnen |
| `/pixelrpg party invite <Spieler>` | Spieler einladen |
| `/pixelrpg party accept` | Einladung annehmen |
| `/pixelrpg party leave` | Party verlassen |
| `/pixelrpg party kick <Spieler>` | Spieler entfernen |
| `/pixelrpg party transfer <Spieler>` | Party-Leitung übertragen |
| `/pixelrpg party disband` | Party auflösen |
| `/pixelrpg party info` | Party-Informationen |
| `/pixelrpg dialogue` | zentralen Dialogdienst öffnen |

Weitere registrierte Bereiche umfassen:

- Items
- Spieleradministration
- Debugging
- Quest-Log
- Dialoge
- Gilden

Administrations- und Debug-Funktionen besitzen eigene Berechtigungen.

---

# Berechtigungen

PixelRPG definiert aktuell zwei zentrale Berechtigungen:

| Permission | Bedeutung | Standard |
|---|---|---|
| `rpg.member` | Zugriff auf registrierte Spielerfunktionen | true |
| `rpg.admin` | administrative PixelRPG-Funktionen | op |

Nicht jeder Unterbefehl benötigt zwingend eine spezielle Permission. Die konkrete Berechtigungsprüfung erfolgt auf Ebene des jeweiligen SubCommands.

---

# Konfiguration

Die zentrale Konfiguration liegt unter:

`src/main/resources/config.yml`

Wichtige Konfigurationsbereiche:

### Speicherung

`storage.*`

- Speicherart
- Speicher-Threads
- Lade-Timeout
- MySQL-Verbindung
- Connection-Pool

### Wirtschaft

`economy.*`

- Bank
- Währung
- Stackgröße

### Kampf

`combat.*`

- Boss-Schadensbegrenzung

### Story

`story.*`

- Zeichen pro Dialogzeile
- Zeilen pro Dialogseite

### Mob-Skalierung

`mob-scaling.*`

- Namensschild-Dauer
- Erfahrung
- Schaden
- Lebenspunkte
- Spielerparität
- Dimensionsskalierung

### NPCs

`npc.*`

- Blickradius
- Namensschildradius
- Blickintervall

### Quests

`quests.*`

- Party-Share-Reichweite

### Bosse

`bosses.*`

- Bossbar-Reichweite
- Aktualisierungsintervalle
- Phasenprüfung
- Biome-Boss-Spawning

### Statistiken

`statistics.*`

- Autosave-Intervall

### Scoreboard

`scoreboard.*`

- Aktualisierungsintervall

---

# Projektstruktur

Die wichtigsten Bereiche des Quellcodes sind logisch nach Systemen getrennt:

```text
src/main/java/de/pixelrpg/rpg/
├── boss/           # Bossdefinitionen und Bossverwaltung
├── combat/         # Kampf- und Combat-State
├── command/        # zentrale Befehle und SubCommands
├── dialogue/       # Dialoge und Dialogdienste
├── economy/        # Wirtschaft und Währung
├── guild/          # Gilden
├── gui/            # bestehende GUI-Komponenten
├── item/           # Item-System
├── npc/            # NPC-Verwaltung und Verhalten
├── party/          # Party-System
├── player/         # Spielerprofile
├── profession/     # Berufe und Crafting
├── quest/          # Quest-System
├── story/          # Story-Kampagne und Story-Fortschritt
└── ...
```

Datengetriebene Inhalte liegen unter:

```text
src/main/resources/
├── data/
│   ├── quests/
│   ├── recipes/
│   ├── item-definitions.json
│   └── story_campaign.yml
├── config.yml
└── paper-plugin.yml
```

---

# Entwicklung

## Verbindliche Entwicklungsplattform

Neue Änderungen werden ausschließlich auf Basis von:

- Java 25
- Paper 26.2
- Paperweight Userdev 2.0.0-beta.21
- Dev Bundle 26.2.build.121-stable
- Mojang-Mappings

entwickelt.

Der Entwicklungsbranch ist:

`test`

Der Referenz-/Produktionsstand ist:

`main`

Änderungen werden auf `test` entwickelt und anschließend nach Prüfung nach `main` übernommen.

---

# Build & Verifikation

Das Projekt verwendet Gradle.

Der normale Build erzeugt:

`Pixel-RPG.jar`

Der normale `jar`-Task erzeugt zusätzlich eine schlanke Variante mit dem Classifier:

`slim`

### ShadowJar

Die Third-Party-Abhängigkeiten werden in das Plugin eingebettet und relocatet:

| Bibliothek | Relocation |
|---|---|
| Gson | `de.pixelrpg.rpg.libs.gson` |
| HikariCP | `de.pixelrpg.rpg.libs.hikari` |
| MySQL | `de.pixelrpg.rpg.libs.mysql` |

Im Java-Quellcode werden weiterhin die normalen Bibliothekspakete verwendet.

### Automatische Verifikation

Der Build enthält eigene Prüfungen gegen:

- Legacy-Bukkit-ChatColor
- Legacy-NMS-Pakete
- CraftBukkit
- verbotene statische Live-Server-Referenzen
- nicht korrekt relocatete Third-Party-Klassen
- falsche bzw. nicht relocatete MySQL-JDBC-Service-Einträge

Diese Prüfungen sind Bestandteil von `check` und sollen sicherstellen, dass die technische Architektur nicht versehentlich durch alte APIs oder falsche Packaging-Strukturen beschädigt wird.

---

# Grundsätze

PixelRPG wird nach einigen festen Grundsätzen entwickelt:

1. **Aktuelle Paper-26.x-APIs statt Legacy-Workarounds**
2. **Java 25 als verbindliche Laufzeit- und Buildbasis**
3. **Mojang-Mappings**
4. **Adventure Components für Text und Chat**
5. **Native Paper-Dialoge**
6. **Datengetriebener Content**
7. **Persistenter persönlicher Spielerfortschritt**
8. **Modulare Systeme statt monolithischer Gameplay-Logik**
9. **Bestehende Funktionalität wird bei Erweiterungen erhalten**
10. **Build- und Architekturprüfungen bleiben Bestandteil des Projekts**

---

## Aktueller Entwicklungsfokus

Der Kern des Plugins ist aufgebaut und miteinander verbunden. Die weitere Entwicklung konzentriert sich damit vor allem auf:

- zusätzlichen Content
- neue Quests und Story-Inhalte
- neue Items und Rezepte
- weitere Bosse
- Balancing
- QoL-Funktionen
- Fehlerbehebungen
- UI-/Dialog-Verbesserungen
- langfristige Erweiterungen des MMORPG-Systems

PixelRPG ist damit als zentrale RPG-Plattform des Pixel-Servers ausgelegt und kann schrittweise um weiteren Content erweitert werden, ohne die grundlegende Systemarchitektur neu aufbauen zu müssen.
