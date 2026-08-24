# PixelRPG – Feature Roadmap

Stand: 2026-08-24 – abgeglichen mit dem aktuellen `main`-Stand

> Diese Datei ist die verbindliche Arbeits-Roadmap. Die Reihenfolge ist von oben nach unten.
> Prozentwerte beschreiben funktionale Reife des aktuell vorhandenen Codes, nicht die Menge an Code.
> 🟢 95–98 % = abgeschlossen, 🟡 50–94 % = in Arbeit, 🔴 0–49 % = offen.
> Ein grüner Status bedeutet nicht automatisch, dass der aktuelle CI-Build oder ein Runtime-Test erfolgreich bestätigt wurde.

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
- [ ] veraltete `isRegisteredInGuild()`-Kompatibilitätsalias vollständig aus allen Consumer-Klassen entfernen

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

# 03 – Progression / Stats — 🟢 98 % ABGESCHLOSSEN

> Der aktuelle `main` enthält die Progressions-/Stats-Implementierung. Offene Punkte sind ausschließlich Validierung, Runtime-Bugs oder bewusstes Balancing.

## Progression
- [x] Level 1–99 normale Progression
- [x] Level 99 → 100 astronomischer Transzendenz-Grind
- [x] XP für Mobs bleibt fest und unabhängig von Spieler-/Gear-Scaling
- [x] Quest-XP definition-driven über `rewardExp`
- [x] XP Clamp / Overflow-Sicherheit
- [x] deterministische Progression
- [x] Player-Level API
- [x] Level-Up Events

## Character Stats
- [x] deterministische Stat-Berechnung
- [x] Cached Player Stats
- [x] HP
- [x] Armor
- [x] Movement Speed
- [x] Reach
- [x] Damage
- [x] Crit
- [x] Crit Damage
- [x] Lifesteal
- [x] Attack Power
- [x] Equipment → Stats → Combat Pipeline
- [x] StatisticsService um Character Stats erweitert
- [x] StatisticsAPI um Character Stats erweitert
- [x] aktive Companion-Passivstats in Character-Stat-Pfad integriert
- [x] Companion-Boni nur solange Companion tatsächlich gespawnt/aktiv ist
- [x] Despawn entfernt Companion-Boni automatisch
- [x] normale Companions erhalten genau einen passiven Stat
- [x] Companion-Passivstärke über Rarity-Budgetierung
- [x] Unique Companions aus der normalen Passiv-Budgetierung ausgenommen
- [x] alte Strength / Agility / Stamina / Intellect / Spell-Power-Pfade aus der StatEngine entfernt
- [x] alte Player-Attribute-Pfade aus der finalen Stats-Pipeline entfernt
- [x] berechnete Stats werden nicht persistiert
- [x] Combat verwendet zentrale Character Stats für Crit, Crit Damage und Lifesteal
- [x] Mob-XP greift auf ursprüngliche Mob-Werte zurück

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

# 05 – Quests — 🟡 90 % IN ARBEIT

> Der Quest-Core ist im aktuellen `main` technisch weitgehend implementiert. Der Build ist grün. Offen sind primär produktiver Quest-Content in `quests_v2.json`, Runtime-Validierung sowie einzelne noch nicht vollständig abgeschlossene Erweiterungen.

## Verbindliche Questtypen
- [x] HUNT – implementiert, inklusive Mob-Kill-Tracking und Party-Propagation
- [x] COLLECT – implementiert, inklusive Inventarprüfung
- [x] TALK_TO_NPC – implementiert, NPC-Interaktion setzt das Ziel auf abgeschlossen
- [x] REACH_LOCATION – implementiert, inklusive Struktur-/Biome-Navigation bzw. Reach-Location
- [x] GLOBAL_EVENT – implementiert, inklusive globalem Progress-State

> ESCORT ist ausdrücklich nicht Teil des finalen Questtypen-Pools. Der alte Escort-Pfad ist bewusst leer/kein aktiver Content.

## Quest Definition & Repository
- [x] Quest Definition / Datenmodell
- [x] QuestType
- [x] JSON-basierte Questdefinitionen
- [x] Quest Repository
- [x] Quest-ID / Titel / Beschreibung
- [x] Zieltyp / Target Key
- [x] Required Amount
- [x] Required Level / Category Level
- [x] Quest Giver NPC
- [x] Reward Money / XP / Items / Companion
- [x] Time Limit / Quest Expiry
- [x] Navigation: Structure / Biome / Radius
- [x] Prerequisites / Previous Quest
- [x] Follow-Up Quest IDs im Datenmodell
- [x] Validierung von Questdefinitionen und Referenzen
- [ ] produktive Questdefinitionen final befüllen – `quests_v2.json` enthält aktuell `definitions: []`

