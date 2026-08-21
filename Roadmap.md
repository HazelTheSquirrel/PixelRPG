# PixelRPG – Master Roadmap — COMPLETE

> **Verbindliche Projektspezifikation für die weitere Entwicklung von PixelRPG.**
>
> Diese Datei beschreibt nicht nur Ideen, sondern die technische und spielerische Richtung, gegen die zukünftige Implementierungen geprüft werden.
>
> **Status:** Die Entwicklungsreihenfolge dieser Roadmap wurde umgesetzt und der aktuelle `src/*`-Stand wurde gegen die definierten Kernregeln geprüft. Der nächste Schritt ist der separate Voll-Audit auf dem Server: Funktionstest, Polish und Bugfixing.

---

# 1. Projektziel

PixelRPG wird ein modernes, optionales Open-World-RPG für **Paper 26.2**.

Minecraft Vanilla und PixelRPG müssen **auf demselben Server, in derselben Welt und gleichzeitig** funktionieren.

PixelRPG soll Minecraft nicht ersetzen. Minecraft liefert die Welt, die Vanilla-Mechaniken und einen großen Teil der vorhandenen Infrastruktur. PixelRPG legt eine klar getrennte RPG-Ebene darüber.

**World of Warcraft: Wrath of the Lich King (WotLK) dient ausschließlich als Referenz und Inspiration.** Es werden nur Konzepte übernommen, die zum PixelRPG-Design passen. PixelRPG soll kein WoW-Klon werden.

---

# 2. Absolute oberste Regel: Registrierung und Isolation

## 2.1 Der wichtigste Systemschalter

Ein Spieler ist grundsätzlich zunächst ein **Vanilla-Spieler**.

Erst nachdem er sich ausdrücklich für PixelRPG registriert hat, darf PixelRPG für diesen Spieler greifen.

```text
Spieler betritt Server
        ↓
Nicht registriert
        ↓
100 % Vanilla
        ↓
NPC-Mannequin ansprechen
        ↓
Native Dialog
        ↓
PixelRPG-Registrierung
        ↓
PlayerProfile aktiviert
        ↓
PixelRPG-Systeme aktiv
```

Diese Regel steht über allen anderen Features.

## 2.2 Nicht registrierter Spieler

Ein nicht registrierter Spieler darf durch PixelRPG nicht zu einem RPG-Spieler gemacht werden.

Nicht aktiv:

- PixelRPG-Level
- PixelRPG-XP
- PixelRPG-Stats
- PixelRPG-Klasse
- PixelRPG-Equipmentpflicht
- Waffenfähigkeiten
- RPG-Mob-Skalierung
- RPG-Loot-Skalierung
- PixelRPG-Quests
- PixelRPG-Berufe
- PixelRPG-Economy-Funktionen
- PixelRPG-Progression

Vanilla bleibt normal:

- Crafting
- Smelting
- Mining
- Farming
- Fishing
- Villager
- Items
- Waffen
- Rüstung
- Mobs
- Redstone
- Dimensionen
- Exploration
- Vanilla-Progression

## 2.3 Registrierter Spieler

Nach der Registrierung wird das PixelRPG-Profil aktiviert.

Dann können unter anderem greifen:

- Level 1–99
- XP
- RPG-Stats
- Klasse
- PixelRPG-Equipment
- Waffenfähigkeiten
- RPG-Combat
- dynamische Mob-Skalierung
- XP-/Loot-Skalierung
- Quests und Story
- Berufe
- PixelRPG-Crafting
- Bank/Shop/Economy
- Party-System

## 2.4 Persistenz

Der Registrierungsstatus muss persistent gespeichert werden.

Ein Server-Neustart darf nicht dazu führen, dass ein registrierter Spieler plötzlich wieder Vanilla ist oder umgekehrt.

## 2.5 Globale Events

Jede globale Event-Implementierung muss prüfen, ob sie registrierte und nicht registrierte Spieler unterschiedlich behandeln muss.

Besonders kritisch:

- Mob Spawn
- Mob Damage
- Mob Health
- Entity Targeting
- Entity Death
- Loot
- XP
- Item Events
- Crafting
- Inventory
- Player Stats
- Combat

