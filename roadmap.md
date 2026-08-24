# PixelRPG – Feature Roadmap

Stand: 2026-08-24 – strikter Code-/Daten-Audit

> Diese Datei ist die verbindliche Arbeits-Roadmap. Die Reihenfolge ist von oben nach unten.
> Die Prozentwerte beschreiben funktionale Reife des aktuell vorhandenen Codes, nicht die Menge an Code.
> Diese Revision wurde gegen den aktuellen `main`-Stand, den vollständigen Repository-Baum, die vorhandenen Java-Klassen, `config.yml`, `paper-plugin.yml`, JSON-Daten und Sprachdateien geprüft.

## Statussystem

- 🟢 **95–98 % – ABGESCHLOSSEN**: Soll-Verhalten ist definiert, implementiert und ausreichend stabil.
- 🟡 **50–94 % – IN ARBEIT**: Funktionale Basis vorhanden, aber Integration, Gameplay oder gewünschtes Verhalten ist noch nicht vollständig.
- 🔴 **0–49 % – OFFEN**: Nur Gerüst/Teilfunktion vorhanden oder Feature fehlt.

## Arbeitsregel

```text
1. Soll-Verhalten gemeinsam definieren
2. kompletten Code dieses Features prüfen
3. Soll gegen Ist vergleichen
4. fehlende/falsche Punkte identifizieren
5. nur notwendige Änderungen umsetzen
6. Build durchführen
7. Runtime testen
8. Fehler beheben
9. PixelRPG.md und roadmap.md aktualisieren
10. bei 95–98 % abhaken
```

## Wichtige Audit-Feststellungen

- Companion `restoreActive()` wird aktuell beim `PlayerJoinEvent` aufgerufen. Das widerspricht der bewusst festgelegten Regel „Logout = Despawn, Login = kein automatisches Wiederbeschwören“. Das muss als offener Punkt behandelt werden.
- `CompanionAbilityEngine` ist aktuell nur ein generischer Cooldown-/Trigger-Rahmen; konkrete Companion-Ability-Effekte sind im Runtime-Code noch nicht implementiert. Die aktuelle `companions.json` enthält keine konkreten Ability-/Passive-Definitionen.
- Camel ist als Companion definiert, aber aktuell **nicht** als Mount aktiviert. Ein Bee-Companion ist im aktuellen Datenbestand nicht vorhanden.
- Professionen sind aktuell **1–99**, nicht 1–100.
- Aktuell sind vier Professionen definiert: BLACKSMITH, PROVISIONER, ALCHEMIST, SCHOLAR.
- Aktuell sind drei Default-Bosse definiert: Forest Tyrant, Frost Sovereign, Void Reaper.
- Aktuell existieren vier Sprachdateien: `de`, `en`, `es`, `fr`.
- Player-Class-Auswahl ist an Registrierung und Level 30 gebunden; Respec ist konfigurierbar, standardmäßig Level 40 / 300 Gold.
- Soulview und Elytra Permit sind echte Player-Attribute.
- Shops werden persistent gespeichert und besitzen eine Migration vom alten YAML-Itemformat auf Base64-Itemdaten.
- Der persönliche Bank-Speicher umfasst zwei Seiten mit je 54 Slots und ist unabhängig von der Vanilla-Endertruhe.
- Ein vollständiges Player-Trading-Runtime-System existiert aktuell nicht.
- Ein vollständiges Guild-Runtime-System existiert aktuell nicht; vorhandene GuildAPI-/Currency-Pfade bleiben trotzdem Bestandteil des Codes.

---

# 01 – Player

## Profil & Lifecycle
- [ ] PlayerProfile / Grunddaten – 95 %
- [ ] PlayerProfileManager – 92 %
- [ ] PlayerProfileRepository – 90 %
- [ ] YAML Player Repository – 90 %
- [ ] MySQL Player Repository – 88 %
- [ ] Async Pre-Login Load – 92 %
- [ ] Join Activation – 92 %
- [ ] Quit Deactivation + Save – 92 %
- [ ] Registrierungsstatus – 90 %
- [ ] Vanilla-/PixelRPG-Spieler-Isolation – 85 %
- [ ] Startbonus-State – 75 %
- [ ] Dirty-State / Save Queue – 92 %
- [ ] Emergency YAML Backup bei MySQL-Fehler – 90 %

## Level & XP
- [ ] Level 1–99 – 95 %
- [ ] Level 100 als reservierte Transzendenz – 95 %
- [ ] XP-Tabelle / WotLK-inspirierte Kurve – 92 %
- [ ] Level-99-Transzendenz-Grind – 90 %
- [ ] XP Clamp / Overflow-Sicherheit – 88 %
- [ ] Level-Up Event – 92 %
- [ ] Player-Level API – 90 %

