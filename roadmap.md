# PixelRPG – Feature Roadmap

Stand: 2026-08-24 – nach vollständigem Abgleich von 01 – Player

> Diese Datei ist die verbindliche Arbeits-Roadmap. Die Reihenfolge ist von oben nach unten.
> Prozentwerte beschreiben funktionale Reife des aktuell vorhandenen Codes, nicht die Menge an Code.
> 🟢 95–98 % = abgeschlossen, 🟡 50–94 % = in Arbeit, 🔴 0–49 % = offen.

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

## Verbindliche Designentscheidungen

- PixelRPG hat keine Klassen.
- PixelRPG hat keine frei verteilbaren Player-Attribute.
- Ein neu registrierter Spieler erhält keinen Startbonus.
- Level/XP sind die zentrale Spielerprogression.
- Das Level beeinflusst über das Balancing-System die Stärke von gefundenen Waffen, Rüstungen und normalen Companions.
- Combat ist MMORPG-nah: Gear, Stats, Weapon Skills und aktive Companions bestimmen die Kampfstärke.
- Character Stats: HP, Armor, Movement Speed, Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power.
- Rüstung: HP, Armor, Movement Speed.
- Waffen: Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power.
- Equipment: Helm, Brust, Hose, Schuhe, Waffe, Nebenhand.
- Gearscore wird aus Item-Level und Item-Definition/Balancing bestimmt.
- Companions geben passive Stat-Boni ausschließlich solange sie aktiv gerufen und draußen sind. Despawn entfernt den Bonus sofort. Unique Companions sind davon ausgenommen und erhalten ein eigenes System.
- Weapon Skills gehören zur jeweiligen Waffe und können aktiv ausgelöst werden.
- Währung: Goldtaler. Virtuell als Kontostand und physisch als Sonnenblume. Keine weitere Währung.
- Spielerhandel erfolgt über ein Auktionshaus. Items werden gelistet; Geld wird beim Kauf virtuell transferiert.
- Vier Professionen bleiben: BLACKSMITH, PROVISIONER, ALCHEMIST, SCHOLAR.
- Gilden werden nicht als vollständiges Gameplay-System umgesetzt.
- Partys unterstützen gemeinsame XP, Loot-Verteilung, Party-Buffs und gemeinsamen Questfortschritt.
- Alte Default-Bosse Forest Tyrant, Frost Sovereign und Void Reaper werden nicht als finales Bosskonzept fortgeführt. Neue Bosse sollen eigene Attack-Patterns, eigene Größe/Präsenz und Companion-Unlocks besitzen.
- Aktuell existiert keine verbindliche Lore.
- Spieler benötigen keine Gameplay-Commands; Interaktion erfolgt über NPCs/Dialoge. Commands bleiben primär Admin-Funktionen.
- Sprachen: Deutsch, Englisch, Spanisch, Französisch; nur dort einsetzen, wo Mehrsprachigkeit sinnvoll ist.
- Travel bleibt beim bestehenden NPC-/Waypoint-Prinzip: NPC schaltet Ziel frei, erneutes Ansprechen zeigt verfügbare Reiseziele.

---

# 01 – Player — 🟢 98 % ABGESCHLOSSEN

## Profil & Lifecycle
- [x] PlayerProfile / Grunddaten – 98 %
- [x] PlayerProfileManager – 98 %
- [x] PlayerProfileRepository – 98 %
- [x] YAML Player Repository – 98 %
- [x] MySQL Player Repository – 98 %
- [x] Async Pre-Login Load – 98 %
- [x] Join Activation – 98 %
- [x] Quit Deactivation + Save – 98 %
- [x] Registrierungsstatus – 98 %
- [x] Vanilla-/PixelRPG-Spieler-Isolation – 98 %
- [x] Kein Startbonus / kein Startbonus-State – 98 %
- [x] Dirty-State / Save Queue – 98 %
- [x] Emergency YAML Backup bei MySQL-Fehler – 98 %

## Level & XP
- [x] Level 1–99 – 98 %
- [x] Level-100-Grenze / reservierter Zustand – 98 %
- [x] XP-Tabelle / Progressionskurve – 98 %
- [x] Level-99-Transzendenz-Grind – 98 %
- [x] XP Clamp / Overflow-Sicherheit – 98 %
- [x] Level-Up Event – 98 %
- [x] Player-Level API – 98 %
- [x] Level → Balancing-Grundlage für Gear/Companions – 98 %

