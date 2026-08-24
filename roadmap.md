# PixelRPG – Feature Roadmap

Stand: 2026-08-24 – nach aktuellem Repository-Abgleich und erfolgreichem Gradle-Build

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

## Verbindliche technische Basis

- Java 25
- Paper 26.x, aktuell verbindliches Ziel: Paper 26.2
- paperweight-userdev 2.0.0-beta.21
- paperweight.paperDevBundle("26.2.build.+")
- Mojang-Mappings
- paper-plugin.yml
- native Minecraft/Paper-26.2 Dialogsystem
- Adventure Components
- kein ChatColor
- keine alten 1.21.x-APIs oder Dialogimplementierungen

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
- Companions geben passive Stat-Boni ausschließlich solange sie aktiv gerufen und draußen sind. Despawn entfernt den Bonus sofort. Unique Companions sind davon ausgenommen.
- Weapon Skills gehören zur jeweiligen Waffe und können aktiv ausgelöst werden. Materialbasierte Waffen erhalten unterschiedliche Skills; Bogen/Armbrust verwenden für Skills Shift+Rechtsklick, damit Vanilla-Spannen/Laden erhalten bleibt.
- PvP ist kein eigenes Gameplay-System. Falls PvP stattfindet, wird normales PixelRPG-Combat-Verhalten verwendet.
- Währung: Goldtaler. Virtuell als Kontostand und physisch als Sonnenblume. Keine weitere Währung.
- Spielerhandel erfolgt über ein Auktionshaus; vollständiger Player-Trade ist nicht priorisiert.
- Vier Professionen bleiben: BLACKSMITH, PROVISIONER, ALCHEMIST, SCHOLAR.
- Gilden werden nicht als vollständiges Gameplay-System umgesetzt.
- Partys unterstützen gemeinsame XP, Loot-Verteilung, Party-Buffs und gemeinsamen Questfortschritt.
- Alte Default-Bosse Forest Tyrant, Frost Sovereign und Void Reaper werden nicht als finales Bosskonzept fortgeführt.
- Aktuell existiert keine verbindliche Lore.
- Spieler benötigen keine Gameplay-Commands; Interaktion erfolgt über NPCs/Dialoge. Commands bleiben primär Admin-Funktionen.
- Sprachen: Deutsch, Englisch, Spanisch, Französisch; nur dort einsetzen, wo Mehrsprachigkeit sinnvoll ist.
- Travel bleibt beim bestehenden NPC-/Waypoint-Prinzip: NPC schaltet Ziel frei, erneutes Ansprechen zeigt verfügbare Reiseziele.

---

# 01 – Player — 🟢 98 % ABGESCHLOSSEN

## Profil & Lifecycle
- [x] PlayerProfile / Grunddaten
- [x] PlayerProfileManager
- [x] PlayerProfileRepository
- [x] YAML Player Repository
- [x] MySQL Player Repository
- [x] Async Pre-Login Load
- [x] Join Activation
- [x] Quit Deactivation + Save
- [x] Registrierungsstatus
- [x] Vanilla-/PixelRPG-Spieler-Isolation
- [x] Kein Startbonus / kein Startbonus-State
- [x] Dirty-State / Save Queue
- [x] Emergency YAML Backup bei MySQL-Fehler

## Level & XP
- [x] Level 1–99
- [x] Level-100-Grenze / reservierter Zustand
- [x] XP-Tabelle / Progressionskurve
- [x] Level-99-Transzendenz-Grind
- [x] XP Clamp / Overflow-Sicherheit
- [x] Level-Up Event
- [x] Player-Level API
- [x] Level → Balancing-Grundlage für Gear/Companions

## Klassen & alte Player-Attribute
- [x] Klassen vollständig entfernt
- [x] Warrior / Ranger / Rogue / Healer / Mage entfernt
- [x] Klassenwahl entfernt
- [x] Klassenwechsel / Respec entfernt
- [x] Respec-Level-Grenze entfernt
- [x] Respec-Kosten entfernt
- [x] ClassBalance entfernt
- [x] PlayerClassChangeEvent entfernt
- [x] Vitality / Agility / Precision / Range / Toughness / Soulview entfernt
- [x] Elytra Permit als Player-Attribut entfernt
- [x] Attributpunkte / Attributkosten / Klassenrabatte entfernt
- [x] alte Attribute → Stats Pipeline entfernt
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
- [x] Vanilla-/PixelRPG-Mischbetrieb sauber abgegrenzt
- [x] PvP nutzt bei tatsächlicher Aktivierung normales PixelRPG-Combat-Verhalten

