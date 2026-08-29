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

**Forensischer Abgleich 29.08.2026:** Keine verbleibenden `LanguageManager`- oder `Player.locale()`-Verwendungen und keine produktiven `lang/*.yml`-Ressourcen gefunden. Die bereits migrierten Übersetzungen wurden praktisch geprüft. Dabei wurden zusätzlich zwei konkrete Deutsch-only-Reste gefunden: englische Stat-Namen im `minecraft:quick_actions`-Charakterprofil sowie englische Item-Namen in diesem Bereich.

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
- [x] praktische Prüfung der Dialogue-Texte erfolgreich durchgeführt
- [x] Chat-Nachrichten praktisch geprüft; Ausgaben korrekt
- [ ] englische Stat-Namen im `minecraft:quick_actions`-Charakterprofil vollständig auf Deutsch umgestellt
- [ ] englische Item-Namen im `minecraft:quick_actions`-Charakterprofil vollständig auf Deutsch umgestellt
- [ ] vollständiger visueller Runtime-Durchgang aller übrigen GUIs/Item-Lores noch ausstehend

**Review-Stand:** Items, Quests, Begleiter, Dialoge, NPC-Ausgaben und Chat-Nachrichten wurden praktisch geprüft. Die genannten Bereiche funktionieren, allerdings wurden im G-Charakterprofil noch englische Stat-/Item-Bezeichnungen festgestellt.

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

## Serverstart

- [x] Pluginstart auf einem echten Paper-26.2-Server durchgeführt
- [x] Serverlog beim Start ohne Fehler
- [x] keine Runtime-Exception beim getesteten Start
- [x] Tasks/NPC-Systeme im getesteten Lauf gestartet
- [x] Datenbankverbindung getestet
- [x] Datenbankverknüpfung im getesteten Lauf funktionierte

## Spielerfunktionen

- [x] Registrierung / Profil im getesteten Lauf funktionsfähig
- [x] NPCs – alle vorhandenen NPCs praktisch getestet
- [x] Dialoge – praktisch getestet
- [x] Chat-Nachrichten – praktisch getestet
- [x] Items – praktisch getestet; Funktion korrekt, aber englische Bezeichnungen im G-Charakterprofil entdeckt
- [x] Companions – praktisch getestet, keine Auffälligkeiten
- [x] Quests – praktisch getestet, keine Auffälligkeiten
- [ ] Level / XP / Stats vollständig geprüft
- [ ] Equipment vollständig geprüft
- [ ] Combat vollständig geprüft
- [ ] Story vollständig geprüft
- [ ] Professions / Crafting vollständig geprüft – dabei wurden fehlende sichtbare Rezepte und falsche Berufsauswahl bei Berufslehrer-NPCs entdeckt
- [ ] Economy
- [ ] Shop
- [ ] Party
- [ ] Guild
- [ ] Bosses
- [ ] Scoreboard
- [ ] Trade
- [ ] Travel

## Bekannte Runtime-Befunde

### Berufslehrer-NPCs

- [x] Ursache identifiziert: `ProfessionTrainerBehavior` öffnete nach dem Lernen bzw. bei bereits gelerntem Beruf die allgemeine `ProfessionDialog.open()`-Ansicht statt der berufsspezifischen Traineransicht.
- [x] Korrektur implementiert: Berufslehrer öffnen jetzt `openTrainerRecipes(player, profession)` und zeigen damit ausschließlich die Rezepte des jeweiligen Berufs.
- [ ] Runtime-Nachtest der vier Berufslehrer nach der Korrektur noch ausstehend.

### Quick Actions / G-Charakterprofil

- [ ] englische Stat-Namen vollständig auf Deutsch umgestellt
- [ ] englische Item-Namen vollständig auf Deutsch umgestellt
- [ ] Runtime-Nachtest nach der Korrektur ausstehend

## Persistenz

- [x] Datenbankverbindung im realen Serverlauf erfolgreich getestet
- [ ] Player-Profil explizit über einen vollständigen Save/Reload-Zyklus geprüft
- [ ] Stats bleiben nach Reload erhalten
- [ ] Equipment bleibt nach Reload erhalten
- [ ] Quests bleiben nach Reload erhalten
- [ ] Economy bleibt nach Reload erhalten
- [ ] Profession-Daten bleiben nach Reload erhalten
- [ ] Companion-Daten bleiben nach Reload erhalten
- [ ] JSON-Content bleibt vollständig nutzbar

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
- [ ] Runtime nach jeder Korrektur erneut testen
- [ ] Regression gegen den echten IST-Zustand vollständig abschließen

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
- [x] Quest-Texte statisch und praktisch prüfen.
- [x] Dialog-Texte statisch und praktisch prüfen.
- [x] Item-Texte statisch und praktisch prüfen.
- [x] Companion-Texte statisch und praktisch prüfen.
- [x] NPC- und Chat-Ausgaben praktisch prüfen.
- [ ] Quick-Actions-Stat-/Item-Bezeichnungen vollständig auf Deutsch bereinigen und nachtesten.
- [x] Ursache der Berufslehrer-/Rezeptanzeige identifizieren und Code korrigieren.
- [ ] Berufslehrer-/Rezeptanzeige nach der Korrektur erneut praktisch testen.
- [ ] Runtime-Regression aller genannten Bereiche vollständig durchführen.

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
- [x] Serverstart praktisch erfolgreich getestet
- [ ] alle Commands zur Laufzeit getestet
- [ ] alle GUIs zur Laufzeit vollständig getestet
- [x] Quests zur Laufzeit getestet
- [x] Dialoge zur Laufzeit getestet
- [x] Items zur Laufzeit getestet
- [x] Companions zur Laufzeit getestet
- [ ] Bosses zur Laufzeit getestet
- [ ] Professions/Crafting vollständig nach der Korrektur getestet
- [ ] Party/Guild zur Laufzeit getestet
- [ ] Economy/Shop zur Laufzeit getestet
- [ ] vollständiger Persistenzzyklus getestet
- [ ] Shutdown zur Laufzeit getestet
- [ ] G-Charakterprofil nach deutscher Stat-/Item-Bereinigung erneut getestet

**Status:** Die statische Language-Bereinigung ist abgeschlossen. Der reale Paper-26.2-Serverstart inklusive Datenbankverbindung wurde ohne Fehler getestet; NPCs und Chat-Ausgaben funktionieren. Items, Quests, Begleiter und Dialoge wurden praktisch geprüft. Zwei konkrete Restfehler wurden gefunden: Berufslehrer-NPCs öffneten die falsche, allgemeine Berufsansicht und im G-Charakterprofil existieren noch englische Stat-/Item-Bezeichnungen. Die Berufslehrer-Ursache wurde im Code korrigiert; deren Nachtest sowie die Quick-Actions-Bereinigung stehen noch aus. Ungeprüfte Punkte werden weiterhin nicht als erledigt markiert.

---

# 9. 🔒 UNVERÄNDERLICHES PRINZIP

> **Wir bauen PixelRPG nicht neu. Wir nehmen dem bestehenden PixelRPG ausschließlich die unnötige Übersetzungskomplexität weg.**
>
> **IST:** funktionierendes PixelRPG mit vorhandenem LanguageManager.  
> **SOLL:** dasselbe funktionierende PixelRPG mit deutscher, eindeutiger Textquelle und ohne mehrsprachige Fehlerquellen.
>
> Jede Änderung, die darüber hinausgeht, ist außerhalb dieser Roadmap und muss separat begründet werden.
