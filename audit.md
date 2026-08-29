# 🕵️ Forensischer 100-%-Audit: PixelRPG

**Repository:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `refactor/central-content-pipeline-v3`  
**Audit-Stand:** 29.08.2026  
**Aktueller Branch-Commit:** `27c6a7bd678b34b1741dd17f4d48d07f06e7ea77`  
**Letzter Commit:** `docs: document PixelRPG region system idea`  
**Historische Referenz:** `9fadeadefba378361722a16fad66be7f62a70560`

> Dieser Bericht ist der forensische IST-Bericht des Branches. Er beschreibt, was tatsächlich vorhanden ist, was bereits praktisch geprüft wurde, welche offenen Befunde bestehen und was vor bzw. nach dem Beta-Freeze noch auf den Entwickler zukommt.

---

# 1. Prüfgrundlage und Vorgehen

Die Prüfung erfolgte direkt gegen den Branch `refactor/central-content-pipeline-v3` und dessen tatsächlichen Git-Tree. Der aktuelle Tree ist vollständig aufgelöst (`truncated=false`). Er umfasst Projektwurzel, CI, Gradle-Konfiguration, `src/main/java`, Ressourcen und JSON-Daten.

Es wurde **kein GitHub-DeepSearch** als Grundlage des Audits verwendet. Die Prüfung orientiert sich am tatsächlichen Dateibestand und an den konkreten Modul-/Dateigrenzen. Die technische Bewertung wurde mit den vorhandenen Projektdefinitionen, der Roadmap, dem bestehenden Audit sowie den vom Entwickler gemeldeten realen Runtime-Tests abgeglichen.

Wichtig für die Interpretation:

- **statisch geprüft** = aus vorhandenem Code/Tree ableitbar
- **praktisch geprüft** = vom Entwickler im echten Serverlauf getestet
- **offen** = noch nicht durch einen vollständigen Runtime-/Regressionstest abgesichert
- **kein Befund** = im geprüften Bestand kein entsprechender Fehler festgestellt

Der aktuelle letzte Commit verändert ausschließlich die Dokumentation des späteren Region-Systems; die Implementierung des RPG-Kerns wurde dadurch nicht verändert.

---

# 2. Aktueller Repository-Bestand

Der Branch enthält die folgenden wesentlichen Bereiche:

- Bootstrap / Plugin-Lifecycle
- öffentliche API und Events
- Player/Profile/Persistenz
- Level/XP/Core
- Stats
- Combat
- Weapon Skills
- Mob Scaling
- Loot
- Items
- Equipment
- Quests
- NPC-System
- Dialog-System
- Quick Actions / Charakterkarte
- Professionen
- Crafting
- Companions
- Boss-System
- Party
- Guild
- Economy / Guild Currency
- Shop
- Trade Depot
- Travel
- Scoreboard
- Story
- JSON-Daten
- YAML-Konfiguration
- MySQL-/YAML-Persistenz
- Gradle-Build
- GitHub Actions
- `paper-plugin.yml`

Die Java-Struktur ist modular und besitzt getrennte Services, Repositories, Registries, Listener, Controller und Datenmodelle.

---

# 3. Plattform / Build-Konfiguration

## Verbindliche Zielplattform

Der Branch verwendet:

- Java 25
- Paper 26.2 Dev Bundle
- `io.papermc.paperweight.userdev` `2.0.0-beta.21`
- Shadow `9.6.1`
- Gson `2.13.1`
- HikariCP `7.0.2`
- MySQL Connector/J `9.7.0`

`build.gradle` verwendet `paperweight.paperDevBundle("26.2.build.+")`, Java 25 und baut mit Shadow das Artefakt `Pixel-RPG.jar`. Hikari, MySQL und Gson werden für das Endartefakt relocated. fileciteturn361file0L2-L5

## Paper Plugin

`paper-plugin.yml` ist vorhanden und definiert:

- `name: PixelRPG`
- `main: de.pixelrpg.rpg.PixelRPGPlugin`
- `bootstrapper: de.pixelrpg.rpg.PixelRPGBootstrap`
- `api-version: '26.2'`
- `rpg.admin`
- `rpg.member`

Damit ist der Branch auf die verbindliche Paper-26.2-Plattform ausgerichtet. fileciteturn362file0L2-L6

