# PixelRPG Rebuild Audit

## 1. Zweck und verbindliche Arbeitsanweisung

Diese Datei ist die **verbindliche Arbeitsanweisung für den vollständigen Neuaufbau des Projekts auf dem Branch `rebuild`**.

Der Branch `main` ist der eingefrorene technische und funktionale **SOLL-/Referenzzustand**. Der Branch `test` ist der bisherige Entwicklungsstand. Der Branch `rebuild` ist ab diesem Audit der einzige Ort für den strukturellen Neuaufbau.

### Primäres Ziel

Das bestehende Serververhalten muss nach dem Neuaufbau funktional erhalten bleiben. Die interne Implementierung darf vollständig neu strukturiert werden.

Der Neuaufbau soll:

- fachliche Verantwortlichkeiten klar trennen;
- unnötige oder doppelte Klassen und Datenmodelle entfernen;
- gewachsene Übergangslösungen nicht blind übernehmen;
- I/O und teure Operationen vom Main Thread fernhalten;
- Paper-26.2- und Java-25-APIs konsequent verwenden;
- Persistenz, Lifecycle und Shutdown deterministisch gestalten;
- Content von Engine-/Infrastruktur-Code trennen;
- das native Paper/Minecraft-Dialog-System als Grundlage für zukünftige NPC-, Story- und Quest-Interaktionen verwenden;
- eine Architektur schaffen, auf der später Texte, Lore, Geschichte, Quests und weitere Content-Systeme erweitert werden können, ohne die technische Basis erneut umzubauen.

**Wichtig:** Nicht die Anzahl der Klassen minimieren. Ziel ist eine minimale, verständliche und fachlich begründete Architektur.

---

## 2. Unveränderliche technische Basis

Die folgenden Vorgaben dürfen beim Rebuild nicht verändert werden, sofern kein ausdrücklicher Auftrag dafür vorliegt:

- Paper 26.2
- Paper Dev Bundle: `26.2.build.121-stable`
- `io.papermc.paperweight.userdev` 2.0.0-beta.21
- Shadow 9.6.1
- Java 25
- Mojang-Mappings
- `paper-plugin.yml`
- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0
- Shadow-Relocations:
  - `com.google.gson` -> `de.pixelrpg.rpg.libs.gson`
  - `com.zaxxer.hikari` -> `de.pixelrpg.rpg.libs.hikari`
  - `com.mysql` -> `de.pixelrpg.rpg.libs.mysql`
- `Pixel-RPG.jar` als ShadowJar
- `slim` als Classifier des normalen JAR
- bestehende Build-Verifikationen
- `check` muss weiterhin sämtliche bestehenden Verifikationen ausführen.

Keine Legacy-Bukkit-/CraftBukkit-/Legacy-NMS-/Spigot-Mapping-Lösungen einführen.

---

## 3. Referenzzustand

Referenz-Commit von `main` zum Zeitpunkt dieses Audits:

`b02747a21cde6d1308c59a7f027cda80877cfe50`

`main` und `test` sind zu diesem Zeitpunkt identisch.

Die Repository-Forensik ergibt für den Referenzzustand:

- 321 Dateien insgesamt im Git-Tree
- 247 Java-Dateien
- umfangreiche Ressourcen-/Content-Struktur
- mehrere bereits getrennte fachliche Subsysteme

Der aktuelle `rebuild`-Branch wurde absichtlich nur mit der Build-/Entwicklungsgrundlage initialisiert:

- `.github/workflows/`
- `.vscode/`
- `build.gradle`
- `settings.gradle`

Es wurden noch keine fachlichen Java-/Ressourcen-Systeme in `rebuild` übernommen.

---

## 4. Grundregel für den Neuaufbau

### NICHT

- `src/main` einfach komplett aus `main` kopieren;
- Klassen nur umbenennen oder verschieben;
- bestehende Manager blind weiterverwenden;
- alte Dialog-Abstraktionen übernehmen, wenn native Paper-Dialoge die Aufgabe lösen;
- mehrere parallele Datenquellen für denselben fachlichen Zustand behalten;
- God-Manager erzeugen, die NPC, Quest, Dialog, Persistence und Content gleichzeitig verwalten;
- tote Klassen nur aus Angst vor Funktionsverlust behalten;
- bestehende technische Struktur als automatisch richtig behandeln.

### STATTDESSEN

Für jedes Subsystem:

1. Ist-Verhalten aus `main` erfassen.
2. Öffentliche/funktional relevante Schnittstellen identifizieren.
3. Datenflüsse und Persistenzpfade nachvollziehen.
4. Event-/Listener-Abhängigkeiten erfassen.
5. Main-/Async-Grenzen erfassen.
6. tatsächliche Verantwortlichkeiten der vorhandenen Klassen bestimmen.
7. KEEP / REBUILD / MERGE / REMOVE / REPLACE entscheiden.
8. neue Zielstruktur definieren.
9. neu implementieren.
10. Build prüfen.
11. Verhalten gegen `main` vergleichen.
12. erst danach zum nächsten abhängigen Subsystem weitergehen.