## Weapon Skills
- [x] Rechtsklick-Erkennung
- [x] WeaponAbilityEngine auf neues Combat-Modell ausgerichtet
- [x] Weapon Ability Metadata / explizite Ability Overrides
- [x] Waffen-Cooldowns
- [x] Ability Level Requirement
- [x] materialbasierte Weapon Skills
- [x] Holz, Stein, Kupfer, Eisen, Gold, Diamant, Netherite mit unterschiedlichen Skills
- [x] Bogen- und Armbrust-Skillpfad
- [x] Shift+Rechtsklick für Bogen/Armbrust
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

> Nächster Hauptarbeitsabschnitt.

## Progression
- [ ] Level 1–99 End-to-End gegen finale Stat-/Gear-Pipeline validieren
- [ ] XP Scaling final validieren
- [ ] deterministische Progression final validieren
- [x] Player-Level API
- [x] Level-Up Events

## Character Stats
- [x] StatEngine vorhanden
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
- [x] StatisticsAPI vorhanden
- [ ] Companion → Stats → Combat vollständig validieren
- [ ] StatisticsService vollständig gegen finale Runtime validieren
- [ ] Stat-Persistenz nur dort ergänzen, wo fachlich erforderlich
- [ ] alle verbliebenen alten Strength/Agility/Stamina/Intellect-/Spell-Power-Pfade vollständig entfernen
- [ ] alle verbliebenen alten Player-Attribute-Pfade vollständig entfernen

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

# 04 – Companions — 🟢 98 % CORE ABGESCHLOSSEN

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
- [x] Companion Stats Runtime-Anbindung
- [x] Leveling / XP
- [x] normale Companion-Level an Spielerlevel gekoppelt
- [x] Unique Companion eigenes Level
- [x] Combat-Regeln
- [x] Companion Equipment / Persistence
- [x] Runtime Registry / Cleanup
- [x] Boss → Companion Unlock

## Passive Companion-Boni

- [x] HP
- [x] Armor
- [x] Movement Speed
- [x] Reach
- [x] Damage
- [x] Crit
- [x] Crit-Schaden
- [x] Lifesteal
- [x] Attack Power
- [x] Bonus nur bei tatsächlich aktivem/gespawntem Companion
- [x] Bonus wird bei Despawn entfernt
- [x] Logout entfernt Runtime-Companion und dessen aktive Boni

> Unique Companions behalten ihre eigene Sonderregelung.

## Companion Abilities

- [x] CompanionAbilityEngine-Grundstruktur
- [x] generische Cooldown-Verwaltung
- [x] Ability Definitions als Datenbasis
- [ ] aktive Companion-Abilities mit tatsächlichen Gameplay-Effekten
- [ ] passive Ability Definitions mit tatsächlichen Gameplay-Effekten
- [ ] vollständige Runtime-Integration der Abilities

---

# 05 – Quests — 🟡 IN ARBEIT

## Verbindliche Questtypen
- [ ] HUNT
- [ ] COLLECT
- [ ] TALK_TO_NPC – technisch implementieren, zunächst kein Content
- [ ] REACH_LOCATION – zunächst nur vorhandene Minecraft-Strukturen
- [ ] GLOBAL_EVENT – technisch implementieren, zunächst kein Content

> ESCORT wird aktuell nicht als aktiver Content-Typ priorisiert.