---

# 4. Build / GitHub Actions

`build.yml` läuft bei Push auf `main` und `refactor/central-content-pipeline-v3` sowie bei Pull Requests.

Der Workflow:

1. checkt das Repository aus,
2. setzt Temurin Java 25,
3. verwendet Gradle 9.2.0,
4. führt `gradle build --no-daemon --stacktrace` aus.

Der Workflow ist damit grundsätzlich auf die Projektvorgaben abgestimmt. fileciteturn360file0L2-L6

**Aktueller forensischer Status:** Der Branch besitzt einen funktionierenden Build-Pfad laut Roadmap und bisherigem Audit. Für den letzten Dokumentationscommit existiert kein neuer Pull-Request-Workflow-Lauf; der Commit selbst enthält ausschließlich Dokumentation. Deshalb wird kein nicht vorhandener CI-Lauf als „grün“ erfunden.

---

# 5. Bootstrap / Plugin-Lifecycle

Vorhanden:

- `PixelRPGBootstrap`
- `PixelRPGPlugin`
- zentraler Service-Lifecycle
- Registrierung der Listener/Services/Tasks
- Shutdown-Pfade

Die Plugin-Hauptklasse verbindet PlayerProfile, Stats, Professionen, Items, Equipment, Crafting, Quests, Bosse, NPCs, Dialoge, Party, Story, Companions, Scoreboard und weitere Komponenten.

**Status:** strukturell vorhanden und im echten Serverstart bereits erfolgreich angelaufen.

**Runtime-Befund:** Serverstart ohne Fehler; keine Runtime-Exception im getesteten Lauf.

---

# 6. Player / Profil / Registrierung / Progression

Vorhanden:

- `PlayerProfile`
- `PlayerProfileManager`
- MySQL-Repository
- YAML-Repository
- Lifecycle Listener
- Registrierungs-/Unregistrierungs-Events
- Level/XP

Die Projektdefinition sieht einen opt-in RPG-Layer vor: Ein neuer Spieler kann zunächst wie ein normaler Vanilla-Spieler behandelt werden und wird erst nach Registrierung vollständig in die RPG-Systeme aufgenommen.

**Status:** technisch vorhanden.

**Praktischer Befund:** Registrierung/Profil wurde im getesteten Lauf funktional geprüft.

**Offen:** Vollständiger Save/Reload/Restart-Test über alle Persistenzbereiche ist noch nicht abgeschlossen.

---

# 7. API / Events

Vorhanden sind unter anderem:

- `PixelRPGProvider`
- `EconomyAPI`
- `GuildAPI`
- `ItemAPI`
- `PartyAPI`
- `StatisticsAPI`
- `CharacterStatType`
- `ApiVersion`
- Boss-/Combat-/Level-/Quest-/Registration-Events

Damit existiert bereits eine öffentliche interne API-Schicht für zentrale RPG-Funktionen.

**Status:** implementiert.

---

# 8. Core / Level / Stats

Vorhanden:

- Level-/XP-System
- RPG Keys
- Statistic Types
- Stat Engine
- Statistics Service
- Listener für Mob-Kills, Tode, Quest/Boss-Ereignisse

Aktuelle RPG-Stats umfassen unter anderem:

- HP
- Armor
- Movement Speed
- Reach
- Damage
- Crit
- Crit Damage
- Lifesteal
- Attack Power

Es gibt laut Projektdefinition keine Klassen und keine frei verteilbaren Player-Attribute.

**Status:** technisch implementiert.

**Runtime:** noch kein vollständiger End-to-End-Regressionsdurchlauf sämtlicher Stat-Berechnungen abgeschlossen.

---

# 9. Combat / Skills / Scaling / Loot

Vorhanden:

- zentrale Schadensberechnung
- Damage Context
- Combat Listener
- Combat State
- Mob XP
- Soulbound Death Handling
- Weapon Ability Engine
- Skill Input Listener
- Mob Level Scaling
- Mob Nameplates
- Loot Drop Listener

Die Combat-Struktur ist bereits in getrennte Verantwortlichkeiten aufgeteilt.

**Status:** implementiert.

**Offen:** Vollständiger praktischer Regressionstest aller Kombinationen aus Spielerlevel, Moblevel, Ausrüstung, Skills, Crit, Lifesteal, Loot und Tod steht noch aus.

