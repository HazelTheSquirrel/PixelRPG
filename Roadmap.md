# PixelRPG Roadmap

## Ziel

PixelRPG wird ein modernes RPG-System für Paper 26.2, das **mit Minecraft Vanilla koexistiert**, statt Vanilla zu ersetzen.

Minecraft Vanilla und PixelRPG müssen auf demselben Server und in derselben Welt parallel spielbar sein.

### Oberste Priorität: Spieler-Registrierung und System-Isolation

**Das PixelRPG-System darf erst greifen, nachdem ein Spieler sich ausdrücklich für PixelRPG registriert hat.**

Vor der Registrierung bleibt der Spieler ein normaler Vanilla-Spieler. PixelRPG darf dessen Vanilla-Spielverhalten, Vanilla-Items, Vanilla-Crafting, Vanilla-Mobs, Vanilla-Progression und andere Vanilla-Systeme nicht verändern.

Nach der Registrierung wird PixelRPG für diesen Spieler aktiviert. Die RPG-Systeme gelten dann nur für den registrierten Spieler.

Diese Trennung ist eine zentrale Architekturregel und schützt beide Spielweisen:

- Vanilla-Spieler bleiben vollständig Vanilla.
- Registrierte PixelRPG-Spieler erhalten die RPG-Systeme.
- Beide Spielertypen können gleichzeitig auf demselben Server und in derselben Welt existieren.
- Globale Eingriffe in Vanilla-Systeme sind zu vermeiden, wenn sie nicht zwischen registrierten und nicht registrierten Spielern unterscheiden können.

---

## 1. Technische Basis

- Paper 26.2
- Java 25
- Mojang Mappings
- `paper-plugin.yml`
- aktuelle Paper-26.x-APIs
- native Minecraft/Paper Dialog API
- Adventure Components
- bestehende YAML-/MySQL-Persistenz weiterverwenden
- ausschließlich `main` als Entwicklungsbranch

### Entwicklungsregeln

- Keine neuen Branches ohne ausdrückliche Freigabe.
- Keine neuen Tags ohne ausdrückliche Freigabe.
- `.gradle` bleibt auf Gradle 9.2.0.
- `build.gradle` und `settings.gradle` werden für diese Roadmap nicht verändert.
- Die Entwicklungsarbeit erfolgt grundsätzlich unter `src/*`.

---

## 2. Vanilla-/PixelRPG-Trennung

Das Plugin ist eine optionale RPG-Ebene über einer normalen Minecraft-Welt.

### Vanilla bleibt Vanilla

Vanilla-Systeme sollen weiterhin funktionieren:

- Vanilla Crafting
- Vanilla Furnace/Smelting
- Vanilla Mining
- Vanilla Farming
- Vanilla Villager
- Vanilla Items
- Vanilla Rüstung
- Vanilla Waffen
- Vanilla Mobs
- Vanilla Dimensionen
- Vanilla Exploration
- Vanilla Redstone
- Vanilla Progression

PixelRPG darf diese Systeme nicht pauschal in RPG-Systeme umwandeln.

### PixelRPG-Objekte

PixelRPG-Items müssen eindeutig von Vanilla-Items unterscheidbar sein.

Ein Vanilla Diamond Sword bleibt ein Vanilla Diamond Sword.

Ein PixelRPG-Schwert ist ein eigenes RPG-Item mit eigener Identität, eigenen Stats, Rarität, Item-Level und gegebenenfalls einer Waffenfähigkeit.

---

## 3. Spieler-Registrierung

Die Registrierung ist der Schalter zwischen Vanilla und PixelRPG.

### Registrierung über NPC-Mannequin

Die bereits vorhandenen **Mannequin-NPCs sind die Schnittstellen zur PixelRPG-Welt**. Der Spieler spricht einen PixelRPG-Mannequin an und gelangt über den nativen Dialog zur Registrierung.

Grundprinzip:

```text
PixelRPG-Mannequin
        ↓
Native Dialog
        ↓
Registrierung
        ↓
PlayerProfile aktiv
        ↓
PixelRPG-Systeme greifen
```

