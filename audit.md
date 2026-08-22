# PixelRPG – Vollständiger System-Audit

> Audit des aktuellen `main`-Stands. Dieser Durchlauf implementiert bewusst **keine neuen Gameplay-Systeme** und verändert keine Balancewerte. Ziel ist ausschließlich, den vorhandenen Stand gegen Roadmap, Balancing-Dokumentation und die inzwischen festgelegte Vanilla-/Koexistenz-Philosophie zu prüfen.

Stand: 2026-08-22  
Basis-Commit: `9818e38d289fab356ab19d4bd2cd0b160abff727`

---

## 1. Gesamturteil

PixelRPG ist technisch bereits deutlich über einen reinen Prototyp hinaus. Die zentralen Bereiche sind vorhanden:

- Player/Profile/Persistenz
- Guild-Registrierung
- Level/XP
- Attribute
- Klassen
- Items/Rarity/Item-Level
- Combat
- Mob-Level-Scaling
- Loot
- Weapon-Abilities
- NPC-System
- native Dialoge
- Quests
- Berufe/Crafting
- Economy
- Party
- Story
- Travel
- Companions
- Bosse/World-Bosse
- Statistics/Scoreboard
- öffentliche API/Event-Schicht

Der aktuelle Hauptbedarf ist **nicht weitere Funktionalität**, sondern Konsistenz.

Die Architektur enthält noch mehrere Überreste aus früheren Entwicklungsphasen. Besonders wichtig sind widersprüchliche Dokumentation, noch vorhandene Mana-Implementierung, ein Input-Mismatch bei Weapon-Abilities, alte GUI-Kompatibilität und teilweise doppelte/abweichende Balance-Fallbacks.

---

## 2. Wichtigster Fund: Mana ist noch tatsächlich implementiert

Die frühere Annahme „Mana ist im aktuellen Code nicht mehr vorhanden“ ist falsch.

Im aktuellen Stand existiert Mana noch an mehreren Stellen:

- `PlayerProfile`: `currentMana`, `manaInitialized`, Verbrauch/Regeneration/Initialisierung
- `ManaRegenerationTask`
- `StatEngine`: Max-Mana und Mana-Operationen
- `WeaponAbilityEngine`: Mana-Kosten werden aus PDC gelesen und geprüft
- `RPGKeys.Item.weaponAbilityManaCost()`
- YAML-Persistenz: `mana.current`, `mana.initialized`
- MySQL-Persistenz: `mana_current`, `mana_initialized`
- MySQL-Schema/Migration
- `PixelRPGPlugin`: Mana-Regenerations-Task wird gestartet und beendet

Damit ist Mana **kein toter Dokumentationsrest**, sondern ein reales, wenn auch konzeptionell nicht finalisiertes System.

### Konsequenz

Mana darf nicht einfach durch das Löschen einer einzelnen Klasse entfernt werden. Eine spätere Bereinigung muss mindestens PlayerProfile, StatEngine, WeaponAbilityEngine, RPGKeys, beide Profil-Repositories, Datenbankschema und Plugin-Lifecycle gemeinsam behandeln.

Da dieser Audit ausdrücklich keine neue Implementierung durchführen soll, bleibt Mana in diesem Durchlauf unangetastet und ist als **Cleanup-Punkt** markiert.

---

## 3. Weapon-Abilities: Dokumentation und tatsächlicher Trigger widersprechen sich

`RPGItemBuilder.withWeaponAbility(...)` beschreibt die Fähigkeit als **Right Click** und schreibt dies auch ins Item-Lore.

`SkillInputListener` prüft dagegen weiterhin:

```text
isSneaking() == true
```

Die tatsächliche Aktivierung ist damit weiterhin **Shift + Rechtsklick**.

Das ist ein klarer Widerspruch zwischen:

- aktuellem Designentscheid
- Item-Lore
- Listener-Implementierung

### Status

**Bug/Cleanup, kein neues Feature.**

Die nächste Bereinigungsrunde muss diese drei Stellen auf eine einzige Wahrheit bringen.

---

## 4. Native Dialoge sind tatsächlich Bestandteil der Architektur

Die Dialog-Implementierung ist kein Mock oder Ersatz-GUI-System. `PixelRPGBootstrap` registriert native Dialoge über die aktuelle Paper-Dialog-API und hängt den Character-Dialog sogar in die Quick-Actions ein.