**Keine globale Änderung ohne garantierte Vanilla-Isolation.**

---

# 3. Technische Leitplanken

- Paper **26.x**, aktuell und verbindlich **26.2**.
- Java **25**.
- Mojang-Mappings.
- `paper-plugin.yml`.
- Native Minecraft/Paper-26.2-Dialogsystem.
- Adventure Components.
- Kein `ChatColor`.
- Keine alten 1.21.x-APIs.
- Keine alten 1.21.x-Dialogimplementierungen.
- Keine CraftBukkit-Namen.
- Keine Legacy-NMS-Pakete.
- `.gradle` bleibt auf **Gradle 9.2.0**.
- `build.gradle` wird nicht verändert.
- `settings.gradle` wird nicht verändert.
- Entwicklungsarbeit erfolgt grundsätzlich unter `src/*`.
- Gearbeitet wird ausschließlich im Branch `main`.
- Keine neuen Branches ohne ausdrückliche Freigabe.
- Keine neuen Tags ohne ausdrückliche Freigabe.

---

# 4. NPC-Mannequins = Schnittstellen zu PixelRPG

Die bereits vorhandenen Mannequin-NPCs sind die zentralen Schnittstellen zwischen Minecraft-Welt und PixelRPG-Systemen.

Ein Mannequin kann beispielsweise anbieten:

- Registrierung
- Bank
- Berufe lernen
- Quest-Annahme
- Quest-Abgabe
- Shop
- Händler
- Blacksmith
- Crafting
- Story
- Reisen
- weitere PixelRPG-Dienste

## 4.1 Standardinteraktion

```text
NPC-Mannequin
      ↓
Native Minecraft Dialog
      ↓
Spielerauswahl
      ↓
PixelRPG-Service
```

Der native Dialog ist der **primäre Interaktionsweg**.

## 4.2 Inventory GUI

Ein Inventory GUI wird nur verwendet, wenn ein Dialog für die Funktion nicht ausreicht.

Beispiele:

- Bank
- umfangreiche Shops
- Item-Auswahl
- komplexe Crafting-Oberflächen

Die bereits vorhandene GUI-Infrastruktur wird wiederverwendet.

Keine parallelen GUI-Systeme ohne konkreten Bedarf.

## 4.3 `/dialogue`

`/dialogue` muss dieselbe zentrale Dialog-Engine verwenden wie die NPC-Mannequins.

Es darf keine zweite, veraltete oder speziell für den Command gebaute Dialogarchitektur bestehen.

---

# 5. G-Taste / `minecraft:quick_actions`

Die Minecraft-Interaktion `minecraft:quick_actions` wird **ausschließlich für das eigene PixelRPG-Spielerprofil** verwendet.

Sie ist nicht die allgemeine NPC-Interaktion.

```text
G / quick_actions
        ↓
eigenes Spielerprofil
        ↓
Charakterkarte
```

Mögliche Informationen:

- Name
- Level
- Klasse
- Health
- Mana
- Stats
- Equipment
- Berufe
- Quests
- persönliche Statistiken

Die Spielerkarte ist eine bewusste, kontextbezogene UI.

**Kein dauerhaftes WoW-artiges MMO-HUD.**

---

# 6. PlayerProfile

`PlayerProfile` ist die zentrale Quelle für den registrierten PixelRPG-Spieler.

Das Profil muss mindestens logisch abbilden können:

- UUID
- Registrierungsstatus
- Level
- XP
- Klasse
- RPG-Stats
- Berufe
- Equipment-/Progressionsdaten
- Questfortschritt
- Economy-Daten, sofern PixelRPG-Economy verwendet wird

Es darf nicht mehrere widersprüchliche Quellen für denselben Spielerstatus geben.

Beispiel:

```text
PlayerProfile
     ├── Registration
     ├── Level / XP
     ├── Class
     ├── Stats
     ├── Professions
     ├── Equipment
     ├── Quests
     └── Economy
```

---

# 7. Level-System

Das alte **F–S-Rank-System wird vollständig ersetzt**.

Neues System:

- Level 1–99
- Level 100 bleibt gesperrt/reserviert
- persistentes Level
- persistente XP
- Level-Up-System
- Level beeinflusst RPG-Progression

