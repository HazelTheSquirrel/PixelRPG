# PixelRPG – Feature Roadmap

Stand: 2026-08-24 – nach vollständigem Abgleich von 01 – Player und 02 – Combat

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
- Vanilla-Schaden bleibt Bestandteil der Berechnung; Vanilla-Angriffsgeschwindigkeit bleibt unverändert.
- Character Stats: HP, Armor, Movement Speed, Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power.
- Basis-Crit-Chance ist 0 %. Crit-Chance kommt über Waffen/aktive Companion-Boni.
- Standard-Crit-Schaden ist ×2 und kann durch Gear/Companions erhöht werden.
- Lifesteal heilt den entsprechenden Prozentsatz des tatsächlich verursachten Schadens.
- Rüstung wird über eine eigene MMORPG-Mitigation berechnet.
- Rüstung: HP, Armor, Movement Speed.
- Waffen: Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power.
- Equipment: Helm, Brust, Hose, Schuhe, Waffe, Nebenhand.
- Gearscore wird aus Item-Level und Item-Definition/Balancing bestimmt.
- Companions geben passive Stat-Boni ausschließlich solange sie aktiv gerufen und draußen sind. Despawn entfernt den Bonus sofort. Unique Companions sind davon ausgenommen und erhalten ein eigenes System.
- Weapon Skills gehören zur jeweiligen Waffe und können aktiv ausgelöst werden. Materialbasierte Waffen erhalten unterschiedliche Skills; Bogen/Armbrust verwenden für Skills Shift+Rechtsklick, damit Vanilla-Spannen/Laden erhalten bleibt.
- PvP ist kein eigenes Gameplay-System. Falls PvP trotzdem stattfindet, wird es mit dem normalen PixelRPG-Combat-Verhalten berechnet.
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

# 02 – Combat — 🟢 98 % ABGESCHLOSSEN

## Kern
- [x] zentrale Damage-Logik
- [x] Player Combat
- [x] Mob Combat
- [x] MMORPG-nahes Damage Scaling
- [x] Vanilla-Schaden bleibt Basisbestandteil
- [x] Vanilla-Attack-Intervalle unverändert
- [x] Stat Scaling
- [x] Crit Chance mit 0 % Basis
- [x] Crit Damage mit ×2 Basis
- [x] Lifesteal
- [x] eigene Armor-Mitigation
- [x] Combat State / Target State
- [x] Combat Cleanup
- [x] Combat Events
- [x] Vanilla-/PixelRPG-Mischbetrieb sauber abgrenzen
- [x] PvP nutzt bei tatsächlicher Aktivierung das normale PixelRPG-Combat-Verhalten

## Weapon Skills
- [x] Rechtsklick-Erkennung
- [x] WeaponAbilityEngine auf neues Combat-Modell ausgerichtet
- [x] Weapon Ability Metadata / explizite Ability Overrides
- [x] Waffen-Cooldowns
- [x] Ability Level Requirement
- [x] materialbasierte Weapon Skills
- [x] Holz, Stein, Kupfer, Eisen, Gold, Diamant, Netherite mit unterschiedlichen Skills
- [x] Bogen- und Armbrust-Skillpfad
- [x] Shift+Rechtsklick für Bogen/Armbrust, um Vanilla-Interaktion nicht zu zerstören
- [x] Player-Quit-Cooldown-Cleanup
- [x] Weapon Skill Damage Context verhindert doppelte Weapon-Power-Anwendung

## Mob Scaling
- [x] Mob-Level = aktives Spielerlevel
- [x] HP Scaling
- [x] Damage Scaling
- [x] Spieler-Parity
- [x] Ausrüstung beeinflusst die Mob-Stärke
- [x] mehrere aktive Spieler werden berücksichtigt
- [x] Original-Mob-Attribute Tracking
- [x] Combat-Timeout und Wiederherstellung der Vanilla-Mobwerte
- [x] Region/Danger Scaling entfernt
- [x] RegionDangerProvider entfernt
- [x] keine künstlichen Region-/Dimensions-Level mehr
- [x] XP aus Mob-Health vorhanden

