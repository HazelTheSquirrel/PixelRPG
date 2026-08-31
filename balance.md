# PixelRPG — Forensische Balance- und Progressionsspezifikation

**Branch:** `test`  
**Audit-Basis:** `ede67647a1ef9533b673c2ab23cd7a0f13128f28`  
**Zweck:** Forensische Bestandsaufnahme des aktuellen Equipment-/Combat-/Mob-Scaling-Systems und verbindliche Grundlage für das künftige Level-1–99-Balancing.  
**Status:** Design-/Balancing-Dokumentation. Dieses Dokument ändert noch keinen Runtime-Code.

---

## 1. Ziel des Systems

PixelRPG besitzt **keine Klassen**. Die Charakterentwicklung entsteht ausschließlich aus Level, Waffen, Rüstung, Sets und den daraus resultierenden Stats.

Das System muss deshalb drei Dinge gleichzeitig erfüllen:

1. Ein Spieler muss seinen Fortschritt **spürbar** merken.
2. Equipment muss sinnvoll skalieren, ohne dass 99 Varianten desselben Items gepflegt werden müssen.
3. Monster müssen auf die tatsächliche Spieler-/Gear-Power reagieren, ohne den Fortschritt vollständig zu neutralisieren.

Die zentrale Designregel lautet:

> **Level schaltet Progression frei. Equipment erzeugt die PixelRPG-Kampfstärke.**

Ein Spieler ohne PixelRPG-Waffe/Rüstung/Set besitzt **keine PixelRPG-Bonusstats** und verhält sich bezüglich des PixelRPG-Stat-Systems wie ein Vanilla-Spieler.

Vanilla-Basiswerte bleiben dabei selbstverständlich erhalten.

---

## 2. Verbindliche Spielerbasis

Ohne aktive PixelRPG-Ausrüstung:

| Stat | PixelRPG-Bonus |
|---|---:|
| HP-Bonus | `0.0 LP` |
| Rüstung-Bonus | `0.0` |
| Bewegungstempo-Bonus | `0.0 %` |
| Reichweiten-Bonus | `0.0 Blöcke` |
| Kritische Trefferchance | `0.0 %` |
| Kritischer Schaden | `0.0 % Bonus` |
| Lebensraub | `0.0 %` |
| Angriffskraft | `0.0` |

Das bedeutet nicht, dass der Spieler keine Vanilla-Werte besitzt. Beispielsweise bleiben die Vanilla-20 HP bestehen. PixelRPG zeigt nur **keinen zusätzlichen PixelRPG-Bonus** an.

---

## 3. Endgame-Caps

Diese Werte bilden die aktuelle Designobergrenze für einen normalen Level-99-Endgame-Build.

| Stat | Ziel-Cap | Status |
|---|---:|---|
| HP | **200 LP** | verbindlich |
| Bewegungstempo | **+30.0 %** | verbindlich |
| Reichweite | **5.0 Blöcke** | verbindlich |
| Kritische Trefferchance | **100.0 %** | verbindlich |
| Kritischer Schaden | **+100.0 % Bonus** | verbindliches Ziel |
| Rüstung | **200.0** | vorläufiges mathematisches Ziel |
| Lebensraub | **8.0 %** | vorläufiges Balance-Ziel |
| Angriffskraft | **60.0** | vorläufiges Endgame-Ziel; über TTK/DTK zu validieren |

### 3.1 HP

`10 LP = 1 Herz`.

`200 LP = 20 Herzen` und damit zwei volle Vanilla-Lebensleisten bei der vorgesehenen Skalierung.

Die 200 LP sind ein **Hard Cap** für den normalen PixelRPG-Charakter. Weitere Systeme wie temporäre Effekte, Bossmechaniken oder zukünftige Transzendenz dürfen diesen Wert nicht stillschweigend überschreiben.

### 3.2 Bewegungstempo

Der PixelRPG-Bonus wird als prozentualer Bonus auf das Vanilla-Attribut modelliert.

Vanilla-Spielerbasis: `minecraft:movement_speed = 0.10`.

`+30.0 %` bedeutet daher eine Zielgröße von ungefähr `0.13` auf der Vanilla-Attributbasis.

`0.15` entspricht dagegen ungefähr `+50 %` gegenüber `0.10` und ist **nicht** das PixelRPG-Zielcap.

Die Paper-26.2-API führt `minecraft:movement_speed`, `minecraft:max_health`, `minecraft:armor`, `minecraft:block_interaction_range` und `minecraft:entity_interaction_range` als aktuelle Vanilla-Attribute. Die API dokumentiert diese Attribute als direkte Repräsentation der Vanilla-Spielattribute.  
Quelle: Paper 26.2 API — `AttributeKeys`.

### 3.3 Reichweite

Block- und Entity-Reichweite sind technisch getrennte Vanilla-Attribute. PixelRPG besitzt aktuell einen gemeinsamen Equipment-Stat und schreibt ihn auf beide Attribute.

Das Balance-Ziel ist:

`0.0 → 5.0 Blöcke PixelRPG-Bonus/Reichweite`.

Die Implementierung muss später eindeutig festlegen, ob `5.0` die **gesamte Reichweite** oder der **PixelRPG-Bonus** ist. Für das Gameplay-Design wird in diesem Dokument `5.0 Blöcke Gesamtziel` verwendet.

---

## 4. Aktueller Forensikbefund — Item-Pipeline

### 4.1 Aktuelle Architektur

Der Branch besitzt bereits eine zentrale Item-Pipeline:

