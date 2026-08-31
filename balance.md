# PixelRPG — Balance Specification v2

**Branch:** `test`  
**Design basis:** `balance/v1-foundation` + current `test/balance.md` + requested replacement model  
**Target:** Paper 26.2 / Java 25  
**Level range:** 1–99  
**System type:** classless MMORPG/RPG  
**Status:** **NEW AUTHORITATIVE BALANCE SPECIFICATION**

> Dieses Dokument ersetzt die vorherige Balance-Spezifikation vollständig. Es ist die verbindliche mathematische Zieldefinition für die nächste Balance-Implementierung. Die Analyse des Branches `balance/v1-foundation` hat gezeigt, dass bereits eine zentrale Item-Pipeline, StatEngine, Level-Gates, Player-Power-Index und dynamisches Mob-Scaling vorhanden sind. Diese Architektur wird nicht blind verworfen; die mathematischen Einheiten und Verantwortlichkeiten werden sauber neu definiert.

---

## 0. Executive Summary

PixelRPG verwendet eine eigene RPG-Skala und darf nicht die Vanilla-Skala mit der RPG-Skala vermischen.

Die wichtigste Grundregel lautet:

> **Ein registrierter PixelRPG-Spieler lebt intern in PixelRPG-HP. Minecraft erhält nur die technisch notwendige umgerechnete Attributdarstellung.**

### Verbindliche HP-Skala

```text
10 PixelRPG-Herzen = 100 PixelRPG HP
1 PixelRPG-Herz   = 10 PixelRPG HP
```

Minecraft verwendet intern weiterhin sein natives `minecraft:max_health`-Attribut:

```text
20.0 Minecraft max_health = 10 Herzen
40.0 Minecraft max_health = 20 Herzen
```

Daher gilt für registrierte PixelRPG-Spieler:

```text
Minecraft max_health = PixelRPG HP / 5
```

Beispiele:

```text
100 PixelRPG HP → 20.0 Minecraft max_health → 10 Herzen
125 PixelRPG HP → 25.0 Minecraft max_health → 12,5 Herzen
150 PixelRPG HP → 30.0 Minecraft max_health → 15 Herzen
200 PixelRPG HP → 40.0 Minecraft max_health → 20 Herzen
```

**Das alte Modell `200 LP = 200 Minecraft-HP` ist ausdrücklich ungültig.** Es würde 100 Minecraft-Herzen erzeugen und damit die Vanilla-Darstellung zerstören.

### Absolute Endgame-Hard-Caps

| Stat | PixelRPG-Skala | Minecraft-Grenze | Endgame-Cap |
|---|---:|---:|---:|
| HP | 100–200 HP | `max_health` 20–40 | **200 PixelRPG HP / 40.0 MC** |
| Armor | 0–20 Punkte | `armor` 0–20 | **20.0 Armor** |
| Movement | 0–30 % Bonus | Basis 0.10 → ca. 0.13 | **+30 %** |
| Reach | Gesamtwert | beide Reach-Attribute | **5.0 Blöcke** |
| Crit Chance | 0–100 % | RPG-System | **100 %** |
| Crit Damage | Bonus | 2.0x Basis | **+100 % / 3.0x gesamt** |
| Lifesteal | 0–8 % | RPG-System | **8 %** |
| Attack Power | absolut | RPG-Schaden | **15.0** |

Kein normaler Level-99-Build darf einen dieser Caps überschreiten. Caps werden **zentral nach sämtlichen Quellen** angewendet: Equipment, Sets, Companion-Passives, temporäre RPG-Buffs und sonstige aktive Statquellen.

---

# 1. Forensischer Ausgangszustand

## 1.1 Was im Foundation-Branch bereits existiert

Der Branch `balance/v1-foundation` besitzt bereits eine brauchbare technische Grundlage:

```text
ItemDefinitionRegistry
        ↓
ItemService
        ↓
RPGItemBuilder
        ↓
PDC Item Stats
        ↓
StatEngine
        ↓
CachedStats
        ↓
Combat / Mob Scaling
```

Die `StatEngine` berücksichtigt bereits registrierte Spieler, Equipment, Set-Boni und Companion-Passives und erzwingt bereits Maximalwerte. Außerdem existiert ein `PlayerPowerIndex`, der aus den aktiven `CachedStats` berechnet wird. fileciteturn21file0 fileciteturn9file0

Die Equipment-Erzeugung verwendet bereits einen zentralen `BalanceModel`, Raritäten, Item-Level und zufällige Stat-Auswahl. fileciteturn19file0 fileciteturn8file0

Das Mob-System erkennt registrierte Spieler als Teilnehmer und passt Mob-HP und Mob-Schaden anhand von Level und einem Gear-Multiplikator an. fileciteturn17file0

## 1.2 Die entscheidenden mathematischen Fehler

### F-BAL-001 — Exponentielles Wachstum

Der bisherige Ansatz nutzte:

```text
levelFactor = growthMultiplier ^ levelProgress
```

mit:

```text
growthMultiplier = 10.0
```

Das erzeugt eine aggressive exponentielle Skalierung und macht niedrige und hohe Level schwer sauber aufeinander abzustimmen. Die aktuelle Datenquelle enthält diesen Wert weiterhin. fileciteturn22file0

**Entscheidung:** `growthMultiplier` ist keine gültige Combat-Balance-Variable mehr.

### F-BAL-002 — Kein gemeinsames Stat-Budget

Das Foundation-Modell teilt das Item-Budget zwar bereits auf ausgewählte Stats auf, verwendet aber noch normalisierte Einzel-Caps als direkte Umrechnung. Dadurch ist das mathematische Verhältnis der Stats nicht ausreichend definiert. fileciteturn8file0

**Entscheidung:** Jedes Item erhält ein explizites Budget. Jeder Stat besitzt einen exakt definierten Budgetpreis. Das Budget wird zuerst auf Stat-Linien verteilt und erst danach in Spielwerte umgerechnet.

### F-BAL-003 — Zu viele Stat-Linien

Foundation erlaubt aktuell für Legendary/Unique bis zu 8 Linien. Da der Kernpool ebenfalls 8 Stats enthält, kann ein Item alle 8 Stats erhalten. Der Builder wählt die Linien aktuell zufällig aus dem vollständigen Pool und teilt das Budget anschließend auf. fileciteturn19file0

**Entscheidung:** Kein normales Legendary/Unique darf mehr automatisch alle acht Kernstats tragen.

### F-BAL-004 — HP-Einheit falsch definiert

Foundation behandelt `MAX_HP_BONUS = 180` als direkten Minecraft-Health-Bonus. Zusammen mit `BASE_HEALTH = 20` führt das bis zu 200 Minecraft-HP. fileciteturn8file0 fileciteturn21file0

**Entscheidung:** PixelRPG HP und Minecraft `max_health` werden strikt getrennt.

### F-BAL-005 — Armor-Einheit falsch dimensioniert

Foundation definiert bis zu 200 Armor und überträgt diesen Wert direkt auf Minecrafts Armor-Attribut. fileciteturn8file0

**Entscheidung:** Das absolute Minecraft-Cap ist 20 Armor-Punkte. PixelRPG darf diesen Wert nicht überschreiten.

### F-BAL-006 — Mob Scaling ist zwar PPI-basiert, aber mathematisch zu grob

