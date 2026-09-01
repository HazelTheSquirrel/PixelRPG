# 🚨 Forensisches Audit: PixelRPG (Test-Branch)

**Audit-Stand:** `test` @ `24d9df464c8b52851697aacaa754561c71abe957`  
**Referenz:** `main` @ `6e42255c89966922bea0b111883e252772b97dd`  
**Branch-Differenz:** `test` ist 23 Commits vor `main`, nicht hinter `main`.  
**CI:** GitHub Actions Build `33559473964` auf `test` = **SUCCESS**.  
**Zielplattform:** Java 25 + Paper 26.2 (`26.2.build.121-stable`) + Mojang-Mappings + `paper-plugin.yml`.

> **Forensische Einschränkung:** Dies ist eine statische Code-/Architekturanalyse des tatsächlich vorliegenden `test`-Stands. 20.00 TPS kann ohne realen 100+-Spieler-Lasttest, Spark-Profiling und eine repräsentative Welt-/Entity-Verteilung nicht garantiert werden. Aussagen über Exploits sind als „bewiesen“, „plausibel“ oder „Test erforderlich“ gekennzeichnet.

---

## 1. Zusammenfassung & Skalierbarkeit (100+ Spieler)

## Klares Urteil: **Deutlich verbessert, aber noch nicht Production-Ready für eine harte 20.00-TPS-Garantie bei 100+ Spielern. Kein einzelner permanenter Server-Killer mehr; mehrere P0/P1-Pfade bleiben jedoch nicht skalierungssauber.**

Der aktuelle `test`-Stand ist gegenüber dem vorherigen Audit massiv besser. Die größten Dauer-Poller wurden entfernt und durch Coalescing-Wakeups bzw. begrenzte zeitbasierte Tasks ersetzt.

Die Build-Basis ist korrekt: Java 25, Paperweight `2.0.0-beta.21`, Paper Dev Bundle `26.2.build.121-stable`, Shadow `9.6.1`, Gson/Hikari/MySQL in den vorgegebenen Versionen. Die vorhandenen Source-/Shadow-Verifikationen bleiben in `check` aktiv.

### Was jetzt gut ist

- `WakeScheduler` verhindert redundante One-Shot-Wakeups pro Key.
- Scoreboard hat keinen permanenten globalen Update-Loop mehr.
- Quest-Passivprüfung hat keinen permanenten 40-Tick-Globalpoller mehr.
- NPC-Look wird durch echte Spielerbewegung geweckt.
- Companion-Idle-Runtime läuft nicht mehr global alle zwei Ticks.
- Mob-Scaling besitzt keinen globalen Cleanup-Poller mehr.
- Trade-Expiry wird pro Listing geplant.
- Bank-/Trade-Dateischreiben sind aus normalen Gameplay-Pfaden in einen Async-Writer verschoben.
- Player-Saves werden pro UUID über eine Save-Chain serialisiert.
- Keine offensichtliche globale `static Map<UUID, Player>`-Leak-Struktur.

### Was noch nicht akzeptabel ist

Die Architektur hat an mehreren Stellen nur den **Scheduler** entfernt, nicht die **algorithmische Kostenstruktur**. Ein Event-getriebener Handler, der bei jedem Event alle Spieler, alle Mobs oder teure World-Queries durchsucht, ist immer noch ein Skalierungsproblem.

| Bereich | Urteil | Priorität |
|---|---|---:|
| Build / API-Grenzen | sauber | P2 |
| Lifecycle | gut | P2 |
| Player Persistence | solide, aber komplex | P1 |
| AsyncFileWriter | sinnvoll, Shutdown/Crash-Härtung fehlt | P1 |
| Scoreboard | stark verbessert, globaler Guild-Scan bleibt | P1 |
| Quest Navigation | Event-driven, aber World-Queries teuer | P1 |
| NPC Look | gut verbessert | P2 |
| Companion | besser, aber Movement-Event heiß | P1 |
| Mob Scaling | Event-driven, pro Hit zu teuer | P1 |
| Trade Depot | Snapshot-I/O async, Economy nicht voll atomar | P0/P1 |
| Bosses | Zeitsteuerung legitim, globale Scans vermeiden | P1 |
| Region Spawns | 100-Tick-Task, aber O(R×S×P) | P1 |

