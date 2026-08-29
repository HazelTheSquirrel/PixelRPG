# 🕵️ Forensischer 100-%-Audit: PixelRPG

**Repository:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `refactor/central-content-pipeline-v3`  
**Audit-Stand:** 29.08.2026  
**Basis:** vollständiger Branch-Tree, Einzeldatei-/Codeprüfung, vorhandene Runtime-Testmeldungen und aktuelle Korrekturen  
**Historische Referenz:** `9fadeadefba378361722a16fad66be7f62a70560`

> Dieser Audit bewertet den tatsächlich vorhandenen Beta-Scope. Es wurde kein GitHub-DeepSearch als Grundlage verwendet. Neue Features sind ausdrücklich nicht Bestandteil der aktuellen Abschlussphase.

---

## 1. Bewertungsregeln

- **Implementiert:** Code/Datenstruktur ist vorhanden.
- **Praktisch getestet:** vom Entwickler im echten Serverlauf geprüft.
- **Statisch behoben:** Codekorrektur ist vorhanden, Runtime-Nachtest steht noch aus.
- **Offen:** kein belastbarer Fehlernachweis, aber der vollständige Test fehlt.
- **Nicht implementiert:** bewusst nicht Bestandteil des aktuellen Scopes oder tatsächlich fehlend.

Ein Prozentwert bedeutet hier **Reifegrad des aktuellen Beta-Scopes**, nicht Vollständigkeit des gesamten zukünftigen Spiels.

---

# 2. Plattform / Build / CI

### Zielplattform

- Java 25
- Paper 26.2
- `io.papermc.paperweight.userdev` `2.0.0-beta.21`
- `paperweight.paperDevBundle("26.2.build.+")`
- `paper-plugin.yml`
- Mojang-Mappings
- ShadowJar

### Build

Der Build-Pfad ist vorhanden und war bereits im Projektverlauf funktionierend. Der GitHub-Actions-Workflow baut mit Java 25 und `gradle build --no-daemon --stacktrace`.

**Status:** 🟢 95 %  
**Offen:** abschließender Build-/CI-Regressionslauf nach den aktuellen Codeänderungen.

---

# 3. Plugin-Kern / Lifecycle

Vorhanden und miteinander verbunden:

- `PixelRPGBootstrap`
- `PixelRPGPlugin`
- zentraler Service-Lifecycle
- Listener-/Task-Registrierung
- Shutdown
- API/Event-Schicht
- Player/Profile
- Stats
- Items
- Equipment
- Quests
- NPCs
- Dialoge
- Professionen
- Crafting
- Begleiter
- Bosse
- Party
- Gilde
- Economy
- Shop
- Travel
- Scoreboard
- Story

**Praktischer Befund:** Serverstart mit Datenbank lief beim Entwickler ohne Fehler.

**Status:** 🟢 95 %

---

# 4. Player / Registrierung / Persistenz

Vorhanden:

- PlayerProfile
- PlayerProfileManager
- MySQL-Repository
- YAML-Repository
- Registrierung / Unregistrierung
- Level / XP
- Save-/Load-Strukturen

**Praktisch geprüft:** Registrierung und Datenbankverbindung.

**Offen:** vollständiger Restart-/Save-/Reload-Regressionslauf für alle Systeme.

**Status:** 🟢 90 %

---

# 5. Stats / Combat / Scaling / Loot

Vorhanden:

- StatEngine
- Statistics Service
- HP
- Rüstung
- Bewegungsgeschwindigkeit
- Reichweite
- Schaden
- Kritische Trefferchance
- Kritischer Schaden
- Lebensraub
- Angriffskraft
- Combat State
- Damage Context
- Mob Scaling
- Weapon Abilities
- Loot
- Soulbound Death Handling

**Status:** 🟢 90 %

**Offen:** vollständiger End-to-End-Regressionslauf aller Stat-/Combat-Kombinationen.

---

# 6. Items / Waffen / Rüstung

Das Item-System ist zentral über `ItemDefinition`, `ItemService` und `RPGItemBuilder` aufgebaut.

Raritäten:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Die Waffenstats wurden bereits auf Deutsch geprüft.

### Aktuelle Korrektur

Die sichtbaren Item-Lore-Reste im `ItemService` wurden ebenfalls auf Deutsch gebracht:

- `Requires Level` → `Benötigt Level`
- `Soulbound` → `Seelengebunden`
- `UNIQUE` → `EINZIGARTIG`
- `Crit` bei Rüstung → `Kritische Trefferchance`
- `Gearscore` → `Ausrüstungswert`
- `Crafted Item` → `Hergestellter Gegenstand`
- `Cooldown` → `Abklingzeit`

Damit betrifft die Deutschbereinigung nicht mehr nur Waffen, sondern auch Rüstungen und die generierten sichtbaren Item-Lore-Zeilen.

**Status:** 🟢 95 %  
**Offen:** Runtime-Test mit neu erzeugten Waffen und Rüstungen.

---

# 7. Rüstungssets

In `equipment-sets.json` sind sechs vollständige Sets definiert:

| Set | Teile | Set-Boni | Levelbereich |
|---|---:|---|---:|
| Donnerwacht | 4 | Bewegung, Reichweite, Angriffskraft | 10 |
| Schattengeflecht | 4 | Krit, Lebensraub, Krit-Schaden | 20 |
| Stahlwall | 4 | Rüstung, HP, Angriffskraft | 30 |
| Sonnengewand | 4 | Bewegung, Krit-Schaden, Krit | 45 |
| Kristallwache | 4 | Schaden, Rüstung, Krit-Schaden | 60 |
| Höllenschmiede | 4 | HP, Schaden, Lebensraub, Angriffskraft | 80 |

Die zugehörigen vier Rüstungsteile pro Set sind in `item-definitions.json` vorhanden und besitzen jeweils die korrekte `setId`.

### Forensischer Befund zur Beschaffung

Die Sets sind **definiert und technisch erzeugbar**, aber in den geprüften Content-Quellen wurde kein normaler Spieler-Beschaffungspfad gefunden:

- keine Set-Items in `crafting-recipes.json`
- keine Set-Items in `boss-reward-items.json`
- kein erkennbarer Quest-Reward-Pfad zu den Set-Item-IDs

Damit ist aktuell **nicht nachgewiesen, dass ein normaler Spieler die sechs Rüstungssets im Spiel tatsächlich erhalten kann**.

Das ist ein echter Content-/Gameplay-Befund, aber die korrekte Beschaffungsquelle darf nicht erfunden werden. Vor einer Implementierung muss feststehen, **welcher bereits vorgesehene Erwerbsweg** dafür gelten soll.

**Status:** 🟡 75 %

---

# 8. Equipment / Equipment Sets Runtime

Vorhanden:

- sechs Equipment-Slots
- Slotvalidierung
- Levelanforderungen
- Equipment-Persistenz
- Set-Erkennung
- Set-Boni
- Armor-Trims
- Stat-Recalculation

**Status:** 🟢 90 %

**Offen:** praktischer Kompletttest jedes Sets mit 2/3/4 Teilen und anschließendem Entfernen/Austauschen von Teilen.

---

# 9. Quests

Vorhanden:

- Quest Repository
- Quest Manager
- Quest Progress
- Quest Text
- Quest Navigation
- Quest Events
- Mob-Kill-Ziele
- Collect-Ziele
- datengetriebene Quest-Dateien

**Praktischer Befund:** Quests wurden vom Entwickler getestet und funktionieren.

**Status:** 🟢 95 %

Weitere Balancing-/QOL-Anpassungen sind erlaubt, solange das bestehende Questkonzept nicht erweitert wird.

---

# 10. NPC / Reception / Dialogue

Vorhanden:

- NPC Manager
- NPC Behaviors
- Reception
- Quest
- Shop
- Travel
- Story
- Banker
- Filler
- Profession Trainer
- natives Paper-Dialog-System

Die vorhandenen NPCs wurden vom Entwickler praktisch durchgetestet.

### Reception-Korrektur

Der sichtbare Button

> `Aus dem Rathaus austreten`

wurde ersetzt durch:

> `PixelRPG-Registrierung aufheben`

Auch die Bestätigungs-/Erfolgstexte wurden entsprechend angepasst. Die Bezeichnung beschreibt jetzt die tatsächliche Funktion: Das Aufheben der PixelRPG-Registrierung und das Zurücksetzen des RPG-Fortschritts.