## Quest Core
- [x] Quest Manager
- [x] Quest Progress
- [x] Quest Completion
- [x] Quest Rewards
- [x] Quest XP
- [x] maximal 5 aktive Quests
- [x] Quest-Annahme mit Level-/Prerequisite-Prüfung
- [x] Quest-Abbruch
- [x] Quest-Zeitlimits / Expiry
- [x] QuestCompletedEvent
- [x] Abschluss nur bei erfülltem Ziel und Rückkehr zum Quest-Giver
- [x] Quest-Rewards für Geld, XP, Items und Companion-Unlock
- [x] Quest Progress Actionbar
- [ ] vollständige Runtime-Validierung aller Core-Flows

## Quest-Ziele & Spielerinformation
- [x] konkretes Questziel in Questlog sichtbar
- [x] aktueller Fortschritt in „Aktive Quests“ sichtbar
- [x] Questziel und Fortschritt in Quest-Detailansicht sichtbar
- [x] QuestText erzeugt konkrete Zielbeschreibung je Questtyp
- [x] aktive Quests werden über die native Locator-Bar navigierbar gemacht
- [x] bis zu 5 aktive Quest-Marker werden verwaltet
- [x] Marker werden bei Abschluss/Abbruch/Logout-Cleanup entfernt

## Tracking
- [x] HUNT / Mob-Kill-Tracking
- [x] COLLECT / Inventar-Tracking
- [x] TALK_TO_NPC / NPC-Zielprüfung
- [x] REACH_LOCATION / Standortprüfung
- [x] GLOBAL_EVENT / globaler Queststatus
- [x] Party-Propagation für HUNT über PartyAPI und Distanz/World-Prüfung
- [ ] vollständiger Runtime-Test aller fünf Trackingpfade

## Quest XP / Scaling
- [x] Quest-XP wird aus `reward.experience` / `rewardExp` der Questdefinition verwendet
- [x] XP wird beim Abschluss vergeben
- [x] `QuestExperienceScalingListener` existiert als Hook für Quest-XP-Scaling
- [ ] separates/ausbalanciertes Quest-XP-Scaling noch nicht final definiert und validiert

## Quest Persistence
- [x] aktive Quests werden im PlayerProfile gehalten
- [x] abgeschlossene Quests werden im PlayerProfile gehalten
- [x] YAML lädt/speichert aktive Quests inklusive Amount und Expiry
- [x] MySQL lädt/speichert aktive Quests inklusive Amount und Expiry
- [x] PlayerProfile dirty-State wird bei Queständerungen berücksichtigt
- [ ] Logout/Login Runtime-Test der Quest-Persistence

## Questketten
- [x] Prerequisites können im Datenmodell definiert werden
- [x] `previousQuest` wird als Prerequisite übernommen
- [x] Follow-Up Quest IDs werden im Repository geladen
- [x] Referenzen werden validiert und bei unbekannten IDs geloggt
- [ ] vollständige automatische Questketten-/Follow-Up-Logik im Gameplay

## Party Quest Share
- [x] PartyAPI wird für Quest-Fortschritt verwendet
- [x] Party-Mitglieder werden auf Welt und Share-Distanz geprüft
- [x] HUNT-Fortschritt kann an Party-Mitglieder propagiert werden
- [ ] COLLECT / TALK_TO_NPC / REACH_LOCATION / GLOBAL_EVENT Party-Verhalten vollständig vereinheitlichen und testen
- [ ] vollständiger Runtime-Test des Party Quest Share

## Abschlusskriterien für 05
- [ ] produktive `quests_v2.json`-Definitionen vollständig befüllen und validieren
- [ ] alle fünf Questtypen im laufenden Server testen
- [ ] Persistence nach Logout/Login testen
- [ ] Questketten vollständig testen
- [ ] Party Quest Share vollständig testen
- [ ] danach 05 auf 🟢 95–98 % setzen und abhaken

---

# 06 – NPC / Dialogue — 🟢 98 % ABGESCHLOSSEN

