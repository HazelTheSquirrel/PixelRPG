# 🚨 Forensisches Audit: PixelRPG (Test-Branch)

**Audit-Ziel:** Paper 26.2 / Java 25 / Mojang-Mappings / `paper-plugin.yml` / 100+ gleichzeitig aktive Spieler.

**Geprüfter Branch:** `test`  
**Head:** `c3fe3330fe8667265f6b0f6943bdac761ccf96a5`  
**Referenz:** `main`  
**Branch-Differenz:** `test` ist 160 Commits vor `main` und 2 Commits dahinter; der Vergleich zeigt, dass `audit.md` auf `main` aus dem Test-Zweig entfernt wurde. Die aktuelle Analyse basiert auf dem tatsächlichen aktuellen `test`-Code, nicht auf dem alten Audit.

> **Hinweis zu Zeilennummern:** Die GitHub-Connector-Ausgabe liefert die Java-Dateien in dieser Umgebung als escaped Content statt als normale Source-Zeilen. Deshalb werden Befunde präzise über **Datei + Methode/Feld/Codepfad** referenziert. Es werden bewusst keine erfundenen Zeilennummern angegeben.

---

## 1. Zusammenfassung & Skalierbarkeit (100+ Spieler)

## Klares Urteil: **NICHT PRODUCTION-READY. Kein unmittelbarer „Plugin tötet jeden Server“-Defekt, aber unter 100+ Spielern existieren mehrere reale Main-Thread-I/O- und O(n²)-Pfade, die 20.00 TPS nicht garantieren.**

Der aktuelle `test`-Stand ist deutlich besser als ein typischer Bukkit-Prototyp. Positiv sind insbesondere:

- exakt gepinnter Paper-26.2-Dev-Bundle,
- Java-25-Toolchain,
- `paper-plugin.yml`,
- Adventure Components statt `ChatColor`,
- PDC-basierte RPG-Item-Identität,
- Snapshot-basierte `PlayerProfile`-Persistenz,
- UUID-basierte Maps,
- per-Spieler Save-Serialisierung,
- HikariCP,
- Prepared Statements,
- transaktionale MySQL-Profil-Saves,
- Chunk-basierter Region-Index,
- begrenzter External-Skin-Cache,
- zentrale Lifecycle-Koordination,
- native Paper-Dialog-API.

Die Build-Konfiguration erfüllt die geforderte technische Basis: Paper 26.2, Java 25, paperweight `2.0.0-beta.21`, Shadow `9.6.1`, Gson `2.13.1`, HikariCP `7.0.2` und MySQL Connector/J `9.7.0`. Die Source-/Shading-Verifikationen bleiben aktiv. fileciteturn2file0L2-L6

Die Architektur hat aber noch mehrere harte Skalierungsprobleme.

### Gesamtbewertung

| Bereich | Urteil | Risiko |
|---|---|---:|
| Build-/Dependency-Basis | gut | niedrig |
| Player-Profil-Snapshotting | deutlich verbessert | mittel |
| MySQL-Transaktion | gut, aber teuer | mittel |
| YAML-Bank/Trade-Storage | **nicht skalierbar** | **kritisch** |
| Scoreboard | funktional, aber O(P²) | **hoch** |
| Companion Runtime | funktional, aber tick-intensiv | **hoch** |
| NPC Look Task | unnötig teuer bei vielen NPCs | hoch |
| Quest Passive Task | potenziell teuer | hoch |
| Mob Scaling | CPU-lastig bei vielen aktiven Mobs | hoch |
| Guild Service API | **semantisch fehlerhaft/konfligierend** | **kritisch** |
| Lifecycle | gut | niedrig |
| Region Index | grundsätzlich gut | niedrig/mittel |
| Security/SSRF | brauchbare Schutzmaßnahmen, aber Lücken | mittel |
| Trade-Duplikationsschutz | **unzureichend für persistenten Markt** | **kritisch** |
| Folia-Unterstützung | **nicht vorhanden** | mittel |

Paper/Folia selbst verlangt für Folia ausdrücklich die passenden Global-, Region-, Async- und Entity-Scheduler; BukkitScheduler-basierte globale Tasks sind keine Folia-Architektur. PixelRPG setzt aktuell `folia-supported: true` nicht und sollte daher auch nicht behaupten, Folia-kompatibel zu sein. citeturn0search0turn0search6

### 100-Spieler-Simulation

Bei 100 Online-Spielern laufen bereits mehrere wiederkehrende Main-Thread-Pfade:

- Scoreboard ungefähr einmal pro Sekunde für **alle Spieler**.
- Quest-Passivprüfung alle 40 Ticks für **alle Spieler**.
- Companion Runtime alle 2 Ticks für jeden aktiven Companion.
- NPC-Look-System alle 5 Ticks für jeden gespawnten NPC.
- Mob-Level-Cleanup jede Sekunde über alle aktiven skalierten Mobs.
- Boss-/Region-/Spawn-/Party-/Playtime-/Navigation-Systeme parallel dazu.

Die reine Anzahl der Tasks ist nicht das Problem. Das Problem ist, dass mehrere Tasks jeweils **weitere lineare Scans** durchführen. Besonders schlimm ist das Scoreboard, das für jeden Spieler erneut über alle Online-Spieler läuft.

Ein Server mit 100 Spielern kann damit durchaus weiterhin 20 TPS schaffen, wenn NPC-, Mob-, Companion- und Quest-Daten klein bleiben. **Eine belastbare 20.00-TPS-Garantie gibt der Code aber nicht.** Bei 100 Spielern plus vielen NPCs, Mobs und aktiven Companions ist die Last nicht mehr konservativ genug.