### 100+-Spieler-Szenario

Bei 100+ Spielern mit gleichzeitigem Combat, Bewegung, Quests, Companion-Runtime und GUI-Interaktion ist nicht der Scheduler selbst der Engpass. Kritisch ist die Summe aus:

1. Damage-Events mit RPG-Berechnung.
2. Mob-Scaling mit Equipment-Scan.
3. PlayerMoveEvent-Wakeups für Companion und Quest-Navigation.
4. Scoreboard-Invalidierungen.
5. räumlichen World-Queries.
6. Bossbar-/Region-Scans.
7. vollständigen YAML-Snapshots bei Marktänderungen.

**20.00 TPS darf aktuell nicht versprochen werden.**

---

## 2. Kritische Performance-Engpässe & Threading-Fehler

### P0 — `TradeDepotManager.expireListing()`: abgelaufene Listings können dauerhaft hängen

**Datei:** `src/main/java/de/pixelrpg/rpg/trade/TradeDepotManager.java`  
**Methode:** `expireListing(UUID listingId)`

Der Ablauf ist aktuell:

```text
Listing läuft ab
→ bankStorage.addTradeGoods(...)
→ false
→ return
```

Wenn das Handelsfach voll ist, bleibt das Listing korrekt erhalten, aber der bereits ausgeführte Expiry-Task ist beendet. Es wird **kein neuer Retry** geplant.

Damit kann ein abgelaufenes Listing dauerhaft im Markt hängen.

**Fix:** `EXPIRED_PENDING_CLAIM`-State oder coalescender One-Shot-Retry. Kein globaler Poller.

```java
if (!bankStorage.addTradeGoods(listing.sellerId(), listing.itemCopy())) {
    scheduleExpiryRetry(listing.id(), 200L);
    return;
}

if (listings.remove(listing.id(), listing)) {
    cancelExpiry(listing.id());
    save();
}
```

---

### P0 — `TradeDepotManager.purchase()`: kein echter persistenter Transaktions-Commit

**Methode:** `purchase(Player buyer, UUID listingId)`

Der aktuelle Rollback ist gut und beseitigt den offensichtlichsten Itemverlust. Trotzdem bleiben mehrere getrennte Zustände:

```text
Buyer-Profil
Listing-Map
Seller/Pending-Payout
Trade-Goods-YAML
Trade-Depot-YAML
```

Diese Zustände besitzen keinen gemeinsamen persistenten Commit. Ein Prozessabsturz zwischen Live-Mutation und Async-Persistenz kann daher einen Zwischenzustand hinterlassen.

Das ist der **größte Economy-/Dupe-Risikobereich** des Projekts.

**SOLL:** transaktionale DB-/Journal-Struktur mit idempotenter Operation-ID. YAML kann Snapshot sein, aber nicht der endgültige Commit-Mechanismus.

---

### P1 — `AsyncFileWriter`: Shutdown-Race und nicht-atomarer Dateiersatz

**Datei:** `src/main/java/de/pixelrpg/rpg/core/AsyncFileWriter.java`

Der eigentliche File-I/O läuft korrekt außerhalb des Serverthreads. Das ist ein klarer Gewinn.

Problem:

```text
worker drain()
→ finally: draining=false
→ pending != empty
→ executor.execute(drain)

parallel
→ shutdown()
→ executor.shutdown()
```

Das erneute `execute()` kann nach `shutdown()` eine `RejectedExecutionException` auslösen.

Zusätzlich schreibt `Files.writeString()` direkt auf die Ziel-Datei. Bei Prozess-/Host-Absturz während des Schreibens ist eine beschädigte Datei möglich.

**SOLL:** deterministischer Shutdown-State + Temp-Datei + `ATOMIC_MOVE` mit Fallback.

---

### P1 — `ScoreboardService.applyGuildPrefixes()`: O(P) pro Spieler-Refresh

**Datei:** `src/main/java/de/pixelrpg/rpg/scoreboard/ScoreboardService.java`  
**Methode:** `applyGuildPrefixes(PlayerScoreboardState state)`

