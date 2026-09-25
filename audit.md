# PixelRPG – Forensisches Architektur-, Sicherheits- und Stabilitätsaudit

**Prüfobjekt:** HazelTheSquirrel/PixelRPG  
**Branch:** `test`  
**Prüfstand:** 25.09.2026  
**Branch-HEAD:** `07ce91803d2f397f81ee606d393edbeafc83a22a`  
**Technische Sollbasis:** Java 25 / Paper 26.2 / Mojang-Mappings  
**Methode:** Statische Analyse des tatsächlichen Branch-Dateibaums, gezielte Quellcodeprüfung der Lifecycle-, Persistenz-, Netzwerk-, NPC-, Quest-, Region-, Combat-, Command- und UI-Kernkomponenten. Keine Ableitung aus Commit-Absichten.

---

## 1. Executive Summary

Der aktuelle `test`-Stand ist ein umfangreiches, modular aufgebautes Paper-Plugin mit **249 Java-Dateien** unter `src/main/java`. Die Architektur ist überwiegend service-/managerorientiert und verwendet für persistente Spielerdaten UUID-basierte Zustände.

Die wichtigsten positiven Befunde:

- Paper-Plugin-Lifecycle und aktuelles Dialogsystem sind vorhanden.
- `paper-plugin.yml` wird verwendet.
- Java 25 und Paper 26.2 Dev-Bundle sind in Gradle konfiguriert.
- HikariCP, Prepared Statements und Transaktionen werden für MySQL verwendet.
- Spielerprofil-I/O läuft über separate Executor.
- Mehrere Runtime-Tasks besitzen explizite Shutdown-Pfade.
- NPC-/Companion-/Quest-/Scoreboard-Zustände verwenden überwiegend UUIDs statt dauerhaft gespeicherter Player-Objekte.
- Externe Skin-Abfragen laufen asynchron und besitzen URL-/Response-Größenprüfungen sowie Schutz gegen lokale/private Ziele.
- Die vorhandenen Build-Verifikationen für Legacy-APIs und Shading bleiben aktiv.

Die wichtigsten negativen Befunde:

1. **PartyGUI-Rest:** `PartySubCommand` referenziert weiterhin `PartyGUI`, obwohl diese Datei im Branch-Dateibestand fehlt. Das ist eine konkrete Codekonsistenz-/Build-Risikoquelle.
2. **Synchrones Shop-YAML-I/O:** `ShopManager.save()` schreibt YAML synchron. Bei häufigen Änderungen oder großen Daten kann dies den Main Thread blockieren.
3. **NPC-Resync-Skalierung:** Teile der NPC-Resynchronisation arbeiten über den gesamten bekannten NPC-Bestand statt ausschließlich über einen räumlichen Index.
4. **Scoreboard-Erfahrung:** Das Plugin überschreibt bewusst die Vanilla-EXP-Bar über `Player#setExp` und `Player#setLevel`. Das ist funktional gewollt, aber ein konkurrierendes UI-System: andere Vanilla-/Plugin-Anzeigen können dadurch verdrängt werden.
5. **Default-Weltcontent:** NPC- und Shop-Daten werden aus Laufzeit-YAMLs erwartet; im Repository liegen keine entsprechenden Default-`npcs.yml`/`shops.yml`.
6. **Externe Abhängigkeit:** MineSkin und externe Skinquellen erweitern die Angriffs-/Ausfallfläche. Der Code enthält Schutzmaßnahmen, bleibt aber netzwerkabhängig.

**Gesamtbewertung:** **7/10** für den untersuchten statischen Stand. Das bedeutet: technisch bereits deutlich über einem einfachen Hobby-Plugin, aber noch nicht frei von Integrations-, Skalierungs- und Betriebsrisiken.

---

# 2. Architektur & Code-Qualität

## 2.1 Lifecycle

`PixelRPGPlugin` übernimmt die zentrale Komposition der Systeme. Die Komponenten werden beim Start aufgebaut und über einen `LifecycleCoordinator` mit Shutdown-Aktionen registriert.

Explizit registrierte Shutdown-Komponenten umfassen unter anderem:

- PlayerProfileManager
- CompanionService
- PartyManager
- CombatStateService
- GuildManager
- RegionManager
- NpcManager
- NpcLookTask
- BossManager
- QuestManager
- ScoreboardService
- PlaytimeTracker
- MobLevelScalingListener
- BiomeBossSpawnTask
- QuestPassiveCheckTask
- RegionEditor
- RegionSpawnService
- BankerBehavior