---

## 2. Kritische Performance-Engpässe & Threading-Fehler

### 2.1 CRITICAL — `BankStorageService` macht komplette YAML-Datei-Schreibvorgänge synchron

**Datei:** `src/main/java/de/pixelrpg/rpg/dialogue/BankStorageService.java`  
**Methoden:** `save`, `saveTradeGoods`, `addTradeGoods`, `saveTo`, `write`

Der gesamte persönliche Bankbestand und das Handelsfach werden als große `YamlConfiguration` gehalten. Bei jedem Speichern wird der Spielerbereich entfernt, neu serialisiert und anschließend die Datei synchron geschrieben. `write()` ruft direkt `target.save(file)` auf. fileciteturn31file0L2-L6

`BankInventoryListener.onBankInventoryClose()` ruft diesen Pfad direkt beim `InventoryCloseEvent` auf. fileciteturn32file0L2-L6

Das ist ein klassischer Main-Thread-I/O-Bug.

#### Lastszenario

100 Spieler öffnen/schließen das Bank-GUI nahezu gleichzeitig:

```text
100 x InventoryCloseEvent
        ↓
100 x YAML mutation
        ↓
100 x target.save(file)
        ↓
100 x komplette Datei serialisieren + schreiben
        ↓
Main Thread wartet
```

Das ist nicht akzeptabel für einen Server, der 20.00 TPS als harte Zielgröße fordert.

### Korrekte Zielarchitektur

```text
Main Thread
   ↓
immutable BankSnapshot
   ↓
per-player / keyed async write queue
   ↓
atomic temp file
   ↓
move/replace
```

Noch besser: Bankdaten in dieselbe MySQL-Infrastruktur integrieren und nur einen einzigen persistenten Storage-Layer verwenden.

---

### 2.2 CRITICAL — `TradeDepotManager` schreibt synchron auf Disk

**Datei:** `src/main/java/de/pixelrpg/rpg/trade/TradeDepotManager.java`  
**Methoden:** `save`, `purchase`, `cancel`, `expireListings`, `createListingFromResponse`, `open`, `shutdown`

`save()` erstellt bei jedem Aufruf eine neue `YamlConfiguration`, serialisiert sämtliche Listings und Pending Payouts und schreibt anschließend synchron mit `yaml.save(file)`. fileciteturn30file0L2-L6

Besonders problematisch:

- Kauf → `save()`
- Verkauf → `save()`
- Cancel → `save()`
- Expire → `save()`
- `open()` kann über `expireListings()` ebenfalls `save()` auslösen.

Das ist ein Main-Thread-Disk-Writer hinter einem Spieler-Interaktionssystem.

### Security-/Dupe-Relevanz

`purchase()` ist zwar auf dem normalen Serverthread seriell und entfernt das Listing vor dem Speichern, aber es besitzt keine persistente Transaktionsgrenze zwischen:

1. Geldabzug,
2. Listing removal,
3. Verkäufergutschrift,
4. BankStorage-Einlagerung,
5. Markt-Speicherung.

Bei einem Prozessabsturz zwischen diesen Operationen kann der In-Memory-Zustand von persistentem Zustand abweichen.

Für einen persistenten Marktplatz ist das nicht robust genug.

### SOLL

Eine Markttransaktion muss atomar sein:

```text
BEGIN
  lock listing
  validate listing state
  debit buyer
  credit seller / create payout
  transfer item
  mark listing SOLD
COMMIT
```

Bei YAML muss mindestens ein Write-Ahead-/Journal-Mechanismus existieren. Für ein MMORPG ist DB-Transaktion die korrekte Lösung.

---

### 2.3 HIGH — `ScoreboardService.applyGuildPrefixes()` ist O(P²)

**Datei:** `src/main/java/de/pixelrpg/rpg/scoreboard/ScoreboardService.java`  
**Methode:** `applyGuildPrefixes`

`startTask()` iteriert über alle Online-Spieler. Für jeden Spieler wird `apply(player, profile)` aufgerufen. `apply()` ruft wiederum `applyGuildPrefixes(state)` auf. Diese Methode iteriert erneut über **alle Online-Spieler**. fileciteturn29file0L2-L6

Damit entsteht:

```text
P Spieler
×
P Spieler im Guild-Prefix-Loop
=
O(P²)
```

Bei 100 Spielern sind das bereits 10.000 Iterationen pro Update. Bei 200 sind es 40.000. Zusätzlich werden synchronisierte `GuildManager.getGuild()`-Zugriffe, Team-Operationen und Component-Erzeugung durchgeführt.

### SOLL

Guild-Scoreboard-Daten einmal pro Tick/Update berechnen:

```java
Map<UUID, GuildView> guildViews = buildGuildViewsOnce();

for (Player player : onlinePlayers) {
    applyScoreboard(player, guildViews);
}
```

Noch besser: Guild-Mitgliedschaftsänderungen markieren einen Dirty-State und aktualisieren nur betroffene Scoreboards.

---

### 2.4 HIGH — `NpcLookTask` scannt für jeden NPC seine Umgebung

**Datei:** `src/main/java/de/pixelrpg/rpg/npc/NpcLookTask.java`  
**Methode:** `tick`

Der Task läuft standardmäßig alle 5 Ticks. Für jeden gespawnten NPC wird `getNearbyEntities(radius, radius, radius)` aufgerufen und anschließend jeder gefundene Entity-Datensatz geprüft. fileciteturn11file0L2-L6