Foundation verwendet bereits aktive Stats für den PPI und nimmt den höchsten Gear-Multiplikator der aktiven Teilnehmer. fileciteturn9file0 fileciteturn17file0

Das ist architektonisch richtig, aber der PPI mittelt acht unterschiedlich wertvolle Stats mit gleicher Gewichtung. Außerdem liegt die aktuelle Mob-Skalierung bei einem linearen `5 HP/Level` und `0.30 Damage/Level` plus Gear-Multiplikator. fileciteturn23file0 fileciteturn24file0

**Entscheidung:** PPI wird als gewichtete Kampfkraft modelliert und nicht als einfacher Mittelwert aller Caps.

---

# 2. Grundprinzipien des neuen Systems

## 2.1 Drei getrennte Ebenen

Jeder Wert muss genau einer Ebene zugeordnet werden:

### Ebene A — RPG-Wert

Die Einheit, die das Game Design verwendet.

Beispiele:

```text
100 HP
14 Armor
25 % Crit
6 % Lifesteal
10 Attack Power
```

### Ebene B — Balance-Wert

Normalisierte Größe für Budget und PPI:

```text
0.00 = 0 % des Caps
1.00 = 100 % des Caps
```

### Ebene C — Minecraft-Attribut

Nur die technische Darstellung im Server:

```text
PixelRPG HP 100 → minecraft:max_health 20.0
PixelRPG HP 200 → minecraft:max_health 40.0
```

**Keine mathematische Balanceformel darf Ebene A und Ebene C direkt vermischen.**

## 2.2 Registrierung ist die Systemgrenze

Nicht registriert:

```text
Vanilla Minecraft
```

Registriert:

```text
PixelRPG Character System
```

Sobald ein Spieler als PixelRPG-Spieler registriert ist, werden die PixelRPG-Basiswerte aktiv:

```text
100 HP
0 Armor Bonus
0 % Crit
0 % Lifesteal
0 Attack Power
```

Wichtig: `100 PixelRPG HP` entspricht technisch `20.0 minecraft:max_health`. Der Spieler hat also weiterhin 10 sichtbare Herzen, aber PixelRPG arbeitet intern mit 100 HP.

Beim Unregister müssen PixelRPG-Modifier vollständig entfernt und Vanilla-Werte wiederhergestellt werden.

---

# 3. Level-Progression 1–99

## 3.1 Power-Funktion

Die neue Levelkurve lautet:

\[
x = \frac{L-1}{98}
\]

\[
P(L)=0.04+0.96\cdot x^{1.15}
\]

mit:

```text
L ∈ [1,99]
P(1)  = 0.04
P(99) = 1.00
```

Die Funktion ist monoton steigend, vermeidet die alte exponentielle Explosion und legt trotzdem einen größeren Teil der sichtbaren Power in die höheren Level.

### 3.2 Referenztabelle

| Level | Power-Faktor |
|---:|---:|
| 1 | 4.0 % |
| 10 | 10.2 % |
| 20 | 18.6 % |
| 30 | 27.7 % |
| 40 | 37.3 % |
| 50 | 47.3 % |
| 60 | 57.6 % |
| 70 | 68.1 % |
| 80 | 78.9 % |
| 90 | 89.9 % |
| 99 | 100.0 % |

## 3.3 Designinterpretation

Level ist **kein direkter Schadensmultiplikator**.

Level bestimmt, wie viel Item-/Character-Power verfügbar ist.

Damit gilt:

```text
Level → Progressionsfreigabe
Gear  → tatsächliche Kampfkraft
PPI   → Skalierungsreferenz für Encounter
```

Das verhindert, dass ein Level-99-Spieler automatisch stark ist, wenn er schlechte Ausrüstung trägt.

---

# 4. Stat-System und Hard Caps

## 4.1 HP

### PixelRPG-Einheit

```text
1 Herz = 10 PixelRPG HP
10 Herzen = 100 PixelRPG HP
20 Herzen = 200 PixelRPG HP
```

### Vanilla-Konvertierung

\[
MCHealth = \frac{RPGHP}{5}
\]

### Werte

| RPG HP | MC max_health | Herzen |
|---:|---:|---:|
| 100 | 20.0 | 10 |
| 110 | 22.0 | 11 |
| 120 | 24.0 | 12 |
| 150 | 30.0 | 15 |
| 180 | 36.0 | 18 |
| 200 | 40.0 | 20 |

### Hard Cap

```text
RPG HP ≤ 200
MC max_health ≤ 40.0
```

Die normale PixelRPG-Ausrüstung darf maximal **+100 RPG HP** gegenüber der registrierten Basis von 100 HP erzeugen.

Damit ist die vom Nutzer gewünschte Semantik eindeutig:

```text
registriert: 100 / 100 HP = 10 Herzen
Endgame:     200 / 200 HP = 20 Herzen
```

## 4.2 Armor

Hard Cap:

```text
0 ≤ Armor ≤ 20
```

Minecraft:

```text
20 Armor = 10 Rüstungssymbole
```

PixelRPG behandelt Armor als tatsächlichen aktiven Gesamtwert, nicht als `+200`-Pseudo-Ressource.

Die Balanceformel für PixelRPG-Schaden lautet:

\[
D_{after}=D\cdot\frac{100}{100+A}
\]

mit:

```text
A = aktiver PixelRPG Armor-Wert
```

Referenz:

| Armor | Multiplikator | Reduktion |
|---:|---:|---:|
| 0 | 1.000 | 0.0 % |
| 5 | 0.952 | 4.8 % |
| 10 | 0.909 | 9.1 % |
| 15 | 0.870 | 13.0 % |
| 20 | 0.833 | 16.7 % |

**Hinweis:** Das ist die PixelRPG-Kampfmitigation. Native Vanilla-Rüstungsmechaniken müssen im Runtime-Design so behandelt werden, dass sie nicht unkontrolliert zusätzlich auf dieselbe RPG-Schadensberechnung wirken. Ein Stat darf nicht doppelt gewertet werden.

## 4.3 Movement Speed

```text
Cap = +30.0 %
```

\[
Speed=0.10\cdot(1+Bonus)
\]

Bei +30 %:

```text
0.10 × 1.30 = 0.13
```

## 4.4 Reach

Reach ist ein **Gesamtzielwert**, kein frei addierbarer Bonus ohne Grenze.

```text
0.0–5.0 Blöcke Gesamt-Reichweite
```

Die Runtime muss `block_interaction_range` und `entity_interaction_range` getrennt setzen und jeweils auf `5.0` begrenzen.

Das Balance-Modell verwendet:

\[
ReachNorm=\frac{Reach-BaseReach}{5.0-BaseReach}
\]

Die tatsächlichen Vanilla-Basiswerte werden nicht als zusätzlicher RPG-Stat ausgegeben.

## 4.5 Crit Chance

```text
0–100 %
```

\[
C=\min(100,\sum C_i)
\]

## 4.6 Crit Damage

Basis:

```text
2.0×
```

PixelRPG-Bonus:

```text
0–100 %
```

Gesamt:

\[
CritMultiplier=2.0+\frac{Bonus}{100}
\]

Beispiele:

| Bonus | Multiplikator |
|---:|---:|
| 0 % | 2.00× |
| 25 % | 2.25× |
| 50 % | 2.50× |
| 75 % | 2.75× |
| 100 % | 3.00× |

## 4.7 Lifesteal

\[
Heal=DealtDamage\cdot\frac{Lifesteal}{100}
\]