`ItemDefinitionRegistry → ItemService → RPGItemBuilder → PDC → StatEngine`.

`RPGItemBuilder` erzeugt Item-Level, Required-Level, Rarität, Item-ID, Instance-ID und die Item-Stats. Die Statwerte werden aktuell über `item-scaling.json`, Raritätsmultiplikator und einen zufälligen Roll-Faktor erzeugt.

Aktuelle Item-Skalierung:

```text
levelProgress = (itemLevel - 1) / 98
levelFactor   = growthMultiplier ^ levelProgress
```

Aktuell ist `growthMultiplier = 10.0`.

Zusätzlich wird aktuell ein zufälliger Faktor zwischen `0.50` und `1.50` angewendet.

Quelle: `RPGItemBuilder.java`, `item-scaling.json`.

### 4.2 Aktuelle Basiswerte

Der Branch enthält aktuell:

```text
weaponDamage       3.0
weaponCritChance   0.5
weaponCritDamage   0.05
weaponReach        0.10
weaponLifesteal    0.25
armor              0.7
health             1.2
movementSpeed      0.001
toolEfficiency     1.0
growthMultiplier  10.0
```

Diese Werte sind **keine finale Balance**. Insbesondere die Kombination aus exponentieller Levelkurve, Raritätsmultiplikator und 0.50–1.50-Roll macht die tatsächlichen Endwerte derzeit zu aggressiv bzw. uneinheitlich für die gewünschten Caps.

Quelle: `src/main/resources/data/item-scaling.json`.

---

## 5. Aktuelle Raritäten

Der Branch besitzt:

```text
COMMON
UNCOMMON
RARE
EPIC
LEGENDARY
UNIQUE
```

Aktuelle Multiplikatoren:

```text
Common      1.00
Uncommon    1.10
Rare        1.22
Epic        1.38
Legendary   1.60
Unique      1.60
```

Aktuelle zufällige Statanzahl:

```text
Common      2
Uncommon    3
Rare        4
Epic        5
Legendary   8
Unique      8
```

**Forensischer Befund:** Der aktuelle Stat-Pool enthält ebenfalls genau acht normale Equipment-Stats. Ein Legendary mit acht Rolls erhält damit aktuell zwangsläufig alle acht Stattypen. Das ist für ein klassenloses RPG langfristig zu wenig Build-Diversität.

### Designentscheidung

`8` bleibt die **maximale Anzahl sichtbarer Stat-Linien** eines normalen Legendary-Items.

Es darf aber nicht bedeuten, dass jedes Legendary zwangsläufig alle acht Kernstats besitzt. Die spätere Implementierung soll zwischen **Stat-Budget** und **Stat-Anzahl** unterscheiden.

---

## 6. Zentrales Balance-Modell

Die bisherige Idee „Level 1–99 braucht 99 Itemdefinitionen“ wird ausdrücklich verworfen.

Ein Item wird weiterhin parametrisch erzeugt:

```text
ItemDefinition
+ Item-Level
+ Rarität
+ Stat-Rolls
        ↓
Stat-Budget
        ↓
Finale Itemwerte
```

Ein Schwert kann deshalb Level 12, 47 oder 99 sein, ohne dass dafür jeweils eine eigene Definition erforderlich ist.

### 6.1 Neue Zielstruktur

```text
Item-Level
    ↓
Level Power Curve
    ↓
Rarity Budget
    ↓
Slot Budget
    ↓
Stat Budget
    ↓
Roll Quality
    ↓
Item Stat
```

Der entscheidende Unterschied zum aktuellen System ist:

> **Nicht jeder Stat wird unabhängig bis zum maximalen Einzelwert hochmultipliziert. Das Item besitzt ein begrenztes Gesamtbudget.**

Dadurch kann ein Legendary acht Stats besitzen, ohne automatisch acht Endgame-Caps gleichzeitig zu erreichen.

---

## 7. Empfohlene Levelkurve 1–99

Die erste Balance-Version verwendet eine weiche, spät stärker werdende Kurve:

```text
progress = (itemLevel - 1) / 98
power    = 0.05 + 0.95 * progress^1.35
```

Damit erhält Level 1 einen kleinen, aber spürbaren Einstieg und Level 99 erreicht 100 % der vorgesehenen Item-Level-Power.

### Referenzwerte

| Item-Level | Power-Faktor |
|---:|---:|
| 1 | 5.0 % |
| 10 | 8.8 % |
| 20 | 15.4 % |
| 30 | 23.4 % |
| 40 | 32.4 % |
| 50 | 42.3 % |
| 60 | 52.9 % |
| 70 | 64.2 % |
| 80 | 76.0 % |
| 90 | 88.4 % |
| 99 | 100.0 % |

Diese Kurve ist bewusst **nicht linear**. Die ersten Level sollen nicht durch minimale Rundungswerte wie `+0.1 %` entwertet werden, während die höheren Level deutlich mehr spürbare Progression liefern.

Die Kurve ist ein Startmodell und wird durch Simulation gegen komplette Builds und Monster-TTK/DTK validiert.

---

## 8. Stat-Definitionen

### HP

PixelRPG-HP ist ein absoluter Wert.

```text
Vanilla: 20 LP
PixelRPG: +0 bis +180 LP
Final: 20 bis 200 LP
```

Die Lore zeigt ausschließlich den PixelRPG-Bonus, während das Profil zusätzlich den aktuellen/maximalen Gesamtwert anzeigen darf.

Beispiel:

```text
Lore:     +18.0 LP
Profil:   138.0 / 200 LP
```

### Rüstung

PixelRPG-Rüstung ist ein zusätzlicher Armor-Wert auf dem Vanilla-Attribut.

Die aktuelle eigene Damage-Mitigation lautet:

```text
damageAfterArmor = damage * (100 / (100 + armor))
```

Bei dieser Formel ergeben sich ungefähr:

| Armor | Schadensreduktion |
|---:|---:|
| 25 | 20.0 % |
| 50 | 33.3 % |
| 100 | 50.0 % |
| 150 | 60.0 % |
| 200 | 66.7 % |

**Vorläufiges Cap: 200 Armor.**

Das muss gegen echte Vanilla-Rüstung und alle weiteren Schadensquellen auf einem Testserver validiert werden.

### Bewegungstempo

PixelRPG speichert den Bonus logisch als Prozentwert.

```text
0.0 % → Vanilla
10.0 % → Vanilla-Basis × 1.10
20.0 % → Vanilla-Basis × 1.20
30.0 % → Vanilla-Basis × 1.30
```

Hard Cap: `30.0 %`.

### Reichweite

Zielwert: `5.0 Blöcke Gesamt-Reichweite`.

Die interne Darstellung muss zwischen Vanilla-Basis und PixelRPG-Bonus unterscheiden.

### Kritische Trefferchance

Crit Chance ist ein Prozentwert von `0.0–100.0 %`.

Hard Cap: `100.0 %`.

100 % darf ein echter Best-in-Slot-Build erreichen.

### Kritischer Schaden

Der aktuelle Combat-Code behandelt `2.0` als Basis-Kritmultiplikator und addiert den PixelRPG-Wert darauf.

Daher soll das System künftig klar definieren:

```text
Vanilla/PX Basis-Crit-Multiplikator: 2.0x
PixelRPG Crit-Damage-Bonus: 0.0–100.0 %
```

Beispiele:

```text
0.0 % Bonus  → 2.0x
25.0 % Bonus → 2.25x
50.0 % Bonus → 2.50x
100.0 % Bonus → 3.00x
```

**Wichtig:** Die Lore darf nicht einfach `2.50x` als `250 % Kritischer Schaden` ausgeben, wenn der Spieler eigentlich `+50.0 % Bonus` besitzt. Bonus und Gesamtmultiplikator müssen getrennt dargestellt werden.

### Lebensraub

Der aktuelle Code heilt:

```text
heal = dealtDamage * lifestealPercent / 100
```

Da maximal 200 LP vorgesehen sind, ist Lifesteal besonders gefährlich für das Balance-System.

**Startwert für die erste Balance-Version: 8.0 % Hard Cap.**

Das ist bewusst niedriger als Crit und Crit Damage.

Lebensraub muss außerdem anhand von realem Schaden, Angriffstempo und maximaler HP getestet werden. Ein hoher Lifesteal-Wert darf keinen Build erzeugen, der bei jedem Treffer praktisch vollständig geheilt wird.

### Angriffskraft

Angriffskraft ist ein **absoluter flacher Schadensbonus**, kein Prozentwert.

Aktuell:

```text
rawDamage = vanillaDamage + attackPower
```

Crit wird anschließend angewendet.

Das vorläufige Endgame-Ziel lautet:

```text
~60 PixelRPG Attack Power
```

Dieses Ziel ist **kein blindes Hard Cap**. Es wird anhand der tatsächlichen Monster-HP, Vanilla-Waffenschäden, Rüstung und gewünschten Trefferzahl validiert.

---

## 9. Endgame-Referenzkurve

Die folgenden Werte sind die **Zielgröße für einen sehr starken, vollständig ausgerüsteten Level-99-Build**, nicht die Werte eines nackten Level-99-Spielers.

| Level | HP gesamt | Armor | Move | Reach | Crit | Crit Dmg Bonus | Lifesteal | Attack Power |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | ~29 | ~10 | ~1.5 % | ~0.25 | ~5 % | ~5 % | ~0.4 % | ~3 |
| 10 | ~36 | ~18 | ~2.6 % | ~0.44 | ~9 % | ~9 % | ~0.7 % | ~5 |
| 20 | ~48 | ~31 | ~4.6 % | ~0.77 | ~15 % | ~15 % | ~1.2 % | ~9 |
| 30 | ~62 | ~47 | ~7.0 % | ~1.17 | ~23 % | ~23 % | ~1.9 % | ~14 |
| 40 | ~78 | ~65 | ~9.7 % | ~1.62 | ~32 % | ~32 % | ~2.6 % | ~19 |
| 50 | ~96 | ~85 | ~12.7 % | ~2.12 | ~42 % | ~42 % | ~3.4 % | ~25 |
| 60 | ~115 | ~106 | ~15.9 % | ~2.65 | ~53 % | ~53 % | ~4.2 % | ~32 |
| 70 | ~136 | ~128 | ~19.3 % | ~3.21 | ~64 % | ~64 % | ~5.1 % | ~39 |
| 80 | ~157 | ~152 | ~22.8 % | ~3.80 | ~76 % | ~76 % | ~6.1 % | ~46 |
| 90 | ~179 | ~177 | ~26.5 % | ~4.42 | ~88 % | ~88 % | ~7.1 % | ~53 |
| 99 | **200** | **200** | **30.0 %** | **5.0** | **100 %** | **100 %** | **8.0 %** | **60** |