Bei 100 NPCs:

```text
20 Updates/s × 100 NPCs = 2.000 NPC-Scans/s
```

Wenn sich viele Spieler in NPC-Nähe befinden, steigt die Arbeit weiter.

### SOLL

- nur NPCs aktualisieren, die mindestens einen Spieler in Sichtweite haben,
- Spieler → nahe NPCs indexieren statt NPC → NearbyEntities zu scannen,
- Update-Rate dynamisch reduzieren,
- Rotation nur bei tatsächlicher Winkeländerung schreiben,
- Entity-Scheduler verwenden, wenn Entity-/Folia-Unterstützung später vorgesehen ist.

Paper dokumentiert ausdrücklich, dass Entity-Scheduler für Entity-gebundene Tasks auf Folia erforderlich ist. citeturn0search0

---

### 2.5 HIGH — `CompanionFollowTask` ist ein permanenter Entity-CPU-Loop

**Datei:** `src/main/java/de/pixelrpg/rpg/companion/CompanionService.java` / `CompanionFollowTask.java`  
**Methoden:** `CompanionService` constructor, `CompanionFollowTask.run`, `updateRuntimeState`, `equipmentHash`, `follow`, `moveTowards`

Der Runtime-Task läuft alle 2 Ticks. fileciteturn8file0L2-L6

Pro Companion werden u. a. durchgeführt:

- Bukkit Entity Lookup,
- Player Lookup,
- PDC Lookup,
- Companion-Definition Lookup,
- Equipment-Hash,
- mehrere Location-Operationen,
- Distanzberechnung,
- Combat Controller,
- Follow-Physik,
- Block-Solid-Abfragen,
- Velocity/Rotation-Updates.

`equipmentHash()` ruft `companionService.getEquipment()` auf. Das ist zwar nach dem ersten Zugriff gecacht, aber die Methode erzeugt weiterhin Hashing-/Clone-Arbeit. fileciteturn7file0L2-L6

Bei 100 aktiven Companions sind es 50 Runtime-Durchläufe pro Sekunde. Bei 500 sind es 250/s.

### SOLL

- Runtime nur für tatsächlich aktive/geladene Companions,
- keine wiederholte PDC-/Definition-Auflösung,
- Equipment-Revision statt `Objects.hash(ItemStack...)`,
- Bewegung nur bei Zustandsänderung,
- Combat-Tickrate getrennt von Follow-Tickrate,
- Targeting nicht jedes Runtime-Tick neu berechnen,
- Entity-Scheduler für Folia-fähige Architektur.

---

### 2.6 HIGH — Companion Equipment enthält weiterhin synchrone Disk-Lesezugriffe im Runtime-Pfad

**Datei:** `CompanionEquipmentStore.java` / `CompanionService.java`  
**Methoden:** `hasEntry`, `load`, `loadFile`, `getEquipment`

`CompanionEquipmentStore.save()` ist korrekt asynchron. Aber `hasEntry()` und `load()` lesen YAML synchron. `CompanionService.getEquipment()` ruft genau diese Methoden auf, wenn der Cache noch nicht gefüllt ist. fileciteturn23file0L2-L6

Der erste Zugriff kann damit auf dem Main Thread eine Datei laden.

Das ist besonders schlecht, weil `CompanionFollowTask` und UI-/Combat-Code dieselbe Service-Schicht verwenden.

### SOLL

Equipment muss beim Player-Load geladen werden oder explizit async vorgeladen werden:

```text
Player pre-login
    ↓
Profile + Companion Equipment async load
    ↓
activateOnJoin
    ↓
Runtime liest nur RAM
```

Kein Disk-I/O in `getEquipment()`.

---

### 2.7 HIGH — Quest-Passivtask macht mehrere vollständige Player-Operationen

**Datei:** `QuestPassiveCheckTask.java`  
**Methode:** `start`

Alle Online-Spieler werden alle `intervalTicks` iteriert. Pro Spieler werden drei potenziell nichttriviale Operationen ausgeführt:

```java
inventoryTracker.refresh(player);
questManager.checkReachLocationQuests(player);
navigationService.refresh(player);
```

fileciteturn10file0L2-L6

Das ist bei 100 Spielern noch beherrschbar, aber die drei Operationen müssen jeweils O(1) bzw. sehr klein sein. Das ist derzeit nicht als harte Architekturgarantie abgesichert.

### SOLL

Event-getriebene Quest-Fortschritte bevorzugen:

- BlockBreakEvent → relevante Quests,
- EntityDeathEvent → relevante Quests,
- InventoryClickEvent → relevante Item-Quests,
- RegionEnter → Reach-Quest,
- Navigation nur bei tatsächlicher Positionsänderung.

Ein globaler Polling-Loop sollte der letzte Ausweg sein.

---

### 2.8 HIGH — Mob Scaling skaliert CPU mit aktiven Mobs × Teilnehmern

**Datei:** `MobLevelScalingListener.java`  
**Methoden:** `applyScaling`, `restoreExpiredScaling`, `gearLevelMultiplier`

`restoreExpiredScaling()` läuft jede Sekunde über alle aktiven Mobs. `applyScaling()` berechnet für alle Teilnehmer das höchste Level und anschließend erneut Gear-Multiplikatoren. Für jeden Spieler werden Inventar-Items extrahiert und Item-Metas/PDC gelesen. fileciteturn12file0L2-L6