Level ist kein Gebietsschlüssel.

```text
Level = Charakterprogression

nicht:

Level = Zugangsberechtigung für eine Zone
```

---

# 8. Stats

Es gibt eine zentrale StatEngine.

Geplante Kernwerte:

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

Weitere Stats werden nur eingeführt, wenn sie für das tatsächliche Gameplay benötigt werden.

Grundmodell:

```text
Basiswerte
   +
Level
   +
Klasse
   +
Equipment
   ↓
Finale RPG-Stats
```

Nicht registrierte Spieler befinden sich außerhalb dieser RPG-Berechnung.

---

# 9. Health / Armor / Mana

Die RPG-Werte sollen mit der Vanilla-Anzeige koexistieren, statt Minecraft unnötig komplett zu ersetzen.

Health und Armor dürfen intern als RPG-Werte präziser berechnet werden, während die sichtbare Minecraft-Oberfläche weiterhin verwendet wird.

Mana besitzt keine normale Vanilla-Leiste und wird deshalb kontextbezogen dargestellt, beispielsweise über:

- Spielerprofil
- Actionbar
- Dialoge
- Item-Lore
- situationsbezogene UI

Kein permanentes MMO-HUD nur für Mana.

---

# 10. Klassen

Klassen bleiben Teil des RPG-Kerns.

Eine Klasse bestimmt hauptsächlich:

- Basiswerte
- Skalierung
- Spielstil
- geeignete RPG-Ausrüstung
- ggf. besondere Eigenschaften

Klassen dürfen Vanilla-Spieler nicht beeinflussen.

**Nicht vorgesehen:**

- Talent-System
- Talenttrees
- Character-Skilltrees
- WoW-Skillbar

---

# 11. Waffenfähigkeiten

Aktive Fähigkeiten werden **über PixelRPG-Waffen** vergeben.

```text
PixelRPG-Waffe
      ↓
Waffenfähigkeit
      ↓
Spielerinteraktion
      ↓
Ressourcenkosten
      ↓
Cooldown
      ↓
Effekt
```

Die Fähigkeit gehört zur Waffe.

Sie gehört nicht zu einem Talentbaum und nicht zu einem separaten Charakter-Skill-System.

Vanilla-Waffen werden dadurch nicht automatisch zu RPG-Waffen.

---

# 12. Equipment und Item-System

PixelRPG-Equipment besitzt eine eigene RPG-Identität.

## Raritäten

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

## Item-Daten

Ein PixelRPG-Item kann besitzen:

- eindeutige PixelRPG Item-ID
- Item-Level
- Level-Anforderung
- Rarität
- RPG-Stats
- Equipment Slot
- Waffenfähigkeit
- Set-Zugehörigkeit
- weitere RPG-Metadaten

## Vanilla-Trennung

```text
minecraft:diamond_sword
```

bleibt ein Vanilla-Item.

Ein PixelRPG-Schwert muss technisch eindeutig als PixelRPG-Item identifizierbar sein.

Material allein ist keine ausreichende PixelRPG-Identität.

---

# 13. Gems / Runes / Sockets abschaffen

Das alte Gem-/Rune-Konzept wird nicht weiterentwickelt.

Vollständig aus dem zukünftigen Kern zu entfernen:

- Gems
- Runes
- Sockets
- Gem Slots
- Passive Gems
- Skill Gems
- Gem Recalculation
- Rune Calculation
- gem-/runeabhängige Progression

Die Progression wird stattdessen über folgende Säulen getragen:

```text
Level
 +
Klasse
 +
Stats
 +
Equipment
 +
Waffenfähigkeiten
```

---

# 14. Open World

PixelRPG verwendet **keine levelbasierten Gebiete**.

Es gibt keine unsichtbare oder künstliche Levelwand vor einer Region.

Ein Spieler darf die Welt erkunden.

Die Herausforderung entsteht durch Gegnerstärke, nicht durch Gebiets-Sperren.

---

# 15. Dimensionen als natürliche Schwierigkeitsstufen

Minecraft liefert bereits natürliche große Weltbereiche.