Hard Cap:

```text
8.0 %
```

Lifesteal wird auf tatsächlich verursachten Schaden angewendet, nicht auf Rohschaden vor Mitigation.

## 4.8 Attack Power

Attack Power ist ein flacher absoluter Bonus:

\[
RawDamage=WeaponDamage+AttackPower
\]

Hard Cap:

```text
15.0 Attack Power
```

Der Wert ist bewusst deutlich kleiner als der bisherige Wert 60.0, weil das neue HP-/Armor-System wesentlich kompakter ist.

---

# 5. Stat-Pool

Der normale Kernpool besitzt acht Stats:

```text
HP
ARMOR
MOVEMENT_SPEED
REACH
ATTACK_POWER
CRIT_CHANCE
CRIT_DAMAGE
LIFESTEAL
```

Ein Item darf nur Stats aus seinem zulässigen Slot-Pool erhalten.

### Waffe

```text
ATTACK_POWER
CRIT_CHANCE
CRIT_DAMAGE
LIFESTEAL
REACH
HP
```

### Rüstung

```text
HP
ARMOR
MOVEMENT_SPEED
REACH
CRIT_CHANCE
LIFESTEAL
```

### Schild

```text
HP
ARMOR
REACH
LIFESTEAL
```

Tools sind vom Combat-Budget getrennt.

---

# 6. Stat-Linien pro Rarität

Die Anzahl der Linien ist eine Designentscheidung und kein Budgetersatz.

| Rarität | Stat-Linien |
|---|---:|
| Common | 1–2 |
| Uncommon | 2–3 |
| Rare | 3–4 |
| Epic | 4–5 |
| Legendary | **4–5** |
| Unique | **4–5** |

**Absolute Regel:** Kein normales Item darf alle acht Kernstats besitzen.

Legendary bedeutet nicht „alle Stats“. Legendary bedeutet „hohes Budget auf wenige, bewusst ausgewählte Stats“.

Das ermöglicht z. B.:

```text
Berserker:
Attack Power + Crit + Crit Damage + Lifesteal

Juggernaut:
HP + Armor + HP + Movement/Reach

Duelist:
Attack Power + Crit + Reach + Movement
```

Die Build-Identität entsteht durch die Auswahl der Stat-Linien, nicht durch Klassen.

---

# 7. Rarity Multipliers

Die Rarität multipliziert das verfügbare Item-Budget.

| Rarität | Multiplier |
|---|---:|
| Common | 0.55 |
| Uncommon | 0.70 |
| Rare | 0.82 |
| Epic | 0.92 |
| Legendary | 1.00 |
| Unique | 1.05 |

Unique ist nur geringfügig stärker als Legendary. Der Abstand soll nicht die gesamte Progression ersetzen.

Unique bleibt außerdem entsprechend der bestehenden Item-Regeln admin-/Content-gesteuert und ist kein zufälliger Massenroll.

---

# 8. Slot Weights

Das normale Endgame besteht für die Balance aus sechs Combat-Slots:

```text
1 × Weapon
5 × defensive slots
```

Für die Budgetrechnung werden folgende Gewichte verwendet:

| Slot | Gewicht |
|---|---:|
| Weapon | 1.50 |
| Chest | 1.50 |
| Legs | 1.25 |
| Helmet | 1.15 |
| Boots | 1.15 |
| Shield / 5. defensive slot | 1.45 |
| **Summe** | **8.00** |

Diese Summe ist absichtlich 8.00, weil die acht Kernstats jeweils als ein vollständiger Cap-Anteil normiert werden können.

Ein anderer Equipment-Layout-Typ darf diese Gesamtbudgetgrenze nicht umgehen. Wenn ein Server kein Schild verwendet, muss der fünfte defensive Slot durch ein anderes eindeutig definiertes Equipment-Segment ersetzt werden; es darf kein zusätzliches Budget entstehen.

---

# 9. Neues Item-Stat-Budget

## 9.1 Grundformel

\[
B_{item}=P(L)\cdot RarityMultiplier\cdot SlotWeight
\]

mit:

```text
P(L)             = Level Power Factor
RarityMultiplier = Raritätswert
SlotWeight       = Slotgewicht
```

Bei Level 99 Legendary Weapon:

\[
B=1.00\cdot1.00\cdot1.50=1.50
\]

Bei Level 99 Common Weapon:

\[
B=1.00\cdot0.55\cdot1.50=0.825
\]

---

# 10. Exakte Stat-Kosten

Die Budgeteinheit ist ein **Cap-Anteil**.

Ein vollständiger Cap eines Stats kostet exakt `1.0 Budget Unit`.

## 10.1 Kosten je Einheit

| Stat | Endgame-Cap | Budgetkosten pro Einheit |
|---|---:|---:|
| 1 RPG HP | 100 Bonus-HP | **0.01000** |
| 1 Armor | 20 Armor | **0.05000** |
| 1 % Movement | 30 % | **0.03333** |
| 1 Reach-Budgetpunkt | 2.0 norm. Bonus | **0.50000** |
| 1 Attack Power | 15 | **0.06667** |
| 1 % Crit Chance | 100 % | **0.01000** |
| 1 % Crit Damage Bonus | 100 % | **0.01000** |
| 1 % Lifesteal | 8 % | **0.12500** |

Die Kosten sind mathematisch äquivalent zu:

\[
Cost(statValue)=\frac{statValue}{statCap}
\]

mit Ausnahme von Reach, weil Reach technisch in einen Gesamtwert konvertiert wird.

## 10.2 Budgetprüfung

Für ein Item mit Statwerten \(s_i\):

\[
B_{used}=\sum_i Cost(s_i)
\]

Es muss gelten:

\[
B_{used}\le B_{item}
\]

Ein Item darf niemals durch Rundung, Random Roll oder Mehrfachquellen über sein Budget kommen.

---

# 11. Budget Allocation statt unabhängiger Max-Rolls

Der Erzeugungsprozess lautet verbindlich:

```text
Item Level
   ↓
Level Power
   ↓
Rarity Multiplier
   ↓
Slot Weight
   ↓
TOTAL ITEM BUDGET
   ↓
Stat-Line Count
   ↓
Stat Pool Selection
   ↓
Weighted Budget Allocation
   ↓
Roll Quality
   ↓
Final Stat Values
   ↓
Budget Validation
   ↓
PDC
```

## 11.1 Kein gleiches Max-Roll für alle Stats

Das alte Prinzip:

```text
jeder Stat = eigener Maximalwert × Level × Rarity
```

ist verboten.

Ein Item besitzt **ein Gesamtbudget**.

Wenn ein Legendary vier Linien besitzt und sein Budget 1.50 beträgt, können beispielsweise folgende Cap-Anteile entstehen:

```text
Attack Power   0.55
Crit Chance    0.40
Crit Damage    0.35
Lifesteal      0.20
--------------------
Summe          1.50
```

Das Item kann nicht zusätzlich kostenlos 0.80 HP oder 0.50 Armor erhalten.

---

# 12. Roll Quality

Der Random-Faktor darf nicht mehr zwischen `0.50` und `1.50` frei skalieren.

Neue Roll Quality:

```text
Minimum = 0.85
Maximum = 1.00
```

Empfohlen:

\[
Q\in[0.85,1.00]
\]

Die Quality verändert nur die Verteilung innerhalb des bereits zugewiesenen Budgets.

