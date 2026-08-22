# PixelRPG – Balancing Audit & Master Baseline

> Verbindliche Arbeitsgrundlage für die Balance von Spieler, Klassen, Attribute, Items, Gegner, Bosse, Quests, Begleiter, XP und Wirtschaft.
>
> Analysebasis: aktueller `main`-Stand, Paper 26.2 / Java 25. Dieses Dokument verändert noch keine Balancewerte im Code. Es hält die Analyse, die erkannten Brüche und die Zielwerte fest, damit die anschließende Umsetzung nicht wieder gegeneinander arbeitet.

---

# 1. Kurzfazit

Das Plugin besitzt inzwischen die notwendigen Systeme, aber die Werte stammen noch aus mehreren Testphasen und sind **nicht als ein gemeinsames 1–99-Balance-Modell gebaut**.

Die größten Probleme sind aktuell:

1. **Spielerattribute skalieren mehrfach.** Klassenformeln und globale Attributwerte wirken teilweise gleichzeitig.
2. **Rüstung kann extrem schnell in die Vanilla-Schadensreduktion laufen.** Vier starke Rüstungsteile + Toughness können bereits sehr früh überproportional werden.
3. **Crit-Chance kann insbesondere beim Rogue extrem hoch werden.**
4. **Itemwerte sind aktuell zufällig (`0.85–1.15`) statt deterministisch.** Das widerspricht dem geplanten festen Item-Balance-System.
5. **Item-Rarity-Multiplikatoren sind zu groß für eine kontrollierte 1–99-Skalierung.** Legendary = `3.0` und Unique = `4.0` erzeugen enorme Sprünge.
6. **Die Item-Level-Skalierung `1 + level * 0.075` ist zu aggressiv, wenn sie mit Rarity und Klassenboni kombiniert wird.**
7. **Normale Mobs skalieren derzeit deutlich anders als Bosse.** Normale Mobs benutzen eine Levelkurve; Bosse multiplizieren aktuell nur ihre Vanilla-Basiswerte.
8. **Mob-Schaden `1.1 pro Level` ist für Level 80–99 zu hoch.**
9. **Mob-HP `8 pro Level` plus `1.15` Parity ist brauchbar, aber noch nicht an Item-DPS und Boss-TTK gekoppelt.
10. **Quest-XP skaliert zu wenig mit dem Spielerlevel.** Bei hohen Levels werden die vorhandenen Questbelohnungen im Verhältnis zur benötigten XP fast bedeutungslos.
11. **Die aktuelle Spieler-XP-Kurve ist bis Level 79 stark WotLK-inspiriert, explodiert aber durch die 80–99-Fortsetzung viel zu stark.**
12. **Companion-XP ist zu teuer für 1–99.** `100 + Level * 50` ergibt ungefähr 257.400 XP bis Level 99, während aktuell nur 25 XP pro Mob und 100 XP pro Quest vergeben werden.
13. **Die Rarity-XP-Multiplikatoren der Companion-JSON werden derzeit nicht in der XP-Vergabe verwendet.**
14. **Companion-Powerwerte sind bereits konfiguriert, aber der aktive Companion-Kampf existiert bewusst noch nicht.** Deshalb darf jetzt noch keine finale Kampfbalance behauptet werden.
15. **Mana besitzt bereits Klassenbasen, aber noch kein endgültiges Skill-/Kostenmodell.** Ohne Skillkosten ist Mana nicht endgültig balancierbar.
16. **Klassen-Set-Boni addieren bereits +8 % und +15 % Schaden/Heilung sowie einen 20-%-Proc.** Das muss in das Gesamt-DPS-Budget eingerechnet werden.
17. **Ökonomie und Attributkosten passen noch nicht zusammen.** `1.40` Kostenmultiplikator explodiert extrem schnell.

Die richtige Reihenfolge ist deshalb:

```text
Levelkurve
 ↓
Spieler-Basiswerte
 ↓
Attribute + Klassen
 ↓
Item-Level / Item-Rarity
 ↓
Spieler-DPS / EHP
 ↓
Mob-Level / Mob-HP / Mob-Damage
 ↓
Quest-XP / Quest-Geld
 ↓
Companion-XP / Companion-Power
 ↓
Boss-HP / Boss-Damage / Boss-Pattern
 ↓
Skills / Mana / Item-Abilities
```

---

# 2. Zielgefühl

PixelRPG soll **nicht** versuchen, WoW numerisch zu kopieren. WotLK dient als Referenz für die Progressionslogik:

- klare Levelbereiche
- erkennbare Gebiete
- Items ersetzen sich schrittweise
- Questbelohnungen fühlen sich relevant an
- normale Gegner sind gefährlich, aber nicht absurd
- Elite/Boss-Gegner verändern das Kampfgefühl
- Raritäten sind spürbar, aber keine zehnfachen Multiplikatoren
- Klassen haben erkennbare Stärken und Schwächen
- ein Companion unterstützt, ersetzt aber niemals den Spieler