Diese werden genutzt.

## Overworld

Die Overworld ist der offene Hauptbereich.

Die Gegnerstärke orientiert sich grundsätzlich an der RPG-Situation des Spielers bzw. an der lokalen Weltdefinition.

## Nether

Der Nether erhält eine deutlich höhere natürliche Gegnerbasis.

Zielwert:

**ca. Level 50+**

Das ist keine Zugangssperre.

Ein Level-10-Spieler darf den Nether betreten.

Er muss nur damit rechnen, dass die Gegner deutlich stärker sind.

## End

Das End erhält eine noch höhere natürliche Gegnerbasis.

Zielwert:

**ca. Level 75+**

Auch dies ist keine Zugangssperre.

---

# 16. Dynamische Mob-Skalierung

Das wichtigste Open-World-System ist die dynamische Skalierung.

Ziel:

```text
Dimension / Weltbasis
        +
Mob-Typ
        +
Umgebung
        +
Spieler-Level
        +
Kampfsituation
        ↓
RPG Mob Level
        ↓
Health
Damage
XP
Loot
```

Die endgültige mathematische Formel wird nicht blind festgelegt. Sie wird anhand des vorhandenen Codes und realen Gameplays bestimmt.

## Kritischer Punkt: mehrere Spieler

Es muss funktionieren, wenn gleichzeitig vorhanden sind:

- Level-10-PixelRPG-Spieler
- Level-50-PixelRPG-Spieler
- nicht registrierte Vanilla-Spieler

Ein Mob darf nicht einfach global auf einen Spielerlevel gesetzt werden, wenn dadurch andere Spieler falsch behandelt werden.

Die Architektur muss die tatsächliche Kampfsituation berücksichtigen.

---

# 17. Loot-Skalierung

Loot ist ein zentraler Bestandteil der Progression.

RPG-Loot kann abhängen von:

- Mob-Level
- Spieler-Level
- Dimension
- Mob-Typ
- Loot-Tabelle
- Item-Level
- Rarität

Nether und End sollen durch ihre höhere Schwierigkeit auch eine entsprechende Loot-Progression ermöglichen.

Vanilla-Drops dürfen nicht blind zerstört werden.

PixelRPG-Loot muss sauber von Vanilla-Loot unterschieden werden.

---

# 18. Vanilla Crafting und PixelRPG Crafting

Beide Systeme existieren parallel.

## Vanilla

```text
Crafting Table
      ↓
Vanilla Rezept
      ↓
Vanilla Item
```

## PixelRPG

```text
NPC-Mannequin
      ↓
Native Dialog
      ↓
PixelRPG Crafting
      ↓
optional Inventory GUI
      ↓
PixelRPG Rezept
      ↓
PixelRPG Item
```

PixelRPG-Berufe dürfen Vanilla-Crafting nicht global blockieren.

---

# 19. Berufe

Berufe sind ein wichtiger MMO-Bestandteil.

Zielbereich:

**Level 1–100**

## Sammelberufe

- Mining
- Woodcutting
- Herbalism
- Skinning
- Fishing

## Herstellungsberufe

- Blacksmithing
- Leatherworking
- Tailoring
- Alchemy
- Cooking

Weitere Berufe nur bei echtem Bedarf.

Berufe bestimmen PixelRPG-Herstellungsmöglichkeiten.

Berufe bestimmen nicht, ob ein Spieler ein Vanilla-Gebiet betreten darf.

---

# 20. NPC-Services

Jeder Service soll möglichst über die NPC-Schnittstelle erreichbar sein.

Beispiel:

```text
Bankier
 ↓
Dialog
 ↓
Bank öffnen
 ↓
Inventory GUI
```

```text
Lehrer / Beruf-NPC
 ↓
Dialog
 ↓
Beruf lernen
```

```text
Quest-NPC
 ↓
Dialog
 ↓
Quest annehmen / abgeben
```

```text
Händler
 ↓
Dialog
 ↓
Shop
```

Damit wird das Mannequin-System zur einheitlichen Service-Schicht von PixelRPG.

---

# 21. Economy / Bank / Shop

Vorhandene Economy-, Bank- und Shop-Systeme werden weiterentwickelt.