---

# 10. Items / Equipment

Vorhanden:

- `ItemDefinition`
- `ItemDefinitionRegistry`
- `ItemService`
- `RPGItemBuilder`
- `ItemRarity`
- `ItemCategory`
- Gear Categories
- Item Economy
- Soulbound
- Unique Items
- Equipment Service
- Equipment Sets
- Equipment Slots

Die Item-Identität wird nicht allein über das Vanilla-Material bestimmt, sondern über zusätzliche PixelRPG-Metadaten.

Raritäten:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

**Praktischer Befund:** Items wurden getestet und funktionieren grundsätzlich.

**Offener sichtbarer Befund:** Im nativen `minecraft:quick_actions`-Charakterprofil sind teilweise noch englische Item-Bezeichnungen sichtbar.

**Folge:** Kein Kern-Item-Systemfehler, sondern ein Deutsch-only-UI-/Textbefund.

---

# 11. Quests

Vorhanden:

- Quest Model
- Quest Manager
- Quest Repository
- Quest Progress
- Quest Text
- Quest Navigation
- passive Checks
- Mob-Kill Listener
- globale Event-Verarbeitung
- Quest Inventory Tracking
- mehrere datengetriebene Quest-Dateien

Datenquellen:

- `quests_v2.json`
- `quests_additional.json`
- `quests_content_expansion_01.json`
- `quests_world_expansion.json`

**Praktischer Befund:** Quests wurden getestet; keine vom Entwickler festgestellten Funktionsfehler.

**Status:** funktionsfähig, aber weitere Balancing-/QOL-Anpassungen können während der Qualitätsphase sinnvoll sein. Das zählt nicht als neue Implementierung, solange das bestehende Questkonzept erhalten bleibt.

---

# 12. NPC-System

Vorhanden:

- `NpcManager`
- `RPGNpc`
- `NpcType`
- `NpcBehaviorRegistry`
- `NpcInteractListener`
- `NpcChunkListener`
- `NpcLookTask`
- Skin Resolver
- mehrere konkrete Behaviors

Behaviors umfassen:

- Reception
- Quest
- Shop
- Travel
- Story
- Banker
- Filler
- Profession Trainer

**Praktischer Befund:** Alle vorhandenen NPCs wurden vom Entwickler durchgetestet. NPC-Interaktionen und Ausgaben funktionieren.

### Bekannter früherer Befund: Profession Trainer

Die vier Handwerks-NPCs hatten zunächst falsche Rezeptdarstellung bzw. zeigten nicht sauber nur den eigenen Beruf. Die Ursache wurde identifiziert: Die Trainerlogik öffnete in einem Pfad die allgemeine Profession-Ansicht statt die berufsspezifische Traineransicht.

Die bestehende Implementierung wurde daraufhin korrigiert.

**Status:** Codekorrektur vorhanden.  
**Offen:** abschließender Runtime-Nachtest aller vier Berufslehrer nach der Korrektur.

---

# 13. Dialogue-System

Vorhanden:

- `DialogueEngine`
- `DialogueTree`
- `DialogueNode`
- `DialogueOption`
- `DialogueCondition`
- `DialogueCommand`
- Progress Store
- Dialogue Tree Service
- spezialisierte Dialoge für Bank, Begleiter, Gilde, Profession, Reception, Travel, Story und Quick Actions

**Praktischer Befund:** Dialogue-Texte wurden getestet und funktionieren.

**Story-NPC:** Der aktuelle Story-NPC/Mannequin mit Buch ist als vorhandenes Story-Element erkennbar. Die weitergehende Überarbeitung der Story ist konzeptionell geplant, aber kein notwendiger technischer Neubau des Dialogue-Systems.

---

# 14. Quick Actions / Charakterprofil

Vorhanden:

- native `minecraft:quick_actions`
- Quick Actions Service
- Quick Actions Listener
- Charakterprofil
- aktive Quests
- Begleiter
- Berufe
- Gilde

**Praktischer Befund:** Das System funktioniert.

**Offene sichtbare Fehler:**

1. Stat-Namen werden teilweise noch Englisch dargestellt.
2. Item-Namen werden teilweise noch Englisch dargestellt.