## Klassen
- [ ] NONE / Klassenloser Zustand – 95 %
- [ ] Warrior – 90 %
- [ ] Ranger – 90 %
- [ ] Rogue – 90 %
- [ ] Healer – 90 %
- [ ] Mage – 90 %
- [ ] Klassenwahl ab Level 30 – 85 %
- [ ] Klassenwahl nur für registrierte Spieler – 90 %
- [ ] Klassenwechsel / Respec – 88 %
- [ ] Respec Level-Grenze – 90 %
- [ ] Respec Kosten – 90 %
- [ ] ClassBalance – 90 %
- [ ] PlayerClassChangeEvent – 95 %

## Attribute
- [ ] Vitality – 90 %
- [ ] Agility – 90 %
- [ ] Precision – 90 %
- [ ] Range – 90 %
- [ ] Toughness – 90 %
- [ ] Soulview – 90 %
- [ ] Elytra Permit – 90 %
- [ ] Max-Punkte je Attribut – 90 %
- [ ] Levelanforderungen je Punkt – 90 %
- [ ] Attributkosten – 90 %
- [ ] Klassenrabatt auf Primärattribute – 90 %
- [ ] Attribute → finale Stats – 85 %
- [ ] Attribut-Persistenz – 90 %

---

# 02 – Combat

## Kern
- [ ] zentrale Damage-Logik – 82 %
- [ ] Guild-/Registrierungs-Schutz – 90 %
- [ ] Player Combat – 78 %
- [ ] Mob Combat – 82 %
- [ ] Damage Scaling – 80 %
- [ ] Class Scaling – 82 %
- [ ] Stat Scaling – 82 %
- [ ] Crit Chance – 85 %
- [ ] Crit Damage – 82 %
- [ ] Lifesteal – 82 %
- [ ] Angriffsgeschwindigkeit / Attack Intervals – 72 %
- [ ] Combat State / Target State – 75 %
- [ ] Combat Cleanup – 85 %
- [ ] Combat Events – 85 %

## Weapon Skills
- [ ] Rechtsklick-Erkennung – 95 %
- [ ] WeaponAbilityEngine – 90 %
- [ ] Weapon Ability Metadata auf Items – 90 %
- [ ] Waffen-Cooldowns – 90 %
- [ ] Ability Level Requirement – 90 %
- [ ] HEAVY_STRIKE – 90 %
- [ ] WHIRLWIND – 90 %
- [ ] ARCANE_BURST – 90 %
- [ ] Player-Quit-Cooldown-Cleanup – 95 %

## Mob Scaling
- [ ] Mob-Level Scaling – 88 %
- [ ] HP Scaling – 88 %
- [ ] Damage Scaling – 88 %
- [ ] Region/Danger Provider – 70 %
- [ ] Dimension Scaling – 90 %
- [ ] Player-Parity – 88 %
- [ ] XP aus Mob-Health – 90 %
- [ ] Original-Mob-Attribute Tracking – 85 %
- [ ] Vanilla-/PixelRPG-Mischbetrieb – 70 %

## Loot / Death
- [ ] Mob Experience – 88 %
- [ ] Loot Drop Chance – 90 %
- [ ] Random Item Rarity Roll – 90 %
- [ ] Item Drop Pool – 90 %
- [ ] Physical Guild Currency Drops – 90 %
- [ ] Soulbound Death Protection – 92 %
- [ ] Death-/Respawn-Integration – 80 %
- [ ] Elytra Permission – 88 %
- [ ] Mob Nameplates – 90 %

---

# 03 – Progression / Stats

- [ ] Level 1–99 End-to-End – 92 %
- [ ] Level-100-Grenze – 92 %
- [ ] XP Scaling – 88 %
- [ ] deterministische Progression – 85 %
- [ ] StatEngine – 88 %
- [ ] Cached Player Stats – 90 %
- [ ] Max Health – 90 %
- [ ] Armor – 88 %
- [ ] Movement Speed – 88 %
- [ ] Block Reach – 85 %
- [ ] Entity Reach – 85 %
- [ ] Bonus Damage – 88 %
- [ ] Crit Chance – 88 %
- [ ] Crit Damage Multiplier – 88 %
- [ ] Lifesteal – 80 %
- [ ] Strength / Agility / Stamina / Intellect – 80 %
- [ ] Attack Power – 82 %
- [ ] Spell Power – 82 %
- [ ] Soulview → Night Vision – 90 %
- [ ] Player Attributes → Stats → Combat – 82 %
- [ ] Equipment → Stats → Combat – 78 %
- [ ] Mob-Kill-Statistik – 90 %
- [ ] Player-Death-Statistik – 90 %
- [ ] Quest-/Boss-Statistiken – 88 %
- [ ] StatisticsService – 88 %
- [ ] StatisticsAPI – 85 %
- [ ] Stat-Persistenz – 88 %

---

# 04 – Companions

> Neue Companion-Inhalte sind aktuell nicht geplant. Dieser Bereich bleibt geschlossen, außer bei echten Bugs oder bewusst beschlossenen Änderungen.

