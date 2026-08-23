# PixelRPG – Companions, Bosses & Mounts

Stand: 2026-08-23

> Verbindliche Content-Referenz für Companion-Freischaltungen, Bosse und Mounts.

## Companion-Grundregeln

- `test-wolf` ist der **Default-Companion**. Jeder Spieler erhält ihn automatisch.
- Normale Companions sind **passiv, unbesiegbar und nicht kampffähig**.
- Normale Companions verwenden keine eigene Kampf-/Schadenslogik und keine EXP-/Damage-Kurve als Gameplay-System.
- Passive Companions liefern stattdessen Utility und passive RPG-Boni.
- Hostile-Mob-Companions werden nicht verwendet.
- `unique-hazel` bleibt **ADMIN-only** und ist die einzige normale Ausnahme: Unique-Mannequin-Companions dürfen eigene Kampf- und Skin-Mechaniken besitzen.
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
| test-pig | EPIC | QUEST `epic_pig_companion` |
| epic-horse | EPIC | BOSS `frost_sovereign` |
| epic-zombie-horse | EPIC | BOSS `void_reaper` |
| epic-skeleton-horse | EPIC | BOSS `forest_tyrant` |
| epic-camel | EPIC | QUEST `companion_camel` |
| test-nautilus | EPIC | QUEST `companion_nautilus` |
| test-creaking | EPIC | QUEST `companion_creaking` |
| test-happy-ghast | EPIC | QUEST `companion_happy_ghast` |
| legendary-iron-golem | LEGENDARY | QUEST `companion_iron_golem` |
| unique-hazel | UNIQUE | ADMIN ONLY |

## Passive Companion Bonuses

Nicht-kämpfende Companions können über die vorhandene RPG-Stat-Pipeline passive Boni liefern, insbesondere:

- Leben
- Rüstung
- Lifesteal
- Crit-Chance
- Crit-Schaden
- weitere vorhandene RPG-Stats, sofern für den jeweiligen Companion konfiguriert

## Mount-System

Mounts sind normale passive Companions mit einer datengetriebenen `mount`-Definition.

Aktuell vorgesehen/implementiert:

1. **Pig** – Boden-Mount, automatischer Sattel.
2. **Horse** – Boden-Mount, Sattel.
3. **Zombie Horse** – Boden-Mount, Sattel.
4. **Skeleton Horse** – Boden-Mount, Sattel.
5. **Nautilus** – Unterwasser-Mount, Sattel.
6. **Bee** – Spezial-Flugmount ohne Sattel.

### Bee-Spezial

- Scale: `1.60`, bewusst deutlich größer als eine normale Biene und ungefähr in der Größenordnung eines Schweins.
- Fluggeschwindigkeit: `1.05` Blöcke/Tick als Zielwert, ungefähr 75 % des gewünschten Elytra-Gefühls.
- Bewegung wird über Paper 26.x `Player#getCurrentInput()` gesteuert.
- `Jump` steigt, `Sneak` sinkt.
- Keine Vanilla-Bienen-Kampfmechanik.
- Die Biene ist unbesiegbar und passiv.

### Mount-Steuerung

- Rechtsklick durch den Besitzer mountet den aktiven Mount.
- Fremde Spieler können den Companion nicht mounten.
- Boden-Mounts nutzen Vorwärts/Rückwärts/Links/Rechts und Sprung.
- Unterwasser-/Flug-Mounts verwenden 3D-Bewegung.
- Sättel werden bei Mount-Definitionen automatisch gesetzt.
- Die Mount-Logik verwendet ausschließlich die aktuelle Paper-26.x-API.

## Unique-Mannequin

`unique-hazel` bleibt die einzige Companion-Ausnahme:

- `MANNEQUIN`
- `UNIQUE`
- `ADMIN-only`
- eigener Combat-Controller
- eigener Skin
- Skin über den bestehenden `MannequinSkinResolver`
- Spielername oder direkte Skin-URL möglich

## Companion-Raritäten

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Unique-Companions bleiben Admin-vergeben und können feste Namen/Skins besitzen.

## Definition of Done

Der Companion-/Mount-Block gilt erst als vollständig abgeschlossen, wenn:

- Build grün ist
- alle Unlock-IDs validiert werden
- Wolf bei neuen Spielern vorhanden ist
- kein normaler Companion ohne Quest/Boss-Quelle existiert
- keine Hostile-Mob-Definition mehr als nutzbarer Companion existiert
- passive Companions unbesiegbar sind
- passive Companions keine eigene Kampf-/XP-Kurve verwenden
- passive Stats korrekt auf den Spieler wirken
- Pig, Horse, Zombie Horse, Skeleton Horse und Nautilus reitbar sind
- Sättel korrekt gesetzt werden
- Nautilus unter Wasser korrekt steuerbar ist
- Bee als vergrößertes Flugmount korrekt steuerbar ist
- Bee ungefähr den vorgesehenen Geschwindigkeitswert erreicht
- Unique-Mannequin weiterhin separat funktioniert
- Relog/Restart aktive Mounts korrekt wiederherstellt
- mehrere Spieler gleichzeitig korrekt funktionieren