WotLK verwendet für seine XP-Kurve ebenfalls stark unterschiedliche Levelbereiche; insbesondere 60–69 und 70–79 haben deutlich andere XP-Bedarfe. Die aktuelle PixelRPG-Tabelle übernimmt diese Struktur bereits weitgehend. citeturn0search0turn0search1

---

# 3. Spieler-Level 1–99

## Aktueller Stand

Die normale Progression ist Level 1–99, Level 100 bleibt reserviert.

Die XP-Tabelle verwendet bis Level 79 eine WotLK-artige Tabelle. Bis Level 79 ist das grundsätzlich eine gute Referenz.

Problematisch ist die Verlängerung danach:

```text
80–99:
geometrische Steigerung bis ungefähr 100× des 80→81-Increments
```

Dadurch landet die aktuelle kumulierte XP bis Level 99 ungefähr bei **758 Millionen XP**.

Das ist für die geplante Open-World-Queststruktur zu hoch.

Zum Vergleich: die WotLK-Tabelle liegt für die ursprüngliche 1–80-Progression bei ungefähr 26,1 Millionen XP. citeturn0search1

## Ziel

Wir behalten die WotLK-Form als Referenz, aber verlängern sie moderat:

```text
1–59      WotLK-nahe
60–69     WotLK-nahe
70–79     WotLK-nahe
80–99     neue PixelRPG-Endprogression
```

Empfohlener Zielbereich:

```text
Level 79 ≈ 24 Mio. XP
Level 99 ≈ 65–75 Mio. XP
```

Damit bleibt 80–99 deutlich länger als normale Levelbereiche, ohne 758 Mio. XP zu verlangen.

**Empfohlene 80–99-Form:**

- Startwert ungefähr 1,7 Mio. XP pro Level
- jährliche/levelweise Steigerung nur ca. 3–4 %
- kein 100×-Sprung
- Level 99 darf langsam werden
- Level 100 bleibt bewusst absurd teuer / Grind

---

# 4. Attribute – aktueller Bruch

Aktuell werden globale Punktwerte und Klassenformeln gleichzeitig verwendet.

Beispiel Toughness:

```text
globale AttributeConfig:
+2 Armor / Punkt

Warrior ClassBalance:
+1.8 Armor / Toughness

tatsächlich:
+3.8 Armor / Toughness
```

Vitality beim Warrior:

```text
+4 HP global
+1.2 HP aus Klasse
= +5.2 HP / Punkt
```

Das ist zu viel, wenn Spieler viele Punkte investieren können.

Ranger und Rogue erhalten zusätzlich sehr starke Crit-Skalierungen.

## Zielmodell

Attribute sollten grundsätzlich **einmal** skalieren.

Empfohlene globale Basiswerte:

| Attribut | Zielwirkung pro Punkt |
|---|---:|
| Vitality | +2.0 Max HP |
| Agility | +0.25 % Bewegungsgeschwindigkeit |
| Agility | +0.20 % Crit Chance |
| Precision | +0.75 Attack/Spell Power |
| Toughness | +1.0 Armor |
| Range | +0.10 Block Reach |
| Range | +0.10 Entity Reach |

Klassen geben anschließend **kleine Identitätsboni**, nicht noch einmal dieselbe Punkt-Skalierung.

---

# 5. Klassen – Zielidentität

Die Klassen sollen nicht durch rohe Multiplikatoren gewinnen, sondern durch unterschiedliche Stat-Budgets.

## Warrior

Stärken:

- HP
- Armor
- Melee

Schwächen:

- geringe Reichweite
- geringere Spell-/Ranged-Effizienz

Zielbasis:

```text
+8–12 HP
+4–6 Armor
+5 % Melee Damage
```

## Ranger

Stärken:

- Ranged Damage
- Movement
- moderater Crit

Zielbasis:

```text
+5 % Ranged Damage
+2–3 % Movement
+3–5 % Crit
```

## Rogue

Stärken:

- Crit
- Burst
- Movement

Schwächen:

- niedrigere Defensive

Zielbasis:

```text
+5 % Crit
+10–15 % Crit Damage
+2 % Movement
```

Crit darf dabei nicht schon durch Attribute bei 70–100 % landen.

## Healer

Stärken:

- Heilung
- Sustain
- hohe Lebensbasis

Zielbasis:

```text
+8–12 HP
+15–20 % Healing
```

## Mage

Stärken:

- Spell Power
- Mana
- Burst

Schwächen:

- Armor
- Melee

Zielbasis:

```text
+15–20 % Spell Power
+30–50 Max Mana
```

---

# 6. Crit – harte Obergrenze

