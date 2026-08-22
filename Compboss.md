# PixelRPG – Companions, Bosses & Mounts

## Ziel

Diese Datei ist die zentrale Arbeitsliste für das geplante Boss-, Companion- und Reittier-System. Sie dient als Design- und Implementierungsgrundlage. Fähigkeiten, Werte und konkrete JSON-Strukturen werden später separat ausgearbeitet.

## Grundregeln

- Spielerlevel: 1–99; Level 100 ist Grind-Endgame.
- Companions besitzen eigene Erfahrung und eigene Level.
- Ein Companion erhält nur Erfahrung, solange er aktiv/ausgesendet ist.
- Companions sind grundsätzlich schwächer als Spieler und Bosse.
- Werte skalieren kontrolliert mit dem Companion-Level.
- Seltenheit bestimmt die grundsätzliche Stärke und den benötigten Fortschritt.
- Companion-Namen können bei normalen Companions nach dem später festzulegenden System behandelt werden.
- Unique-Companions werden ausschließlich von Admins vergeben.
- Unique-Companions können feste Namen und feste Skins besitzen und sind nicht umbenennbar.
- Zunächst werden ausschließlich passive Companion-Fähigkeiten umgesetzt; aktive Fähigkeiten folgen erst nach Fertigstellung des Basissystems.
- Boss- und Companion-Werte sollen vollständig konfigurierbar bleiben.
- Quests, Bosse und Companions sollen von externen JSON/YAML-Dateien geladen werden können.
- Boss-Kämpfe dürfen die Welt nicht unnötig zerstören; `block_break: false` ist für entsprechende Fähigkeiten vorgesehen.
- Ein Boss darf nicht doppelt im aktiven Boss-System vorkommen.
- Vanilla-Sonderbosse wie der Ender Dragon werden zunächst nicht in den normalen Boss-Pool aufgenommen.

## Reittiere – erster fester Satz

Diese sieben Reittiere werden als erste Reittier-Ziele übernommen:

1. Pig – Boden-Reittier
2. Bee – Flug-Reittier
3. Skeleton Horse – klassisches Boden-Reittier / Skelettreiter-Boss
4. Strider – Nether-/Lava-Reittier
5. Goat – Charge-Reittier
6. Frog – Sprung-Reittier
7. Turtle – langsames, defensives Reittier

## Boss-Pool

### Overworld

| Entity | Boss | Kernidee | Companion | Mount |
|---|---|---|---|---|
| Chicken | Die Eierkanone | Riesiges Huhn, Eier-Salven | Chicken | – |
| Pig | Der Schweineexpress | Extrem schnelle Charges | Pig | Pig |
| Sheep | Der Wollkoloss | Stampfen und Knockback | Sheep | – |
| Cow | Kuhzilla | Stampfen und Schockwellen | Cow | – |
| Goat | Der Rammbock | Aggressive Charges | Goat | Goat |
| Rabbit | Der Todeshüpfer | Riesensprünge und Landungen | Rabbit | – |
| Bee | Die Bienenkönigin | Fliegender Boss und Luftangriffe | Bee | Bee |
| Fox | Der Taschendieb | Stehlen und Flucht | Fox | – |
| Turtle | Die lebende Festung | Defensive Panzerphase | Turtle | Turtle |
| Panda | Der Wut-Panda | Roll-/Knockback-Angriff | Panda | – |
| Frog | Der Froschkönig | Riesensprung und Schockwelle | Frog | Frog |
| Zombie | Der Untote | Verstärkung bei HP-Schwellen | Zombie | – |
| Husk | Der Wüstenkönig | Wüsten-/Sandmechaniken | Husk | – |
| Skeleton | Der Scharfschütze | Reiter auf Skelettpferd | Skeleton | Skeleton Horse |
| Stray | Der Frostschütze | Frostpfeile und Verlangsamung | Stray | – |
| Bogged | Der Sumpfschütze | Giftpfeile und Giftzonen | Bogged | – |
| Slime | Der Schleimkoloss | Mehrere Split-Phasen | Slime | – |
| Squid | Das Tintenmonster | Tintenwolken und Unsichtbarkeit | Squid | – |

### Nether

| Entity | Boss | Kernidee | Companion | Mount |
|---|---|---|---|---|
| Blaze | Der Feuersturm | Flug und Feuerkugel-Muster | Blaze | – |
| Ghast | Der Himmelsbrecher | Feuerball-Salven | Ghast | – |
| Hoglin | Der Netherbulle | Brutale Charges | Hoglin | – |
| Zoglin | Die rasende Bestie | Permanenter aggressiver Nahkampf | Zoglin | – |
| Piglin Brute | Der Goldene Henker | Schwerer Nahkampf und Kombos | Piglin Brute | – |
| Wither Skeleton | Der Schwarze Ritter | Schwerer Nahkampf | Wither Skeleton | – |
| Magma Cube | Der Magmakern | Split- und Feuerphasen | Magma Cube | – |
| Strider | Der Lavawanderer | Lava-Boss | Strider | Strider |

### The End

| Entity | Boss | Kernidee | Companion | Mount |
|---|---|---|---|---|
| Enderman | Der Leerenspringer | Teleports und Hinterhalte | Enderman | – |
| Shulker | Der Schwerkraftmeister | Levitation und Bewegungsstörung | Shulker | – |
| Endermite | Der Leerenwurm | Überraschungs-/Teleportmechaniken | Endermite | – |
| Ender Dragon | Vanilla-Sonderfall | Bestehender Vanilla-Boss | – | – |