Die Registrierung ist der zentrale Einstieg in PixelRPG. Alles weitere baut darauf auf.

### Nicht registriert

- keine PixelRPG-Level-/XP-Progression
- keine PixelRPG-Stats
- keine PixelRPG-Mob-Skalierung für den Spieler
- keine PixelRPG-Loot-Skalierung für den Spieler
- keine PixelRPG-Klassenmechanik
- keine PixelRPG-Quests
- keine PixelRPG-RPG-Items als Pflichtsystem
- Vanilla-Verhalten bleibt erhalten

### Registriert

- PixelRPG-Profil wird aktiviert
- Level-/XP-System greift
- RPG-Stats greifen
- Klassen greifen
- skalierte RPG-Mobs und RPG-Loot können für diesen Spieler aktiv werden
- PixelRPG-Quests/NPCs/Ökonomie können genutzt werden
- PixelRPG-Equipment wird aktiv

---

## 4. NPCs und Dialoge – zentrale Schnittstelle

NPC-Mannequins sind die **Schnittstellen zu den PixelRPG-Systemen**.

Sie sind kein Ersatz für Minecraft-Vanilla-NPCs, sondern bieten registrierten PixelRPG-Spielern den Zugang zu Plugin-Funktionen.

Mögliche Services:

- Registrierung
- Bank
- Berufe lernen
- Quest-Annahme und Abgabe
- Shop
- Blacksmithing
- Crafting
- Reisen
- Story
- weitere PixelRPG-Dienste

### Interaktionsprinzip

Die Standardkette lautet:

```text
NPC-Mannequin
      ↓
Native Minecraft Dialog
      ↓
Spielerauswahl
      ↓
PixelRPG-Aktion
```

**Dialog ist der primäre Einstieg.**

Wenn eine Funktion mit einem Dialog sinnvoll umgesetzt werden kann, wird der native Dialog verwendet.

Wenn eine Funktion eine komplexere Darstellung benötigt, wird ein Inventory-GUI verwendet. Das bereits vorhandene GUI-System dient dabei als Grundlage; es wird kein paralleles UI-System ohne Bedarf gebaut.

Beispiel:

```text
Bankier-Mannequin
      ↓
Dialog
      ↓
[Bank öffnen]
      ↓
Inventory GUI
```

### `/dialogue`

`/dialogue` verwendet dieselbe zentrale Dialog-Engine wie die NPC-Mannequins.

Es darf kein zweites, paralleles Dialogsystem entstehen.

---

## 5. Spielerprofil / G-Taste

Die Minecraft-Interaktion `minecraft:quick_actions` wird **ausschließlich für das eigene PixelRPG-Spielerprofil** verwendet.

Sie ist **nicht** die allgemeine NPC-Interaktion.

Ziel:

```text
G / minecraft:quick_actions
        ↓
eigenes PixelRPG-Profil
        ↓
Charakterkarte
```

Die Spielerkarte kann unter anderem anzeigen:

- Name
- Level
- Klasse
- Health
- Mana
- RPG-Stats
- Ausrüstung
- Berufe
- Quests
- Statistiken
- weitere persönliche PixelRPG-Informationen

Damit sind die beiden Interaktionswege strikt getrennt:

```text
NPC-Mannequin → Native Dialoge → PixelRPG-Dienste

G / quick_actions → eigenes Spielerprofil → Charakterkarte
```

Es wird kein WoW-artiges dauerhaftes MMO-HUD daraus gebaut.

---

## 6. Spieler-Level

- Level 1–99
- Level 100 bleibt gesperrt
- XP-System
- Level-Up-System
- persistentes Spielerprofil
- Level-basierte Stats
- Level-basierte Ausrüstung
- Level-basierte XP-/Loot-Berechnung

Es gibt **kein F–S-Rank-System** mehr.

---

## 7. Stats und Combat

Eine zentrale StatEngine bildet die Grundlage für das RPG-Kampfsystem.