Aktuell kann der Rogue durch Klassenformel + globale Agility/Precision-Werte sehr schnell in extreme Crit-Bereiche kommen.

Das ist für ein RPG mit Items, Begleitern und Skills gefährlich.

Ziel:

```text
Level 1 ohne Ausrüstung: ~5 %
Level 50 gut ausgebaut: ~15–25 %
Level 99 stark ausgebaut: ~25–40 %
```

Nur besondere Items/Skills dürfen kurzfristig darüber gehen.

Empfohlene Hardcaps:

```text
Crit Chance: 50 %
Crit Damage: 250 %
```

Ein 100-%-Crit-Build soll nicht entstehen.

---

# 7. HP / EHP

Die aktuelle Grundidee mit Minecraft-internen HP bleibt sinnvoll.

Wichtig:

```text
Darstellung ≠ interne RPG-Balance
```

Ein Spieler kann intern deutlich mehr als 20 HP besitzen, während die Vanilla-UI bzw. die geplante PixelRPG-Anzeige eine verständliche Zahl zeigt.

Zielbereiche für einen normal ausgerüsteten Spieler:

| Level | typisches HP-Ziel |
|---:|---:|
| 1 | 20–25 |
| 20 | 30–45 |
| 40 | 45–65 |
| 60 | 60–90 |
| 80 | 80–120 |
| 99 | 100–160 |

Tank-Builds dürfen darüber liegen.

---

# 8. Rüstung – aktuell zu gefährlich

Die aktuelle Item-Rüstung wird als Vanilla `Attribute.ARMOR` gesetzt.

Vier Rüstungsteile besitzen jeweils dieselbe Grundformel. Bei hohen Itemlevels und Legendary-Multiplikator `3.0` entstehen dadurch sehr hohe Armorwerte.

Zusätzlich kommt Toughness dazu.

Das ist gefährlich, weil Minecraft die Armor selbst in die Schadensreduktion einbezieht.

## Ziel

Armor soll bei einem gut ausgerüsteten Level-99-Spieler ungefähr folgende Größenordnung erreichen:

```text
leichter Build: 25–40 Armor
mittlerer Build: 40–55 Armor
Tank-Build: 55–70 Armor
```

Wir wollen nicht dauerhaft am Vanilla-Maximum kleben.

Die Items sollten deshalb deutlich kleinere Armorwerte erhalten als aktuell.

---

# 9. Items – aktueller Stand

`RPGItemBuilder` berechnet aktuell:

```text
levelFactor = 1 + itemLevel * 0.075
roll = 0.85–1.15
```

Waffe:

```text
(2 + levelFactor * 1.8) × Rarity × Roll
```

Armor:

```text
(1.5 + levelFactor * 1.2) × Rarity × Roll
```

Health:

```text
(1 + levelFactor * 0.9) × Rarity × Roll
```

Das führt bei Level 99 ungefähr zu:

```text
Weapon base: 17.2
Armor base: 11.6 pro Stück
Health base: 8.6 pro Stück
```

Vor dem zufälligen Roll und vor Rarity.

Mit Legendary ×3 wird daraus ungefähr:

```text
Weapon: 43.8–59.2
Armor: 29.6–40.0 pro Stück
Health: 21.9–29.6 pro Stück
```

Das ist für das Gesamtmodell zu hoch.

## Entscheidung

Der Zufallsroll `0.85–1.15` wird für das zukünftige Kern-Item-System entfernt.

Items sollen feste Werte besitzen:

```text
itemId
itemLevel
rarity
slot
baseStats
scaling
abilities
growth
requirements
```

Das ermöglicht reproduzierbare Balance.

---

# 10. Item-Level-Skalierung – Ziel

Die aktuelle lineare `+7.5 % pro Level` ist zu aggressiv.

Empfehlung:

```text
Basis × moderates Wachstum
```

Ziel:

```text
Level 1 → 99
ungefähr 8–12× Stat-Wachstum
```

Eine exponentielle Formel mit ungefähr 2.5 % Wachstum pro Level liegt in diesem Bereich.

Die genaue Formel wird anschließend in `item-scaling.json` festgeschrieben.

---

# 11. Rarity – Zielwerte

Aktuell:

```text
Common      1.00
Uncommon    1.15
Rare        1.40
Epic        2.00
Legendary   3.00
Unique      4.00
```

Diese Abstände sind für ein System mit Level 1–99 zu groß.

Empfohlene Zielmultiplikatoren:

```text
Common      1.00
Uncommon    1.10
Rare        1.22
Epic        1.38
Legendary   1.60
Unique      individuell
```

Unique darf nicht automatisch „4× stärker“ bedeuten.

Unique bedeutet:

```text
einzigartig
admin-only
individuelle Werte
individuelle Fähigkeiten
```

---

# 12. Item-Stat-Budget

Jedes Item bekommt künftig ein Budget statt einer pauschalen Multiplikator-Explosion.