Diese beiden Punkte sind aktuell die klarsten noch sichtbaren Reste der Deutsch-only-Umstellung.

---

# 15. Companions / Begleiter

Intern bleibt die technische Bezeichnung **`Companion`** bestehen.

Vorhanden:

- Companion Definition
- Companion Registry
- Runtime Registry
- Service
- Instance
- Stats
- Stats Calculator
- Progression
- Rarity
- Equipment
- Equipment Store
- Equipment Listener
- Follow Task
- Combat Controller
- Mount Controller
- Mount Listener
- Boss Reward Listener
- Experience Listener
- Mannequin Companion Controller

Datenquelle:

- `companions.json`

**Sichtbarer Ingame-Begriff:** `Begleiter`.

**Praktischer Befund:** Begleiter wurden getestet und zeigten im getesteten Lauf keine Auffälligkeiten.

**Status:** technisch umfangreich implementiert und grundsätzlich funktionsfähig.

---

# 16. Professionen / Crafting

Die vorhandenen Berufe sind:

- Blacksmith
- Provisioner
- Alchemist
- Scholar

Vorhanden:

- Profession Enum/Model
- Profession Service
- Profession System
- Profession Activity Listener
- Craft Recipe
- Crafting Recipe Registry
- Crafting Service
- Rarity Roller
- Profession Dialog
- Profession Trainer Behavior
- zentrale `crafting-recipes.json`

Die Rezeptdatei ist umfangreich und datengetrieben.

**Bekannter Befund:** Die Trainer-/Rezeptanzeige war fehlerhaft und wurde im Code korrigiert.

**Noch offen:** Die vier Berufslehrer müssen nach der Korrektur jeweils einzeln vollständig im Runtime-Test geprüft werden. Dabei ist sicherzustellen, dass jeder NPC ausschließlich seinen eigenen Beruf bzw. dessen Rezepte anbietet.

---

# 17. Boss-System

Vorhanden:

- `BossDefinition`
- `BossKind`
- `BossPhase`
- `ActiveBoss`
- `BossManager`
- `BossRepository`
- Attack Pattern Registry
- mehrere konkrete Attack Patterns
- BossBar
- Damage Contribution
- Death Handling
- Combust Handling
- Reward Item Listener
- World Boss Protection
- datengetriebene Boss Rewards

Vorhandene Pattern-Beispiele:

- Enrage Buff
- Projectile Volley
- Slam Attack
- Summon Adds

Es gibt eine Trennung zwischen Biome-Bossen und World-Bossen.

**Status:** umfangreich implementiert.

**Offen:** vollständiger praktischer Runtime-Test aller Bossarten, Spawnbedingungen, Phasen, Patterns, Contribution, Loot und Companion-Rewards.

---

# 18. Party-System

Vorhanden:

- Party Model
- Party Manager
- Disconnect Handling
- Party API

Die Party besitzt gemeinsame Mechaniken für XP/Loot/Quests sowie Mitglieder-/Einladungs-/Leadership-Logik.

**Status:** implementiert.

**Offen:** vollständiger Runtime-Regressionsdurchlauf.

---

# 19. Guild-System

Vorhanden:

- Guild Model
- Guild Manager
- Guild API
- Guild Bank
- Guild Bank Holder
- Guild Bank Listener
- Guild Bank Service
- Guild Bank Storage
- Guild Compass
- Gildenmitglieder
- Gildenmeister
- Einladungen
- Gildenbasis/-stadt

Das technische Spielergilden-System existiert bereits.

**Wichtig:** Das spätere freie Polygon-Region-System ist **nicht** implementiert und gehört ausdrücklich nicht zum aktuellen Beta-Freeze.

**Offen:** vollständiger praktischer Test der vorhandenen Gildenfunktionen.

---

# 20. Economy / Shop / Trade / Travel

Vorhanden:

### Economy
- Guild Currency Factory
- Pickup Listener

### Shop
- Shop Entry
- Shop Manager
- Shop GUI
- Shop Editor GUI

### Trade
- Trade Depot Listing
- Trade Depot Manager
- Trade Depot GUI
- Sell GUI

### Travel
- Guild Compass
- Travel Listener
- Travel Dialog

**Status:** technisch vorhanden.

**Offen:** vollständige Runtime-Regression dieser Bereiche.

---

# 21. Scoreboard / Spielzeit