> Der NPC-/Dialogue-Core ist im aktuellen `main` umgesetzt und der CI-Build ist grün. NPCs bleiben manuell durch Admins gespawnt; bestehende NPC-Erstellung, Name/Titel und Skin-System bleiben erhalten. Die native Minecraft/Paper-26.2-Dialog-API bleibt die Dialogoberfläche.

## NPC-Core
- [x] bestehendes NPC-Erstellungs-/Spawn-System beibehalten
- [x] NPCs werden manuell durch Admins gespawnt
- [x] keine automatische NPC-Generierung durch das System
- [x] bestehende NPC-Namen / Titel / Rollen beibehalten
- [x] bestehendes NPC-Skin-System beibehalten
- [x] NPCs sind für alle Spieler dauerhaft sichtbar
- [x] normale Minecraft-NPC-Interaktion als einziger Interaktionsweg
- [x] G-Taste bleibt ausschließlich Spieler-Interaktion
- [x] definierte NPC-Typen/Funktionen
- [x] mehrere passende Funktionen pro NPC möglich
- [x] Funktionen werden typbezogen eingeschränkt
- [x] Beispiel: Blacksmith → Schmiedequests + Schmied erlernen + Rezepte kaufen

## Native Dialogue
- [x] native Minecraft/Paper-26.2 Dialogsystem als Basis
- [x] Vanilla-nahe und schlichte Dialogdarstellung
- [x] verzweigte Dialoge
- [x] mehrere Antwortmöglichkeiten
- [x] Dialogoptionen können Folge-Dialoge öffnen
- [x] Dialogoptionen können direkte Gameplay-Aktionen auslösen
- [x] Quest-Aktionen über Dialogoptionen
- [x] Profession-Aktionen über Dialogoptionen
- [x] Shop-Aktionen über Dialogoptionen
- [x] Bank-Aktionen über Dialogoptionen
- [x] Travel-Aktionen über Dialogoptionen
- [x] Reception-/Quick-Action-Aktionen über Dialogoptionen

## Dialogue Conditions & Progression
- [x] Dialogbedingungen
- [x] spielerabhängige Dialogoptionen
- [x] Queststatus als Bedingung
- [x] Level-/Progressionsbedingungen
- [x] Profession-/Gameplay-Bedingungen
- [x] spielerbezogener Dialog-/Story-Fortschritt
- [x] gesehene Dialogschritte werden gespeichert
- [x] abgeschlossene Dialog-/Storyschritte werden gespeichert
- [x] einmalige Dialoge/Dialogschritte

## System-Integration
- [x] Quest Dialogue als zentrale Quest-Oberfläche
- [x] Quest-Annahme über Dialog
- [x] Quest-Abgabe über Dialog
- [x] Profession Dialogue
- [x] Shop Dialogue
- [x] Bank Dialogue
- [x] Travel Dialogue
- [x] Story Dialogue
- [x] Companion Dialogue
- [x] Reception Dialogue
- [x] Quick Actions
- [x] Filler-NPCs für `Talk to Filler(id)` / `Bring X to Filler(id)` vorbereitbar

## Mehrsprachigkeit
- [x] Dialogue-Content mehrsprachig vorbereitbar
- [x] Deutsch
- [x] Englisch
- [x] Spanisch
- [x] Französisch
- [x] Texte nicht auf eine einzelne Sprache als Systemvoraussetzung festgelegt

## Abschlusskriterien für 06
- [x] NPC-/Dialogue-Core umgesetzt
- [x] vereinbarte NPC-Funktionslogik umgesetzt
- [x] native Dialogoberfläche umgesetzt
- [x] Dialog-Verzweigungen / Conditions / Actions umgesetzt
- [x] Dialog-Fortschritt / Once-only umgesetzt
- [x] Quest-/Profession-/Shop-/Bank-/Travel-/Story-Integration umgesetzt
- [x] Mehrsprachigkeitsgrundlage umgesetzt
- [x] CI-Build grün

---

# 07 – Items — 🟢 98 % ABGESCHLOSSEN

> Das Item-System ist im aktuellen `main` entsprechend der festgelegten Item-Regeln umgesetzt. PixelRPG-Items basieren standardmäßig auf Vanilla-Materialien, besitzen aber eine eigene stabile PixelRPG-Definition/ID. Normale Vanilla-Items bleiben davon getrennt und unangetastet.

