# PixelRPG – Feature Roadmap

Stand: 2026-08-25 – historische Roadmap-Struktur wiederhergestellt und mit `main` abgeglichen

> Diese Datei ist die verbindliche Arbeits-Roadmap. Die Punkte 01–22 werden einzeln geführt und von oben nach unten bearbeitet.
> Die Roadmap wurde aus der historischen Master-/Feature-Roadmap rekonstruiert. Spätere Kurzfassungen und reine Tabellen ersetzen die einzelnen Spezifikationen nicht.
> Prozentwerte beschreiben funktionale Reife des aktuell vorhandenen Codes, nicht die Menge an Code.
>
> 🟢 95–98 % = abgeschlossen · 🟡 50–94 % = in Arbeit · 🔴 0–49 % = offen · 🔵 vorbereitet/Zukunft · ⚪ verworfen

## Arbeitsregel

```text
1. Soll-Verhalten gemeinsam definieren
2. kompletten Code dieses Features prüfen
3. Soll gegen Ist vergleichen
4. fehlende/falsche Punkte identifizieren
5. Altbestand im selben Arbeitsschritt entfernen
6. notwendige Änderungen umsetzen
7. Build durchführen
8. Runtime testen
9. PixelRPG.md und roadmap.md aktualisieren
10. bei 95–98 % abhaken
```

## Verbindliche technische Basis

- Java 25
- Paper 26.x, verbindliches Ziel Paper 26.2
- paperweight-userdev 2.0.0-beta.21
- `paperweight.paperDevBundle("26.2.build.+")`
- Mojang-Mappings
- `paper-plugin.yml`
- natives Minecraft/Paper-26.2 Dialogsystem
- Adventure Components
- kein `ChatColor`
- keine alten 1.21.x-APIs oder Dialogimplementierungen
- keine CraftBukkit-/Legacy-NMS-Namen

## Verbindliche Designentscheidungen

- PixelRPG hat **keine Klassen**.
- PixelRPG hat **keine frei verteilbaren Player-Attribute**.
- Ein neu registrierter Spieler erhält keinen Startbonus.
- Level/XP sind die zentrale Spielerprogression.
- Das Level beeinflusst über das Balancing-System die Stärke von Waffen, Rüstungen und normalen Companions.
- Combat ist MMORPG-nah: Gear, Stats, Weapon Skills und aktive Companions bestimmen die Kampfstärke.
- Vanilla-Schaden bleibt Bestandteil der Berechnung; Vanilla-Angriffsgeschwindigkeit bleibt unverändert.
- Character Stats: HP, Armor, Movement Speed, Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power.
- Basis-Crit-Chance ist 0 %. Crit-Chance kommt über Waffen und aktive Companion-Boni.
- Standard-Crit-Schaden ist ×2 und kann durch Gear/Companions erhöht werden.
- Lifesteal heilt den entsprechenden Prozentsatz des tatsächlich verursachten Schadens.
- Rüstung wird über eine eigene MMORPG-Mitigation berechnet.
- Equipment: Helm, Brust, Hose, Schuhe, Waffe, Nebenhand.
- Gearscore wird aus Item-Level und Item-Definition/Balancing bestimmt.
- Companions geben passive Stat-Boni ausschließlich solange sie aktiv gerufen und draußen sind; Despawn entfernt den Bonus sofort. Unique Companions sind davon ausgenommen.
- Weapon Skills gehören zur jeweiligen Waffe. Bogen/Armbrust verwenden für Skills Shift+Rechtsklick, damit Vanilla-Spannen/Laden erhalten bleibt.
- PvP ist kein eigenes Gameplay-System.
- Währung: Goldtaler; virtuell als Kontostand und physisch als Sonnenblume. Keine weitere Währung.
- Spielerhandel erfolgt über ein Auktionshaus; vollständiger Player-Trade ist nicht priorisiert.
- Vier Professionen bleiben: `BLACKSMITH`, `PROVISIONER`, `ALCHEMIST`, `SCHOLAR`.
- Gilden sind ein eigenes soziales Gameplay-System mit Gildenstadt/-basis und Gildenbank.
- Partys unterstützen gemeinsame XP, Loot-Verteilung, Party-Buffs und gemeinsamen Questfortschritt.
- `/party` und Gilden-Commands sind Komfortfunktionen; NPC/Dialoge bleiben die eigentliche Gameplay-Interaktion.
- Alte Default-Bosse Forest Tyrant, Frost Sovereign und Void Reaper werden nicht als finales Bosskonzept fortgeführt.
- Biom-Bosse sind regionale Spezialmonster, keine Worldbosse. Sie sind an ein bestimmtes Minecraft-Biom gebunden und nutzen die interne 10-%-Spawnprüfung.
- Worldbosse sind ausschließlich Admin-gestartete Events. Aktuell gibt es sechs Worldbosse mit festen Leveln, individuellen Belohnungen für aktive Teilnehmer, mehreren Phasen, Adds und individuellen Angriffsmustern.
- Worldbosse dürfen die Weltumgebung nicht dauerhaft beschädigen oder verändern.
- Ein eigenes Regions-/Mob-Scaling-System außerhalb der Biom-Bosse wird nicht umgesetzt.
- Aktuell existiert keine verbindliche Lore.
- Travel folgt dem NPC-/Waypoint-Prinzip.
- Mehrsprachigkeit: Deutsch, Englisch, Spanisch, Französisch, soweit sinnvoll.