Vorhanden:

- `ScoreboardService`
- `PlaytimeTracker`
- Charakterkarten-/Scoreboard-Anbindung

**Status:** implementiert.

**Offen:** vollständiger praktischer Regressionstest.

---

# 22. Story-System

Vorhanden:

- `StoryManager`
- `StoryChapter`
- `StoryBookFactory`
- Story-NPC-Dialog

Die Architektur ist ausreichend, um Story-Inhalte schrittweise über vorhandene NPCs/Dialoge/Storybooks zu erzählen.

**Status:** technisch vorhanden.

**Offen:** vollständiger Runtime-Test des gesamten Story-Flows.

---

# 23. Datengetriebener Content

Vorhandene JSON-Dateien:

- `boss-reward-items.json`
- `companions.json`
- `crafting-recipes.json`
- `equipment-sets.json`
- `item-definitions.json`
- `item-scaling.json`
- `mob-scaling.json`
- `quests_additional.json`
- `quests_content_expansion_01.json`
- `quests_v2.json`
- `quests_world_expansion.json`
- `unique-items.json`

Der Content ist damit weitgehend von der Java-Logik getrennt und zentral editierbar.

**Praktischer Befund:** Items, Quests und Begleiter wurden in der Praxis geprüft.

---

# 24. Sprache / Deutsch-only Refactor

Die Roadmap definiert Deutsch als einzige Spielersprache des Projekts.

Bereits erledigt:

- LanguageManager entfernt
- `lang.*`-Package entfernt
- Translation-Laufzeitabhängigkeiten entfernt
- Locale-Abhängigkeiten für Spielertexte entfernt
- Sprachdateien entfernt
- Translation Keys entfernt
- GUI-Texte migriert
- Dialogtexte migriert
- Item-Texte migriert
- Quest-Texte migriert
- Companion-Texte migriert
- Boss-/Story-Texte geprüft
- Command-Ausgaben geprüft
- Actionbar-/Scoreboard-Ausgaben geprüft
- Chat-Nachrichten praktisch geprüft

**Praktischer Gesamtbefund:** Die bisher geprüften Übersetzungen funktionieren. Es bestehen keine grundsätzlichen Probleme mit der Deutsch-only-Umstellung.

**Noch offene sichtbare Reste:** Quick-Actions-Stat-Namen und Quick-Actions-Item-Namen.

---

# 25. Datenbank / Persistenz

Vorhanden:

- `DatabaseManager`
- HikariCP
- MySQL Repository
- YAML Repository
- PlayerProfile Repository Abstraktion

**Praktischer Befund:** Datenbankverbindung wurde im echten Serverlauf getestet und funktionierte.

**Noch nicht vollständig bewiesen:**

- kompletter Save/Reload-Zyklus
- Restart-Persistenz
- Stats nach Reload
- Equipment nach Reload
- Questfortschritt nach Reload
- Economy nach Reload
- Professionen nach Reload
- Companion-Daten nach Reload
- vollständige Cross-System-Persistenz

Das ist kein gemeldeter Fehler, sondern ein noch fehlender Beweis durch vollständigen Regressionstest.

---

# 26. Commands

Vorhanden sind unter anderem:

- Root Command
- Guild Commands
- Boss Subcommand
- Companion Subcommand
- Debug Subcommand
- Item Subcommand
- NPC Subcommand
- Party Subcommand
- Player Admin Subcommand
- Quest Admin Subcommand
- Quest Log
- Shop Subcommand

Die gewünschte Benutzerform ist:

```text
/pixelrpg
/pixelrpgadmin
```

und anschließend die jeweiligen Subcommands/Tab-Completions, nicht eine für Spieler sichtbare Namespace-Schreibweise wie `/pixelrpg:pixelrpgadmin`.

**Status:** Command-Registrierung ist vorhanden; der vollständige Runtime-Test aller Commands inklusive Tab Completion ist noch offen.

**Technischer Prüfhinweis:** `RootCommand`/Command-Adapter müssen separat gegen die verbindlichen modernen Paper-26.x-Command-Vorgaben geprüft werden. Dieser Punkt darf nicht ungeprüft als Fehler oder als behoben bezeichnet werden, solange kein konkreter Runtime-/API-Befund vorliegt.

---

