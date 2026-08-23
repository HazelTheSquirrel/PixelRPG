# PixelRPG – Companions, Bosses & Mounts

Stand: 2026-08-23

> Verbindliche Content-Referenz für Companion-Freischaltungen, Bosse und spätere Rewards.

## 100%-Content-Regel

- `test-wolf` ist der **Default-Companion**. Jeder Spieler erhält ihn automatisch.
- Alle anderen normalen Companions müssen über **Quests oder Bosse** freigeschaltet werden.
- `unique-hazel` bleibt **ADMIN-only** und ist nicht über normale Quests/Bosse erhältlich.
- Companion-ID, Rarity und Unlock-Quelle werden datengetrieben in `companions.json` gepflegt.
- Java enthält keine Companion-ID-spezifische Unlock-Sonderlogik.

## Companion Unlock Matrix

| Companion | Rarity | Unlock |
|---|---|---|
| test-wolf | UNCOMMON | DEFAULT |
| test-bee | COMMON | QUEST `bee_nest` |
| common-chicken | COMMON | QUEST `companion_chicken` |
| common-cow | COMMON | QUEST `companion_cow` |
| common-sheep | COMMON | QUEST `companion_sheep` |
| common-rabbit | COMMON | QUEST `companion_rabbit` |
| common-bat | COMMON | QUEST `companion_bat` |
| uncommon-cat | UNCOMMON | QUEST `companion_cat` |
| uncommon-fox | UNCOMMON | QUEST `companion_fox` |
| uncommon-goat | UNCOMMON | QUEST `companion_goat` |
| test-copper-golem | UNCOMMON | QUEST `companion_copper_golem` |
| uncommon-parrot | UNCOMMON | QUEST `companion_parrot` |
| uncommon-armadillo | UNCOMMON | QUEST `companion_armadillo` |
| uncommon-panda | UNCOMMON | QUEST `companion_panda` |
| test-zombie | RARE | QUEST `rare_zombie_companion` |
| test-bogged | RARE | QUEST `companion_bogged` |
| test-parched | RARE | QUEST `companion_parched` |
| rare-skeleton | RARE | BOSS `forest_tyrant` |
| rare-spider | RARE | QUEST `companion_spider` |
| rare-creeper | RARE | QUEST `companion_creeper` |
| test-pig | EPIC | QUEST `epic_pig_companion` |
| epic-horse | EPIC | BOSS `frost_sovereign` |
| epic-camel | EPIC | QUEST `companion_camel` |
| test-nautilus | EPIC | QUEST `companion_nautilus` |
| test-creaking | EPIC | QUEST `companion_creaking` |
| test-happy-ghast | EPIC | QUEST `companion_happy_ghast` |
| legendary-iron-golem | LEGENDARY | QUEST `companion_iron_golem` |
| test-warden | LEGENDARY | QUEST `legendary_warden_companion` |
| legendary-ravager | LEGENDARY | BOSS `void_reaper` |
| test-sulfur-cube | LEGENDARY | QUEST `companion_sulfur_cube` |
| unique-hazel | UNIQUE | ADMIN ONLY |

## Boss Rewards

Die vorhandenen World-Boss-Definitionen werden als Companion-Reward-Quellen verwendet:

- `forest_tyrant` → `rare-skeleton`
- `frost_sovereign` → `epic-horse`
- `void_reaper` → `legendary-ravager`

Die Zuordnung steht in der jeweiligen Companion-Definition unter `unlock.bossId`. Der Boss selbst muss keine Companion-spezifische Java-Logik kennen.

## Quest-Rewards

Die Companion-Questinhalte sind direkt in `src/main/resources/data/quests.json` integriert. Damit bleibt der bestehende Quest-Loader die einzige Quelle für Questdefinitionen.

Die Questbelohnungen decken alle nicht durch Bosse freigeschalteten normalen Companions ab.

## Companion-Raritäten

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Unique-Companions bleiben Admin-vergeben und können feste Namen/Skins besitzen.

## Companion-Design

Für jeden Companion werden langfristig definiert:

- Entity / Visual
- Seltenheit
- Basiswerte
- Level 1–99
- eigene XP
- XP-Kurve
- Skalierung
- passive Fähigkeiten
- Cooldowns
- Reichweite
- Verhalten
- Follow-Distanz
- Größe/Scale
- Spawn-/Despawn-Verhalten
- Freischaltung
- Quest-/Bossquelle
- Datenkonfiguration
- Teststatus

## Boss-Content

Das bestehende Boss-System besitzt Definitionen, aktive Bosse, Phasen, Loot, Contribution, Angriffsmuster und Spawnlogik.

Für jeden Boss werden bei der Content-Abnahme geprüft:

- Entity/Variante
- Dimension
- Tier
- Spawnbedingungen
- Größe/Scale
- Werte
- Bewegung
- Angriffe
- Phasen
- HP-Schwellen
- Cooldowns
- Reichweiten
- Status-Effekte
- Umgebungseinfluss
- Loot
- Companion-Belohnung
- Teststatus

Vanilla-Sonderbosse wie Ender Dragon bleiben vom normalen Boss-Pool getrennt.

## Mount-Pool

Spätere Designziele:

1. Pig – Boden
2. Bee – Flug
3. Skeleton Horse – Boden
4. Strider – Nether/Lava
5. Goat – Charge
6. Frog – Sprung
7. Turtle – langsam/defensiv

Das Mount-System ist bewusst noch nicht Teil des Companion-Unlock-100%-Blocks.

## Fortschrittsstatus

### 🟢 Abschnitt 01 – Unlock-Grundregeln

FERTIG

- Wolf automatisch
- alle anderen normalen Companions locked
- Hazel Admin-only
- datengetriebene Unlock-Definitionen

### 🟢 Abschnitt 02 – Quest-Companion-Content

FERTIG

- alle normalen Quest-Companions besitzen eine Questquelle
- Quest-Rewards sind in `quests.json` hinterlegt
- Wolf-Quest ist keine Freischaltquelle mehr

### 🟢 Abschnitt 03 – Boss-Companion-Content

FERTIG

- Forest Tyrant → Skeleton
- Frost Sovereign → Horse
- Void Reaper → Ravager
- generischer BossDefeated → Companion-Unlock

### 🟡 Abschnitt 04 – Ingame-Abnahme

OFFEN

- Questannahme und Abschluss für jeden Companion testen
- Bosskill und Reward testen
- Relog/Restart testen
- mehrere Spieler bei Bosskills testen
- bereits besessene Rewards testen
- Companion-Dialog und Auswahl testen

### 🟡 Abschnitt 05 – Spätere Companion-Fähigkeiten

OFFEN

- passive Fähigkeiten
- aktive Fähigkeiten
- Cooldowns
- Ressourcen/Mana
- Support/Utility
- besondere Unique-Mechaniken

## Definition of Done

Der Content-Block gilt erst als vollständig abgeschlossen, wenn:

- Build grün ist
- alle Unlock-IDs validiert werden
- Wolf bei neuen Spielern vorhanden ist
- kein normaler Companion ohne Quest/Boss-Quelle existiert
- Hazel nicht normal freischaltbar ist
- Quest-Rewards funktionieren
- Boss-Rewards funktionieren
- Relog/Restart die Besitzdaten erhält
- mehrere Spieler gleichzeitig korrekt belohnt werden
- keine Companion-ID-spezifische Unlock-Logik in Java erforderlich ist