**Interpretation:** Das sind keine automatischen Level-Boni. Sie bilden eine Zielhülle dafür, was ein außergewöhnlich gut ausgerüsteter Build ungefähr erreichen können soll.

Ein normaler Spieler auf demselben Level muss darunter liegen.

---

## 10. Equipment-Builds statt Klassen

Da PixelRPG keine Klassen besitzt, entstehen Archetypen aus Stat-Verteilungen.

Beispiele:

### Berserker

```text
Attack Power
Crit Chance
Crit Damage
```

### Juggernaut

```text
HP
Armor
```

### Vampire

```text
Attack Power
Crit
Lifesteal
```

### Ranger

```text
Reach
Crit
Crit Damage
```

### Swift

```text
Movement Speed
Reach
Crit
```

Diese Bezeichnungen sind **keine Klassen**. Sie sind nur Beschreibungen von Builds.

Das ist wichtig: Der Spieler darf seine Rolle durch sein Gear selbst erzeugen.

---

## 11. Stat-Budget-System

Das zukünftige Item-System soll nicht mehr jeden Stat unabhängig maximal skalieren.

Ein Item erhält ein Gesamtbudget:

```text
Item-Level
× Rarity Budget
× Slot Budget
= Item Stat Budget
```

Jeder Stat-Roll verbraucht einen Teil dieses Budgets.

Dadurch gilt:

> Ein Item mit acht Stats ist vielseitig, aber nicht automatisch achtmal so stark.

### Raritätsrichtung

Die aktuelle Raritätsordnung bleibt erhalten:

```text
Common
Uncommon
Rare
Epic
Legendary
Unique
```

Die Rarität darf sowohl die Anzahl der möglichen Stats als auch das Gesamtbudget beeinflussen.

Die aktuellen Multiplikatoren `1.00 / 1.10 / 1.22 / 1.38 / 1.60` sind nur der historische Ist-Stand und werden durch die neue Budgetsimulation ersetzt bzw. neu kalibriert.

### Stat-Anzahl

Aktuelle Zielrichtung:

```text
Common      2
Uncommon    3
Rare        4
Epic        5
Legendary   bis zu 8
```

„Bis zu 8“ ist absichtlich gewählt. Ein Legendary soll nicht allein durch die Anzahl der Zeilen automatisch alle Endgame-Caps erreichen.

---

## 12. Best-in-Slot und Werte über 100 %

Nicht jeder Wert muss dieselbe Semantik besitzen.

### Harte Gameplay-Caps

Diese dürfen nicht überschritten werden:

```text
HP               200 LP
Movement Speed     30.0 %
Reach               5.0 Blöcke
Crit Chance       100.0 %
```

### Werte mit möglichem Overcap

Bei bestimmten Stats kann ein zukünftiges internes Overcap erlaubt werden, wenn es gameplayseitig sinnvoll ist.

Beispielsweise könnte ein Item einen besonders guten Roll besitzen, der rechnerisch über einem Soft Target liegt.

Die Engine muss trotzdem zwischen:

```text
Raw Roll
Final Effective Value
```

unterscheiden.

Ein Best-in-Slot-Item darf also außergewöhnlich gut sein, ohne dass dadurch ein harter Gameplay-Cap wie Bewegungstempo oder Reichweite gebrochen wird.

---

## 13. Sets

Sets sind Teil der Equipment-Power und keine zweite Charakterklasse.

Der aktuelle `EquipmentSetService` zählt nur verwendbare Set-Teile, also Teile, deren Required-Level erfüllt ist. Die Set-Boni werden anschließend in die `StatEngine` integriert.

Das Grundprinzip bleibt:

```text
Item Stats
+
Set Bonuses
=
Equipment Power
```

Set-Boni sollen einen Build spezialisieren, nicht die normalen Item-Caps umgehen.

Für die erste Balance-Version wird empfohlen, Set-Boni aus einem **separaten, kleinen Bonusbudget** zu finanzieren. Sie sollen einen Build abrunden und nicht alleine den größten Teil eines Stats erzeugen.

---

## 14. Level-Gating

Das gewünschte Verhalten bleibt verbindlich:

```text
Item kann angelegt werden
        ↓
Required Level nicht erreicht?
        ↓
Item bleibt im Slot
        ↓
PixelRPG-Stats werden nicht aktiviert
        ↓
Set-Teil zählt nicht
        ↓
Weapon Ability darf nicht aktiviert werden
```

Das ist bereits teilweise umgesetzt: `StatEngine` und `EquipmentSetService` prüfen das Required-Level.

Der Weapon-Ability-Pfad muss zusätzlich zentral gegen dieselbe Nutzbarkeitsprüfung abgesichert werden.

---

## 15. Forensischer Befund — Monster Scaling

Der aktuelle Branch besitzt eine echte Mob-Level-Skalierung.

Aktuelle Basisformel:

```text
Mob HP     = 20 + playerLevel * hpPerLevel
Mob Damage =  2 + playerLevel * damagePerLevel
```

Aktuelle JSON-Werte:

```text
hpPerLevel       = 5.0
damagePerLevel   = 0.30
playerParity     = 1.00
gearMultiplier   = 0.75–1.25
```

Dadurch entstehen beispielsweise:

| Spielerlevel | Mob HP | Mob Damage |
|---:|---:|---:|
| 1 | 25 | 2.3 |
| 10 | 70 | 5.0 |
| 20 | 120 | 8.0 |
| 50 | 270 | 17.0 |
| 80 | 420 | 26.0 |
| 99 | 515 | 31.7 |