# 27. Sicherheits-/Forensikprüfung

Im bisherigen statischen Audit wurden keine unmittelbaren Hinweise auf folgende problematische Mechanismen festgestellt:

- `Runtime.getRuntime().exec()`
- `ProcessBuilder`
- versteckte Reflection-Codeausführung
- `Unsafe`
- `URLClassLoader`
- verdächtige Bytecode-Downloads
- fest verdrahtete UUID-Hintertüren
- echte API-Keys im Repository
- versteckte Konsolenbefehlsausführung

Die Datenbankkonfiguration enthält weiterhin den Platzhalter `CHANGE_ME`. Ein echtes Produktionspasswort darf nicht in Git committed werden.

---

# 28. Aktueller Fehler-/Risikoüberblick

## 🟥 Konkrete offene sichtbare Befunde

### 1. Quick Actions – Stat-Namen

Englische Stat-Bezeichnungen im Charakterprofil.

**Art:** UI/Text  
**Auswirkung:** sichtbar, funktional nicht kritisch.

### 2. Quick Actions – Item-Namen

Englische Item-Bezeichnungen im Charakterprofil.

**Art:** UI/Text  
**Auswirkung:** sichtbar, funktional nicht kritisch.

### 3. Profession Trainer – Nachtest

Die Codekorrektur ist vorhanden, aber der abschließende praktische Test aller vier Trainer steht noch aus.

**Art:** Regression offen.

---

## 🟨 Noch nicht ausreichend praktisch abgesichert

- Level/XP vollständig
- Stats vollständig
- Equipment vollständig
- Combat vollständig
- Story vollständig
- Professions/Crafting nach Korrektur
- Economy
- Shop
- Party
- Guild
- Bosses
- Scoreboard
- Trade
- Travel
- Commands/Tab Completion
- vollständige Persistenz
- Shutdown

Diese Punkte sind **nicht automatisch Bugs**. Sie sind offene Testfälle.

---

# 29. Was vor Beta-Übergabe tatsächlich noch auf dich zukommt

Die Reihenfolge sollte nicht neue Features enthalten, sondern nur die vorhandene Implementierung auf möglichst nahe 100 % bringen:

1. Quick-Actions-Stat-Namen auf Deutsch.
2. Quick-Actions-Item-Namen auf Deutsch.
3. Vier Profession-Trainer nach der Korrektur einzeln testen.
4. Rezeptdarstellung je Beruf verifizieren.
5. Save/Reload/Restart mit Datenbank testen.
6. Noch nicht getestete Kernsysteme einzeln durchspielen.
7. Boss-System praktisch testen.
8. Guild/Party praktisch testen.
9. Economy/Shop/Trade/Travel praktisch testen.
10. Commands und Tab Completion praktisch testen.
11. Shutdown testen.
12. Alle während dieser Tests gefundenen Fehler beheben.
13. Danach **keine neuen Systeme** mehr für die Beta einführen.

Balancing, QOL, Textkorrekturen und das Präzisieren bereits vorhandener Systeme bleiben ausdrücklich zulässig, solange das bestehende Feature nicht konzeptionell erweitert wird.

---

# 30. Beta-Freeze / Definition of Done

Der Beta-Stand ist erreicht, wenn:

- das geplante Feature-Set vollständig vorhanden ist,
- alle bekannten konkreten Fehler behoben sind,
- der Build funktioniert,
- der Server sauber startet,
- die Datenbank funktioniert,
- die wesentlichen Spielerflüsse praktisch getestet wurden,
- die vorhandenen NPCs/Dialoge funktionieren,
- Items/Quests/Begleiter funktionieren,
- die Profession-Trainer nach der Korrektur funktionieren,
- die sichtbaren Deutsch-only-Reste bereinigt sind,
- Persistenz ausreichend getestet wurde,
- keine neuen Features mehr notwendig sind.

**100 % bedeutet hier nicht „perfekt für immer“.**

Es bedeutet:

> **100 % des für diese Beta vorgesehenen Implementierungsumfangs ist vorhanden, funktionsfähig und ausreichend getestet.**

Danach beginnt die Spieler-Testphase:

- Bugs melden
- Probleme melden
- positive Erfahrungen sammeln
- Verbesserungsvorschläge sammeln
- Wünsche sammeln