Sie darf niemals:

```text
Budget überschreiten
Hard Cap überschreiten
```

Ein schlechter Roll soll schlechter sein, aber kein mathematischer Ausreißer darf ein komplettes Itemsystem zerstören.

---

# 13. Endgame-Budget und Hard-Cap-Erreichbarkeit

Bei sechs Level-99-Unique-Slots wäre das theoretische Rohbudget:

\[
B_{total}=8.00\cdot1.05=8.40
\]

Dieses Rohbudget darf nicht automatisch in mehr als acht vollständige Cap-Einheiten umgewandelt werden.

Der Character Layer führt deshalb einen zweiten globalen Cap:

\[
B_{character}\le8.00
\]

Das bedeutet:

```text
Item Budget ≠ Charakterbudget
```

Item-Budgets erzeugen Gear.

Der Character Aggregator summiert aktive Werte und clamp't anschließend jeden Stat auf seinen individuellen Hard Cap.

Dadurch ist ein perfekter Build mathematisch in der Lage, die gewünschten Endgame-Caps zu erreichen, aber niemals darüber hinaus.

---

# 14. Character Stat Aggregation

## 14.1 Aktive Quellen

Der Character Aggregator berücksichtigt:

```text
Equipped Items
+ Set Bonuses
+ Active Companion Passive
+ sonstige explizit freigegebene RPG-Statquellen
```

Level selbst erzeugt keine direkten Combat-Stats.

## 14.2 Aggregation

Für jeden Stat:

\[
S_{raw}=\sum_{source=1}^{n}S_{source}
\]

Danach:

\[
S_{final}=clamp(S_{raw},0,S_{cap})
\]

Beispiel HP:

```text
Basis = 100 RPG HP
Items = +82 HP
Set   = +12 HP
Comp. = +10 HP
----------------
Raw   = 204 HP
Final = 200 HP
```

Der Spieler erhält niemals 204 HP.

---

# 15. Level-Gate

Ein Item darf weiterhin angelegt werden, aber seine Stats und Fähigkeiten werden erst aktiv, wenn das benötigte Level erreicht wurde.

```text
requiredLevel <= playerLevel
```

Nur dann zählt das Item in:

```text
StatEngine
PPI
Combat
Set Activation
Ability Activation
```

Ein Item unter dem Required Level ist technisch ausgerüstet, aber statistisch **inaktiv**.

Das schützt das Balance-System davor, dass ein Level-1-Spieler ein Level-99-Item als versteckten Stat-Stick verwendet.

---

# 16. Player Power Index (PPI)

## 16.1 Ziel

PPI beschreibt die **tatsächlich aktive Kampfkraft** eines Spielers.

Nicht:

```text
Item Level
Gearscore
Rarity
Anzahl Items
```

sondern ausschließlich aktive, bereits angewendete Character Stats.

## 16.2 Normierte Statwerte

Für jeden Stat wird zunächst normalisiert:

\[
N_i=clamp\left(\frac{S_i}{Cap_i},0,1\right)
\]

## 16.3 Stat-Gewichte

Nicht jeder Stat besitzt dieselbe Kampfrelevanz.

Verbindliche Startgewichte:

| Stat | Gewicht |
|---|---:|
| HP | 0.16 |
| Armor | 0.16 |
| Movement | 0.06 |
| Reach | 0.05 |
| Attack Power | 0.22 |
| Crit Chance | 0.14 |
| Crit Damage | 0.14 |
| Lifesteal | 0.07 |
| **Summe** | **1.00** |

## 16.4 PPI-Formel

\[
PPI=\sum_i w_iN_i
\]

Damit:

```text
0.00 = keinerlei aktive RPG-Kampfkraft
1.00 = vollständiger Cap-Build
```

## 16.5 Warum kein einfacher Mittelwert?

Ein Punkt Crit Chance ist nicht dieselbe Kampfkraft wie ein Punkt Attack Power.

Ein Tank mit hoher HP und Armor soll einen hohen PPI erhalten, aber nicht so behandelt werden, als hätte er denselben offensiven Output wie ein DPS-Build.

Der PPI ist deshalb eine **Encounter-Referenz**, kein Damage-Wert.

---

# 17. Offensive und Defensive PPI-Komponenten

Für feinere Encounter-Entscheidungen werden zusätzlich zwei Teilindizes geführt.

### Offensive Power Index

\[
OPI=0.45N_{Attack}+0.25N_{Crit}+0.20N_{CritDamage}+0.10N_{Lifesteal}
\]

### Defensive Power Index

\[
DPI=0.55N_{HP}+0.35N_{Armor}+0.10N_{Movement}
\]

### Utility Index

\[
UI=0.60N_{Reach}+0.40N_{Movement}
\]

Der Gesamt-PPI bleibt:

\[
PPI=0.55OPI+0.35DPI+0.10UI
\]

Diese Form kann später durch Telemetrie angepasst werden, ohne die eigentlichen Stat-Caps zu verändern.

---

# 18. Monster Scaling

## 18.1 Grundprinzip

Ein Monster wird nicht anhand des Item-Levels skaliert.

Es erhält:

```text
Encounter Level
+ Player Level
+ Player PPI
```

Die vorhandene Foundation-Architektur erfasst bereits registrierte Teilnehmer und wendet Level/Gear Scaling auf Monsterattribute an. fileciteturn17file0

Die mathematische Berechnung wird jedoch ersetzt.

## 18.2 Level-Faktor

Für Spielerlevel `L`:

\[
F_L=P(L)
\]

## 18.3 Gear-Faktor

\[
F_G=0.85+0.30\cdot PPI
\]

Damit gilt:

```text
PPI 0.00 → 0.85
PPI 0.50 → 1.00
PPI 1.00 → 1.15
```

Das ist absichtlich enger als die bisherige Spanne 0.75–1.25. fileciteturn9file0

Der Grund: Das Monster soll Gear nicht vollständig neutralisieren.

---

# 19. Monster HP Formel

Für einen normalen Mob-Archetyp mit Basiswert `H0`:

\[
MobHP=H_0\cdot(1+1.80\cdot P(L))\cdot F_G
\]

Das erzeugt eine klare Levelprogression, ohne exponentielle Explosion.

Beispiel mit `H0 = 20`:

| Spielerlevel | P(L) | PPI | HP ungefähr |
|---:|---:|---:|---:|
| 1 | 0.04 | 0.00 | 20.7 |
| 20 | 0.19 | 0.25 | 31.5 |
| 50 | 0.47 | 0.50 | 50.9 |
| 80 | 0.79 | 0.75 | 72.7 |
| 99 | 1.00 | 1.00 | 80.5 |

Die tatsächlichen H0-Werte werden pro Mob-Archetyp definiert. Diese Formel ist die Skalierungsbasis, nicht die endgültige HP jedes Vanilla-Mobs.

---

# 20. Monster Damage Formel

Für Basis-Mob-Schaden `D0`:

\[
MobDamage=D_0\cdot(1+1.20\cdot P(L))\cdot(0.90+0.20\cdot PPI)
\]

Bei Level 99:

```text
PPI 0.00 → 2.20 × 0.90 = 1.98 × D0
PPI 0.50 → 2.20 × 1.00 = 2.20 × D0
PPI 1.00 → 2.20 × 1.10 = 2.42 × D0
```