NPC-Verhalten wie Schmied, Questgeber, Händler, Reise, Story und Bank verwenden native Dialoge als Einstieg.

Das bestätigt die inzwischen festgelegte Vanilla-First-Strategie.

### Wichtig

Die vorhandenen Inventory-GUIs sind deshalb nicht pauschal „tot“:

- einige werden weiterhin für tatsächliche Inventaroperationen benötigt,
- einige sind Admin-/Editor-Werkzeuge,
- andere existieren als Kompatibilitäts-/Legacy-Schicht.

Sie dürfen daher nicht blind gelöscht werden.

---

## 5. Vanilla-/PixelRPG-Isolation ist grundsätzlich gut, aber noch nicht überall risikofrei

Die meisten RPG-Systeme prüfen sauber `GuildAPI.isRegistered(...)` bzw. `PlayerProfile.isRegisteredInGuild()`.

Das ist besonders sichtbar bei:

- XP
- Loot
- Economy
- Shop
- Bank
- Berufe
- Quests
- Reise
- Story
- NPC-Services

Das entspricht der gewünschten Koexistenz.

### Kritischer Bereich: Mob Scaling

`MobLevelScalingListener` verändert die tatsächlichen Attribute eines Vanilla-Monsters global, sobald ein registrierter Spieler daran teilnimmt.

Das Monster bleibt also dieselbe Entity für die Welt, erhält aber temporär RPG-Werte.

Das ist konzeptionell vertretbar, erzeugt aber einen wichtigen Mischfall:

```text
Vanilla-Spieler
       ↓
selbe Welt / selbes Monster
       ↑
RPG-Spieler
```

Während ein RPG-Spieler aktiv am Kampf beteiligt ist, ist das Monster kein rein neutrales Vanilla-Monster mehr.

Die bestehende `CombatDamageListener`-Isolation muss deshalb in echten Servertests besonders geprüft werden.

Das ist **kein Grund, das Scaling-System jetzt umzubauen**. Es ist ein zentraler Testfall für die spätere Validierung.

---

## 6. Balance-Dokumentation ist gegenüber dem Code veraltet

`balancing.md` beschreibt teilweise noch die Situation **vor** den bereits vorgenommenen Balance-Anpassungen.

Beispiele:

- alte Item-Skalierungsformeln
- alte Rarity-Multiplikatoren
- alter Companion-XP-Stand
- alte Attributwerte
- Mana als geplantes/finalisierbares System
- Status „Codeänderungen noch nicht vorgenommen“, obwohl inzwischen mehrere Änderungen erfolgt sind

Das Dokument ist als historische Analyse wertvoll, aber als **aktuelle Master-Baseline** nicht mehr zuverlässig.

### Konsequenz

`balancing.md` sollte in einem späteren Dokumentationsschritt in drei klare Bereiche getrennt werden:

```text
HISTORISCHE ANALYSE
AKTUELLER CODE-STAND
OFFENE BALANCE-ENTSCHEIDUNGEN
```

Keine neuen Balancewerte in diesem Audit.

---

## 7. Attribute: JSON und Legacy-Fallback besitzen unterschiedliche Werte

`attributes.json` enthält inzwischen die neue reduzierte Baseline, unter anderem:

- Vitality: `+2 HP`
- Agility Speed: `+0.0025`
- Agility Crit: `+0.20`
- Precision: `+0.75`
- Range: `+0.10`
- Toughness: `+1 Armor`

`AttributeConfig` besitzt jedoch weiterhin andere Legacy-Fallbackwerte, zum Beispiel:

- Vitality: `4.0`
- Agility Speed: `0.008`
- Agility Crit: `1.5`
- Precision: `1.2`
- Toughness: `2.0`

Da der normale Startup-Pfad JSON lädt, sind die neuen Werte im Normalfall aktiv. Die Fallbackwerte sind aber eine zweite, widersprüchliche Balancequelle.

### Status

**Technische Bereinigung erforderlich.**

Eine einzige Datenquelle sollte die endgültige Wahrheit sein.

---

## 8. Klassen: gleicher Datenquellen-Fehler

`class-balance.json` enthält die aktuelle datengetriebene Klassenbaseline.

