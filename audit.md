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

**Aktuelle Phase: alle weiterhin im Scope befindlichen implementierten Systeme konsequent auf 100 % bringen.**

100 % bedeutet dabei nicht nur „Klasse vorhanden“, sondern:

```text
implementiert
→ vollständig integriert
→ sinnvoll spielbar
→ Edge Cases behandelt
→ Persistenz korrekt
→ Content ausreichend vorhanden
→ Build sauber
→ Server-/Runtime-Test bestanden
→ Dokumentation synchron
→ danach ✅
```

Content darf und soll erweitert werden, wenn ein vorhandenes System für einen vollständigen Gameplay-Loop noch zu dünn ist. Dazu gehören insbesondere Crafting-Rezepte, Loot, Quest-/Boss-Rewards, Item-Progression und die tatsächliche Einbindung von Soulbound.

## Verworfen / dauerhaft außerhalb des Scopes

Die folgenden Systeme wurden ausdrücklich abgelehnt und werden **nicht implementiert, nicht weiter geplant und nicht als offene Aufgaben geführt**:

- Mana-System als Gameplay-Ressource
- Blessings
- Curses
- Gems
- Runes
- Sockets
- fertiges/eigenständiges Resourcepack-System
- vollständiges Guild-System

Das gilt auch dann, wenn historische Klassen, Keys, Language-Einträge oder Dokumentationsreste dieser Systeme noch im Repository gefunden werden. Solche Reste werden bei Cleanup/Analyse entfernt oder entsprechend als historische Altlasten behandelt, sofern sie nicht mehr verwendet werden.

Das bedeutet ausdrücklich **nicht**, dass jede bestehende Guild-bezogene Klasse automatisch entfernt wird. Aktive, weiterhin benötigte Economy-/Guild-Currency-Pfade bleiben bestehen, sofern sie tatsächlich verwendet werden. Verworfen ist das **vollständige Guild-System als zukünftiges Feature**.

## Bereits durchgeführte Cleanup-Schritte

### Resourcepack

Das bisherige `resourcepack/` wurde vollständig aus `main` entfernt. Ein neues fertiges Resourcepack ist **nicht mehr geplant** und wird nicht als zukünftige Aufgabe geführt.

### Companion-Stat-Daten

`src/main/resources/data/companion-stats.json` wurde entfernt, da im aktuellen Runtime-Pfad keine Verwendung dieser separaten generischen Statquelle nachweisbar war. Das aktive Companion-System verwendet `companions.json` und die Companion-/Mannequin-Controller.

### Veraltete GUI-Klassen

Entfernt:

- `AttributeTraderGUI`
- `BankGUI`
- `ClassSelectionGUI`
- `TravelGUI`
- `QuestCategoryGUI`
- `QuestBoardGUI`

Die letzten beiden bildeten einen nicht erreichbaren Quest-Category-/Board-GUI-Pfad. Aktive GUIs bleiben bestehen, wenn sie tatsächlich verwendet werden.

### Veraltete Config

`effects.aura-interval-ticks` wurde aus `config.yml` entfernt. Der zugehörige alte Equipment-Aura-Pfad existiert nicht mehr und der Config-Key wurde im aktuellen Code nicht verwendet.

### Nachgewiesene ungenutzte Java-Klassen

Entfernt:

- `command/impl/CraftingCommand.java` – nicht registriert und nicht Teil des aktuellen Command-Pfades.
- `combat/ClassSetBonusService.java` – kein aktueller Aufrufer/Runtime-Pfad.
- `item/ClassSetItemFactory.java` – keine aktuelle Item-/Loot-/Command-Verwendung.
- `item/ClassSetSlot.java` – gehörte ausschließlich zum entfernten Class-Set-Factory-Pfad.

Die dazugehörigen `RPGKeys.Item.classSetClass()` und `RPGKeys.Item.classSetSlot()` wurden ebenfalls entfernt.

### Aktive, aber historisch benannte Systeme