## Core
- [x] Companion Registry – 98 %
- [x] Companion Definitions – 98 %
- [x] Companion Ownership – 95 %
- [x] Companion Unlock – 95 %
- [x] Unique Companion Unlock – 95 %
- [x] Aktivieren / Deaktivieren – 98 %
- [x] Spawn – 95 %
- [x] Logout Despawn – 98 %
- [ ] **Kein automatischer Restore beim Login** – 0 %
- [x] Follow-System – 95 %
- [x] Movement-Typen – 95 %
- [x] Passive Stats nur bei tatsächlich beschworenem Companion – 95 %
- [x] Companion Stats Calculator – 95 %
- [x] Companion Leveling – 95 %
- [x] Normale Companion-Level an Spielerlevel gekoppelt – 95 %
- [x] Unique Companion eigenes Level – 95 %
- [x] Companion XP nur für aktiven Companion – 95 %
- [x] Combat-Regeln – 95 %
- [x] Hostile-Mob-Targeting – 95 %
- [x] Spieler-Ziele ausgeschlossen – 95 %
- [x] freundliche Ziele ausgeschlossen – 95 %
- [x] Companion Equipment – 95 %
- [x] Companion Equipment Persistence – 92 %
- [x] Companion Runtime Registry – 98 %
- [x] Runtime Cleanup – 98 %
- [x] Companion Entity Death Cleanup – 95 %
- [x] Runtime Tick – 95 %
- [x] Unique Hazel Mannequin – 95 %
- [x] Boss → Companion Unlock – 95 %

## Companion Abilities
- [ ] CompanionAbilityEngine – 35 %
- [ ] Ability Definitions – 20 %
- [ ] konkrete aktive Companion-Abilities – 20 %
- [ ] konkrete passive Ability Definitions – 20 %
- [ ] Ability Cooldowns – 60 %
- [ ] Ability Runtime Integration – 20 %

## Aktueller Companion-Roster
- [x] common-chicken – 95 %
- [x] common-cow – 95 %
- [x] common-sheep – 95 %
- [x] common-rabbit – 95 %
- [x] common-bat – 95 %
- [x] uncommon-wolf – 95 %
- [x] uncommon-cat – 95 %
- [x] uncommon-fox – 95 %
- [x] uncommon-goat – 95 %
- [x] uncommon-copper-golem – 95 %
- [x] uncommon-parrot – 95 %
- [x] uncommon-armadillo – 95 %
- [x] uncommon-panda – 95 %
- [x] epic-pig – 95 %
- [x] epic-horse – 95 %
- [x] epic-zombie-horse – 95 %
- [x] epic-skeleton-horse – 95 %
- [x] epic-camel – 95 %
- [x] epic-nautilus – 95 %
- [x] epic-creaking – 95 %
- [x] epic-happy-ghast – 95 %
- [x] legendary-iron-golem – 95 %
- [x] unique-hazel – 98 %

## Mounts
- [x] Pig Ground Mount – 95 %
- [x] Horse Ground Mount – 95 %
- [x] Zombie Horse Ground Mount – 95 %
- [x] Skeleton Horse Ground Mount – 95 %
- [x] Nautilus Underwater Mount – 90 %
- [ ] Camel Mount – 0 % (Companion existiert, Mount ist nicht aktiviert)
- [ ] Flying Mount Runtime – 70 % (generischer Controller vorhanden, kein aktueller Flying-Companion)
- [ ] Bee Spezial-Flugmount – 0 % (nicht im aktuellen Datenbestand)
- [x] Owner-only Mounting – 95 %
- [x] Saddle Handling – 90 %
- [x] Mount Movement Input – 90 %
- [ ] Mount Cleanup / dismount edge cases – 90 %
- [ ] Mount Restart-Verhalten – 75 %

---

# 05 – Quests

## Core
- [ ] Quest Definition – 90 %
- [ ] Quest Repository – 90 %
- [ ] Quest Manager – 90 %
- [ ] Quest Progress – 90 %
- [ ] Quest Completion – 85 %
- [ ] Quest Rewards – 80 %
- [ ] Quest XP – 90 %
- [ ] Quest XP Scaling – 90 %
- [ ] maximal 5 aktive Quests – 82 %
- [ ] Quest Persistence – 85 %
- [ ] QuestCompletedEvent – 95 %

## Questtypen
- [ ] HUNT – 90 %
- [ ] COLLECT – 88 %
- [ ] TALK_TO_NPC – 90 %
- [ ] ESCORT – 60 %
- [ ] REACH_LOCATION – 85 %
- [ ] GLOBAL_EVENT – 82 %

## Tracking / Navigation
- [ ] Mob-Kill-Tracking – 92 %
- [ ] passive Quest Checks – 82 %
- [ ] GlobalEventState – 82 %
- [ ] Quest Navigation – 85 %
- [ ] Navigation Compass Identity – 90 %
- [ ] Recovery Compass – 65 %
- [ ] Questketten – 70 %
- [ ] aktiver Quest-Unlock-Flow E2E – 70 %
- [ ] Party Quest Share Range – 85 %

