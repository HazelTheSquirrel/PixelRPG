# 🚨 Forensisches Audit: PixelRPG (Test-Branch)

**Audit-Stand:** `test` @ `070c5ac6313409c0c9e73da544a7cd8803c07e6b`  
**Referenz:** `main` @ `9928b5abb2f44c88b45c756ac262f3f6620e2714`  
**Branch-Zustand:** `test` ist 161 Commits vor `main` und 2 Commits hinter `main`; der Branch ist divergent. Der geprüfte `test`-Head ist der Commit, der dieses Audit-Dokument selbst aktualisiert hat.  
**Zielplattform:** Java 25 + Paper 26.2 (`26.2.build.121-stable`) + Mojang-Mappings + `paper-plugin.yml`.

> **Forensische Einschränkung:** Die GitHub-API liefert Java-Dateien in dieser Umgebung als escaped Einzeiler. Deshalb werden Befunde über **Datei + Klasse + Methode/Feld/Codepfad** referenziert. Es werden keine erfundenen Zeilennummern angegeben. Die Bewertung basiert auf dem tatsächlichen `test`-Stand.

---

## 1. Zusammenfassung & Skalierbarkeit (100+ Spieler)

## Klares Urteil: **NICHT PRODUCTION-READY. Aktuell kein einzelner fataler Server-Killer, aber mehrere reale Main-Thread-I/O-, O(P²)- und hochfrequente Entity-Scan-Pfade verhindern eine seriöse 20.00-TPS-Garantie bei 100+ aktiven Spielern.**

Das Projekt ist kein chaotischer Bukkit-Prototyp. Der `test`-Stand enthält bereits mehrere vernünftige Modernisierungen:

- Java-25-Toolchain und `options.release = 25`.
- Paperweight `2.0.0-beta.21` und Paper Dev Bundle `26.2.build.121-stable`.
- `paper-plugin.yml` statt altem Plugin-Descriptor.
- Paper Lifecycle COMMANDS-Registrierung und `BasicCommand`-Adapter.
- Adventure Components statt `ChatColor`.
- UUID-basierte Player-/Entity-Maps.
- PDC für RPG-Metadaten.
- HikariCP und Prepared Statements.
- MySQL-Profil-Saves mit Transaktion und Persistenz-Revision.
- Per-Spieler Save-Serialisierung.
- Atomare YAML-Schreibvorgänge bei Guild/NPC-Persistenz.
- Begrenzter Skin-Cache und keine HTTP-Redirects.
- Zentrales Lifecycle-Shutdown.
- Native aktuelle Paper-Dialog-Klassen im Dialog-/Trade-System.

Die Build-Konfiguration ist grundsätzlich auf die geforderte Basis gepinnt; die bestehenden Source- und Shadow-Verifikationen sind vorhanden und `check` hängt sie ein. fileciteturn7file0L2-L6 Der Plugin-Descriptor ist ebenfalls auf API 26.2 gesetzt. fileciteturn21file0L2-L6

Das Problem ist nicht fehlende Struktur, sondern dass mehrere Subsysteme **unterschiedliche Persistenz- und Laufzeitmodelle** verwenden und dabei die teuersten Operationen genau in Spieler-Interaktions- oder Tick-Pfaden liegen.

### Gesamturteil

| Bereich | Urteil | Priorität |
|---|---|---:|
| Build-/Dependency-Basis | gut | P2 |
| Plugin-/Lifecycle-Struktur | gut | P2 |
| Player-Profile/MySQL | gut, aber komplex | P1 |
| YAML-Guild/NPC-Persistenz | akzeptabel, aber skalierungsbegrenzt | P1 |
| persönliche Bank | **Main-Thread-Disk-I/O** | **P0** |
| Trade Depot | **Main-Thread-Disk-I/O + nicht-atomare Wirtschaftstransaktion** | **P0** |
| Scoreboard | **O(P²) pro Update** | **P0** |
| Companion Runtime | **hochfrequente Entity-/Block-/PDC-Arbeit** | **P0/P1** |
| NPC Look | **NearbyEntity-Scan pro NPC** | **P1** |
| Mob Scaling | **wiederholtes Teilnehmer-/Gear-Scanning** | **P1** |
| Quest Passive | **Poll statt primär Event-getrieben** | **P1** |
| Security | keine offensichtliche Backdoor; Wirtschaft/SSRF-Härtung unvollständig | P0/P1 |
| Memory | keine offensichtliche dauerhafte Player-Leak-Struktur; einige Runtime-Maps müssen weiter gehärtet werden | P1 |
| Folia | nicht als Folia-System ausgelegt | P2 |

### 100+-Spieler-Simulation

Bei 100 Spielern und hoher Feature-Aktivität entstehen gleichzeitig:

1. Scoreboard-Updates über alle Spieler.
2. Pro Spieler erneute Guild-/Party-Auflösung.
3. Guild-Prefix-Scan über **alle** Online-Spieler für **jeden** Spieler.
4. Quest-Passivprüfung über alle Online-Spieler.
5. Companion Runtime alle 2 Ticks für jeden aktiven Companion.
6. NPC-Look alle konfigurierten Ticks für jeden gespawnten NPC.
7. Mob-Scaling-Cleanup jede Sekunde über aktive skalierte Mobs.
8. Event-getriebene Combat-/Loot-/Profession-/Quest-Arbeit.
9. Bei Bank-/Trade-Interaktionen synchrone YAML-Schreibvorgänge.
10. Async-Profil-I/O und weitere YAML-I/O-Executors.

Die Zahl der Scheduler allein ist **nicht** das Problem. Das Problem ist die Kombination aus hoher Frequenz, linearen Scans und synchronem Disk-I/O.

**20.00 TPS als harte Zusicherung ist mit dem aktuellen Code nicht vertretbar.** Bei 100 Spielern in ruhigem Betrieb kann der Server problemlos laufen. Bei 100+ Spielern plus vielen Companions, NPCs, Mobs und gleichzeitigen Markt-/Bankaktionen ist das Design nicht ausreichend konservativ.

