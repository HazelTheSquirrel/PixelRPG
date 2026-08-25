# PixelRPG – Zentrale Projektdokumentation

Stand: 2026-08-25

> Zentrale Dokumentation für das PixelRPG-Projekt. Die konkrete Abarbeitung erfolgt ausschließlich über `roadmap.md`.

## 1. Technische Basis

- Java 25
- Paper 26.x, verbindliches Entwicklungsziel: Paper 26.2
- Paperweight Userdev `2.0.0-beta.21`
- `paperweight.paperDevBundle("26.2.build.+")`
- Mojang-Mappings
- `paper-plugin.yml`
- natives Minecraft/Paper-26.2-Dialogsystem
- Adventure Components
- kein `ChatColor`
- keine alten 1.21.x-APIs oder Dialogimplementierungen

## 2. Verbindliche Designentscheidungen

- PixelRPG hat **keine Klassen**.
- PixelRPG hat **keine frei verteilbaren Player-Attribute**.
- Ein neu registrierter Spieler erhält keinen Startbonus.
- Level/XP sind die zentrale Spielerprogression.
- Combat-Stärke entsteht aus Gear, Stats, Weapon Skills und aktiven Companions.
- Character Stats: HP, Armor, Movement Speed, Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power.
- Basis-Crit-Chance ist 0 %.
- Standard-Crit-Schaden ist ×2 und kann durch Gear/Companions erhöht werden.
- Lifesteal heilt den entsprechenden Prozentsatz des tatsächlich verursachten Schadens.
- Vanilla-Schaden bleibt Bestandteil der Berechnung; Vanilla-Angriffsgeschwindigkeit bleibt unverändert.
- Rüstung wird über eine eigene MMORPG-Mitigation berechnet.
- Waffen können Reach, Damage, Crit, Crit-Schaden, Lifesteal und Attack Power liefern.
- Equipment: Helm, Brust, Hose, Schuhe, Waffe, Nebenhand.
- Gearscore basiert auf Item-Level und Item-Definition/Balancing.
- Companions geben passive Stat-Boni nur solange sie aktiv gerufen und draußen sind; Despawn entfernt den Bonus sofort. Unique Companions sind davon ausgenommen.
- PvP ist kein eigenes Gameplay-System.
- Währung ist Goldtaler; virtuell als Kontostand und physisch als Sonnenblume.
- Vollständiger Player-Trade ist nicht priorisiert; Spielerhandel erfolgt über das Auktionshaus.
- Professionen: BLACKSMITH, PROVISIONER, ALCHEMIST, SCHOLAR.
- Partys unterstützen gemeinsame XP, Loot-Verteilung, Party-Buffs und gemeinsamen Questfortschritt.
- Gilden sind ein eigenes soziales System mit Gildenstadt/-basis und Gildenbank.
- Ein eigenes Regions-/Mob-Scaling-System außerhalb der Biom-Bosse wird nicht umgesetzt.

## 3. Player / Progression

- Spieler werden zunächst als Vanilla-Spieler behandelt.
- Erst nach PixelRPG-Registrierung greifen RPG-Systeme.
- Spieler-Level: 1–99.
- Level 100 ist für Endgame reserviert.
- XP-/Level-Up-System und Player-Level-Up-Events sind vorhanden.
- Es gibt keine Klassenwahl und keine frei verteilbaren Attributpunkte.

## 4. Combat / Stats

Das aktive System umfasst:

- zentrale Damage-Logik
- Weapon-Abilities
- waffenbezogene Cooldowns
- Mob Scaling
- Loot
- Boss Damage
- RPG-Stats
- Soulbound Death Protection

Weapon-Abilities besitzen keine Mana-Kosten.

Aktive Character Stats:

- HP
- Armor
- Movement Speed
- Reach
- Damage
- Crit
- Crit-Schaden
- Lifesteal
- Attack Power

## 5. Items / Equipment