---

# 01 – Player — 🟢 96 %

## Registrierung & Isolation
- [x] PlayerProfile als zentrale RPG-Spielerquelle
- [x] Registrierung / Unregistrierung
- [x] persistenter Registrierungsstatus
- [x] registrierte und nicht registrierte Spieler getrennt behandeln
- [x] Vanilla-Spieler bleiben außerhalb der RPG-Progression
- [ ] vollständiger Event-by-Event-Isolation-Audit

## Lifecycle & Persistence
- [x] Join-Lifecycle
- [x] Quit-Lifecycle
- [x] Profil laden
- [x] Profil speichern
- [x] Shutdown-Speicherung/Cleanup
- [ ] vollständiger Restart-/Recovery-Test

## Progression
- [x] Level-System
- [x] XP-System
- [x] XP-Kurve
- [x] Level-Up-Verarbeitung
- [x] Level-Up-Event
- [x] Player-Level API
- [x] Level 100 bleibt Endgame-Grenze/reserviert

## Stats
- [x] zentrale Character-Stats
- [x] finale Stat-Berechnung
- [x] Stats API
- [x] Stat-Persistenz
- [x] keine frei verteilbaren Attribute
- [x] keine Klassen

---

# 02 – Combat — 🟡 90 %

- [x] zentrale Damage-Logik
- [x] Vanilla-Schaden als Bestandteil
- [x] Damage-Modifikatoren
- [x] Crit Chance
- [x] Crit Damage
- [x] Lifesteal
- [x] Armor-Mitigation
- [x] Weapon Skills
- [x] Weapon Cooldowns
- [x] Player Combat
- [x] Mob Combat
- [x] Combat State
- [x] Combat Events
- [x] Combat Cleanup
- [x] Boss Damage
- [x] PvP ohne separates PvP-System
- [ ] vollständige E2E-Regressionsprüfung aller Weapon Skills
- [ ] vollständige Vanilla-/PixelRPG-Mob-Isolation unter Combat

## Bewusst nicht vorgesehen
- [x] keine Talentbäume
- [x] keine Charakter-Skilltrees
- [x] keine WoW-Skillbar
- [x] keine Veränderung der Vanilla-Angriffsgeschwindigkeit

---

# 03 – Progression — 🟢 96 %

- [x] Level 1–99
- [x] Level 100 als reservierte Endgame-Grenze
- [x] persistente XP
- [x] deterministische XP-Kurve
- [x] Level-Up Events
- [x] Level beeinflusst RPG-Balancing
- [x] Level beeinflusst Waffen-/Rüstungs-/Companion-Stärke über Balancing
- [x] keine levelbasierten Gebiets-Sperren
- [x] Equipment-Progression
- [x] Gearscore-/Item-Level-Grundlage
- [ ] vollständige End-to-End-Progression Level 1 → 99

---

# 04 – Companions — 🟢 96 %

## Core
- [x] Companion Registry
- [x] Companion Definitions
- [x] Ownership
- [x] Unlock
- [x] Unique Companion Unlock
- [x] Aktivieren / Deaktivieren
- [x] Spawn / Despawn
- [x] Logout Cleanup
- [x] kein automatischer Restore beim Login
- [x] Follow-System
- [x] Movement-Typen
- [x] Passive Companions
- [x] passive Stats nur bei aktivem Companion
- [x] Combat-Regeln
- [x] Hostile-Mob-Targeting
- [x] Companion XP
- [x] Companion Level 1–99
- [x] Companion Equipment
- [x] Companion Abilities
- [x] Runtime Registry / Cleanup
- [x] Persistence
- [ ] vollständige E2E-Prüfung aller Companion-Abilities