Die Werte werden aktuell durch den Gear-Multiplier beeinflusst.

### Kritischer Forensikbefund

Der aktuelle Gear-Multiplier basiert **nicht auf der tatsächlichen finalen Stat-Power** des Spielers. Er verwendet den Durchschnitt der Item-Level der getragenen Items und vergleicht diesen mit dem Spielerlevel.

Damit gilt derzeit vereinfacht:

```text
Mob Scaling
← Spielerlevel
← durchschnittliches Item-Level
```

und nicht:

```text
Mob Scaling
← tatsächliche Spieler-Power
```

Das ist für das endgültige Balance-System nicht ausreichend.

Ein Level-99-Spieler mit schlechtem Gear und ein Level-99-Spieler mit perfektem Best-in-Slot-Gear können derzeit trotz stark unterschiedlicher tatsächlicher Stats ähnlich behandelt werden.

### Zielzustand

Die Monster-Skalierung soll langfristig einen **Power Index** verwenden:

```text
Player Power Index
    ↓
Mob Scaling
```

Der Index basiert auf den tatsächlich aktivierten Equipment-Stats, nicht nur auf Item-Level.

---

## 16. Monster müssen Fortschritt zulassen

Monster dürfen nicht exakt im gleichen Verhältnis wachsen wie der Spieler.

Schlechtes Design wäre:

```text
Spieler +50 % Power
Monster +50 % Power
```

Dann fühlt sich der Fortschritt nicht existent an.

Gewünscht ist:

```text
Spieler wächst deutlich
Monster wächst kontrolliert
Spieler gewinnt netto an Effizienz
```

Der wichtigste Balance-Messwert wird deshalb:

```text
TTK = Time To Kill
DTK = Time To Die
```

Ein Spieler soll mit besserem Gear:

- Monster schneller töten.
- mehr Fehler verzeihen können.
- stärkere Gebiete erreichen.
- neue Builds ausprobieren können.

Monster Scaling soll diese Vorteile dämpfen, aber nicht eliminieren.

---

## 17. Mob Scaling — Zielarchitektur

Langfristig:

```text
Player Level
      +
Equipment Power Index
      +
Region / Mob Tier
      ↓
Target Mob Level
      ↓
Mob Base HP / Damage
      ↓
Controlled Player-Parity Multiplier
```

Dabei muss das System weiterhin berücksichtigen:

- Spielerlevel
- Region
- Mob-Tier
- Gruppen-/Teilnehmer-Situation
- Gear-Power

Der aktuelle höchste Teilnehmerlevel darf nicht einfach als alleinige Wahrheit für alle Werte verwendet werden, wenn unterschiedliche Spieler mit stark unterschiedlichem Gear beteiligt sind.

---

## 18. Combat-Balance

Der aktuelle Player-Damage-Pfad lautet vereinfacht:

```text
Vanilla Weapon Damage
+
PixelRPG Attack Power
=
Raw Damage
```

Dann:

```text
Crit?
    ↓
Raw × Crit Multiplier
```

Dann:

```text
Armor Mitigation
    ↓
Final Damage
```

Lifesteal basiert anschließend auf dem tatsächlich verursachten Final Damage.

Das ist als Grundarchitektur brauchbar.

### Zielwerte für Tests

Ein normaler Level-99-Endgame-Build soll nicht jeden normalen Level-99-Mob in einem Treffer töten.

Als erste Testziele:

```text
Normaler Mob:
~8–12 normale Treffer
~3–6 kritische Treffer, abhängig vom Build
```

Ein reiner Crit-Build darf schneller töten, bezahlt dafür aber mit weniger defensiven Stats.

Ein Tank-Build soll deutlich länger überleben, aber weniger Schaden verursachen.

Ein Lifesteal-Build soll zwischen diesen Extremen liegen.

Diese Zahlen sind **Testziele**, keine unveränderlichen Gameplay-Garantien.

---

## 19. Armor-Balance

Die aktuelle eigene Formel ist:

```text
mitigation = 100 / (100 + armor)
```

Das ist für ein RPG gut geeignet, weil jeder weitere Armor-Punkt immer weniger zusätzliche Reduktion liefert.

Beispiel:

```text
100 Armor → 50.0 % weniger Schaden
200 Armor → 66.7 % weniger Schaden
300 Armor → 75.0 % weniger Schaden
```

Deshalb ist `200 Armor` ein sinnvoller erster Test-Cap.

Das System darf nicht gleichzeitig eine unkontrollierte zweite Armor-Mitigation in der Vanilla-Schadensberechnung erzeugen. Die bestehende Combat-Pipeline versucht bereits, den gewünschten Custom-Schaden über die Vanilla-Mitigation zurückzurechnen; dies muss bei der Implementierung exakt erhalten bzw. gegen Paper 26.2 getestet werden.

---

## 20. Lebensraub-Balance

Lebensraub wird nicht anhand eines beliebigen Prozentwerts entschieden.

Er wird anhand folgender Kombination getestet:

```text
Attack Power
× Crit
× Attack Speed / Trefferfrequenz
× Lifesteal
× Max HP
```

Beispiel bei 200 LP:

```text
50 Schaden × 8 % = 4 LP Heilung pro Treffer
100 Schaden × 8 % = 8 LP Heilung pro Treffer
```

100 Schaden pro Treffer bei 8 % Lifesteal würde bereits 8 LP pro Treffer zurückgeben. Deshalb ist Lifesteal absichtlich deutlich niedriger zu behandeln als Crit Chance.