PixelRPG-Economy ist ein PixelRPG-System und darf Vanilla-Spieler nicht ungewollt in die RPG-Economy zwingen.

Bank und umfangreiche Shops können Inventory-GUIs verwenden.

Dialoge bleiben der Einstiegspunkt.

---

# 22. Quests und Story

Das vorhandene Quest-/Story-System wird auf die neue Architektur ausgerichtet.

Mögliche Questtypen:

- Kill
- Collect
- Talk
- Explore
- Craft
- Gather
- Story
- Boss
- wiederholbare Aufgaben bei tatsächlichem Bedarf

Quest-Interaktion:

```text
NPC-Mannequin
      ↓
Dialog
      ↓
Quest-Aktion
```

Kein separater WoW-Quest-HUD.

---

# 23. Party und Open-World-Multiplayer

Das vorhandene Party-System bleibt Bestandteil des Projekts.

Es muss mit der offenen Welt und der dynamischen Skalierung funktionieren.

Zu berücksichtigen:

- unterschiedliche PixelRPG-Level
- gemeinsamer Kampf
- XP-Verteilung
- Loot
- Mob-Skalierung
- registrierte Spieler
- nicht registrierte Spieler in derselben Umgebung

Party ist Open-World-orientiert.

---

# 24. Bosse und World Content

Das bestehende Boss-System wird auf Open-World-Content ausgerichtet.

Mögliche Inhalte:

- seltene Overworld-Bosse
- starke Nether-Bosse
- starke End-Bosse
- besondere Welt-Events

Es wird **kein Dungeon-/Raid-System als Kernvoraussetzung** gebaut.

---

# 25. UI/HUD-Prinzip

Die normale Minecraft-Oberfläche bleibt erhalten.

Es wird kein permanentes WoW-HUD gebaut.

Kontextbezogene Informationen dürfen dargestellt werden über:

- native Dialoge
- Inventory GUI
- Actionbar
- Bossbar
- Chat/Adventure Components
- Item-Lore
- Spielerprofil

Die UI soll nur dann erscheinen, wenn sie für die jeweilige Aktion sinnvoll ist.

---

# 26. Explizit NICHT Teil der Roadmap

Die folgenden Systeme werden bewusst nicht gebaut:

- levelbasierte Gebiete
- Level-Gebietssperren
- Talent-System
- Talenttrees
- Character-Skilltrees
- separate Character-Skillprogression
- WoW-Skillbar
- Dungeons als Kernsystem
- Raids
- Dungeon Finder
- Raid Finder
- Reputation
- Fraktionen als MMO-Progressionssystem
- permanentes MMO-HUD
- Target Frames
- Party Frames
- WoW-Quest-Tracker-HUD
- Gems
- Runes
- Sockets
- globale RPG-Konvertierung von Vanilla-Items
- globale RPG-Konvertierung von Vanilla-Mobs für nicht registrierte Spieler
- unnötige Neuimplementierung vorhandener Vanilla-Systeme

---

# 27. Minecraft Vanilla aktiv nutzen

PixelRPG soll vorhandene Minecraft-Mechaniken bevorzugen.

Priorität:

1. vorhandene Minecraft-/Paper-Mechanik verwenden
2. vorhandene PixelRPG-Infrastruktur wiederverwenden
3. nur bei fehlender Funktion eigene Infrastruktur bauen

Beispiele:

- Vanilla-Weltgeneration statt eigener Weltgeneration
- Vanilla-Dimensionen statt eigener Gebiete
- Vanilla-Items als Basismaterial für RPG-Items
- Vanilla-Mobs als Basis für RPG-Gegner
- Vanilla-Inventory als GUI-Grundlage
- native Dialoge statt eigener Dialogsimulation
- `minecraft:quick_actions` für das Spielerprofil

---

# 28. Architekturregeln

## Eine Quelle pro System

Es darf nicht mehrere widersprüchliche Implementierungen desselben Systems geben.

Es gibt jeweils eine zentrale Quelle für:

- PlayerProfile
- Registrierung
- Level/XP
- Stats
- Klassen
- Item-Identität
- Mob-Skalierung
- Loot
- Dialoge