---

## 2. Kritische Performance-Engpässe & Threading-Fehler

### P0 — `BankStorageService`: synchroner kompletter YAML-Write im Spieler-Eventpfad

**Datei:** `src/main/java/de/pixelrpg/rpg/dialogue/BankStorageService.java`  
**Methoden:** `save`, `saveTradeGoods`, `addTradeGoods`, `saveTo`, `write`

Die Klasse hält komplette YAML-Konfigurationen im RAM. `saveTo()` löscht den Spielerbereich, serialisiert bis zu 108 Bank-Slots bzw. 54 Handelsfach-Slots und ruft anschließend `target.save(file)` auf. `write()` führt diesen Dateizugriff direkt aus. fileciteturn4file0L2-L6

Der `synchronized`-Modifier macht die Situation nicht schneller; er serialisiert nur konkurrierende Aufrufer. Wenn ein Bank-GUI geschlossen wird, landet dieser Code auf dem Serverthread.

**Worst Case:** 100 Spieler schließen gleichzeitig ihr Bank-GUI → 100 YAML-Serialisierungen und Dateischreibvorgänge hintereinander auf dem Main Thread.

Das ist ein klassischer Tick-Stall. Ein SSD-System macht das weniger sichtbar, aber nicht korrekt. Dateisystem-Latenz, GC und YAML-Serialisierung sind nicht deterministisch genug für einen 50-ms-Tick.

**SOLL:** immutable Snapshot im Main Thread erzeugen, Snapshot async persistieren, pro Spieler sequenzieren und atomar ersetzen. Noch besser: Bank, Trade und Wirtschaft in den bestehenden MySQL-Storage-Layer integrieren.

---

### P0 — `TradeDepotManager`: synchroner Voll-Write bei Marktoperationen

**Datei:** `src/main/java/de/pixelrpg/rpg/trade/TradeDepotManager.java`  
**Methoden:** `save`, `purchase`, `cancel`, `expireListings`, `claimPendingPayout`, `createListingFromResponse`

`save()` baut bei jedem Aufruf eine neue `YamlConfiguration`, serialisiert alle Listings und Pending Payouts und schreibt anschließend `yaml.save(file)`. `purchase()` und `cancel()` rufen `save()` direkt auf; `expireListings()` ebenfalls. fileciteturn5file0L2-L6

Das ist nicht akzeptabel für einen stark frequentierten Marktplatz.

Zusätzlich macht `canFitTradeGoods()` einen kompletten Leseweg aus der persistenten Bank und baut ein temporäres Bukkit-Inventar auf. Damit kann eine einzige Marktinteraktion bereits Datei-Lesezugriff + Item-Clones + Inventory-Simulation + Datei-Schreibzugriff enthalten.

**SOLL:** Marktbestand im RAM, persistenter Transaktionslog/DB und gebündelte Async-Persistenz. Die UI darf nie von einem vollständigen YAML-Read/Write abhängen.

---

### P0 — `TradeDepotManager.purchase()`: wirtschaftlich nicht atomar und Verlust-/Dupe-Gefahr bei Zustandsfehlern

Der Kaufablauf ist derzeit:

```text
Listing lesen
→ Handelsfach-Kapazität prüfen
→ Geld aus Buyer-Profil entfernen
→ Listing entfernen
→ Verkäufer gutschreiben / Pending Payout
→ Item ins Handelsfach schreiben
→ trade-depot.yml speichern
```

Der kritische Fehler: `canFitTradeGoods()` und `bankStorage.addTradeGoods()` sind zwei getrennte Operationen. Zwischen ihnen gibt es keine atomare Reservation. `addTradeGoods()` kann fehlschlagen, nachdem der Kauf bereits wirtschaftlich verändert wurde. Der Code ignoriert den Rückgabewert an dieser Stelle.

Damit existiert mindestens ein **Item-Verlustpfad**:

```text
buyer.removeMoney() == true
listing removed
seller credited
addTradeGoods() == false
→ Kauf wird trotzdem als erfolgreich abgeschlossen
→ Item ist nicht garantiert zugestellt
```

Das ist kein theoretischer Schönheitsfehler. Vollständiges Handelsfach, konkurrierende Änderungen oder ein Fehler beim Storage können diesen Zustand erzeugen.

`cancel()` hat dieselbe Struktur.

**SOLL:** Eine Transaktion muss erst die Zielkapazität reservieren bzw. das Item in einen sicheren Pending-Transfer überführen und danach Geld/Listing verändern. Persistenz muss dieselbe Transaktionsgrenze haben.

Beispielhafte Zielstruktur:

```java
// Alle Operationen laufen auf dem Serverthread bzw. innerhalb einer serialisierten
// Economy-Transaktion; DB-Persistenz muss dieselbe logische Transaktion abbilden.
if (!reserveTradeGoodsSlot(buyer.getUniqueId(), listing.itemCopy())) {
    return false;
}
if (!buyerProfile.removeMoney(listing.price())) {
    releaseTradeGoodsReservation(buyer.getUniqueId(), listing.id());
    return false;
}
if (!listings.remove(listing.id(), listing)) {
    buyerProfile.addMoney(listing.price());
    releaseTradeGoodsReservation(buyer.getUniqueId(), listing.id());
    return false;
}
commitTradeTransfer(listing, buyer.getUniqueId());
return true;
```

Das Snippet beschreibt die erforderliche Commit-/Rollback-Struktur; `reserveTradeGoodsSlot` und `commitTradeTransfer` müssen an die konkrete Storage-Implementierung angebunden werden. Die aktuelle Implementierung besitzt diese Transaktionsgrenze nicht.

---

### P0 — `ScoreboardService.applyGuildPrefixes()`: O(P²)

