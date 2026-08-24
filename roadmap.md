# PixelRPG – Feature Roadmap

Stand: 2026-08-24

> Diese Datei ist die einzige aktuelle Arbeits-Roadmap. Die Reihenfolge ist verbindlich: von oben nach unten.
> Die Prozentwerte wurden am 24.08.2026 gegen den aktuellen Repository-Baum, die zentrale Projektdokumentation, vorhandene Daten-Dateien und die aktuell vorhandenen Feature-Klassen neu bewertet.

## Statussystem

- 🟢 **95–98 % – ABGESCHLOSSEN**: Feature ist definiert, integriert und ausreichend stabil. Nur Bugs oder bewusst beschlossene neue Ideen öffnen es wieder.
- 🟡 **50–94 % – IN ARBEIT**: Funktionale Basis vorhanden, aber Soll-Verhalten oder Integration noch nicht vollständig abgeschlossen.
- 🔴 **0–49 % – OFFEN**: Feature ist nur teilweise vorhanden oder noch nicht als vollständiges Gameplay-System umgesetzt.

Die Prozentwerte beschreiben funktionale Reife, nicht Code-Menge.

## Arbeitsregel

```text
1. Soll-Verhalten gemeinsam definieren
2. aktuellen Code vollständig für dieses Feature prüfen
3. vorhandene Implementierung gegen Soll-Verhalten vergleichen
4. fehlende / fehlerhafte Punkte identifizieren
5. nur notwendige Änderungen umsetzen
6. Build durchführen
7. Server-/Runtime-Test durchführen
8. Fehler beheben
9. PixelRPG.md und roadmap.md aktualisieren
10. 95–98 % → Feature abhaken
```

---

# 01 – Player

## Profil & Lifecycle

- [ ] PlayerProfile / Grunddaten – 92 %
- [ ] PlayerProfileManager – 90 %
- [ ] PlayerProfileRepository – 85 %
- [ ] MySQL Player Repository – 85 %
- [ ] YAML Player Repository – 80 %
- [ ] Join-Lifecycle – 90 %
- [ ] Quit-Lifecycle – 90 %
- [ ] Registrierungsstatus – 85 %
- [ ] Vanilla-/PixelRPG-Spieler-Isolation – 80 %

## Level & XP

- [ ] Spieler-Level 1–99 – 92 %
- [ ] Level 100 als reserviertes Endgame – 90 %
- [ ] Spieler-XP – 88 %
- [ ] XP-Kurve – 82 %
- [ ] Level-Up-Verarbeitung – 88 %
- [ ] PlayerLevelUpEvent – 90 %
- [ ] Player-Level API – 82 %

## Klassen

- [ ] Warrior – 85 %
- [ ] Ranger – 85 %
- [ ] Rogue – 85 %
- [ ] Healer – 85 %
- [ ] Mage – 85 %
- [ ] Klassenwahl – 75 %
- [ ] Klassenwechsel – 75 %
- [ ] ClassBalance – 85 %
- [ ] Klassenwerte → Stats – 75 %
- [ ] PlayerClassChangeEvent – 90 %

## Attribute

- [ ] Attribute-Definitionen – 90 %
- [ ] Vitality – 90 %
- [ ] Agility – 90 %
- [ ] Precision – 90 %
- [ ] Range – 90 %
- [ ] Toughness – 90 %
- [ ] Attributkosten – 80 %
- [ ] Attribute → finale Stats – 75 %
- [ ] Stat-Persistenz – 80 %

---

# 02 – Combat

## Kern

- [ ] zentrale Damage-Logik – 78 %
- [ ] Damage-Modifikatoren – 72 %
- [ ] Crit Chance – 70 %
- [ ] Crit Damage – 68 %
- [ ] Angriffsgeschwindigkeit – 60 %
- [ ] Class Scaling – 75 %
- [ ] Stat Scaling – 75 %
- [ ] Player Combat – 70 %
- [ ] Mob Combat – 72 %
- [ ] Combat State – 70 %
- [ ] Combat Cleanup – 82 %
- [ ] Combat Events – 78 %

## Weapon Skills

- [ ] SkillInputListener / Rechtsklick-Erkennung – 82 %
- [ ] WeaponAbilityEngine – 80 %
- [ ] Weapon-Abilities – 78 %
- [ ] Waffen-Cooldowns – 88 %
- [ ] Ability-Validierung – 75 %
- [ ] keine Mana-Kosten – 95 %