## Loot / Death
- [x] Mob Experience
- [x] Loot Drop Chance
- [x] Random Item Rarity Roll
- [x] Unique wird niemals zufällig gerollt
- [x] Item Drop Pool
- [x] Physische Goldtaler-Drops
- [x] Soulbound Death Protection
- [x] Death-/Respawn-Integration
- [x] Mob Nameplates

---

# 03 – Progression / Stats — 🟡 IN ARBEIT

- [ ] Level 1–99 End-to-End
- [ ] XP Scaling
- [ ] deterministische Progression
- [x] StatEngine vorhanden und auf neue Gear-Stats ausgerichtet
- [x] Cached Player Stats
- [x] Max Health
- [x] Armor
- [x] Movement Speed
- [x] Block Reach / Entity Reach
- [x] Damage
- [x] Crit Chance
- [x] Crit Damage
- [x] Lifesteal
- [x] Attack Power
- [x] Equipment → Stats → Combat Grundpfad
- [ ] Companion → Stats → Combat vollständig integrieren
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
- [x] Item Stats für aktuelle Weapon-/Armor-Berechnung vorhanden
- [ ] Gearscore
- [ ] Soulbound
- [x] Weapon Skill Mapping
- [ ] Shoppreise
- [ ] Resourcepack Integration

## Gear-Balancing

- [x] Rüstung: HP, Armor, Movement Speed
- [x] Waffen: Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power
- [ ] Gearscore aus Item-Level + Item-Definition/Balancing
- [x] Level des Spielers bestimmt über Balancing die erreichbare Gear-Stärke

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
- [x] Equipment → Player Stats Grundpfad
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
- [ ] neue Boss Registry
- [ ] neue Spawn-Logik
- [ ] deutlich größere Boss-Präsenz als normale Monster
- [ ] eigene Attack Patterns
- [ ] Boss Phasen
- [ ] Enrage / Spezialmechaniken
- [ ] Adds, sofern sinnvoll
- [ ] Boss Loot
- [ ] Boss XP
- [ ] Companion Unlocks über Bosse
- [ ] neue Companion-Bosse, z. B. Chicken Boss → Chicken Companion
- [ ] Boss Statistics

---

# 16 – Story — 🔴 CONTENT OFFEN

> Aktuell existiert keine verbindliche Lore.

## Technik
- [x] Story Manager Grundstruktur
- [x] Story Chapter Grundstruktur
- [x] Story Persistence Grundstruktur
- [ ] vollständige Story E2E
- [ ] Story Books
- [ ] Story NPC Dialogue
- [ ] Story Quest Integration

## Content
- [ ] Welt/Lore definieren
- [ ] Hauptgeschichte definieren
- [ ] Kapitel definieren
- [ ] zentrale Charaktere definieren
- [ ] Story-NPCs definieren
- [ ] Story-Quests definieren

---

# 17 – GUI / UI — 🟡 IN ARBEIT

- [ ] Crafting GUI
- [ ] Blacksmith GUI
- [ ] Party GUI
- [ ] Quest Log
- [ ] Quest Details
- [ ] Shop GUI
- [ ] Shop Editor
- [ ] Companion UI
- [ ] Bank GUI
- [ ] Native Dialog UI
- [ ] Character Card
- [ ] Scoreboard

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

- [x] YAML Storage
- [x] MySQL Storage
- [x] HikariCP
- [x] Async Persistence Grundstruktur
- [x] Save Queue Grundstruktur
- [x] Shutdown Save
- [x] Emergency Backup
- [ ] vollständiger Persistenz-Audit für alle späteren Systeme
- [ ] Migrationen
- [ ] Recovery / Corruption Handling
- [ ] Storage Performance Audit

---

# 19 – Commands / Permissions — 🟡 IN ARBEIT

> Spieler benötigen keine Gameplay-Commands. Interaktion erfolgt über NPCs/Dialoge.

## Admin
- [ ] `/rpgadmin`
- [ ] Companion Admin
- [ ] NPC Admin
- [ ] Boss Admin
- [ ] Quest Admin
- [ ] Shop Admin
- [ ] Crafting Admin

## Permissions
- [ ] `rpg.admin`
- [ ] `rpg.member`
- [ ] Admin-Schutz für alle administrativen Systeme

---

# 20 – API / Events — 🟡 IN ARBEIT