## Klassen
- [x] Klassen vollständig entfernt
- [x] Warrior entfernt
- [x] Ranger entfernt
- [x] Rogue entfernt
- [x] Healer entfernt
- [x] Mage entfernt
- [x] Klassenwahl entfernt
- [x] Klassenwechsel / Respec entfernt
- [x] Respec-Level-Grenze entfernt
- [x] Respec-Kosten entfernt
- [x] ClassBalance entfernt
- [x] PlayerClassChangeEvent entfernt

## Attribute
- [x] Vitality entfernt
- [x] Agility entfernt
- [x] Precision entfernt
- [x] Range entfernt
- [x] Toughness entfernt
- [x] Soulview entfernt
- [x] Elytra Permit als Player-Attribut entfernt
- [x] Attributpunkte entfernt
- [x] Attributkosten entfernt
- [x] Klassenrabatte entfernt
- [x] Attribute → Stats Pipeline entfernt
- [x] Attribut-Persistenz entfernt

---

# 02 – Combat — 🟡 IN ARBEIT

## Kern
- [ ] zentrale Damage-Logik
- [ ] Player Combat
- [ ] Mob Combat
- [ ] MMORPG-nahes Damage Scaling
- [ ] Stat Scaling
- [ ] Crit Chance
- [ ] Crit Damage
- [ ] Lifesteal
- [ ] Attack Intervals
- [ ] Combat State / Target State
- [ ] Combat Cleanup
- [ ] Combat Events
- [ ] Vanilla-/PixelRPG-Mischbetrieb sauber abgrenzen

## Weapon Skills
- [x] Rechtsklick-Erkennung
- [ ] WeaponAbilityEngine vollständig auf neues Combat-Modell ausrichten
- [ ] Weapon Ability Metadata auf Items
- [ ] Waffen-Cooldowns
- [ ] Ability Level Requirement
- [ ] konkrete Weapon Skills
- [x] Player-Quit-Cooldown-Cleanup

## Mob Scaling
- [ ] Mob-Level Scaling
- [ ] HP Scaling
- [ ] Damage Scaling
- [ ] Region/Danger Provider
- [ ] Dimension Scaling
- [ ] Player-Parity
- [ ] XP aus Mob-Health
- [ ] Original-Mob-Attribute Tracking

## Loot / Death
- [ ] Mob Experience
- [ ] Loot Drop Chance
- [ ] Random Item Rarity Roll
- [ ] Item Drop Pool
- [ ] Physische Goldtaler-Drops
- [ ] Soulbound Death Protection
- [ ] Death-/Respawn-Integration
- [ ] Mob Nameplates

---

# 03 – Progression / Stats — 🟡 IN ARBEIT

- [ ] Level 1–99 End-to-End
- [ ] XP Scaling
- [ ] deterministische Progression
- [ ] StatEngine
- [ ] Cached Player Stats
- [ ] Max Health
- [ ] Armor
- [ ] Movement Speed
- [ ] Block Reach / Entity Reach
- [ ] Damage
- [ ] Crit Chance
- [ ] Crit Damage
- [ ] Lifesteal
- [ ] Attack Power
- [ ] Equipment → Stats → Combat
- [ ] Companion → Stats → Combat
- [ ] StatisticsService
- [ ] StatisticsAPI
- [ ] Stat-Persistenz, soweit für Stats überhaupt erforderlich
- [ ] alte Strength/Agility/Stamina/Intellect-/Spell-Power-Pfade vollständig entfernen
- [ ] alte Player-Attribute-Pfade vollständig entfernen

## Finaler Character-Stat-Satz

```text
HP
Armor
Movement Speed
Reach
Damage
Crit
Crit-Schaden
Lifesteal
Attack Power
```

---

# 04 – Companions — 🟢 95–98 % CORE ABGESCHLOSSEN

> Neue Companion-Inhalte sind vorerst nicht geplant. Änderungen nur bei Bugs oder bewusst beschlossenen Systemänderungen.

## Core
- [x] Registry
- [x] Definitions
- [x] Ownership
- [x] Unlocks
- [x] Unique Unlocks
- [x] Aktivieren / Deaktivieren
- [x] Spawn / Despawn
- [x] Logout = Despawn
- [x] Login = kein automatisches Wiederbeschwören
- [x] Follow / Movement
- [x] Passive Stats nur bei aktivem Companion
- [x] Companion Stats Calculator
- [x] Leveling / XP
- [x] normale Companion-Level an Spielerlevel gekoppelt
- [x] Unique Companion eigenes Level
- [x] Combat-Regeln
- [x] Companion Equipment / Persistence
- [x] Runtime Registry / Cleanup
- [x] Boss → Companion Unlock