Geplante Werte:

- Health
- Mana
- Armor
- Strength
- Agility
- Stamina
- Intellect
- Attack Power
- Spell Power
- Critical Chance
- weitere Werte nur bei tatsächlichem Bedarf

Stats sollen aus Spielerbasiswerten, Level, Klasse und PixelRPG-Equipment entstehen.

Vanilla-Spieler dürfen von diesen RPG-Stats nicht betroffen sein.

---

## 8. Klassen

Klassen bleiben Bestandteil des PixelRPG-Systems.

Eine Klasse definiert hauptsächlich:

- Basiswerte
- Skalierung
- Spielstil
- verfügbare Ausrüstung
- ggf. besondere Interaktionen

Es gibt **kein Talent-System**.

Es gibt **keine klassischen Charakter-Skilltrees**.

---

## 9. Waffen und Fähigkeiten

Aktive Fähigkeiten kommen ausschließlich über PixelRPG-Waffen.

Beispiel:

```text
PixelRPG-Waffe
    ↓
Waffenfähigkeit
    ↓
Aktivierung durch definierte Interaktion
    ↓
Mana-/Ressourcenkosten + Cooldown + Effekt
```

Die Fähigkeit gehört zur Waffe und nicht zu einem separaten Talentbaum.

Vanilla-Waffen erhalten dadurch nicht automatisch PixelRPG-Fähigkeiten.

---

## 10. Equipment

PixelRPG-Equipment erhält ein eigenes RPG-System.

### Raritäten

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

### Item-System

- Item Level
- Level-Anforderung
- RPG-Stats
- Equipment Slots
- Waffenfähigkeiten
- Set-Boni, sofern sinnvoll
- eindeutige PixelRPG-Item-Identität

Das alte Gem-/Rune-Modell wird nicht weitergeführt.

---

## 11. Gems / Runes entfernen

Das bisherige Gem-/Rune-Konzept wird vollständig aus dem zukünftigen Kernsystem entfernt.

Zu entfernen bzw. nicht weiterzuentwickeln:

- Gems
- Runes
- Socket-System
- Gem Slots
- Passive Gems
- Skill Gems
- Gem Recalculation
- gem-/runeabhängige Progression

Stats und Fähigkeiten kommen stattdessen direkt aus Spieler, Klasse und Equipment/Waffen.

---

## 12. Open World und dynamische Skalierung

PixelRPG verwendet **keine festen Level-Gebiete**.

Die Welt bleibt frei begehbar.

Ein Spieler kann mit niedrigem Level theoretisch jede Dimension betreten. Das System sagt nicht pauschal: "Du darfst hier nicht hin."

Stattdessen entscheidet die Stärke der Gegner, ob der Spieler dort überleben kann.

### Dimensionen als natürliche Schwierigkeitsbereiche

Beispielhafte Basiswerte:

- Overworld: offen / Spieler-Level als Hauptreferenz
- Nether: Gegnerstufe mindestens ca. 50+
- End: Gegnerstufe mindestens ca. 75+

Diese Werte sind keine Zugangssperren.

Beispiel:

```text
Level 10 Spieler → Nether-Gegner ca. Level 50+
Level 10 Spieler → End-Gegner ca. Level 75+
```

Der Spieler kann trotzdem dorthin gehen.

### Dynamische Skalierung

Gegner, XP und Loot sollen sich an der Situation des registrierten PixelRPG-Spielers orientieren.

Ziel:

```text
World/Dimension Base
        +
Mob Type / Umgebung
        +
Player Level
        ↓
RPG Mob Level
        ↓
Damage / Health / XP / Loot
```

Die Skalierung darf Vanilla-Spieler nicht beeinflussen.

---

## 13. Vanilla-Mobs und PixelRPG-Mobs

Vanilla-Mobs dürfen nicht global für alle Spieler in RPG-Mobs verwandelt werden.

Für registrierte PixelRPG-Spieler muss die RPG-Skalierung sauber umgesetzt werden, ohne Vanilla-Spieler zu beschädigen.