## Mob Scaling

- [ ] Mob-Level Scaling – 85 %
- [ ] HP Scaling – 85 %
- [ ] Damage Scaling – 85 %
- [ ] Region/Danger Scaling – 80 %
- [ ] Dimension Scaling – 85 %
- [ ] Player-Parity – 75 %
- [ ] XP aus Mob-Health – 80 %
- [ ] Vanilla-/PixelRPG-Mischbetrieb – 60 %

## Loot / Death

- [ ] Mob Experience – 82 %
- [ ] Loot Drops – 75 %
- [ ] Soulbound Death Protection – 90 %
- [ ] Death-/Respawn-Integration – 80 %
- [ ] Elytra Permission – 80 %
- [ ] Mob Nameplates – 85 %

---

# 03 – Progression / Stats

- [ ] Level 1–99 End-to-End – 90 %
- [ ] Level-100-Grenze – 90 %
- [ ] XP Scaling – 85 %
- [ ] deterministische Progression – 82 %
- [ ] StatEngine – 78 %
- [ ] StatisticsService – 80 %
- [ ] RPGStatsListener – 80 %
- [ ] Mob-Kill-Statistik – 80 %
- [ ] Player-Death-Statistik – 80 %
- [ ] Quest/Boss-Statistiken – 75 %
- [ ] Player Attributes → Stats → Combat – 70 %
- [ ] Equipment → Stats → Combat – 65 %
- [ ] vollständige End-to-End-Progression – 65 %

---

# 04 – Companions

> Neue Companion-Inhalte sind aktuell nicht geplant. Dieser Bereich bleibt geschlossen und wird nur bei echten Bugs oder bewusst beschlossenen neuen Ideen wieder geöffnet.

## Core

- [x] Companion Registry – 98 %
- [x] Companion Definitions – 98 %
- [x] Companion Ownership – 95 %
- [x] Companion Unlock – 95 %
- [x] Unique Companion Unlock – 95 %
- [x] Aktivieren / Deaktivieren – 98 %
- [x] Spawn – 95 %
- [x] Despawn – 98 %
- [x] Logout Cleanup – 98 %
- [x] kein automatischer Restore beim Login – 98 %
- [x] Follow-System – 95 %
- [x] Movement-Typen – 95 %
- [x] Passive Companions – 95 %
- [x] Passive Stats nur bei aktivem Companion – 95 %
- [x] Combat-Regeln – 95 %
- [x] Hostile-Mob-Targeting – 95 %
- [x] Spieler-/Tier-/freundliche Ziele ausgeschlossen – 95 %
- [x] Companion XP – 95 %
- [x] Companion Level 1–99 – 95 %
- [x] Unique Level 1–99 – 95 %
- [x] Companion Equipment – 95 %
- [x] Companion Abilities – 95 %
- [x] Companion Runtime Registry – 98 %
- [x] Runtime Cleanup – 98 %
- [x] Runtime Tick Performance – 95 %
- [x] Companion Persistence – 95 %

## Aktuell konfigurierte Companion-Content-Typen

- [x] Common passive Companions – 95 %
- [x] Uncommon passive/combat Companions – 95 %
- [x] Epic Companions – 95 %
- [x] Legendary Companions – 95 %
- [x] Unique Hazel Mannequin – 95 %
- [x] datengetriebene Unlocks – 95 %
- [x] Rarities Common → Unique – 95 %
- [x] Rename-Regeln – 95 %

## Mounts

- [x] Pig Ground Mount – 95 %
- [x] Horse Ground Mount – 95 %
- [x] Zombie Horse Ground Mount – 95 %
- [x] Skeleton Horse Ground Mount – 95 %
- [x] Nautilus Underwater Mount – 90 %
- [ ] Camel Mount – 50 %
- [ ] Bee Spezial-Flugmount – 50 %
- [ ] Mount Ownership / Fremd-Mount-Schutz – 90 %
- [ ] Mount Cleanup – 90 %
- [ ] Mount Restart-Verhalten – 75 %

## Unique Hazel

- [x] Mannequin Companion – 95 %
- [x] Skin Resolver – 95 %
- [x] eigener Combat Controller – 95 %
- [x] ADMIN-only – 98 %

---

# 05 – Quests

## Core