## Mounts
- [ ] Pig Mount
- [ ] Horse Mount
- [ ] Zombie Horse Mount
- [ ] Skeleton Horse Mount
- [ ] Nautilus Mount
- [ ] Bee Flugmount
- [ ] Mount Ownership / Fremd-Mount-Schutz
- [ ] Mount Cleanup
- [ ] Mount Persistence / Restore

## Unique Mannequin
- [ ] Unique-Hazel-Mannequin
- [ ] Skin Resolver
- [ ] eigener Combat Controller

---

# 05 – Quests — 🟡 90 %

- [x] Quest Definitions
- [x] Quest Repository / Registry
- [x] Quest Manager
- [x] Quest Progress
- [x] Quest Completion
- [x] Quest Rewards
- [x] Quest XP
- [x] Quest XP Scaling
- [x] HUNT
- [x] COLLECT
- [x] TALK_TO_NPC
- [x] REACH_LOCATION
- [x] GLOBAL_EVENT
- [x] Mob-Kill-Tracking
- [x] Quest Navigation
- [x] Recovery Compass
- [x] Passive Quest Checks
- [x] Global Event State
- [x] Quest Events
- [x] Quest Persistence
- [x] maximal 5 aktive Quests
- [x] Questdetail-Dialog
- [x] tatsächlicher Questname in aktiven Quests
- [x] konkretes Ziel/Fortschritt sichtbar
- [x] Belohnungen/Zeitinformationen sichtbar
- [x] Quest abbrechen
- [x] `ESCORT` als nicht unterstützter Quest-Typ entfernt
- [ ] vollständige Runtime-Regression aller Quest-Dialoge
- [ ] alle aktiven Quest-Unlocks E2E testen
- [ ] Questketten vollständig prüfen

**Verbindliche UI-Regel:** Das Questziel muss beim NPC vor Annahme und jederzeit in „Aktive Quests“ eindeutig lesbar sein. Spieler dürfen nicht raten müssen, was zu tun ist.

---

# 06 – NPC / Dialogue — 🟢 96 %

## NPC
- [x] NPC Grundsystem
- [x] NPC Definitions
- [x] NPC Spawn
- [x] NPC Interaktion
- [x] NPC Persistence
- [x] Chunk Lifecycle
- [x] Login Resync
- [x] Quest Integration
- [x] Service-NPC-Grundlage
- [x] Travel-NPC
- [ ] vollständiger Shop-/Service-E2E-Audit

## Dialogue
- [x] Native Dialog Framework
- [x] Dialogue Definitions
- [x] Dialogue Choices
- [x] Dialogue Actions
- [x] NPC → Dialogue
- [x] Quest Dialogue
- [x] Shop-/Bank-/Travel-Grundlagen
- [x] Companion Dialogue
- [x] Profession Dialogue
- [x] Soulbound Dialog Action
- [x] `/dialogue` verwendet zentrale Dialogarchitektur
- [ ] vollständiger Audit auf alte/duplizierte Dialogpfade

---

# 07 – Items — 🟢 96 %

- [x] RPGItemBuilder
- [x] Item Definitions
- [x] Item Service
- [x] Item Categories
- [x] Gear Categories
- [x] Common
- [x] Uncommon
- [x] Rare
- [x] Epic
- [x] Legendary
- [x] Unique
- [x] Item Stats
- [x] Item Level
- [x] Gearscore-Grundlage
- [x] Custom Item Metadata / PDC
- [x] Crafted Items
- [x] Soulbound
- [x] Item Economy Values
- [x] Item API
- [x] Vanilla Item Isolation als Designziel
- [ ] vollständige Resourcepack-Integration aller finalen Items
- [ ] vollständiger Vanilla-/PixelRPG-Mischbetrieb-Test

---

# 08 – Equipment — 🟢 95 %

- [x] Equipment Slots
- [x] Helm
- [x] Brust
- [x] Hose
- [x] Schuhe
- [x] Mainhand/Waffe
- [x] Nebenhand
- [x] Equipment Stats
- [x] Armor Stats
- [x] Weapon Stats
- [x] Equipment Restrictions
- [x] Level Requirements
- [x] Rarity Integration
- [x] Equipment → Player Stats
- [x] Equipment Cleanup
- [ ] vollständiger Equipment-GUI-E2E-Test
- [ ] vollständiger Level-/Item-Level-Balancing-Audit