Dabei sind insbesondere zu berücksichtigen:

- Multiplayer mit Vanilla- und PixelRPG-Spielern
- unterschiedliche Spieler-Level in derselben Welt
- Mob-Zielauswahl
- Schaden
- Health
- XP
- Loot
- Nametags nur wenn sinnvoll und isoliert

---

## 14. Loot

Loot ist ein Kernbestandteil der Progression.

Loot soll mit der RPG-Situation skalieren:

- Mob-Level
- Spieler-Level
- Dimension
- Mob-Typ
- Loot-Tabelle
- Item-Level
- Rarität

Nether und End sollen durch ihre höheren Basiswerte automatisch hochwertigeren bzw. gefährlicheren Content erzeugen.

Vanilla-Drops müssen erhalten bleiben, sofern ein Vanilla-Item betroffen ist.

PixelRPG-Loot wird zusätzlich bzw. gezielt für registrierte Spieler erzeugt.

---

## 15. Quests und Story

Das bestehende Quest-/Story-System wird weiterentwickelt.

Quest-Arten können umfassen:

- Kill
- Collect
- Talk
- Explore
- Craft
- Gather
- Story
- Boss
- Daily/Repeatable, falls später benötigt

Quests sind PixelRPG-Inhalte und werden nur für registrierte Spieler relevant.

---

## 16. Berufe

Berufe bleiben ein wichtiger MMO-Bestandteil.

### Sammelberufe

- Mining
- Woodcutting
- Herbalism
- Skinning
- Fishing

### Herstellungsberufe

- Blacksmithing
- Leatherworking
- Tailoring
- Alchemy
- Cooking
- weitere Berufe bei Bedarf

Berufe können auf **1–100** skalieren.

Berufe bestimmen, was ein PixelRPG-Spieler herstellen kann, aber nicht, welche Vanilla-Bereiche er betreten darf.

---

## 17. Crafting-Koexistenz

Vanilla-Crafting bleibt vollständig Vanilla.

PixelRPG-Crafting ist ein separates System.

Beispiel:

```text
Vanilla:
Crafting Table → Vanilla Rezept

PixelRPG:
NPC → Native Dialog → PixelRPG Crafting → RPG Item
```

Kein PixelRPG-Beruf darf Vanilla-Crafting pauschal blockieren.

---

## 18. Economy

Bestehende Economy-, Shop- und Bank-Systeme werden weiterentwickelt.

PixelRPG-Economy soll getrennt von Vanilla-Mechaniken bleiben, soweit dies für die Koexistenz erforderlich ist.

---

## 19. Party / Multiplayer

Das bestehende Party-System bleibt.

Es soll mit der Open-World-Skalierung funktionieren:

- unterschiedliche Spieler-Level
- gemeinsames Kämpfen
- XP-Verteilung
- Loot
- Mob-Skalierung

Es gibt **keinen Dungeon-/Raid-Zwang**.

Open-World-Gruppenspiel ist der Schwerpunkt.

---

## 20. Bosse und World Content

Das bestehende Boss-System bleibt und wird auf Open-World-Content ausgerichtet.

Mögliche Inhalte:

- seltene World Bosse
- starke Nether-Bosse
- starke End-Bosse
- besondere Overworld-Bosse
- Events

Bosse sind keine Instanz-Dungeons vorausgesetzt.

---

## 21. Was ausdrücklich NICHT gebaut wird

- keine Level-Zonen
- keine Level-Gebiets-Sperren
- kein Talent-System
- keine Talenttrees
- keine separaten Character Skills
- keine Skillbar als WoW-Kopie
- keine Dungeons als Pflichtsystem
- keine Raids
- kein Dungeon Finder
- keine Reputation-/Fraktions-Progression
- kein WoW-artiges MMO-HUD
- keine Target Frames
- keine Party Frames
- kein Quest Tracker als WoW-HUD
- kein Gem-System
- kein Rune-System
- kein Socket-System
- keine globale Umwandlung von Vanilla-Items in RPG-Items
- keine globale Umwandlung von Vanilla-Mobs für nicht registrierte Spieler

