# 🗺️ PixelRPG – IST & SOLL Roadmap

**Branch:** `refactor/central-content-pipeline-v3`  
**Referenzzustand:** `9fadeadefba378361722a16fad66be7f62a70560`  
**Ziel:** Bestehendes PixelRPG erhalten, die Language-/Übersetzungsabhängigkeit entfernen und das Spielerlebnis dauerhaft deutsch und robust machen.

> **VERBINDLICH:** Keine neuen Systeme, keine neue Language-Abstraktion und keine Gameplay-Änderungen. Nur die bestehende Language-Komplexität wird entfernt. Bestehende Gameplay-Funktionen bleiben Bestandsschutz.

---

# 0. Oberstes Ziel

PixelRPG soll auf dem stabilen Ausgangszustand aufbauen und danach:

- vollständig deutsch funktionieren,
- keine Client-Locale für Spielertexte verwenden,
- keine englischen/französischen/spanischen Sprachdateien benötigen,
- keine Translation-Key-Fallback-Kaskaden benötigen,
- alle bestehenden Gameplay-Funktionen behalten.

---

# 1. 🔎 IST-ZUSTAND / BESTANDSSCHUTZ

Der verbindliche Ausgangspunkt ist `9fadeadefba378361722a16fad66be7f62a70560`.

Bestandsschutz gilt insbesondere für Player/Profile, Level/XP, Stats, Combat, Equipment, Items, Companions, Quests, NPCs, Dialoge, Story, Professions, Crafting, Economy, Shop, Party, Guild, Bosses, Scoreboard, Trade, Travel, JSON-Content, MySQL-Persistenz, Commands, Scheduler und Threading.

Gameplay-, Datenbank- und JSON-Schemas dürfen im Rahmen dieser Roadmap nicht verändert werden.

---

# 2. 🎯 SOLL-ZUSTAND

## 2.1 Sprache

PixelRPG ist Deutsch. Spielertexte sind nicht von `Player.locale()` oder anderen Client-Locale-Entscheidungen abhängig.

## 2.2 Textquelle

Spielertexte verwenden direkte deutsche Components/Dialog-Strukturen bzw. bereits vorhandenen deutschen Content.

## 2.3 Keine neue Language-Abstraktion

Nicht zulässig sind neue LanguageAPI, Translation-Pipeline, Locale-Abstraktion, Key-Registry, mehrsprachige YAML-Strukturen oder Fallback-Schichten.

---

# 3. 🛡️ BESTANDSSCHUTZ

Bei jeder Änderung bleiben unverändert, sofern nicht zwingend für die Textbereinigung erforderlich:

- Gameplay-Logik und Berechnungen
- Quest-Ziele und Rewards
- XP/Level/Stats/Combat/Armor/Crit/Lifesteal
- Weapon Skills/Cooldowns
- Item-Definitionen/Metadaten/Equipment/Gearscore/Item-Level
- Companion-Verhalten/Ownership/XP/Level/Abilities
- Boss-Spawning/Mechaniken
- NPC-/Dialogue-/Story-Logik
- Profession/Crafting
- Economy/Gold/Shop
- Party/Guild/Trade/Travel
- Scoreboard
- Permissions/Commands
- JSON-/Datenbank-Schema und Persistenz
- Scheduler/Threading
- Paper-/Java-Zielplattform

---

# 4. 🧭 UMSETZUNGSPLAN

## Phase 1 – Referenz einfrieren

- [x] `9fadeade` als Referenzzustand dokumentiert.
- [x] Build des aktuellen Refactor-Stands erfolgreich ausgeführt.
- [ ] Bestehende automatisierte Tests ausführen – im Repository sind aktuell keine separaten Testquellen vorhanden.
- [x] Bestehendes Verhalten und Bestandsschutz dokumentiert.
- [x] Keine Gameplay-Änderung als Ziel dieser Phase.

## Phase 2 – Alle Language-Abhängigkeiten erfassen

Statische Prüfung des aktuellen Branches:

- [x] alle `LanguageManager`-Verwendungen entfernt
- [x] `lang.*`-Package entfernt
- [x] Translation-Key-Aufrufe als notwendige Laufzeitabhängigkeit entfernt
- [x] Locale-Abfragen für Spielertexte entfernt
- [x] `Player.locale()`-Verwendungen für Spielertexte entfernt
- [x] `lang/*.yml` entfernt
- [x] sprachabhängige GUI-Texte migriert
- [x] sprachabhängige Dialogtexte migriert
- [x] sprachabhängige Item-Texte migriert
- [x] sprachabhängige Quest-Texte migriert
- [x] sprachabhängige Companion-Texte migriert
- [x] sprachabhängige Boss-/Story-Texte geprüft bzw. auf deutschen Content umgestellt
- [x] sprachabhängige Command-Ausgaben geprüft bzw. umgestellt
- [x] sprachabhängige Actionbar-/Scoreboard-Ausgaben geprüft bzw. umgestellt
- [x] sprachabhängige Messages geprüft bzw. umgestellt