---

# 09 – Crafting — 🟢 96 %

- [x] Crafting Registry
- [x] Crafting Definitions
- [x] Crafting Recipes
- [x] Crafted Item Factory
- [x] Profession Requirements
- [x] Custom Crafting
- [x] Dialogue-based Crafting
- [x] Crafting GUI/Selection soweit erforderlich
- [x] Crafting Validation
- [x] Level-/Rarity-Progression
- [x] Vanilla Crafting bleibt parallel funktionsfähig
- [ ] vorhandene Rezepte vollständig E2E testen
- [ ] vollständiger Vanilla-/PixelRPG-Crafting-Isolationstest

---

# 10 – Professions — 🟢 96 %

- [x] Profession Framework
- [x] Profession Levels
- [x] Profession XP
- [x] Profession 1–100
- [x] Profession Recipes
- [x] Profession Requirements
- [x] Profession UI/Dialog-Grundlage
- [x] Profession Integration
- [x] BLACKSMITH
- [x] PROVISIONER
- [x] ALCHEMIST
- [x] SCHOLAR
- [x] persistente Profession-Daten
- [ ] vollständige Definition aller finalen Rezepte und E2E-Test

**Wichtig:** Profession-Level sind für Crafting/Profession-Fortschritt gedacht, nicht als Zugangssperre für Weltgebiete.

---

# 11 – Economy / Gold — 🟢 96 %

- [x] Economy API
- [x] Goldtaler als einzige Währung
- [x] virtueller Kontostand
- [x] physische Sonnenblume als Gold-Repräsentation
- [x] Currency Storage
- [x] Currency Transactions
- [x] Item Economy Values
- [x] Price Definitions / Economy-Grundlage
- [x] Bank
- [x] Economy Persistence
- [x] NPC/Dialog-Anbindung
- [ ] vollständiger Shop-E2E-Test
- [ ] vollständiger Economy-Restart-/Rollback-Test

**Nicht vorgesehen:** zweite Währung, Gems/Runes als Währung oder eine globale Economy für Vanilla-Spieler.

---

# 12 – Party — 🟢 98 %

- [x] Party API
- [x] Party Creation
- [x] Party Members
- [x] Invite System
- [x] Leave Party
- [x] Party Events
- [x] Party Data
- [x] Party GUI/Charakterdialog-Anbindung
- [x] Party Gameplay Integration
- [x] Party Lifecycle / Disconnect Handling
- [x] gemeinsame XP
- [x] Loot-Verteilung
- [x] gemeinsamer Questfortschritt
- [x] Party-Buffs-Grundlage
- [x] `/party` als Komfortcommand
- [x] NPC/Dialoge bleiben primäre Gameplay-Interaktion
- [ ] vollständiger Party-Buff-E2E-Test
- [ ] vollständiger Multiplayer-Regressionstest

---

# 13 – Travel / Waypoints — 🟢 96 %

- [x] Travel-NPC
- [x] Waypoint-/Zieldefinitionen
- [x] Ziel freischalten über NPC
- [x] verfügbare Reiseziele beim erneuten Ansprechen anzeigen
- [x] Travel über Dialog
- [x] Persistenz der Freischaltungen
- [x] keine levelbasierte Gebiets-Sperre
- [ ] vollständiger Travel-E2E-Test über alle definierten Ziele
- [ ] Fehler-/Disconnect-Verhalten vollständig prüfen

---

# 14 – GUI / UI — 🟢 95 %

- [x] GUI-Grundlage
- [x] Main RPG/Character UI
- [x] Quest UI
- [x] Companion UI
- [x] Equipment UI
- [x] Item UI-Grundlagen
- [x] Party UI
- [x] Dialogue UI
- [x] Admin UI/Command-Integration
- [x] Bank UI
- [x] Native Minecraft Dialog Integration
- [x] G-Taste über `minecraft:quick_actions`
- [x] G-Taste nur für das eigene PixelRPG-Spielerprofil
- [x] kein permanentes MMO-HUD
- [ ] vollständiger Legacy-GUI-Audit
- [ ] vollständiger UI-/Language-Regressionstest

---

# 15 – Database / Persistence — 🟢 95 %