`onDisable()` schließt zuerst den Lifecycle und hebt danach Bukkit-Services auf. Die statische Plugin-Referenz wird anschließend auf `null` gesetzt.

**Befund:** 🟢 sauberer Lifecycle-Grundaufbau.

## 2.2 Event-Handling

Die Registrierung erfolgt zentral in `PixelRPGPlugin.onEnable()`.

Vorhandene Listener decken u. a. ab:

- Player lifecycle
- Combat
- Loot
- Mob scaling
- Bosses
- NPCs
- Quests
- Party
- Guild
- Scoreboard
- Statistics
- Companions
- GUI
- Skills
- Regions
- Soulbound

Auffällig positiv: zustandsbehaftete Listener besitzen häufig Join/Quit-/World-Change-Bereinigung.

Beispiele:

- `NpcLookTask` entfernt Player-Zustand bei Quit.
- `BiomeBossSpawnTask` entfernt Biome-Caches bei Quit.
- `QuestPassiveCheckTask` cancelt den UUID-bezogenen Wake-Zustand bei Quit.
- `ScoreboardService` entfernt Player-Scoreboard-State bei Quit.
- `CompanionFollowTask` cancelt den Owner-Zustand bei Quit.

**Befund:** 🟢 kein offensichtlicher Listener-bedingter Player-Referenz-Leak in den geprüften Kernkomponenten.

## 2.3 Listener-Prioritäten

In den gezielt geprüften Kernlistenern ist keine systematische Fehlverwendung extremer Event-Prioritäten erkennbar. Die Architektur setzt eher auf fachlich getrennte Listener.

**Restprüfung:** Für eine vollständige Laufzeitverifikation aller Event-Reihenfolgen wäre ein echter Server-Test mit konkurrierenden Plugins erforderlich.

---

# 3. Scheduler & Threading

## 3.1 Async-I/O

Der Spielerpersistenzpfad verwendet:

- `ExecutorService`
- mehrere Worker-Threads
- UUID-basierte Save-Chains
- `CompletableFuture`
- kontrolliertes Shutdown/Flush

Das reduziert Main-Thread-I/O.

## 3.2 AsyncFileWriter

`AsyncFileWriter` verwendet einen Single-Thread-Executor und schreibt Daten außerhalb des Serverthreads. Der Writer besitzt:

- Queue-/Drain-Mechanik
- Lifecycle-Lock
- atomisches Verschieben, soweit unterstützt
- Cleanup
- Shutdown mit Timeout
- `shutdownNow()` als Fallback

**Befund:** 🟢.

## 3.3 Bukkit API aus Async-Kontext

Der externe Skin-Service ist korrekt getrennt:

1. Netzwerk über `HttpClient.sendAsync()`
2. Ergebnisverarbeitung
3. Bukkit-Mannequin-Anwendung über `Bukkit.getScheduler().runTask(plugin, ...)`

Das ist genau die relevante Trennung: Netzwerk außerhalb des Main Threads, Entity-/Bukkit-Mutation zurück auf den Serverthread.

**Befund:** 🟢.

## 3.4 Periodische Tasks

Mehrere periodische Tasks laufen synchron:

- Boss-Biome-Spawnprüfung
- Quest-Passivprüfung
- Scoreboard-EXP-Bar-Update
- NPC-/Companion-Zustände teilweise event-/wake-basiert

Die geprüften Tasks besitzen Shutdown-/Cancel-Pfade.

**Risiko:** Synchrones Scannen muss proportional zur Spieler-/NPC-Anzahl bleiben.

---

# 4. Speicher & Player-Referenzen

## 4.1 Positiv

`PlayerProfileManager` verwendet:

```java
Map<UUID, PlayerProfile>
```

und nicht:

```java
Map<Player, PlayerProfile>
```

Auch mehrere Runtime-Caches verwenden UUIDs.

Das reduziert die Gefahr, dass ein Offline-Player durch eine Map dauerhaft stark referenziert wird.

## 4.2 Temporäre Player-Referenzen

Lokale Methodenvariablen mit `Player` sind selbstverständlich vorhanden. Das ist kein Leak.

Die geprüften persistenten Zustandscontainer speichern überwiegend:

- UUID
- Profile
- Entity UUID
- IDs
- immutable Daten

**Befund:** 🟢.

---

# 5. Datenbank & Persistenz

## 5.1 MySQL

`DatabaseManager` verwendet HikariCP.

Vorhanden:

- Connection Pool
- Connection Timeout
- Validation Timeout
- Keepalive
- Max Lifetime
- Leak Detection
- Prepared Statements

Die Konfiguration validiert:

- Host
- Port
- Datenbank-Identifier
- Poolgröße
- Timeout
- SSL-Modus

Der SSL-Modus ist auf eine feste Whitelist begrenzt.

## 5.2 SQL-Injection

Die geprüften Spielerprofil-Abfragen verwenden Prepared Statements:

```java
PreparedStatement statement =
    connection.prepareStatement(sql);
statement.setString(1, uuid.toString());
```

Die dynamischen Identifier im DatabaseManager werden vor Verwendung validiert.

**Befund:** 🟢 kein offensichtlicher SQL-Injection-Befund in den geprüften Datenbankpfaden.

## 5.3 Transaktionen

Der Player-Save-Pfad:

- startet eine Transaktion
- prüft die Persistenzrevision
- schreibt abhängige Tabellen
- commitet
- rollt bei SQLException zurück
- stellt AutoCommit wieder her

Die Revision verhindert stale writes.

**Befund:** 🟢 robust.

---

# 6. IT-Forensik / Schadcode

## 6.1 Hardcoded Backdoors

In den geprüften Kernquellen wurde kein Mechanismus gefunden, der nach folgendem Muster arbeitet:

- versteckte UUID-OP-Freischaltung
- geheime Benutzer-/Passwort-Kombination
- automatische OP-Vergabe
- dynamisches Remote-Classloading
- versteckte Command-Ausführung
- externe Webhook-Übertragung von Spielerdaten

Die vorhandene administrative Berechtigung ist explizit:

```yaml
rpg.admin:
  default: op
```

und wird über die Command-Permissions geprüft.

**Befund:** 🟢 kein konkreter Backdoor-Befund.

## 6.2 Netzwerkzugriffe

Der auffällige externe Netzwerkcode sitzt in `ExternalSkinService`.

Bekannte Ziele:

- `textures.minecraft.net`
- `api.mineskin.org`
- vom Content angegebene HTTPS/HTTP-Bildquellen

Die Klasse enthält:

- URL-Längenlimit
- Response-Größenlimit
- Timeout
- Scheme-Prüfung
- Hostprüfung
- Schutz vor localhost/private/reserved IPs
- Candidate-Limit
- Redirect-Verhalten
- asynchrone Requests

**Befund:** 🟢/🟠. Die Schutzmaßnahmen sind gut; externe HTTP-Abhängigkeit bleibt ein Betriebsrisiko.

---

# 7. SSRF-/URL-Forensik

`ExternalSkinService` prüft externe URIs gegen private/reservierte Adressbereiche.

Das ist relevant, weil Skinquellen teilweise vom Content/Administrator kommen können.

Zusätzlich werden nur erwartete URL-Schemata akzeptiert.

**Rest-Risiko:** DNS-Rebinding-/TOCTOU-Szenarien sind bei reiner Host-IP-Prüfung grundsätzlich ein bekanntes Problem. Für normale Serverkonfiguration ist das deutlich reduziert, aber nicht mathematisch ausgeschlossen.

**Priorität:** Mittel.

---

# 8. NMS / Packets / ProtocolLib

Der Branch enthält keine separaten NMS-/Packet-/ProtocolLib-Klassen im Java-Dateibaum.

Es gibt keine gefundenen Legacy-Pfade:

- `net.minecraft.server.v1_XX_RY`
- CraftBukkit
- Bungee `ChatColor`

Das aktuelle Dialogsystem nutzt die Paper-Registrierungs-/Dialog-API und Mojang-/Paper-nahe aktuelle APIs.

**Befund:** 🟢 keine erkennbare Legacy-NMS-Abhängigkeit.

---

# 9. Dialogsystem

`PixelRPGBootstrap` registriert native Dialoge über:

- `RegistryEvents.DIALOG`
- `DialogBase`
- `DialogType`
- `DialogAction.customClick`
- `DialogTagKeys.QUICK_ACTIONS`

