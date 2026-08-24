# PixelRPG – Feature Roadmap

Stand: 2026-08-24

> Diese Datei ist die **einzige aktuelle Arbeits-Roadmap** des Projekts.
> Wir arbeiten die Features von oben nach unten ab.

## Statussystem

- 🟢 **95–98 % – ABGESCHLOSSEN**: Feature wird nicht mehr aktiv angefasst, außer bei Bugs oder einer bewusst beschlossenen neuen Idee.
- 🟡 **50–94 % – IN ARBEIT**: Grundsystem vorhanden, aber definierte Punkte fehlen.
- 🔴 **0–49 % – OFFEN**: Feature ist noch nicht ausreichend umgesetzt.

Ein Prozentwert ist eine Schätzung des funktionalen Fertigstellungsgrades, nicht der Code-Menge.

## Arbeitsregel

Für jedes Feature:

```text
1. Soll-Verhalten gemeinsam definieren
2. Repository vollständig analysieren
3. vorhandene Implementierung prüfen
4. fehlende / fehlerhafte Punkte identifizieren
5. nur notwendige Änderungen umsetzen
6. Build durchführen
7. Server-/Runtime-Test durchführen
8. Fehler beheben
9. Dokumentation aktualisieren
10. 95–98 % → ABGESCHLOSSEN
```

---

# 01 – Player

## Profil & Lifecycle

- [ ] Spieler-Profil / Grunddaten – 90 %
- [ ] Spieler-Daten laden – 85 %
- [ ] Spieler-Daten speichern – 85 %
- [ ] Join-Lifecycle – 85 %
- [ ] Quit-Lifecycle – 85 %
- [ ] Vanilla-/PixelRPG-Spieler-Isolation – 75 %

## Progression

- [ ] Spieler-Level 1–99 – 90 %
- [ ] Spieler-XP – 85 %
- [ ] XP-Kurve – 80 %
- [ ] Level-Up-Verarbeitung – 85 %
- [ ] Level-Up-Event – 85 %
- [ ] Player-Level API – 80 %

## Klassen

- [ ] Player-Class-System – 70 %
- [ ] Klassenwahl – 70 %
- [ ] Klassenwechsel – 70 %
- [ ] Klassenwerte / Scaling – 75 %
- [ ] Klassenintegration in Stats – 70 %

## Spieler-Stats

- [ ] Attribute – 75 %
- [ ] Attribute → finale Stats – 70 %
- [ ] Stats API – 70 %
- [ ] Stat-Persistenz – 75 %

---

# 02 – Combat

- [ ] Zentrale Damage-Logik – 75 %
- [ ] Damage-Modifikatoren – 70 %
- [ ] Crit Chance – 65 %
- [ ] Crit Damage – 65 %
- [ ] Angriffsgeschwindigkeit – 60 %
- [ ] Weapon-Abilities – 75 %
- [ ] Weapon-Cooldowns – 85 %
- [ ] Player Combat – 65 %
- [ ] Mob Combat – 70 %
- [ ] Combat State – 70 %
- [ ] Combat Events – 75 %
- [ ] Combat Cleanup – 80 %
- [ ] Boss Damage – 70 %
- [ ] Vanilla-/PixelRPG-Mob-Isolation – 65 %

## Mob Scaling

- [ ] HP Scaling – 80 %
- [ ] Damage Scaling – 80 %
- [ ] Dimension Scaling – 80 %
- [ ] Player-Parity Scaling – 70 %
- [ ] XP Scaling aus Mob Health – 75 %
- [ ] Vanilla-/PixelRPG-Mischbetrieb – 50 %

---

# 03 – Progression

- [ ] Level 1–99 – 90 %
- [ ] Level 100 Endgame-Grenze – 90 %
- [ ] XP Scaling – 85 %
- [ ] Level-Up Events – 85 %
- [ ] Stat Progression – 70 %
- [ ] Klassen-Progression – 65 %
- [ ] Equipment-Progression – 65 %
- [ ] deterministische Progression – 80 %
- [ ] vollständige End-to-End-Progression – 60 %

---

# 04 – Companions

> Neue Companion-Inhalte sind aktuell nicht geplant. Dieser Bereich wird nur noch auf Fehler, Integration und definierte Restpunkte geprüft.

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
- [x] Companion XP – 95 %
- [x] Companion Level 1–99 – 95 %
- [x] Unique Level 1–99 – 95 %
- [x] Companion Equipment – 90 %
- [x] Companion Abilities – 85 %
- [x] Runtime Registry – 95 %
- [x] Runtime Cleanup – 95 %
- [x] Runtime Performance – 95 %
- [x] Companion Persistence – 95 %