`ClassBalance` besitzt zusätzlich weiterhin harte Java-Formeln als Fallback.

Die beiden Datensätze unterscheiden sich teilweise erheblich.

Damit existieren auch hier zwei mögliche Wahrheiten:

```text
JSON-Balance
    vs.
Java-Fallback-Balance
```

### Status

Nicht sofort entfernen, weil der Fallback bewusst als Fehlerpfad gedacht ist.

Aber vor dem finalen Polish muss entschieden werden, ob ein Fallback überhaupt noch einen anderen Balancezustand enthalten darf.

---

## 9. Item-System: Kernskalierung ist inzwischen sauberer als die Dokumentation

`RPGItemBuilder` verwendet aktuell:

- deterministische Levelskalierung
- Rarity-Multiplikator
- feste Basestats aus `item-scaling.json`
- kein zufälliges `0.85–1.15`-Kernstat-Roll

Das entspricht der späteren Balanceentscheidung deutlich besser als die alte Beschreibung in `balancing.md`.

`withWeaponAbility(...)` ist ebenfalls bereits auf Right Click im Lore ausgelegt.

### Offener Punkt

Die eigentlichen Weapon-Abilities sind noch hart im `WeaponAbilityEngine` codiert (`HEAVY_STRIKE`, `WHIRLWIND`, `ARCANE_BURST`). Das ist für den jetzigen Stand akzeptabel, sollte aber nicht mit einem bereits vollständig datengetriebenen Ability-System verwechselt werden.

Kein Ausbau in diesem Audit.

---

## 10. Companion-System: Fundament vorhanden, Combat bewusst noch nicht vollständig

Das Companion-System besitzt bereits:

- Level 1–99
- XP
- Rarity
- aktive Auswahl
- Persistenz
- Entity-State
- Follow-Verhalten
- Rename-Regeln
- Unique-Regeln

`companion-stats.json` existiert ebenfalls.

Die Datei selbst beschreibt ausdrücklich, dass Companion-Combat in Phase 1 deaktiviert ist.

Das ist positiv: Hier sollte **jetzt nichts erzwungen werden**.

### Auffälligkeit

Die Companion-XP-Konfiguration ist datengetrieben, während die historische Balance-Dokumentation noch von älteren festen XP-Werten ausgeht.

Die tatsächliche XP-Vergabe muss daher später gegen die aktuelle JSON-Basis validiert werden.

---

## 11. Quests: Systemisch weit fortgeschritten

Die Questarchitektur deckt inzwischen ab:

- HUNT
- COLLECT
- TALK_TO_NPC
- ESCORT
- REACH_LOCATION
- GLOBAL_EVENT
- Voraussetzungen
- Folgequests
- Zeitlimits
- Party-Sharing
- globale Events
- Rewards
- Companion-Rewards
- Navigation

Die Daten in `quests.json` sind ebenfalls deutlich konkreter als ein reines Testsystem.

### Audit-Fazit

Quest-System nicht erweitern.

Jetzt nur noch auf:

- tatsächliche Ausführbarkeit
- Reward-Balance
- NPC-Verknüpfungen
- Vanilla-Isolation
- Persistenz

testen.

---

## 12. Legacy-GUIs nicht pauschal entfernen

Im Repo existieren weiterhin mehrere Inventory-GUI-Klassen.

Ein Teil davon ist weiterhin aktiv:

- Shop
- Bank-Inventaroperationen
- Admin-Editoren
- Crafting-/Admin-Kompatibilität

Andere Teile sind Übergangsschichten.

### Entscheidung

Nicht „alle GUIs löschen“.

Stattdessen später für jede GUI klassifizieren:

```text
RUNTIME
ADMIN TOOL
INVENTORY OPERATION
LEGACY / DEAD
```

Erst danach löschen.

---

## 13. Persistenz ist solide aufgebaut, aber Mana hängt noch tief darin

PlayerProfileManager besitzt:

- Pre-login Loading
- Timeout
- Async Save Queue
- Save-Chaining pro UUID
- Emergency YAML Backup bei MySQL-Fehlern
- YAML und MySQL Repository
- Shutdown Flush

Das ist für den aktuellen Framework-Charakter eine gute Grundlage.