Der alte permanente O(P²)-Scheduler ist weg. Die Methode selbst scannt aber weiterhin zweimal `Bukkit.getOnlinePlayers()`.

Wenn 100 Scoreboards gleichzeitig dirty werden, kann daraus wieder ungefähr:

```text
100 Scoreboard-Refreshes
× 100 Online-Spieler
= 10.000 Spieler-Inspektionen
```

werden.

**SOLL:** Guild-State zentral indexieren. Guild-Join/Leave/Rename invalidiert nur betroffene Spieler. Kein globaler Online-Scan innerhalb eines einzelnen Player-Renderers.

Zusätzlicher Fehler: `apply()` beendet sich bei unveränderten `lastLines`, bevor Guild-Team-State aktualisiert wird. Sidebar-State und Team-State müssen getrennte Dirty-Domänen sein.

---

### P1 — `QuestPassiveCheckTask` / `QuestNavigationService`: teure World-Queries auf Bewegungsereignissen

**Dateien:** `QuestPassiveCheckTask.java`, `QuestNavigationService.java`  
**Methoden:** `process()`, `refresh()`, `resolveWorldTarget()`, `findWorldTarget()`

Der globale Poller ist entfernt, aber Blockwechsel eines Spielers können weiterhin `refresh()` auslösen.

`refresh()` verarbeitet bis zu fünf sichtbare aktive Quests. Bei Struktur-/Biome-Zielen kann synchron `locateNearestStructure()` bzw. `locateNearestBiome()` laufen.

Der Chunk-basierte Cache reduziert Wiederholungen, verhindert aber nicht teure Queries beim Betreten neuer Chunks.

**SOLL:**

- Zielauflösung als eigene invalidierbare Domäne.
- `REACH_LOCATION` ausschließlich über direkte Distanzprüfung.
- Struktur-/Biome-Ziele stärker cachen.
- Quest-State-Änderungen statt Bewegung als primärer Trigger für die meisten Navigation-Updates.

---

### P1 — `CompanionFollowTask`: zu viel Arbeit in einem PlayerMove-Wakeup

**Datei:** `CompanionFollowTask.java`  
**Methoden:** `onMove()`, `runOwner()`, `updateRuntimeState()`, `follow()`

Die Blockgrenzen-Drosselung ist richtig. Ein schneller Spieler kann trotzdem viele Wakeups erzeugen.

Ein Wakeup kann Player-/Entity-Lookups, PDC, Companion-Registry, Equipment-Hash, Equipment-Anwendung, Stat-Berechnung, Attribute und mehrere Blockabfragen auslösen.

**SOLL:**

```text
Movement
→ nur Follow-Position invalidieren

Equipment change
→ Equipment-State invalidieren

Level change
→ Stats invalidieren

Combat
→ temporäre Combat-Runtime aktivieren

Idle
→ schlafen
```

Nicht alle Zustandsarten bei jeder Bewegung neu berechnen.

---

### P1 — `CompanionFollowTask`: semantischer Risiko-Punkt

Nach einem normalen `follow()` wird im Idle-Pfad kein weiterer Wakeup geplant. Damit wird der Companion effektiv erst beim nächsten relevanten Spieler-Blockwechsel erneut verarbeitet.

Das ist performanceoptimal, kann aber das alte kontinuierliche Follow-Verhalten verändern.

**SOLL:** kurze aktive Follow-Phase mit wenigen One-Shot-Ticks nur dann, wenn der Companion tatsächlich außerhalb des Stop-Radius liegt. Danach wieder Sleep.

---

### P1 — `MobLevelScalingListener.applyScaling()`: vollständige Gear-Rechnung bei Damage

**Datei:** `MobLevelScalingListener.java`  
**Methoden:** `onRpgDamage()`, `markParticipant()`, `applyScaling()`, `gearLevelMultiplier()`

Jeder relevante Treffer kann `applyScaling()` auslösen. Die Methode scannt Teilnehmer, sucht Spieler, liest mehrere Equipment-Slots und Item-PDC und berechnet anschließend neue Mob-Attribute.

Bei hoher Angriffsdichte ist das unnötige Wiederholungsarbeit.

**SOLL:** `ScaledMobState` cachen und nur bei Änderung der Inputs invalidieren.

