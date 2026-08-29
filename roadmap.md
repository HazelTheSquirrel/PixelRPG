# 🗺️ PixelRPG – IST & SOLL Roadmap

**Branch:** `refactor/central-content-pipeline-v3`  
**Referenzzustand:** `9fadeadefba378361722a16fad66be7f62a70560`  
**Ziel:** Bestehendes PixelRPG erhalten, die große Language-/Übersetzungsabhängigkeit entfernen und das Spielerlebnis dauerhaft deutsch und robust machen.

> **VERBINDLICH:** Diese Roadmap ist eine IST-&-SOLL-Arbeitsgrundlage. Sie beschreibt, was bereits vorhanden ist, was geschützt werden muss und was geändert werden darf. Sie ist kein Freibrief für einen weiteren Großumbau.

---

## 0. Oberstes Ziel

PixelRPG soll auf dem stabilen Ausgangszustand aufbauen.

Die spätere große Multilanguage-Architektur wird **nicht** weitergeführt und **nicht** erneut aufgebaut.

Das endgültige System soll:

- vollständig auf Deutsch funktionieren,
- keine Übersetzungsfehler mehr erzeugen,
- nicht von mehreren Sprachdateien abhängig sein,
- nicht von der Minecraft-Client-Locale abhängig sein,
- keine Translation-Key-Parität zwischen Sprachen benötigen,
- keine Fallback-Kaskaden für Spielertexte benötigen,
- seine bestehenden Gameplay-Funktionen vollständig behalten.

**Wichtig:** Die Entfernung der Language-Komplexität darf niemals dazu führen, dass eine bereits funktionierende Funktion entfernt oder verändert wird.

---

# 1. 🔎 IST-ZUSTAND

## 1.1 Referenz

Der verbindliche Ausgangspunkt ist:

`9fadeadefba378361722a16fad66be7f62a70560`

Dieser Commit enthält bereits einen funktionierenden `LanguageManager`. Das ist bekannt und kein Widerspruch zum Ziel.

**Entscheidend:** Wir behandeln den vorhandenen LanguageManager als Bestandteil des IST-Zustands, aber wir führen den späteren großen Multilanguage-Umbau nicht fort.

## 1.2 Bestehende Funktionalität

Die bestehende Plugin-Architektur umfasst unter anderem:

- Player/Profile
- Level/XP
- Character Stats
- Combat
- Equipment
- Items
- Companions
- Quests
- NPCs
- Dialoge
- Story
- Professions
- Crafting
- Economy
- Shop
- Party
- Guild
- Bosses
- Scoreboard
- Trade
- Travel
- JSON-Content
- MySQL-Persistenz
- Commands
- Scheduler/Tasks
- GUIs

Diese Bereiche sind **Bestandsschutz**.

## 1.3 Bestehende Sprache

Der Ausgangszustand besitzt bereits sprachbezogene Infrastruktur und deutsche/weitere Sprachressourcen.

Das SOLL besteht ausdrücklich **nicht** darin, diese Architektur weiter auszubauen.

Stattdessen soll die Spielertextdarstellung auf eine einzige deutsche Quelle vereinfacht werden.

---

# 2. 🎯 SOLL-ZUSTAND

## 2.1 Sprache

**PixelRPG ist Deutsch.**

Alle Spielertexte werden deutsch ausgegeben.

Es gibt keine produktive Notwendigkeit mehr für:

- Englisch
- Französisch
- Spanisch
- weitere Locale
- Client-Locale-Erkennung
- Translation-Key-Fallbacks
- Sprachdatei-Synchronisierung

## 2.2 Deutsche Textquelle

Spielertexte sollen möglichst direkt und eindeutig aus deutschem Code-Content entstehen.

Beispielprinzip:

```java
Component.text("Quest abgeschlossen!")
```

oder die technisch passende vorhandene Component-/Dialog-Variante.

Dabei gilt:

> **Nur die Textquelle wird vereinfacht. Die Funktion, in der dieser Text verwendet wird, bleibt erhalten.**

## 2.3 Keine neue Language-Abstraktion

Wir ersetzen die alte Komplexität **nicht** durch eine neue komplexe Abstraktion.

Insbesondere nicht:

- neue LanguageAPI
- neue Translation-Pipeline
- neue Locale-Abstraktion
- neue Sprach-Key-Registry
- neue mehrsprachige YAML-Struktur
- neue Fallback-Schichten

---

# 3. 🛡️ BESTANDSSCHUTZ

Bei jeder Änderung gilt:

### Darf nicht verändert werden, außer es ist zwingend für die deutsche Textumstellung erforderlich

- Gameplay-Logik
- Quest-Logik
- Quest-Ziele
- Quest-Rewards
- XP-Berechnung
- Level-System
- Stats-Berechnung
- Combat-Berechnung
- Armor-Mitigation
- Crit-System
- Lifesteal
- Weapon Skills
- Weapon Cooldowns
- Item-Definitionen
- Item-Metadaten
- Equipment-Slots
- Gearscore
- Item-Level
- Companion-Verhalten
- Companion Ownership
- Companion XP/Level
- Companion Abilities
- Boss-Spawning
- Boss-Mechaniken
- NPC-Logik
- Dialogue-Aktionen
- Profession-System
- Crafting-Rezepte
- Economy
- Gold
- Shop-Logik
- Party-Logik
- Guild-Logik
- Trade
- Travel
- Scoreboard-Funktion
- Permissions
- Commands
- JSON-Schema
- Datenbank-Schema
- Persistenz
- Scheduler
- Threading
- Paper-/Java-Zielplattform