**Datei:** `src/main/java/de/pixelrpg/rpg/scoreboard/ScoreboardService.java`  
**Methoden:** `startTask`, `apply`, `applyGuildPrefixes`

`startTask()` iteriert über alle Online-Spieler. `apply()` wird pro Spieler ausgeführt. Danach ruft jeder dieser Aufrufe `applyGuildPrefixes()` auf. Diese Methode iteriert erneut über **alle Online-Spieler**. fileciteturn6file0L2-L6

Bei `P` Spielern ist der Prefix-Teil ungefähr:

```text
P scoreboard applications
× P online players scanned per application
= O(P²)
```

100 Spieler = ~10.000 Spieler-Paare pro Update. 200 = ~40.000. Zusätzlich werden Guild-Lookups, Team-Erzeugung, Component-Erzeugung und Scoreboard-Mutationen durchgeführt.

**Noch schlimmer:** `appendGuildLine()` macht pro Spieler einen weiteren Guild-Lookup und `appendPartyLine()` kann `Bukkit.getOfflinePlayer()` für jedes Party-Mitglied ausführen.

**SOLL:** Guild-View einmal berechnen und nur Dirty-Scoreboards aktualisieren. Eine Guild-Mitgliedschaftsänderung muss gezielt die betroffenen Spieler markieren.

Zielmuster:

```java
Map<UUID, GuildView> guildViews = buildGuildViewsOnce();
for (Player player : Bukkit.getOnlinePlayers()) {
    PlayerScoreboardState state = stateByPlayer.get(player.getUniqueId());
    if (state == null || !state.dirty()) continue;
    applyScoreboard(player, guildViews);
}
```

Noch besser ist ein event-getriebenes Dirty-Modell: Guild-Join/Leave/Rename invalidiert exakt die betroffenen Scoreboards.

---

### P1 — `ScoreboardService`: Legacy-Formatzeichen trotz Adventure-Strategie

**Datei:** `ScoreboardService.java`  
**Feld:** `INVISIBLE_ENTRIES`

Das Plugin vermeidet die Klasse `ChatColor`, verwendet aber weiterhin direkte `§0` bis `§e`-Strings als Scoreboard-Entry-Identitäten. fileciteturn6file0L2-L6

Das ist kein klassischer Sicherheitsbug und wird vom bestehenden Verifier nicht erfasst, widerspricht aber der verbindlichen Text-/Chat-Architektur. Diese Identitäten sollten auf eine moderne, formatierungsunabhängige Entry-Strategie umgestellt werden.

---

### P1 — `NpcLookTask`: räumlicher Scan pro NPC statt Spieler-/Chunk-Index

**Datei:** `src/main/java/de/pixelrpg/rpg/npc/NpcLookTask.java`  
**Methode:** `tick`

Der Task iteriert über jeden gespawnten NPC, ruft `Bukkit.getEntity()` auf und führt anschließend `livingEntity.getNearbyEntities(radius, radius, radius)` aus. Danach werden Location-/Distanzberechnungen für gefundene Entities durchgeführt. fileciteturn16file0L2-L6

Wenn 100 NPCs vorhanden sind und der Task alle 5 Ticks läuft:

```text
20 / 5 = 4 Updates/s
4 × 100 NPCs = 400 NearbyEntity-Abfragen/s
```

Bei 500 NPCs sind es 2.000 Abfragen/s. In dicht besiedelten Hubs explodiert zusätzlich die Zahl der gefundenen Entities.

**SOLL:** NPCs über Chunk-/Spieler-Indizes verwalten. Nur NPCs mit tatsächlich anwesenden Spielern in Reichweite drehen. Rotation nur ändern, wenn sich das Ziel tatsächlich geändert hat.

---

### P1 — `CompanionFollowTask`: 2-Tick-Hotloop mit Entity-, PDC-, Item- und Blockarbeit

**Datei:** `src/main/java/de/pixelrpg/rpg/companion/CompanionFollowTask.java`  
**Methoden:** `run`, `updateRuntimeState`, `equipmentHash`, `follow`, `moveTowards`, `shouldStepUp`, `shouldStepDown`, `isSolid`

Der zentrale Runtime-Task läuft mit hoher Frequenz und verarbeitet jeden aktiven Companion. Pro Durchlauf können auftreten:

- Player-Lookup,
- Entity-Lookup,
- PDC-Lookups,
- Registry-Lookup,
- Equipment-Lookup,
- Hash-Berechnung über mehrere ItemStacks,
- Stat-Berechnung,
- Entity-Attribute-Updates,
- Location-/Vector-Operationen,
- Blockabfragen für Step-Up/Step-Down,
- Velocity-/Rotation-Updates,
- Combat-Targeting.

fileciteturn23file0L2-L6

100 aktive Companions = 50 Runtime-Durchläufe pro Sekunde. 500 = 250/s. Die Frequenz ist für simple Follow-Physik vertretbar, nicht aber für jedes Mal erneute State-Auflösung und potenziell teure Combat-/Blocklogik.

**SOLL:**

- immutable Runtime-State/Revision pro Companion,
- Equipment-Revision statt wiederholtem `Objects.hash(ItemStack...)`,
- Combat-Tickrate getrennt vom Follow-Tick,
- Targeting mit Cooldown,
- Block-Sondierung nur wenn Bewegung tatsächlich nötig ist,
- keine Disk-Leseoperation aus dem Runtime-Pfad.

---

### P1 — `CompanionService.getEquipment()` / `CompanionEquipmentStore.load()`: möglicher synchroner Disk-Read im Hotpath

Die Companion-Equipment-Store-Implementierung kann asynchron speichern, aber ein nicht gecachter Equipment-Zustand wird über den Load-Pfad aus YAML rekonstruiert. `CompanionFollowTask.equipmentHash()` ruft den Equipment-Service regelmäßig auf. Der erste Zugriff kann daher einen Disk-Read im Serverthread verursachen.