Beispiel Level 99:

| Itemtyp | Common | Rare | Epic | Legendary |
|---|---:|---:|---:|---:|
| Waffe Bonus Damage | 25–30 | 31–37 | 35–42 | 42–50 |
| Armor pro Stück | 6–8 | 8–10 | 10–13 | 12–16 |
| HP pro Rüstungsteil | 7–10 | 9–12 | 11–15 | 14–20 |

Das sind **Zielbereiche**, noch keine endgültigen Einzelitems.

---

# 13. Waffen-DPS

Die Vanilla-Waffenschadensbasis bleibt Teil des Grundschadens.

PixelRPG-Bonusdamage wird darübergelegt.

Ziel für Level 99:

```text
gewöhnliche gute Waffe:
~30–45 effektiver Grundschaden vor Klassen-/Crit-Boni

sehr starke Legendary-Waffe:
~45–60
```

Damit braucht ein Spieler gegen normale gleichlevelige Gegner mehrere Treffer.

Ein Boss darf nicht in 2 Hits verschwinden.

---

# 14. Gegner – aktueller Stand

Aktuelle Formel:

```text
HP = (20 + Level × 8) × Dimension × 1.15
Damage = (2 + Level × 1.1) × Dimension × 1.15
```

Das ergibt ungefähr:

| Level | Overworld HP | Overworld Damage |
|---:|---:|---:|
| 1 | 32 | 3.6 |
| 20 | 207 | 27.6 |
| 40 | 391 | 52.9 |
| 60 | 575 | 78.2 |
| 80 | 759 | 103.5 |
| 99 | 934 | 127.5 |

Der HP-Wert ist grundsätzlich verwendbar.

Der Schaden ist zu hoch.

## Zielmodell

Erster Balancing-Kandidat:

```text
HP = 20 + Level × 8
Damage = 2 + Level × 0.65–0.75
```

Dimensionen geben anschließend zusätzliche Gefahr:

```text
Overworld: ×1.00
Nether:    HP ×1.20 / Damage ×1.15
The End:   HP ×1.35 / Damage ×1.25
```

Die Dimension darf schwieriger sein, aber nicht einfach nur „alles hat 60 % mehr Werte“.

---

# 15. Gegner-TTK

Ziel für normale Gegner:

```text
gleiches Level:
ca. 5–10 Spielerhits
```

Stärkere Varianten:

```text
Elite:
ca. 10–20 Hits
```

Boss:

```text
mehrere Phasen
mehrere Dutzend effektive Spieleraktionen
```

Die genaue Zahl hängt von Klasse, Waffe, Crit und Companion ab.

---

# 16. Normale Gegner müssen nicht alle gleich stark sein

Das zukünftige Mob-Modell sollte einen zusätzlichen Rollenfaktor besitzen:

```text
MINION
NORMAL
BRUISER
TANK
RANGED
ELITE
```

Beispiel:

```text
Zombie NORMAL
Skeleton RANGED
Spider FAST
Husk BRUISER
Iron Golem TANK
```

Damit muss nicht jeder Mob eigene komplizierte Java-Logik besitzen.

---

# 17. Bosse – aktueller Bruch

Das ist aktuell einer der wichtigsten Balancefehler.

Normale Mobs werden anhand ihres Levels skaliert.

Bosse werden dagegen aktuell ungefähr so erzeugt:

```text
Vanilla Max HP × Boss Health Multiplier
Vanilla Attack Damage × Boss Damage Multiplier
```

Beispiel:

```text
Level 99 Wither Skeleton
Health Multiplier 6.0
```

Der Boss wird damit nicht automatisch zu einem echten Level-99-Gegner.

Er erhält nur einen Multiplikator auf seine Vanilla-Basiswerte.

Das muss geändert werden.

## Ziel

Boss:

```text
Boss-Level
 ↓
Level-99-Mob-Basiswerte
 ↓
Boss-Tier-Multiplikator
 ↓
Phasenmodifikatoren
```

Damit ist garantiert:

```text
Boss > Elite > Normalmob
```

und nicht:

```text
Vanilla Boss ×6
```

---

# 18. Boss-Tiers

Empfohlene interne Tiers:

```text
World Boss Tier 1
World Boss Tier 2
World Boss Tier 3
Legendary World Boss
```

Ziel-HP relativ zum gleichleveligen normalen Mob:

```text
Tier 1: 4–6×
Tier 2: 6–9×
Tier 3: 9–12×
Legendary: 12–18×
```

Nicht jeder Boss muss Tier 4 sein.

---

# 19. Boss-Schaden

Boss-Schaden darf niemals nur durch rohe Zahlen gefährlich werden.

Ein Boss soll gefährlich sein durch:

- Pattern
- Telegraphing
- Positionierung
- Adds
- Phasen
- Timing
- kleine Fehlerfenster

