# PixelRPG – Aktueller Projektstand / Master-Audit

Stand: 2026-08-23

> **Dieses Dokument ist die zentrale aktuelle Referenz für den technischen Projektstand.**
> Nach jeder größeren Analyse, Bereinigung oder Testphase wird diese Datei aktualisiert.
> Spezifische Markdown-Dateien (`balancing.md`, `Compboss.md`, `Roadmap_Quests.md`) bleiben als Fach-/Arbeitsdokumente bestehen. Bei Widersprüchen gilt dieses Audit.

## Technische Basis

- Java 25
- Paper 26.x, verbindlich Paper 26.2
- Paperweight `2.0.0-beta.21`
- `paperweight.paperDevBundle("26.2.build.+")`
- Mojang-Mappings
- `paper-plugin.yml`
- native Minecraft/Paper-26.2-Dialogsystem
- Adventure Components
- kein `ChatColor`
- keine alten 1.21.x-APIs/Dialogimplementierungen

## Aktuelle Phase

PixelRPG ist kein reiner Prototyp mehr. Die Kernsysteme sind vorhanden.

**Aktuelle Phase: systematischer Voll-Audit, Validation, Polish und gezielte Bugfixes.**

Wir konzentrieren uns aktuell auf **Aufräumen und Altlastenbereinigung**.

Arbeitsregel für Altlasten:

```text
nicht verwendet → entfernen
verwendet, aber veraltete Abhängigkeit → aufräumen / auf aktuellen Pfad bringen
verwendet und aktuell → bestehen lassen
unklar → erst Verwendung nachweisen, dann entscheiden
```

Companions befinden sich vollständig in der Testphase und werden bei diesem Cleanup **nicht als Altlasten bewertet**.

Keine neuen großen Gameplay-Systeme, solange offene P0/P1-Punkte nicht geprüft sind.

Vorhandene Kernbereiche:

- Player/Profile/Persistenz
- Registrierung und Vanilla-/PixelRPG-Isolation
- Level/XP
- Attribute/Klassen
- Items/Rarity/Item-Level/Equipment
- Combat/Mob Scaling/Loot
- Weapon-Abilities
- NPC-Mannequins
- native Dialoge
- Quests
- Berufe/Crafting
- Economy/Party
- Story/Travel
- Companions (Testphase)
- Bosse/World-Bosse
- Statistics/Scoreboard
- API/Event-Schicht

## Bereits durchgeführte Cleanup-Schritte

### Resourcepack

Das bisherige `resourcepack/` wurde vollständig aus `main` entfernt.

Es war kein Bestandteil des aktuellen Plugin-Build-/Runtime-Pfades und enthielt zusätzlich veraltete Assets, unter anderem Mana-Potion-Daten.

Ein neues Resourcepack wird später separat und passend zum finalen Item-/Asset-System aufgebaut.

### Companion-Stat-Daten

`src/main/resources/data/companion-stats.json` wurde entfernt, da im aktuellen Runtime-Pfad keine Verwendung dieser separaten generischen Statquelle nachweisbar war. Das aktive Companion-System verwendet `companions.json` und die Companion-/Mannequin-Controller.

### Veraltete GUI-Klassen

Folgende alte Inventory-GUIs wurden entfernt, da der aktuelle Interaktionspfad über native Dialoge bzw. andere aktive Inventory-Systeme läuft:

- `AttributeTraderGUI`
- `BankGUI`
- `ClassSelectionGUI`
- `TravelGUI`

Aktive GUIs bleiben bestehen, wenn sie tatsächlich noch verwendet werden.

### Veraltete Config

`effects.aura-interval-ticks` wurde aus `config.yml` entfernt. Der zugehörige alte Equipment-Aura-Pfad existiert nicht mehr und der Config-Key wurde im aktuellen Code nicht verwendet.

## Wichtige Lifecycle-Regeln

### Persistente NPC-Mannequins