Vorläufiges Cap: **8.0 %**.

---

## 21. Lore und Profil — eine einzige Wahrheit

Das System darf niemals drei verschiedene Stat-Berechnungen besitzen.

Ziel:

```text
Equipment
   ↓
StatEngine
   ↓
Final CachedStats
   ├── Gameplay
   ├── Item Lore
   ├── Profil
   └── Debug/API
```

### Lore

Jeder Wert muss mit der richtigen Einheit und konsistenten Nachkommastellen erscheinen.

Beispiele:

```text
+12.0 LP
+24.5 Rüstung
+7.5 % Bewegungstempo
+1.25 Reichweite
+18.0 % Kritische Trefferchance
+35.0 % Kritischer Schaden
+3.0 % Lebensraub
+27.0 Angriffskraft
```

`0` darf bei Dezimalstats nicht plötzlich als `0.0` oder `0 %` inkonsistent erscheinen.

### Profil

Das Profil muss die **finalen aktiven Werte** zeigen.

Beispiel:

```text
Leben              142.0 / 200.0 LP
Rüstung              96.5
Bewegungstempo       +18.5 %
Reichweite             3.25 Blöcke
Kritische Chance      54.0 %
Kritischer Schaden    +62.0 %
Lebensraub             4.5 %
Angriffskraft         38.0
```

### Aktueller Profil-Befund

`CharacterCardScoreboardService` rundet aktuell praktisch alle Werte auf ganze Zahlen. Außerdem wird Crit Damage aktuell als Gesamtmultiplikator gespeichert/angezeigt und nicht sauber als separater Bonuswert.

Das muss im späteren Implementierungsschritt korrigiert werden, damit der Spieler seine tatsächlichen Werte sehen kann.

---

## 22. Companion-Befund

Der aktuelle `StatEngine` kann zusätzlich passive Stats eines aktiven Companions in die Spielerwerte einrechnen.

Das steht im Konflikt mit der aktuellen Balance-Anforderung:

> **Stats werden nur über Waffen und Rüstung erzeugt.**

Für die neue Equipment-Balance gilt daher:

- Companion-Passivstats dürfen nicht heimlich den Equipment-Balance-Budgetrahmen sprengen.
- Entweder werden Companion-Kampfstats als separates zukünftiges System behandelt oder explizit aus der Kern-Equipment-Statkurve herausgenommen.
- Die Balance dieses Dokuments betrachtet die Kernstats zunächst **ohne Companion-Boni**.

Companion-System und Equipment-System dürfen später bewusst kombiniert werden, aber nur mit einem eigenen Power-Budget.

---

## 23. Wichtiger Unterschied: Item-Level vs. Player-Level

Ein Spielerlevel und ein Itemlevel sind zwei verschiedene Größen.

```text
Player Level
→ bestimmt, was benutzt werden darf

Item Level
→ bestimmt, wie stark ein Item rollen kann

Rarity
→ bestimmt Budget / Qualität / Statanzahl

Roll
→ bestimmt die konkrete Itemqualität
```

Ein Level-50-Spieler darf also beispielsweise ein Level-40-Item tragen, wenn dessen Required-Level erfüllt ist.

Ein Level-20-Spieler kann ein Level-50-Item besitzen/tragen, aber dessen PixelRPG-Stats bleiben deaktiviert, solange das Required-Level nicht erfüllt ist.

---

## 24. Best-in-Slot

Best-in-Slot bedeutet nicht:

```text
jedes Item maximal
```

sondern:

```text
passende Stat-Kombination
+
hohes Item-Level
+
hohe Rarität
+
guter Roll
+
passende Set-Boni
=
Best-in-Slot
```

Das erzeugt einen echten Grund, Items auszutauschen und zu vergleichen.

Ein schlechter Legendary-Roll darf daher schlechter sein als ein perfekter Epic-Roll für einen bestimmten Build.

Das ist gewollt.

---

## 25. Keine Klassen — dafür echte Build-Entscheidungen

Die wichtigsten Balancefragen werden deshalb nicht lauten:

> „Ist Klasse A stärker als Klasse B?“

sondern:

> „Ist dieser Stat-Mix stärker als jener Stat-Mix für diese Spielsituation?“

Ein Spieler soll beispielsweise zwischen:

```text
+30 LP
+5 Armor
```

und

```text
+8 % Crit
+6 % Crit Damage
```

eine echte Entscheidung treffen können.

Das ist der Kern des klassenlosen Systems.

---

## 26. Balance-Matrix für die Entwicklung

Jede Änderung an einem Stat muss mindestens diese Levelstufen prüfen:

```text
1
10
20
30
40
50
60
70
80
90
99
```

Für jede Stufe werden mindestens drei Builds simuliert:

```text
Defensiv
Offensiv
Hybrid
```

Zusätzlich:

```text
Worst Roll
Average Roll
Best Roll
```

Und für Endgame:

```text
Normal Legendary
Strong Legendary
Best-in-Slot Legendary
Set Build
```

---

## 27. Pflicht-Simulationen vor Runtime-Änderungen

Vor der Implementierung der neuen Kurve müssen folgende Fragen rechnerisch beantwortet werden:

### A — Progression

```text
Ist Level 20 spürbar stärker als Level 10?
Ist Level 50 spürbar stärker als Level 30?
Ist Level 80 spürbar stärker als Level 60?
Ist Level 99 deutlich stärker als Level 80?
```

### B — Gear