Nicht alles mit alten Begriffen ist automatisch ungenutzt. Beispielsweise existieren `GuildAPI`, `EconomyAPI`, `GuildCurrencyItemFactory` und `GuildCompassListener` weiterhin in aktiven Pfaden. Diese werden nicht gelöscht, sondern separat darauf geprüft, ob ihre Benennung/Abstraktion noch zum aktuellen System passt.

## Bereits gestartete 100-%-Arbeiten

### Soulbound

Soulbound ist nicht mehr nur eine isolierte Service-Klasse. Es gibt jetzt eine explizite native Charakterprofil-Dialog-Aktion zum Binden des identifizierten Gegenstands in der Haupthand.

Der bestehende Death-/Respawn-Pfad schützt soulbound Gegenstände weiterhin vor dem normalen Drop beim Tod registrierter PixelRPG-Spieler.

Noch zu validieren:

- Identified → Soulbound
- bereits Soulbound
- nicht identifiziert
- Tod → Respawn
- voller Inventar-Slot nach Respawn
- Vanilla-Spieler-Isolation

### Crafting

Die Crafting-Registry wurde um zusätzliche Progressionsstufen und Rezepte erweitert. Alle vier vorhandenen Berufe besitzen jetzt eine deutlich breitere Level-/Rarity-Spanne bis Level 99.

Die zusätzlichen Rezepte sind noch Bestandteil der Runtime-/Balance-Abnahme und gelten erst nach erfolgreichem Build und Server-Test als abgeschlossen.

## Wichtige Lifecycle-Regeln

### Persistente NPC-Mannequins

NPCs sind dauerhafte PixelRPG-Weltschnittstellen. Sie müssen nach Login und relevantem Chunk-Lifecycle wieder sichtbar sein.

```text
NPC-Definition → NpcManager → Entity → Chunk-/Login-Resync
```

### Companions

Companions sind **keine persistenten Welt-NPCs**.

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

Zu prüfen ist ausschließlich, ob das erneute Rufen nach Login zuverlässig funktioniert.

## Dialoge / Quick Actions – feste Scope-Regel

Bestehende Dialogsysteme werden **nicht eigenständig verändert**.

Eine Änderung an einem bestehenden Dialog, bestehenden Button oder bestehenden Quick-Action-Bereich erfolgt nur dann, wenn Hazel dies ausdrücklich und gezielt verlangt. Die Soulbound-Aktion ist eine solche ausdrücklich gewünschte Gameplay-Erweiterung.

## Vanilla-/PixelRPG-Isolation

Ein Spieler ist zunächst Vanilla. Erst nach PixelRPG-Registrierung dürfen RPG-Systeme greifen.

Besonders kritisch zu validieren:

- Mob Spawn/Damage/Health/Targeting/Death
- Loot und XP
- Item Events
- Crafting und Inventory
- Combat

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

Soulbound muss als echte Gameplay-Eigenschaft behandelt werden und darf nicht auf einen ungenutzten PDC-Key reduziert bleiben.

## Combat / Scaling / Loot

Vorhanden:

- zentrale Damage-Logik
- Weapon-Abilities
- Mob Scaling
- Loot
- Boss-Damage
- Class/Stat-Einfluss
- Soulbound Death Protection

Weapon-Abilities verwenden Rechtsklick und waffenbezogene Cooldowns. Es gibt keine Mana-Kosten.

Mob Scaling ist strukturell vorhanden; der reale Vanilla-/PixelRPG-Mischbetrieb muss noch getestet werden.

Loot wird weiter auf vollständige Level-/Rarity-/Boss-/Reward-Einbindung geprüft.

## Dialoge / NPC-Interaktion

Native Dialoge sind der primäre Interaktionsweg. Vorhanden sind u. a. Registrierung, Empfang, Schmied, Quest, Shop, Travel, Story, Bank, Companion und Berufe.

Inventory-GUIs bleiben dort, wo ein Dialog nicht ausreicht. Sie werden nicht pauschal entfernt.

## Quests

Das Quest-System besitzt Repository, Manager, Progress, Navigation, globale Events, passive Checks, Mob-Kills und XP-/Progressionslogik.