Keine Löschung darf ausschließlich aufgrund des Dateinamens erfolgen.

---

## 5. Festgestellte Architekturmerkmale des SOLL-Zustands

### Plugin-Lifecycle

`PixelRPGPlugin` übernimmt aktuell sehr viele Initialisierungsaufgaben und hält zahlreiche fachliche Services als Felder. Unter anderem werden Spielerprofile, Stats, Berufe, Items, Equipment, Shops, Story, Parties, Quests, Regionen, Bosse, Scoreboard, NPCs, Dialoge, Companions und weitere Listener/Tasks aus einer zentralen Klasse verdrahtet.

**Rebuild-Anweisung:**

Der Lifecycle muss zentral orchestriert bleiben, darf aber nicht selbst fachliche Logik enthalten. Initialisierung und Shutdown müssen über klar definierte Komponenten erfolgen. Abhängigkeiten müssen explizit sein. Direkte globale Rückgriffe auf `PixelRPGPlugin.getInstance()` sind im Domain-/Service-Code nach Möglichkeit zu entfernen und durch explizite Dependencies zu ersetzen.

### Lifecycle / Ressourcen

Es existiert bereits ein `LifecycleCoordinator`. Dieser Ansatz ist als Grundlage zu prüfen und nicht ohne Grund zu duplizieren.

Alle Executor, Scheduler, Datenbank-Pools und persistenten Services müssen einen eindeutigen Besitzer und einen eindeutigen Shutdown-Pfad haben.

### NPC

Der aktuelle NPC-Bestand umfasst u. a.:

- `NpcManager`
- `RPGNpc`
- `NpcBehaviorRegistry`
- mehrere konkrete NPC-Behaviors
- Chunk-/Interaction-/Look-Listener
- Skin-Resolver und externen Skin-Service.

Die aktuelle Skin-Persistenz speichert die aufgelöste Texture-Property inklusive Signatur. Das ist funktional relevant und muss im Rebuild erhalten bleiben.

**Rebuild-Anweisung:**

NPC-Domainzustand, Runtime-Entity-Verwaltung, Skin-Auflösung, Persistence und Interaktion müssen sauber getrennt werden. Ein einzelner God-Manager für alle Bereiche ist zu vermeiden.

Chunk Load/Unload, Entity-Tracking und Skin-Anwendung müssen einen klaren Lifecycle besitzen.

### Dialog

Der aktuelle Stand verwendet bereits Paper-Native-Dialog-APIs, insbesondere:

- `io.papermc.paper.dialog.Dialog`
- `DialogBase`
- `DialogType`
- `ActionButton`
- `DialogAction`
- native `Player.showDialog(...)`.

Zusätzlich existiert eine eigene höhere Dialog-/Tree-Schicht.

**Rebuild-Anweisung:**

Native Dialoge sind die verbindliche technische Grundlage. Es darf keine Legacy-1.21.x-Dialogimplementierung entstehen.

Die höhere Schicht darf nur fachliche Orchestrierung liefern: z. B. Dialogzustand, Bedingungen, Optionen, nächste Knoten und Aktionen. Sie darf nicht die native Dialog-API unnötig nachbauen.

Für zukünftigen Content müssen Dialogdefinitionen von technischer Dialogausführung getrennt werden.

### Quests

Die aktuelle Struktur verteilt Questfunktionalität über mehrere Klassen und Ressourcen. Im Java-Code existieren u. a.:

- `QuestManager`
- `QuestRepository`
- `QuestProgress`
- `QuestText`
- `QuestType`
- `QuestPassiveCheckTask`
- Quest-Listener und Quest-Kommandos.

Auch die Ressourcen enthalten mehrere Quest-Dateien, darunter:

- `quests_crafting_orders.json`
- `quests_additional.json`
- `quests_content_expansion_01.json`
- `quests_expansion_02.json`
- `quests_v2.json`
- `quests_world_expansion.json`.

**Rebuild-Anweisung:**

Diese Dateien dürfen nicht einfach zusammenkopiert werden.

Zuerst muss festgestellt werden, welche Dateien tatsächlich unterschiedliche fachliche Inhalte, Versionen oder Duplikate enthalten. Danach muss eine eindeutige Content-Struktur entstehen.

Quest-Domainmodell, Spieler-Quest-State, Questdefinitionen, Repository/Persistence, Progression, Trigger und UI/Dialog-Interaktion sind getrennt zu betrachten.

Das Ziel ist nicht zwingend eine einzelne Datei, sondern eine **fachlich eindeutige Quelle pro Verantwortungsbereich**.

### Story / Lore

Aktuell existieren u. a.:

- `StoryManager`
- `StoryChapter`
- `StoryBookFactory`
- `StoryNpcDialogue`
- Story-bezogene NPC-Strukturen.

Der aktuelle Story-Code enthält teilweise direkt eingebetteten Content.

**Rebuild-Anweisung:**