NPCs sind dauerhafte PixelRPG-Weltschnittstellen. Sie müssen nach Login und relevantem Chunk-Lifecycle wieder sichtbar sein.

```text
NPC-Definition → NpcManager → Entity → Chunk-/Login-Resync
```

### Companions

Companions sind **keine persistenten Welt-NPCs**.

Das gewünschte Verhalten ist:

```text
Spieler besitzt Companion
        ↓
Spieler ruft Companion
        ↓
Companion wird gespawnt
        ↓
Logout
        ↓
Companion verschwindet
        ↓
Login
        ↓
Companion bleibt weg
        ↓
Spieler ruft ihn erneut
```

**Kein automatischer Companion-Restore beim Login.**

Zu prüfen ist deshalb ausschließlich, ob das erneute Rufen nach Login zuverlässig funktioniert.

## Dialoge / Quick Actions – feste Scope-Regel

Bestehende Dialogsysteme werden **nicht eigenständig verändert**.

Das gilt insbesondere für:

- bestehende NPC-Dialoge
- bestehende native Dialoge
- bestehende `minecraft:quick_actions`
- bereits vorhandene Buttons/Actions innerhalb dieser Systeme

**Es werden keine bestehenden Buttons hinzugefügt, entfernt oder umsortiert, nur weil bei einer Analyse weitere sinnvolle Möglichkeiten auffallen.**

Die einzige reguläre Ausnahme ist der Bereich `minecraft:quick_actions` über die G-Interaktion für **neu hinzukommende Companions**. Dort darf für einen neuen Companion die dafür notwendige Aktion ergänzt werden.

Eine Änderung an einem bestehenden Dialog, bestehenden Button oder bestehenden Quick-Action-Bereich erfolgt **nur dann, wenn Hazel dies ausdrücklich und gezielt verlangt**.

Analyseergebnisse zu bestehenden Dialogen dürfen dokumentiert werden, führen aber ohne ausdrückliche Anweisung **nicht automatisch zu einer Umsetzung**.

## Vanilla-/PixelRPG-Isolation

Ein Spieler ist zunächst Vanilla. Erst nach PixelRPG-Registrierung dürfen RPG-Systeme greifen.

Besonders kritisch zu validieren:

- Mob Spawn/Damage/Health/Targeting/Death
- Loot und XP
- Item Events
- Crafting und Inventory
- Combat

Wichtigster Mischfall:

```text
Vanilla-Spieler
      ↓
selbe Welt / selbe Entity
      ↑
PixelRPG-Spieler
```

Mob Scaling darf Vanilla-Spieler nicht unbeabsichtigt in RPG-Mechaniken ziehen.

## Persistenz

YAML und MySQL bleiben die beiden PlayerProfile-Repositories.

Noch zu validieren:

- YAML laden/speichern
- bestehende MySQL-Datenbank laden/speichern
- Server-Neustart
- persistente Registrierung
- Verhalten bei historischen Mana-Spalten

Alte Produktionsspalten werden nicht automatisch destruktiv gelöscht.

## Items / Equipment

PixelRPG-Metadaten/PDC sind die primäre Item-Identität. Vanilla-Material allein ist keine PixelRPG-Identität.

Raritäten:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

## Combat / Scaling

Vorhanden:

- zentrale Damage-Logik
- Weapon-Abilities
- Mob Scaling
- Loot
- Boss-Damage
- Class/Stat-Einfluss

Weapon-Abilities verwenden Rechtsklick und waffenbezogene Cooldowns. Es gibt keine Mana-Kosten.

Mob Scaling ist strukturell vorhanden; der reale Vanilla-/PixelRPG-Mischbetrieb muss noch getestet werden.

## Dialoge / NPC-Interaktion

Native Dialoge sind der primäre Interaktionsweg. Vorhanden sind u. a. Registrierung, Empfang, Schmied, Quest, Shop, Travel, Story, Bank, Companion und Berufe.

Inventory-GUIs bleiben dort bestehen, wo ein Dialog nicht ausreicht. Sie werden nicht pauschal entfernt.