Für 100 % müssen vorhandene Questtypen tatsächlich durchgespielt und ihre Rewards/Companion-Unlocks validiert werden.

## Companions / Bosse / Mounts

Companion-Grundsystem und Boss-System sind vorhanden.

Companions werden bis zum vollständigen Runtime-Abschluss gebracht. Mounts bleiben außerhalb des aktuellen implementierten Scopes und sind ein späteres Feature.

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

# Offene 100-%-Completion-Aufgaben

## P0 – Build / Runtime

1. Build gegen Paper 26.2 erfolgreich durchführen.
2. Vanilla-/PixelRPG-Mischtest für Mob Scaling durchführen.
3. YAML- und bestehende MySQL-Persistenz nach dem Cleanup testen.
4. Soulbound E2E testen.
5. Neue Crafting-Rezepte E2E testen.

## P1 – Systeme vollständig machen

6. Language-Dateien vollständig gegen tatsächlich verwendete Keys prüfen.
7. Alte Rank-/Gem-/Rune-/Socket-/HUD-Texte aus Language-Dateien entfernen, sobald die Nichtverwendung bestätigt ist.
8. Inventory-GUIs einzeln als Runtime/Admin/Inventaroperation/Legacy klassifizieren.
9. PDC-Keys und historische Datenpfade auf tote Einträge prüfen.
10. Config-Keys systematisch gegen den aktuellen Code abgleichen.
11. API-Schicht prüfen: verwendete APIs behalten, veraltete/inkonsistente APIs aufräumen.
12. Quest-/Companion-Daten gegen die tatsächliche Runtime-Verwendung prüfen; Companion-Testdaten dabei ausdrücklich ausnehmen.
13. Weitere Java-Orphan-Kandidaten aus Command/GUI/Service/Factory-Schichten gegen den tatsächlichen Dependency-Graph prüfen.
14. Loot-Tabellen und Rewards ausbauen, bis jeder aktive Content-Pfad einen sinnvollen Reward-Loop besitzt.
15. Crafting-Progression auf vollständige Level-/Rarity-Stufen prüfen und Rezepte bei Bedarf erweitern.
16. Items/Equipment/Rarities in Combat, Loot, Crafting und Shop konsistent integrieren.
17. Bosses inklusive Phasen, Damage Contribution, Death, Loot und Companion Rewards vollständig validieren.
18. Companions inklusive Unlock, Besitz, Auswahl, Follow, Progression, Equipment, Combat und erneutem Rufen vollständig validieren.

## P2 – Abschlussprüfung

19. End-to-End: Registrierung → NPC → Dialog → Quest → Combat → XP → Loot/Reward.
20. Persistenztest über Server-Neustart.
21. Level-/Scaling-Testmatrix.
22. Party-/Shop-/Bank-/Travel-End-to-End.
23. Story-End-to-End.
24. Language-/UI-End-to-End.
25. Abschließender Voll-Audit nach allen Completion-Arbeiten.

## Definition von 100 %

Ein System erhält erst **✅**, wenn alle für dieses System relevanten Punkte erfüllt sind:

```text
Code vorhanden
+ Runtime integriert
+ Content ausreichend
+ Persistenz korrekt
+ Fehlerfälle behandelt
+ Vanilla-Isolation korrekt
+ Build erfolgreich
+ Server-Test erfolgreich
+ Dokumentation aktuell
= 100 % / ✅
```

## Arbeitsregel

```text
Analyse
   ↓
Implementierungslücke feststellen
   ↓
Code / Daten / Content ergänzen
   ↓
Build
   ↓
Server-Test
   ↓
Fehler beheben
   ↓
erneut testen
   ↓
Dokumentation aktualisieren
   ↓
System = ✅ 100 %
```

Keine erfundenen APIs, keine alten Minecraft-/Paper-Versionen und keine unnötigen Komplett-Refactorings.

**Dieses Dokument ist die Master-Referenz für den aktuellen Projektstand.**