## Item Core
- [x] feste Item IDs / Item Definitions
- [x] datengetriebene Item-Definitionen
- [x] Vanilla-Material als Standardbasis eines PixelRPG-Items
- [x] PixelRPG-ID, Vanilla-Material und Resourcepack-Identifier getrennt
- [x] Item-Kategorien: Melee Weapon, Ranged Weapon, Helmet, Chestplate, Leggings, Boots, Shield, Tool
- [x] Gear-Kategorien: Helm, Brust, Hose, Schuhe, Waffe, Nebenhand
- [x] Melee / Ranged getrennt vom Equipment-Slot
- [x] Item-Definitionen werden validiert
- [x] normale Vanilla-Items bleiben normale Vanilla-Items
- [x] PixelRPG-Erkennung über stabile Item-Metadaten/ID

## Rarität & Item Level
- [x] Common
- [x] Uncommon
- [x] Rare
- [x] Epic
- [x] Legendary
- [x] Unique
- [x] Item Level 1–99
- [x] Required Level unabhängig vom Item Level
- [x] Items können unter dem Spielerlevel liegen
- [x] Items können über dem Spielerlevel liegen und erst ab dem definierten Required Level nutzbar sein

## Unique Items
- [x] Unique bedeutet exakt ein konkretes Exemplar serverweit
- [x] Unique wird niemals zufällig generiert oder gerollt
- [x] Unique kann ausschließlich durch Admin-Vergabe erhalten werden
- [x] Unique-Vergabe ist serverweit eindeutig
- [x] Unique-Crafting / normale Generierung ist ausgeschlossen

## Item Stats
- [x] Rüstung: HP
- [x] Rüstung: Armor
- [x] Rüstung: Movement Speed
- [x] Rüstung: Crit Chance
- [x] Waffen: Reach
- [x] Waffen: Damage
- [x] Waffen: Crit Chance
- [x] Waffen: Crit-Schaden
- [x] Waffen: Lifesteal
- [x] Waffen: Attack Power
- [x] Item Stats werden in die zentrale Player-Stats-Pipeline eingespeist

## Gearscore & Balancing
- [x] Gearscore
- [x] Gearscore basiert auf Item-Level + Rarity + Item-Definition/Balancing
- [x] Spielerlevel dient als Grundlage für die erreichbare Gear-Stärke
- [x] Item-Level und Required Level sind getrennte Werte
- [x] Gear kann bewusst unterhalb des Spielerlevels liegen

## Soulbound
- [x] Soulbound verhindert Handel/Übertragung auf andere Spieler
- [x] Soulbound bleibt vom Besitzer nutzbar
- [x] Soulbound kann vom Besitzer gelagert und zerstört werden
- [x] Soulbound wird beim Tod nicht gedroppt
- [x] Soulbound wird beim Tod direkt im Inventar des Besitzers gehalten

## Weapon Skills
- [x] Weapon Skill gehört zum konkreten Item / zur konkreten Waffendefinition
- [x] materialbasierte Waffen können unterschiedliche Weapon Skills besitzen
- [x] Holz, Stein, Kupfer, Eisen, Gold, Diamant und Netherite können unterschiedliche Skills besitzen
- [x] Bogen-/Armbrust-Skillpfad bleibt mit Vanilla-Spannen/Laden kompatibel

## Shop & Resourcepack
- [x] bestehendes Admin-Shop-NPC-System bleibt die Shop-Grundlage
- [x] Item-Definitionen können feste Shoppreise führen
- [x] keine automatische Preisberechnung aus Item-Level + Rarity + Typ
- [x] Resourcepack-Identifier ist Bestandteil der Item-Definition
- [x] Resourcepack-Darstellung ist unabhängig von der Vanilla-Materialbasis
- [x] Item-Definitionen bilden die technische Liste der tatsächlich existierenden PixelRPG-Items

## Admin / API
- [x] ItemAPI
- [x] ItemService
- [x] ItemDefinition Registry
- [x] RPGItemBuilder
- [x] Admin-Item-Liste
- [x] Admin-Item-Vergabe
- [x] Unique-Item-Vergabe über Admin-Funktionen

## Abschlusskriterien für 07
- [x] stabile Item-IDs / Definitionen
- [x] finale Item-Kategorien
- [x] finale Raritäten
- [x] Item-Level 1–99
- [x] unabhängige Required Levels
- [x] Item Stats
- [x] Gearscore
- [x] Soulbound Death Protection
- [x] Weapon Skill Mapping
- [x] Admin-Shop-Anbindung
- [x] Resourcepack-Trennung
- [x] datengetriebene Item-Definitionen
- [x] Vanilla-/PixelRPG-Items sauber getrennt
- [x] CI-Build grün

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

