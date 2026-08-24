# PixelRPG – Zentrale Projektdokumentation

Stand: 2026-08-24

> Zentrale Dokumentation für das PixelRPG-Projekt. Historische Einzel-Dokumentationen wurden hier zusammengeführt. Die konkrete Abarbeitung erfolgt ausschließlich über `roadmap.md`.

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

## 2. Entwicklungsprinzip

PixelRPG wird Feature für Feature fertiggestellt. Ein Feature gilt als abgeschlossen, wenn es implementiert, integriert, spielbar, persistent, robust, getestet und dokumentiert ist.

Zielbereich: **95–98 % = abgeschlossen**. Absolute 100 % werden nicht als dauerhafter Zustand betrachtet, weil zukünftige Bugs, Minecraft-Änderungen oder neue Gameplay-Ideen jederzeit auftreten können.

Arbeitsweise:

```text
Soll-Verhalten definieren
→ vorhandenen Code vollständig prüfen
→ Lücken identifizieren
→ nur notwendige Änderungen umsetzen
→ Build
→ Server-/Runtime-Test
→ Fehler beheben
→ Dokumentation aktualisieren
→ Feature abhaken
```

## 3. Aktueller Scope

### Bewusst außerhalb des zukünftigen Scopes

- Mana als Gameplay-Ressource
- Blessings
- Curses
- Gems
- Runes
- Sockets
- alte F–S-Ranks
- vollständiges eigenständiges Resourcepack-System
- vollständiges Guild-System als zukünftiges Großfeature

Historische Klassen, Keys oder Texte dieser Systeme sind nicht automatisch aktiv. Bei Cleanup wird ihre tatsächliche Verwendung geprüft.

### Aktive Progressionsbasis

```text
Spieler-Level 1–99
Level 100 = reserviertes Endgame
Spieler-Level → Attribute/Klasse → Equipment → finale Stats → Combat/Mob Scaling → XP/Loot
```

Keine Mana-Kosten für Weapon-Abilities.

## 4. Player / Progression

- Spieler werden zunächst als Vanilla-Spieler behandelt.
- Erst nach PixelRPG-Registrierung greifen RPG-Systeme.
- Spieler-Level: 1–99.
- Level 100 ist für Endgame reserviert.
- XP-Kurve bis Level 79 ist WotLK-inspiriert; ab Level 80 kontrollierte Endgame-Kurve.
- Klassen: Warrior, Ranger, Rogue, Healer, Mage.
- Klassen- und Stat-Systeme greifen nur auf registrierte PixelRPG-Spieler.
- Player-Level-Up-Events und Statistik-APIs sind vorhanden.

## 5. Attribute / Balancing

Aktive Attribute stammen aus `src/main/resources/data/attributes.json`.

| Attribut | Effekt pro Punkt |
|---|---:|
| Vitality | +2 HP |
| Agility | +0.0025 Bewegungsgeschwindigkeit |
| Agility | +0.20 % Crit Chance |
| Precision | +0.75 Bonus Damage |
| Range | +0.10 Block Interaction Range |
| Range | +0.10 Entity Interaction Range |
| Toughness | +1 Armor |

Kosten-Baseline:

- Base: `30.0`
- Multiplikator: `1.08`
- Klassenrabatt für Primärattribute: `0.75`
- Soulview: `500.0`
- Elytra Permit: `750.0`

Klassen-Baseline liegt in `data/class-balance.json`.

| Klasse | Armor | Health | Speed | Melee | Ranged | Spell | Heal | Crit | Crit Damage |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Warrior | 5.0 | 10.0 | 0 | 1.05 | 0.95 | 0.90 | 1.00 | 0 | 1.00 |
| Ranger | 0 | 3.0 | 0.0025 | 0.95 | 1.05 | 1.00 | 1.00 | 3 | 1.00 |
| Rogue | 0 | 0 | 0.002 | 1.00 | 1.00 | 1.00 | 1.00 | 5 | 1.15 |
| Healer | 3.0 | 10.0 | 0 | 0.95 | 0.95 | 1.00 | 1.15 | 0 | 1.00 |
| Mage | -2.0 | -2.0 | 0 | 0.85 | 1.00 | 1.15 | 1.00 | 0 | 1.00 |

## 6. Combat

Vorhanden bzw. Bestandteil des aktiven Systems:

- zentrale Damage-Logik
- Weapon-Abilities
- waffenbezogene Cooldowns
- Mob Scaling
- Loot
- Boss Damage
- Class-/Stat-Einfluss
- Soulbound Death Protection

Weapon-Abilities werden per Rechtsklick ausgelöst und besitzen waffenbezogene Cooldowns. Es gibt keine Mana-Kosten.

Mob Scaling Baseline:

- HP pro Level: `5.0`
- Damage pro Level: `0.30`
- Player-Parity-Multiplier: `1.05`
- XP pro Max-Health: `4.0`

| Dimension | HP | Damage | Base Level |
|---|---:|---:|---:|
| Overworld | 1.00 | 1.00 | 1 |
| Nether | 1.15 | 1.10 | 50 |
| The End | 1.30 | 1.20 | 75 |

Das Mischverhalten von Vanilla- und registrierten PixelRPG-Spielern bleibt Bestandteil der Runtime-Validierung.

## 7. Items / Equipment

PixelRPG verwendet PDC-/Metadata-Identität als primäre Item-Identität. Das Vanilla-Material allein ist keine PixelRPG-Identität.

Raritäten:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Item-Scaling-Baseline aus `data/item-scaling.json`:

```text
Growth multiplier = 10.0
Weapon damage base = 3.0
Weapon crit base = 0.5
Armor base = 0.7
Health base = 1.2
Tool efficiency base = 1.0
```

| Rarity | Multiplikator |
|---|---:|
| Common | 1.00 |
| Uncommon | 1.10 |
| Rare | 1.22 |
| Epic | 1.38 |
| Legendary | 1.60 |
| Unique | 1.60 |

Unique ist keine normale Zufallsrarität.

Soulbound ist eine echte Gameplay-Eigenschaft. Der aktuelle Charakterprofil-Dialog besitzt eine native Aktion zum Binden des identifizierten Gegenstands in der Haupthand. Der Death-/Respawn-Pfad schützt Soulbound-Gegenstände vor dem normalen Drop registrierter PixelRPG-Spieler.

## 8. Quests

Das Quest-System besitzt Repository, Manager, Progress, Navigation, globale Events, passive Checks, Mob-Kills sowie XP-/Progressionslogik.

Verbindliche Designregeln:

- Open World statt künstlicher Level-Gebiete.
- Keine Reputation.
- Keine Titel.
- Maximal 5 aktive Quests als aktueller Designwert.
- Questketten statt Filler-Quests.
- NPCs und echte Orte stehen im Mittelpunkt.
- Recovery Compass ist ein Richtungshelfer, kein vollständiges GPS.
- Veränderliche Questdaten bleiben datengetrieben.

Questtypen:

- `HUNT`
- `COLLECT`
- `TALK_TO_NPC`
- `ESCORT`
- `REACH_LOCATION`
- `GLOBAL_EVENT`

Primäre Questdatenquelle: `src/main/resources/data/quests.json`.

## 9. NPC / Dialogue

Native Minecraft-/Paper-Dialoge sind der primäre Interaktionsweg.

Vorhandene Dialogbereiche umfassen unter anderem:

- Registrierung
- Empfang
- Schmied
- Quest
- Shop
- Travel
- Story
- Bank
- Companion
- Berufe
- Soulbound

Inventory-GUIs bleiben dort bestehen, wo Dialoge nicht ausreichen.

NPCs sind dauerhafte PixelRPG-Weltschnittstellen und müssen nach Login und relevantem Chunk-Lifecycle wieder sichtbar sein.

## 10. Companions

Companions sind Runtime-Entities und **keine persistenten Welt-NPCs**.

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
- `unique-hazel` ist ADMIN-only und die einzige normale Ausnahme für eigenen Combat-/Skin-Mechanismus.
- Companion-ID, Rarity und Unlock-Quelle sind datengetrieben in `companions.json`.
- Java enthält keine Companion-ID-spezifische Unlock-Sonderlogik.

Unlocks sind datengetrieben über Quest-/Boss-Quellen. Raritäten: Common, Uncommon, Rare, Epic, Legendary, Unique.

Passive Boni können unter anderem Leben, Rüstung, Lifesteal, Crit-Chance und Crit-Schaden beeinflussen, sofern konfiguriert.