## Companion-Kandidaten und geplante Seltenheiten

| Companion | Seltenheit | Geplante Richtung |
|---|---|---|
| Chicken | Common | gelegentliche Eier |
| Pig | Epic | Reittier-/Charge-Bezug |
| Sheep | Common | defensive passive Unterstützung |
| Cow | Uncommon | kleine Schockwelle |
| Goat | Uncommon | kleiner Charge |
| Rabbit | Uncommon | hoher Sprung |
| Bee | Common | kleiner Bienenangriff |
| Fox | Rare | Utility-/Steal-Mechanik |
| Turtle | Epic | defensive Fähigkeit + Reittier |
| Panda | Rare | Roll-/Knockback-Angriff |
| Frog | Rare | Sprungfähigkeit + Reittier |
| Zombie | Rare | abgeschwächte Unterstützung |
| Husk | Rare | Schwächungs-/Sandmechanik |
| Skeleton | Rare | abgeschwächter Fernkampfangriff |
| Stray | Epic | Frostangriff |
| Bogged | Epic | Giftangriff |
| Slime | Epic | kleine Split-/Bounce-Mechanik |
| Squid | Uncommon | Tintenwolke |
| Blaze | Legendary | Feuerprojektile |
| Ghast | Legendary | kleiner Feuerball |
| Hoglin | Epic | Charge |
| Zoglin | Legendary | aggressiver Charge |
| Piglin Brute | Legendary | Nahkampfunterstützung |
| Wither Skeleton | Legendary | Schwertangriff |
| Magma Cube | Legendary | Feuer-/Bounce-Mechanik |
| Enderman | Legendary | kurzer Teleport |
| Shulker | Legendary | abgeschwächte Levitation |
| Endermite | Rare | kleine Teleport-/Burrow-Mechanik |

## Companion-Seltenheiten

Geplante Kategorien:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Die genaue Stat-Skalierung, XP-Kurve und Stärke jeder Seltenheit wird separat als Balance-System definiert. Ziel ist, dass Companions sinnvoll mit Spielerlevel, Gegnern und Bossen harmonieren, ohne den Spieler zu ersetzen.

## Companion- und Boss-Beziehung

Boss-Fähigkeiten dürfen deutlich spektakulärer sein als die Fähigkeiten des daraus erhaltenen Companions. Ein Boss kann beispielsweise mehrere Eier gleichzeitig verschießen, während der entsprechende Chicken-Companion nur gelegentlich ein Ei erzeugt. Dadurch bleibt der Companion hilfreich, ohne den Bosskampf zu reproduzieren.

Nicht jeder Boss muss einen Companion oder ein Reittier vergeben. Dadurch bleiben besondere Belohnungen selten und wertvoll.

## Boss-Design-Checkliste

Für jeden Boss müssen später festgelegt werden:

- Entity/Variante
- Dimension
- Seltenheit bzw. Boss-Tier
- Spawnchance
- Spawnbedingungen
- Größe/Scale
- Basiswerte
- Lebenspunkte
- Schaden
- Verteidigung
- Bewegung
- Angriffe
- Angriffsmuster
- Phasen
- HP-Schwellen
- Cooldowns
- Reichweiten
- besondere Mechaniken
- Knockback
- Status-Effekte
- `block_break`
- Umgebungseinfluss
- Loot
- Companion-Belohnung
- Reittier-Belohnung
- Teststatus

## Companion-Design-Checkliste

Für jeden Companion müssen später festgelegt werden:

- Entity
- Seltenheit
- Basiswerte
- Level 1–99
- eigene XP
- XP-Kurve
- Skalierung pro Level
- passive Fähigkeiten
- interne Cooldowns
- Reichweite
- Schaden/Unterstützung
- Verhalten
- Follow-Distanz
- Größe/Scale
- Spawn-/Despawn-Verhalten
- benötigte Freischaltung
- mögliche Questbelohnung
- mögliche Bossbelohnung
- JSON/YAML-Konfiguration
- Teststatus

## Reittier-Design-Checkliste

Für jedes Reittier müssen später festgelegt werden:

- Entity
- Bewegungsgeschwindigkeit
- Sprungkraft
- Flug/Lava/Wasser-Verhalten
- Größe/Scale
- Mount-Sitzposition
- Kollisionsverhalten
- besondere Bewegungseigenschaften
- Freischaltung
- Boss-/Quest-/sonstige Quelle
- Teststatus

## Geplante spätere Erweiterungen

- Weitere Mobs aus dem Minecraft-Mob-Spektrum prüfen.
- Auch ungewöhnliche bzw. historisch vorhandene April-Fools-/Test-Entities nur dann berücksichtigen, wenn sie technisch in der Zielplattform verfügbar und sinnvoll verwendbar sind.
- Variantensystem für unterschiedliche Zombie-, Skeleton-, Slime- und andere Mob-Varianten.
- Größenanpassung pro Entity, um große oder bewusst kleinere Companions zu ermöglichen.
- Questbelohnungen können bestimmte Companions freischalten.
- Bossbelohnungen können seltene Companions oder Reittiere enthalten.
- Unique-Companions werden ausschließlich durch Admin-Freischaltung vergeben.
- Aktive Companion-Fähigkeiten erst nach Fertigstellung und Stabilisierung des passiven Basissystems.

## Status

**Designphase:** Basis-Pool definiert.

**Als nächstes:** Bossangriffe, Werte, Belohnungen, Companion-Passivfähigkeiten und Reittierverhalten einzeln spezifizieren und anschließend in die konfigurierbaren Dateien überführen.