## Player
- [x] PlayerLevelUpEvent
- [x] PlayerRegistrationEvent
- [x] PlayerUnregistrationEvent

## Combat
- [x] PlayerCombatEnterEvent
- [x] PlayerCombatExitEvent
- [ ] weitere Combat Events nur bei tatsächlichem Bedarf

## Quests / Bosses
- [x] QuestCompletedEvent
- [x] BossDefeatedEvent

## API
- [ ] stabile öffentliche API prüfen
- [ ] API-Versionierung
- [ ] Event Contracts dokumentieren
- [ ] keine unnötigen APIs veröffentlichen

---

# 21 – Configuration / Language / Data — 🟡 IN ARBEIT

- [x] config.yml Grundstruktur
- [x] JSON Data Manager
- [x] Item Scaling Data
- [x] Mob Scaling Data
- [x] Companion Data
- [x] Quest Data
- [x] Deutsch
- [x] Englisch
- [x] Spanisch
- [x] Französisch
- [ ] vollständige Übersetzungsabdeckung
- [ ] fehlende Keys automatisch erkennen
- [ ] Fallback-Verhalten prüfen
- [ ] Konfigurationsvalidierung

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
- [ ] Disconnect Cleanup
- [ ] Anzeige nur bei registrierten PixelRPG-Spielern

---

# 23 – Travel — 🟡 IN ARBEIT

- [x] NPC-basiertes Travel-Grundprinzip
- [x] Waypoint Unlock
- [x] NPC erneut ansprechen → verfügbare Reiseziele
- [x] Teleport zu freigeschalteten Zielen
- [ ] vollständige Travel-Persistence prüfen
- [ ] Travel Permissions
- [ ] Distanz-/Cooldown-Regeln prüfen
- [ ] Dialog-Integration vollständig prüfen

---

# 24 – Runtime / Events / Tasks / Cleanup — 🟡 IN ARBEIT

- [x] Plugin Initialisierung
- [x] Service Lifecycle Grundstruktur
- [x] Listener Registration
- [x] Scheduled Tasks
- [x] Companion Runtime
- [x] Mob Scaling Runtime Cleanup
- [x] Combat State Cleanup
- [ ] vollständiger Shutdown Audit
- [ ] Task Leak Audit
- [ ] Listener Leak Audit
- [ ] Memory Safety Audit
- [ ] Runtime Performance Audit

---

# 25 – Build / CI / Testing — 🟡 IN ARBEIT

## Verbindliche Plattform
- [x] Java 25
- [x] Paper 26.2
- [x] Paperweight Userdev 2.0.0-beta.21
- [x] Mojang-Mappings
- [x] paper-plugin.yml
- [x] ShadowJar

## CI
- [x] GitHub Build Workflow Grundstruktur
- [ ] Produktions-Buildprüfung
- [ ] Artifact Verification
- [ ] reproduzierbarer Build

## Tests
- [ ] Unit Tests
- [ ] Integration Tests
- [ ] Gameplay Tests
- [ ] Combat Regression Tests
- [ ] Persistence Tests
- [ ] Multiplayer / Party Tests
- [ ] Companion Regression Tests
- [ ] Boss Tests
- [ ] Performance Tests
- [ ] Edge-Case Tests
- [ ] Level-1-bis-99 Scaling Tests

---

# Aktueller Arbeitsstand

```text
01 – Player       🟢 98 %
02 – Combat       🟢 98 %
03 – Progression / Stats   🟡
04 – Companions   🟢 95–98 % Core
05 – Quests       🟡
06 – NPC/Dialog   🟡
07 – Items        🟡
08 – Equipment    🟡
09 – Crafting     🟡
10 – Professions  🟡
11 – Economy      🟡
12 – Auction House 🔴
13 – Party        🟡
14 – Guilds       🔴 nicht geplant
15 – Bosses       🔴
16 – Story        🔴
17 – GUI/UI       🟡
18 – Storage      🟡
19 – Commands     🟡
20 – API/Events   🟡
21 – Config       🟡
22 – Scoreboard   🟡
23 – Travel       🟡
24 – Runtime      🟡
25 – Build/Test   🟡
```

> Nächster verbindlicher Arbeitsblock: **03 – Progression / Stats**.