**SOLL:** Equipment beim Player-/Companion-Aktivieren vollständig laden und anschließend nur RAM lesen. Runtime-Hotpaths dürfen keinerlei Datei-I/O auslösen.

---

### P1 — `QuestPassiveCheckTask`: Polling von drei Subsystemen pro Spieler

**Datei:** `src/main/java/de/pixelrpg/rpg/quest/QuestPassiveCheckTask.java`  
**Methode:** `start`

Alle Online-Spieler werden alle 40 Ticks verarbeitet. Für jeden Spieler werden aufgerufen:

```java
inventoryTracker.refresh(player);
questManager.checkReachLocationQuests(player);
navigationService.refresh(player);
```

fileciteturn17file0L2-L6

Das ist bei 100 Spielern nicht automatisch fatal, aber es ist Polling statt primär event-getriebener Zustandsänderung.

**SOLL:**

- Inventarquests auf Inventory-/Item-Events aktualisieren.
- Killquests auf Kill-/Damage-Events aktualisieren.
- Standortquests über Movement/Chunk/Region-Transitions dirty setzen.
- Navigation nur bei Zieländerung oder in niedriger Frequenz aktualisieren.

Ein langsamer Fallback-Poll kann bleiben, aber er darf nicht die primäre Logik sein.

---

### P1 — `MobLevelScalingListener`: wiederholtes Scanning aktiver Mobs und Spieler-Gear

**Datei:** `src/main/java/de/pixelrpg/rpg/combat/scaling/MobLevelScalingListener.java`  
**Methoden:** `onPlayerLevelUp`, `markParticipant`, `applyScaling`, `gearLevelMultiplier`, `restoreExpiredScaling`

Bei jedem relevanten Treffer kann `applyScaling()` laufen. Dort werden alle Teilnehmer des Mobs betrachtet und für jeden Online-Spieler das Gear aus Armor + Mainhand + Offhand zusammengesetzt. fileciteturn18file0L2-L6

Der Cleanup läuft jede Sekunde über alle aktiven Mobs und ruft für jeden Teilnehmer erneut `guildAPI.isRegistered()` auf. Anschließend wird bei verbleibenden Teilnehmern erneut skaliert.

**SOLL:**

- Effective-Player-Combat-Level im Runtime-State cachen.
- Gear-Level nur bei Equipment-Änderung invalidieren.
- Mob-Level erst neu berechnen, wenn Teilnehmermenge/Level/Gear-Revision geändert wurde.
- Expiration per DelayQueue/TimeWheel oder nach nächstem Ablauf sortierter Struktur statt vollständigem Mob-Scan.

---

### P1 — `NpcManager.resyncPlayer()`: linearer NPC-Scan bei jedem Login

**Datei:** `NpcManager.java`  
**Methode:** `resyncPlayer`

Bei einem Login wird über alle geladenen NPCs iteriert. Für jeden NPC werden Welt-/Chunk-/Entity-Prüfungen und ggf. Entity-Resynchronisation ausgeführt. Das ist kein Tick-Hotpath und deshalb nicht P0, skaliert aber schlecht mit großen NPC-Mengen.

**SOLL:** Chunk-Index verwenden und nur relevante Chunks bzw. NPCs des Spielers betrachten. Der Manager besitzt bereits einen Chunk-Index; `resyncPlayer()` sollte diesen konsequent verwenden.

---

### P1 — `GuildManager`: gutes Async-Design, aber globale Synchronisation

**Datei:** `GuildManager.java`

Positiv: YAML-Snapshots werden auf dem Serverthread erstellt und anschließend über einen Single-Thread-Executor atomar geschrieben. fileciteturn10file0L2-L6

Negativ: Fast sämtliche Guild-Operationen sind `synchronized`. Das ist bei kleinen Guild-Datenstrukturen akzeptabel, verhindert aber unnötig parallele Read-Operationen. Kritischer ist die semantische Kopplung von Economy und Guild-Persistenz: `createGuild()` zieht Geld aus dem Player-Profil ab und speichert anschließend nur die Guild-YAML asynchron.

Das erzeugt ein Crash-Fenster:

```text
Gold im RAM -1000
Guild im RAM erstellt
Guild YAML async gespeichert
Server stirbt bevor Player-Profil gespeichert ist
→ Guild existiert nach Neustart
→ Gold ist wieder auf altem Wert
```

Damit kann ein Prozessabsturz die Gildengründung effektiv kostenlos machen.

Das ist ein **wirtschaftlicher Konsistenzfehler**, auch wenn er nicht durch normales Doppelclick-Spamming ausgelöst wird.

---

### P1 — `PlayerProfileManager`: gutes Snapshot-Modell, aber hohe Komplexität

**Datei:** `PlayerProfileManager.java`  
**Methoden:** `captureAndEnqueue`, `persistSnapshot`, `enqueue`, `shutdown`

Die per-UUID Save-Chain ist grundsätzlich sauber: Snapshot im RAM, Persistenz off-thread, sequentiell pro UUID. fileciteturn11file0L2-L6

Positiv ist auch die Persistenz-Revision. `MySQLPlayerProfileRepository.save()` sperrt die DB-Revision und verwirft einen veralteten Snapshot. fileciteturn3file0L2-L6

Risiko: Das System besitzt mehrere Persistenzdomänen parallel. Player-Profil, Guild, Bank, Trade, NPC und Companion-Equipment haben jeweils eigene Queues/Dateien/Transaktionen. Dadurch entstehen Cross-System-Commit-Fenster, die eine einzelne DB-Transaktion nicht abfangen kann.

**SOLL:** Eine zentrale Persistenzschicht für alle wirtschaftlich relevanten Daten. YAML nur für Konfiguration/kleine administrative Daten.

---

### P1 — MySQL: Transaktional sauber, aber vollständiger Rewrite von Quest/Equipment/Stats