## Daten
- [ ] quests.json – 90 %
- [ ] quests_v2.json Legacy-Status – 50 %
- [ ] doppelte Questquellen bereinigen / Migrationsentscheidung – 60 %
- [ ] Datenkonsistenz zwischen Definition und Runtime – 80 %

---

# 06 – NPC / Dialogue

## NPC Core
- [ ] RPGNpc – 90 %
- [ ] NpcManager – 90 %
- [ ] NpcType – 90 %
- [ ] NpcBehavior – 90 %
- [ ] Behavior Registry – 90 %
- [ ] NPC Spawn – 88 %
- [ ] NPC Interaktion – 90 %
- [ ] NPC Look Task – 82 %
- [ ] Chunk Lifecycle – 78 %
- [ ] Login Resync – 82 %
- [ ] NPC Persistence – 78 %
- [ ] NPC PDC Identity – 90 %
- [ ] Mannequin Skin Resolver – 90 %

## NPC Behaviors
- [ ] Banker – 88 %
- [ ] Blacksmith – 88 %
- [ ] Filler – 85 %
- [ ] Profession Trainer – 82 %
- [ ] Quest NPC – 90 %
- [ ] Reception – 90 %
- [ ] Shop – 85 %
- [ ] Story – 82 %
- [ ] Travel – 85 %

## Dialogue
- [ ] DialogueEngine – 90 %
- [ ] Native Minecraft/Paper Dialog Framework – 90 %
- [ ] DialogueCommand – 85 %
- [ ] Dialogue Definitions / Flows – 82 %
- [ ] Dialogue Choices – 85 %
- [ ] Dialogue Actions – 82 %
- [ ] NPC → Dialogue – 90 %
- [ ] Quest Dialogue – 88 %
- [ ] Companion Dialogue – 88 %
- [ ] Profession Dialogue – 80 %
- [ ] Shop Dialogue – 82 %
- [ ] Travel Dialogue – 85 %
- [ ] Bank Dialogue – 88 %
- [ ] Story Dialogue – 82 %
- [ ] Soulbound Dialog Action – 95 %
- [ ] Quick Actions Dialog – 85 %
- [ ] Reception Dialog – 85 %

---

# 07 – Items

## Item Identity / Builder
- [ ] RPGItemBuilder – 95 %
- [ ] ItemService – 92 %
- [ ] Item Definitions – 82 %
- [ ] Item Identification Flag – 95 %
- [ ] Stable Item ID – 95 %
- [ ] Unique Item Instance ID – 95 %
- [ ] Item Category – 92 %
- [ ] Gear Category Registry – 90 %
- [ ] Guild Item Marker – 90 %
- [ ] Item Validation / API – 90 %

## Rarity
- [ ] Common – 95 %
- [ ] Uncommon – 95 %
- [ ] Rare – 95 %
- [ ] Epic – 95 %
- [ ] Legendary – 95 %
- [ ] Unique – 95 %
- [ ] Rarity Stat Multipliers – 92 %
- [ ] Weighted Random Rarity Roll – 92 %
- [ ] Unique niemals zufällig rollen – 95 %

## Scaling / Stats
- [ ] Item Level 1–99 – 90 %
- [ ] Level Requirement – 90 %
- [ ] Item Growth Curve – 90 %
- [ ] Bonus Damage – 90 %
- [ ] Crit Chance – 90 %
- [ ] Lifesteal – 90 %
- [ ] Armor Value – 90 %
- [ ] Health Bonus – 90 %
- [ ] Tool Efficiency – 90 %
- [ ] Item Stat Profile – 85 %
- [ ] Gearscore – 60 %
- [ ] Item Scaling JSON – 90 %

## Special
- [ ] CraftedItemFactory – 82 %
- [ ] SoulboundService – 95 %
- [ ] Soulbound Death Integration – 92 %
- [ ] Weapon Ability Metadata – 90 %
- [ ] Shop Price Tag Metadata – 70 %
- [ ] Guild Item / Currency Metadata – 85 %
- [ ] Item Economy Values – 80 %
- [ ] Vanilla Item Isolation – 72 %
- [ ] Resourcepack Item Integration – 50 %
- [ ] Item Identity / Validation E2E – 78 %

---

# 08 – Equipment

- [ ] Equipment Slots – 88 %
- [ ] Armor – 85 %
- [ ] Weapons – 82 %
- [ ] Mainhand – 90 %
- [ ] Offhand – 78 %
- [ ] Shields – 80 %
- [ ] Tools – 82 %
- [ ] Equipment Stats – 82 %
- [ ] Armor Stats – 82 %
- [ ] Weapon Stats – 82 %
- [ ] Tool Stats – 80 %
- [ ] Equipment Restrictions – 70 %
- [ ] Level Requirements – 85 %
- [ ] Rarity Integration – 88 %
- [ ] Equipment → Player Stats – 82 %
- [ ] Equipment Cleanup – 88 %
- [ ] Equipment Persistence – 75 %
- [ ] Companion Equipment – 95 %
- [ ] Equipment GUI – 65 %

---

# 09 – Crafting