Der Code verwendet zwar `ConcurrentHashMap`, aber der Task selbst läuft synchron. Thread-Safety der Map ist nicht der Performance-Hauptpunkt.

Das Problem ist die Arbeit:

```text
Mobs × Teilnehmer × Inventar-Items
```

Bei großen Mob-Pulls und mehreren Spielern kann das schnell mehrere tausend Item-Checks pro Sekunde erzeugen.

### SOLL

Pro Spieler einen gecachten `GearScalingSnapshot` führen:

```text
Equipment change
    ↓
recalculate gear multiplier once
    ↓
cache by player UUID
```

Monster-Skalierung darf anschließend nur noch den gecachten Faktor lesen.

---

### 2.9 MEDIUM — `PlayerProfileManager` ist jetzt wesentlich besser, aber die Persistenz ist weiterhin teuer

**Datei:** `PlayerProfileManager.java`  
**Methoden:** `captureAndEnqueue`, `enqueue`, `persistSnapshot`

Die aktuelle Implementierung macht etwas Wichtiges richtig: `PlayerProfile.snapshotForSave()` ist synchronisiert und erstellt eine Kopie des Profils einschließlich Collections, QuestProgress und ItemStacks. Dadurch wird nicht mehr die Live-Instanz direkt in den Repository-Thread gereicht. fileciteturn26file0L2-L6

Das ist eine klare Verbesserung gegenüber dem alten Design.

Auch `saveRequested` und die UUID-spezifische `saveChain` sorgen dafür, dass Saves pro Spieler seriell bleiben. fileciteturn25file0L2-L12

### Verbleibende Risiken

- Jeder Save serialisiert potentiell das komplette Profil.
- MySQL-Saves löschen und rekonstruieren aktive Quests und Equipment.
- Statistikdaten werden breit upserted.
- Vier parallele I/O-Threads können bei 100 Spielern hohe DB-Last erzeugen.
- `loadForPreLogin()` wartet synchron bis zum Timeout auf den I/O-Future. Das blockiert nicht den Server-Tick, kann aber Login-/Connection-Threads unnötig lange halten.

Das Snapshot-Modell ist akzeptabel. Es braucht jetzt nur noch **Dirty-field / incremental persistence**, wenn die Datenmenge weiter wächst.

---

### 2.10 HIGH — MySQL-Save schreibt zu viel

**Datei:** `MySQLPlayerProfileRepository.java`  
**Methode:** `save`

Der Save macht in einer Transaktion:

1. `SELECT ... FOR UPDATE`
2. Player-Upsert
3. komplette Active-Quest-Löschung
4. komplette Active-Quest-Reinsertion
5. komplette Equipment-Löschung
6. komplette Equipment-Reinsertion
7. Statistik-Upserts
8. alle Profession-Statistiken
9. alle freigeschalteten Rezepte
10. Commit

fileciteturn6file0L2-L6

Das ist korrektheitsorientiert, aber nicht effizient.

Bei 100 Spielern und häufigen Saves wird unnötig viel DB-Arbeit erzeugt.

### Noch wichtiger: stale Stats / Recipes

Der Save macht `UPSERT`, aber entfernt nicht explizit alte `pixelrpg_player_stats`-Rows, die aus dem Snapshot verschwunden sind.

Damit können entfernte:

- Statistiken,
- Profession-Flags,
- Recipe-Unlocks

in der Datenbank verbleiben und beim nächsten Load wieder auftauchen.

Das ist ein **persistenter Datenintegritätsfehler**.

### SOLL

Entweder:

```text
profile snapshot contains full authoritative state
→ DELETE obsolete rows
→ INSERT current rows
```

oder besser:

```text
dirty professions
 dirty recipes
 dirty stats
→ update only changed records
```

Für Geld/Inventar/Progression muss eine klare Autoritätsregel gelten.

---

### 2.11 MEDIUM — `DatabaseManager` ist sicherer geworden, aber Credentials und TLS brauchen Betriebsdisziplin

`DatabaseManager` validiert Host, Port, Datenbankname und SSL-Modus. Der Default ist `REQUIRED`, was deutlich besser ist als eine pauschale SSL-Deaktivierung. Hikari-Timeouts, Poolgröße, Leak Detection und Lifetime sind ebenfalls konfiguriert. fileciteturn9file0L2-L6

Die URL wird aber weiterhin aus Konfigurationswerten zusammengesetzt. Das ist für lokale Serverkonfiguration vertretbar, aber Secrets gehören nicht in Git-committete Config-Dateien.

**Audit-Regel:** Kein Passwort, Token oder API-Key darf im Repository liegen.

Die aktuelle External-Skin-Implementierung akzeptiert alternativ `PIXELRPG_MINESKIN_API_KEY`, was die richtige Richtung ist. fileciteturn24file0L2-L6

---

### 2.12 MEDIUM — RegionManager ist deutlich besser als ein globaler Regionscan

**Datei:** `RegionManager.java`

Der aktuelle `find()`-Pfad nutzt einen Chunk-Index und prüft nur Kandidaten des aktuellen Chunks. Das ist für Runtime-Lookups grundsätzlich die richtige Datenstruktur. fileciteturn13file0L2-L6

Auch `isExplicitSpawnPoint()` nutzt inzwischen den Spawnpoint-Index. Der alte „alle Regionen durchsuchen“-Fehler ist im aktuellen Test-Code nicht mehr vorhanden.

### Verbleibendes Problem

`create/delete` aktualisieren bei großen Region-Bounding-Boxes sehr viele Chunk-Index-Einträge. Das ist für Admin-Operationen akzeptabel, aber nicht während normalen Player-Ticks.

