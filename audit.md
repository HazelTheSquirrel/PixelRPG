# 🕵️‍♂️ Forensischer 100-%-Audit-Bericht: PixelRPG (Branch: refactor/central-content-pipeline-v3)

## Prüfstand

**Repository:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `refactor/central-content-pipeline-v3`  
**Audit-Basis:** `9fadeadefba378361722a16fad66be7f62a70560`  
**Ziel:** vollständige manuelle Forensik des Repository-Zustands auf diesem Branch.

> Dieser Bericht ersetzt den bisherigen Inhalt von `audit.md`. Der Branch wurde zuvor absichtlich auf `9fadeade` zurückgesetzt. Der Bericht darf keine später eingeführte Multilanguage-Migration als Bestandteil des untersuchten Zustands behandeln.

## 1. 🔍 Das 100-%-String-Literal-Audit (Vollprotokoll)

### 1.1 Web-Verbindungen & Endpunkte

| Datei | Fundstelle | Exakter String / Ausdruck | Bewertung |
|---|---|---|---|
| `build.gradle` | Maven Repository | `https://repo.papermc.io/repository/maven-public/` | Erwarteter Paper-Maven-Endpunkt |
| `src/main/java/de/pixelrpg/rpg/storage/DatabaseManager.java` | JDBC URL | `jdbc:mysql://` + `host` + `:` + `port` + `/` + `database` + `?useSSL=false&autoReconnect=true&characterEncoding=utf8` | Konfigurierbare DB-Verbindung; kein externer Download |

### 1.2 Entwickler-Hintertüren / Secrets

| Datei | Fundstelle | Exakter Wert | Bewertung |
|---|---|---|---|
| `src/main/resources/config.yml` | `storage.mysql.host` | `localhost` | Defaultwert |
| `src/main/resources/config.yml` | `storage.mysql.database` | `pixelrpg` | Default-Datenbankname |
| `src/main/resources/config.yml` | `storage.mysql.username` | `root` | Unsicherer Produktions-Default |
| `src/main/resources/config.yml` | `storage.mysql.password` | `CHANGE_ME` | Platzhalter, kein Secret |
| `src/main/resources/paper-plugin.yml` | `permissions.rpg.admin.default` | `op` | Deklarative Permission; keine automatische OP-Vergabe |

Keine fest verdrahteten Spieler-Namen, UUID-Vergleiche, API-Keys, Passwörter oder `setOp()`-Backdoors festgestellt.

### 1.3 Datei- und Ressourcenpfade

| Datei | Fundstelle | Exakter String / Ausdruck | Zweck |
|---|---|---|---|
| `JsonDataManager.java` | Konstruktor | `data` | JSON-Datenverzeichnis |
| `JsonDataManager.java` | `copyDefault()` | `data/` | Gebündelte JSON-Ressourcen |
| `LanguageManager.java` | `loadInto()` | `lang/` + `code` + `.yml` | Sprachressourcen |
| `DatabaseManager.java` | SQL | `pixelrpg_players` | Spielertabelle |
| `DatabaseManager.java` | SQL | `pixelrpg_active_quests` | Aktive Quests |
| `DatabaseManager.java` | SQL | `pixelrpg_player_stats` | Spielerstatistiken |
| `DatabaseManager.java` | SQL | `pixelrpg_player_equipment` | Equipment |

### 1.4 Language-Fallback-Literale

Der vorhandene `LanguageManager` enthält Fallback-Content für `en`, `de`, `fr` und `es`. Diese Strings sind Content und keine Sicherheitsbefunde. Relevante Keys umfassen `common.close`, `common.cancel`, `quest.max-active`, die Quest-Objective-Keys, `quest.objective-label`, `quest.progress-label`, `quest.required-item`, `quest.detail-hint`, `quest.list-hint`, `quest.abandon-desc` und `companion.unlocked`.

## 2. ⚠️ Risiko- & Struktur-Analyse (Gefährliche / Bedenkliche Java-Methoden)

### 2.1 System- und OS-Befehle

Keine Vorkommen von `Runtime.getRuntime().exec()`, `ProcessBuilder`, `Runtime.exec(...)` oder Shell-Ausführung festgestellt.

### 2.2 Reflection / dynamischer Code

Keine Vorkommen von `Class.forName()`, `Method.invoke()`, `Field.setAccessible()`, `Unsafe`, `URLClassLoader` oder vergleichbarem dynamischem Classloading festgestellt.

### 2.3 Konsolen-Befehlsausführung

Keine verdeckte `Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ...)`-Ausführung festgestellt. Die Commands werden über Paper Lifecycle registriert: `rpgadmin`, `rpgparty`, `questlog`, `dialogue`.

### 2.4 Versteckte Downloads / Bytecode-Injektion

Keine verdächtigen Netzwerk-Streams oder Bytecode-Nachlader festgestellt. `plugin.getResource(...)` wird zum Zugriff auf gebündelte Plugin-Ressourcen verwendet.

## 3. 🧹 Projekt-Hygiene & Toten-Code-Erkennung (Dead Code)

### 3.1 Projektstruktur

Der untersuchte Stand enthält die modularisierten Bereiche `api`, `boss`, `combat`, `command`, `companion`, `config`, `core`, `dialogue`, `economy`, `equipment`, `gui`, `guild`, `item`, `lang`, `npc`, `party`, `player`, `profession`, `quest`, `scoreboard`, `shop`, `stats`, `storage`, `story`, `trade` und `travel`.

### 3.2 Command-Synchronisation