## Core
- [ ] Quest Definition finalisieren
- [ ] Quest Repository
- [ ] Quest Manager
- [ ] Quest Progress
- [ ] Quest Completion
- [ ] Quest Rewards
- [ ] Quest XP
- [ ] Quest XP Scaling
- [ ] maximal 5 aktive Quests
- [ ] Quest Persistence vollständig validieren
- [x] QuestCompletedEvent vorhanden
- [ ] Mob-Kill-Tracking vollständig validieren
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
- [x] Native Minecraft/Paper Dialog Framework als Basis
- [ ] NPC → Dialogue vollständig validieren
- [ ] Quest Dialogue
- [ ] Profession Dialogue
- [ ] Shop Dialogue
- [ ] Bank Dialogue
- [ ] Travel Dialogue
- [ ] Story Dialogue
- [ ] Companion Dialogue
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
- [x] Raritäten: Common, Uncommon, Rare, Epic, Legendary, Unique
- [ ] Item Level
- [ ] Levelanforderungen
- [x] Item Stats für aktuelle Weapon-/Armor-Berechnung vorhanden
- [ ] Gearscore
- [x] Soulbound
- [x] Weapon Skill Mapping
- [ ] Shoppreise
- [ ] Resourcepack Integration

## Gear-Balancing

- [x] Rüstung: HP, Armor, Movement Speed
- [x] Waffen: Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power
- [ ] Gearscore aus Item-Level + Item-Definition/Balancing
- [x] Spielerlevel bestimmt über Balancing die erreichbare Gear-Stärke

---

# 08 – Equipment — 🟡 IN ARBEIT

- [ ] Helm
- [ ] Brust
- [ ] Hose
- [ ] Schuhe
- [ ] Waffe
- [ ] Nebenhand
- [x] Equipment Stats
- [ ] Level Requirements
- [ ] Rarity
- [x] Equipment → Player Stats Grundpfad
- [ ] Persistence vollständig validieren
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

- [ ] Level-System 1–100 finalisieren
- [ ] Profession XP
- [ ] Rezepte / Freischaltungen
- [ ] Profession Persistence
- [ ] Profession Dialogue
- [ ] Profession GUI, wo notwendig
- [ ] Balancing / Level-Gates

---

# 11 – Economy / Trading — 🟡 IN ARBEIT

## Economy
- [ ] Goldtaler-Kontostand
- [ ] physische Goldtaler als Sonnenblume
- [ ] Bank
- [ ] Shop
- [ ] Reward-Integration
- [ ] Economy Persistence vollständig validieren

## Trading
- [ ] Auktionshaus
- [ ] Item Listing
- [ ] Kaufabwicklung
- [ ] virtueller Geldtransfer
- [ ] sichere Transaktionsabwicklung
- [ ] Player Trading nur umsetzen, falls später ausdrücklich wieder priorisiert

---

# 12 – Party — 🟡 IN ARBEIT

- [x] Party API
- [x] Party Creation
- [x] Members
- [x] Invites
- [x] Leave
- [x] Events
- [ ] gemeinsame XP
- [ ] Loot-Verteilung
- [ ] Party-Buffs
- [ ] gemeinsamer Questfortschritt
- [ ] Persistence / Lifecycle vollständig validieren

---

# 13 – Bosses — 🟡 IN ARBEIT

- [x] Active Boss
- [x] Boss Attack Patterns
- [x] Pattern Registry
- [x] Boss Combat Grundsystem
- [x] Boss Events
- [x] Companion-Unlock-Anbindung
- [ ] vollständige Phasenlogik
- [ ] Damage Contribution
- [ ] Death / Cleanup
- [ ] Loot
- [ ] vollständiger Boss-Gameplay-Loop
- [ ] neue finale Bosse mit eigenen Attack-Patterns und eigener Präsenz

---

# 14 – Mounts — 🟡 IN ARBEIT

## Vorgesehene Mounts
- [ ] Pig – Boden-Mount
- [ ] Horse – Boden-Mount
- [ ] Zombie Horse – Boden-Mount
- [ ] Skeleton Horse – Boden-Mount
- [ ] Nautilus – Unterwasser-Mount
- [ ] Bee – Spezial-Flugmount

## Bee
- [ ] Scale 1.60
- [ ] Fluggeschwindigkeit 1.05 Blöcke/Tick
- [ ] Paper-26.x-Input-Steuerung
- [ ] Jump steigt
- [ ] Sneak sinkt
- [ ] keine Vanilla-Bienen-Kampfmechanik
- [ ] unbesiegbar / passiv