- [ ] Quest Definition – 88 %
- [ ] Quest Repository – 88 %
- [ ] Quest Manager – 88 %
- [ ] Quest Progress – 88 %
- [ ] Quest Completion – 82 %
- [ ] Quest Rewards – 78 %
- [ ] Quest XP – 88 %
- [ ] Quest XP Scaling – 88 %
- [ ] maximal 5 aktive Quests – 82 %
- [ ] Quest Persistence – 82 %
- [ ] QuestCompletedEvent – 90 %

## Questtypen

- [ ] HUNT – 88 %
- [ ] COLLECT – 82 %
- [ ] TALK_TO_NPC – 88 %
- [ ] ESCORT – 60 %
- [ ] REACH_LOCATION – 82 %
- [ ] GLOBAL_EVENT – 78 %

## Tracking / Navigation

- [ ] Mob-Kill-Tracking – 88 %
- [ ] passive Quest Checks – 78 %
- [ ] GlobalEventState – 78 %
- [ ] Quest Navigation – 82 %
- [ ] Recovery Compass – 65 %
- [ ] Questketten – 68 %
- [ ] aktiver Quest-Unlock-Flow E2E – 65 %

## Daten

- [ ] quests.json als primäre Quelle – 85 %
- [ ] quests_v2.json Legacy-/Migrationsstatus – 50 %
- [ ] Datenkonsistenz / doppelte Questquellen – 60 %

---

# 06 – NPC / Dialogue

## NPC

- [ ] RPGNpc – 80 %
- [ ] NpcManager – 82 %
- [ ] NpcType – 80 %
- [ ] NpcBehavior – 82 %
- [ ] Behavior Registry – 80 %
- [ ] NPC Spawn – 82 %
- [ ] NPC Interaktion – 82 %
- [ ] NPC Look Task – 78 %
- [ ] Chunk Lifecycle – 70 %
- [ ] Login Resync – 78 %
- [ ] NPC Persistence – 72 %

## NPC Behaviors

- [ ] Banker – 85 %
- [ ] Blacksmith – 82 %
- [ ] Filler – 80 %
- [ ] Profession Trainer – 75 %
- [ ] Quest NPC – 85 %
- [ ] Reception – 85 %
- [ ] Shop – 75 %
- [ ] Story – 78 %
- [ ] Travel – 78 %

## Dialogue

- [ ] DialogueEngine – 88 %
- [ ] Native Minecraft/Paper Dialog Framework – 88 %
- [ ] Dialogue Definitions – 82 %
- [ ] Dialogue Choices – 82 %
- [ ] Dialogue Actions – 80 %
- [ ] NPC → Dialogue – 88 %
- [ ] Quest Dialogue – 88 %
- [ ] Companion Dialogue – 88 %
- [ ] Profession Dialogue – 72 %
- [ ] Shop Dialogue – 75 %
- [ ] Travel Dialogue – 78 %
- [ ] Bank Dialogue – 82 %
- [ ] Story Dialogue – 78 %
- [ ] Soulbound Dialog Action – 95 %
- [ ] Quick Actions Dialog – 80 %

---

# 07 – Items

- [ ] RPGItemBuilder – 92 %
- [ ] ItemService – 88 %
- [ ] Item Definitions – 82 %
- [ ] ItemCategory – 88 %
- [ ] GearCategoryRegistry – 82 %
- [ ] ItemRarity – 92 %
- [ ] Common – 92 %
- [ ] Uncommon – 92 %
- [ ] Rare – 92 %
- [ ] Epic – 92 %
- [ ] Legendary – 92 %
- [ ] Unique – 92 %
- [ ] ItemStatProfile – 78 %
- [ ] Item Stats – 78 %
- [ ] Item Level – 68 %
- [ ] Gearscore – 60 %
- [ ] Custom Metadata / PDC Identity – 90 %
- [ ] CraftedItemFactory – 75 %
- [ ] SoulboundService – 92 %
- [ ] Item Economy Values – 72 %
- [ ] ItemAPI – 78 %
- [ ] Vanilla Item Isolation – 65 %
- [ ] Resourcepack Item Integration – 50 %
- [ ] Item Scaling – 70 %
- [ ] Item Identity / Validation E2E – 70 %

---

# 08 – Equipment