## Passive Companion-Boni

- [ ] HP
- [ ] Armor
- [ ] Movement Speed
- [ ] Reach
- [ ] Damage
- [ ] Crit
- [ ] Crit-Schaden
- [ ] Lifesteal
- [ ] Attack Power

> Diese Boni dürfen ausschließlich aktiv sein, solange der jeweilige Companion tatsächlich gerufen und draußen ist. Unique Companions erhalten eine eigene Regelung.

## Companion Abilities

- [ ] CompanionAbilityEngine
- [ ] Ability Definitions
- [ ] aktive Companion-Abilities
- [ ] passive Ability Definitions
- [ ] Cooldowns
- [ ] Runtime Integration

---

# 05 – Quests — 🟡 IN ARBEIT

## Verbindliche Questtypen
- [ ] HUNT
- [ ] COLLECT
- [ ] TALK_TO_NPC – technisch implementieren, zunächst kein Content
- [ ] REACH_LOCATION – zunächst nur vorhandene Minecraft-Strukturen
- [ ] GLOBAL_EVENT – technisch implementieren, zunächst kein Content

> ESCORT ist aktuell nicht Bestandteil des gewünschten Questumfangs und wird nicht als aktiver Content-Typ priorisiert.

## Core
- [ ] Quest Definition
- [ ] Quest Repository
- [ ] Quest Manager
- [ ] Quest Progress
- [ ] Quest Completion
- [ ] Quest Rewards
- [ ] Quest XP
- [ ] Quest XP Scaling
- [ ] maximal 5 aktive Quests
- [ ] Quest Persistence
- [ ] QuestCompletedEvent
- [ ] Mob-Kill-Tracking
- [ ] Quest Navigation
- [ ] Questketten
- [ ] Party Quest Share

---

# 06 – NPC / Dialogue — 🟡 IN ARBEIT

## NPC-Typen
- [ ] Banker
- [ ] Blacksmith
- [ ] Filler
- [ ] Profession Trainer
- [ ] Quest NPC
- [ ] Reception
- [ ] Shop
- [ ] Story
- [ ] Travel

## Dialogue
- [ ] Native Minecraft/Paper Dialog Framework
- [ ] NPC → Dialogue
- [ ] Quest Dialogue
- [ ] Profession Dialogue
- [ ] Shop Dialogue
- [ ] Bank Dialogue
- [ ] Travel Dialogue
- [ ] Story Dialogue
- [ ] Quick Actions
- [ ] Reception Dialogue

> Filler-NPCs müssen später für `Talk to Filler(id)` und `Bring X to Filler(id)`-Questlogik verwendbar sein.

---

# 07 – Items — 🟡 IN ARBEIT

- [ ] Item IDs
- [ ] Unique Item IDs
- [ ] Item Kategorien
- [ ] Gear Kategorien
- [ ] Validierung
- [ ] Raritäten: Common, Uncommon, Rare, Epic, Legendary, Unique
- [ ] Item Level
- [ ] Levelanforderungen
- [ ] Item Stats
- [ ] Gearscore
- [ ] Soulbound
- [ ] Weapon Skills
- [ ] Shoppreise
- [ ] Resourcepack Integration

## Gear-Balancing

- [ ] Rüstung: HP, Armor, Movement Speed
- [ ] Waffen: Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power
- [ ] Gearscore aus Item-Level + Item-Definition/Balancing
- [ ] Level des Spielers bestimmt über Balancing die erreichbare Gear-Stärke

---

# 08 – Equipment — 🟡 IN ARBEIT

- [ ] Helm
- [ ] Brust
- [ ] Hose
- [ ] Schuhe
- [ ] Waffe
- [ ] Nebenhand
- [ ] Equipment Stats
- [ ] Level Requirements
- [ ] Rarity
- [ ] Equipment → Player Stats
- [ ] Persistence
- [ ] Equipment GUI

---

# 09 – Crafting — 🟡 IN ARBEIT

- [ ] Recipe Registry
- [ ] Crafting Service
- [ ] Crafting Definitions
- [ ] Crafted Item Factory
- [ ] Berufsanforderungen
- [ ] Rezeptfreischaltungen
- [ ] Dialogue-Crafting
- [ ] Crafting GUI
- [ ] Blacksmith GUI
- [ ] möglichst breite Abdeckung der Minecraft-Herstellungsarten, sinnvoll auf die vier Berufe verteilt

---