**Datei:** `MySQLPlayerProfileRepository.java`  
**Methode:** `save`

Der Save ist transaktional, Prepared Statements werden verwendet und die Revision wird per `SELECT ... FOR UPDATE` geschützt. fileciteturn3file0L2-L6

Der Preis ist jedoch hoch: aktive Quests werden vollständig gelöscht und neu geschrieben, Equipment vollständig gelöscht und neu geschrieben, Statistiken per Batch upserted. Bei häufigen Save-Triggern erzeugt das unnötige DB-Arbeit.

**SOLL:** Dirty-Subsysteme getrennt persistieren oder diff-basiert schreiben. Ein Player-Login/Logout-Save darf nicht zwangsläufig komplette Equipment-/Questtabellen neu schreiben.

---

### P2 — `ExternalSkinService`: Async HTTP gut, SSRF-Grenze konzeptionell nicht vollständig

**Datei:** `ExternalSkinService.java`  
**Methoden:** `apply`, `isSafeExternalHost`, `normalizeUrl`

Positiv:

- HTTP läuft async.
- Timeout 15 Sekunden.
- Redirects sind deaktiviert.
- Response-Größe ist auf 256 KiB begrenzt.
- URL-Schema wird beschränkt.
- Userinfo/Fragment werden verworfen.
- Cache ist begrenzt und TTL-basiert.

fileciteturn9file0L2-L6

Aber: Die lokale `isSafeExternalHost()`-Prüfung schützt primär den Plugin-Prozess. Die eigentliche URL wird danach an **MineSkin** geschickt. Der Remote-Dienst lädt die URL. Damit verhindert die lokale DNS-Prüfung nicht automatisch, dass ein externer Dienst eine private Zieladresse aufruft.

**SOLL:** Nur streng erlaubte Hostnamen verwenden, wenn Skin-URLs überhaupt frei administrierbar sein müssen. Für maximale Sicherheit ausschließlich bekannte Minecraft-Texture-Hosts oder ein eigenes kontrolliertes Asset-Backend akzeptieren.

---

## 3. Sicherheitslücken & Exploit-Potenzial

### S0 — Trade-Economy-Commit ist nicht atomar

Siehe P0 oben. Das ist die wichtigste funktionale Sicherheitslücke.

Betroffene Methoden:

- `TradeDepotManager.purchase()`
- `TradeDepotManager.cancel()`
- `TradeDepotManager.expireListings()`
- `BankStorageService.addTradeGoods()`

Die Kombination aus Vorab-Kapazitätsprüfung und anschließendem separatem Insert/Save ist TOCTOU-artig: **prüfen und committen sind nicht eine Operation**.

---

### S0 — Crash-Konsistenz zwischen Economy und Guild

`GuildManager.createGuild()` verändert das Player-Profil und persistiert danach die Guild asynchron. Ein Crash im falschen Zeitfenster kann einen Zustand erzeugen, in dem die Guild nach Neustart existiert, die Kosten aber nicht dauerhaft gespeichert wurden. fileciteturn10file0L2-L6

Das muss durch gemeinsame transaktionale Persistenz oder eine eindeutige Saga-/Journal-Logik gelöst werden.

---

### S1 — Trade-Listing-Preis: `double` als Wirtschaftswert

`TradeDepotManager` verwendet `double` für Preise und berechnet Gebühren als `listing.price() * (1.0D - SALE_FEE)`. Erst `PlayerProfile` konvertiert über `Money.fromMajor()` in Minor Units. fileciteturn5file0L2-L6 fileciteturn14file0L2-L6

Das verhindert viele klassische Rundungsfehler, aber die **Domäne selbst** bleibt `double`-basiert. Für ein persistentes Echtgeld-/Spielgeldsystem ist das unnötig riskant.

**SOLL:** Preise intern ausschließlich als `long minorUnits`. Dialoginput wird einmal validiert und danach nicht mehr als `double` geführt.

Beispiel:

```java
long priceMinorUnits = Money.fromMajor(parsedAmount);
long fee = Math.multiplyExact(priceMinorUnits, 5L) / 100L;
long sellerMinorUnits = priceMinorUnits - fee;
```

Die konkrete Overflow-Behandlung muss zusätzlich vor `multiplyExact` erfolgen; noch besser ist `BigDecimal` ausschließlich an der Eingabegrenze und danach `long`.

---

### S1 — `Money.fromMajor()` sättigt Maximalwerte

`Money.fromMajor()` wandelt große Werte in Minor Units um und sättigt bei `Long.MAX_VALUE`. fileciteturn14file0L2-L6

Das ist technisch deterministisch, aber semantisch gefährlich: Ein absichtlich oder versehentlich übergroßer Preis kann auf das maximale Guthaben abgebildet werden statt hart abgelehnt zu werden.

**SOLL:** Eingaben oberhalb eines konfigurierten Maximalwertes ablehnen. Wirtschaftswerte brauchen eine explizite obere Grenze.

---

### S1 — GUI-/Inventar-Sicherheit ist teilweise korrekt, aber Trade sollte auf Item-Identität härten

`openSellDialog()` und `createListingFromResponse()` prüfen das aktuelle Inventar gegen das ausgewählte Item und Menge. Das ist gut. fileciteturn5file0L2-L6

Die Prüfung `isSimilar()` plus Amount ist aber keine persistente Objektidentität. Bei einzigartigen RPG-Items sollte die vorhandene Unique-Item-/PDC-Identität zusätzlich geprüft werden, sofern der Gegenstand individuell eindeutig sein soll.

**SOLL:** Für Unique-/Soulbound-/RPG-Items klare Identitätsregeln definieren:

- Definition-ID,
- Unique-ID falls vorhanden,
- Besitzer-ID bei Soulbound,
- erlaubte Menge,
- keine nicht vertrauenswürdigen PDC-Felder vom Client.