```text
Ist ein besseres Item spürbar?
Ist ein Legendary wirklich interessant?
Kann ein schlechter Legendary-Roll schlechter als ein guter Epic-Roll sein?
```

### C — Combat

```text
Wie viele Treffer benötigt ein Spieler?
Wie viele Treffer überlebt er?
Wie stark ist ein Crit wirklich?
Wie viel Heilung liefert Lifesteal?
```

### D — Monster

```text
Wie stark skaliert ein Monster mit dem Spieler?
Wie stark mit dem Gear?
Bleibt der Spieler trotz Scaling stärker?
```

### E — Caps

```text
Kann ein normaler Build den Cap erreichen?
Kann nur Best-in-Slot ihn erreichen?
Kann ein einzelnes Item den Cap brechen?
```

---

## 28. Forensische Probleme des aktuellen Systems

### F-BAL-001 — Exponentielle 1–99-Kurve ist zu grob

`growthMultiplier = 10.0` spannt eine zehnfache End-to-End-Skalierung auf, bevor Rarität und Roll berücksichtigt werden.

**Folge:** Die Basiswerte sind derzeit nicht zuverlässig auf die gewünschten Endgame-Caps kalibriert.

**Ziel:** neue Power-Kurve + Stat-Budget.

### F-BAL-002 — Einzelstat-Rolls besitzen kein gemeinsames Budget

Jeder gewählte Stat wird unabhängig aus `base × level × rarity × roll` berechnet.

**Folge:** Mehr Stats können gleichzeitig extrem groß werden.

**Ziel:** gemeinsames Item-Budget.

### F-BAL-003 — Legendary mit 8 Stats nutzt aktuell alle 8 Stattypen

Der aktuelle Pool enthält acht Stattypen und Legendary wählt acht.

**Folge:** Legendary ist nicht wirklich ein Build-Roll, sondern eine fast vollständige Stat-Sammlung.

**Ziel:** bis zu acht Stat-Linien aus einem erweiterbaren/gewichteten Pool und begrenztem Budget.

### F-BAL-004 — Profilanzeige verliert Dezimalpräzision

Die Character-Card-Synchronisierung rundet Werte auf Integer.

**Folge:** echte Werte wie `12.5 %` werden nicht sichtbar.

**Ziel:** Profilanzeige verwendet dieselben finalen `CachedStats` mit definierter Formatierung.

### F-BAL-005 — Crit Damage ist semantisch uneinheitlich

Intern wird ein Multiplikator verwendet, während die Item-Lore teilweise Prozentwerte aus Rohwerten ableitet.

**Ziel:** intern klar trennen zwischen `critDamageBonusPercent` und `critMultiplier`.

### F-BAL-006 — Mob Gear Scaling basiert auf Item-Level statt tatsächlicher Power

Aktuell wird ein durchschnittliches Item-Level verwendet.

**Folge:** zwei Spieler mit gleichem Item-Level, aber stark unterschiedlichen Rolls können für Monster nahezu gleich stark wirken.

**Ziel:** Player Power Index aus finalen aktiven Stats.

### F-BAL-007 — Mob Scaling ist ereignisgetrieben, nicht primär Spawn-getrieben

Der vorhandene `MobLevelScalingListener` aktiviert/aktualisiert die Skalierung beim Targeting bzw. bei RPG-Schaden und stellt sie nach Timeout wieder her.

**Folge:** Der aktuelle Branch ist nicht als vollständige „Spawn-Skalierung anhand des Spielers“ zu verstehen.

**Ziel:** Spawn-/Region-System und Combat-Scaling später bewusst zusammenführen, ohne ungewollte Vanilla-Mobs zu verändern.

### F-BAL-008 — Companion-Passivstats beeinflussen aktuell dieselbe StatEngine

**Folge:** Equipment ist aktuell nicht die einzige Quelle für aktive Spielerstats.

**Ziel:** klare Power-Budgets für Equipment und zukünftige Companion-Boni.

---

## 29. Implementierungsreihenfolge

Die Balance wird **nicht** durch einzelne Multiplikatoränderungen gefixt.

Reihenfolge:

```text
1. Stat-Semantik festlegen
2. Einheitliche CachedStats definieren
3. Levelkurve implementieren
4. Item-Stat-Budget implementieren
5. Raritätsbudget kalibrieren
6. Slot-Gewichtung kalibrieren
7. HP validieren
8. Angriffskraft + Monster-TTK validieren
9. Rüstung + Monster-DTK validieren
10. Crit validieren
11. Crit Damage validieren
12. Lifesteal validieren
13. Movement validieren
14. Reach validieren
15. Set-Budget validieren
16. Mob Power Index implementieren
17. Lore aus Finalstats generieren
18. Profil aus Finalstats generieren
19. Level-Gate für Abilities zentralisieren
20. automatisierte Balance-Tests ergänzen
```

---

## 30. Testfälle, die zwingend bestehen müssen

### Vanilla-Zustand

```text
Spieler ohne PixelRPG-Gear
→ PixelRPG-Bonusstats überall 0
→ Vanilla-Verhalten unverändert
```

### Level-Gate

```text
Spieler Level 10
Item Required Level 20
→ Item bleibt tragbar
→ Stats = 0
→ Set-Teil = nicht aktiv
→ Ability = nicht aktiv
```

### Aktivierung

```text
Spieler Level 20
Item Required Level 20
→ Stats aktiv
→ Set zählt
→ Ability darf funktionieren
```

### Unequip