Ein einzelner normaler Treffer darf einen gut ausgerüsteten Spieler nicht einfach zufällig löschen.

Der aktuelle `boss-max-hit-percent-of-max-hp = 0.12` ist ein sinnvoller Sicherheitsmechanismus, muss aber später mit den tatsächlichen Boss-Skills abgeglichen werden.

Ziel für normalen Bosskontakt:

```text
normaler Treffer:
~5–12 % Spieler-Max-HP

schwerer telegraphierter Treffer:
~15–25 %

ultimative Mechanik:
>25 % nur bei klarer Warnung / vermeidbarer Mechanik
```

---

# 20. Boss-Phasen

Die vorhandenen Phasen sind strukturell gut.

Aktuelle Beispiele:

```text
100 %
66 %
33 %
```

bzw.

```text
100 %
50 %
20 %
```

Das ist sinnvoll.

Die bisherigen Testangriffe wie `SLAM`, `SUMMON_ADDS`, `ENRAGE_BUFF` und `PROJECTILE_VOLLEY` müssen später pro Boss individuell dosiert werden.

Ein Boss soll nicht einfach alle vorhandenen Pattern gleichzeitig verwenden.

---

# 21. Boss-Pool

Die geplante `Compboss.md` bleibt die Designquelle für:

- Boss
- Angriffe
- Companion
- Reittier

Die aktuelle technische `bosses.yml`-Architektur ist bereits geeignet, aber die Definitionen müssen später von generischen Testbossen auf den geplanten vollständigen Boss-Pool erweitert werden.

---

# 22. Companion – aktueller Stand

Companions haben bereits:

- Level 1–99
- XP
- Persistenz
- Rarity
- aktive Auswahl
- Folgen
- Name
- Größenkonzept
- passive Phase

Das Fundament ist gut.

Kampfbalance ist bewusst noch nicht aktiv.

Das ist richtig und sollte so bleiben.

---

# 23. Companion-XP – aktueller Bruch

Aktuell:

```text
XP für Level N → N+1
= 100 + N × 50
```

Von Level 1 bis 99 ergeben sich ungefähr **257.400 XP**.

Aktuelle Quellen:

```text
Mob-Kill: 25 XP
Quest:    100 XP
```

Damit würde ein Companion allein über Mob-Kills mehr als **10.000 Kills** benötigen.

Das ist für einen normalen Companion nicht sinnvoll.

## Ziel

Zielbereich bis Level 99:

```text
Common:      ~45.000–55.000 XP
Uncommon:    ~50.000–60.000 XP
Rare:        ~55.000–65.000 XP
Epic:        ~60.000–70.000 XP
Legendary:   ~70.000–85.000 XP
```

Die höhere XP-Anforderung ist ein Teil der Seltenheit, aber sie darf nicht absurd werden.

---

# 24. Companion-XP-Multiplikatoren

In `companions.json` existieren bereits `xpMultiplier`-Werte.

Aktuell wird die XP-Vergabe jedoch direkt über feste `mobKill`-/`questCompletion`-Werte durchgeführt.

Das bedeutet:

```text
xpMultiplier vorhanden
aber nicht Teil der tatsächlichen Vergabe
```

Das muss bei der späteren Umsetzung korrigiert werden.

---

# 25. Companion-Power

Grundregel:

```text
Spieler = 100 %
Companion = Unterstützung
```

Wenn später Kampffähigkeiten aktiviert werden:

| Rarity | Zielanteil am Spieler-DPS |
|---|---:|
| Common | 5–7 % |
| Uncommon | 7–9 % |
| Rare | 9–12 % |
| Epic | 12–15 % |
| Legendary | 15–20 % |
| Unique | individuell |

Das ist **nicht** „Companion macht 20 % des gesamten Spielerschadens in jeder Situation“.

Ein Support-Companion kann stattdessen weniger Damage und mehr Utility geben.

---

# 26. Companion-Spezialisierung

Die geplanten Mobs sollen unterschiedliche Rollen bekommen.

Beispiele:

```text
Bee       → Utility / leichter DoT
Wolf      → offensiver Nahkampf
Turtle    → Defensive
Fox       → Utility
Skeleton  → Ranged
Blaze     → Magic/Ranged
Shulker   → Control
Warden    → Legendary Control/Tank
```

Damit müssen wir nicht jeden Companion über rohe Damagewerte unterscheiden.

---

# 27. Companion-Level-Skalierung

Aktuell existieren bereits:

```text
healthPerLevel = 0.008
 damagePerLevel = 0.006
speedPerLevel = 0.0015
```

mit Caps:

```text
Health ×1.80
Damage ×1.60
Speed ×1.15
```

Das ist als reine technische Wachstumsform brauchbar.

Für das echte Kampfsystem sollte aber später gelten:

```text
Companion-Basiswert
 × Rarity
 × LevelGrowth
 × RoleModifier
```

nicht:

```text
Entity-Vanilla-Werte × alles
```

Ein Warden darf nicht automatisch absurd stark sein, nur weil die Vanilla-Entity absurd starke Werte besitzt.

---

# 28. Companion-Größe

Die Trennung ist richtig:

```text
Scale ≠ Power
```

Beispiele:

```text
Mini-Warden: 0.50
Große Biene: 2.00
Kleiner Ravager: 0.55
```

Die Größe darf ausschließlich visuell bzw. mechanisch bewusst definiert sein.

---

# 29. Quest-XP – aktueller Bruch

Aktuelle Beispiele:

```text
Level 20 Quest: 300 XP
Level 45 Quest: 900 XP
Level 50 Quest: 1200 XP
Level 60 Quest: 1800 XP
Level 85 Quest: 5000 XP
Level 90 Quest: 6000 XP
Level 93 Quest: 7000 XP
```

Das Verhältnis zur benötigten Spieler-XP fällt stark ab.

Beispiel:

```text
Level 20 → nächstes Level: ~20.800 XP
Quest: 300 XP
≈ 1.4 %
```

Das ist noch okay.

Später:

```text
Level 80+:
Quest-XP nur noch ein Bruchteil eines Prozents
```

Das zerstört die gefühlte Relevanz von Quests.

## Ziel

Quest-XP soll pro Quest ungefähr relativ zum nächsten Level berechnet werden.

Empfehlung:

```text
kleine Nebenquest:      0.5–1.0 %
normale Quest:          1.0–2.0 %
Storyquest:             2.0–3.5 %
großes Finale/Event:    3.5–6.0 %
```

Die Summe mehrerer Quests innerhalb eines Levelbereichs darf einen erheblichen Teil der Level-XP liefern.

---

# 30. Quest-Anzahl

Das Limit von 5 aktiven Quests bleibt bestehen.

Das bedeutet nicht, dass nur 5 Quests existieren.

Für die spätere Levelprogression sollte ungefähr gelten:

```text
pro 5 Level:
4–8 relevante Quests
```

Damit ergeben sich langfristig mehrere hundert echte Quests.

Wichtig:

```text
Quest-Level ≠ Quest-Anzahl
```

Eine Questkette darf mehrere Level begleiten.

---

# 31. Quest-Freischaltung

Der bestehende Wert:

```text
recommendedLevel - 5
```

bleibt sinnvoll.

Er sorgt dafür, dass Spieler eine Quest nicht erst exakt auf dem vorgesehenen Level sehen.

Aber:

```text
früh freigeschaltet
≠ automatisch leicht
```

Die tatsächliche Schwierigkeit muss zum empfohlenen Level passen.

---

# 32. Quest-Belohnungen

Belohnungen müssen in drei Budgets geteilt werden:

```text
XP
Geld
besondere Belohnung
```

Ein Companion darf nicht zusätzlich zu einer maximalen XP-, Geld- und Item-Belohnung einfach gratis oben drauf kommen, wenn der Companion selbst bereits sehr wertvoll ist.

Legendary Companion als Questreward bedeutet:

```text
Quest selbst darf geringer monetär sein
Companion ist die Hauptbelohnung
```

---

# 33. Quest → Companion Progression

Geplante Struktur:

```text
Level 1–20:
Common / erste Uncommon

Level 20–40:
Uncommon / erste Rare

Level 40–60:
Rare / erste Epic

Level 60–80:
Epic / sehr selten Legendary-Vorbereitung

Level 80–99:
Legendary
```

Das ist besser als „Legendary einfach aus einem normalen Mob-Drop“.

---

# 34. Economy

Aktuelle Quest-Geldwerte steigen grundsätzlich sinnvoll an, müssen aber mit Itemkosten, Attributkosten und Loot gekoppelt werden.

Besonders problematisch ist:

```text
Attribute cost multiplier = 1.40
```

Beispiele:

```text
1. Punkt: ~30
10. Punkt: ~620
20. Punkt: ~17.900
30. Punkt: ~518.000
```

Das explodiert.

## Ziel

Empfehlung:

```text
Kostenmultiplikator ungefähr 1.06–1.10
```

oder ein Softcap-Modell.

Attributkosten sollen teuer werden, aber nicht mathematisch unbrauchbar.

---

# 35. Mana

Aktuell existieren Klassenbasen:

```text
Warrior 130
Rogue   150
Ranger  160
Healer  180
Mage    200
```

bei Basis-Intellect 10.

Das ist als Ausgangspunkt brauchbar.

## Ziel

Mana soll eine Ressource sein, kein zweiter HP-Balken.

Richtwerte für spätere Skills:

```text
kleine Fähigkeit:       5–15 Mana
normale Fähigkeit:     15–30 Mana
starke Fähigkeit:      30–60 Mana
Ultimate:              60–120 Mana
```