Der Unterschied zwischen schlechtem und perfektem Gear bleibt damit spürbar, aber das Monster erhält keinen 1:1-Ausgleich für jeden Gear-Fortschritt.

---

# 21. Warum Mob Scaling den Gear-Fortschritt nicht neutralisieren darf

Falsches System:

```text
Spieler wird 50 % stärker
Monster wird ebenfalls 50 % stärker
→ Fortschritt fühlt sich nicht an
```

Neues Ziel:

```text
Spieler Gear ↑
    ↓
PPI ↑
    ↓
Monster etwas stärker
    ↓
Spieler bleibt netto stärker
    ↓
TTK sinkt
DTK steigt
```

Das ist eine zentrale Designanforderung.

---

# 22. TTK / DTK Zielwerte

## 22.1 Definition

\[
TTK=\frac{EffectiveMobHP}{AveragePlayerDPS}
\]

\[
DTK=\frac{EffectivePlayerHP}{AverageIncomingDPS}
\]

Für diskrete Nahkampfangriffe:

\[
HitsToKill=ceil\left(\frac{MobHP}{AverageHitDamage}\right)
\]

## 22.2 Level-99 Standard-Mob Ziel

Für einen normalen Level-99-Mob werden folgende Zielbereiche verwendet:

### Berserker/DPS-Build

```text
4–7 Treffer
~4–6 Sekunden TTK
```

### Balanced Build

```text
6–9 Treffer
~6–9 Sekunden TTK
```

### Tank Build

```text
8–12 Treffer
~8–12 Sekunden TTK
```

Ein Tank darf langsamer töten, muss aber deutlich länger überleben.

## 22.3 DTK Ziel

Ein Standard-Mob soll einen vollständigen Endgame-Build nicht innerhalb eines einzelnen normalen Angriffs töten.

Ziel:

```text
mindestens 8–15 normale Treffer
```

je nach Mob-Archetyp.

Elite-Mobs dürfen deutlich gefährlicher sein.

---

# 23. Build-Archetypen ohne Klassen

PixelRPG bleibt klassenlos.

Stats erzeugen implizite Spielstile.

## Berserker

Priorität:

```text
Attack Power
Crit Chance
Crit Damage
Lifesteal
```

Ziel:

```text
hoher Burst
niedrige bis mittlere Defensive
```

## Juggernaut

Priorität:

```text
HP
Armor
Lifesteal
Movement
```

Ziel:

```text
hohe DTK
niedrigere DPS
```

## Duelist

Priorität:

```text
Attack Power
Crit
Movement
Reach
```

Ziel:

```text
hoher konstanter Schaden
Positionierung
```

## Sustained DPS

Priorität:

```text
Attack Power
Crit
Lifesteal
Crit Damage
```

Ziel:

```text
hoher Schaden + Selbstheilung
```

Es gibt keine Klassenrestriktion. Ein Spieler baut diese Profile ausschließlich über Gear.

---

# 24. Set-Boni

Set-Boni sind **zusätzliche Statquellen**, kein Weg um Item-Budgets zu umgehen.

Ein Setbonus wird zuerst als normale Statquelle aggregiert und anschließend durch denselben Character Hard-Cap begrenzt.

Beispiel:

```text
Items:
HP +90

Set:
HP +20

Companion:
HP +10

Raw = 120
Cap = 100 Bonus
Final Bonus = 100
```

Bei HP:

```text
100 Basis + 100 Bonus = 200 RPG HP
```

---

# 25. Companion-Regel

Companion-Passives werden wie aktive Character-Statquellen behandelt.

Sie dürfen nicht den globalen Character-Cap umgehen.

Ein Companion kann einen Build vervollständigen, aber niemals:

```text
200 HP → 230 HP
20 Armor → 25 Armor
100 % Crit → 115 % Crit
```

erzeugen.

Die Foundation-StatEngine bindet Companion-Passives bereits in die Statberechnung ein und clamp't anschließend auf die zentralen Caps. fileciteturn21file0

Diese zentrale Aggregation bleibt das gewünschte Architekturprinzip.

---

# 26. Combat-Reihenfolge

Die Kampfberechnung wird strikt in dieser Reihenfolge definiert:

```text
1. Angriff erkennen
2. registrierten PixelRPG-Spieler bestimmen
3. aktive CachedStats lesen
4. Weapon Damage bestimmen
5. Attack Power addieren
6. Crit prüfen
7. Crit Damage anwenden
8. Ziel-Armor bestimmen
9. PixelRPG-Mitigation anwenden
10. finalen Schaden bestimmen
11. Lifesteal aus realem Schaden berechnen
12. HP clampen
13. Combat Events / Statistics auslösen
```

Formell:

\[
D_0=W+AP
\]

\[
D_1=D_0\cdot CritMultiplier
\]

\[
D_2=D_1\cdot\frac{100}{100+A}
\]

\[
Heal=D_2\cdot\frac{LS}{100}
\]

---

# 27. Crit-Erwartungswert

Für Balance-Simulationen ist der durchschnittliche Crit-Schaden wichtiger als ein einzelner Max-Hit.

Mit Crit Chance `C` und Crit Multiplier `M`:

\[
ExpectedDamage=D\cdot((1-C)+C\cdot M)
\]

mit `C` als Dezimalwert.

Bei:

```text
C = 50 %
M = 2.50
```

gilt:

\[
ED=D\cdot(0.5+0.5\cdot2.5)=1.75D
\]

Das ist die relevante Größe für TTK-Simulationen.

---

# 28. Attack Power Cap Begründung

Der alte Wert von 60 Attack Power ist für das neue HP-System zu hoch.

Bei:

```text
100 RPG HP = 10 Herzen
200 RPG HP = 20 Herzen
```

muss ein einzelner normaler Hit nicht plötzlich einen großen Teil der gesamten Lebensleiste entfernen.

`15 Attack Power` ist deshalb der neue Start-Hard-Cap.

Der Cap darf erst nach realen TTK-Simulationen geändert werden.

Wenn die Simulation zeigt, dass:

```text
DPS zu niedrig → Weapon Base Damage anpassen
DPS zu hoch   → Attack Power Budgetkosten erhöhen
```

und nicht einfach das HP-Cap erhöhen.

---

# 29. Reach-Balance

Reach ist extrem stark, obwohl es keinen direkten Schaden verursacht.

Deshalb wird Reach mit einem eigenen Budgetpreis bewertet und erhält keine kostenlose Synergie aus mehreren Items.

Ein Spieler darf maximal:

```text
5.0 Blöcke Block Reach
5.0 Blöcke Entity Reach
```

erreichen.

Die zwei technischen Minecraft-Attribute werden getrennt verwaltet.

Der Character Stat bleibt aber ein gemeinsamer Gameplay-Wert.

---

# 30. Rarity + Stat-Line Design

Rarität bestimmt **wie viel Budget** ein Item besitzt.

Stat-Lines bestimmen **wie konzentriert** das Budget verteilt wird.

Das erzeugt einen wichtigen Unterschied:

### Common

```text
wenig Budget
wenige Linien
```

### Legendary

```text
viel Budget
4–5 Linien
```

Ein Legendary mit vier Linien kann deshalb auf seinen gewählten Stats deutlich stärker sein als ein Legendary mit fünf Linien.

Das ist beabsichtigt.

Ein Spieler soll echte Entscheidungen zwischen:

```text
breitem Item
vs.
konzentriertem Item
```