---

### S1 — `NpcManager` akzeptiert administrative Skin-URLs; Eingabe muss an Permission-Grenze bleiben

`NpcManager.updateSkin()` speichert den Skin-String und ruft den Skin-Resolver auf. fileciteturn24file0L2-L6

Das ist nur dann akzeptabel, wenn ausschließlich `rpg.admin`-geschützte Commands diesen Pfad erreichen. Die Sicherheitsgrenze darf nicht allein auf GUI-/Command-UI-Logik beruhen; der Manager selbst sollte administrative Mutationen klar separieren bzw. der Command-Layer muss nachweisbar die Permission erzwingen.

---

### S1 — Keine offensichtliche Backdoor oder hartcodierte Geheimnisse gefunden

Im geprüften Code wurden keine offensichtlichen Aufrufe wie `Runtime.exec`, Prozessstarts, versteckte Remote-Code-Ausführung oder hartcodierte Datenbankpasswörter/Tokens festgestellt.

Die MineSkin-API-Konfiguration kommt aus Config bzw. `PIXELRPG_MINESKIN_API_KEY`, was die richtige Richtung ist. fileciteturn9file0L2-L6

**Wichtig:** Das ist ein statischer Befund. Es ist keine Garantie gegen kompromittierte Laufzeitumgebung, externe Libraries oder Geheimnisse außerhalb des Git-Repositories.

---

### S2 — Command-Permission-Architektur ist modern, aber Adapter-Kompatibilität erhöht Komplexität

`PaperBasicCommandAdapter` verwendet Paper `BasicCommand`, delegiert intern aber auf Bukkit-`CommandExecutor`/`CommandSender`. fileciteturn22file0L2-L6

Das funktioniert als Brücke, ist aber architektonisch nicht ideal. Für ein langfristiges Paper-26.x-Core-Plugin sollte die Command-Domain direkt auf Paper Brigadier/BasicCommand ausgerichtet werden.

Das ist kein Security-Bug, sondern eine technische Schuldposition.

---

## 4. Detaillierte Kritik der Implementierungen

### 4.1 `PlayerProfile`

**Positiv:**

- Fast alle Felder sind gekapselt.
- `synchronized` schützt zusammengesetzte Profile-Operationen.
- Snapshots klonen Equipment und Questdaten.
- Dirty-Tracking ist vorhanden.
- Experience und Playtime saturieren gegen Overflow.
- Geld wird in Minor Units gehalten.

fileciteturn13file0L2-L6

**Kritik:**

- Ein einzelnes synchronisiertes God-Object ist langfristig schwer zu warten.
- Profile enthält Economy, Quests, Profession, Equipment, Settings und Statistics gleichzeitig.
- Jeder Snapshot kopiert mehrere Collections und alle Equipment-ItemStacks.
- Cross-Domain-Änderungen können nur über `dirty` erkannt werden; feineres Dirty-Tracking fehlt.

**Zielarchitektur:** `PlayerProfile` als Aggregat mit klaren Sub-States oder immutable Value Objects:

```text
PlayerProfile
 ├─ EconomyState
 ├─ ProgressionState
 ├─ ProfessionState
 ├─ QuestState
 ├─ EquipmentState
 ├─ PreferenceState
 └─ StatisticsState
```

Nicht sofort alles refactoren; zuerst Persistence- und Hotpath-Probleme beseitigen.

---

### 4.2 `MySQLPlayerProfileRepository`

Prepared Statements, Transaktion und Revision sind gut. fileciteturn3file0L2-L6

Die Methode `save()` ist jedoch viel zu groß und führt mehrere unabhängige Persistenzaufgaben in einem Block aus. Das erschwert Fehleranalyse und Performance-Profiling.

**SOLL:**

```text
savePlayerRow()
saveActiveQuests()
saveEquipment()
saveStatistics()
```

Alle Methoden bekommen dieselbe Connection und bleiben Teil derselben Transaktion.

---

### 4.3 `DatabaseManager`

Die Eingabevalidierung von Host, Port und Datenbank-Identifier ist gut. SSL ist konfigurierbar und standardmäßig `REQUIRED`. Poolgröße und Timeout sind begrenzt. fileciteturn12file0L2-L6

Kritik:

- `ssl-mode: DISABLED` bleibt als Konfigurationsoption erlaubt. Das ist für eine Produktionswirtschaft eine gefährliche Default-Fluchtmöglichkeit.
- Migrationen und Schema-Versionierung sind noch sehr klein und nicht als robustes Migration-Framework ausgelegt.
- `leakDetectionThreshold=15s` ist als Diagnose gut, sollte aber nicht als dauerhafte Performance-Strategie verstanden werden.

---

### 4.4 `GuildManager`

Der Snapshot-/Executor-Ansatz ist deutlich besser als synchrones YAML-Speichern. Atomare Move-Operationen sind richtig. fileciteturn10file0L2-L6

Aber die Economy-Operationen und Guild-Persistenz sind getrennt. Für ein MMORPG mit wertvollen Guild-Assets ist das nicht akzeptabel.

Zusätzlich sind `getGuild`, `getMembers`, `isMember` etc. komplett synchronisiert. Bei 100 Spielern und häufigen Scoreboard-Abfragen entsteht unnötige Lock-Aktivität.

**SOLL:** immutable Guild-Snapshots + lockfreies Read-Modell; Schreiboperationen seriell über eine Guild-State-Queue.

---

### 4.5 `ScoreboardService`

Das Scoreboard-Rendering ist grundsätzlich sauberer als ein vollständiger Neuaufbau bei jedem Tick: `lastLines` wird verglichen und nur bei Änderung aktualisiert. Das ist gut. fileciteturn6file0L2-L6

Der große Fehler ist der Guild-Prefix-Teil. Das Delta-Update-Modell wird dort durch einen vollständigen Online-Spieler-Scan wieder zunichtegemacht.