**Status:** 🟢 95 %  
**Offen:** Runtime-Nachtest der geänderten Reception-Texte.

---

# 11. Professionen / Handwerker-NPCs / Crafting

Berufe:

- Blacksmith
- Provisioner
- Alchemist
- Scholar

Vorhanden:

- Profession Service
- Profession Trainer
- Profession Dialog
- Crafting Recipe Registry
- Crafting Service
- datengetriebene Rezepte

Der zuvor festgestellte Fehler, dass die vier Handwerker nicht sauber ihre eigenen Rezeptbereiche anzeigten, wurde im Code korrigiert.

**Status:** 🟢 90 %

**Offen:** vier NPCs einzeln im Spiel prüfen:

1. Blacksmith → ausschließlich Blacksmith-Rezepte
2. Provisioner → ausschließlich Provisioner-Rezepte
3. Alchemist → ausschließlich Alchemist-Rezepte
4. Scholar → ausschließlich Scholar-Rezepte

---

# 12. Quick Actions / Charakterprofil

Das native `minecraft:quick_actions`-System ist vorhanden und funktioniert grundsätzlich.

### Korrektur abgeschlossen

Die sichtbaren englischen Stat-Namen im Charakterprofil wurden auf Deutsch geändert:

- Armor → Rüstung
- Movement Speed → Bewegungsgeschwindigkeit
- Reach → Reichweite
- Damage → Schaden
- Crit → Kritische Trefferchance
- Crit-Schaden → Kritischer Schaden
- Lifesteal → Lebensraub
- Attack Power → Angriffskraft
- EXP → Erfahrung
- STATS → WERTE
- HP → LP

Zusätzlich wird die Item-Rarität in diesem Bereich nicht mehr nur als interner englischer Enum-Name ausgegeben, sondern als deutsche sichtbare Bezeichnung.

**Status:** 🟢 95 %  
**Offen:** Runtime-Test im tatsächlichen `G`-Menü.

---

# 13. Begleiter

Intern bleibt `Companion` vollständig erhalten.

Sichtbar im Spiel ist **Begleiter**.

### Korrektur abgeschlossen

Der `common-chicken` / „PixelRPG Huhn“-Eintrag wird nicht mehr im nativen `PixelRPG – Begleiter`-Menü aus `minecraft:quick_actions` angezeigt.

Wichtig: Der Companion wurde **nicht aus dem internen Companion-System gelöscht**. Die Änderung betrifft ausschließlich die sichtbare Quick-Action-Liste.

**Status:** 🟢 95 %  
**Offen:** Runtime-Test der Begleiterliste und Prüfung, dass alle übrigen Begleiter weiterhin angezeigt und bedienbar sind.

---

# 14. Boss-System

Vorhanden:

- BossDefinition
- BossManager
- BossRepository
- BossPhase
- ActiveBoss
- Attack Pattern Registry
- BossBar
- Damage Contribution
- Death Handling
- Reward Handling
- World-Boss-Schutz
- Biome-/World-Boss-Trennung

**Status:** 🟢 85 %

**Offen:** vollständiger Runtime-Test aller vorhandenen Biome- und World-Bosse einschließlich Phasen, Angriffsmustern, Loot und Companion-Rewards.

---

# 15. Party / Guild / Economy / Shop / Trade / Travel

Alle Systeme besitzen konkrete Java-Implementierungen und die dazugehörigen Services/Manager/GUI/Dialoge.

**Status:** 🟢 85–90 % je System.

**Offen:** vollständige Runtime-Regression.

Es besteht aktuell kein belastbarer Befund, dass diese Systeme grundsätzlich kaputt sind. Sie sind lediglich noch nicht in gleicher Tiefe praktisch abgesichert wie NPCs, Dialoge, Quests, Items und Datenbankstart.

---

# 16. Story-System

Vorhanden:

- StoryManager
- StoryChapter
- StoryBookFactory
- Story-NPC/Mannequin
- Dialog-Anbindung

Das System ist vorhanden. Die geplante spätere inhaltliche Überarbeitung des Buch-/Story-NPCs ist Content-Arbeit und kein Grund, den Beta-Freeze aufzubrechen.

**Status:** 🟢 85 %

---

# 17. Sprache / Deutsch-only