## Unique Hazel
- [x] separates MANNEQUIN-Konzept
- [x] eigener Combat-Controller / Skin-Resolver vorhanden
- [ ] vollständiger Gameplay-Loop final validieren

---

# 15 – GUI / UI — 🟡 IN ARBEIT

- [ ] aktive GUIs gegen tatsächliche Runtime-Verwendung prüfen
- [ ] nicht erreichbare Legacy-GUIs weiter entfernen
- [ ] native Dialoge als bevorzugten Interaktionsweg beibehalten
- [ ] Inventory-GUIs nur dort einsetzen, wo funktional sinnvoll
- [ ] Character-/Stats-UI
- [ ] Companion UI
- [ ] Quest UI
- [ ] Equipment UI
- [ ] Crafting UI

---

# 16 – Persistence / Database — 🟡 IN ARBEIT

- [x] YAML Player Repository
- [x] MySQL Player Repository
- [x] HikariCP
- [x] MySQL Connector
- [x] Player-Persistenzpfade
- [x] Companion-Persistenzpfade
- [x] Quest-Persistenzpfade
- [ ] Server-Neustart für alle relevanten Systeme vollständig validieren
- [ ] Fehler-/Recovery-Pfade vollständig validieren
- [ ] keine destruktive automatische Entfernung historischer Produktionsspalten

---

# 17 – API / Commands / Permissions — 🟡 IN ARBEIT

## API
- [x] PixelRPG Provider
- [x] Economy API
- [x] Item API
- [x] Party API
- [x] Statistics API
- [x] Character Stat API
- [x] relevante Events
- [ ] API Runtime-Verträge vollständig validieren

## Commands
- [ ] Commands gegen tatsächliche Registrierung prüfen
- [ ] Commands auf Admin-Funktionen begrenzen
- [ ] keine Gameplay-Abhängigkeit von Player-Commands

## Permissions
- [ ] tatsächliche Registrierung und Runtime-Verwendung vollständig validieren

---

# 18 – Cleanup / Release Quality — 🟡 IN ARBEIT

- [x] mehrere nachweislich ungenutzte Legacy-GUIs entfernt
- [x] nicht registrierter CraftingCommand entfernt
- [x] ungenutzte Class-Set-Service-/Factory-Klassen entfernt
- [x] veraltete Companion-Stat-Datei entfernt
- [x] alter Equipment-Aura-Config-Key entfernt
- [x] altes Resourcepack-Verzeichnis entfernt
- [x] aktueller Compile-Fehler im PixelRPGPlugin behoben
- [x] aktueller Gradle-Build erfolgreich
- [ ] verbleibende historische Reste per Dependency-Prüfung bewerten
- [ ] Runtime-Test nach jedem größeren Feature
- [ ] finale Dokumentation synchron halten

---

# Aktueller Arbeitsstand

```text
01 Player        🟢 98 %
02 Combat        🟢 98 %
03 Progression   🟡 IN ARBEIT  ← NÄCHSTER ABSCHNITT
04 Companions    🟢 98 % Core
05 Quests        🟡 IN ARBEIT
06 NPC/Dialogue  🟡 IN ARBEIT
07 Items         🟡 IN ARBEIT
08 Equipment     🟡 IN ARBEIT
09 Crafting      🟡 IN ARBEIT
10 Professions   🟡 IN ARBEIT
11 Economy       🟡 IN ARBEIT
12 Party         🟡 IN ARBEIT
13 Bosses        🟡 IN ARBEIT
14 Mounts        🟡 IN ARBEIT
15 GUI/UI        🟡 IN ARBEIT
16 Persistence   🟡 IN ARBEIT
17 API/Commands  🟡 IN ARBEIT
18 Cleanup       🟡 IN ARBEIT
```

## Definition of Done

```text
Code vorhanden
+ Runtime integriert
+ Content ausreichend
+ Persistenz korrekt
+ Fehlerfälle behandelt
+ Vanilla-Isolation korrekt
+ Build erfolgreich
+ Server-Test erfolgreich
+ Dokumentation aktuell
= Feature bei 95–98 % abgeschlossen
```

Absolute 100 % werden nicht als dauerhafter Zustand betrachtet. Ziel ist ein stabiler, getesteter Stand von 95–98 % je Feature.