treffen können.

---

# 31. Stat-Auswahl

Die Auswahl der Stat-Linien darf nicht einfach zufällig gleichverteilt über alle acht Stats erfolgen.

Sie muss mindestens diese Regeln einhalten:

1. Kein Duplikat desselben Stat-Typs auf einem Item.
2. Keine ungültigen Slot-Stats.
3. Keine acht Stats auf einem Item.
4. Stat-Auswahl zuerst, Budgetverteilung danach.
5. Ein Item darf nicht mehr als 100 % seines eigenen Stat-Caps ausgeben.
6. Das Gesamtbudget des Items darf nicht überschritten werden.

Optional kann später eine Affinitätstabelle eingeführt werden, z. B.:

```text
Weapon → offensive Stats bevorzugen
Chest  → HP/Armor bevorzugen
Boots  → Movement/Reach bevorzugen
```

Die Affinität darf aber nur die Wahrscheinlichkeit beeinflussen, nicht das Budget umgehen.

---

# 32. Item Budget Beispiel

Level 50 Legendary Chest:

\[
P(50)=0.4726
\]

\[
B=0.4726\cdot1.00\cdot1.50=0.7089
\]

Vier Stat-Linien könnten das Budget beispielsweise so verwenden:

```text
HP       0.30
Armor    0.25
Movement 0.08
Lifesteal 0.0789
----------------
Summe    0.7089
```

Umrechnung:

```text
HP:        0.30 × 100 = 30 RPG HP Bonus
Armor:     0.25 × 20  = 5 Armor
Movement:  0.08 × 30  = 2.4 %
Lifesteal: 0.0789 × 8 = 0.6312 %
```

Das Item erhält nicht zusätzlich noch einen kostenlosen fünften Stat.

---

# 33. Perfect Endgame Beispiel

Ein theoretischer Perfect-Build kann folgende Caps erreichen:

```text
200 RPG HP
20 Armor
+30 % Movement
5.0 Reach
100 % Crit
+100 % Crit Damage
8 % Lifesteal
15 Attack Power
```

Das bedeutet nicht, dass jedes einzelne Item diese Werte besitzt.

Die Werte werden über sechs Slots verteilt.

Beispiel einer extremen Buildverteilung:

```text
Weapon:
Attack Power + Crit + Crit Damage + Lifesteal

Chest:
HP + Armor + HP + Armor

Legs:
HP + Armor + Movement + HP

Helmet:
HP + Crit + Lifesteal + Movement

Boots:
Movement + Reach + HP + Crit

Shield:
Armor + HP + Reach + Lifesteal
```

Der Character Aggregator entscheidet am Ende ausschließlich über die tatsächlich aktiven Summen.

---

# 34. PPI und Builds

Zwei Level-99-Spieler können denselben PPI besitzen und trotzdem völlig unterschiedlich spielen.

Beispiel:

```text
Tank:
DPI hoch
OPI mittel

DPS:
OPI hoch
DPI niedrig
```

Der Gesamt-PPI ist daher nur eine Encounter-Skalierungsreferenz.

Monster dürfen nicht anhand des PPI plötzlich immun gegen den bevorzugten Build werden.

---

# 35. Multiplayer Scaling

Wenn mehrere registrierte Spieler ein Monster bekämpfen, darf nicht blind der höchste PPI eines Spielers verwendet werden.

Die Teilnehmerliste wird gewichtet.

Empfohlene Gruppenpower:

\[
PPI_{group}=0.70\cdot PPI_{highest}+0.30\cdot PPI_{average}
\]

Der höchste Spieler verhindert Unter-Skalierung.

Der Durchschnitt verhindert, dass ein einzelner Endgame-Spieler eine Gruppe aus schwachen Spielern vollständig hochskaliert.

Für `n` Spieler:

```text
highest = max(PPI_i)
average  = sum(PPI_i)/n
```

Dann:

\[
PPI_{group}=0.70highest+0.30average
\]

---

# 36. Combat Timeout

Die Foundation verwendet bereits einen Combat-Teilnehmer-Timeout von fünf Sekunden. fileciteturn17file0

Dieses Verhalten ist für die neue Mathematik sinnvoll:

```text
Damage / Target / Combat Activity
        ↓
Teilnehmer aktiv
        ↓
Mob scaled
        ↓
5 Sekunden ohne Aktivität
        ↓
Teilnehmer entfernt
        ↓
Scaling neu berechnet / Vanilla restore
```

Die fünf Sekunden sind ein technischer Timeout und kein Balancewert.

---

# 37. Mob Archetypes

Normale Mobs sollten nicht alle identische Werte erhalten.

Empfohlene Archetypen:

| Archetyp | HP | Damage | Ziel |
|---|---:|---:|---|
| Normal | 1.00× | 1.00× | Standardkampf |
| Bruiser | 1.35× | 1.10× | zäher Nahkämpfer |
| Glass Cannon | 0.75× | 1.45× | hoher Druck |
| Tank | 1.80× | 0.75× | langer Kampf |
| Elite | 2.50× | 1.60× | Endgame-Herausforderung |

Die Multiplikatoren werden nach der Level-/PPI-Skalierung angewendet.

---

# 38. Bosses

Bosses verwenden nicht automatisch die normalen Mob-Werte.

Die bestehende Boss-Pipeline besitzt bereits einen separaten Boss-Schadensschutz und weitere Bossmechaniken. Der Audit bestätigt, dass Boss-Hit-Cap und Boss-Scaling als eigene Systeme existieren. fileciteturn12file0

Für normale Boss-Balance gilt:

```text
Boss HP = Mob HP × Boss Archetype Multiplier
Boss Damage = Mob Damage × Boss Archetype Multiplier
```

Boss-Phasen, Fähigkeiten und spezielle Mechaniken dürfen zusätzlich wirken, müssen aber ihre Schadenswirkung auf die 200-RPG-HP-Hard-Cap-Skala abstimmen.

---

# 39. XP

XP darf nicht direkt proportional zu beliebig aufgeblasenen Monster-HP werden.

Das Foundation-System besitzt aktuell `xpPerMaxHealth = 4.0` in der JSON-Konfiguration. fileciteturn23file0

Für das neue System gilt:

\[
XP=BaseXP\cdot LevelFactor\cdot DifficultyFactor
\]

Monster-HP darf höchstens indirekt Einfluss nehmen.

Sonst entsteht der Exploit:

```text
Monster bekommt extrem viel HP
→ XP explodiert
→ künstliche Farm
```

---

# 40. Rundung und Präzision

Intern:

```text
Double
```

Für PDC und Lore gelten getrennte Darstellungen.

Empfehlung:

```text
HP          → 0.1
Armor       → 0.1
Movement    → 0.1 %
Reach       → 0.1
Crit        → 0.1 %
CritDamage  → 0.1 %
Lifesteal   → 0.1 %
Attack      → 0.1
```

Rundung darf erst nach der Budgetberechnung erfolgen.

**Nie:**

```text
round(each stat)
→ budget check
```

Sondern:

```text
budget calculation
→ exact values
→ cap
→ final rounding
→ PDC/Lore
```

---

# 41. Anti-Exploit-Regeln

Folgende Fälle müssen zentral abgefangen werden:

### E-001 — Over-cap Item

Wenn ein Item nach Laden/Import über einen Cap liegt:

```text
clamp + warning
```