Das entspricht der für dieses Projekt geforderten aktuellen Paper-26.x-Mechanik.

**Befund:** 🟢.

---

# 10. Commands & Permission-Sicherheit

Der zentrale Command-Baum verwendet explizite Subcommand-Permissions.

Im aktuellen Root-Baum vorhanden:

- item
- player
- debug
- party
- questlog
- dialogue
- guild
- companion
- npc
- shop
- quest
- boss
- region
- edit

Administrative Komponenten verwenden überwiegend `rpg.admin`.

Player-Funktionen verwenden `rpg.member` oder keine zusätzliche Subcommand-Permission, wenn sie bewusst allgemein verfügbar sind.

**Befund:** 🟢.

## Kritischer Integrationsbefund

`PartySubCommand` referenziert `PartyGUI`, obwohl `PartyGUI.java` im aktuellen Branch-Dateibestand nicht vorhanden ist.

Das ist kein Berechtigungsproblem, sondern ein konkreter Integrationsfehler.

**Priorität:** Kritisch.

---

# 11. Exploit- und Input-Validierung

Die geprüften Systeme validieren zahlreiche Eingaben:

- UUIDs
- Level
- XP
- Gold
- Profession
- Item IDs
- Item Stats
- Zahlenwerte
- finite Doubles
- Mengen
- Rezeptdaten
- Bossdefinitionen
- Datenbank-Identifier
- SSL-Modus
- Skin URLs
- externe Response-Größen

Die Crafting-Registry prüft zusätzlich Abhängigkeiten und Custom-Item-Referenzen.

**Befund:** 🟢 insgesamt solide.

## Restbefund

YAML-basierte Laufzeitdaten wie Shop-/NPC-Dateien sind weniger streng schema-validiert als Crafting-/Bossdaten.

**Priorität:** Mittel.

---

# 12. Item-/Dupe-Risiken

Die Item- und Trade-Systeme arbeiten nicht ausschließlich mit Materialnamen, sondern besitzen PixelRPG-Item-IDs/PDC-basierte Identifikation.

Das ist grundsätzlich geeignet, Custom Items von Vanilla-Materialien zu unterscheiden.

Der Trade-Depot-Kaufpfad prüft:

- Listing-ID
- Käufer
- Verkäufer
- Warenbestand
- verfügbaren Platz
- Profilzustände
- Persistenz

**Kein konkreter Dupe-Bug wurde aus den geprüften Pfaden bewiesen.**

Für eine endgültige Dupe-Freigabe wären allerdings Belastungstests mit parallelen Klicks/Transaktionen erforderlich.

---

# 13. Performance

## 13.1 Positiv

- UUID-Caches statt Player-Key-Caches
- asynchrones Profil-I/O
- asynchrones File-I/O
- event-driven Quest-Wakeups
- event-driven NPC-/Companion-Reaktionen
- HikariCP
- vorbereitete SQL-Abfragen
- Task-Cancel beim Shutdown

## 13.2 NPC-Resync

Ein relevanter Skalierungspunkt ist die Resynchronisation eines Spielers gegen bekannte NPCs.

Wenn die Gesamtzahl der NPCs stark wächst, kann eine globale Iteration pro Resync teuer werden.

**Empfehlung:** Chunk-/Region-Index auch für Player-Resync konsequent verwenden.

## 13.3 Scoreboard

`ScoreboardService` aktualisiert die EXP-Bar periodisch:

```java
Bukkit.getScheduler().runTaskTimer(
    plugin,
    this::refreshExperienceBars,
    0L,
    80L
);
```

Die Methode iteriert über Online-Spieler.

Bei hoher Spielerzahl ist das linear:

`O(P)`

alle 80 Ticks.

Bei normalen Servergrößen ist das vertretbar.

## 13.4 Scoreboard Guild Updates

Einige Guild-Updates iterieren über vorhandene Scoreboard-States bzw. Online-Spieler.

Auch hier gilt linearer Aufwand.

**Priorität:** Niedrig bis Mittel.

---

# 14. Synchrones I/O – konkreter Befund

Der wichtigste konkrete Performance-Befund außerhalb des Datenbank-I/O ist `ShopManager.save()`.

Der Pfad ruft synchron:

```java
yaml.save(file);
```

auf.

Wenn dieser Pfad durch häufige Adminänderungen oder andere Runtime-Aktionen ausgelöst wird, kann die Dateischreiboperation den Serverthread blockieren.