Neue Wünsche werden nicht automatisch implementiert. Sie werden nach dem Beta-Feedback bewertet.

---

# 31. Strikte Abgrenzung: spätere Ideen

Die folgende Idee wurde bewusst nur dokumentiert und ist **nicht Teil des aktuellen Beta-Umfangs**:

## PixelRPG Region System

Geplantes eigenes Region-System als Mischung aus freier WorldEdit-artiger Polygonmarkierung und WorldGuard-artiger Regionslogik.

Grundidee:

- Stick als Markierungswerkzeug
- beliebig viele Konturpunkte
- diagonale und unregelmäßige Grenzen
- Polygon-Flächenberechnung
- Min-Y/Max-Y
- tatsächliche Form statt Bounding Box
- Wildnis als Default
- Dörfer, Städte, Ruinen, Gildenstädte, Bossgebiete usw. als eigene Regionen
- Enter-/Leave-Nachrichten
- spätere Verknüpfung mit bestehenden PixelRPG-Systemen
- Minecraft-Biome bleiben von PixelRPG-Regionen getrennt

Die vollständige Idee ist in `docs/ideas/pixelrpg-region-system.md` dokumentiert.

**Status:** reine spätere Idee. Keine Implementierung im aktuellen Freeze.

---

# 32. Forensisches Gesamturteil

Der Branch ist kein leerer Prototyp und auch kein loses Sammelsurium von Einzelideen. Er enthält einen bereits umfangreichen, modularen RPG-Kern.

Die wesentlichen Systeme sind vorhanden:

- Player/Profile
- Level/XP
- Stats
- Combat
- Skills
- Scaling
- Items
- Equipment
- Quests
- NPCs
- Dialoge
- Quick Actions
- Berufe
- Crafting
- Begleiter
- Bosse
- Party
- Guild
- Economy
- Shop
- Trade
- Travel
- Scoreboard
- Story
- Persistenz

Der aktuelle Hauptrest der gerade abgeschlossenen Deutsch-only-Refactor-Phase besteht nicht aus einer fehlenden Language-Architektur, sondern aus wenigen sichtbaren Textresten und noch ausstehenden Regressionstests.

Der wichtigste konkrete sichtbare Rest ist derzeit das native Quick-Actions-Charakterprofil mit englischen Stat- und Item-Bezeichnungen.

Der wichtigste funktionale offene Testfall ist derzeit die vollständige Regression der Berufslehrer nach der bereits vorgenommenen Korrektur sowie der noch nicht vollständig durchgeführte Persistenz-/Systemdurchlauf.

**Es gibt im aktuellen Audit keinen belastbaren Grund, den bestehenden Beta-Freeze wegen eines neuen Gameplay-Systems aufzubrechen.**

Der richtige nächste Schritt ist deshalb nicht mehr „mehr bauen“, sondern:

> **vorhandenes System für System auf 100 % bringen, Fehler beseitigen, testen, stabilisieren und anschließend den Spielern übergeben.**

---

# 33. Audit-Schlussstatus

**Codebestand:** umfangreich implementiert  
**Language-Refactor:** weitgehend abgeschlossen  
**Deutsche Runtime-Texte:** bisher praktisch unauffällig  
**Serverstart:** erfolgreich getestet  
**Datenbank:** erfolgreich getestet  
**NPCs:** praktisch getestet  
**Dialoge:** praktisch getestet  
**Chat:** praktisch getestet  
**Items:** praktisch getestet  
**Quests:** praktisch getestet  
**Begleiter:** praktisch getestet  
**Profession Trainer:** Code korrigiert, Nachtest offen  
**Quick Actions:** funktional, zwei sichtbare englische Textreste offen  
**Build:** Build-Pfad vorhanden und erfolgreich dokumentiert  
**CI:** korrekt konfiguriert; für den aktuellen reinen Dokumentationscommit kein neuer PR-Workflow-Lauf vorhanden  
**Persistenz-Volltest:** offen  
**Vollständige Runtime-Regression:** offen  
**Neue Features:** während des Beta-Freeze nicht erforderlich

**Forensisches Fazit:** Der Branch befindet sich realistisch in der Phase **„bestehende Implementierung fertig testen und stabilisieren“**, nicht mehr in der Phase „neue Kernsysteme entwickeln“.