---

### P1 — `MobLevelScalingListener.onPlayerLevelUp()`: globaler Mob-Scan

**Methode:** `onPlayerLevelUp(PlayerLevelUpEvent event)`

Ein Level-Up iteriert über alle `activeParticipants`, obwohl die umgekehrte Relation bereits konzeptionell bekannt ist.

**SOLL:**

```text
player UUID → Set<mob UUID>
```

Dann werden beim Level-Up nur tatsächlich betroffene Mobs neu skaliert.

---

### P1 — `RegionSpawnService.hasNearbyPlayer()`: unnötiger Welt-Spielerscan

**Datei:** `RegionSpawnService.java`  
**Methoden:** `tick()`, `hasNearbyPlayer()`

Der 100-Tick-Intervall ist akzeptabel. Der Algorithmus ist jedoch:

```text
Region × SpawnPoint × world.getPlayers()
```

Das ist bei vielen Regionen/Spawnpunkten unnötig.

**SOLL:** aktuelle Paper-Nearby-Player-Abfrage am Spawnpunkt bzw. Chunk-Index.

---

### P1 — `BossManager.updateBossBar()`: Welt-Spielerscan

**Datei:** `BossManager.java`  
**Methode:** `updateBossBar()`

Für jeden aktiven Boss werden alle Spieler der Welt iteriert und anschließend per Distanz gefiltert.

Der Radius ist bereits bekannt. Eine lokale Nearby-Player-Abfrage ist die richtige primitive.

---

### P1 — `BossManager.cleanup()`: kompletter Welt-Entity-Scan

**Methoden:** `cleanup()`, `activeBossEntityWorldEntities()`

Beim World-Boss-Cleanup wird `world.getEntities()` verwendet und danach nach Boss-Markern gesucht.

Das ist ein unnötiger potentieller O(E)-Spike genau in einem ohnehin eventreichen Boss-Death-Tick.

**SOLL:** Add-Entities beim Spawn in `ActiveBoss` registrieren und beim Cleanup nur diese UUIDs entfernen.

---

### P1 — `PlaytimeTracker.flushAll()`: 100er DB-Burst

**Datei:** `PlaytimeTracker.java`  
**Methode:** `flushAll()`

Alle 6000 Ticks werden alle Online-Spieler geflusht. Das ist nicht Main-Thread-blockierend, weil die Profile asynchron gespeichert werden.

Es erzeugt aber einen synchronisierten DB-Burst.

**SOLL:** Dirty-Profile mit Jitter/Coalescing speichern, statt alle Spieler exakt gleichzeitig zu flushen.

---

### P1 — `MySQLPlayerProfileRepository.save()`: hohe Write-Amplification

**Datei:** `MySQLPlayerProfileRepository.java`  
**Methode:** `save(PlayerProfile profile)`

Ein Save löscht und erstellt Active Quests und Equipment vollständig neu und upsertet sämtliche Stats.

Das ist korrekt serialisiert, aber nicht effizient.

Zusätzlich werden obsolete Stat-Keys nicht gelöscht. Ein Reset kann daher alte Daten in der Datenbank zurücklassen, die später wieder geladen werden.

**SOLL:** Delta-Persistenz oder explizites Replace pro Subsystem mit Cleanup.

---

## 3. Sicherheitslücken & Exploit-Potenzial

### P0/P1 — Economy-State-Consistency

Siehe Trade Depot oben. Dies ist der wichtigste echte Exploit-Kandidat.

Ein klassischer Online-Race zwischen zwei Spielern ist durch den Main-Thread zwar weitgehend ausgeschlossen, weil Bukkit-Gameplay auf einem Thread serialisiert wird. Das Problem ist **Crash-Consistency**, nicht primär Java-Thread-Race.

---

### P1 — `CompanionEquipmentListener`: manuelle Inventaraktionen sind dupe-sensitiv

**Datei:** `CompanionEquipmentListener.java`  
**Methoden:** `onInventoryClick()`, `onInventoryDrag()`

Der Shift-Click-Pfad cancelt Bukkit und verändert danach Top-Inventar und Current Item manuell.

Das kann korrekt sein, ist aber ein klassischer Bereich für Dupe-Glitches.