**Empfehlung:** Snapshot auf dem Main Thread, Schreiben über bestehenden Async-File-Writer.

Nicht den gesamten ShopManager blind asynchron machen: Bukkit-/Inventory-Zustände müssen weiterhin synchron gelesen werden.

---

# 15. Fehlerbehandlung

Die geprüften Kernkomponenten besitzen umfangreiche `try/catch`- und `finally`-Blöcke.

Positiv:

- SQL Rollback
- Executor Shutdown Fallback
- InterruptedException-Behandlung
- temporäre Datei-Cleanup
- HTTP-Fehler als failed futures
- ungültige UUIDs werden verworfen
- ungültige Enum-Werte werden abgefangen

## Negativer Restpunkt

Mehrere Datenparser verwenden bewusst:

```java
catch (IllegalArgumentException ignored) {
}
```

Das ist bei optionalen/alten Datenformaten teilweise sinnvoll, kann aber fehlerhafte Contentdaten unsichtbar machen.

**Empfehlung:** Bei Admin-/Contentdaten mindestens Warn-Logging mit Dateiname/Key.

**Priorität:** Niedrig bis Mittel.

---

# 16. Robustheit bei Shutdown

Die Architektur besitzt einen klaren Shutdown-Pfad.

Besonders positiv:

- Executor werden beendet.
- Pending Saves werden berücksichtigt.
- Tasks werden gecancelt.
- Scoreboards werden bereinigt.
- Services werden deregistriert.
- statische Plugin-Referenz wird gelöscht.

**Befund:** 🟢.

---

# 17. Vanilla-EXP-Bar / UI-Konflikt

`ScoreboardService` setzt aktiv:

```java
player.setLevel(level);
player.setExp(progress);
```

Damit wird die Vanilla-EXP-Bar für PixelRPG-Zwecke übernommen.

Das ist kein klassischer Memory-/Performance-Bug.

Es ist aber eine **bewusste UI-Priorisierung**.

Folge:

- Vanilla-XP-Anzeige wird durch PixelRPG-Level/Progress ersetzt.
- Andere Systeme, die `setExp()`/`setLevel()` verwenden, können konkurrieren.
- Quest-Navigation muss daher nicht gegen eine zweite EXP-Bar „gewinnen“, sondern gegen die Scoreboard-/HUD-Logik getrennt betrachtet werden.

Für eine Quest-Navigation ist daher ein eigenes UI-Channel-Konzept sinnvoll.

**Befund:** 🟠 Integrations-/UI-Risiko, kein Sicherheitsproblem.

---

# 18. Content-/Runtime-Integration

Die Codebasis besitzt umfangreiche Contentdaten, aber nicht jeder Contentbestand ist automatisch Weltcontent.

Besonders relevant:

- `npcs.yml` wird runtime-seitig erwartet.
- `shops.yml` wird runtime-seitig erwartet.
- Regionen sind technisch vorhanden, konkrete Weltregionen müssen separat persistiert/angelegt werden.

Das bedeutet:

> Die Runtime-Engine ist vorhanden; ein leerer Datenordner bedeutet nicht automatisch eine vollständig bevölkerte RPG-Welt.

---

# 19. Build-Konfiguration

`build.gradle` erfüllt die vorgegebene Basis:

- Java 25
- Paperweight 2.0.0-beta.21
- Paper Dev Bundle 26.2.build.121-stable
- Shadow 9.6.1
- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0

Shadow Relocations:

- Gson → `de.pixelrpg.rpg.libs.gson`
- Hikari → `de.pixelrpg.rpg.libs.hikari`
- MySQL → `de.pixelrpg.rpg.libs.mysql`

Die vorhandenen Verification-Tasks bleiben Bestandteil von `check`.

**Befund:** 🟢.

---

# 20. Sicherheitsklassifikation

| Bereich | Befund |
|---|---|
| Backdoor | 🟢 kein konkreter Befund |
| OP-Hardcoding | 🟢 kein konkreter Befund |
| Remote-Classloading | 🟢 kein Befund |
| SQL Injection | 🟢 kein konkreter Befund |
| SSRF | 🟢/🟠 Schutz vorhanden, Rest-Risiko |
| Dupe | 🟢 kein konkreter Befund aus statischer Prüfung |
| Packet Abuse | 🟢 keine eigene Packet-Schicht |
| NMS Legacy | 🟢 kein Befund |
| Player Memory Leak | 🟢 kein offensichtlicher Kernbefund |
| Async Bukkit-Zugriff | 🟢 überwiegend sauber getrennt |
| Main-thread I/O | 🟠 Shop-Persistenz |
| Scheduler Leak | 🟢 Shutdown-Pfade vorhanden |
| Input Validation | 🟢/🟠 abhängig vom Datenformat |