Die Persistenz wird korrekt auf einen eigenen Executor gelegt. Das ist gut.

---

### 2.13 MEDIUM — `GuildManager` blockiert nicht auf Disk, aber benutzt global synchronisierte Methoden

Guild-Persistenz wird auf einem eigenen Executor serialisiert. Das ist korrekt. fileciteturn16file0L2-L6

Allerdings sind praktisch alle Getter `synchronized`. In Kombination mit `ScoreboardService.applyGuildPrefixes()` wird daraus bei 100 Spielern unnötige Lock-Konkurrenz auf dem Main Thread.

Die richtige Lösung ist ein immutable/read-mostly Guild-Snapshot:

```text
GuildState immutable

write:
  replace snapshot atomically

read:
  lock-free / volatile reference
```

---

## 3. Sicherheitslücken & Exploit-Potenzial

### 3.1 CRITICAL — GuildAPI-Service-Kollision

**Dateien:**

- `PlayerProfileManager.java`
- `GuildManager.java`
- `GuildAPI.java`
- `CompanionFollowTask.java`
- `MobLevelScalingListener.java`

`GuildAPI` ist semantisch überhaupt keine reine Guild-API. Es enthält:

```text
isRegistered
getLevel
getExperience
addExperience
```

fileciteturn17file0L2-L6

`PlayerProfileManager` registriert sich als `GuildAPI`. Danach registriert sich auch `GuildManager` als `GuildAPI`. Beide verwenden dieselbe ServicePriority `Normal`. fileciteturn25file0L2-L6 fileciteturn16file0L2-L6

Das ist architektonisch falsch und kann funktional katastrophal werden.

`CompanionFollowTask` lädt `GuildAPI` aus dem ServiceManager und verwendet `getLevel()` für die Companion-Levelauflösung. fileciteturn7file0L2-L6

`MobLevelScalingListener` verwendet ebenfalls `GuildAPI.getLevel()`.

`GuildManager.getLevel()` gibt jedoch bei Guild-Mitgliedschaft lediglich `1` zurück und sonst `0`. fileciteturn16file0L2-L6

Damit kann das Verhalten davon abhängen, welche `GuildAPI`-Implementierung der ServiceManager zurückliefert.

### Das ist kein Style-Problem. Das ist ein echter Runtime-Bug.

### SOFORTIGE Korrektur

`GuildAPI` muss in zwei unabhängige Interfaces zerlegt werden:

```java
public interface PlayerProgressionAPI {
    boolean isRegistered(UUID uuid);
    int getLevel(UUID uuid);
    long getExperience(UUID uuid);
    void addExperience(UUID uuid, long amount);
}
```

und:

```java
public interface GuildAPI {
    Optional<Guild> getGuild(UUID playerId);
    boolean isMember(UUID guildId, UUID playerId);
    Set<UUID> getMembers(UUID guildId);
}
```

Danach darf nur jeweils eine konkrete Implementierung den jeweiligen Service bereitstellen.

---

### 3.2 HIGH — Trade-Depot hat keine DB-Transaktion

Wie oben beschrieben ist der Kauf logisch mehrstufig. Ein Servercrash kann zu Inkonsistenzen führen.

Besonders kritisch ist:

```text
buyerProfile.removeMoney(price)
listings.remove(listingId)
sellerProfile.addMoney(...)
bankStorage.addTradeGoods(...)
save()
```

fileciteturn30file0L2-L6

Es existiert keine gemeinsame Commit-Grenze.

Das ist ein **Crash-Dupe-/Item-Loss-Risiko**, nicht zwingend ein normaler Race-Condition-Dupe.

---

### 3.3 HIGH — Trade-Listing basiert auf vollständiger ItemStack-Similarity, nicht auf unveränderlicher Listing-ID im Itemzustand

Beim Einstellen wird das Item aus dem Inventory entfernt und als `ItemStack` im Listing gespeichert. Beim Kauf wird eine Kopie ausgeliefert. Das ist grundsätzlich okay.

Die Auswahl im Dialog wird gegen den aktuellen Inventory-Slot geprüft:

```text
isTradeableRpgItem(current)
current.isSimilar(selectedItem)
current.getAmount() == selectedItem.getAmount()
```

fileciteturn30file0L2-L6

Das verhindert viele triviale UI-Stale-State-Probleme.

Aber für ein wirklich robustes Handelsmodell sollte die Dialogaktion eine serverseitige, kurzlebige Listing-/Selection-Token-ID besitzen. Ein Client sollte nicht durch rekonstruierte ItemStack-Daten die Autorität über die Auswahl erhalten.

---

### 3.4 HIGH — Companion Equipment ist dateibasiert und besitzt keine transaktionale Konsistenz mit Companion-State

Companion-Fortschritt und Companion-Equipment werden in unterschiedlichen Speichern/Queues behandelt. fileciteturn8file0L2-L6 fileciteturn23file0L2-L6

Crash-Szenario:

```text
Companion progression save erfolgreich
Equipment save noch pending
Server crash
```

→ Companion und Equipment können unterschiedliche Zeitstände besitzen.

Für kosmetisches Equipment ist das tolerierbar. Für gameplayrelevantes Equipment nicht.

---

### 3.5 MEDIUM — External Skin: DNS-TOCTOU bleibt theoretisch möglich

`ExternalSkinService.isSafeExternalHost()` löst DNS auf und blockiert private/loopback/link-local/site-local Adressen. Das ist gut. Redirects sind deaktiviert. Response-Größe und Timeout sind limitiert. fileciteturn24file0L2-L6