PixelRPG verwendet PDC-/Metadata-Identität als primäre Item-Identität. Das Vanilla-Material allein ist keine PixelRPG-Identität.

Raritäten:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Unique ist keine normale Zufallsrarität.

Soulbound ist eine echte Gameplay-Eigenschaft. Der Charakterprofil-Dialog besitzt eine native Aktion zum Binden eines identifizierten Gegenstands in der Haupthand. Soulbound-Gegenstände werden beim Tod registrierter PixelRPG-Spieler geschützt.

## 6. Quests

Das Quest-System besitzt Repository, Manager, Progress, Navigation, globale Events, passive Checks, Mob-Kills sowie XP-/Progressionslogik.

Verbindliche Regeln:

- Open World statt künstlicher Level-Gebiete.
- Keine Reputation.
- Keine Titel.
- Maximal 5 aktive Quests.
- Questketten statt Filler-Quests.
- NPCs und echte Orte stehen im Mittelpunkt.
- Recovery Compass ist ein Richtungshelfer, kein vollständiges GPS.
- Questdaten bleiben datengetrieben.
- Questziele müssen für Spieler sichtbar und verständlich sein.
- Aktive Quests zeigen Titel, Beschreibung, Fortschritt und relevante Informationen.
- Eine aktive Quest kann aus der Charakterkarte geöffnet und dort abgebrochen werden.

Verbindlicher Questtypen-Pool:

- `HUNT`
- `COLLECT`
- `TALK_TO_NPC`
- `REACH_LOCATION`
- `GLOBAL_EVENT`

**`ESCORT` gehört nicht zum finalen Questtypen-Pool.**

Primäre Questdatenquelle: `src/main/resources/data/quests_v2.json`.

## 7. NPC / Dialogue

Native Minecraft-/Paper-Dialoge sind der primäre Interaktionsweg.

Vorhandene Dialogbereiche umfassen unter anderem:

- Registrierung
- Empfang
- Quest
- Shop
- Travel
- Story
- Bank
- Companion
- Berufe
- Soulbound
- Charakterkarte
- Aktive Quests

Inventory-GUIs bleiben dort bestehen, wo Dialoge funktional nicht ausreichen.

NPCs sind dauerhafte PixelRPG-Weltschnittstellen und werden über den relevanten Chunk-Lifecycle verwaltet.

## 8. Charakterkarte / G-Interaktion

Die Charakterkarte wird über die native Minecraft-Quick-Action-Interaktion `minecraft:quick_actions` geöffnet.

Die Charakterkarte enthält unter anderem:

- Charakterprofil
- Aktive Quests
- Begleiter
- Berufe
- Gilde
- Schließen

Der Bereich **Aktive Quests** zeigt die echten Questtitel statt nur Quest-IDs. Beim Anklicken einer Quest öffnet sich ein Detaildialog mit Beschreibung, Ziel/Fortschritt und relevanten Belohnungs-/Zeitinformationen. Von dort kann die Quest abgebrochen oder zur Liste zurückgekehrt werden.

## 9. Companions

Companions sind Runtime-Entities und keine persistenten Welt-NPCs.

Lifecycle:

```text
Spieler besitzt Companion
→ Spieler ruft Companion
→ Companion spawnt
→ Logout
→ Companion despawnt
→ Login
→ Companion bleibt weg
→ Spieler ruft ihn erneut
```

Kein automatischer Companion-Restore beim Login.

Grundregeln:

- `test-wolf` ist Default-Companion.
- Normale Companions sind passiv, unbesiegbar und nicht kampffähig.
- Normale Companions liefern Utility und passive RPG-Boni.
- Hostile-Mob-Companions werden nicht verwendet.
- `unique-hazel` ist ADMIN-only und eine separate Ausnahme für Combat-/Skin-Mechanik.
- Companion-ID, Rarity und Unlock-Quelle sind datengetrieben.

### Mounts