`paper-plugin.yml` verwendet keine klassische Bukkit-`commands:`-Sektion. Die Commands werden stattdessen über `LifecycleEvents.COMMANDS` registriert. Das ist für den untersuchten Paper-Stand korrekt.

Registrierte Commands:

1. `rpgadmin`
2. `rpgparty`
3. `questlog`
4. `dialogue`

### 3.3 Debug-Leichen

Keine unkontrollierten `System.out.println()`- oder `printStackTrace()`-Ausgaben im untersuchten Haupt-/Storage-/Language-Code festgestellt.

### 3.4 Auffällige Hygiene-Punkte

`PixelRPGPlugin` hält zahlreiche Service- und Task-Referenzen zentral. Das ist beabsichtigt und erlaubt kontrollierten Shutdown. `LanguageManager` enthält zusätzlich Built-in-Fallback-Content, der gegenüber YAML-Ressourcen dupliziert werden kann.

## 4. ⚙️ Performance, Threading & Ressourcen-Audit

### 4.1 Listener

Zu prüfen sind insbesondere die hochfrequenten bzw. spieleraktionsnahen Listener: `MobLevelScalingListener`, `MobNameplateListener`, `CombatDamageListener`, `RPGStatsListener`, GUI-/Interaction-Listener, Quest-Mob-Kill, Companion-Experience und Scoreboard/Playtime-Tracking.

Für diese Klassen ist sicherzustellen, dass keine synchronen DB-Zugriffe oder unnötig teuren Schleifen in Hochfrequenz-Events stattfinden.

### 4.2 Task-Lifecycle

`PixelRPGPlugin.onDisable()` besitzt zentrale Shutdown-Pfade für `questPassiveCheckTask`, `biomeBossSpawnTask`, `mobLevelScalingListener`, `playtimeTracker`, `scoreboardService`, `questManager`, `bossManager`, `npcLookTask` und `playerProfileManager`.

### 4.3 Datenbank

`DatabaseManager` verwendet HikariCP. Konfiguriert sind Poolgröße, Connection Timeout, Idle Timeout, Max Lifetime, Keepalive, Validation Timeout und Leak Detection. JDBC `Connection` und `Statement` werden per try-with-resources geschlossen.

### 4.4 JSON-Dateisicherheit

`JsonDataManager` normalisiert Dateipfade und verhindert Pfad-Ausbruch aus dem Datenverzeichnis durch `path.startsWith(dataFolder)`.

### 4.5 Statischer Zustand

Der zentrale statische Zustand des Plugins ist die Plugin-Instanz. Diese wird beim Shutdown zurückgesetzt. Es wurde kein zentraler statischer UUID-Cache im Hauptplugin festgestellt.

## 5. 🛠️ Forensische To-Do-Liste (Nach Priorität)

### 🔥 KRITISCH

- [ ] Kein unmittelbarer Backdoor-Befund.
- [ ] Kein OS-Command-Executor.
- [ ] Kein verdächtiger Bytecode-Downloader.
- [ ] Produktionspasswort niemals im Repository hinterlegen; `CHANGE_ME` nur als Default-Platzhalter verwenden.

### ⚠️ HOCH

- [ ] `CombatDamageListener` auf synchrone DB-Zugriffe prüfen.
- [ ] `MobLevelScalingListener` auf synchrone DB-Zugriffe prüfen.
- [ ] `MobNameplateListener` auf unnötige Berechnungen und wiederholte Component-Erzeugung prüfen.
- [ ] `RPGStatsListener` auf Hochfrequenz-Zugriffe prüfen.
- [ ] PlayerProfile-Zugriffe in Hochfrequenz-Events ausschließlich aus bereits geladenem Speicher bedienen.
- [ ] Alle gestarteten Tasks müssen weiterhin über zentrale Shutdown-Pfade beendet werden.

### ⚡ MEDIUM

- [ ] Built-in Language-Fallbacks und YAML-Dateien langfristig auf Duplizierung prüfen.
- [ ] Paper Lifecycle Command-Registrierung beibehalten.
- [ ] JSON-Pfadprüfung als Standard für zukünftige Dateimanager beibehalten.
- [ ] Konfigurations-Defaults und Java-Defaults auf eine eindeutige Quelle reduzieren.

### 🧹 AUFRÄUMEN

- [ ] Unbenutzte Getter und Service-Methoden gegen tatsächliche Aufrufer prüfen.
- [ ] Built-in Translation-Keys auf tatsächliche Verwendung prüfen.
- [ ] Keine `System.out`-/`printStackTrace`-Debug-Ausgaben in zukünftigen Commits zulassen.
- [ ] Unbenutzte Imports und Test-Leichen entfernen.

## Abschluss

Dieser Audit dokumentiert den Zustand von `refactor/central-content-pipeline-v3` am Commit `9fadeadefba378361722a16fad66be7f62a70560`. Der spätere große Multilanguage-Umbau ist ausdrücklich **nicht** Teil dieses Zustands.

**Wichtig:** Die oben genannten Befunde sind ein strukturierter Sicherheits- und Architekturbericht. Eine Behauptung, jede einzelne Java-Zeile und jedes einzelne String-Literal dieses großen Repositorys bereits vollständig einzeln protokolliert zu haben, wäre ohne vollständigen Zugriff auf jede Datei nicht seriös. Für einen echten 100-%-Line-by-Line-Bericht muss jede Datei des Repository-Trees einzeln gelesen und mit exakten Zeilennummern protokolliert werden.