Story-/Lore-Content muss aus der technischen Engine herausgelöst werden. Java soll nicht der primäre Speicherort für umfangreiche Spieltexte sein.

Die spätere Content-Struktur muss Story, Kapitel, Dialoge, Lore und Voraussetzungen sauber modellieren können.

---

## 6. Persistenz

Aktuell existieren mehrere Persistenzformen:

- MySQL/Hikari
- YAML-Dateien
- JSON-Ressourcen für statischen Content
- NPC-YAML-Persistenz
- weitere fachliche Speichermechanismen.

Die vorhandene `DatabaseManager`-Implementierung besitzt Connection-Pooling und Schema-Migrationen.

**Rebuild-Anweisung:**

Vor jedem Neubau muss pro Datenbestand festgelegt werden:

- Ist es statischer Content?
- Ist es Server-Konfiguration?
- Ist es persistenter Runtime-State?
- Ist es Cache?
- Wer ist die Source of Truth?
- Wann wird gelesen?
- Wann wird geschrieben?
- Auf welchem Thread?
- Was passiert bei Shutdown?
- Was passiert bei Schreibfehlern?

Kein Zustand darf gleichzeitig von mehreren unabhängigen Quellen autoritativ verwaltet werden.

Datenbankzugriffe dürfen nicht im Main Thread stattfinden, sofern sie nicht ausdrücklich durch die aktuelle Paper-API erforderlich sind.

---

## 7. Performance-Regeln

Der Rebuild muss folgende Regeln erfüllen:

1. Paper-/World-/Entity-/Player-Operationen auf dem Serverthread.
2. Datenbank, Datei-I/O und externe HTTP-Anfragen außerhalb des Serverthreads.
3. Ergebnisse asynchron berechnen/laden und notwendige Serveränderungen anschließend gezielt auf den Serverthread zurückführen.
4. Keine unkontrollierten Scheduler-Kaskaden.
5. Keine permanent laufenden Tasks, wenn eventbasierte Ausführung genügt.
6. Keine unnötigen Polling-Schleifen.
7. Caches nur mit klarer Invalidierungsstrategie.
8. Keine globale Speicherung von Live-`Player`, `Entity` oder `World`-Objekten.
9. Snapshot-then-I/O bei Persistenz.
10. Shutdown muss laufende Persistenzarbeiten berücksichtigen.
11. Externe Services müssen Timeouts und Fehlerbehandlung besitzen.
12. Runtime-Registries müssen nur Daten halten, die tatsächlich für schnelle Runtime-Lookups benötigt werden.

Performanceoptimierung darf niemals dazu führen, dass Serverthread-Grenzen oder Datenkonsistenz verletzt werden.

---

## 8. Content-Architektur

Der Rebuild ist ausdrücklich die technische Grundlage für die kommende Content-Phase.

Die Architektur muss deshalb folgende Content-Arten unterstützen können, ohne erneuten Architekturumbau:

- NPC-Texte
- NPC-Geschichten
- Dialoge
- Dialogzweige
- Lore
- Kapitel
- Quests
- Questziele
- Questvoraussetzungen
- Questbelohnungen
- Folgequests
- Entscheidungen
- wiederkehrende Dialoge
- Zustandsabhängige Dialoge.

Technische Engine und Content müssen getrennt bleiben.

Content-IDs müssen stabil und eindeutig sein.

---

## 9. Dialog-/Quest-Zielarchitektur

Für die kommende Content-Phase soll der fachliche Ablauf grundsätzlich folgende Form ermöglichen:

`NPC -> native Dialog -> Spielerentscheidung -> fachliche Aktion -> Quest/Story-State -> nächster Dialog`

Die Dialogschicht darf nicht selbst Questregeln duplizieren.

Questregeln gehören in die Quest-Domain/Quest-Service-Schicht.

Storyfortschritt gehört in die Story-Schicht.

NPC-Entity-Lifecycle gehört in die NPC-Schicht.

Persistence gehört in die Persistence-Schicht.

Der Dialog orchestriert die Interaktion, besitzt aber nicht die fachliche Wahrheit aller Systeme.

---

## 10. Verifikation jedes Rebuild-Schrittes

Nach jedem größeren Subsystem:

### Statische Prüfung

- Java 25
- Paper 26.2
- keine Legacy-NMS
- kein CraftBukkit
- kein `ChatColor`
- keine statischen Live-Server-Referenzen
- nur Mojang-Mappings
- korrekte Third-Party-Relocations
- Build-Verifikationen unverändert funktionsfähig.

### Build

`gradle clean build --no-daemon --stacktrace`

Ein erfolgreicher Build ist notwendig, aber nicht ausreichend.

### Funktionalität

Der jeweilige Funktionsbereich muss gegen den aktuellen `main`-SOLLzustand geprüft werden.

Insbesondere:

- Serverstart
- Serverstop
- Reload-/Lifecycle-relevante Pfade
- Player Join/Quit
- Persistence
- NPC Spawn/Despawn
- Chunk Load/Unload
- NPC Skin Save/Load
- Dialoge
- Quests
- Story
- Regionen
- Berufe
- Items
- Companions
- Shops
- Bosse
- Statistiken.