- [ ] CraftRecipe – 90 %
- [ ] CraftingRecipeRegistry – 90 %
- [ ] CraftingService – 82 %
- [ ] Crafting Definitions – 80 %
- [ ] Crafting Recipes – 80 %
- [ ] Crafted Item Factory – 82 %
- [ ] Profession Requirements – 75 %
- [ ] Recipe Purchase / Unlock – 85 %
- [ ] Recipe Price Calculation – 85 %
- [ ] Custom Crafting – 75 %
- [ ] Dialogue-based Crafting – 78 %
- [ ] Crafting GUI – 75 %
- [ ] Blacksmith GUI – 80 %
- [ ] Crafting Validation – 80 %
- [ ] Level Requirements – 85 %
- [ ] Rarity on Recipe Output – 85 %
- [ ] 43 aktuell definierte Rezepte – 90 %
- [ ] vollständige E2E-Rezeptnutzung – 70 %

---

# 10 – Professions

## Framework
- [ ] Profession Model – 90 %
- [ ] ProfessionService – 90 %
- [ ] ProfessionSystem – 85 %
- [ ] Profession Gathering – 82 %
- [ ] Profession Learn Flow – 85 %
- [ ] Profession Levels 1–99 – 90 %
- [ ] Profession XP – 88 %
- [ ] Profession XP Curve – 88 %
- [ ] Recipe Unlocks – 88 %
- [ ] Recipe Purchase – 88 %
- [ ] Profession Requirements – 82 %
- [ ] Guild Registration Requirement – 90 %

## Berufe
- [ ] BLACKSMITH – 90 %
- [ ] PROVISIONER – 90 %
- [ ] ALCHEMIST – 90 %
- [ ] SCHOLAR – 90 %
- [ ] geplante Berufe vollständig definieren – 80 %

## Integration
- [ ] Profession Trainer NPC – 82 %
- [ ] Profession Dialog – 80 %
- [ ] Profession UI – 65 %
- [ ] Profession → Crafting – 88 %
- [ ] Profession → Economy – 80 %
- [ ] Profession Persistence – 88 %

---

# 11 – Economy / Shop / Bank

## Player Economy
- [ ] EconomyAPI – 90 %
- [ ] Player Currency / Wallet – 85 %
- [ ] Deposit – 90 %
- [ ] Withdraw – 90 %
- [ ] Balance – 90 %
- [ ] Currency Persistence – 82 %
- [ ] Physical Guild Currency – 90 %
- [ ] Currency Pickup → Wallet – 90 %
- [ ] Currency Stack Limit – 90 %

## Item Economy
- [ ] Item Economy Config – 80 %
- [ ] Item Values – 80 %
- [ ] Price Definitions – 70 %
- [ ] Loot Currency Chance – 90 %
- [ ] Loot Currency Amount Range – 90 %

## Shops
- [ ] ShopManager – 90 %
- [ ] ShopEntry – 90 %
- [ ] Shop per NPC ID – 90 %
- [ ] Shop Load / Save – 90 %
- [ ] Base64 Item Serialization – 90 %
- [ ] Legacy Shop Item Migration – 90 %
- [ ] Shop GUI – 75 %
- [ ] Shop Editor GUI – 78 %
- [ ] Shop Command – 82 %
- [ ] NPC Shop Integration – 85 %
- [ ] Shop Shutdown Save – 90 %

## Bank
- [ ] Bank Storage – 88 %
- [ ] 2 Bank Pages – 90 %
- [ ] 54 Slots pro Seite – 90 %
- [ ] unabhängiger Vanilla-Enderchest-Speicher – 90 %
- [ ] Bank Inventory GUI – 85 %
- [ ] Bank Deposit / Withdraw – 85 %
- [ ] Bank Quick Amount – 80 %
- [ ] Bank Persistence – 82 %

---

# 12 – Trading

> Im aktuellen Repository existiert kein vollständiges Player-to-Player-Trading-Runtime-System.

- [ ] Player Trading – 10 %
- [ ] Trade Requests – 10 %
- [ ] Trade GUI – 10 %
- [ ] Item Validation – 10 %
- [ ] Currency Trading – 10 %
- [ ] Trade Confirmation – 10 %
- [ ] Anti-Duplication – 0 %
- [ ] Cancellation / Disconnect Handling – 0 %
- [ ] Trade Persistence / Recovery – 0 %

---

# 13 – Party

- [ ] Party Model – 90 %
- [ ] PartyManager – 88 %
- [ ] Party API – 85 %
- [ ] Party Creation – 85 %
- [ ] Party Leader – 90 %
- [ ] maximal 9 Mitglieder – 90 %
- [ ] Add Member – 85 %
- [ ] Remove Member – 85 %
- [ ] Leader Promotion – 85 %
- [ ] Party Disconnect Cleanup – 88 %
- [ ] Party Invite Flow – 78 %
- [ ] Party Leave – 82 %
- [ ] Party Events – 80 %
- [ ] Party Quest Sharing – 80 %
- [ ] Party Quest Share Range – 85 %
- [ ] Party GUI – 70 %
- [ ] Party HUD State – 65 %
- [ ] Party Gameplay Integration – 65 %