**SOLL:** Scoreboard-Daten als vorberechneten View behandeln und UI nur bei Dirty-State verändern.

---

### 4.6 `NpcManager`

Der Chunk-Index ist eine gute Entscheidung. `getSpawnedEntityUuids()` gibt eine Kopie zurück und verhindert direkte Mutation. YAML-Persistenz läuft über einen dedizierten Executor. fileciteturn24file0L2-L6

Kritik:

- `spawnEntityFor()` ist stark an Bukkit-Laufzeitobjekte gebunden und sollte strikt Main-Thread-only bleiben.
- `resyncPlayer()` nutzt den Chunk-Index nicht maximal.
- `saveAll()` queued komplette Snapshots bei jeder administrativen Änderung. Das ist für Admin-Frequenz okay, aber ein Save-Coalescing wäre sauberer.

---

### 4.7 `ExternalSkinService`

Die Async-HTTP-Architektur ist richtig. Cache-Limit, TTL, Timeout, Redirect-Disable und Response-Limit sind gute Sicherheitsmaßnahmen. fileciteturn9file0L2-L6

Kritik:

- Remote URL wird an einen weiteren externen Dienst delegiert.
- 15 Sekunden Timeout ist für eine einzelne HTTP-Anfrage okay, aber viele parallele NPC-Skin-Anfragen können Thread-/Connection-Ressourcen binden.
- Es gibt kein explizites Request-Coalescing: mehrere identische gleichzeitige Skin-Requests können denselben Remote-Request mehrfach auslösen.

**SOLL:** `Map<String, CompletableFuture<ProfileProperty>> inFlight` ergänzen und identische Requests deduplizieren.

---

### 4.8 `MobLevelScalingListener`

Die Idee, Originalattribute im PDC zu speichern und beim Ende der Teilnahme wiederherzustellen, ist vernünftig. fileciteturn18file0L2-L6

Der Fehler liegt in der Berechnungsfrequenz. Gear und Player-Level ändern sich selten; das System berechnet sie aber in häufigen Combat-/Cleanup-Pfaden neu.

**SOLL:** Cache-Key:

```text
mobUuid
+ participantSetRevision
+ maxPlayerLevel
+ gearRevision
```

Nur bei Änderung neu skalieren.

---

### 4.9 `QuestPassiveCheckTask`

Die Lifecycle-Bereinigung ist gut: Task wird gecancelt, Listener werden über `HandlerList.unregisterAll()` entfernt und Navigation wird geleert. fileciteturn17file0L2-L6

Das Problem ist die Architektur des Pollings. Quest-Fortschritt sollte grundsätzlich aus Events entstehen; Polling ist nur für Zustände sinnvoll, die Minecraft nicht sauber als Event liefert.

---

### 4.10 `paper-plugin.yml`

Die Datei ist korrekt modernisiert und nutzt API 26.2. Die Permissions sind klar benannt. fileciteturn21file0L2-L6

`rpg.member` hat `default: true`. Das ist in Ordnung, wenn jede Command-Implementierung Registrierung/Spielerstatus selbst korrekt validiert. Es darf aber niemals als Ersatz für Autorisierung innerhalb wirtschaftlicher/adminseitiger Services dienen.

---

### 4.11 Build-Verifikation

Die bestehenden Verifikationen sind wertvoll und dürfen nicht entfernt werden. fileciteturn7file0L2-L6

Sie prüfen aktuell insbesondere:

- `ChatColor`,
- Legacy-NMS,
- CraftBukkit,
- statische Live-Server-Referenzen,
- Shadow-Relocations,
- JDBC-Service-Relocation.

Sie erkennen jedoch **keine**:

- synchronen `File`-/YAML-Writes,
- synchronen YAML-Reads in Hotpaths,
- O(P²)-Algorithmen,
- Bukkit-API-Aufrufe aus Async-Kontexten,
- unatomare Economy-Transaktionen,
- `§`-Legacy-Formatstrings.

**SOLL:** Statische Architekturlinter ergänzen, ohne bestehende Checks abzuschwächen.

---

## 5. Konkreter Action-Plan

### P0 — MUSS VOR PRODUKTION

1. **BankStorageService aus dem Main-Thread-I/O entfernen.**
   - RAM-Snapshot.
   - Async Writer.
   - Per-Spieler Write-Coalescing.
   - Atomare Datei-Ersetzung.
   - Langfristig MySQL.

2. **TradeDepot komplett transaktional machen.**
   - Kein `yaml.save()` im Interaktionspfad.
   - Kein getrenntes `canFit()` → `add()`.
   - Item-Transfer muss garantiert committen oder vollständig rollbacken.
   - Listing-State braucht `ACTIVE/SOLD/CANCELLED/EXPIRED` statt bloßem Entfernen aus einer Map.

3. **Trade-/Economy-Werte intern als `long` Minor Units führen.**
   - `double` nur beim UI-Parsing.
   - Konfigurierbare Maximalwerte.
   - Overflow hart ablehnen.

4. **Scoreboard O(P²) beseitigen.**
   - Guild-Views einmal vorberechnen.
   - Dirty-State pro Spieler.
   - Guild-Änderungen gezielt invalidieren.
   - Keine vollständige Online-Spieleriteration pro Scoreboard-Spieler.

5. **Companion Runtime entschlacken.**
   - Kein Disk-Read im Runtime-Pfad.
   - Equipment-/Stats-Revisionen.
   - Combat und Follow entkoppeln.
   - Block-Checks nur bei Bedarf.

### P1 — SOLLTE ALS NÄCHSTES

6. **NPC-Look von NPC→NearbyEntities auf indexierte Spieler/NPC-Beziehungen umstellen.**
7. **Quest-Polling auf Event-/Dirty-basierte Updates umstellen.**
8. **Mob-Scaling mit Cache/Revisionen versehen.**
9. **Guild Read-Modell lockärmer machen.**
10. **Cross-System-Economy-Persistenz vereinheitlichen.**
11. **Skin-Requests deduplizieren (`inFlight` Futures) und erlaubte Hostnamen härter beschränken.**
12. **Unique-Item-Identität bei Trade-/GUI-Transfers explizit validieren.**