## Mounts

- [ ] Pig Mount – 90 %
- [ ] Horse Mount – 90 %
- [ ] Zombie Horse Mount – 90 %
- [ ] Skeleton Horse Mount – 90 %
- [ ] Nautilus Mount – 85 %
- [ ] Bee Flugmount – 80 %
- [ ] Mount Ownership / Fremd-Mount-Schutz – 90 %
- [ ] Mount Cleanup – 90 %
- [ ] Mount Persistence / Restore nach Restart – 75 %

## Unique Mannequin

- [ ] Unique-Hazel Mannequin – 90 %
- [ ] Skin Resolver – 90 %
- [ ] eigener Combat Controller – 85 %

---

# 05 – Quests

- [ ] Quest Definition – 85 %
- [ ] Quest Repository / Registry – 85 %
- [ ] Quest Manager – 85 %
- [ ] Quest Progress – 85 %
- [ ] Quest Completion – 80 %
- [ ] Quest Rewards – 75 %
- [ ] Quest XP – 85 %
- [ ] Quest XP Scaling – 85 %
- [ ] HUNT – 85 %
- [ ] COLLECT – 80 %
- [ ] TALK_TO_NPC – 85 %
- [ ] ESCORT – 60 %
- [ ] REACH_LOCATION – 80 %
- [ ] GLOBAL_EVENT – 75 %
- [ ] Mob-Kill-Tracking – 85 %
- [ ] Quest Navigation – 80 %
- [ ] Recovery Compass – 65 %
- [ ] Passive Quest Checks – 75 %
- [ ] Global Event State – 75 %
- [ ] Quest Events – 85 %
- [ ] Quest Persistence – 80 %
- [ ] maximal 5 aktive Quests – 80 %
- [ ] Questketten – 65 %
- [ ] alle aktiven Quest-Unlocks E2E testen – 60 %

---

# 06 – NPC / Dialogue

## NPC

- [ ] NPC Grundsystem – 75 %
- [ ] NPC Definitions – 75 %
- [ ] NPC Spawn – 80 %
- [ ] NPC Interaktion – 80 %
- [ ] NPC Persistence – 70 %
- [ ] Chunk Lifecycle – 65 %
- [ ] Login Resync – 75 %
- [ ] Quest Integration – 75 %
- [ ] Shop Integration – 50 %
- [ ] Service-NPCs – 60 %

## Dialogue

- [ ] Native Dialog Framework – 85 %
- [ ] Dialogue Definitions – 80 %
- [ ] Dialogue Choices – 80 %
- [ ] Dialogue Actions – 75 %
- [ ] NPC → Dialogue – 85 %
- [ ] Quest Dialogue – 85 %
- [ ] Shop Dialogue – 70 %
- [ ] Travel Dialogue – 70 %
- [ ] Bank Dialogue – 75 %
- [ ] Companion Dialogue – 85 %
- [ ] Profession Dialogue – 65 %
- [ ] Soulbound Dialog Action – 90 %

---

# 07 – Items

- [ ] RPGItemBuilder – 90 %
- [ ] Item Definitions – 80 %
- [ ] Item Service – 85 %
- [ ] Item Categories – 85 %
- [ ] Gear Categories – 80 %
- [ ] Common – 90 %
- [ ] Uncommon – 90 %
- [ ] Rare – 90 %
- [ ] Epic – 90 %
- [ ] Legendary – 90 %
- [ ] Unique – 90 %
- [ ] Item Stats – 75 %
- [ ] Item Level – 65 %
- [ ] Gearscore – 60 %
- [ ] Custom Item Metadata / PDC – 85 %
- [ ] Crafted Items – 70 %
- [ ] Soulbound – 90 %
- [ ] Item Economy Values – 70 %
- [ ] Item API – 75 %
- [ ] Vanilla Item Isolation – 60 %
- [ ] Resourcepack Item Integration – 50 %

---

# 08 – Equipment

- [ ] Equipment Slots – 80 %
- [ ] Armor – 75 %
- [ ] Weapons – 70 %
- [ ] Mainhand – 80 %
- [ ] Offhand – 65 %
- [ ] Equipment Stats – 70 %
- [ ] Armor Stats – 70 %
- [ ] Weapon Stats – 70 %
- [ ] Equipment Restrictions – 60 %
- [ ] Level Requirements – 55 %
- [ ] Rarity Integration – 80 %
- [ ] Equipment → Player Stats – 65 %
- [ ] Equipment GUI – 60 %
- [ ] Equipment Cleanup – 80 %