# 10 – Professions — 🟡 IN ARBEIT

## Verbindliche Berufe
- [ ] BLACKSMITH
- [ ] PROVISIONER
- [ ] ALCHEMIST
- [ ] SCHOLAR

- [ ] Level-System 1–100 prüfen/umsetzen
- [ ] Profession XP
- [ ] Rezepte / Freischaltungen
- [ ] Trainer-Integration
- [ ] Crafting-Integration
- [ ] Persistence

---

# 11 – Economy / Shop / Bank — 🟡 IN ARBEIT

## Währung
- [ ] Goldtaler als einzige Währung
- [ ] virtueller Kontostand
- [ ] physischer Goldtaler als Sonnenblume
- [ ] keine weiteren Währungen

## Shop
- [ ] ShopManager
- [ ] ShopEntries
- [ ] NPC-Shops
- [ ] Persistence
- [ ] Shop GUI
- [ ] Shop Editor
- [ ] Admin Commands

## Bank
- [ ] persönlicher Bank-Speicher
- [ ] 2 Seiten × 54 Slots
- [ ] unabhängig von Vanilla-Endertruhe
- [ ] Persistence

---

# 12 – Auction House — 🔴 OFFEN

> Direktes Spieler-zu-Spieler-Trading wird nicht umgesetzt.

- [ ] Items einstellen
- [ ] Items suchen / kaufen
- [ ] virtuelle Zahlung beim Kauf
- [ ] Verkäufer erhält Gold virtuell
- [ ] Listing Persistence
- [ ] Ablauf / Entfernung von Listings
- [ ] Item Validation
- [ ] Anti-Duplication
- [ ] Disconnect-/Recovery-Sicherheit
- [ ] GUI

---

# 13 – Party — 🟡 IN ARBEIT

- [ ] Party erstellen
- [ ] Leader
- [ ] Mitglieder
- [ ] Invite
- [ ] Leave
- [ ] Leader Promotion
- [ ] Disconnect Cleanup
- [ ] gemeinsame XP
- [ ] Loot-Verteilung
- [ ] Party-Buffs
- [ ] gemeinsamer Questfortschritt
- [ ] Party GUI
- [ ] Party HUD

---

# 14 – Guilds — 🔴 NICHT GEPLANT

> Es wird kein vollständiges Guild-Gameplay-System geben.
>
> Bestehende technische Guild-/Currency-Altpfade werden bei den jeweiligen Systemarbeiten entfernt oder entkoppelt, sobald sie nicht mehr benötigt werden.

---

# 15 – Bosses — 🔴 NEUES SYSTEM ERFORDERLICH

> Forest Tyrant, Frost Sovereign und Void Reaper stammen aus dem alten System und sind kein verbindlicher finaler Boss-Content.

- [ ] neue Boss Definition
- [ ] Boss Manager
- [ ] Spawn-System
- [ ] Spawn Chance / Regeln
- [ ] eigene Größe gegenüber normalen Mobs
- [ ] eigene Attack Patterns
- [ ] Phasen / Mechaniken nach Bedarf
- [ ] Enrage nach Bedarf
- [ ] Adds nach Bedarf
- [ ] Loot
- [ ] XP
- [ ] Gold
- [ ] Companion Unlocks
- [ ] Boss Statistics

## Boss-Companion-Prinzip

```text
Boss besiegen → Companion Unlock
Beispiel: Chicken Boss → Chicken Companion
```

---

# 16 – Story — 🔴 OFFEN

> Aktuell existiert keine verbindliche Lore.

- [ ] Welt/Lore definieren
- [ ] Hauptgeschichte definieren
- [ ] Kapitel
- [ ] Story Dialogue
- [ ] Story Persistence
- [ ] Story Rewards

---

# 17 – GUI / UI — 🟡 IN ARBEIT

- [ ] Character Card
- [ ] Crafting GUI
- [ ] Blacksmith GUI
- [ ] Party GUI
- [ ] Quest Log
- [ ] Quest Details
- [ ] Shop GUI
- [ ] Shop Editor
- [ ] Companion UI
- [ ] Bank GUI
- [ ] native Dialog UI

## Character Card

```text
Level
EXP
HP
Armor
Movement Speed
Reach
Damage
Crit
Crit-Schaden
Lifesteal
Attack Power
```

---

# 18 – Database / Persistence / Storage — 🟡 IN ARBEIT