---

# 14 – Guilds

> Kein vollständiges Guild-Gameplay als eigenes Runtime-System. GuildAPI und Guild-Currency sind Infrastruktur für bereits vorhandene Systeme.

- [ ] GuildAPI – 65 %
- [ ] Guild Join Event – 85 %
- [ ] Guild Leave Event – 85 %
- [ ] Guild Registration State – 90 %
- [ ] Guild Currency Integration – 80 %
- [ ] Guild Currency Item – 80 %
- [ ] Guild Currency Pickup – 85 %
- [ ] Guild Compass – 80 %
- [ ] Guild GUI – 15 %
- [ ] Guild Ranks – 0 %
- [ ] Guild Persistence als eigenes System – 0 %
- [ ] Guild Progression – 0 %
- [ ] Scope-Entscheidung dokumentiert – 95 %

---

# 15 – Bosses

## Core
- [ ] BossDefinition – 90 %
- [ ] ActiveBoss – 88 %
- [ ] BossManager – 88 %
- [ ] BossRepository – 90 %
- [ ] YAML Boss Persistence – 88 %
- [ ] WorldBossSpawnTask – 85 %
- [ ] Auto Spawn – 85 %
- [ ] Max Concurrent Bosses – 85 %
- [ ] Spawn Radius – 85 %
- [ ] Spawn Chance – 80 %

## Patterns
- [ ] BossAttackPattern – 90 %
- [ ] BossAttackPatternRegistry – 90 %
- [ ] Slam Attack – 90 %
- [ ] Projectile Volley – 90 %
- [ ] Summon Adds – 88 %
- [ ] Enrage Buff – 88 %
- [ ] BossPhase – 85 %
- [ ] Phase Health Thresholds – 88 %
- [ ] Phase Attack Intervals – 88 %
- [ ] Phase Announcements – 75 %

## Combat / Rewards
- [ ] Boss Damage Contribution – 85 %
- [ ] Boss Damage Cap – 90 %
- [ ] Boss Death Handling – 88 %
- [ ] Boss Loot Config – 85 %
- [ ] Guaranteed Rarity – 85 %
- [ ] Boss Money Reward – 85 %
- [ ] Boss XP Reward – 85 %
- [ ] Boss Item Drops – 80 %
- [ ] Boss → Companion Unlock – 95 %
- [ ] Boss Statistics – 88 %
- [ ] Boss UI – 50 %
- [ ] Boss E2E Gameplay Loop – 65 %

## Aktuelle Default-Bosse
- [ ] Forest Tyrant – 85 %
- [ ] Frost Sovereign – 85 %
- [ ] Void Reaper – 85 %

---

# 16 – Story

- [ ] StoryChapter – 85 %
- [ ] StoryManager – 80 %
- [ ] Story Persistence – 75 %
- [ ] Story Chapter Ordering – 85 %
- [ ] Story Chapter Completion – 80 %
- [ ] Story XP Reward – 85 %
- [ ] StoryBookFactory – 85 %
- [ ] Book Text Wrapping – 85 %
- [ ] Configurable Characters per Line – 85 %
- [ ] Configurable Lines per Page – 85 %
- [ ] Story NPC Dialogue – 80 %
- [ ] Story E2E – 45 %
- [ ] vollständiger Story-Content – 20 %
- [ ] aktuelles Prologue-Kapitel – 90 %

---

# 17 – GUI / UI

## Framework
- [ ] AbstractGUI – 88 %
- [ ] GUIHolder – 88 %
- [ ] GUIListener – 88 %
- [ ] Inventory Interaction Safety – 80 %

## Aktuelle GUIs
- [ ] CraftingGUI – 78 %
- [ ] BlacksmithGUI – 82 %
- [ ] PartyGUI – 70 %
- [ ] QuestLogGUI – 82 %
- [ ] QuestDetailGUI – 82 %
- [ ] ShopGUI – 75 %
- [ ] ShopEditorGUI – 78 %
- [ ] Companion UI / Dialogue – 85 %
- [ ] Bank UI – 85 %
- [ ] Native Dialog UI – 90 %
- [ ] Main RPG UI / Character Card – 78 %
- [ ] Scoreboard / Character Card – 78 %

## Player UI States
- [ ] Scoreboard Enabled State – 75 %
- [ ] Party HUD Enabled State – 65 %
- [ ] Quest Tracker Enabled State – 65 %
- [ ] Actionbar Feedback – 82 %
- [ ] Missing Translation Fallback – 90 %

---

# 18 – Database / Persistence / Storage