### E-002 — Over-budget Item

Wenn:

\[
B_{used}>B_{item}
\]

ist das Item ungültig und darf nicht als gültiges Balance-Item behandelt werden.

### E-003 — Level-Gate Bypass

Item unter Required Level:

```text
Stats = 0
Abilities = inactive
Set contribution = 0
PPI contribution = 0
```

### E-004 — Companion Cap Bypass

Companion wird nach Aggregation gecapped.

### E-005 — Set Cap Bypass

Set wird nach Aggregation gecapped.

### E-006 — Multiple Attribute Modifiers

Es darf immer nur der zentrale PixelRPG-Modifier für ein Attribut existieren.

Die Foundation-StatEngine entfernt bereits den vorhandenen Modifier anhand des Keys, bevor ein neuer gesetzt wird. fileciteturn21file0

Dieses Prinzip bleibt verbindlich.

---

# 42. Minecraft Attribute Bridge

Die Attribute Bridge ist die einzige Stelle, die PixelRPG-Werte in Minecraft-Werte übersetzt.

## HP

\[
MCMaxHealth=RPGHP/5
\]

## Movement

\[
MCMovement=0.10\cdot(1+RPGMovement/100)
\]

## Armor

```text
MC Armor = clamp(RPG Armor, 0, 20)
```

## Reach

```text
MC Block Reach  = clamp(targetBlockReach, 0, 5)
MC Entity Reach = clamp(targetEntityReach, 0, 5)
```

Diese Bridge darf keine Balanceentscheidungen treffen.

Sie konvertiert nur.

---

# 43. HP UI

Die interne RPG-Anzeige sollte immer PixelRPG-Einheiten verwenden:

```text
HP: 137 / 200
```

Die Vanilla-Herzleiste bleibt sichtbar und wird über Minecrafts Health Scale dargestellt.

Wichtig:

```text
100 RPG HP = 10 Herzen
```

nicht:

```text
100 RPG HP = 50 Herzen
```

Die Foundation verwendet bereits `setHealthScaled(true)` und begrenzt die Health Scale auf maximal 40.0. fileciteturn21file0

Das ist mit der neuen Zieldefinition kompatibel, sofern `max_health` auf maximal 40.0 begrenzt wird.

---

# 44. Legacy-Werte — ausdrücklich ungültig

Die folgenden Werte dürfen nicht als neue Balance verwendet werden:

```text
growthMultiplier = 10.0
MAX_HP_BONUS = 180 Minecraft HP
MAX_ARMOR = 200
MAX_ATTACK_POWER = 60
MAX_REACH_BONUS = 0.5 als Gesamtmodell
Legendary = 8 Stats
Unique = 8 Stats
0.50–1.50 unbounded roll
```

Sie gehören zur historischen Forensik und nicht zur neuen Balance.

Die vorhandene `item-scaling.json` enthält diese Legacy-Werte noch. fileciteturn22file0

Die neue Runtime muss sie ignorieren oder durch die zentrale neue Balance-Konfiguration ersetzen.

---

# 45. Code-Blueprint

Der folgende Blueprint beschreibt die gewünschte Architektur. Er ist bewusst kein vollständiger Runtime-Patch.

```java
public record CharacterStats(
        double hp,
        double armor,
        double movementPercent,
        double reach,
        double attackPower,
        double critChance,
        double critDamageBonusPercent,
        double lifesteal
) {}

public final class BalanceModel {

    public static double levelPower(int level) {
        int safe = Math.clamp(level, 1, 99);
        double x = (safe - 1.0) / 98.0;
        return 0.04 + 0.96 * Math.pow(x, 1.15);
    }

    public static double itemBudget(
            int itemLevel,
            double rarityMultiplier,
            double slotWeight
    ) {
        return levelPower(itemLevel)
                * rarityMultiplier
                * slotWeight;
    }

    public static double statCost(Stat stat, double value) {
        return switch (stat) {
            case HP -> value / 100.0;
            case ARMOR -> value / 20.0;
            case MOVEMENT -> value / 30.0;
            case REACH -> value / 2.0;
            case ATTACK_POWER -> value / 15.0;
            case CRIT_CHANCE -> value / 100.0;
            case CRIT_DAMAGE -> value / 100.0;
            case LIFESTEAL -> value / 8.0;
        };
    }

    public static CharacterStats cap(CharacterStats raw) {
        return new CharacterStats(
                Math.clamp(raw.hp(), 100.0, 200.0),
                Math.clamp(raw.armor(), 0.0, 20.0),
                Math.clamp(raw.movementPercent(), 0.0, 30.0),
                Math.clamp(raw.reach(), 0.0, 5.0),
                Math.clamp(raw.attackPower(), 0.0, 15.0),
                Math.clamp(raw.critChance(), 0.0, 100.0),
                Math.clamp(raw.critDamageBonusPercent(), 0.0, 100.0),
                Math.clamp(raw.lifesteal(), 0.0, 8.0)
        );
    }
}
```

## PPI Blueprint

```java
public static double ppi(CharacterStats stats) {
    double hp       = (stats.hp() - 100.0) / 100.0;
    double armor    = stats.armor() / 20.0;
    double move     = stats.movementPercent() / 30.0;
    double reach    = stats.reach() / 5.0;
    double attack   = stats.attackPower() / 15.0;
    double crit     = stats.critChance() / 100.0;
    double critDmg  = stats.critDamageBonusPercent() / 100.0;
    double lifesteal= stats.lifesteal() / 8.0;

    return 0.16 * hp
         + 0.16 * armor
         + 0.06 * move
         + 0.05 * reach
         + 0.22 * attack
         + 0.14 * crit
         + 0.14 * critDmg
         + 0.07 * lifesteal;
}
```

## Mob Blueprint

```java
public static double gearFactor(double ppi) {
    return 0.85 + 0.30 * Math.clamp(ppi, 0.0, 1.0);
}

public static double mobHealth(
        double baseHealth,
        int playerLevel,
        double ppi
) {
    double level = BalanceModel.levelPower(playerLevel);
    return baseHealth * (1.0 + 1.80 * level) * gearFactor(ppi);
}

public static double mobDamage(
        double baseDamage,
        int playerLevel,
        double ppi
) {
    double level = BalanceModel.levelPower(playerLevel);
    return baseDamage
            * (1.0 + 1.20 * level)
            * (0.90 + 0.20 * Math.clamp(ppi, 0.0, 1.0));
}
```

---

# 46. Implementation Responsibilities

## BalanceModel

Verantwortlich für:

```text
Level curve
Rarity multiplier
Slot weight
Stat cost
Hard caps
Budget validation
```

## RPGItemBuilder

Verantwortlich für:

```text
Stat selection
Budget allocation
Roll quality
PDC serialization
Lore
```

Nicht verantwortlich für Character Caps.

## StatEngine

Verantwortlich für:

```text
Registration gate
Level gate
Equipped items
Set bonuses
Companion passives
Character aggregation
Global hard caps
Minecraft attribute bridge
CachedStats
```

## PlayerPowerIndex

Verantwortlich für:

```text
active CachedStats → PPI
```

Nicht für Item-Level.

## MobLevelScalingListener

Verantwortlich für:

```text
participants
Group PPI
Mob scaling
Timeout
Restore
```

Nicht für die Definition der Stat-Caps.

---

# 47. Determinismus