- [x] Database Connection
- [x] HikariCP
- [x] MySQL
- [x] YAML Repository / Konfigurationsdaten
- [x] Player Persistence
- [x] Companion Persistence
- [x] Quest Persistence
- [x] Economy Persistence
- [x] Guild Persistence
- [x] Item-/Equipment-Daten soweit erforderlich
- [x] Connection Lifecycle
- [x] Shutdown Cleanup
- [ ] vollständiger Server-Restart-Persistenztest aller Systeme
- [ ] historische DB-Spalten/Keys vollständig auditieren

---

# 16 – Commands / Permissions — 🟢 96 %

- [x] `/rpgadmin`
- [x] `/rpgparty` / Party-Komfortpfad
- [x] `/questlog`
- [x] `/dialogue`
- [x] Gilden-Komfortcommands
- [x] Permission Framework
- [x] `rpg.admin`
- [x] `rpg.member`
- [x] Command Validation
- [x] Command-Orphans weitgehend bereinigt
- [ ] vollständiger Orphan-/Permission-Audit

**Regel:** Commands sind keine Voraussetzung für die eigentlichen RPG-Systeme; NPC/Dialoge bleiben der primäre Gameplay-Weg.

---

# 17 – Bosses — 🟢 98 %

## Gemeinsames Boss-System
- [x] Boss Framework
- [x] Boss Definitions / Registry
- [x] feste Boss-Level
- [x] Boss Stats
- [x] BossBar
- [x] Attack Patterns
- [x] datengetriebene Boss-Loot-Tables
- [x] Boss → optionaler Companion Unlock
- [x] Tageslicht-Immunität
- [x] kein automatischer Respawn

## Biom-Bosse
- [x] keine Worldbosse
- [x] exklusives Minecraft-Biom
- [x] interne Spawnprüfung
- [x] 10-%-Spawnchance bei der Spawnprüfung
- [x] festes Boss-Level
- [x] keine Worldboss-Phasenpflicht
- [x] keine Adds
- [x] Biom-Bindung zur Runtime
- [x] thematische Vanilla-Angriffsmuster

## Worldbosse
- [x] sechs finale Worldbosse
- [x] ausschließlich Admin-Event
- [x] Admin-Spawn/Start
- [x] kein automatischer Respawn
- [x] aktive Teilnehmer im Event-Gebiet
- [x] individueller Loot pro aktivem Teilnehmer
- [x] mehrere HP-Phasen
- [x] AoE
- [x] Positionswechsel
- [x] Adds ausschließlich bei Worldbossen
- [x] Projektile
- [x] Slams
- [x] Enrage
- [x] Reaktionsmechaniken
- [x] harte Fehlerbestrafung
- [x] gefährliche temporäre Bereiche
- [x] keine dauerhaften Blockänderungen
- [x] Event-Cleanup

### Aktuelle Worldbosse
- [x] Der Risskoloss
- [x] Der Sturmherrscher
- [x] Der Abgrundfürst
- [x] Der Seelenverschlinger
- [x] Der Endbote
- [x] Der Uralte Weltenwächter

## Altbestand
- [x] Forest Tyrant entfernt
- [x] Frost Sovereign entfernt
- [x] Void Reaper entfernt
- [x] keine alten Testbosse als finales Konzept
- [ ] vollständiger Runtime-E2E-Test aller Bossphasen

---

# 18 – Regions / Mob Scaling — ⚪ VERWORFEN

Dieser Punkt wird nicht umgesetzt.

- [x] eigenes Region-System verworfen
- [x] Region Definitions verworfen
- [x] Mob-Level-Scaling über Regionen verworfen
- [x] Danger Zones verworfen
- [x] Region Rewards verworfen
- [x] Region-Verknüpfung mit Quests verworfen
- [x] Region-Verknüpfung mit Travel verworfen
- [x] RPG-Mob-Zonen verworfen

**Verbindliche Regel:** Minecraft-Biome werden ausschließlich für Biom-Bosse aus 17 verwendet. Es gibt kein separates Regions-/Mob-Scaling-System.

---

# 19 – Gilden — 🟢 98 %

## Gilden-Core
- [x] Gilden-System
- [x] persistente Gilden
- [x] eindeutiger Gildenname
- [x] maximal 50 Mitglieder
- [x] Gildenmeister
- [x] Mitglieder
- [x] keine weiteren Gildenränge
- [x] Gilde erstellen ab Spieler-Level 20
- [x] Gildengründung kostet 2.500 Gold
- [x] Gildengründung über NPC/Dialog
- [x] Gildenfunktion über `minecraft:quick_actions`