---

# 4. 🧭 UMSETZUNGSPLAN

## Phase 1 – Referenz einfrieren

- [ ] `9fadeade` als Referenzzustand dokumentiert lassen.
- [ ] Vor Änderungen Build ausführen.
- [ ] Bestehende Tests ausführen.
- [ ] Bestehendes Verhalten dokumentieren.
- [ ] Keine Gameplay-Änderung in dieser Phase.

**Ergebnis:** Wir können jederzeit feststellen, ob die Language-Bereinigung etwas anderes kaputtgemacht hat.

---

## Phase 2 – Alle Language-Abhängigkeiten erfassen

Manuell und vollständig prüfen:

- [ ] alle `LanguageManager`-Verwendungen
- [ ] alle `lang.*`-Packages
- [ ] alle Translation-Key-Aufrufe
- [ ] alle Locale-Abfragen
- [ ] alle `Player.locale()`-Verwendungen
- [ ] alle `lang/*.yml`
- [ ] alle sprachabhängigen GUI-Texte
- [ ] alle sprachabhängigen Dialogtexte
- [ ] alle sprachabhängigen Item-Texte
- [ ] alle sprachabhängigen Quest-Texte
- [ ] alle sprachabhängigen Companion-Texte
- [ ] alle sprachabhängigen Boss-/Story-Texte
- [ ] alle sprachabhängigen Command-Ausgaben
- [ ] alle sprachabhängigen Actionbars/Scoreboards
- [ ] alle sprachabhängigen Messages

Für jeden Fund:

1. Datei
2. Methode
3. bisherige Textquelle
4. deutscher Zieltext
5. benötigte Placeholder
6. benötigte Component-/Dialog-Struktur
7. Abhängigkeiten
8. Testfall

**Noch nichts löschen.**

---

## Phase 3 – Deutsche Texte festlegen

Jede Spielertextstelle erhält eine eindeutige deutsche Endfassung.

Dabei müssen erhalten bleiben:

- Placeholder
- Zahlenwerte
- Item-Namen
- Quest-Namen
- Fortschrittswerte
- Hover-Text
- Click-Aktionen
- Farben/Formatierung
- MiniMessage-Struktur, sofern technisch erforderlich
- Dialog-Aktionen
- Buttons
- GUI-Struktur

Beispiel:

```text
ALT:
Translation-Key → LanguageManager → YAML → Fallback → Component

SOLL:
Deutscher Text → bestehende Component-/Dialog-Struktur
```

---

## Phase 4 – Locale-Abhängigkeit entfernen

- [ ] Keine Spielertextausgabe mehr abhängig von `Player.locale()`.
- [ ] Keine Verzweigungen `de/en/fr/es` ausschließlich für Übersetzungen.
- [ ] Keine Locale-Fallback-Kaskaden.
- [ ] Keine Translation-Key-Auflösung als notwendiger Zwischenschritt.
- [ ] Keine Sprachdatei darf fehlen und dadurch eine Funktion brechen.

**Achtung:** Locale-/Language-Code darf nur entfernt werden, wenn er ausschließlich Übersetzungszwecken dient. Andere Funktionalität darf nicht versehentlich mit entfernt werden.

---

## Phase 5 – Sprachdateien zurückbauen

Erst wenn keine Laufzeitabhängigkeit mehr besteht:

- [ ] nicht mehr benötigte englische Sprachdateien entfernen
- [ ] nicht mehr benötigte französische Sprachdateien entfernen
- [ ] nicht mehr benötigte spanische Sprachdateien entfernen
- [ ] nicht mehr benötigte Translation-Keys entfernen
- [ ] ungenutzte Language-Hilfsklassen entfernen
- [ ] ungenutzte Imports entfernen

**Keine Datei löschen, solange ein tatsächlicher Aufrufer oder eine Ressourcenkonfiguration sie noch benötigt.**

---

## Phase 6 – LanguageManager bewerten

Erst nach der Migration:

- [ ] alle verbleibenden Aufrufer des `LanguageManager` prüfen
- [ ] feststellen, ob er noch irgendeine nichtsprachliche Funktion besitzt
- [ ] falls ausschließlich Übersetzung: entfernen
- [ ] falls technische Restfunktion benötigt wird: minimalisieren
- [ ] keine neue komplexe Language-Schicht als Ersatz bauen

---

# 5. 🧪 REGRESSION NACH JEDEM BEREICH

Nach jedem umgebauten Bereich:

## Build

- [ ] Gradle Build erfolgreich
- [ ] keine Compile-Fehler
- [ ] keine fehlenden Ressourcen
- [ ] keine neuen relevanten Warnings

## Serverstart