Companion Runtime, Follow, Movement, Passive-System, Combat-Regeln, XP/Level, Equipment, Runtime-Registry und Cleanup wurden bereits umfassend poliert. Neue Companion-Inhalte sind aktuell nicht geplant.

### Mounts

Aktuell vorgesehen:

- Pig – Boden-Mount
- Horse – Boden-Mount
- Zombie Horse – Boden-Mount
- Skeleton Horse – Boden-Mount
- Nautilus – Unterwasser-Mount
- Bee – Spezial-Flugmount

Bee-Zielwerte:

- Scale `1.60`
- Fluggeschwindigkeit `1.05` Blöcke/Tick
- Steuerung über Paper 26.x `Player#getCurrentInput()`
- Jump steigt, Sneak sinkt
- keine Vanilla-Bienen-Kampfmechanik
- unbesiegbar und passiv

Unique-Hazel bleibt ein separates `MANNEQUIN`-System mit eigenem Combat-Controller und Skin-Resolver.

## 11. Bosses

Das Repository besitzt ein eigenes Boss-System mit Active Boss, Attack Patterns, Pattern Registry, Combat und Boss-Events. Bosses können als Unlock-Quelle für Companions dienen.

Für den Abschluss müssen Phasen/Patterns, Damage Contribution, Death, Loot und Companion Rewards als vollständiger Gameplay-Loop validiert werden.

## 12. Crafting / Professions

Crafting besitzt eine Registry, Progressionsstufen und Rezepte. Die vier vorhandenen Berufe wurden um zusätzliche Level-/Rarity-Stufen erweitert.

Die zusätzlichen Rezepte gelten erst nach erfolgreichem Build und Server-Test als abgeschlossen.

Dialog-basiertes Crafting und Berufe sind Teil des vorgesehenen Gameplay-Loops.

## 13. Economy / Trading / Party / Guild

Economy- und Item-Werte sind vorhanden und werden weiter in Shop-, Bank- und Reward-Pfade integriert.

Party besitzt API, Creation, Members, Invites, Leave und Events.

Player Trading ist noch nicht vollständig als Gameplay-System abgeschlossen.

Ein vollständiges Guild-System ist derzeit ausdrücklich kein zukünftiges Großfeature. Bestehende tatsächlich verwendete Guild-/Economy-Currency-Pfade bleiben erhalten, bis ihre konkrete Verwendung geprüft wurde.

## 14. GUI / UI

Aktive GUIs werden einzeln nach tatsächlicher Verwendung bewertet. Nicht erreichbare Legacy-GUIs wurden bereits entfernt.

Native Dialoge bleiben der bevorzugte Weg für Interaktionen, während Inventory-GUIs dort verwendet werden, wo sie funktional sinnvoll sind.

## 15. Persistence / Database

YAML und MySQL bleiben die PlayerProfile-Repositories.

- HikariCP wird verwendet.
- MySQL Connector wird verwendet.
- Player-, Companion- und Quest-Daten besitzen Persistenzpfade.
- Historische Produktionsspalten werden nicht destruktiv automatisch gelöscht.
- Persistenz über Server-Neustart ist Bestandteil der Abschlussprüfung.

## 16. API / Commands / Permissions

Vorhandene API-Bereiche umfassen unter anderem:

- PixelRPG Provider
- Economy API
- Guild API
- Item API
- Party API
- Statistics API
- eigene Events für Boss, Klassenwechsel, Guild Join/Leave, Level-Up und Quest Completion

Commands und Permissions werden gegen ihre tatsächliche Registrierung und Runtime-Verwendung geprüft.

## 17. Cleanup-Regeln

Bereits entfernte, nachweislich ungenutzte Bereiche umfassen unter anderem:

- mehrere alte Quest-/Admin-/Travel-/Bank-/Class-Selection-GUIs
- nicht registrierten CraftingCommand
- ungenutzte Class-Set-Service-/Factory-Klassen
- veraltete Companion-Stat-Datei
- alten Equipment-Aura-Config-Key
- das alte Resourcepack-Verzeichnis

Weitere historische Reste werden nur nach tatsächlicher Dependency-Prüfung entfernt.

## 18. Definition of Done

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

Diese Dokumentation beschreibt den **Projektstand und die verbindlichen Designentscheidungen**. Konkrete offene Arbeit wird ausschließlich in `roadmap.md` gepflegt.