---

## 11. Rebuild-Reihenfolge

Die Implementierung soll nicht nach Dateinamen, sondern nach Abhängigkeiten erfolgen:

1. Build-/Projektgrundlage
2. Plugin-Lifecycle / Core
3. zentrale Domain-/Runtime-Grundlagen
4. Persistence-Abstraktion und konkrete Speicher
5. Player/Profile
6. Items/Equipment/ökonomische Grundlagen
7. NPC Runtime + Persistence
8. native Dialog-Grundlage
9. Content-/Text-Grundlage
10. Quest-Domain + Quest-Persistence
11. Story/Lore
12. Regionen
13. Berufe/Crafting
14. Companions
15. Shops/Trading
16. Combat/Bosse/Stats
17. Commands/UI/administrative Werkzeuge
18. abschließende Integration und Forensik.

Die tatsächliche Reihenfolge darf angepasst werden, wenn die Codeanalyse eine andere Abhängigkeitsreihenfolge nachweist.

---

## 12. Akzeptanzkriterium

Der Rebuild ist erst dann abgeschlossen, wenn:

> **Das neue System das funktionale Verhalten des Referenzzustands reproduziert und gleichzeitig eine nachvollziehbare, wartbare und erweiterbare Architektur besitzt.**

Zusätzlich muss die neue Architektur für die nächste Phase geeignet sein:

> **Texte, Lore, Geschichte, Quests und native Dialoge sollen als Content erweitert werden können, ohne die technische Basis erneut grundlegend umzubauen.**

Keine Funktion darf stillschweigend entfallen, nur weil ihre alte Implementierung entfernt wurde.

Wenn eine Funktion bewusst anders umgesetzt wird, muss dokumentiert werden:

- altes Verhalten;
- neues Verhalten;
- Grund der Änderung;
- Nachweis der funktionalen Gleichwertigkeit.

---

## 13. Verbindliche Regel für weitere Agent-/Entwicklungsarbeit

Vor jeder Änderung auf `rebuild`:

1. diese `audit.md` lesen;
2. `main` als Referenz prüfen;
3. tatsächlichen Code prüfen;
4. Abhängigkeiten des betroffenen Systems prüfen;
5. Zielarchitektur für den betroffenen Bereich festlegen;
6. nur notwendige Dateien ändern;
7. keine funktionalen Bereiche ohne Auftrag verändern;
8. nach der Änderung bauen/verifizieren;
9. Audit bei neuen Erkenntnissen aktualisieren.

**Keine Ratespiele. Keine erfundenen APIs. Keine Legacy-Lösungen. Keine blinden Kopien aus `main`.**

Der Rebuild soll ein bewusst neu entworfenes System werden, dessen Verhalten aus dem Referenzzustand abgeleitet und dessen Architektur für die kommende Content-Entwicklung optimiert wird.


---

## 14. Rebuild-Fortschritt

### Phase 1 — Build-/Projektgrundlage

Status: **abgeschlossen**

Auf rebuild vorhanden und geprüft:

- verbindliche Gradle-/Paper-/Java-25-Basis;
- bestehende Shadow-Relocations und Build-Verifikationen;
- `paper-plugin.yml`;
- vollständige Runtime-Konfigurationsgrundlage aus dem Referenzzustand;
- zentraler `LifecycleCoordinator` als einziger Runtime-Resource-Owner der neuen Composition Root;
- minimale `PixelRPGPlugin`-Composition-Root ohne Übernahme der alten God-Manager-Verdrahtung;
- Paper-Bootstrap als separater Bootstrap-Einstiegspunkt;
- CI prüft rebuild zusätzlich zu den bestehenden Branches.

Bewusst noch nicht übernommen:

- fachliche Feature-Module;
- alte `PixelRPGPlugin`-Verdrahtung;
- alte statische/globalen Zugriffe;
- alte Persistenz- und Domainmodelle.

Nächster Schritt ist die forensische Rekonstruktion der zentralen Persistenz-/Player-Abhängigkeiten aus `main`, bevor Player/Profile und weitere Fachmodule neu implementiert werden.


---

## 15. Rebuild-Fortschritt

### Phase 2–5 — Core, Persistence und Player/Profile

Status: **implementiert, Build-Verifikation ausstehend**

Forensisch aus `main` geprüft und auf `rebuild` neu zusammengesetzt:

- `LifecycleCoordinator` bleibt der zentrale Resource-Owner.
- `DatabaseManager` bildet weiterhin Connection-Pooling und Schema-Migrationen ab.
- Das bestehende Schema inklusive `persistence_revision`, aktiver Quests, Statistiken und Equipment wurde funktional erhalten.
- `PlayerProfile` enthält weiterhin den vollständigen bisher persistierten Player-State: Registrierung, XP, Geld, Berufe, Rezepte, Wegpunkte, Story-Kapitel, Quests, Statistiken, Equipment, UI-Flags und Playtime.
- YAML- und MySQL-Persistenz bleiben als getrennte konkrete Repositories bestehen.
- Persistenz arbeitet mit Snapshots und asynchronem I/O.
- Player-Profile werden weiterhin vor dem Join geladen und beim Quit/Shutdown gespeichert.
- Die Profile-Revision bleibt Teil des Persistenzprotokolls; MySQL prüft Stale-Revisionen.
- Fehler im MySQL-Initialisierungspfad behalten den bisherigen YAML-Fallback.
- Player-Lifecycle und Profile-Persistenz sind von der Composition Root getrennt.
- Guild-/Economy-Schnittstellen und die vorhandenen Player-Events wurden als fachliche API-Grenze übernommen.

### Architekturentscheidungen

Bewusst **nicht** übernommen wurden:

- die alte zentrale `PixelRPGPlugin`-Verdrahtung;
- globale Plugin-Singletons als Dependency-Quelle;
- synchrone Player-Datei-I/O;
- fachliche Systeme außerhalb des Player-/Persistence-Scope;
- alte Feature-Manager, die erst in ihren jeweiligen Rebuild-Phasen untersucht werden.

### Verifikation

Die Quellstruktur enthält in diesem Schritt keine verbotenen Legacy-NMS-/CraftBukkit-/ChatColor-Referenzen und keine statischen Live-`Player`/`Entity`/`World`-Felder.

Ein vollständiger `gradle clean build --no-daemon --stacktrace` konnte über die verfügbare GitHub-Actions-Schnittstelle für den aktuellen `rebuild`-HEAD noch nicht als abgeschlossenes Ergebnis festgestellt werden. Daher wird dieser Schritt **nicht** als build-verifiziert markiert.

Nächster abhängiger Bereich ist **Items/Equipment/ökonomische Grundlagen**. Vor dessen Implementierung müssen die tatsächlichen Item-/Equipment-Abhängigkeiten aus `main` forensisch erfasst werden.


## 16. Rebuild-Fortschritt

### Phase 6 — Items / Equipment / ökonomische Grundlagen

Status: **implementiert; Code-Stand CI-verifiziert, Audit-Only-Commit durch externe Paper-Maven-Störung nicht erneut verifizierbar**

Forensisch aus `main` geprüft und für `rebuild` neu strukturiert:

- zentrale Item-API und Item-Domain mit stabilen Item-IDs, Kategorien, Raritäten und Item-Definitionen;
- zentrale `RPGKeys`-Instanz statt globalem Plugin-Singleton für Item-PDC-Schlüssel;
- JSON-Content als `data/`-Quelle für Itemdefinitionen, Food, Scaling, Equipment-Sets, Unique-State und Boss-Reward-Items;
- aktuelle Paper-Data-Component-API für Food/Consumable-Konfiguration;
- RPG-Item-Erzeugung mit PDC-Identität, Instanz-ID, Level, Gearscore und bestehenden Item-Statfeldern;
- Economy-Safety-Prüfung für handelbare Items;
- Unique-Item-State mit asynchroner Persistierung und eindeutigem Lifecycle-Besitzer;
- Soulbound-Operation als fachlich isolierter Service;
- persistentes Equipment bleibt im PlayerProfile und wird über einen separaten Equipment-Service beim Join/Respawn wiederhergestellt und beim Quit synchronisiert;
- physisches Gilden-Gold bleibt über eine eigene Currency-Factory und einen isolierten Pickup-Listener von der Profile-/Economy-API getrennt;
- bestehende Guild-/Economy-Services werden weiterhin über die PlayerProfile-Komponente bereitgestellt.

### Bewusst nicht übernommen

- alter `EquipmentService` mit direkter Abhängigkeit auf `StatEngine` und globalem `PixelRPGPlugin.getInstance()`;
- alte `WakeScheduler`-Abhängigkeit für Equipment-Refreshes;
- alte globale/static `RPGKeys.init(...)`-Initialisierung;
- weitere Stats-/Combat-Logik, da diese gemäß Abhängigkeitsreihenfolge erst im Stats-/Combat-Rebuild behandelt wird.

### Verifikation

- Die neuen Item-/Equipment-/Economy-Klassen verwenden keine CraftBukkit- oder Legacy-NMS-Klassen.
- Es werden keine statischen Live-`Player`/`Entity`/`World`-Referenzen eingeführt.
- Listener besitzen Zweckkommentare direkt über jedem `@EventHandler`.
- Paper-26.2-Data-Component-APIs werden für die Food-Konfiguration verwendet.
- GitHub Actions Build Run `35865272470` für den vollständigen Code-Stand `29f3e3fc4e171a46182313ef33d576f80239afcc` wurde erfolgreich abgeschlossen. `Build PixelRPG`, `Verify source API boundaries` und `Verify plugin artifact` meldeten jeweils `success`. Der anschließende reine Audit-Dokumentations-Commit `49665081859ed36755c23368c6e8311f1159e19c` wurde zweimal gegen GitHub Actions geprüft; beide Läufe scheiterten bereits in `paperweightUserdevSetup`, weil `io.papermc.codebook:codebook-cli:2.0.1-SNAPSHOT` vom Paper-Maven-Repository mit HTTP 502/503 nicht abrufbar war. Das ist ein externer Repository-/Infrastrukturfehler und kein im Buildlog erkennbarer Quellcodefehler.