- [ ] DatabaseManager – 90 %
- [ ] HikariCP – 95 %
- [ ] MySQL Connector – 95 %
- [ ] StorageType – 90 %
- [ ] YAML Storage – 90 %
- [ ] MySQL Storage – 88 %
- [ ] Storage Initialization – 90 %
- [ ] Connection Lifecycle – 90 %
- [ ] Connection Timeout – 90 %
- [ ] Save Executor – 90 %
- [ ] Per-Player Save Queue – 90 %
- [ ] Save Flush on Shutdown – 90 %
- [ ] Emergency Backup – 90 %
- [ ] Player Persistence – 90 %
- [ ] Companion Persistence – 95 %
- [ ] Quest Persistence – 85 %
- [ ] Profession Persistence – 88 %
- [ ] Story Persistence – 75 %
- [ ] Shop Persistence – 90 %
- [ ] Bank Persistence – 82 %
- [ ] Party Persistence – 0 %
- [ ] Guild Persistence – 0 %
- [ ] historische Datenfelder / Migration – 65 %

---

# 19 – Commands / Permissions

## Framework
- [ ] PaperBasicCommandAdapter – 90 %
- [ ] RootCommand – 90 %
- [ ] SubCommand Framework – 90 %
- [ ] Command Registration – 90 %
- [ ] Command Validation – 80 %
- [ ] paper-plugin.yml Registrierung – 95 %

## Commands
- [ ] `/rpgadmin` – 90 %
- [ ] Blacksmith Command – 85 %
- [ ] Boss Command – 85 %
- [ ] Companion Command – 90 %
- [ ] NPC Command – 88 %
- [ ] Party Command – 85 %
- [ ] Quest Admin Command – 88 %
- [ ] Quest Log Command – 88 %
- [ ] Shop Command – 88 %
- [ ] `/rpgparty` – 85 %
- [ ] `/questlog` – 88 %
- [ ] `/dialogue` – 85 %

## Permissions
- [ ] Permission Framework – 85 %
- [ ] `rpg.admin` – 95 %
- [ ] `rpg.member` – 95 %
- [ ] Admin-only Companion Schutz – 98 %

---

# 20 – API / Events

## Provider / APIs
- [ ] ApiVersion – 95 %
- [ ] PixelRPGProvider – 90 %
- [ ] EconomyAPI – 85 %
- [ ] GuildAPI – 65 %
- [ ] ItemAPI – 90 %
- [ ] PartyAPI – 85 %
- [ ] StatisticsAPI – 85 %
- [ ] API Service Registration – 90 %

## Events
- [ ] PlayerLevelUpEvent – 95 %
- [ ] PlayerClassChangeEvent – 95 %
- [ ] PlayerJoinGuildEvent – 90 %
- [ ] PlayerLeaveGuildEvent – 90 %
- [ ] QuestCompletedEvent – 95 %
- [ ] BossDefeatedEvent – 90 %
- [ ] Event Contracts / API Stability – 75 %
- [ ] API Documentation – 45 %

---

# 21 – Configuration / Language / Data

## Configuration
- [ ] config.yml – 90 %
- [ ] Storage Configuration – 90 %
- [ ] Language Configuration – 90 %
- [ ] Economy Configuration – 88 %
- [ ] Item Configuration – 88 %
- [ ] Combat Configuration – 88 %
- [ ] Attribute Configuration – 90 %
- [ ] Class Balance Configuration – 90 %
- [ ] Mob Scaling Configuration – 90 %
- [ ] NPC Configuration – 88 %
- [ ] Quest Configuration – 88 %
- [ ] Boss Configuration – 90 %
- [ ] Scoreboard Configuration – 88 %
- [ ] Story Configuration – 88 %
- [ ] Classes Respec Configuration – 90 %

## JSON Data
- [ ] JsonDataManager – 90 %
- [ ] attributes.json – 90 %
- [ ] class-balance.json – 90 %
- [ ] companions.json – 95 %
- [ ] item-scaling.json – 90 %
- [ ] mob-scaling.json – 90 %
- [ ] quests.json – 90 %
- [ ] quests_v2.json – 50 %
- [ ] JSON Path Safety – 95 %
- [ ] Default Data Copy – 90 %
- [ ] Data Validation / Failure Handling – 80 %

## Language
- [ ] LanguageManager – 90 %
- [ ] English (`en.yml`) – 90 %
- [ ] German (`de.yml`) – 90 %
- [ ] Spanish (`es.yml`) – 90 %
- [ ] French (`fr.yml`) – 90 %
- [ ] English Fallback – 95 %
- [ ] Placeholder Replacement – 90 %
- [ ] MiniMessage Rendering – 90 %
- [ ] Actionbar Translation – 90 %
- [ ] Language Completeness Audit – 75 %

---

# 22 – Scoreboard / Playtime

- [ ] ScoreboardService – 85 %
- [ ] PlaytimeTracker – 85 %
- [ ] Character Card Scoreboard – 80 %
- [ ] RPG Stats → Scoreboard – 78 %
- [ ] Player Level → Scoreboard – 85 %
- [ ] Player Class → Scoreboard – 85 %
- [ ] Playtime Display – 80 %
- [ ] Quest/Progression → Scoreboard – 65 %
- [ ] Scoreboard Update Interval – 90 %
- [ ] Playtime Autosave – 88 %
- [ ] Playtime Persistence – 82 %
- [ ] Scoreboard Toggle State – 75 %