- [ ] YAML
- [ ] MySQL
- [ ] HikariCP
- [ ] Save Queues
- [ ] Shutdown Save
- [ ] Emergency Backup
- [ ] Player Persistence
- [ ] Companion Persistence
- [ ] Quest Persistence
- [ ] Profession Persistence
- [ ] Story Persistence
- [ ] Shop Persistence
- [ ] Bank Persistence
- [ ] Auction-House Persistence

---

# 19 – Commands / Permissions — 🟡 IN ARBEIT

> Keine normalen Spieler-Gameplay-Commands. Spieler benutzen NPCs/Dialoge.

- [ ] Admin Commands
- [ ] `rpg.admin`
- [ ] notwendige System-/Debug-Commands
- [ ] NPC/Boss/Shop/Quest/Companion Administration

---

# 20 – API / Events — 🟡 IN ARBEIT

## Erhalten / benötigt
- [ ] PlayerLevelUpEvent
- [ ] PlayerRegistrationEvent
- [ ] PlayerUnregistrationEvent
- [ ] QuestCompletedEvent
- [ ] BossDefeatedEvent
- [ ] Party Events
- [ ] saubere Player-Level API
- [ ] saubere Stat API
- [ ] saubere Economy API

> Klassen- und Attribut-Events/APIs sind entfernt und dürfen nicht wieder eingeführt werden.

---

# 21 – Configuration / Language / Data — 🟡 IN ARBEIT

- [ ] `config.yml`
- [ ] Balancing-Konfiguration
- [ ] Item Scaling
- [ ] Mob Scaling
- [ ] Quest-Daten
- [ ] Companion-Daten
- [ ] Boss-Daten
- [ ] Sprachdateien
- [ ] Datenvalidierung
- [ ] Fallbacks

## Sprachen

- [ ] Deutsch
- [ ] Englisch
- [ ] Spanisch
- [ ] Französisch

> Mehrsprachigkeit nur an Stellen einsetzen, an denen sie tatsächlich sinnvoll ist.

---

# 22 – Scoreboard / Playtime — 🟡 IN ARBEIT

## Scoreboard

- [ ] Level
- [ ] Quest-/Progressionsinformationen
- [ ] Partymitglieder
- [ ] Geld
- [ ] getötete Gegner
- [ ] Tode

## Playtime

- [ ] Playtime Tracking
- [ ] Persistence
- [ ] Anzeige/Integration nach Bedarf

---

# 23 – Travel — 🟢 BESTEHENDES KONZEPT BEIBEHALTEN

- [x] Travel NPC
- [x] Waypoint Freischaltung
- [x] erneutes Ansprechen zeigt verfügbare Ziele
- [x] Teleport zu freigeschalteten Zielen
- [ ] vollständige E2E-Prüfung
- [ ] Persistence-Prüfung
- [ ] Permissions/Distance-Prüfung

---

# 24 – Runtime / Events / Tasks / Cleanup — 🟡 IN ARBEIT

- [ ] Plugin Initialisierung
- [ ] Services
- [ ] Listener
- [ ] Tasks
- [ ] Companion Runtime
- [ ] Mob Scaling
- [ ] NPC Tasks
- [ ] Boss Tasks
- [ ] Scoreboard
- [ ] Autosaves
- [ ] Cleanup
- [ ] Memory Safety
- [ ] Disconnect-/Logout Edge Cases

---

# 25 – Build / CI / Testing — 🟡 IN ARBEIT

## Verbindliche Plattform

- [x] Java 25
- [x] Paper 26.x
- [x] Paper 26.2 als aktuelle Zielplattform
- [x] paperweight-userdev 2.0.0-beta.21
- [x] Mojang-Mappings
- [x] `paper-plugin.yml`
- [x] ShadowJar

## Qualität

- [ ] Produktions-Buildprüfung
- [ ] GitHub Build Workflow
- [ ] Artifact Verification
- [ ] Unit Tests
- [ ] Integration Tests
- [ ] Gameplay Tests
- [ ] Regression Tests
- [ ] Performance Tests
- [ ] Persistence Tests
- [ ] Multiplayer/Load Tests

---

# Aktueller Arbeitsstand

## Abgeschlossen

- **01 – Player: 98 % / abgeschlossen**

## Als Nächstes

- **02 – Combat**
- danach **03 – Progression / Stats**
- danach weitere Roadmap-Punkte in Reihenfolge

> Bei jeder Bearbeitung gilt: Wenn dabei veralteter Code entdeckt wird, der dem gemeinsam festgelegten Soll widerspricht, wird er im selben Arbeitsschritt sauber entfernt. Altbestand wird nicht bewusst liegen gelassen.