- [ ] Equipment Slots – 82 %
- [ ] Armor – 78 %
- [ ] Weapons – 72 %
- [ ] Mainhand – 82 %
- [ ] Offhand – 68 %
- [ ] Equipment Stats – 72 %
- [ ] Armor Stats – 72 %
- [ ] Weapon Stats – 72 %
- [ ] Equipment Restrictions – 62 %
- [ ] Level Requirements – 58 %
- [ ] Rarity Integration – 82 %
- [ ] Equipment → Player Stats – 68 %
- [ ] Equipment GUI – 60 %
- [ ] Equipment Cleanup – 82 %
- [ ] Equipment Persistence – 70 %

---

# 09 – Crafting

- [ ] CraftRecipe – 75 %
- [ ] CraftingRecipeRegistry – 78 %
- [ ] CraftingService – 72 %
- [ ] Crafting Definitions – 65 %
- [ ] Crafting Recipes – 60 %
- [ ] Crafted Item Factory – 75 %
- [ ] Profession Requirements – 45 %
- [ ] Custom Crafting – 60 %
- [ ] Dialogue-based Crafting – 65 %
- [ ] Crafting GUI – 65 %
- [ ] Crafting Validation – 60 %
- [ ] Level-/Rarity-Progression – 55 %
- [ ] vorhandene Rezepte E2E – 45 %

---

# 10 – Professions

- [ ] Profession Model – 65 %
- [ ] ProfessionService – 62 %
- [ ] ProfessionSystem – 60 %
- [ ] Profession Gathering – 65 %
- [ ] Profession Levels – 50 %
- [ ] Profession XP – 45 %
- [ ] Profession 1–100 – 45 %
- [ ] Profession Recipes – 55 %
- [ ] Profession Requirements – 45 %
- [ ] Profession Trainer NPC – 75 %
- [ ] Profession Dialog – 72 %
- [ ] Profession UI – 55 %
- [ ] alle geplanten Berufe vollständig definieren – 40 %

---

# 11 – Economy

- [ ] Economy API – 75 %
- [ ] Player Currency – 60 %
- [ ] Currency Storage – 60 %
- [ ] Currency Transactions – 58 %
- [ ] Item Economy Values – 72 %
- [ ] Price Definitions – 55 %
- [ ] Guild Currency Item Factory – 70 %
- [ ] Guild Currency Pickup – 70 %
- [ ] NPC Economy – 45 %
- [ ] ShopManager – 72 %
- [ ] Shop Entry – 75 %
- [ ] Shop GUI – 60 %
- [ ] Shop Editor GUI – 65 %
- [ ] Bank Storage – 78 %
- [ ] Bank Inventory GUI – 78 %
- [ ] Economy Persistence – 60 %

---

# 12 – Trading

> Ein vollständiges Player-Trading-System ist im aktuellen Codebestand nicht als eigenes Runtime-System vorhanden.

- [ ] Player Trading – 25 %
- [ ] Trade Requests – 20 %
- [ ] Trade GUI – 20 %
- [ ] Item Validation – 20 %
- [ ] Currency Trading – 15 %
- [ ] Trade Confirmation – 15 %
- [ ] Anti-Duplication – 10 %
- [ ] Cancellation / Disconnect Handling – 10 %

---

# 13 – Party

- [ ] Party – 80 %
- [ ] PartyManager – 82 %
- [ ] Party API – 78 %
- [ ] Party Creation – 75 %
- [ ] Party Members – 75 %
- [ ] Invite System – 70 %
- [ ] Leave Party – 75 %
- [ ] Party Events – 75 %
- [ ] Party Disconnect Handling – 80 %
- [ ] Party Data – 65 %
- [ ] Party GUI – 65 %
- [ ] Party Gameplay Integration – 55 %

---

# 14 – Guilds

> Ein vollständiges Guild-System ist derzeit ausdrücklich kein zukünftiges Großfeature. Vorhandene API-/Currency-Pfade werden nicht blind entfernt.

- [ ] GuildAPI – 55 %
- [ ] Guild Join/Leave Events – 80 %
- [ ] Guild Currency – 65 %
- [ ] Guild Compass – 70 %
- [ ] Guild Compass Listener – 70 %
- [ ] Guild Persistence – 35 %
- [ ] Guild Creation – 25 %
- [ ] Guild Membership – 30 %
- [ ] Guild Ranks – 15 %
- [ ] Guild GUI – 15 %
- [ ] Scope-Entscheidung dokumentieren – 80 %

---

# 15 – Bosses