Pflicht-Testmatrix:

- Shift-click Stack > 1.
- Number-key swap.
- Double-click collect-to-cursor.
- Drag über mehrere Slots.
- Volles Zielinventar.
- Cursor mit Item.
- Disconnect während der Mutation.
- GUI mehrfach öffnen/schließen.

**Status:** kein bewiesener Dupe im statischen Audit, aber **P1-Hardening erforderlich**.

---

### P1 — `ItemService.isRPGItem()`: zu schwache Wirtschaftsintegritätsprüfung

**Methode:** `isRPGItem(ItemStack item)`

Aktuell reicht eine vorhandene RPG-Item-ID in der PDC. Für interne Items ist das bequem, für Economy-Gates aber zu schwach.

Ein Trade-/Bank-System sollte mindestens validieren:

```text
PDC-ID
+ bekannte Definition
+ gültige Item-Schema-Version
+ erlaubte Kategorie
+ valide Instanz-ID
+ gültige wirtschaftliche Handelbarkeit
```

Unbekannte/veraltete RPG-Items sollten nicht automatisch wirtschaftlich vertrauenswürdig sein.

---

### P1 — `ItemService.createDefinedItem()`: Unique-Claim vor vollständigem Commit

Der Unique-Claim erfolgt vor der finalen Item-Auslieferung. Bei einem unerwarteten Fehler nach dem Claim kann ein Unique-Slot verbraucht sein, ohne dass das Item erfolgreich ausgegeben wurde.

**SOLL:** Claim und Ausgabe über einen gemeinsamen Commit-/Rollback-Pfad führen.

---

### P1 — `ExternalSkinService`: gute SSRF-Härtung, aber externer Fetcher bleibt Trust Boundary

**Positiv:**

- HTTP/HTTPS geprüft.
- Userinfo/Fragment abgelehnt.
- HTTPS für externe Hosts.
- Redirects deaktiviert.
- private, loopback, link-local und multicast Adressen blockiert.
- Response-Größe limitiert.
- Timeout limitiert.
- Cache auf 512 Einträge begrenzt.

Die externe URL wird anschließend an MineSkin weitergegeben. Damit kann MineSkin den Zielserver abrufen. Das ist kein direkter SSRF gegen den Minecraft-Host, aber eine externe Abuse-Trust-Boundary.

**SOLL:** externe Skin-URLs ausschließlich aus autorisierten NPC-Admin-Konfigurationen akzeptieren; optional Host-Allowlist.

---

### P2 — Keine offensichtliche Backdoor oder echter Secret-Leak

Im geprüften Source wurde keine offensichtliche Remote-Command-Backdoor und kein echter hardcodierter API-Key gefunden.

Die MineSkin-Authentifizierung kann aus `PIXELRPG_MINESKIN_API_KEY` kommen. Das ist korrekt.

`config.yml` enthält nur `CHANGE_ME`, keinen echten Secret-Wert.

---

### P2 — `DatabaseManager`: Least-Privilege verbessern

`config.yml` verwendet als Beispiel `root` und leeres Passwort als Default, während `DatabaseManager` SSL standardmäßig auf `REQUIRED` setzt.

Der SSL-Default ist gut. `root` als Beispiel ist schlecht.

**SOLL:** dedizierten DB-User mit minimalen Tabellenrechten dokumentieren und standardmäßig keinen Root-Account voraussetzen.

---

### P2 — Command Permissions

`RootCommand` prüft die `SubCommand.permission()` zentral. Administrative Commands verwenden `rpg.admin`, Member-Funktionen `rpg.member`.

Das ist grundsätzlich sauber.

Auffällig: `PlayerAdminSubCommand.setProfession()` persistiert nach der Mutation nicht explizit, während die anderen Admin-Mutationen `saveProfileAsync()` auslösen.

Das ist kein Permission-Bypass, aber ein Datenkonsistenzfehler.

---

## 4. Detaillierte Kritik der Implementierungen

### Core / Lifecycle

**`PixelRPGPlugin`** ist ein großer, aber verständlicher Composition Root. Die vielen direkten Service-Abhängigkeiten sind langfristig schwer testbar, funktionieren aber als Plugin-Architektur.