Aber DNS-Sicherheit ist grundsätzlich eine TOCTOU-Problematik: Die Adresse, die während der Validierung aufgelöst wird, muss nicht exakt der Adresse entsprechen, die ein späterer HTTP-Stack verwendet.

Für die aktuelle Architektur ist das Risiko begrenzt, weil externe Bild-URLs über MineSkin verarbeitet werden und nicht direkt vom Plugin heruntergeladen werden.

### SOLL

Nur explizit erlaubte Hostnames akzeptieren, z. B.:

```text
textures.minecraft.net
```

oder eine strikte Allowlist.

---

### 3.6 MEDIUM — API-/Command-Boundaries müssen konsequent serverseitig validieren

Die GUI-Schichten machen bereits viele Validierungen. Das ist gut.

Aber jede Economy-/Inventory-/Quest-Operation muss unabhängig vom UI validieren. Eine GUI ist niemals eine Security Boundary.

Das gilt insbesondere für:

- Shop-Kauf,
- Crafting,
- Guild-Erstellung,
- Trade-Depot,
- Quest-Admin,
- Companion-Admin,
- Region-Editor.

`CraftingService.craft()` validiert beispielsweise Beruf, Level, Unlock-State, Materialkosten und Unique-Rarity selbst. Das ist korrekt. fileciteturn15file0L2-L6

---

### 3.7 MEDIUM — Crafting ist im Main Thread und bewusst synchron

**Datei:** `CraftingService.java`  
**Methode:** `craft`

Das ist grundsätzlich richtig, weil Inventory-Manipulationen und Bukkit-World-State nicht async durchgeführt werden dürfen. Paper weist ausdrücklich darauf hin, dass große Teile der Bukkit-API async nicht threadsicher sind. citeturn0search6

Die Methode scannt allerdings mehrfach das komplette Storage-Inventory:

- `hasMaterialCosts`
- `hasItemCosts`
- `removeMaterialCosts`
- `removeItemCosts`

Bei einem einzelnen Craft ist das klein. Ein Spam-Angriff über extrem schnelle Clicks kann aber unnötige CPU-Arbeit erzeugen.

### SOLL

Eine einzige Inventory-Analyse erstellen:

```text
InventorySnapshot
  ├─ Material counts
  └─ RPG item-id counts
```

und anschließend atomar verbrauchen.

---

### 3.8 Keine offensichtlichen Hardcoded Backdoors gefunden

Im geprüften Code wurden keine offensichtlichen:

- Remote-Command-Backdoors,
- versteckten OP-Vergaben,
- fest eingebauten MySQL-Passwörter,
- Token-Strings,
- `Runtime.exec()`-Pfade,
- beliebigen SQL-Konkatenationen für Spielerinputs

als offensichtlicher Backdoor-Mechanismus identifiziert.

Die Datenbank nutzt Prepared Statements und validiert kritische Konfigurationsbestandteile. fileciteturn6file0L2-L6 fileciteturn9file0L2-L6

Das ist ein klarer Pluspunkt.

---

## 4. Detaillierte Kritik der Implementierungen

### 4.1 `PixelRPGPlugin` — zu großer Composition Root

`PixelRPGPlugin` verdrahtet praktisch das komplette System: Player, Stats, Profession, Items, Quests, Regionen, Bosse, NPCs, Companion, Scoreboard, Guilds, Commands und Listener. fileciteturn4file0L2-L6

Als Composition Root ist das legitim. In dieser Größe wird es aber gefährlich.

Der Code ist faktisch ein manuelles Dependency-Injection-Framework.

### SOLL

Domänenmodule sollten einen eigenen `Module`-Installer besitzen:

```text
PlayerModule
QuestModule
ItemModule
CombatModule
NpcModule
RegionModule
EconomyModule
```

`PixelRPGPlugin` macht dann nur:

```java
playerModule.install(context);
questModule.install(context);
combatModule.install(context);
...
```

Keine fachliche Logik in der Plugin-Klasse.

---

### 4.2 `PlayerProfile` — gutes Synchronisationsniveau, aber zu viel Mutable State

Die Klasse synchronisiert praktisch alle Getter/Setter und erstellt tiefe Snapshots. Das verhindert viele klassische Java-Races. fileciteturn26file0L2-L6

Das ist sauberer als eine unsynchronisierte ConcurrentHashMap mit mutable Values.

Aber `synchronized` auf nahezu jedem Zugriff ist kein Ersatz für ein gutes Ownership-Modell.

### SOLL

Der Main Thread besitzt den Live-State.

Async-Threads sehen ausschließlich immutable Snapshots.

Dann kann `PlayerProfile` selbst wieder einfacher werden.

---

### 4.3 `PlayerProfile.snapshotForSave()` — gute Lösung

Diese Methode ist einer der stärksten Teile des aktuellen Codes.

Sie kopiert:

- primitive Werte,
- EnumMaps,
- Sets,
- QuestProgress,
- Statistics,
- ItemStacks.

Zusätzlich wird `dirty` auf dem Live-Profil sauber kontrolliert. fileciteturn26file0L2-L6

**Bewertung:** behalten.

---

### 4.4 `MySQLPlayerProfileRepository` — korrektheitsorientiert, aber Full-Rewrite-Charakter

Die SQL-Transaktion ist sauber mit `try-with-resources`, `setAutoCommit(false)`, `commit` und `rollback`. fileciteturn6file0L2-L6