- [ ] BossDefinition – 85 %
- [ ] ActiveBoss – 82 %
- [ ] BossManager – 82 %
- [ ] BossRepository – 82 %
- [ ] BossAttackPattern – 80 %
- [ ] BossAttackPatternRegistry – 80 %
- [ ] Slam Attack – 80 %
- [ ] Projectile Volley – 80 %
- [ ] Summon Adds – 78 %
- [ ] Enrage Buff – 78 %
- [ ] BossPhase – 65 %
- [ ] Boss Combat – 75 %
- [ ] Boss Damage Contribution – 65 %
- [ ] Boss Death – 80 %
- [ ] Boss Loot Config – 70 %
- [ ] Boss Rewards – 68 %
- [ ] Boss → Companion Unlock – 88 %
- [ ] World Boss Spawn Task – 72 %
- [ ] Boss Persistence – 45 %
- [ ] Boss UI – 40 %
- [ ] Boss E2E Gameplay Loop – 45 %

---

# 16 – Story

> Story wurde im vollständigen Repository-Baum als eigener aktiver Bereich gefunden und wird deshalb neu in die Roadmap aufgenommen.

- [ ] StoryChapter – 65 %
- [ ] StoryManager – 65 %
- [ ] StoryBookFactory – 65 %
- [ ] Story → NPC Integration – 60 %
- [ ] Story → Dialogue Integration – 65 %
- [ ] Story Progress Persistence – 35 %
- [ ] Story E2E – 35 %

---

# 17 – GUI / UI

- [ ] AbstractGUI – 80 %
- [ ] GUIHolder – 80 %
- [ ] GUIListener – 80 %
- [ ] CraftingGUI – 70 %
- [ ] BlacksmithGUI – 72 %
- [ ] PartyGUI – 65 %
- [ ] QuestLogGUI – 80 %
- [ ] QuestDetailGUI – 78 %
- [ ] ShopGUI – 60 %
- [ ] ShopEditorGUI – 65 %
- [ ] Companion UI / Dialogue – 85 %
- [ ] Bank UI – 80 %
- [ ] Native Dialog UI – 88 %
- [ ] Main RPG UI / Character Card – 75 %
- [ ] Scoreboard / Character Card – 75 %
- [ ] Legacy GUI Audit – 80 %

---

# 18 – Database / Persistence / Storage

- [ ] DatabaseManager – 85 %
- [ ] HikariCP – 92 %
- [ ] MySQL Connector – 90 %
- [ ] StorageType – 85 %
- [ ] YAML Storage – 80 %
- [ ] Player Persistence – 88 %
- [ ] Companion Persistence – 95 %
- [ ] Quest Persistence – 82 %
- [ ] Economy Persistence – 60 %
- [ ] Guild Persistence – 35 %
- [ ] Item Persistence – 70 %
- [ ] Bank Persistence – 80 %
- [ ] Server-Restart Persistence – 65 %
- [ ] Connection Lifecycle – 82 %
- [ ] historische Datenfelder / Migration – 65 %

---

# 19 – Commands / Permissions

- [ ] PaperBasicCommandAdapter – 85 %
- [ ] RootCommand – 85 %
- [ ] SubCommand Framework – 85 %
- [ ] Blacksmith Command – 80 %
- [ ] Boss Command – 82 %
- [ ] Companion Command – 85 %
- [ ] NPC Command – 82 %
- [ ] Party Command – 80 %
- [ ] Quest Admin Command – 82 %
- [ ] Quest Log Command – 82 %
- [ ] Shop Command – 80 %
- [ ] Permission Framework – 75 %
- [ ] Admin Permission – 90 %
- [ ] Player Permission – 85 %
- [ ] Command Validation – 75 %
- [ ] paper-plugin.yml Registrierung – 90 %

---

# 20 – API / Events

- [ ] ApiVersion – 90 %
- [ ] PixelRPGProvider – 88 %
- [ ] EconomyAPI – 78 %
- [ ] GuildAPI – 55 %
- [ ] ItemAPI – 78 %
- [ ] PartyAPI – 78 %
- [ ] StatisticsAPI – 75 %
- [ ] BossDefeatedEvent – 88 %
- [ ] PlayerClassChangeEvent – 90 %
- [ ] PlayerJoinGuildEvent – 82 %
- [ ] PlayerLeaveGuildEvent – 82 %
- [ ] PlayerLevelUpEvent – 90 %
- [ ] QuestCompletedEvent – 90 %
- [ ] API Documentation – 45 %
- [ ] API Stability / Contracts – 70 %