**`LifecycleCoordinator`** schließt Ressourcen rückwärts und ist idempotent. Das ist gut. Shutdown-Exceptions werden allerdings vollständig verschluckt; mindestens Logging sollte erfolgen.

**`WakeScheduler`** ist die richtige zentrale primitive für Sleep-Mode. Sie sollte künftig Lifecycle-State und Debug-Metriken besitzen, damit verlorene/überzählige Wakeups messbar werden.

---

### Player Profile

`PlayerProfile` + `PlayerProfileManager` besitzen inzwischen ein sinnvolles Revision-/Dirty-Modell. Die Save-Chain pro UUID verhindert parallele Save-Überschreibungen.

Die Architektur ist allerdings komplex genug, dass ein dedizierter Save-Coordinator langfristig sicherer wäre.

`MySQLPlayerProfileRepository` verwendet Prepared Statements und Transaktionen. Das ist gut. Die vollständigen Delete/Reinsert-Schritte sind jedoch für hohe Save-Frequenz unnötig teuer.

---

### Scoreboard

Die Event-/Dirty-Architektur ist ein deutlicher Fortschritt.

Die verbleibende Schwäche ist die Vermischung von Sidebar- und Team-State. Guild-Prefixe müssen als eigener invalidierbarer Zustand behandelt werden.

Die direkten `§0`–`§e`-Strings in `INVISIBLE_ENTRIES` sind kein Sicherheitsproblem, widersprechen aber der verbindlichen modernen Adventure-Strategie und sollten langfristig durch eine formatierungsunabhängige Entry-Strategie ersetzt werden.

---

### Quest

Die Quest-Zielverfolgung ist jetzt weitgehend ereignisorientiert. Inventory-/Mob-Kill-Trigger passen gut.

Navigation ist die verbleibende schwere Domäne. Struktur-/Biome-Lookups sind synchron und müssen deshalb aggressiver gecacht und invalidiert werden.

---

### NPC

`NpcLookTask` ist aktuell eine der gelungensten Optimierungen:

```text
PlayerMoveEvent
→ Blockwechsel
→ lokale Nearby-Entity-Suche
→ NPC-Filter
→ Nearby-Player-Suche
```

Damit ist der frühere NPC-Globalpoller strukturell beseitigt.

---

### Companion

Die Companion-Komponenten sind modular, aber `CompanionFollowTask` übernimmt weiterhin zu viele Verantwortlichkeiten. Die Runtime sollte Orchestrator sein, nicht gleichzeitig Movement-, Equipment-, Stats- und Combat-State verwalten.

---

### Combat / Stats

`CombatDamageListener` ist ein sehr häufiger Event-Listener, die Kernrechnung ist aber überwiegend O(1) und verwendet den `StatEngine`-Cache.

`CombatDamageCalculator` ist klein und deterministisch.

Der Schwachpunkt ist Mob Scaling, weil dort trotz Event-Modell zu viel Zustand pro Treffer neu berechnet wird.

---

### Boss

Zeitbasierte Boss-Tasks sind legitim. Attack-Intervals und Phasen sind echte Zeitbedingungen.

Nicht legitim für High-Scale ist jedoch ein globaler Welt-Entity- oder Welt-Spieler-Scan innerhalb dieser Tasks. Boss-Viewer und Add-Entities müssen indexiert/räumlich begrenzt sein.

---

### Region

Region-Geometrie und Policy sind ordentlich getrennt. `RegionSpawnService` besitzt aber noch den falschen Index: Region/Spawnpoint kennt nicht direkt die relevanten Spieler/Chunks.

Das ist ein guter Kandidat für eine spätere Chunk-basierte Dirty-Architektur.

---

### GUI / Inventory

GUI-Code ist event-driven, aber Inventory-Mutationen müssen adversarial getestet werden. Gerade Trade, Bank und Companion Equipment sind wirtschaftlich relevant.

---

### Memory

Es gibt keine offensichtliche dauerhafte Player-Referenz-Leak-Struktur.

Gut:

- UUIDs statt `Player` als langfristige Keys.
- Quit-Cleanup in mehreren Runtime-Systemen.
- Scheduler-Cleanup beim Shutdown.
- Quest-/NPC-Marker werden entfernt.