Das ist wesentlich besser als untransaktionale Einzelupdates.

Das Hauptproblem ist nicht SQL-Injection, sondern unnötiger Write-Amplification.

### Optimierung

Ein `ProfileDelta`-Modell einführen:

```text
ProfileDelta
 ├─ moneyChanged
 ├─ experienceChanged
 ├─ professionChanges
 ├─ questChanges
 ├─ equipmentChanges
 ├─ statisticChanges
 └─ recipeChanges
```

Nur diese Änderungen werden gespeichert.

---

### 4.5 `GuildManager` — fachlich sauberer als früher, aber API-Vertrag falsch

Die synchronisierten Mutationen für Guild-Erstellung, Einladung und Annahme sind grundsätzlich korrekt. Der `MAX_MEMBERS`-Check findet vor dem Join statt und die Methoden sind serialisiert. fileciteturn16file0L2-L6

Der gravierende Fehler ist die Implementierung des falschen Interfaces `GuildAPI`.

Das muss beseitigt werden.

---

### 4.6 `CraftingService` — gute serverseitige Validierung

Positiv:

- Recipe wird serverseitig aufgelöst.
- Registrierung wird geprüft.
- Beruf wird geprüft.
- Level wird geprüft.
- Unlock wird geprüft.
- Unique-Items werden geblockt.
- Materialkosten werden geprüft.
- Item-ID-Kosten werden geprüft.
- Ergebnis wird serverseitig erzeugt.

fileciteturn15file0L2-L6

Das ist die richtige Security Boundary.

Das Crafting-System ist nicht der primäre Exploit-Kandidat.

---

### 4.7 `CraftingGUI` — funktional, aber rendert zu viel

`render()` scannt für jedes Rezept das Spielerinventar für Material- und Item-ID-Kosten. Bei vielen Rezepten wird daraus:

```text
Rezepte × Inventarslots
```

Der Spieler öffnet das GUI und der komplette Inhalt wird berechnet.

Bei 50 Rezepten × 36 Slots sind das bereits 1.800 Slot-Checks pro Render. Bei jeder Seitenaktion wird erneut gerendert. fileciteturn14file0L2-L6

### SOLL

Ein `InventoryCostSnapshot` einmal erstellen und für alle Rezepte verwenden.

---

### 4.8 `ScoreboardService` — unnötige Bukkit-Objektarbeit

Neben O(P²) erzeugt `buildLines()` regelmäßig neue Components und ruft `CompanionService.getActive()` auf. `appendPartyLine()` verwendet außerdem `Bukkit.getOfflinePlayer()` für Party-Mitglieder. fileciteturn29file0L2-L6

Die Scoreboard-Daten sollten event-/dirty-getrieben sein.

---

### 4.9 `CompanionService` — zu viele Verantwortlichkeiten

Die Klasse vereint:

- Companion Registry,
- Persistence,
- Ownership,
- Runtime Entity Lifecycle,
- Equipment,
- Progression,
- Rename,
- Spawn/Despawn,
- Mount Integration.

fileciteturn8file0L2-L6

Das ist zu viel für eine Klasse.

### SOLL

Aufteilen in:

```text
CompanionRepository
CompanionProgressionService
CompanionRuntimeService
CompanionEquipmentService
CompanionSpawnService
CompanionMountService
```

---

### 4.10 `RegionManager` — gutes Spatial Indexing

Der Chunk-Index ist die richtige Datenstruktur. `find()` muss nicht alle Regionen prüfen. fileciteturn13file0L2-L6

Das ist ausdrücklich **kein** Bereich, der jetzt blind refactored werden sollte.

---

### 4.11 `ExternalSkinService` — gute Schutzmaßnahmen, aber API-Aufruf ist teuer

Positiv:

- HTTPS-only für externe URLs,
- kein Userinfo/Fragment,
- Timeout,
- kein Redirect-Following,
- Response-Limit,
- begrenzter Cache,
- TTL,
- Main-Thread-Apply.

fileciteturn24file0L2-L6

Das ist für einen externen HTTP-Service ordentlich.

Verbesserung: Request-Deduplication.

Wenn 20 NPCs dieselbe neue URL gleichzeitig anfordern, sollten nicht 20 HTTP Requests entstehen:

```java
Map<String, CompletableFuture<ProfileProperty>> inFlight;
```

---

### 4.12 `LifecycleCoordinator` — gut und behalten

Die Reverse-Order-Shutdownlogik ist korrekt. `AtomicBoolean` verhindert doppeltes Schließen. Ressourcenfehler blockieren nicht den Rest des Shutdowns. fileciteturn34file0L2-L6

Das ist eine der Verbesserungen, die **nicht** wieder zurückgebaut werden sollte.

---

## 5. Konkreter Action-Plan

## P0 — MUSS VOR PRODUCTION

### 1. GuildAPI sofort trennen

**Dateien:**

- `api/GuildAPI.java`
- `player/PlayerProfileManager.java`
- `guild/GuildManager.java`
- alle `GuildAPI`-Consumer

**Ziel:** keine zwei semantisch unterschiedlichen Implementierungen unter demselben Service-Key.

---

### 2. BankStorageService komplett aus dem Main-Thread-I/O entfernen

**Dateien:**

- `BankStorageService.java`
- `BankInventoryListener.java`

**Ziel:** immutable Snapshot + async per-player persistence + atomic file replacement oder DB.

---

### 3. TradeDepot persistent transaktional machen

**Datei:** `TradeDepotManager.java`