Bestehende Dialoge und bestehende Buttons bleiben grundsätzlich unverändert. Änderungen daran erfolgen nur auf ausdrückliche, gezielte Anweisung von Hazel.

## Quests

Das Quest-System besitzt Repository, Manager, Progress, Navigation, globale Events, passive Checks, Mob-Kills und XP-/Progressionslogik.

Die Quest-Roadmap bleibt Designreferenz. Jetzt wird die Runtime gegen reale Questdefinitionen und End-to-End-Abläufe validiert.

## Companions / Bosse / Mounts

Companion-Grundsystem und Boss-System sind vorhanden.

Companion-Kampflogik bleibt bis zur Stabilisierung des Basissystems zurückgestellt.

Zu prüfen:

- Companion erneut rufen nach Login
- Spawn/Despawn
- Skin
- Equipment
- Follow
- Datenpersistenz
- Entity-Verlust
- Boss Spawn/Phasen/Damage/Death/Contribution/Loot

## Balancing

`balancing.md` ist die Fachreferenz für aktuelle Balancing-Werte.

Aktive Grundsätze:

- Level 1–99 reguläre Progression
- Level 100 reserviert/Endgame
- deterministische Progression
- keine Mana-Ressource
- keine Blessings/Curses
- keine Gems/Runes/Sockets
- keine alten F–S-Ranks

Das Balancing ist strukturell definiert, aber noch nicht final servervalidiert. Keine Einzelwerte auf Verdacht ändern.

# Offene Cleanup-/Audit-Aufgaben

## P0

1. Build gegen Paper 26.2 erfolgreich durchführen.
2. Vanilla-/PixelRPG-Mischtest für Mob Scaling durchführen.
3. YAML- und bestehende MySQL-Persistenz nach dem Cleanup testen.

## P1

4. Language-Dateien vollständig gegen tatsächlich verwendete Keys prüfen.
5. Alte Rank-/Gem-/Rune-/Socket-/HUD-Texte aus Language-Dateien entfernen, sobald die Nichtverwendung bzw. der veraltete Pfad bestätigt ist.
6. Inventory-GUIs einzeln als Runtime/Admin/Inventaroperation/Legacy klassifizieren.
7. PDC-Keys und historische Datenpfade auf tote Einträge prüfen.
8. Config-Keys systematisch gegen den aktuellen Code abgleichen.
9. API-Schicht prüfen: verwendete APIs behalten, veraltete/inkonsistente APIs aufräumen.
10. Quest-/Companion-Daten gegen die tatsächliche Runtime-Verwendung prüfen; Companion-Testdaten dabei ausdrücklich ausnehmen.

## P2

11. End-to-End: Registrierung → NPC → Dialog → Quest → Combat → XP → Loot/Reward.
12. Persistenztest über Server-Neustart.
13. Level-/Scaling-Testmatrix.
14. Abschließender Voll-Audit nach den Cleanup-Schritten.

## Arbeitsregel

```text
Analyse → Verwendung nachweisen → nicht verwendet = entfernen
                         ↓
              verwendet + veraltet = aufräumen
                         ↓
              verwendet + aktuell = bestehen lassen
                         ↓
                       Build
                         ↓
                    Server-Test
                         ↓
                   Audit aktualisieren
```

Zusätzlich gilt:

```text
Analyse eines bestehenden Systems
        ↓
Änderung notwendig?
        ↓
Nein → nur dokumentieren
        ↓
Ja
        ↓
Keine Umsetzung ohne ausdrückliche, gezielte Anweisung von Hazel
```

Insbesondere bei bestehenden Dialogen, Buttons und `minecraft:quick_actions` wird nichts eigenständig verändert.

Keine erfundenen APIs, keine alten Minecraft-/Paper-Versionen und keine unnötigen Komplett-Refactorings.

**Dieses Dokument ist die Master-Referenz für den aktuellen Projektstand.**