Der größte Cleanup-Kandidat innerhalb der Persistenz ist aktuell Mana, weil es dort mehrfach als echte persistierte Information existiert.

---

## 14. API-/Framework-Schicht ist bereits sinnvoll getrennt

Vorhanden sind unter anderem:

- `GuildAPI`
- `EconomyAPI`
- `ItemAPI`
- `PartyAPI`
- `StatisticsAPI`
- `PixelRPGProvider`
- eigene Events

Das unterstützt die Framework-Idee.

Diese Schicht sollte während des Polishing möglichst stabil bleiben.

Keine neue API nur wegen einzelner interner Features hinzufügen.

---

## 15. Paper-/Java-Vorgaben

Der aktuelle Build-Stand verwendet:

- Java 25
- Paperweight `2.0.0-beta.21`
- Paper Dev Bundle `26.1.2.build.+`
- Shadow
- `paper-plugin.yml`

Damit ist ein Punkt aus der Projektvorgabe noch nicht vollständig umgesetzt:

> **Das Projekt soll aktuell gegen Paper 26.2 entwickelt werden.**

Der Repository-Stand ist weiterhin auf **26.1.2**.

Das ist kein Gameplay-Feature und sollte als eigener technischer Upgrade-Schritt behandelt werden, nicht nebenbei innerhalb des Audits.

---

## 16. Event-Listener-Regel

Die geprüften zentralen Listener besitzen bereits die geforderten kurzen Kommentare über `@EventHandler`.

Dieser Stil sollte bei zukünftigen Änderungen beibehalten werden.

---

# Priorisierte Restarbeiten – ohne neue Features

## P0 – muss vor echtem Gameplay-Testing geklärt werden

1. Weapon Ability Trigger: Right Click vs. Shift + Right Click
2. Mana-Entscheidung technisch vollständig durchführen oder bewusst zurückstellen
3. Vanilla/RPG Mob-Scaling-Mischfälle testen
4. tatsächlichen Build gegen Paper 26.2 prüfen

## P1 – Konsistenz/Polish

5. `balancing.md` aktualisieren
6. Legacy-Fallbackwerte von `attributes.json` und `class-balance.json` bereinigen
7. alle GUI-Klassen klassifizieren
8. Companion- und Quest-Daten gegen tatsächliche Runtime-Verwendung prüfen
9. PDC-Keys auf tote/historische Felder prüfen
10. Datenquellen pro System auf genau eine Masterquelle reduzieren

## P2 – finale Validierung

11. Vanilla-Spieler-Testmatrix
12. PixelRPG-Spieler-Testmatrix
13. gemischte Welt mit beiden Spielertypen
14. Level-Stufen 1/10/20/30/40/50/60/70/80/90/99
15. Persistenz Reload/Restart
16. NPC/Dialog/Quest/Reward-End-to-End
17. Combat/Scaling/Loot-End-to-End
18. Boss/Companion erst danach

---

# Was in diesem Audit NICHT gemacht wurde

Bewusst nicht verändert:

- keine neuen Gameplay-Systeme
- keine neuen Items
- keine neuen Skills
- keine neuen Quests
- keine neuen NPCs
- keine neuen Klassen
- keine neuen Balancewerte
- keine neue Mana-Mechanik
- keine neue UI
- keine externen Dependencies

Dieser Commit dient ausschließlich dazu, den tatsächlichen Zustand festzuhalten und die nächsten Cleanup-/Testschritte eindeutig zu machen.

---

# Fazit

PixelRPG braucht momentan **mehr Ordnung als Inhalt**.

Die wichtigste Erkenntnis des Audits ist nicht, dass große Systeme fehlen. Im Gegenteil: Die meisten geplanten Kernsysteme existieren bereits.

Das Problem ist, dass einige davon noch aus unterschiedlichen Entwicklungsphasen stammen und dadurch mehrere Wahrheiten nebeneinander existieren.

Die nächste Phase sollte deshalb lauten:

```text
AUDIT
 ↓
KONSISTENZ
 ↓
CLEANUP
 ↓
BUILD
 ↓
SERVER-TEST
 ↓
BUGFIX / POLISH
 ↓
ERNEUTER AUDIT
```

Erst wenn dieser Zyklus sauber durchlaufen ist, sollte wieder über neue Funktionalität gesprochen werden.