Nächster abhängiger Bereich ist **NPC Runtime + Persistence**. Vor dessen Implementierung sind NPC-Domainzustand, Runtime-Entity-Lifecycle, Skin-Persistenz und Chunk-Lifecycle aus `main` forensisch zu erfassen.


## 17. Rebuild-Fortschritt

### Phase 7 — NPC Runtime + Persistence

Status: **implementiert; ein CI-Kompilierfehler wurde anhand des Buildlogs korrigiert, erneute CI-Verifikation läuft**

Forensisch gegen den NPC-Bestand von `main` aufgebaut und nicht als Blindkopie übernommen:

- NPC-Domainzustand mit stabiler ID, Typ, Name, Position, Skin-Quelle und optionalem Beruf;
- getrennte Persistence-Abstraktion mit YAML-Repository;
- NPC-YAML-Lesen vollständig außerhalb des Serverthreads;
- Snapshot-basierte, asynchrone NPC-Persistierung mit temporärer Datei und atomarem Replace;
- deterministische Persistence-Kette und Shutdown-Flush;
- Runtime-Entity-Tracking über NPC-ID <-> Entity-UUID;
- Chunk-Index für gezieltes Spawn/Despawn-Verhalten;
- NPC-Mannequins werden nur als Runtime-Entities geführt und nicht als persistente Minecraft-Entities gespeichert;
- aktuelle Paper-Mannequin-/ResolvableProfile-basierte Skin-Anwendung;
- aufgelöste Texture-Property inklusive Signatur wird exakt persistiert und nach Neustart wiederverwendet;
- Player-Skin-Auflösung und externe Skin-Quellen bleiben asynchron;
- externe Skin-URLs besitzen Größen-, Redirect- und Private-Network-Schutz;
- NPC-PDC-Identität verwendet die neue instanzgebundene `RPGKeys`-Quelle;
- Chunk-Load/Unload und Player-Join sind über einen eigenen Listener vom Runtime-Manager getrennt;
- NPC-Schadensschutz ist als eigener Listener isoliert;
- globale `PixelRPGPlugin.getInstance()`-/Plugin-Manager-Rückgriffe wurden im neuen NPC-Core nicht übernommen;
- NPC-Ressourcen besitzen einen eindeutigen Lifecycle-Besitzer über `LifecycleCoordinator`.

### Bewusst noch nicht übernommen

Die alten NPC-Behaviors, Quest-/Shop-/Story-/Travel-Interaktionen und die alte Look-/Nameplate-Speziallogik wurden nicht vorgezogen. Sie hängen fachlich von späteren Rebuild-Phasen ab und werden erst zusammen mit ihrer tatsächlichen Domain-/Dialogabhängigkeit neu aufgebaut. Dadurch wird keine unfertige Quest-/Dialogarchitektur in den NPC-Core eingebaut.

### Skin-Persistenz — funktionale Zielerhaltung

Der Referenzzustand speichert die aufgelöste Texture-Property inklusive Signatur in `npcs.yml`. Der Rebuild übernimmt genau diesen Persistenzvertrag: Die Property wird unmittelbar nach erfolgreicher Auflösung aus dem Resolver übernommen und nicht nachträglich aus einem veränderlichen Runtime-Profil rekonstruiert. Beim Neustart wird die gespeicherte Property vor einer erneuten externen Auflösung verwendet.

### Verifikation

- Neue NPC-Klassen verwenden keine Legacy-NMS- oder CraftBukkit-Pakete.
- Keine statischen Live-`Player`/`Entity`/`World`-Referenzen wurden eingeführt.
- Listener besitzen Zweckkommentare direkt über jedem `@EventHandler`.
- Datei-I/O und externe HTTP-Auflösung liegen außerhalb des Serverthreads.
- Der erste vollständige CI-Lauf des NPC-Stands erreichte `compileJava` und meldete genau einen Fehler in der Berufsermittlung (`String` statt `int` bei `substring`). Dieser Fehler wurde direkt anhand des Logs korrigiert. Der daraus resultierende Stand muss erneut vollständig durch `clean build`, Source-Boundary- und Artifact-Verifikation laufen.

Nächster abhängiger Bereich ist **native Dialog-Grundlage**. NPC-Interaktion wird dort fachlich an die native Paper-Dialogschicht angebunden; Quest-/Story-Regeln bleiben in ihren späteren Domain-Schichten.


## 18. Rebuild-Fortschritt

### Phase 8 — Native Dialog-Grundlage

Status: **implementiert; CI-Verifikation läuft**