Aktuell vorgesehen:

- Pig – Boden-Mount
- Horse – Boden-Mount
- Zombie Horse – Boden-Mount
- Skeleton Horse – Boden-Mount
- Nautilus – Unterwasser-Mount
- Bee – Spezial-Flugmount

## 10. Bosses

Das Repository besitzt ein eigenes Boss-System mit:

- Boss Definitions / Registry
- Active Boss
- feste Boss-Level
- Boss Stats
- BossBar
- Attack Patterns
- Damage Contribution
- Death Handling
- datengetriebenen Loot-Tables
- optionalen Companion Rewards

Biom-Bosse sind an ein Minecraft-Biom gebunden. Worldbosse sind ausschließlich Admin-gestartete Events.

## 11. Crafting / Professions

Crafting besitzt Registry, Rezepte und Progressionsstufen. Rezepte werden datengetrieben verwaltet.

Die vier Professionen sind:

- BLACKSMITH
- PROVISIONER
- ALCHEMIST
- SCHOLAR

Crafting und Berufe sind Bestandteil des vorgesehenen Gameplay-Loops.

## 12. Economy / Trading / Party / Guild

Economy- und Item-Werte werden in Shop-, Bank- und Reward-Pfade integriert.

### Party

Party besitzt:

- Party API
- Creation
- Members
- Invites
- Leave
- Kick
- Leadership Transfer
- Disband
- gemeinsame XP-/Loot-/Quest-Mechaniken
- Events und Disconnect-Handling
- persistente Profildaten

Komfortbefehl:

- `/party`
- `/party invite <Spieler>`
- `/party accept`
- `/party leave`
- `/party kick <Spieler>`
- `/party transfer <Spieler>`
- `/party disband`
- `/party info`

`/rpgparty` bleibt als bestehender Kompatibilitäts-/Komfortbefehl erhalten.

### Guild

Gilden besitzen persistente Daten, eindeutige Namen, maximal 50 Mitglieder, einen Gildenmeister, Einladungen und eine gemeinsame Gildenbank. Gildenfunktionen sind zusätzlich über NPCs/Dialoge und die dafür vorgesehenen Komfortbefehle erreichbar.

## 13. GUI / UI

Native Dialoge bleiben der bevorzugte Weg für Interaktionen.

Inventory-GUIs werden nur dort verwendet, wo sie funktional sinnvoll sind, beispielsweise für Crafting, Party oder komplexe Inventarverwaltung.

## 14. Persistence / Database

YAML und MySQL bleiben die PlayerProfile-Repositories.

- HikariCP wird verwendet.
- MySQL Connector wird verwendet.
- Player-, Companion-, Quest-, Profession-, Economy-, Party-, Equipment- und Guild-Daten besitzen Persistenzpfade.
- Persistenz über Server-Neustart ist Bestandteil der Abschlussprüfung.

## 15. API / Commands / Permissions

Vorhandene API-Bereiche umfassen unter anderem:

- PixelRPG Provider
- Economy API
- Guild API
- Item API
- Party API
- Statistics API
- Events für Boss, Level-Up und Quest Completion

Gameplay-Systeme sollen grundsätzlich über NPCs/Dialoge funktionieren. Commands sind Komfortfunktionen und keine Voraussetzung für die Kernmechaniken.

## 16. Cleanup-Regeln

Veraltete Klassen, Systeme und Dokumentationsreste werden nur nach tatsächlicher Dependency-Prüfung entfernt.

Insbesondere dürfen keine alten Klassen-, frei verteilbaren Attribut- oder `ESCORT`-Designs erneut als verbindliche Projektbestandteile dokumentiert werden.

## 17. Definition of Done

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
= Feature abgeschlossen
```

Diese Dokumentation beschreibt den Projektstand und die verbindlichen Designentscheidungen. Konkrete offene Arbeit wird ausschließlich in `roadmap.md` gepflegt.