---

# 21 – Configuration / Language / Data

## Configuration

- [ ] config.yml – 85 %
- [ ] JsonDataManager – 85 %
- [ ] Attribute Config – 90 %
- [ ] Class Balance Config – 90 %
- [ ] Companion Data – 95 %
- [ ] Item Scaling Data – 80 %
- [ ] Mob Scaling Data – 85 %
- [ ] Quest Data – 85 %
- [ ] Config Validation – 70 %

## Language

- [ ] LanguageManager – 82 %
- [ ] German – 85 %
- [ ] English – 85 %
- [ ] Spanish – 82 %
- [ ] French – 82 %
- [ ] Missing-Key Handling – 75 %
- [ ] Language Coverage – 70 %

## Data Cleanup

- [ ] quests.json / quests_v2.json Entscheidung – 60 %
- [ ] ungenutzte historische Data Keys – 70 %
- [ ] Resourcepack-Integration als bewusster Scope – 70 %

---

# 22 – Scoreboard / Playtime

- [ ] ScoreboardService – 78 %
- [ ] PlaytimeTracker – 78 %
- [ ] Character Card Scoreboard – 75 %
- [ ] RPG Stats → Scoreboard – 70 %
- [ ] Quest/Progression → Scoreboard – 60 %
- [ ] Persistenz der Playtime – 65 %

---

# 23 – Travel

- [ ] Guild Compass Item Factory – 70 %
- [ ] Guild Compass Listener – 70 %
- [ ] Travel Dialogue – 78 %
- [ ] NPC Travel Behavior – 78 %
- [ ] Travel Destination Validation – 65 %
- [ ] Travel Permissions – 65 %
- [ ] Travel E2E – 55 %

---

# 24 – Runtime / Events / Tasks / Cleanup

- [ ] zentrale Plugin-Initialisierung – 88 %
- [ ] Bootstrap/Plugin Lifecycle – 88 %
- [ ] Event Listener Registrierung – 85 %
- [ ] Scheduled Tasks – 82 %
- [ ] Companion Runtime Task – 95 %
- [ ] NPC Look Task – 78 %
- [ ] Quest Passive Check Task – 78 %
- [ ] World Boss Spawn Task – 72 %
- [ ] Player Cleanup – 88 %
- [ ] Companion Cleanup – 98 %
- [ ] Party Cleanup – 80 %
- [ ] NPC Cleanup – 75 %
- [ ] Database Shutdown – 85 %
- [ ] Exception Isolation – 70 %
- [ ] Runtime Memory Safety – 75 %
- [ ] Performance Audit – 72 %

---

# 25 – Build / CI / Testing

## Build

- [ ] Java 25 Toolchain – 98 %
- [ ] Paper 26.2 Dev Bundle – 98 %
- [ ] Paperweight 2.0.0-beta.21 – 98 %
- [ ] ShadowJar – 90 %
- [ ] GitHub Actions Build – 80 %
- [ ] reproduzierbarer Release-Build – 75 %

## Testing

- [ ] Unit Tests – 20 %
- [ ] Integration Tests – 15 %
- [ ] Gameplay Tests – 30 %
- [ ] Regression Tests – 15 %
- [ ] Performance Tests – 15 %
- [ ] Edge-Case Tests – 25 %
- [ ] Persistence Tests – 25 %
- [ ] Combat Tests – 20 %
- [ ] Quest E2E Tests – 25 %
- [ ] Companion E2E Tests – 45 %
- [ ] Boss E2E Tests – 20 %

---

# Definition of Done

Ein Feature wird bei **95–98 %** abgehakt, wenn:

```text
Soll-Verhalten definiert
+ vorhandener Code geprüft
+ Runtime integriert
+ Content ausreichend
+ Persistenz korrekt
+ Fehlerfälle behandelt
+ Vanilla-Isolation korrekt
+ Build erfolgreich
+ Server-/Gameplay-Test erfolgreich
+ Dokumentation aktuell
= ABGESCHLOSSEN
```

100 % wird nicht als dauerhafter Zustand angestrebt. Neue Bugs, Minecraft-/Paper-Änderungen und bewusst neue Gameplay-Ideen dürfen ein abgeschlossenes Feature später wieder öffnen.