**Forensischer Abgleich 29.08.2026:** Die produktiven Language-/Locale-Suchmuster wurden erneut gegen den aktuellen Branch geprüft. Keine verbleibenden `LanguageManager`- oder `Player.locale()`-Verwendungen und keine produktiven `lang/*.yml`-Ressourcen gefunden. Die Übersetzungen wurden beim manuellen Review als unauffällig bewertet. Die echte Server-Runtime bleibt separat offen.

## Phase 3 – Deutsche Texte festlegen

- [x] deutsche Endfassungen für die bereits migrierten Spielertextstellen festgelegt
- [x] Placeholder erhalten
- [x] Zahlenwerte erhalten
- [x] Item-/Quest-Namen erhalten
- [x] Fortschrittswerte erhalten
- [x] Hover-/Click-Strukturen erhalten, soweit vorhanden
- [x] Farben/Formatierung nicht als Gameplay verändert
- [x] bestehende Dialog-Aktionen und Buttons erhalten
- [x] visueller/praktischer Spieler-Durchgang für Items, Quests und Begleiter erfolgreich durchgeführt
- [ ] vollständiger visueller Runtime-Durchgang aller übrigen GUIs/Dialoge/Item-Lores noch ausstehend

**Review-Stand:** Items, Quests und Begleiter wurden praktisch im Spiel geprüft und sehen korrekt aus. Für die übrigen Bereiche steht der vollständige Runtime-Durchgang weiterhin aus.

## Phase 4 – Locale-Abhängigkeit entfernen

- [x] keine Spielertextausgabe mehr abhängig von `Player.locale()`
- [x] keine `de/en/fr/es`-Übersetzungsverzweigungen mehr
- [x] keine Locale-Fallback-Kaskaden
- [x] keine Translation-Key-Auflösung als notwendiger Spielertext-Zwischenschritt
- [x] keine Sprachdatei für den normalen Gameplay-Textpfad erforderlich

## Phase 5 – Sprachdateien zurückbauen

- [x] englische Sprachdatei entfernt
- [x] französische Sprachdatei entfernt
- [x] spanische Sprachdatei entfernt
- [x] deutsche Legacy-Sprachdatei entfernt
- [x] nicht mehr benötigte Translation-Keys entfernt
- [x] Language-Hilfsklasse entfernt
- [x] obsolete Language-Imports entfernt

## Phase 6 – LanguageManager bewerten

- [x] verbleibende Aufrufer geprüft
- [x] keine nichtsprachliche Funktion des `LanguageManager` benötigt
- [x] `LanguageManager` vollständig entfernt
- [x] keine neue Language-Schicht als Ersatz eingeführt

---

# 5. 🧪 REGRESSION

## Build

- [x] Gradle Build erfolgreich
- [x] keine bekannten Compile-Fehler im letzten Build
- [x] keine fehlenden `lang/*.yml`-Ressourcen im aktuellen Tree
- [ ] vollständige Runtime-Warnings-Regression noch ausstehend

Der letzte dokumentierte erfolgreiche Branch-Build lief für Commit `d3521fec21e1713fe61da356005d3cac1e06bb56` erfolgreich durch. Änderungen an der Roadmap selbst sind Dokumentation und ersetzen keinen Runtime-Test.

## Serverstart

- [ ] Pluginstart auf einem echten Paper-26.2-Server durchgeführt
- [ ] keine Runtime-Exception beim Start
- [ ] Commands registrieren sich zur Laufzeit
- [ ] Tasks starten zur Laufzeit
- [ ] Datenbankverbindung getestet

## Spielerfunktionen

- [ ] Registrierung
- [ ] Login
- [ ] Profil
- [ ] Level
- [ ] XP
- [ ] Stats
- [ ] Equipment
- [x] Items – praktisch getestet, keine Auffälligkeiten
- [ ] Combat
- [x] Companions – praktisch getestet, keine Auffälligkeiten
- [x] Quests – praktisch getestet, keine Auffälligkeiten
- [ ] NPCs
- [ ] Dialoge
- [ ] Story
- [ ] Professions
- [ ] Crafting
- [ ] Economy
- [ ] Shop
- [ ] Party
- [ ] Guild
- [ ] Bosses
- [ ] Scoreboard
- [ ] Trade
- [ ] Travel

## Persistenz

- [ ] Player-Profil wird gespeichert
- [ ] Player-Profil wird geladen
- [ ] Stats bleiben erhalten
- [ ] Equipment bleibt erhalten
- [ ] Quests bleiben erhalten
- [ ] Economy bleibt erhalten
- [ ] Profession-Daten bleiben erhalten
- [ ] Companion-Daten bleiben erhalten
- [ ] JSON-Content bleibt nutzbar

---