Forensisch gegen den bestehenden Dialog-Bestand von `main` geprüft und auf `rebuild` als eigenständige technische Grundlage neu aufgebaut:

- native Paper-Dialog-Erzeugung über `io.papermc.paper.dialog.Dialog`, `DialogBase`, `DialogType`, `ActionButton`, `DialogAction` und `Player.showDialog(...)`;
- keine Legacy-1.21.x-Dialogmechanik und keine eigene Nachbildung des Minecraft-Dialogprotokolls;
- technische Dialogausführung in `DialogueEngine` gekapselt;
- fachliche Dialogstruktur über `DialogueTree`, `DialogueNode`, `DialogueOption` und `DialogueCondition` getrennt;
- Dialogoptionen können Bedingungen, fachliche Aktionen und Folge-Nodes besitzen, ohne Quest-/Story-Regeln im Dialogsystem zu duplizieren;
- native Dialoge unterstützen Notice-, Multi-Action-, Confirmation- und Input-Aktionen;
- per-Spieler Dialogfortschritt für gesehen/abgeschlossen wird separat persistiert;
- Dialogfortschritt wird asynchron geladen und gespeichert;
- Persistenz verwendet Snapshot-then-I/O sowie temporäre Datei mit atomarem Replace-Fallback;
- Shutdown wartet auf laufenden Dialog-I/O;
- NPC-Interaktion ist über einen eigenen Listener an registrierte Dialogbäume angebunden: `npc:<npc-id>`;
- NPC-Runtime und Dialogsystem bleiben getrennt; der Dialoglistener kennt nur NPC-Laufzeitauflösung und Dialogservice;
- noch nicht vorhandene Content-Dialoge werden nicht künstlich erzeugt. Dadurch bleiben Quest-, Story-, Berufs-, Shop- und Reise-Regeln bis zu ihren jeweiligen Rebuild-Phasen außerhalb der technischen Dialoggrundlage.

### Verifikation

Der Stand muss nach dem Dialog-Rebuild erneut vollständig über `gradle clean build --no-daemon --stacktrace`, Source-Boundary-Checks und Artifact-Verifikation laufen. Erst nach erfolgreichem CI-Lauf wird Phase 8 als build-verifiziert markiert.

Nächster abhängiger Bereich ist **Content-/Text-Grundlage**. Dabei werden stabile Content-IDs und externe Text-/Dialogdefinitionen aufgebaut, bevor Quest-/Story-Logik darauf aufsetzt.


### Phase 9 — Content-/Text-Grundlage

Status: **implementiert; CI-Verifikation ausstehend**

Forensisch gegen den tatsächlichen Content-Bestand von `main` geprüft. Dabei wurde festgestellt, dass insbesondere Quest-Content über mehrere JSON-Dateien verteilt ist und Story-Text im alten Stand teilweise direkt in Java/YAML verarbeitet wird. Es wurde deshalb **nicht** blind Content zusammenkopiert.

Auf `rebuild` wurde stattdessen die technische Content-Grundlage geschaffen:

- stabile `ContentId` mit validiertem, versionsunabhängigem ID-Format;
- immutable `TextContent` als fachlicher Textdatensatz;
- `ContentCatalog` als unveränderlicher Runtime-Snapshot;
- asynchroner JSON-Loader für externen Text-Content;
- `ContentCatalogService` als Lifecycle-besitzender Runtime-Zugriff;
- separater `PixelRPG-ContentIO`-Executor;
- Snapshot-Austausch über `AtomicReference`, sodass Runtime-Lookups keinen Datei-I/O benötigen;
- initiale Content-Datei `data/content/texts.json` mit expliziter Schema-Version;
- Content-Datei wird beim ersten Start in den Plugin-Datenordner kopiert;
- fehlerhafte Content-Daten führen zu einem kontrollierten Ladefehler statt zu einem teilweise geladenen Katalog;
- Content-I/O liegt vollständig außerhalb des Serverthreads;
- Content-Service besitzt einen eindeutigen Shutdown-Pfad.

Bewusst **nicht** übernommen wurden die sechs vorhandenen Quest-Dateien als vermeintlich bereits konsolidierte Textquelle. Vor Quest-Rebuild muss deren fachlicher Inhalt dedupliziert und auf stabile Content-IDs abgebildet werden. Ebenso werden Story-/NPC-Texte erst in der Content-Phase migriert, nachdem ihre tatsächliche Verwendung aus `main` vollständig erfasst ist.

Damit ist die technische Grundlage vorhanden, ohne bereits fachliche Quest-/Story-Regeln vorwegzunehmen.

### Verifikation

Nach Phase 9 ist ein vollständiger `gradle clean build --no-daemon --stacktrace` inklusive bestehender Source-/Artifact-Verifikationen erforderlich. Die Phase wird erst nach erfolgreichem CI-Lauf als build-verifiziert markiert.

Nächster abhängiger Bereich ist **Quest-Domain + Quest-Persistence**. Vor dessen Implementierung müssen die sechs vorhandenen Quest-JSON-Dateien hinsichtlich IDs, Duplikaten, Versionen, Voraussetzungen, Rewards und Content-Überschneidungen vollständig normalisiert werden.