---

## 22. Nutzung von Minecraft Vanilla

PixelRPG soll so viel Vanilla wie möglich **verwenden**, statt bestehende Minecraft-Systeme unnötig nachzubauen.

Beispiele:

- Vanilla-Weltgeneration
- Overworld
- Nether
- End
- Vanilla-Mobs als Grundlage
- Vanilla-Items als Grundlage für eindeutig getrennte RPG-Items
- Vanilla-Inventare
- Vanilla-Interaktionen, wo passend
- native Dialoge
- native Sounds
- native Particles
- native Entities
- Vanilla-Crafting für Vanilla-Spieler
- Vanilla-Redstone
- Vanilla-Farming
- Vanilla-Exploration

PixelRPG ergänzt Minecraft, es ersetzt Minecraft nicht.

---

## 23. Prioritäten für die Umsetzung

### Phase 1 – Schutz und Fundament

1. Spieler-Registrierung als harte Aktivierungsgrenze
2. Vanilla-/PixelRPG-Isolation
3. vollständige Paper-26.2-API-Prüfung
4. native Dialog API
5. `/dialogue`
6. NPC → Dialog → Aktion
7. NPC-Mannequin als zentrale PixelRPG-Schnittstelle
8. G / `minecraft:quick_actions` ausschließlich für das eigene Spielerprofil
9. Spielerprofil / Charakterkarte
10. Level 1–99
11. XP
12. Player Profile
13. StatEngine

### Phase 2 – RPG Combat

14. Health
15. Mana
16. Klassen
17. Combat Stats
18. Mob Scaling
19. Dimension Scaling
20. XP Scaling
21. Loot Scaling

### Phase 3 – Equipment

22. PixelRPG Item Identity
23. Rarity
24. Item Level
25. Item Stats
26. Armor
27. Accessories
28. Weapon Abilities
29. Sets
30. Gems/Runes vollständig entfernen

### Phase 4 – Open World Content

31. Quest-System
32. Story
33. NPC Services
34. Shops
35. Bank
36. Economy
37. Berufe
38. PixelRPG Crafting
39. World Bosse
40. Open-World Events

### Phase 5 – Multiplayer

41. Party
42. Open-World Scaling für Gruppen
43. Party XP
44. gemeinsames Open-World Gameplay
45. Loot-Verteilung

### Phase 6 – Feinschliff

46. Vanilla-Kompatibilität vollständig prüfen
47. registrierte vs. nicht registrierte Spieler testen
48. Vanilla-Items und PixelRPG-Items sauber trennen
49. Vanilla-Mobs und PixelRPG-Skalierung sauber trennen
50. Dialoge und NPC-Schnittstellen vereinheitlichen
51. alte Rank-/Gem-/Rune-/Test-Reste entfernen
52. Paper-26.2-API und native Mechaniken abschließend prüfen

---

## Architektur-Leitsatz

> **PixelRPG ist eine optionale RPG-Ebene über Minecraft Vanilla.**
>
> **Ein Spieler wird erst durch die ausdrückliche Registrierung zum PixelRPG-Spieler.**
>
> **NPC-Mannequins sind die Schnittstellen zu den PixelRPG-Systemen. Dialoge sind der primäre Interaktionsweg. Inventory-GUIs werden nur dort eingesetzt, wo ein Dialog nicht ausreicht.**
>
> **Die G-Taste (`minecraft:quick_actions`) gehört ausschließlich zum eigenen Spielerprofil und öffnet die persönliche PixelRPG-Charakterkarte.**
>
> **Vanilla und PixelRPG müssen auf demselben Server und in derselben Welt koexistieren können, ohne sich gegenseitig zu beschädigen.**
>
> **Die Welt bleibt Open World. Progression entsteht durch Spieler-Level, dynamische Skalierung, Equipment, Waffenfähigkeiten, Quests, Berufe, Loot und Weltinhalte – nicht durch künstliche Gebietsgrenzen.**