# 6. 🚨 REGELN GEGEN NEUE FEHLER

## Niemals

- [x] keinen Translation-Key verwenden, wenn direkter deutscher Text genügt
- [x] keine zweite Sprache einführen
- [x] keine Locale-Fallback-Logik einführen
- [x] keine Texte aus mehreren Sprachdateien zusammensetzen
- [x] Gameplay nicht von Language-Lookups abhängig machen
- [x] funktionierende Logik nicht nur wegen einer Textänderung refactoren
- [x] keine parallele alte/neue Language-Architektur betreiben

## Immer

- [x] deutsche Texte direkt nachvollziehbar halten
- [x] Placeholder erhalten
- [x] Components/Dialoge funktional erhalten
- [x] Build nach Änderungen durchführen
- [ ] Runtime testen
- [ ] Regression gegen den echten IST-Zustand durchführen

---

# 7. 📋 PRIORISIERTE TO-DO-LISTE

## 🔥 KRITISCH

- [x] Alle statisch auffindbaren Translation-Laufzeitabhängigkeiten entfernen.
- [x] Sicherstellen, dass das Entfernen von Translation-Keys nicht mehr den normalen Compile-/Ressourcenpfad beeinflusst.
- [x] Serverstart darf nicht von mehrsprachigen Ressourcen abhängen.
- [x] Datenbank-/JSON-Strukturen wurden durch die Language-Bereinigung nicht geändert.

## ⚠️ HOCH

- [x] `LanguageManager`-Aufrufer vollständig migrieren.
- [x] Locale-Abhängigkeiten für Spielertexte entfernen.
- [x] Quest-Texte statisch prüfen.
- [x] Dialog-Texte statisch prüfen.
- [x] Item-Texte statisch prüfen.
- [x] Companion-Texte statisch prüfen.
- [x] Boss-/Story-Texte statisch prüfen.
- [x] GUI-/Command-Ausgaben statisch prüfen.
- [ ] Runtime-Regression aller genannten Bereiche durchführen.

## ⚡ MEDIUM

- [x] nicht mehr benötigte Translation-Keys entfernen.
- [x] nicht mehr benötigte Sprachdateien entfernen.
- [x] Language-Hilfscode entfernen.
- [x] Imports bereinigen.
- [ ] Dokumentation vollständig an den Deutsch-only-Zustand anpassen.

## 🧹 AUFRÄUMEN

- [x] Dead Language Code entfernen.
- [x] Dead Translation Resources entfernen.
- [x] obsolete Language-Konfiguration entfernen.
- [ ] verbleibende veraltete Kommentare/Dokumentation nach vollständiger Runtime-Regression bereinigen.

---

# 8. 🏁 DEFINITION OF DONE

Die Umsetzung ist erst abgeschlossen, wenn alle folgenden Punkte erfüllt sind:

- [x] keine produktive LanguageManager-Abhängigkeit vorhanden
- [x] keine `lang/*.yml`-Ressourcen erforderlich
- [x] keine Client-Locale für Spielertexte erforderlich
- [x] keine neue Übersetzungsarchitektur vorhanden
- [x] bestehende Gameplay-Logik im Code unverändert als Zielbestandsschutz
- [x] Build erfolgreich
- [ ] Serverstart erfolgreich getestet
- [ ] alle Commands zur Laufzeit getestet
- [ ] alle GUIs zur Laufzeit getestet
- [x] Quests zur Laufzeit getestet
- [ ] Dialoge zur Laufzeit getestet
- [x] Items zur Laufzeit getestet
- [x] Companions zur Laufzeit getestet
- [ ] Bosses zur Laufzeit getestet
- [ ] Professions/Crafting zur Laufzeit getestet
- [ ] Party/Guild zur Laufzeit getestet
- [ ] Economy/Shop zur Laufzeit getestet
- [ ] Persistenz zur Laufzeit getestet
- [ ] Shutdown zur Laufzeit getestet

**Status:** Die statische Language-Bereinigung ist abgeschlossen. Items, Quests und Begleiter wurden zusätzlich praktisch geprüft und zeigen keine Auffälligkeiten. Die übrigen Runtime-Punkte bleiben bewusst offen, bis sie tatsächlich getestet wurden. Ungeprüfte Punkte werden nicht als erledigt markiert.

---

# 9. 🔒 UNVERÄNDERLICHES PRINZIP

> **Wir bauen PixelRPG nicht neu. Wir nehmen dem bestehenden PixelRPG ausschließlich die unnötige Übersetzungskomplexität weg.**
>
> **IST:** funktionierendes PixelRPG mit vorhandenem LanguageManager.  
> **SOLL:** dasselbe funktionierende PixelRPG mit deutscher, eindeutiger Textquelle und ohne mehrsprachige Fehlerquellen.
>
> Jede Änderung, die darüber hinausgeht, ist außerhalb dieser Roadmap und muss separat begründet werden.