---

# 21. Priorisierte To-do-Liste

## 🔴 Kritisch

### A-01 – PartyGUI-Referenz entfernen

**Datei:** `PartySubCommand.java`

Die Referenz auf die nicht vorhandene `PartyGUI` muss auf das aktuelle Dialogsystem umgestellt oder vollständig entfernt werden.

### A-02 – Build auf dem aktuellen HEAD verifizieren

Der GitHub-Combined-Status des untersuchten HEAD enthält aktuell keine Status-Einträge. Daher ist aus dem Connector kein erfolgreicher Build ableitbar.

Der nächste technische Schritt sollte ein echter Gradle-Build auf dem aktuellen Branch sein.

---

## 🟠 Mittel

### A-03 – Shop-Speicherung vollständig asynchronisieren

Main-Thread-Snapshot:

```java
String serialized = yaml.saveToString();
fileWriter.submit(path, serialized);
```

Die Bukkit-Objekte müssen vorher synchron gelesen werden.

### A-04 – NPC-Resync räumlich indexieren

Nicht:

```java
for (Npc npc : allNpcs) {
    ...
}
```

sondern möglichst:

```text
Player → Chunk → relevante NPCs
```

### A-05 – Runtime-YAMLs schema-validieren

Für:

- npcs.yml
- shops.yml
- weitere Weltdefinitionen

sollten Pflichtfelder und Typen beim Laden geprüft und bei Fehlern mit präzisem Warnlog gemeldet werden.

### A-06 – Externe Skinquellen weiter härten

Optional:

- DNS-Rebinding-resistente Verbindungsauswahl
- Host-Allowlist für bekannte Provider
- strengere Content-Type-Prüfung
- Image-Decoding-Limits
- Rate-Limit pro Quelle

---

## 🟡 Niedrig

### A-07 – Ignorierte Parserfehler loggen

Statt:

```java
catch (IllegalArgumentException ignored) {
}
```

bei Contentdaten:

```java
catch (IllegalArgumentException exception) {
    logger.warning("Invalid content value ...");
}
```

### A-08 – Scoreboard-Guild-Updates messen

Bei steigender Spielerzahl Profiling durchführen.

### A-09 – EXP-Bar und Quest-HUD klar trennen

Die EXP-Bar sollte als reservierter PixelRPG-UI-Kanal dokumentiert werden, damit zukünftige Systeme nicht versehentlich `setExp()` überschreiben.

---

# 22. Endurteil

**Statischer Stabilitäts-/Sicherheitsstand: 7/10**

### Begründung

**+** moderne Paper-Architektur  
**+** Java-25-Basis  
**+** native Dialog-API  
**+** UUID-orientierte Player-State-Verwaltung  
**+** asynchrones Persistenzsystem  
**+** HikariCP + Prepared Statements  
**+** Transaktions-/Revision-Schutz  
**+** gute URL-/SSRF-Grundhärtung  
**+** kontrollierte Shutdowns  
**+** vorhandene Build-Verifikationen  

gegen:

**−** konkrete PartyGUI-Inkonsistenz  
**−** synchrones Shop-I/O  
**−** einige lineare globale Scans  
**−** externe Skin-Infrastruktur  
**−** teilweise schwächere Runtime-YAML-Validierung  
**−** umfangreicher Content benötigt noch Welt-/Runtime-Anbindung  
**−** kein aktueller CI-Status am geprüften HEAD

Die Bewertung ist ausdrücklich ein **technischer Zustandswert des untersuchten Codes**, keine Aussage über die Qualität einer einzelnen Person oder Commit-Historie.

---

# 23. Abschluss

Dieser Audit bewertet den tatsächlichen Branch `test` am Prüfstand 25.09.2026. Historische Commitzahlen und vermutete Entwicklerabsichten wurden nicht als Evidenz verwendet.

Die getrennte Feature-/Content-Forensik liegt in **`Forensik.md`**.