Die genaue Kostenkurve wird erst festgelegt, wenn Skills/Abilities vollständig definiert sind.

---

# 36. Class Set Bonus

Aktuell:

```text
2 Teile: +8 % Damage / Healing
4 Teile: zusätzlich +15 %
4 Teile: 20 % Proc
```

Damit sind 4 Teile effektiv bereits:

```text
+23 % Damage
+23 % Healing
```

plus Proc-Schaden.

Das ist stark genug, dass es zwingend in das Item-Budget gehört.

Empfehlung:

```text
Setbonus darf Build definieren,
aber nicht Itemwerte ersetzen.
```

Der Proc muss später in der durchschnittlichen DPS-Berechnung berücksichtigt werden.

---

# 37. WotLK-Referenz für Items

WotLK ist für uns interessant, weil Itemlevel und Qualität die Werte eines Items systematisch strukturieren. Itemlevel ist dort eng mit den Stats und der Qualität verbunden. citeturn0search10

Wir übernehmen daher das Prinzip:

```text
Item Level
 ↓
Stat Budget
 ↓
Rarity / Quality
 ↓
Slot Budget
 ↓
besondere Stats
```

Wir übernehmen nicht die WotLK-Rohzahlen.

---

# 38. WotLK-Referenz für Sekundärstats

WotLK verwendet Rating-Systeme für Crit, Haste, Expertise usw. citeturn0search5

PixelRPG sollte nicht blind dasselbe System kopieren.

Für Minecraft ist ein einfaches Prozent-/Punktesystem sinnvoller.

Aber das Grundprinzip bleibt:

```text
Stats werden mit steigendem Itemlevel teurer
```

Das verhindert, dass ein Level-99-Item mit einem einzelnen 1-%-Stat genauso viel kostet wie ein Level-10-Item.

---

# 39. Gesamt-Balancebudget

Für einen gleichleveligen Spieler soll das Gesamtmodell ungefähr so aussehen:

```text
Spieler alleine:
100 %

Spieler + normale gute Ausrüstung:
~110–130 %

Spieler + sehr starke Ausrüstung:
~130–160 %

Spieler + Legendary Companion:
~145–180 % je nach Rolle
```

Das sind keine direkten Damage-Multiplikatoren, sondern ein Denkmodell für die Gesamtleistung.

Ein Companion darf nicht aus:

```text
100 % Spieler
+ 100 % Companion
= 200 %
```

werden.

---

# 40. Combat-Zielkurve

## Level 1

```text
Spieler:
20–25 HP
3–6 effektiver Grundschaden

Normalmob:
~30 HP
~3 Damage
```

Kämpfe sollen kurz sein.

## Level 20

```text
Spieler:
30–45 HP
~10–20 Damage

Normalmob:
~200 HP
~15–25 Damage
```

Kämpfe dauern länger, ohne gefährlich absurd zu werden.

## Level 50

```text
Spieler:
50–80 HP
~25–40 Damage

Normalmob:
~420 HP
~35–45 Damage
```

## Level 80

```text
Spieler:
80–120 HP
~40–55 Damage

Normalmob:
~660 HP
~55–65 Damage
```

## Level 99

```text
Spieler:
100–160 HP
~45–60 Damage

Normalmob:
~800 HP
~65–75 Damage
```

Die tatsächlichen Werte hängen stark von Waffe, Klasse, Armor und Crit ab.

---

# 41. Was noch NICHT finalisiert werden darf

Folgende Dinge müssen bewusst offen bleiben:

- finale Skillwerte
- finale Skillkosten
- finale Companion-Kampfwerte
- finale Boss-Pattern-Schadenswerte
- endgültige Item-Ability-Skalierung
- finale Heilerwerte
- PvP-Balance
- Level-100-Transcendence
- genaue Raid-/World-Boss-DPS-Anforderungen

Diese Punkte werden erst festgezogen, wenn die Baseline implementiert ist.

---

# 42. Neue Daten-Dateien

Um das System wirklich wartbar zu machen, sollen folgende Dateien entstehen:

```text
data/player-scaling.json
data/attributes.json
data/class-balance.json
data/item-scaling.json
data/items.json
data/item-abilities.json
data/mob-scaling.json
data/mob-types.json
data/companions.json
data/companion-stats.json
data/companion-abilities.json
data/quests.json
data/bosses.json oder bosses.yml
```

Die Dateien sollen klar getrennt sein.

Beispiel:

```text
item-scaling.json
→ allgemeine Formel

items.json
→ konkretes Item

item-abilities.json
→ Fähigkeit
```

Damit kann ein Serverbetreiber Werte ändern, ohne Java-Code zu verändern.

---

# 43. Reihenfolge der eigentlichen Umsetzung