Weiter härten:

- Mob Scaling zusätzlich bei Entity-Unload/Invalidation bereinigen.
- Navigation-Cache maximal begrenzen.
- Async-Pending-Snapshots bei dauerhaft defektem Storage begrenzen/monitoren.

---

## 5. Konkreter Action-Plan

### Sofort — P0

1. **Trade Depot auf echte transaktionale Economy-State-Maschine umstellen.**
2. **Trade-Expiry bei vollem Handelsfach mit Retry/Pending-Claim reparieren.**
3. **Bank/Trade-Crash-Recovery mit atomaren Snapshots oder Journal absichern.**

### Als Nächstes — P1

4. Scoreboard-Guild-Prefixe über Guild-Mitgliederindex statt globale Online-Scans aktualisieren.
5. `MobLevelScalingListener` auf `Player → Mobs`-Index + Cached Scaling State umbauen.
6. Bossbar auf Nearby-Player-Queries umstellen.
7. World-Boss-Adds direkt im `ActiveBoss` indexieren.
8. Region-Spawnpunkte nach Welt/Chunk indexieren.
9. Quest-Navigation stärker cachen und nur bei relevanten Invalidierungen neu berechnen.
10. Companion Runtime in Movement/Combat/Equipment/Visual Dirty-State trennen.
11. Companion-/Bank-/Trade-GUIs mit vollständiger Inventory-Action-Testmatrix prüfen.
12. `AsyncFileWriter` Shutdown- und Crash-Atomicity reparieren.

### Danach — P2

13. MySQL-Write-Amplification durch Delta-Persistenz reduzieren.
14. Stale Stats beim Reset/Save löschen.
15. Admin-Mutationen einheitlich persistieren.
16. Lifecycle-Exceptions loggen.
17. Navigation-/Async-Caches mit harten Limits versehen.
18. Legacy-`§`-Entry-IDs aus dem Scoreboard entfernen.

### Empfohlene Lasttest-Matrix

Vor einer Produktionsfreigabe:

```text
100 Spieler
+ 100 aktive Scoreboards
+ 100 aktive Quests
+ 100 aktive Companions
+ 100+ gleichzeitig kämpfende Mobs
+ mehrere aktive Bosses
+ viele Region-Spawnpunkte
+ parallele Bank-/Trade-Aktionen
+ Playtime-Autosave
+ Chunk-Wechsel / Teleports
```

Messen mit Spark bzw. Tick-Profiler:

- Median MSPT.
- P95 MSPT.
- P99 MSPT.
- Tick-Overruns > 50 ms.
- Allocation rate.
- GC pauses.
- SQL latency.
- Async executor queue depth.
- Anzahl aktiver Wakeups.
- Anzahl aktiver Boss-/Companion-Runtimes.

### Endurteil

**Der `test`-Stand ist kein Server-Killer mehr.** Die wichtigste Architekturentscheidung — weg von globalem Dauer-Polling hin zu Event-/Dirty-/Wake-Modellen — war richtig und zeigt bereits Wirkung.

**Er ist aber noch nicht der finale High-Scale-Core.** Die nächsten Optimierungen müssen nicht weitere Scheduler entfernen, sondern die verbleibenden Eventpfade auf **O(1), O(local-nearby) oder gezielt indexierte Mengen** reduzieren und Economy-Operationen crash-atomar machen.

Die Zielregel für den Core lautet:

```text
EVENT
  ↓
INVALIDATION
  ↓
COALESCED ONE-SHOT WAKEUP
  ↓
O(1) / O(local-nearby) UPDATE
  ↓
ASYNC PERSISTENCE / TRANSACTIONAL COMMIT
```

Nicht:

```text
EVENT
  ↓
SCAN ALL PLAYERS
  ↓
SCAN ALL ENTITIES
  ↓
SCAN ALL QUESTS
  ↓
SERIALIZE ALL DATA
  ↓
WRITE ALL DATA
```

**Freigabeurteil: Build/CI = grün. Architektur = deutlich verbessert. Produktionsfreigabe für 100+ Spieler bei garantierten 20.00 TPS = noch nicht freigeben.**