Das Item-System soll zufällige Items erzeugen, aber die Balanceprüfung muss deterministisch sein.

Jedes Item benötigt weiterhin eine stabile Instance-ID.

Für Debugging sollte ein Item intern nachvollziehbar machen können:

```text
itemLevel
rarity
slot
selectedStats
budget
budgetUsed
rollQuality
finalValues
```

Damit kann ein fehlerhaftes Item exakt reproduziert werden.

---

# 48. Balance Test Matrix

Vor einer Freigabe müssen mindestens folgende Szenarien simuliert werden:

### Spieler

```text
Level 1 naked
Level 1 starter gear
Level 10 average
Level 30 average
Level 50 average
Level 70 average
Level 80 strong
Level 90 strong
Level 99 BIS
```

### Builds

```text
DPS
Crit
Tank
Balanced
Lifesteal
Reach/Utility
```

### Mobs

```text
Normal
Bruiser
Glass Cannon
Tank
Elite
Boss
```

### Prüfgrößen

```text
Average Hit
Crit Hit
Expected Hit
DPS
TTK
Incoming DPS
DTK
Lifesteal per second
PPI
Mob HP
Mob Damage
```

---

# 49. Balance Acceptance Criteria

Die neue Balance gilt erst als akzeptiert, wenn alle folgenden Regeln erfüllt sind:

- [ ] Level 1–99 ist monoton und ohne Sprungexplosion.
- [ ] Level 99 erreicht exakt Power 1.00.
- [ ] Kein Item überschreitet sein Stat-Budget.
- [ ] Kein normales Item besitzt mehr als 5 Stat-Linien.
- [ ] Kein normales Item besitzt alle 8 Kernstats.
- [ ] Kein registrierter Spieler überschreitet 200 RPG HP.
- [ ] Kein registrierter Spieler überschreitet `minecraft:max_health = 40.0`.
- [ ] 100 RPG HP entsprechen exakt 20.0 Minecraft max_health.
- [ ] 200 RPG HP entsprechen exakt 40.0 Minecraft max_health.
- [ ] 20 Armor werden niemals überschritten.
- [ ] +30 % Movement werden niemals überschritten.
- [ ] Reach 5.0 wird niemals überschritten.
- [ ] Crit Chance 100 % wird niemals überschritten.
- [ ] Crit Damage Bonus 100 % wird niemals überschritten.
- [ ] Lifesteal 8 % wird niemals überschritten.
- [ ] Attack Power 15 wird niemals überschritten.
- [ ] Level-Gated Items liefern unter Required Level keine Stats.
- [ ] PPI verwendet ausschließlich aktive Stats.
- [ ] Mob Scaling verwendet PPI, nicht Item-Level als Gear Proxy.
- [ ] Besseres Gear führt netto zu besserer Kampfkraft.
- [ ] TTK sinkt bei besserem Gear.
- [ ] DTK steigt bei besserem Gear.
- [ ] Ein Tank kann länger überleben, ohne DPS automatisch zu maximieren.
- [ ] Ein DPS-Build kann schneller töten, ohne unsterblich zu werden.
- [ ] Lifesteal erzeugt keine Vollheilungs-Schleife.
- [ ] Set- und Companion-Boni können Caps nicht umgehen.

---

# 50. Migration von Foundation v1

Die vorhandene Foundation-Implementierung darf nicht mit der neuen Mathematik vermischt werden.

Migration:

```text
FOUNDATION v1
     ↓
forensisch erhalten
     ↓
NEW BALANCE v2
```

Nicht:

```text
v1 Werte einzeln weiterdrehen
```

Folgende Runtime-Werte müssen durch das neue zentrale Modell ersetzt werden:

```text
MAX_HP_BONUS
MAX_ARMOR
MAX_REACH_BONUS
MAX_ATTACK_POWER
growthMultiplier
Rarity budgets
Stat line limits
Roll quality
PPI weights
Mob gear multiplier
```

Die übrige technische Pipeline soll möglichst erhalten bleiben, sofern sie die neuen mathematischen Einheiten korrekt transportieren kann.

---

# 51. Verbindliche Designentscheidungen

## D-001

**PixelRPG HP ist nicht Minecraft HP.**

## D-002

**100 RPG HP = 10 Herzen.**

## D-003

**200 RPG HP = 20 Herzen und ist das absolute normale Endgame-Cap.**

## D-004

**Minecraft `max_health` darf für normale PixelRPG-Spieler niemals über 40.0 steigen.**

## D-005

**Armor ist maximal 20 Minecraft Armor-Punkte.**

## D-006

**Ein Item besitzt ein Gesamtbudget.**

## D-007

**Ein Legendary/Unique besitzt maximal 4–5 Stat-Linien.**

## D-008

**Kein Item besitzt automatisch alle acht Stats.**

## D-009

**PPI wird aus tatsächlich aktiven Character Stats berechnet.**

## D-010

**Monster dürfen Gear-Fortschritt nicht vollständig neutralisieren.**

## D-011

**Level bestimmt Progressionszugang; Gear bestimmt Kampfkraft.**

## D-012

**Alle Hard Caps werden zentral nach der Aggregation erzwungen.**

---

# 52. Finales Modell in einer Übersicht

```text
                         PLAYER
                           │
                    Registered Gate
                           │
                           ▼
                    Level 1–99
                           │
                           ▼
                  Level Power Curve
                           │
                           ▼
                     EQUIPMENT
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
           Rarity                   Slot Weight
              │                         │
              └────────────┬────────────┘
                           ▼
                      ITEM BUDGET
                           │
                           ▼
                   4–5 STAT LINES
                           │
                           ▼
                   STAT ALLOCATION
                           │
                           ▼
                    ROLL QUALITY
                           │
                           ▼
                     ITEM VALUES
                           │
                           ▼
                  LEVEL-GATE CHECK
                           │
                           ▼
                  CHARACTER AGGREGATOR
                           │
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
         Items          Sets         Companion
            └──────────────┼──────────────┘
                           ▼
                     HARD CAP LAYER
                           │
                           ▼
                    CharacterStats
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
       Minecraft Bridge                 PPI
              │                         │
              ▼                         ▼
      Vanilla Attributes         Encounter Scaling
                                      │
                                      ▼
                                  Mob HP/Damage
                                      │
                                      ▼
                              TTK / DTK Validation
```

---

# 53. Schlussdefinition

PixelRPG ist kein Vanilla-Stat-System mit großen Zahlen. Es ist ein RPG-System, das Minecraft als technische Präsentations- und Simulationsplattform verwendet.

Deshalb ist die wichtigste mathematische Trennung:

```text
PIXELRPG-WELT

100 HP = 10 Herzen
200 HP = 20 Herzen

        ↓ Conversion

MINECRAFT-WELT

20.0 max_health = 10 Herzen
40.0 max_health = 20 Herzen
```

Die alte Spezifikation hat diese beiden Ebenen vermischt. Genau daraus entstanden die überhöhten HP- und Armor-Werte.

Die neue Spezifikation korrigiert das Grundproblem nicht durch einen einzelnen Zahlenwechsel, sondern durch eine klare Einheitentrennung, ein gemeinsames Item-Budget, begrenzte Stat-Linien, gewichtete Kampfkraft und PPI-basiertes Encounter Scaling.

**Dieses Dokument ist ab jetzt die alleinige mathematische Balance-Referenz für die weitere PixelRPG-Balance-Implementierung.**