## Dialog-Zentralisierung

NPCs und `/dialogue` verwenden dieselbe Dialog-Engine.

## Registrierung überall berücksichtigen

Jedes System, das Spieler, Entities, Items oder Weltmechaniken verändert, muss die Registrierung berücksichtigen.

## Vanilla niemals versehentlich konvertieren

Ein Vanilla-Spieler soll nicht plötzlich PixelRPG spielen, nur weil er in der Nähe eines registrierten Spielers steht.

## Native API bevorzugen

Paper 26.2 und die aktuellen Minecraft-Mechaniken werden verwendet.

Keine alten Workarounds aus früheren Minecraft-Versionen.

---

# 29. Alte Systeme: prüfen, entfernen oder umbauen

Bei der Weiterentwicklung von `src/*` müssen bestehende Systeme gegen diese Roadmap geprüft werden.

## Vollständig entfernen

- F–S-Rank-System
- Gem-System
- Rune-System
- Socket-System
- alte rankabhängige Progression
- gem-/runeabhängige Stats
- veraltete Dialog-Workarounds
- tote Testimplementierungen
- nicht mehr verwendete Legacy-Klassen

## Überarbeiten

- PlayerProfile
- Level/XP
- Stats
- Klassen
- Equipment
- Item-System
- Mob Scaling
- Loot
- Quests
- Story
- Berufe
- Economy
- Party
- NPC-System
- Dialogsystem
- `/dialogue`
- Spielerprofil / `quick_actions`

## Wiederverwenden, wenn kompatibel

- vorhandene Inventory-GUIs
- vorhandene Persistenz
- vorhandene NPC-/Mannequin-Infrastruktur
- vorhandene Quest-Strukturen
- vorhandene Party-Strukturen
- vorhandene Boss-Strukturen
- vorhandene Economy-Strukturen

---

# 30. Entwicklungsreihenfolge

## Phase 1 – Fundament und Schutz

1. Registrierungsstatus vollständig definieren.
2. Registrierung persistent machen.
3. Registrierung über NPC-Mannequin + nativen Dialog fertigstellen.
4. Alle globalen Systeme auf Registrierung prüfen.
5. Vanilla-Spieler vollständig isolieren.
6. NPC-Interaktion zentralisieren.
7. Native 26.2 Dialog-Engine finalisieren.
8. `/dialogue` auf dieselbe Engine bringen.
9. `minecraft:quick_actions` ausschließlich für das Spielerprofil verwenden.

## Phase 2 – Player Core

10. PlayerProfile bereinigen.
11. F–S-Rank vollständig entfernen.
12. Level 1–99 implementieren/finalisieren.
13. XP implementieren/finalisieren.
14. StatEngine finalisieren.
15. Health finalisieren.
16. Mana finalisieren.
17. Armor finalisieren.
18. Klassen finalisieren.

## Phase 3 – Item Core

19. Vanilla-/PixelRPG-Item-Trennung finalisieren.
20. Eindeutige PixelRPG Item-ID.
21. Raritäten.
22. Item-Level.
23. Level-Anforderungen.
24. Equipment-Stats.
25. Waffenfähigkeiten.
26. Gems/Runes/Sockets entfernen.
27. Altes Item-/Rank-System bereinigen.

## Phase 4 – Open-World Scaling

28. Overworld-Basis definieren.
29. Nether-Basis ca. 50+.
30. End-Basis ca. 75+.
31. Mob-Level-System.
32. Health-Skalierung.
33. Damage-Skalierung.
34. XP-Skalierung.
35. Loot-Skalierung.
36. Mehrspieler-Skalierung.
37. Vanilla-/PixelRPG-Mischszenarien testen.

## Phase 5 – NPC und Content

38. NPC-Service-Schicht.
39. Bank.
40. Shops.
41. Berufe.
42. PixelRPG-Crafting.
43. Quests.
44. Story.
45. Party.
46. World Bosse.
47. World Events.

## Phase 6 – Bereinigung