- [ ] Plugin startet
- [ ] keine Exception
- [ ] Commands registrieren sich
- [ ] Tasks starten
- [ ] Datenbankverbindung funktioniert

## Spielerfunktionen

- [ ] Registrierung
- [ ] Login
- [ ] Profil
- [ ] Level
- [ ] XP
- [ ] Stats
- [ ] Equipment
- [ ] Items
- [ ] Combat
- [ ] Companions
- [ ] Quests
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

- [ ] einen Translation-Key verwenden, wenn ein direkter deutscher Text genügt
- [ ] eine zweite Sprache einführen
- [ ] eine Locale-Fallback-Logik einführen
- [ ] einen Text aus mehreren Sprachdateien zusammensetzen
- [ ] Gameplay von einem Language-Lookup abhängig machen
- [ ] funktionierende Logik nur wegen einer Textänderung refactoren
- [ ] mehrere unabhängige Textsysteme parallel einführen
- [ ] alte und neue Language-Architektur dauerhaft parallel betreiben

## Immer

- [ ] deutsche Texte direkt nachvollziehbar halten
- [ ] Placeholder unverändert erhalten
- [ ] Components/Dialoge funktional unverändert lassen
- [ ] Build nach Änderungen durchführen
- [ ] Runtime testen
- [ ] Regression gegen den IST-Zustand durchführen

---

# 7. 📋 PRIORISIERTE TO-DO-LISTE

## 🔥 KRITISCH

- [ ] Alle Translation-Laufzeitabhängigkeiten identifizieren.
- [ ] Sicherstellen, dass kein Gameplay-System beim Entfernen eines Translation-Keys ausfällt.
- [ ] Sicherstellen, dass Serverstart ohne mehrsprachige Ressourcen funktioniert.
- [ ] Keine Datenbank-/JSON-Struktur im Rahmen der Language-Bereinigung verändern.

## ⚠️ HOCH

- [ ] `LanguageManager`-Aufrufer vollständig migrieren.
- [ ] Locale-Abfragen für Spielertexte entfernen.
- [ ] Deutsche Texte in Quests vollständig prüfen.
- [ ] Deutsche Texte in Dialogen vollständig prüfen.
- [ ] Deutsche Texte in Items vollständig prüfen.
- [ ] Deutsche Texte in Companions vollständig prüfen.
- [ ] Deutsche Texte in Boss-/Story-Systemen vollständig prüfen.
- [ ] GUI-/Command-Ausgaben vollständig prüfen.

## ⚡ MEDIUM

- [ ] nicht mehr benötigte Translation-Keys entfernen.
- [ ] nicht mehr benötigte Sprachdateien entfernen.
- [ ] Language-Hilfscode entfernen.
- [ ] Imports bereinigen.
- [ ] Dokumentation an den Deutsch-only-Zustand anpassen.

## 🧹 AUFRÄUMEN

Erst wenn alles funktioniert:

- [ ] Dead Language Code entfernen.
- [ ] Dead Translation Keys entfernen.
- [ ] Dead YAML Resources entfernen.
- [ ] Veraltete Kommentare entfernen.
- [ ] Veraltete Dokumentation entfernen.

---

# 8. 🏁 DEFINITION OF DONE

Die Umsetzung ist **nicht** abgeschlossen, nur weil die Sprachdateien gelöscht wurden.

Sie ist erst abgeschlossen, wenn:

- [ ] PixelRPG vollständig auf Deutsch funktioniert.
- [ ] keine Client-Locale mehr den Text bestimmt.
- [ ] keine englischen/französischen/spanischen Übersetzungen benötigt werden.
- [ ] kein fehlender Translation-Key einen Fehler erzeugen kann.
- [ ] keine Sprachdatei für das Gameplay erforderlich ist.
- [ ] alle bestehenden Gameplay-Systeme weiterhin funktionieren.
- [ ] Build erfolgreich ist.
- [ ] Serverstart erfolgreich ist.
- [ ] alle Commands funktionieren.
- [ ] alle GUIs funktionieren.
- [ ] Quests funktionieren.
- [ ] Dialoge funktionieren.
- [ ] Items funktionieren.
- [ ] Companions funktionieren.
- [ ] Bosses funktionieren.
- [ ] Professions/Crafting funktionieren.
- [ ] Party/Guild funktionieren.
- [ ] Economy/Shop funktionieren.
- [ ] Persistenz funktioniert.
- [ ] Shutdown funktioniert.
- [ ] keine neue Übersetzungsarchitektur vorhanden ist.

---

# 9. 🔒 UNVERÄNDERLICHES PRINZIP

> **Wir bauen PixelRPG nicht neu. Wir nehmen dem bestehenden PixelRPG ausschließlich die unnötige Übersetzungskomplexität weg.**
>
> **IST:** funktionierendes PixelRPG mit vorhandenem LanguageManager.  
> **SOLL:** dasselbe funktionierende PixelRPG mit deutscher, eindeutiger Textquelle und ohne mehrsprachige Fehlerquellen.
>
> Jede Änderung, die darüber hinausgeht, ist außerhalb dieser Roadmap und muss separat begründet werden.