## Schritt 1 – XP

- Spieler-XP 1–99 neu ausbalancieren
- Companion-XP 1–99 neu ausbalancieren
- Quest-XP relativ zum Level definieren

## Schritt 2 – Spieler

- Attribute vereinfachen
- Klassenidentität festlegen
- Crit begrenzen
- HP/EHP definieren
- Mana-Basis definieren

## Schritt 3 – Items

- `items.json`
- `item-scaling.json`
- feste Itemwerte
- keine zufälligen Kernstats
- Slot-Budgets
- Rarity-Budget
- Itemlevel-Anforderungen

## Schritt 4 – Gegner

- Levelbasis
- Mob-Rollen
- HP
- Damage
- Dimensionen
- XP

## Schritt 5 – Boss

- Bosswerte an Mob-Level koppeln
- Boss-Tiers
- HP/Damage-Budget
- Pattern-Damage
- Phasen

## Schritt 6 – Quests

- Quest-XP
- Geld
- Items
- Companion-Rewards
- Levelbereiche 1–99

## Schritt 7 – Companion

- XP
- Level
- Rarity
- Rolle
- Powerbudget
- später Skills

## Schritt 8 – Testmatrix

Jede Stufe testen:

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

Jeweils:

```text
Spieler
+ Common Gear
+ Uncommon Gear
+ Rare Gear
+ Epic Gear
+ Legendary Gear
+ Companion
vs
Normalmob
Elite
Boss
```

---

# 44. Definition of Balance

Die Balance gilt erst als brauchbar, wenn folgende Aussagen gleichzeitig stimmen:

- kein einzelner Stat dominiert alle anderen
- keine Klasse ist dauerhaft objektiv besser
- Rarity fühlt sich relevant an, ohne das Levelsystem zu zerstören
- Level-99-Gear ersetzt Level-80-Gear spürbar, aber nicht um Größenordnungen
- normale Gegner sind besiegbar, aber nicht bedeutungslos
- Bosse benötigen Mechaniken statt nur riesige HP
- Companion ist hilfreich, aber kein zweiter Spieler
- Quests tragen sichtbar zur Progression bei
- Geld reicht für normale Progression, aber nicht für unendliche Attributkäufe
- Mana unterstützt Skillrotationen
- Items und Skills teilen sich ein gemeinsames Statbudget
- Dimensionen erhöhen die Gefahr nachvollziehbar
- Spieler können einen Fehler überleben und daraus lernen
- Endgame darf langsam werden, aber nicht mathematisch absurd

---

# 45. Offene Werte, die vor der finalen Berechnung noch festgelegt werden müssen

Diese Punkte waren im aktuellen Code nicht eindeutig genug, um sie seriös zu raten:

1. Wie viele Attributpunkte erhält ein Spieler tatsächlich pro Level?
2. Werden Attributpunkte gekauft oder automatisch vergeben?
3. Wie viele aktive/passive Skill-Slots soll jede Klasse haben?
4. Welche konkrete Skillrotation ist pro Klasse vorgesehen?
5. Welche Itemslots werden final außer den vier Armor-Slots und Waffen verwendet?
6. Soll Itemlevel exakt dem Spielerlevel entsprechen oder in Sprüngen laufen?
7. Wie viele Items soll ein Spieler durchschnittlich pro 5-Level-Bereich erhalten?
8. Wie häufig sollen Legendary Items tatsächlich erscheinen?
9. Wie viele Quests soll ein Spieler durchschnittlich für einen Levelaufstieg abschließen?
10. Wie viel Prozent der Level-XP soll aus Quests und wie viel aus Combat kommen?

Diese Punkte dürfen nicht geraten werden. Sobald sie entschieden sind, werden sie hier ergänzt und die Zahlenmatrix erneut gerechnet.

---

# 46. Aktueller Status

**Analyse:** abgeschlossen.

**Balance-Baseline:** definiert.

**Codeänderungen:** noch nicht vorgenommen.

**Wichtigste sofortige Korrekturen vor Item-/Skill-Bau:**

```text
1. Spieler-XP 80–99 entschärfen
2. Attribute entdoppeln
3. Crit begrenzen
4. Armorbudget reduzieren
5. Item-Rarity-Multiplikatoren reduzieren
6. zufälligen Item-Roll entfernen
7. Itemwerte datengetrieben machen
8. Mob-Damage reduzieren
9. Bosse an Levelbasis koppeln
10. Quest-XP levelabhängig machen
11. Companion-XP stark reduzieren
12. Companion-Rarity-XP wirklich anwenden
13. Attributkosten entschärfen
14. danach erst Fähigkeiten und Item-Abilities festlegen
```

**Merksatz für die weitere Entwicklung:**

> Wir balancen nicht einzelne Systeme. Wir balancen eine gemeinsame Progressionskurve von Level 1 bis 99.