---

# 09 – Crafting

- [ ] Crafting Registry – 70 %
- [ ] Crafting Definitions – 55 %
- [ ] Crafting Recipes – 50 %
- [ ] Crafted Item Factory – 70 %
- [ ] Profession Requirements – 35 %
- [ ] Custom Crafting – 50 %
- [ ] Dialogue-based Crafting – 60 %
- [ ] Crafting GUI – 55 %
- [ ] Crafting Validation – 50 %
- [ ] Level-/Rarity-Progression – 50 %
- [ ] vorhandene Rezepte E2E testen – 40 %

---

# 10 – Professions

- [ ] Profession Framework – 40 %
- [ ] Profession Levels – 35 %
- [ ] Profession XP – 30 %
- [ ] Profession 1–100 – 30 %
- [ ] Profession Recipes – 35 %
- [ ] Profession Requirements – 30 %
- [ ] Profession UI – 25 %
- [ ] Profession Integration – 30 %
- [ ] alle vier vorhandenen Berufe vollständig definieren – 35 %

---

# 11 – Economy

- [ ] Economy API – 75 %
- [ ] Player Currency – 60 %
- [ ] Currency Storage – 60 %
- [ ] Currency Transactions – 55 %
- [ ] Item Economy Values – 65 %
- [ ] Price Definitions – 50 %
- [ ] NPC Economy – 40 %
- [ ] Shop – 45 %
- [ ] Bank – 70 %
- [ ] Economy GUI – 35 %
- [ ] Economy Persistence – 60 %

---

# 12 – Trading

- [ ] Player Trading – 35 %
- [ ] Trade Requests – 30 %
- [ ] Trade GUI – 30 %
- [ ] Item Validation – 30 %
- [ ] Currency Trading – 25 %
- [ ] Trade Confirmation – 25 %
- [ ] Anti-Duplication – 25 %
- [ ] Trade Cancellation / Disconnect Handling – 20 %

---

# 13 – Party

- [ ] Party API – 75 %
- [ ] Party Creation – 70 %
- [ ] Party Members – 70 %
- [ ] Invite System – 65 %
- [ ] Leave Party – 70 %
- [ ] Party Events – 70 %
- [ ] Party Data – 60 %
- [ ] Party GUI – 55 %
- [ ] Party Gameplay Integration – 50 %
- [ ] Party Lifecycle / Disconnect Handling – 60 %

---

# 14 – Guilds

> Ein vollständiges Guild-System ist derzeit außerhalb des zukünftigen Scopes. Vorhandene aktive Guild-/Economy-Currency-Pfade werden nicht blind entfernt.

- [ ] Aktive Guild API prüfen – 60 %
- [ ] Guild Currency-Pfade prüfen – 60 %
- [ ] Historische Guild-Reste klassifizieren – 50 %
- [ ] Entscheidung über verbleibende Guild-Komponenten dokumentieren – 40 %

---

# 15 – Bosses

- [ ] Boss Definition – 80 %
- [ ] Active Boss – 80 %
- [ ] Boss Attack Patterns – 70 %
- [ ] Boss Pattern Registry – 70 %
- [ ] Boss Combat – 70 %
- [ ] Boss Phases – 60 %
- [ ] Boss Damage Contribution – 60 %
- [ ] Boss Death – 75 %
- [ ] Boss Rewards – 65 %
- [ ] Boss Loot – 65 %
- [ ] Boss → Companion Unlock – 85 %
- [ ] Boss Lifecycle – 70 %
- [ ] Boss Persistence – 40 %
- [ ] Boss UI – 40 %
- [ ] Boss E2E-Test – 35 %

---

# 16 – GUI / UI

- [ ] GUI Framework – 75 %
- [ ] Main RPG GUI – 65 %
- [ ] Quest GUI – 75 %
- [ ] Companion GUI – 80 %
- [ ] Equipment GUI – 60 %
- [ ] Item GUI – 60 %
- [ ] Party GUI – 55 %
- [ ] Dialogue UI – 85 %
- [ ] Admin GUI – 65 %
- [ ] Shop GUI – 45 %
- [ ] Bank GUI – 70 %
- [ ] Native Minecraft UI Integration – 75 %
- [ ] Legacy GUI Audit – 75 %