```text
Gear entfernt
→ Stats verschwinden
→ Vanilla-Basis bleibt
→ Profil/Lore/Gameplay synchron
```

### Cap

```text
Gear erzeugt mehr als Cap
→ Finaler Wert wird korrekt begrenzt
→ Lore zeigt effektiven Wert
→ Profil zeigt effektiven Wert
→ Gameplay verwendet effektiven Wert
```

### Best-in-Slot

```text
Perfekter Level-99-Build
→ sehr stark
→ darf relevante Soft Targets erreichen
→ darf harte Gameplay-Caps nicht brechen
```

### Monster

```text
Besseres Gear
→ Monster dürfen stärker werden
→ Spieler muss trotzdem einen Netto-Fortschritt spüren
```

---

## 31. Balance-Philosophie

PixelRPG soll nicht versuchen, Zahlen möglichst groß zu machen.

Die richtige Frage lautet:

> **Wie viel stärker fühlt sich ein besser ausgerüsteter Spieler tatsächlich an?**

Ein guter Stat ist einer, dessen Wirkung der Spieler bemerkt.

Ein schlechter Stat ist einer, der mathematisch existiert, aber im Spiel praktisch nichts verändert.

Deshalb sind Werte wie:

```text
+0.1 %
+0.2 %
+0.3 %
```

bei den meisten sichtbaren Kernstats im frühen/mittleren Spiel unerwünscht, wenn der Unterschied nicht spürbar ist.

Die Progression soll stattdessen klar lesbare Sprünge liefern:

```text
schlechtes Gear
→ brauchbares Gear
→ gutes Gear
→ sehr gutes Gear
→ Endgame Gear
→ Best-in-Slot
```

---

## 32. Endgültige Designregel

> **PixelRPG ist ein klassenloses Equipment-RPG.**
>
> Der Spieler wird nicht durch eine Klasse definiert, sondern durch die Kombination seiner Ausrüstung.
>
> Level 1–99 liefert die langfristige Progressionsachse. Item-Level bestimmt die mögliche Stärke. Rarität bestimmt Budget und Vielfalt. Rolls bestimmen die konkrete Qualität. Sets spezialisieren Builds. Monster reagieren auf Spieler-Power, dürfen den Fortschritt aber nicht vollständig neutralisieren.
>
> Alle sichtbaren und spielrelevanten Werte müssen aus derselben finalen Stat-Berechnung stammen.

---

## 33. Was dieses Dokument bewusst NICHT macht

Dieses Dokument ist die **Balance-Grundlage**, keine Freigabe für blindes Überschreiben des aktuellen Codes.

Insbesondere werden folgende Werte erst nach Simulation endgültig festgeschrieben:

- exakte Armor-Obergrenze
- endgültiger Lifesteal-Cap
- endgültiger Attack-Power-Cap
- exakte Rarity-Budgets
- exakte Slot-Budgets
- exakte Roll-Spannen
- exakte Monster-Parity
- exakte TTK/DTK-Ziele pro Region/Mob-Tier

Die vorläufigen Zahlen in diesem Dokument sind bewusst so gewählt, dass sie **mathematisch miteinander funktionieren können** und anschließend durch Simulation und echte Paper-26.2-Tests validiert werden.

---

## 34. Quellen / geprüfte Repository-Komponenten

Die Forensik dieses Dokuments basiert auf dem Branch `test` und insbesondere auf:

- `src/main/java/de/pixelrpg/rpg/core/Level.java`
- `src/main/java/de/pixelrpg/rpg/item/RPGItemBuilder.java`
- `src/main/java/de/pixelrpg/rpg/item/ItemRarity.java`
- `src/main/resources/data/item-scaling.json`
- `src/main/java/de/pixelrpg/rpg/stats/StatEngine.java`
- `src/main/java/de/pixelrpg/rpg/equipment/EquipmentSetService.java`
- `src/main/java/de/pixelrpg/rpg/combat/CombatDamageCalculator.java`
- `src/main/java/de/pixelrpg/rpg/combat/CombatDamageListener.java`
- `src/main/java/de/pixelrpg/rpg/combat/scaling/MobLevelScalingListener.java`
- `src/main/java/de/pixelrpg/rpg/combat/scaling/MobScalingConfig.java`
- `src/main/resources/data/mob-scaling.json`
- `src/main/java/de/pixelrpg/rpg/dialogue/CharacterCardScoreboardService.java`
- bestehender `audit.md`

Die aktuelle Paper-26.2-API bestätigt die relevanten Vanilla-Attribute `MAX_HEALTH`, `ARMOR`, `MOVEMENT_SPEED`, `BLOCK_INTERACTION_RANGE` und `ENTITY_INTERACTION_RANGE` als aktuelle Attribute der Plattform.

---

## 35. Abschlussstatus

**Forensische Bestandsaufnahme:** abgeschlossen für die für Balance relevanten Kernpfade.  
**Balance-Architektur:** definiert.  
**Level-1–99-Modell:** definiert.  
**Endgame-Caps:** definiert bzw. bei Armor/Lifesteal/Attack Power zunächst als validierbare Zielwerte markiert.  
**Monster-Interaktion:** berücksichtigt.  
**Lore/Profil-Konsistenz:** berücksichtigt.  
**Klassenloses Build-System:** berücksichtigt.  
**Runtime-Code verändert:** **Nein.**

Der nächste technische Schritt ist eine **reine Balance-Simulation**, bevor die neuen Werte in `RPGItemBuilder`, `StatEngine`, Mob Scaling, Lore und Profil übernommen werden.