Der große Language-Refactor ist abgeschlossen.

Praktisch getestet wurden bereits:

- NPC-Texte
- Dialoge
- Quests
- Items
- Begleiter
- Chat-Nachrichten
- Serverausgaben

Die noch vorhandenen englischen UI-Reste aus dem letzten Audit wurden jetzt **statisch korrigiert**:

- Item-Lore
- Rüstungs-Lore
- Quick-Actions-Charakterprofil
- sichtbare Item-Raritäten im Quick-Actions-Kontext

**Status:** 🟢 95 %

**Offen:** einmaliger Runtime-Durchlauf, um sicherzustellen, dass keine weitere sichtbare englische Ausgabe aus diesen Pfaden kommt.

---

# 18. Commands

Die gewünschte Benutzerform bleibt:

```text
/pixelrpg
/pixelrpgadmin
```

mit Subcommands und Tab Completion.

Die Namespace-Schreibweise `/pixelrpg:pixelrpgadmin` soll nicht als normale Benutzerbedienung erforderlich sein.

**Status:** 🟢 95 %

**Offen:** vollständiger Runtime-Test aller relevanten Commands und Tab-Completions.

---

# 19. Datenbank / Persistenz

Vorhanden:

- HikariCP
- MySQL Connector
- DatabaseManager
- MySQL Repository
- YAML Repository
- Profile Persistence

**Praktischer Befund:** Datenbankverbindung funktionierte im echten Serverlauf.

**Offen:** vollständiger Restart-Test mit:

- Profil
- Level/XP
- Quests
- Equipment
- Stats
- Economy
- Professionen
- Begleiter
- Gilde

**Status:** 🟢 90 %

---

# 20. Sicherheits-/Forensik-Befund

Im bisherigen statischen Audit wurden keine unmittelbaren Hinweise auf typische versteckte Ausführungsmechanismen wie `Runtime.exec`, `ProcessBuilder`, verdächtige Bytecode-Downloads, `Unsafe`, `URLClassLoader` oder fest verdrahtete Hintertüren festgestellt.

Produktionspasswörter/Secrets dürfen weiterhin nicht in Git committed werden.

**Status:** 🟢 kein konkreter Befund.

---

# 21. Aktueller Fehler-/Risikoüberblick

## Behobene konkrete Befunde

| Befund | Status |
|---|---|
| Englische Stat-Namen im Charakterprofil | ✅ Code behoben |
| Englische sichtbare Item-Lore-Reste | ✅ Code behoben |
| Englische sichtbare Item-Raritäten im Quick Actions Kontext | ✅ Code behoben |
| `Armor` im Charakterprofil | ✅ Rüstung |
| `Movement Speed` | ✅ Bewegungsgeschwindigkeit |
| `Reach` | ✅ Reichweite |
| `Damage` | ✅ Schaden |
| `Crit` | ✅ Kritische Trefferchance |
| `Lifesteal` | ✅ Lebensraub |
| `Attack Power` | ✅ Angriffskraft |
| `common-chicken` im Quick-Action-Begleitermenü | ✅ ausgeblendet |
| `Aus dem Rathaus austreten` | ✅ umbenannt |
| Bestätigungstexte der Reception | ✅ angepasst |

## Noch offen

| Befund | Art | Status |
|---|---|---|
| Rüstungssets normal im Spiel erhältlich | Content/Design | 🟡 nicht nachgewiesen |
| Vier Profession-Trainer nach Korrektur | Runtime-Test | 🟡 offen |
| Quick Actions nach Textkorrektur | Runtime-Test | 🟡 offen |
| Begleiterliste nach Chicken-Filter | Runtime-Test | 🟡 offen |
| Persistenz kompletter Restart | Runtime-Test | 🟡 offen |
| Boss-Kompletttest | Runtime-Test | 🟡 offen |
| Party/Gilde/Economy/Shop/Trade/Travel | Runtime-Test | 🟡 offen |
| vollständige Build-/CI-Regression nach den aktuellen Änderungen | Build/CI | 🟡 offen |

---

# 22. Was jetzt noch auf dem Weg zu 100 % fehlt

Das sind **keine neuen Features**.

Es handelt sich ausschließlich um:

1. die aktuellen Codeänderungen im echten Spiel testen,
2. die vier Profession-Trainer vollständig testen,
3. alle sechs Rüstungssets auf tatsächliche Beschaffung prüfen,
4. vollständigen Persistenz-/Restart-Test durchführen,
5. Biome-/World-Bosse durchtesten,
6. Party/Gilde/Economy/Shop/Trade/Travel regressieren,
7. Commands/Tab Completion regressieren,
8. Build/CI nach den Änderungen prüfen,
9. dabei gefundene Bugs beheben,
10. danach Beta-Freeze halten.

### Rüstungssets – besondere Entscheidung

Hier darf **keine neue Beschaffungsmechanik erfunden werden**, nur damit die Sets „irgendwie“ erhältlich sind. Der aktuelle Befund lautet deshalb bewusst:

> Die sechs Sets existieren technisch und sind mit vollständigen Set-IDs definiert, aber ein regulärer Spieler-Erwerb ist in den geprüften Content-Quellen derzeit nicht nachgewiesen.

Das muss vor der Beta geklärt werden, ohne den bestehenden Scope unnötig zu erweitern.

---

# 23. Beta Definition of Done

Der aktuelle Beta-Scope ist fertig, wenn:

- alle vorgesehenen Systeme vorhanden sind,
- bekannte konkrete Fehler behoben sind,
- die aktuellen Textkorrekturen praktisch bestätigt sind,
- NPCs/Dialoge/Quests/Items/Begleiter funktionieren,
- Profession-Trainer korrekt funktionieren,
- die Beschaffung der vorgesehenen Rüstungssets geklärt ist,
- Persistenz ausreichend getestet ist,
- Build und CI funktionieren,
- die offenen Regressionstests abgeschlossen sind.

**100 % bedeutet nicht „für immer perfekt“.**

100 % bedeutet:

> **Alles, was für diese Beta vorgesehen war, ist vorhanden, funktioniert und wurde ausreichend getestet.**

Danach beginnt die Spieler-Testphase.

---

# 24. Beta-Freeze

Der Implementierungsstopp bleibt bestehen.

Erlaubt bleiben:

- Bugfixes
- Balancing
- QOL
- Textkorrekturen
- Präzisierung bestehender Systeme
- Korrektur unvollständiger vorhandener Inhalte

Nicht erlaubt als Teil dieses Abschlusses:

- neue Kernsysteme
- neue große Gameplay-Mechaniken
- neue Progressionssysteme
- neue Gildenarchitektur
- das Region-System
- sonstige neue Ideen aus der Zukunftsplanung

---

# 25. Spätere Region-System-Idee

`docs/ideas/pixelrpg-region-system.md` bleibt reine Dokumentation.

Die Idee eines eigenen WorldEdit-/WorldGuard-artigen PixelRPG-Systems mit frei geformten Polygonregionen ist **nicht implementiert** und darf den aktuellen Beta-Freeze nicht beeinflussen.

---

# 26. Forensisches Gesamturteil

PixelRPG ist auf diesem Branch kein Prototyp mehr, bei dem grundlegende RPG-Systeme fehlen. Der wesentliche Beta-Scope ist vorhanden und mehrere Kernbereiche wurden bereits real im Server getestet.

Die aktuelle Arbeit ist eindeutig eine **Fertigstellungs-, Test- und Stabilisierungsphase**.

Die zuletzt gemeldeten sichtbaren Deutsch-only-Probleme wurden im Code gezielt bereinigt:

- Rüstung
- Rüstungs-Lore
- Rüstungs-/Item-Sichttexte
- Charakterprofil
- Begleiterliste
- Reception

Der einzige neue inhaltliche Befund aus der aktuellen Prüfung betrifft die **Rüstungssets**: Sie sind technisch definiert, aber ein normaler Erwerb durch Spieler ist anhand der geprüften Content-Pfade nicht nachgewiesen.

Das ist kein Grund, jetzt ein neues System zu erfinden. Es ist ein Content-/Beschaffungsbefund, der vor dem Beta-Release geklärt werden muss.

## Schlussurteil

> **PixelRPG befindet sich realistisch bei der Fertigstellung des vorhandenen Beta-Scopes. Die nächste Arbeit ist Testen, Fehlerbehebung, Balancing und Stabilisierung — nicht neue Features entwickeln.**