---

# 17 – Database / Persistence

- [ ] Database Connection – 85 %
- [ ] HikariCP – 90 %
- [ ] MySQL – 85 %
- [ ] YAML Repository – 80 %
- [ ] Player Persistence – 85 %
- [ ] Companion Persistence – 95 %
- [ ] Quest Persistence – 80 %
- [ ] Economy Persistence – 60 %
- [ ] Guild Persistence / aktive Pfade – 40 %
- [ ] Item Persistence – 70 %
- [ ] Connection Lifecycle – 80 %
- [ ] Server-Restart-Persistenz – 60 %
- [ ] historische DB-Spalten prüfen – 70 %

---

# 18 – Commands / Permissions

- [ ] Admin Commands – 80 %
- [ ] Party Commands – 75 %
- [ ] Quest Commands – 75 %
- [ ] Dialogue Commands – 75 %
- [ ] Permission Framework – 70 %
- [ ] Admin Permission – 85 %
- [ ] Player Permission – 80 %
- [ ] Command Validation – 70 %
- [ ] Command-Orphans prüfen – 75 %

---

# 19 – API / Integration

- [ ] PixelRPG Provider – 85 %
- [ ] API Versioning – 80 %
- [ ] Economy API – 75 %
- [ ] Guild API – 60 %
- [ ] Item API – 75 %
- [ ] Party API – 75 %
- [ ] Statistics API – 70 %
- [ ] Custom Events – 80 %
- [ ] API Documentation – 40 %
- [ ] API-Orphans / tote Schnittstellen – 60 %

---

# 20 – Configuration / Language

- [ ] Configuration Framework – 80 %
- [ ] Companion Configuration – 90 %
- [ ] Quest Configuration – 80 %
- [ ] Item Configuration – 75 %
- [ ] Economy Configuration – 70 %
- [ ] Language System – 65 %
- [ ] Message Management – 70 %
- [ ] Default Configuration – 80 %
- [ ] Language Keys gegen Code prüfen – 50 %
- [ ] alte Rank/Gem/Rune/Socket/HUD-Texte entfernen – 50 %
- [ ] Config Keys gegen Code abgleichen – 60 %
- [ ] PDC Keys / historische Datenpfade prüfen – 55 %

---

# 21 – Runtime / Events / Tasks

- [ ] Scheduled Tasks – 80 %
- [ ] Companion Runtime Task – 95 %
- [ ] Quest Passive Task – 75 %
- [ ] Event Listener System – 80 %
- [ ] Player Lifecycle – 85 %
- [ ] Shutdown Cleanup – 85 %
- [ ] Runtime Cleanup – 90 %
- [ ] Error Handling – 70 %
- [ ] Performance Safety – 75 %
- [ ] Listener-Orphans prüfen – 70 %
- [ ] Task-Orphans prüfen – 70 %

---

# 22 – Testing / Abschluss

- [ ] Unit Tests – 20 %
- [ ] Integration Tests – 15 %
- [ ] Gameplay Tests – 30 %
- [ ] Automated Regression Tests – 15 %
- [ ] Performance Tests – 15 %
- [ ] Edge-Case Tests – 25 %
- [ ] Build / CI – 75 %

## End-to-End-Testmatrix

- [ ] Registrierung → NPC → Dialog → Quest → Combat → XP → Loot/Reward
- [ ] Registrierung → Companion Unlock → Auswahl → Spawn → Follow → Despawn → erneutes Rufen
- [ ] Spieler-Level 1 → 99
- [ ] Equipment → Stats → Combat → Loot
- [ ] Crafting → Profession → Item → Equipment
- [ ] Quest → Boss → Reward → Companion Unlock
- [ ] Shop / Bank / Travel
- [ ] Party
- [ ] Story
- [ ] Language / UI
- [ ] Server-Neustart → Persistenz
- [ ] Vanilla-Spieler-Isolation

## Abschlussregel

Ein Bereich wird erst als **🟢 ABGESCHLOSSEN** markiert, wenn alle relevanten Unterfeatures geprüft wurden und die Definition of Done aus `PixelRPG.md` erfüllt ist.

Neue Ideen dürfen einen abgeschlossenen Bereich später bewusst wieder öffnen. Das passiert nur durch eine neue Designentscheidung oder einen tatsächlichen Bug – nicht durch endloses Polishing ohne konkreten Nutzen.