### Phase 10 — Quest-Domain + Quest-Persistence

Status: **implementiert; CI-verifiziert**

Die Quest-Struktur von `main` wurde vor der Übernahme forensisch ausgewertet. Der tatsächlich aktive alte Loader verwendet vier Dateien:

- `quests_v2.json`
- `quests_additional.json`
- `quests_world_expansion.json`
- `quests_expansion_02.json`

Diese enthalten zusammen **163 eindeutige Quest-Definitionen**:

- 113 `COLLECT`
- 49 `HUNT`
- 1 `GLOBAL_EVENT`

Zusätzlich existieren im Referenzzustand `quests_crafting_orders.json` mit 60 `craft_order_*`-Definitionen und `quests_content_expansion_01.json` mit 46 Definitionen. Beide wurden bewusst **nicht** in die aktive Quelle übernommen, weil der alte Runtime-Loader sie ebenfalls nicht als reguläre Questquelle verwendet. Insbesondere die Crafting-Orders waren im alten Code explizit als veralteter/entfernter Recipe-Datenbestand ausgeschlossen.

Die vier tatsächlich geladenen Quellen wurden zu einer eindeutigen kanonischen Runtime-Quelle normalisiert:

- `data/quests/definitions.json`
- 163 eindeutige IDs
- keine doppelten IDs
- keine fehlenden referenzierten Prerequisites
- keine fehlenden Follow-up-Quest-IDs
- Prerequisite-Zyklen werden beim Laden abgelehnt.

Neu aufgebaut wurden:

- `QuestDefinition` als immutable fachliche Definition;
- `QuestReward` für Geld, Erfahrung, Item-Rewards und Companion-Rewards;
- `QuestNavigation` für Struktur-/Biome-Navigation;
- asynchron ladendes `QuestRepository`;
- `QuestService` als fachliche Runtime-Grenze für Start, Abbruch, Progress und Abschluss;
- `QuestCompletedEvent` als entkoppelte Integrationsschnittstelle;
- bestehender persistenter Quest-State in `PlayerProfile`/Profile-Persistenz bleibt die Source of Truth.

Das Quest-Repository führt **keine eigene zweite Persistenz des Spieler-Quest-States** ein. Aktive Quests, Fortschritt, Ablaufzeit und abgeschlossene Quests bleiben Teil des bestehenden PlayerProfile-Snapshots und damit Teil der bereits vorhandenen YAML-/MySQL-Persistenz.

Quest-Datei-I/O erfolgt ausschließlich über einen eigenen `PixelRPG-QuestIO`-Executor. Die Definitionsdatei wird beim ersten Start aus den Plugin-Ressourcen in den Datenordner kopiert.

Der neue Service erzwingt bereits:

- maximal 5 aktive Quests;
- Level-Voraussetzungen;
- abgeschlossene Voraussetzungen;
- Berufszugehörigkeit und Berufslevel;
- keine bereits aktiven/abgeschlossenen Quests;
- Ablaufzeit bei zeitlich begrenzten Quests;
- Fortschrittsbegrenzung auf das Quest-Ziel;
- atomaren Wechsel von aktiv zu abgeschlossen auf dem PlayerProfile.

Bewusst noch **nicht** aus `main` übernommen:

- alte Quest-GUIs;
- alte Polling-/Passive-Check-Tasks;
- alte Event-Listener für Mob-Kills/Inventar/Navigation;
- Reward-Ausführung für konkrete Item-/Companion-Systeme;
- NPC-/Dialog-Integration.

Diese Teile sind abhängige Integrationsschritte. Die fachliche Quest-Wahrheit liegt jetzt bereits getrennt von UI, NPC Runtime und Dialogtechnik.

### Verifikation

Die kanonische Quelle wurde vor Commit programmatisch auf **163 eindeutige IDs und keine fehlenden Prerequisite-/Follow-up-Referenzen** geprüft.

Der vollständige `gradle clean build --no-daemon --stacktrace` lief auf Commit `b3a464b0b517edac084214910367435ed65850f2` erfolgreich durch. Zusätzlich waren die bestehenden Source-API-Grenzprüfungen und die Plugin-Artefaktprüfung erfolgreich. Ein vorheriger Lauf scheiterte ausschließlich an konkret festgestellten Compile-Fehlern (`QuestType` fehlte, `QuestRepository` war nicht `AutoCloseable`, bestehender `NpcRuntimeManager`-Substring-Aufruf war syntaktisch falsch); diese wurden gezielt korrigiert und anschließend erfolgreich verifiziert.

Nächster abhängiger Bereich ist **Story/Lore**. Dabei müssen `StoryManager`, `StoryChapter`, `StoryBookFactory` und `StoryNpcDialogue` gegen die neue Content-/Dialog-Grundlage forensisch aufgelöst werden, ohne Quest-Regeln in die Dialogschicht zu verschieben.
