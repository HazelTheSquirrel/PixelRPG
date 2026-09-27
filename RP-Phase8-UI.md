# PixelRPG – RP Phase 8 Audit

**Status:** abgeschlossen auf `test`  
**Phase:** 8 – UI  
**Referenz:** RP.md  
**Technischer Stand:** Java 25 + Paper 26.2 + Mojang-Mappings

## Questtracking

- Questtracking ist in die bestehende Sidebar integriert.
- Ein aktives Questziel wird vor der permanenten Levelanzeige dargestellt.
- Storyquests erhalten eine eigene visuelle Kennzeichnung.
- Fortschritt wird direkt als aktueller/benötigter Wert angezeigt.
- Ein vorhandenes Navigationsziel wird direkt im Questbereich ausgewiesen.
- Der Quest-Tracker ist für neue Profile standardmäßig aktiviert.
- Der bestehende persistierte Schalter `quest-tracker-enabled` bleibt erhalten.

## Navigation

- Persistierte Quest-Navigationsziele werden über einen eigenen Navigation-Service verarbeitet.
- Der Vanilla-Kompass wird auf das aktive Questziel ausgerichtet.
- Storyquests werden bei mehreren aktiven navigierbaren Quests bevorzugt.
- Nach Join, Weltwechsel und Questabschluss wird die Navigation automatisch synchronisiert.
- Bei deaktiviertem Tracker oder fehlendem Ziel wird der Kompass auf die aktuelle Position zurückgesetzt.
- Weltgrenzen werden über die persistierte World-UUID geprüft; kein Zugriff auf nicht geladene Welten ist erforderlich.

## Levelanzeige / XP-Bar

- Die vorhandene Levelanzeige bleibt im Scoreboard erhalten.
- Questtracking steht vor der permanenten Levelanzeige.
- Die vorhandene Vanilla-EXP-Bar bleibt die sichtbare PixelRPG-XP-Bar.
- Die Level-/XP-Logik verwendet weiterhin die zentrale Level-1–60-Definition.

## Dialoge

- Native Paper-Dialoge bleiben die verbindliche Interaktionsbasis.
- Die bestehenden G-/Schnellaktionen wurden um Quest-Tracker und Navigation ergänzt.
- Es wurden keine alten 1.21.x-Dialog-Workarounds eingeführt.

## Party

- Die Partyverwaltung verwendet weiterhin den vorhandenen nativen `PartyDialog`.
- `/pixelrpg party` öffnet die native Party-Oberfläche.
- Ein separates Legacy-Party-GUI-System wurde nicht neu eingeführt.

## Scoreboard

- Die bestehende eventgetriebene Sidebar bleibt erhalten.
- Questtracking wird nur aus dem vorhandenen Profilzustand aufgebaut.
- Guild- und Party-Anzeigen bleiben bestehen.
- Bestehende Scoreboard-Persistenz und Profileinstellungen bleiben erhalten.

## Persistenz

- YAML lädt `quest-tracker-enabled` mit aktiviertem Standardwert.
- MySQL-Schema verwendet für neue Installationen den aktivierten Standardwert.
- Bestehende Profile behalten ihren gespeicherten individuellen Zustand.

## Forensik

- Keine Änderung an `main`.
- Entwicklung ausschließlich auf `test`.
- Keine neue Legacy-Chat- oder Legacy-NMS-API.
- Keine statischen `Player`/`Entity`/`World`-Referenzen eingeführt.
- Bestehende Build-Verifikation bleibt unverändert.
- Paper 26.2 native Dialog-/Audience-Mechanik bleibt Grundlage.

## Definition of Done – Phase 8

- [x] Questtracking
- [x] Navigation
- [x] Levelanzeige
- [x] XP-Bar
- [x] Dialoge
- [x] Party
- [x] Scoreboard
- [x] Persistenz der UI-Einstellungen
- [x] Keine Legacy-UI-API
- [x] `test` als Entwicklungsbranch
- [x] `main` unverändert