48. Alte Dialogsysteme entfernen.
49. Alte Rank-Klassen entfernen.
50. Alte Gem-/Rune-Klassen entfernen.
51. Tote Services entfernen.
52. Nicht mehr verwendete Datenmodelle entfernen.
53. Nicht mehr verwendete Listener entfernen.
54. Legacy-Code aus früheren Versionen entfernen.
55. Alle Systeme erneut gegen die Registrierungsgrenze prüfen.

---

# 31. Testmatrix

Jede relevante Änderung muss mindestens diese Situationen berücksichtigen.

## Szenario A – Vanilla

```text
Spieler A = nicht registriert
```

Er muss Minecraft normal spielen können.

## Szenario B – PixelRPG

```text
Spieler A = registriert, Level 10
```

RPG-Systeme greifen.

## Szenario C – gemischte Welt

```text
Spieler A = Vanilla
Spieler B = PixelRPG Level 10
```

Beide müssen gleichzeitig korrekt funktionieren.

## Szenario D – unterschiedliche Level

```text
Spieler A = PixelRPG Level 10
Spieler B = PixelRPG Level 50
```

Scaling darf nicht blind nur einen Spieler berücksichtigen.

## Szenario E – Nether

```text
Level 10 → Nether betreten
```

Kein Zugang verweigert; Gegner können ca. 50+ sein.

## Szenario F – End

```text
Level 10 → End betreten
```

Kein Zugang verweigert; Gegner können ca. 75+ sein.

## Szenario G – Vanilla Item

```text
Vanilla Diamond Sword
```

Darf nicht automatisch PixelRPG-Stats/Fähigkeiten erhalten.

## Szenario H – PixelRPG Item

```text
PixelRPG Diamond Sword
```

Muss eindeutig als PixelRPG-Item erkannt werden können.

---

# 32. Definition of Done

Eine Funktion gilt erst als fertig, wenn:

- sie auf Paper 26.2 basiert
- sie Java 25 verwendet
- sie die aktuellen APIs verwendet
- sie die Registrierungsgrenze berücksichtigt
- sie Vanilla-Spieler nicht ungewollt beeinflusst
- sie mit registrierten Spielern funktioniert
- sie in gemischten Welten funktioniert
- sie keine alten 1.21.x-Mechaniken verwendet
- sie keine parallele Infrastruktur erzeugt, wenn bereits eine vorhanden ist
- sie zur Open-World-Philosophie passt
- sie keine ausgeschlossenen Systeme einführt
- sie sauber mit den bestehenden PixelRPG-Systemen zusammenarbeitet

---

# 33. Goldene Regeln für jede zukünftige Implementierung

> **1. Registrierung zuerst.**
>
> Ein nicht registrierter Spieler ist Vanilla.

> **2. Vanilla bleibt Vanilla.**
>
> PixelRPG ergänzt Minecraft und ersetzt es nicht pauschal.

> **3. NPC-Mannequin ist die Schnittstelle.**
>
> PixelRPG-Dienste werden grundsätzlich über NPC + Dialog erreichbar.

> **4. Native Dialoge zuerst.**
>
> GUI nur, wenn Dialog für die Funktion nicht ausreicht.

> **5. G / `minecraft:quick_actions` ist das eigene Profil.**
>
> Keine Vermischung mit NPC-Interaktion.
>
> **6. Open World.**
>
> Keine Level-Gebiete und keine künstlichen Zugangssperren.
>
> **7. Scaling statt Sperren.**
>
> Nether und End dürfen gefährlich sein, ohne den Spieler auszusperren.
>
> **8. Waffen geben Fähigkeiten.**
>
> Kein Talent-/Character-Skill-System.
>
> **9. Gems und Runes sind Geschichte.**
>
> Sie gehören nicht zum zukünftigen Kernsystem.
>
> **10. WotLK ist Inspiration, nicht die Bauanleitung.**
>
> Nur passende MMO-Konzepte werden übernommen.
>
> **11. Minecraft Vanilla wird genutzt.**
>
> Vorhandene Mechaniken werden bevorzugt, bevor etwas neu gebaut wird.
>
> **12. Keine globalen Seiteneffekte.**
>
> Ein PixelRPG-Spieler darf nicht versehentlich das Vanilla-Spiel eines anderen Spielers verändern.
>
> **13. `main` ist der Arbeitsstand.**