## Mitglieder
- [x] Einladen
- [x] `/gildeneinladen <Spieler>`
- [x] `/gildeannehmen`
- [x] `/gildeverlassen`
- [x] `/gildeinfo`
- [x] nur Gildenmeister darf einladen
- [x] Gildenmeister kann nicht einfach selbst verlassen
- [x] Einladungen sind kein dauerhaftes Mitgliedschaftsrecht

## Gildenbank / Basis
- [x] eigene Gildenbank
- [x] Banker-NPC
- [x] 54 Slots
- [x] persistente Speicherung
- [x] Zugriff nur für Mitglieder
- [x] getrennt vom persönlichen Wallet
- [x] Gildenstadt/-basis als manuell erstellte Gemeinschaft
- [x] WorldEdit/WorldGuard für Bau/Schutz
- [x] keine eigene Protection-/Regions-Engine

## Bewusst nicht umgesetzt
- [x] keine Gilden-XP
- [x] keine Gilden-Level
- [x] keine Gildenquests
- [x] keine Gebäude-Freischaltungen
- [x] kein Gildenhandel
- [x] kein eigenes Gilden-PvP-System

---

# 20 – Moderation / Admin — 🟢 98 %

## Zentrale Admin-Schnittstelle
- [x] `/rpgadmin`
- [x] Admin-Unterbefehle
- [x] `rpg.admin`

## NPC
- [x] NPC erstellen
- [x] NPC konfigurieren/bearbeiten
- [x] NPC entfernen/verwalten
- [x] keine manuelle Dateiänderung als Voraussetzung

## Items
- [x] Admin-Give
- [x] Item Inspect
- [x] Items administrativ erstellen
- [x] RPG-Stats administrativ setzen
- [x] Eingabevalidierung

## Quests
- [x] Quest geben
- [x] Quest entfernen/abbrechen
- [x] Quest resetten
- [x] Questfortschritt setzen
- [x] Quest abschließen
- [x] Queststatus einsehen/verwalten

## Bosses / Player
- [x] Boss Management
- [x] Worldboss starten/spawnen
- [x] relevante Spielerdaten einsehen
- [x] relevante Spielerdaten administrieren
- [x] Spielerdaten zurücksetzen
- [x] Level/XP administrieren

## Debug
- [x] `/rpgadmin debug`
- [x] Admin-Zugriff
- [x] gezielte Diagnose-/Testmöglichkeiten
- [ ] vollständiger Admin-E2E-Test aller Unterbefehle

---

# 21 – API / Configuration / Runtime — 🟡 90 %

## API
- [x] PixelRPG Provider
- [x] API Versioning
- [x] Economy API
- [x] Guild API
- [x] Item API
- [x] Party API
- [x] Statistics API
- [x] Custom Events
- [ ] API Documentation vollständig
- [ ] API-Orphans / tote Schnittstellen vollständig prüfen

## Configuration / Language
- [x] Configuration Framework
- [x] Companion Configuration
- [x] Quest Configuration
- [x] Item Configuration
- [x] Economy Configuration
- [x] Language System-Grundlage
- [x] Message Management
- [x] Default Configuration
- [ ] Language Keys gegen Code vollständig prüfen
- [ ] alte Rank/Gem/Rune/Socket/HUD-Texte vollständig entfernen
- [ ] Config Keys gegen Code vollständig abgleichen
- [ ] historische PDC Keys/Datenpfade prüfen

## Runtime / Events / Tasks
- [x] Scheduled Tasks
- [x] Companion Runtime Task
- [x] Quest Passive Task
- [x] Event Listener System
- [x] Player Lifecycle
- [x] Shutdown Cleanup
- [x] Runtime Cleanup
- [x] Error Handling-Grundlage
- [x] Performance Safety-Grundlagen
- [ ] Listener-Orphans prüfen
- [ ] Task-Orphans prüfen
- [ ] vollständiger Runtime-Leak-/Cleanup-Audit

---

# 22 – Testing / Abschluss — 🟡 75 %

## Tests
- [ ] Unit Tests
- [ ] Integration Tests
- [ ] Gameplay Tests
- [ ] automatisierte Regressionstests
- [ ] Performance Tests
- [ ] Edge-Case Tests
- [x] Build / CI-Grundlage
- [ ] vollständige Runtime-Testmatrix