**Ziel:** DB-Tabelle für Listings + Transaktion für Kauf/Cancel/Expire/Payout.

Minimal:

```text
trade_listings
trade_payouts
```

Kauf muss eine echte DB-Transaktion werden.

---

### 4. Scoreboard O(P²) eliminieren

**Datei:** `ScoreboardService.java`

Nicht mehr:

```text
for player
    for onlinePlayer
```

Stattdessen einmalige Guild-Sicht pro Update oder Dirty-State-Updates.

---

### 5. Companion Equipment aus Runtime-Disk-I/O entfernen

Equipment beim Login/Preload laden. Runtime nur RAM.

---

## P1 — MUSS VOR 100+ SPIELERN

### 6. NPC-Look optimieren

- Spieler-zentriertes Spatial Query.
- Adaptive Tickrate.
- keine unnötige Rotation.

### 7. Quest-Polling reduzieren

Event-getriebene Fortschrittsberechnung.

### 8. Mob-Gear-Scaling cachen

Gear-Level nicht bei jedem Monster-Scaling neu aus dem Inventory berechnen.

### 9. Crafting GUI Inventory Snapshot

Inventar nur einmal analysieren.

### 10. MySQL incremental persistence

Full-Rewrite von Quests/Equipment/Stats reduzieren.

---

## P2 — ARCHITEKTUR

### 11. `CompanionService` zerlegen

### 12. `PixelRPGPlugin` in Module-Installer zerlegen

### 13. GuildState immutable/read-mostly machen

### 14. Gemeinsame Persistence-Abstraktion

Aktuell existieren mehrere parallele Systeme:

```text
PlayerProfile → MySQL/YAML
Guild → YAML
Bank → YAML
Trade → YAML
Companion → YAML
Companion Equipment → YAML
Shop → YAML
Region → YAML
Quest → YAML
Boss → YAML
```

Das ist für ein wachsendes MMORPG ein Wartungsproblem.

Langfristig sollte ein klarer Split gelten:

```text
Static game data
    → JSON/YAML resources

Persistent player/economy state
    → MySQL

Runtime state
    → RAM only
```

---

## P3 — FOLIA-READY ARCHITEKTUR, FALLS SPÄTER GEWÜNSCHT

Der aktuelle Code ist **nicht Folia-ready**. Das ist kein Fehler, solange das Plugin ausschließlich Paper 26.2 targetiert und `folia-supported` nicht gesetzt ist.

Wenn Folia später unterstützt werden soll, müssen insbesondere folgende Systeme neu gedacht werden:

- Companion Follow
- NPC Look
- Entity Teleports
- Boss Entity Runtime
- Region Spawn
- Mob Scaling
- Player-/Entity-bezogene Tasks

Folia verlangt dafür passende Scheduler und Entity-Scheduler statt eines globalen BukkitScheduler-Modells. citeturn0search0turn0search1

---

## Performance-Zielwerte für die Abnahme

Vor einem Production-Go sollten Lasttests mindestens folgendes simulieren:

```text
100 Spieler
100 aktive Companions
200+ NPCs
500+ aktive skalierte Mobs
100+ aktive Quests
50+ Guilds
100+ Trade-Listings
100+ Bank-Operationen/min
50+ Crafting-Operationen/s Burst
```

Dabei müssen gemessen werden:

- MSPT p50
- MSPT p95
- MSPT p99
- GC pause time
- Allocation rate
- DB latency
- DB connection pool saturation
- async queue depth
- main-thread disk I/O
- Anzahl synchroner YAML writes
- Anzahl Entity lookups pro Tick
- Anzahl Inventory scans pro Tick

### Harte Abnahmekriterien

```text
p95 MSPT < 35 ms
p99 MSPT < 45 ms
keine synchronen Datei-/DB-Writes im Player-Interaction-Pfad
keine unbounded Collections
keine O(P²)-Tasks
keine doppelte Service-Implementierung für semantisch verschiedene APIs
keine persistenten Markttransaktionen ohne atomare Commit-Grenze
```

---

## Schlussurteil

PixelRPG ist **kein Schrott** und der aktuelle `test`-Stand enthält mehrere technisch gute Korrekturen. Besonders das PlayerProfile-Snapshotting, die MySQL-Transaktion, die Region-Indexierung, die Hikari-Konfiguration, der External-Skin-Schutz und die Lifecycle-Koordination sind solide Ansätze. fileciteturn26file0L2-L6 fileciteturn6file0L2-L6 fileciteturn13file0L2-L6 fileciteturn9file0L2-L6 fileciteturn34file0L2-L6

Aber ein MMORPG-Plugin für 100+ Spieler darf nicht nur „meistens funktionieren“. Die aktuelle Architektur enthält noch **mehrere deterministische Main-Thread-I/O-Pfade**, einen **O(P²)-Scoreboard-Loop**, mehrere **permanente Entity-Scan-Loops** und vor allem eine **fehlerhafte GuildAPI-Servicearchitektur**.

Das wichtigste Ergebnis ist deshalb:

> **Der aktuelle `test`-Branch ist nicht bereit für einen 100+ Spieler Production-Server mit der Anforderung „20.00 TPS / minimale MSPT“.**
>
> **Die kritischsten Baustellen sind nicht die RPG-Mathematik oder Paper-API-Nutzung, sondern Persistence, Service-Verträge und wiederkehrende globale Scans.**

Keine kosmetischen Refactorings zuerst. Erst die P0-Probleme beheben, danach Lastprofiling, anschließend P1-Optimierungen. Alles andere ist Zeitverschwendung.