### P2 — TECHNISCHE SCHULD

13. `PlayerProfile` in fachliche Sub-States zerlegen.
14. `MySQLPlayerProfileRepository.save()` in kleinere Transaktionsschritte aufteilen.
15. Command-Adapter langfristig direkt auf Paper-26.x-Command-Modell ausrichten.
16. `§`-Entry-Identitäten im Scoreboard entfernen.
17. Build-Checks um Main-Thread-I/O und bekannte Hotpath-Muster erweitern.
18. Performance-Test mit mindestens 100/200/300 Spielern und kontrollierten NPC-/Companion-/Mob-Zahlen etablieren.

---

## Empfohlene Belastungstests vor Freigabe

Ein reiner Compile-Test reicht nicht. Der Server muss mit Spark/Timings/JFR unter reproduzierbarer Last vermessen werden.

### Test A — 100 Spieler, normal

- 100 Profile online.
- 50 Companions.
- 100 NPCs.
- 500 aktive Mobs.
- Scoreboards aktiv.
- normale Questaktivität.

### Test B — 100 Spieler, Kampf

- 100 Spieler in derselben Region.
- 100+ aktive Companions.
- 1.000+ Mobs.
- hohe Damage-Event-Rate.
- Mob-Scaling aktiv.

### Test C — 100 Spieler, Economy-Sturm

- 50 Spieler öffnen/schließen Bank.
- 50 Spieler listen/kaufen/canceln parallel.
- gleichzeitige Profil-Saves.
- absichtliche Storage-Latenz simulieren.

### Test D — Crash-Konsistenz

Während folgender Operationen Prozess hart beenden:

- Guild-Erstellung.
- Trade-Kauf.
- Trade-Cancel.
- Listing-Expiration.
- Player-Quit-Save.

Danach Datenbank/YAML gegen erwarteten Zustand vergleichen.

### Abnahmekriterien

```text
P99 Main-Thread-Tick < 50 ms
P95 Main-Thread-Tick deutlich unter 50 ms
keine synchronen Disk-Writes in Spielerinteraktionen
keine Economy-Duplikation
kein Item-Verlust
keine unbounded Maps
keine Exceptions unter normaler Last
kein Task-Wachstum nach wiederholtem Reload/Shutdown
```

20.00 TPS darf nicht aus einem kurzen Testfenster abgeleitet werden. Entscheidend sind P95/P99-Tickzeiten, GC-Pausen und Worst-Case-Spikes.

---

## Schlussurteil

**PixelRPG/test ist technisch bereits deutlich über einem einfachen Plugin-Prototyp, aber noch nicht auf Core-/MMORPG-Produktionsniveau.** Die stärksten positiven Punkte sind das moderne Paper-Setup, die Async-Profile-Persistenz, die MySQL-Revisionen, die PDC-Nutzung und das grundsätzlich saubere Lifecycle-/Snapshot-Denken.

Die harten Probleme liegen in den Übergängen zwischen diesen Systemen: Bank und Trade schreiben weiterhin synchron YAML, Scoreboard skaliert quadratisch, Companion-/Mob-Systeme rechnen zu häufig neu und Economy-Operationen sind nicht über mehrere Storage-Domänen atomar.

**Freigabeentscheidung: BLOCKED.**

Der Code sollte **nicht** mit dem Anspruch veröffentlicht werden, unter 100+ gleichzeitig aktiven Spielern zuverlässig 20.00 TPS bei minimalen MSPT zu garantieren. Zuerst müssen die P0-Punkte behoben und anschließend mit realistischen Worst-Case-Lasttests vermessen werden.

---

### Geprüfte Kernartefakte

- `build.gradle` — Build-/Verifier-Basis. fileciteturn7file0L2-L6
- `paper-plugin.yml` — Paper-26.2-Plugin-Descriptor. fileciteturn21file0L2-L6
- `PlayerProfile` / `PlayerProfileManager` / `MySQLPlayerProfileRepository` — Profile, Snapshotting, Revisionen. fileciteturn13file0L2-L6 fileciteturn11file0L2-L6 fileciteturn3file0L2-L6
- `BankStorageService` — Bank-Persistenz. fileciteturn4file0L2-L6
- `TradeDepotManager` / `TradeDepotGUI` — Markt und UI. fileciteturn5file0L2-L6 fileciteturn19file0L2-L6
- `ScoreboardService` — Scoreboard-Hotpath. fileciteturn6file0L2-L6
- `CompanionFollowTask` — Companion-Hotpath. fileciteturn23file0L2-L6
- `NpcLookTask` / `NpcManager` — NPC Runtime und Persistenz. fileciteturn16file0L2-L6 fileciteturn24file0L2-L6
- `QuestPassiveCheckTask` — Quest-Polling. fileciteturn17file0L2-L6
- `MobLevelScalingListener` — Mob-Scaling. fileciteturn18file0L2-L6
- `GuildManager` — Guild-State und Persistenz. fileciteturn10file0L2-L6
- `ExternalSkinService` — externe HTTP-/Skin-Integration. fileciteturn9file0L2-L6
- `DatabaseManager` / `Money` — DB-/Money-Grenzen. fileciteturn12file0L2-L6 fileciteturn14file0L2-L6

**CI-Hinweis:** Für den geprüften Branch-Head wurden über die verfügbare GitHub-Schnittstelle keine Workflow-Runs zurückgeliefert. Das Audit ist daher eine statische Code-/Architekturanalyse und kein Ersatz für einen tatsächlichen 100+-Spieler-Lasttest auf einem laufenden Paper-26.2-Server.