## Core
- [ ] Profession Registry
- [ ] Profession Level 1–100
- [ ] Profession XP
- [ ] Profession Progression
- [ ] Profession Recipes
- [ ] Profession Trainer
- [ ] Profession Persistence

---

# 11 – Economy / Gold — 🟡 IN ARBEIT

- [ ] Goldtaler Kontostand
- [ ] Sonnenblume als physischer Goldtaler
- [ ] Goldtaler Pickup
- [ ] Wallet API
- [ ] Economy Persistence
- [ ] Shoppreise
- [ ] Auktionshaus

---

# 12 – Party — 🟡 IN ARBEIT

- [ ] Party Core
- [ ] Party Invite
- [ ] Party Leave / Kick
- [ ] gemeinsame XP
- [ ] Loot-Verteilung
- [ ] Party-Buffs
- [ ] gemeinsamer Questfortschritt
- [ ] Party Persistence / Cleanup

---

# 13 – Travel / Waypoints — 🟡 IN ARBEIT

- [ ] Travel NPC
- [ ] Waypoint Registry
- [ ] Waypoint Unlock
- [ ] Reiseziele über NPC-Dialog
- [ ] erneutem NPC-Ansprechen zeigt freigeschaltete Ziele
- [ ] Travel Kosten
- [ ] Travel Cooldown

---

# 14 – Shops / Auction House — 🟡 IN ARBEIT

- [ ] Shop Core
- [ ] Shop Definitions
- [ ] Buy / Sell
- [ ] NPC Shop Dialogue
- [ ] Auktionshaus Core
- [ ] Listings
- [ ] Kaufen / Verkaufen
- [ ] Gebühren
- [ ] Expiration

---

# 15 – UI / HUD — 🟡 IN ARBEIT

- [ ] Vanilla HUD-Kompatibilität
- [ ] HP Anzeige
- [ ] Armor Anzeige
- [ ] Mana-Konzept
- [ ] Level / XP Anzeige
- [ ] Character Stats Anzeige
- [ ] Companion Anzeige
- [ ] Quest Anzeige
- [ ] Combat Feedback

---

# 16 – Resource Pack / Custom Items — 🟡 IN ARBEIT

- [ ] Resourcepack-Struktur
- [ ] Custom Item Mapping
- [ ] Item Models
- [ ] Custom Textures
- [ ] Custom Armor
- [ ] Custom Weapon Models
- [ ] Resourcepack Versionierung
- [ ] Plugin ↔ Resourcepack Item IDs

---

# 17 – Persistence / Database — 🟡 IN ARBEIT

- [x] Player Persistence Grundsystem
- [x] YAML Fallback
- [x] MySQL Repository
- [ ] Companion Persistence vollständig validieren
- [ ] Quest Persistence vollständig validieren
- [ ] Profession Persistence
- [ ] Economy Persistence
- [ ] Equipment Persistence
- [ ] Schema Versioning
- [ ] Migration System

---

# 18 – Admin / Debug — 🟡 IN ARBEIT

- [ ] Admin Command Framework
- [ ] Player Debug
- [ ] Stats Debug
- [ ] Combat Debug
- [ ] Companion Debug
- [ ] Quest Debug
- [ ] Economy Debug
- [ ] Item Debug
- [ ] Reload / Registry Diagnostics

---

# 19 – Content / World — 🔴 OFFEN

- [ ] World-Struktur
- [ ] Gebiete
- [ ] Dungeons
- [ ] Bosse
- [ ] Gegner-Varianten
- [ ] NPC Content
- [ ] Quest Content
- [ ] Item Content
- [ ] Companion Content
- [ ] Profession Content
- [ ] Loot Tables
- [ ] verbindliche Lore

> Content wird erst auf Basis der technisch stabilen Systeme ausgearbeitet.

---

# 20 – Final Validation — 🔴 OFFEN

- [ ] kompletter Clean Build
- [ ] Runtime Smoke Test
- [ ] Player Lifecycle Test
- [ ] Progression Test
- [ ] Combat Test
- [ ] Companion Test
- [ ] Quest Test
- [ ] NPC / Dialogue Test
- [ ] Item / Equipment Test
- [ ] Economy Test
- [ ] Party Test
- [ ] Travel Test
- [ ] Resourcepack Test
- [ ] Persistence / Migration Test
- [ ] Performance Test
- [ ] Fehler-/Log-Review
- [ ] Release Build