---

# 23 – Travel

- [ ] Guild Compass Item Factory – 90 %
- [ ] Guild Compass Marker – 90 %
- [ ] Guild Compass Listener – 90 %
- [ ] Unlocked Waypoints – 85 %
- [ ] Waypoint Persistence – 85 %
- [ ] Nearest Travel NPC Resolution – 90 %
- [ ] Distance Display – 90 %
- [ ] Direction Display – 90 %
- [ ] Travel Dialogue – 85 %
- [ ] NPC Travel Behavior – 85 %
- [ ] Travel Permissions – 70 %
- [ ] Travel E2E – 65 %

---

# 24 – Runtime / Events / Tasks / Cleanup

## Bootstrap / Lifecycle
- [ ] zentrale Plugin-Initialisierung – 90 %
- [ ] PixelRPGBootstrap – 90 %
- [ ] Plugin Lifecycle – 90 %
- [ ] Service Registration – 90 %
- [ ] Listener Registration – 90 %
- [ ] Shutdown Ordering – 88 %

## Tasks
- [ ] Player Save Executor – 90 %
- [ ] Quest Timer Task – 85 %
- [ ] Quest Passive Check Task – 82 %
- [ ] Companion Runtime Task – 95 %
- [ ] Companion Passive Stats Task – 85 %
- [ ] Mob Scaling Task – 85 %
- [ ] NPC Look Task – 82 %
- [ ] Boss Spawn Task – 85 %
- [ ] Boss Phase / Bar Tasks – 82 %
- [ ] Scoreboard Task – 85 %
- [ ] Playtime Autosave – 88 %

## Cleanup / Safety
- [ ] Player Logout Cleanup – 90 %
- [ ] Companion Entity Cleanup – 98 %
- [ ] Companion Runtime Cleanup – 98 %
- [ ] Party Disconnect Cleanup – 88 %
- [ ] Weapon Ability Cooldown Cleanup – 95 %
- [ ] Boss Shutdown Cleanup – 85 %
- [ ] Scoreboard Shutdown Cleanup – 90 %
- [ ] Playtime Shutdown Cleanup – 90 %
- [ ] Runtime Memory Safety – 78 %
- [ ] Invalid Entity Safety – 85 %
- [ ] Invalid Definition Safety – 85 %
- [ ] Performance Audit – 75 %

---

# 25 – Build / CI / Testing

## Build
- [x] Java 25 Toolchain – 98 %
- [x] Paper 26.2 Dev Bundle – 98 %
- [x] Paperweight Userdev 2.0.0-beta.21 – 98 %
- [x] Mojang-Mappings Runtime – 98 %
- [x] `paper-plugin.yml` – 98 %
- [x] ShadowJar – 98 %
- [ ] Final Production Build Verification – 90 %

## CI
- [ ] GitHub Build Workflow – 85 %
- [ ] Automated Gradle Build – 85 %
- [ ] Build Artifact Verification – 70 %
- [ ] Failure Diagnostics – 70 %

## Testing
- [ ] Unit Tests – 20 %
- [ ] Integration Tests – 15 %
- [ ] Gameplay Tests – 30 %
- [ ] Automated Regression Tests – 15 %
- [ ] Performance Tests – 15 %
- [ ] Edge-Case Tests – 25 %
- [ ] Persistence Tests – 25 %
- [ ] Data Validation Tests – 25 %
- [ ] Multiplayer Load Tests – 10 %

---

# Repository Coverage Check

Die aktuelle Codebasis enthält die folgenden aktiven Feature-/Infrastrukturpakete und sie sind jeweils in dieser Roadmap abgebildet:

```text
api/          → 20
boss/         → 15
combat/       → 02
companion/    → 04
command/      → 19
config/       → 21
core/         → 01 / 03 / 07 / 20 / 24
dialogue/     → 06 / 11 / 17
economy/      → 11 / 14
gui/          → 17
item/         → 07 / 08 / 11
lang/         → 21
npc/          → 06
party/        → 13
player/       → 01
profession/   → 09 / 10
quest/        → 05
scoreboard/   → 22
shop/         → 11
stats/        → 03
storage/      → 18
story/        → 16
travel/       → 23
```

Zusätzlich abgebildete Ressourcen:

```text
config.yml
data/attributes.json
data/class-balance.json
data/companions.json
data/item-scaling.json
data/mob-scaling.json
data/quests.json
data/quests_v2.json
lang/de.yml
lang/en.yml
lang/es.yml
lang/fr.yml
paper-plugin.yml
build.gradle
.github/workflows/build.yml
```

## Aktueller Audit-Grundsatz

> Ein Feature wird erst auf 🟢 gesetzt, wenn wir es bei der späteren Bearbeitung gegen seinen tatsächlichen Code und sein gemeinsam definiertes Soll-Verhalten geprüft haben. Das Vorhandensein einer Klasse allein gilt nicht als „fertig“.