## Verbindliche End-to-End-Testmatrix

- [ ] Registrierung → NPC → Dialog → Quest → Combat → XP → Loot/Reward
- [ ] Registrierung → Companion Unlock → Auswahl → Spawn → Follow → Despawn → erneutes Rufen
- [ ] Spieler-Level 1 → 99
- [ ] Equipment → Stats → Combat → Loot
- [ ] Crafting → Profession → Item → Equipment
- [ ] Quest → Boss → Reward → Companion Unlock
- [ ] Shop / Bank / Travel
- [ ] Party inkl. XP, Loot, Buffs und Questfortschritt
- [ ] Gilden inkl. Erstellung, Einladung, Bank und Persistenz
- [ ] Worldboss inkl. Phasen, Adds, Loot und Cleanup
- [ ] Vanilla-Spieler parallel zu PixelRPG-Spielern
- [ ] Vanilla-Crafting parallel zu PixelRPG-Crafting
- [ ] Server-Neustart → vollständige Persistenz
- [ ] Language / UI
- [ ] Commands / Permissions

## Abschlusskriterium

Ein Roadmap-Punkt wird erst bei **95–98 %** abgeschlossen, wenn der funktionale Umfang umgesetzt, Altbestand bereinigt, Build grün und die relevanten Runtime-/E2E-Tests erfolgreich sind.

---

# Historischer Abgleich

Die heutige Struktur basiert auf der ursprünglichen ausführlichen PixelRPG-Master-Roadmap und der späteren Feature-Roadmap. Die historische Master-Roadmap enthielt deutlich mehr Detail als die zwischenzeitliche Kurzfassung; insbesondere Registrierung/Isolation, NPC-Mannequins, G-Taste, Stats, Health/Armor/Mana, Open World, Dimensionen, dynamische Skalierung, Loot, Crafting, Berufe, NPC-Services, Economy, Quests, Party und Boss-Content wurden ursprünglich einzeln spezifiziert. fileciteturn44file0L3-L7

Die spätere Feature-Roadmap führte die Punkte ebenfalls einzeln von 01 bis 22 und enthielt dafür konkrete Unterpunkte zu Player, Combat, Progression, Companions, Quests, NPC/Dialogue, Items, Equipment, Crafting, Professions, Economy, Trading, Party, Guilds, Bosses, GUI, Persistence, Commands, API, Configuration, Runtime und Testing. fileciteturn43file0L41-L160 fileciteturn43file0L161-L340 fileciteturn43file0L341-L502

Die aktuelle `main`-Roadmap hatte zuletzt nur noch ausgewählte Bereiche ausführlich dargestellt, insbesondere die bereits synchronisierten Quest-/Party-Punkte sowie Bosses, Regions, Gilden und Moderation/Admin. Diese Verkürzung ist mit dieser Datei rückgängig gemacht: **01–22 sind wieder einzeln und dauerhaft sichtbar.**

---

# Übersicht

| # | Bereich | Status |
|---|---|---|
| 01 | Player | 🟢 96 % |
| 02 | Combat | 🟡 90 % |
| 03 | Progression | 🟢 96 % |
| 04 | Companions | 🟢 96 % |
| 05 | Quests | 🟡 90 % |
| 06 | NPC / Dialogue | 🟢 96 % |
| 07 | Items | 🟢 96 % |
| 08 | Equipment | 🟢 95 % |
| 09 | Crafting | 🟢 96 % |
| 10 | Professions | 🟢 96 % |
| 11 | Economy / Gold | 🟢 96 % |
| 12 | Party | 🟢 98 % |
| 13 | Travel / Waypoints | 🟢 96 % |
| 14 | GUI / UI | 🟢 95 % |
| 15 | Database / Persistence | 🟢 95 % |
| 16 | Commands / Permissions | 🟢 96 % |
| 17 | Bosses | 🟢 98 % |
| 18 | Regions / Mob Scaling | ⚪ verworfen |
| 19 | Gilden | 🟢 98 % |
| 20 | Moderation / Admin | 🟢 98 % |
| 21 | API / Configuration / Runtime | 🟡 90 % |
| 22 | Testing / Abschluss | 🟡 75 % |

**Wichtig:** Die Tabelle ist nur die Übersicht. Die Abschnitte 01–22 darüber sind die verbindliche Detail-Roadmap.
