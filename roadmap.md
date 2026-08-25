# PixelRPG – Feature Roadmap

Stand: 2026-08-25 – abgeglichen mit dem aktuellen `main`-Stand

> Diese Datei ist die verbindliche Arbeits-Roadmap. Die Reihenfolge ist von oben nach unten.
> Prozentwerte beschreiben funktionale Reife des aktuell vorhandenen Codes, nicht die Menge an Code.
> 🟢 95–98 % = abgeschlossen, 🟡 50–94 % = in Arbeit, 🔴 0–49 % = offen, 🔵 = vorbereitet / Zukunft, ⚪ = verworfen.

## Arbeitsregel

```text
1. Soll-Verhalten gemeinsam definieren
2. kompletten Code dieses Features prüfen
3. Soll gegen Ist vergleichen
4. fehlende/falsche Punkte identifizieren
5. Altbestand im selben Arbeitsschritt entfernen
6. notwendige Änderungen umsetzen
7. Build durchführen
8. Runtime testen
9. PixelRPG.md und roadmap.md aktualisieren
10. bei 95–98 % abhaken
```

## Verbindliche technische Basis
- Java 25
- Paper 26.x, aktuell verbindliches Ziel: Paper 26.2
- paperweight-userdev 2.0.0-beta.21
- paperweight.paperDevBundle("26.2.build.+")
- Mojang-Mappings
- paper-plugin.yml
- native Minecraft/Paper-26.2 Dialogsystem
- Adventure Components
- kein ChatColor
- keine alten 1.21.x-APIs oder Dialogimplementierungen

## Verbindliche Designentscheidungen
- PixelRPG hat keine Klassen.
- PixelRPG hat keine frei verteilbaren Player-Attribute.
- Ein neu registrierter Spieler erhält keinen Startbonus.
- Level/XP sind die zentrale Spielerprogression.
- Das Level beeinflusst über das Balancing-System die Stärke von gefundenen Waffen, Rüstungen und normalen Companions.
- Combat ist MMORPG-nah: Gear, Stats, Weapon Skills und aktive Companions bestimmen die Kampfstärke.
- Vanilla-Schaden bleibt Bestandteil der Berechnung; Vanilla-Angriffsgeschwindigkeit bleibt unverändert.
- Character Stats: HP, Armor, Movement Speed, Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power.
- Basis-Crit-Chance ist 0 %. Crit-Chance kommt über Waffen/aktive Companion-Boni.
- Standard-Crit-Schaden ist ×2 und kann durch Gear/Companions erhöht werden.
- Lifesteal heilt den entsprechenden Prozentsatz des tatsächlich verursachten Schadens.
- Rüstung wird über eine eigene MMORPG-Mitigation berechnet.
- Rüstung: HP, Armor, Movement Speed.
- Waffen: Reach, Damage, Crit, Crit-Schaden, Lifesteal, Attack Power.
- Equipment: Helm, Brust, Hose, Schuhe, Waffe, Nebenhand.
- Gearscore wird aus Item-Level und Item-Definition/Balancing bestimmt.
- Companions geben passive Stat-Boni ausschließlich solange sie aktiv gerufen und draußen sind. Despawn entfernt den Bonus sofort. Unique Companions sind davon ausgenommen.
- Weapon Skills gehören zur jeweiligen Waffe und können aktiv ausgelöst werden. Materialbasierte Waffen erhalten unterschiedliche Skills; Bogen/Armbrust verwenden für Skills Shift+Rechtsklick, damit Vanilla-Spannen/Laden erhalten bleibt.
- PvP ist kein eigenes Gameplay-System. Falls PvP stattfindet, wird normales PixelRPG-Combat-Verhalten verwendet.
- Währung: Goldtaler. Virtuell als Kontostand und physisch als Sonnenblume. Keine weitere Währung.
- Spielerhandel erfolgt über ein Auktionshaus; vollständiger Player-Trade ist nicht priorisiert.
- Vier Professionen bleiben: BLACKSMITH, PROVISIONER, ALCHEMIST, SCHOLAR.
- Gilden sind ein eigenes soziales Gameplay-System mit gemeinsamer Gildenstadt/-basis und Gildenbank.
- Partys unterstützen gemeinsame XP, Loot-Verteilung, Party-Buffs und gemeinsamen Questfortschritt.
- Party-Komfortfunktionen sind zusätzlich über `/party` verfügbar; NPC/Dialoge bleiben die eigentliche Gameplay-Interaktion.
- Alte Default-Bosse Forest Tyrant, Frost Sovereign und Void Reaper werden nicht als finales Bosskonzept fortgeführt.
- Biom-Bosse sind regionale Spezialmonster und keine Worldbosse. Sie sind an ein bestimmtes Minecraft-Biom gebunden, können dort über die interne 10-%-Spawnprüfung erscheinen und verbrennen nicht durch Tageslicht.
- Worldbosse sind ausschließlich Admin-gestartete Events. Es gibt aktuell sechs Worldbosse mit festen Leveln, individuellen Belohnungen für aktive Teilnehmer, mehreren Phasen, Adds und individuellen Angriffsmustern. Tageslicht verbrennt sie nicht. Es gibt keinen automatischen Worldboss-Respawn.
- Worldbosse dürfen die Weltumgebung nicht dauerhaft beschädigen oder verändern: keine Blockzerstörung durch Explosionen, keine dauerhaft erzeugten Feuer-/Lava-Schäden und keine von Worldboss-Mechaniken verursachten permanenten Blockänderungen.
- Worldboss-Angriffe dürfen Spieler und Entities treffen und Vanilla-Kampfmechaniken wie Projektile, AoE, Knockback, Status-Effekte und temporäre Gefahrenbereiche verwenden.
- Ein eigenes Regions-/Mob-Scaling-System außerhalb der Biom-Bosse wird nicht umgesetzt.
- Aktuell existiert keine verbindliche Lore.
- Spieler benötigen keine Gameplay-Commands als Voraussetzung für die Systeme; die eigentlichen Interaktionen erfolgen über NPCs/Dialoge. Die wenigen Gilden-Commands dienen als komfortable Zusatzfunktionen für Einladungen, Annahme, Verlassen und Info. `/party` ist ebenfalls nur eine Komfortfunktion.
- Sprachen: Deutsch, Englisch, Spanisch, Französisch; nur dort einsetzen, wo Mehrsprachigkeit sinnvoll ist.
- Travel bleibt beim bestehenden NPC-/Waypoint-Prinzip: NPC schaltet Ziel frei, erneutes Ansprechen zeigt verfügbare Reiseziele.

---

# Aktuelle Abweichungen / zuletzt synchronisierte Punkte

## 05 – Quests
- [x] `ESCORT` als nicht mehr unterstützter Quest-Typ entfernt.
- [x] Aktive Quests im PixelRPG-Charakterdialog zeigen den tatsächlichen Questnamen.
- [x] Klick auf eine aktive Quest öffnet einen Questdetail-Dialog.
- [x] Questdetail zeigt Beschreibung und aktuelles Ziel/Fortschritt.
- [x] Questdetail zeigt vorhandene Belohnungs-/Zeitinformationen.
- [x] Aktive Quests können über den Questdetail-Dialog abgebrochen werden.
- [ ] vollständige Runtime-Regressionstests der Quest-Dialoge noch ausstehend.

## 12 – Party
- [x] `/party` als zusätzlicher Party-Komfortcommand vorhanden.
- [x] bestehende Party-Funktionen bleiben über den Party-Command erreichbar.
- [x] NPC/Dialoge bleiben die eigentliche Gameplay-Interaktion.

---

# 17 – Bosses — 🟢 98 % ABGESCHLOSSEN

## Gemeinsames Boss-System
- [x] Boss Framework
- [x] Boss Definitions / Registry
- [x] feste Boss-Level
- [x] Boss Stats
- [x] bestehende BossBar
- [x] bestehende Attack-Patterns
- [x] datengetriebene Boss-Loot-Tables
- [x] Boss → optionaler Companion Unlock
- [x] Tageslicht verbrennt Bosse nicht
- [x] kein automatischer Respawn

## Biom-Bosse
- [x] Biom-Bosse sind keine Worldbosse
- [x] Biom-Boss ist exklusiv an ein Minecraft-Biom gebunden
- [x] interne Spawnprüfung
- [x] 10-%-Spawnchance bei der Spawnprüfung
- [x] festes Boss-Level
- [x] keine Worldboss-Phasenpflicht
- [x] keine Adds
- [x] Biom-Bindung wird bei der Runtime berücksichtigt
- [x] Tageslicht-Immunität ohne automatische allgemeine Feuer-/Lava-Immunität
- [x] finale Biom-Boss-Definitionen und thematische Vanilla-Angriffsmuster umgesetzt

## Worldbosse
- [x] sechs finale Worldbosse umgesetzt
- [x] Worldboss ist ausschließlich ein Admin-Event
- [x] Admin kann Worldboss-Event starten/spawnen
- [x] kein automatischer Worldboss-Respawn
- [x] aktive Spieler im Event-Gebiet können teilnehmen
- [x] individueller Loot für jeden aktiven Teilnehmer
- [x] mehrere HP-Phasen
- [x] große AoE-Angriffe
- [x] Positionswechsel
- [x] Adds ausschließlich bei Worldbossen
- [x] Projektil-Angriffe
- [x] Slam-Angriffe
- [x] Enrage
- [x] Reaktionsmechaniken
- [x] harte Fehlerbestrafung
- [x] lange Kämpfe / hohe Herausforderung
- [x] zeitweise gefährliche Bereiche / Boss-Mechaniken
- [x] Tageslicht-Immunität ohne automatische allgemeine Feuer-/Lava-Immunität
- [x] Worldboss-Mechaniken können keine Blöcke dauerhaft zerstören oder verändern
- [x] Worldboss-Entities und temporäre Event-Mechaniken werden beim Event-Ende bereinigt

### Aktuelle Worldbosse
- [x] Der Risskoloss
- [x] Der Sturmherrscher
- [x] Der Abgrundfürst
- [x] Der Seelenverschlinger
- [x] Der Endbote
- [x] Der Uralte Weltenwächter

## Loot / Teilnahme
- [x] datengetriebene Loot-Tables
- [x] garantierte Drops
- [x] Chance-Drops
- [x] Gold / XP Rewards
- [x] normale Bosse verwenden Party-System für Loot
- [x] Worldbosse belohnen aktive Teilnehmer individuell
- [x] Party bleibt beim Worldboss unabhängig vom individuellen Loot
- [x] Worldboss-Rewards verwenden das bestehende PixelRPG-Lootsystem
- [x] Unique Companions bleiben bis zur Fertigstellung des Companion-Systems bewusst offen

## Altbestand / Content
- [x] Forest Tyrant entfernt
- [x] Frost Sovereign entfernt
- [x] Void Reaper entfernt
- [x] keine alten Testbosse als finales Bosskonzept weitergeführt
- [x] bestehende Boss-Engine auf Biom-Bosse und Worldboss-Events ausgerichtet

---

# 18 – Regions / Mob Scaling — ⚪ VERWORFEN

Dieser Punkt wird **nicht umgesetzt**.

- [x] eigenes Region-System verworfen
- [x] Region Definitions verworfen
- [x] Mob Level Scaling über Regionen verworfen
- [x] Danger Zones verworfen
- [x] Region Rewards verworfen
- [x] Region-Verknüpfung mit Quests verworfen
- [x] Region-Verknüpfung mit Travel verworfen
- [x] RPG-Mob-Zonen verworfen

**Verbindliche Regel:** Minecraft-Biome werden ausschließlich für die Biom-Bosse aus **17 – Bosses** verwendet. Es gibt kein separates Regions-/Mob-Scaling-System.

---

# 19 – Gilden — 🟢 98 % ABGESCHLOSSEN

## Gilden-Core
- [x] Gilden-System
- [x] persistente Gilden
- [x] eindeutiger Gildenname
- [x] maximal 50 Mitglieder
- [x] Gildenmeister
- [x] Mitglieder
- [x] keine weiteren Gildenränge
- [x] Gilde erstellen ab Spieler-Level 20
- [x] Gildengründung kostet 2.500 Gold aus dem persönlichen Wallet
- [x] Gildengründung über Reception / Dialog
- [x] Gildenfunktion über `minecraft:quick_actions` (G)

## Mitglieder / Befehle
- [x] Gildenmeister kann Spieler einladen
- [x] Einladung über `/gildeneinladen <Spieler>`
- [x] Einladung über `/gildeannehmen`
- [x] Gilde verlassen über `/gildeverlassen`
- [x] Gildeninformationen über `/gildeinfo`
- [x] Befehle sind Komfortfunktionen; das System bleibt über NPC/Dialoge zugänglich
- [x] nur Gildenmeister darf einladen
- [x] Gildenmeister kann die Gilde nicht einfach verlassen
- [x] Einladungen werden nicht als dauerhaftes Mitgliedschaftsrecht gespeichert

## Gildenbank
- [x] eigene Gildenbank
- [x] Zugriff über Banker-NPC
- [x] gemeinsame 54-Slot-Gildenbank
- [x] persistente Speicherung
- [x] Zugriff nur für Gildenmitglieder
- [x] Gildenbank ist vom persönlichen Spieler-Wallet getrennt

## Gildenstadt / Basis
- [x] Gilde ist als Gemeinschaft mit eigener Stadt/Basis konzipiert
- [x] Stadt wird manuell erstellt
- [x] WorldEdit/WorldGuard übernehmen Bau und Schutz
- [x] PixelRPG baut kein eigenes Stadt-/Protection-/Regions-System
- [x] Bett bleibt persönlicher Spawnpunkt
- [x] Travel-NPC bleibt für Schnellreise zuständig

## Bewusst nicht umgesetzt
- [x] keine Gilden-XP
- [x] keine Gilden-Level
- [x] keine Gildenquests
- [x] keine Gebäude-Freischaltungen
- [x] kein Gildenhandel
- [x] kein eigenes Gilden-PvP-System
- [x] keine eigene Regions-/Protection-Engine

---

# 20 – Moderation / Admin — 🟢 98 % ABGESCHLOSSEN

## Zentrale Admin-Schnittstelle
- [x] zentraler `/rpgadmin`-Command
- [x] bestehende `/rpgadmin npc create`-Struktur als Grundlage beibehalten und erweitert
- [x] Admin-Funktionen als Unterbefehle organisiert
- [x] Admin-Berechtigung (`rpg.admin`) für die Schnittstelle

## NPC Management
- [x] NPC über `/rpgadmin npc` erstellen
- [x] bestehende NPC-Create-Mechanik weiterverwendet
- [x] NPC konfigurieren/bearbeiten
- [x] NPC entfernen/verwalten
- [x] keine manuelle Dateiänderung als Voraussetzung

## Item Management
- [x] Admin-Give-System
- [x] Item-Inspect-System
- [x] Items administrativ erstellen
- [x] RPG-Stats administrativ verändern
- [x] Validierung der Item-/Stat-Eingaben

## Quest Management
- [x] Quest geben
- [x] Quest entfernen/abbrechen
- [x] Quest zurücksetzen
- [x] Quest-Fortschritt setzen
- [x] Quest abschließen
- [x] Queststatus administrativ einsehen/verwalten

## Boss Management
- [x] bestehendes Boss Management
- [x] Worldboss-Event administrativ starten/spawnen

## Player Management
- [x] relevante PixelRPG-Spielerdaten einsehen
- [x] relevante Spielerdaten administrativ verändern
- [x] Spielerdaten zurücksetzen
- [x] Level/XP und weitere verwaltbare Progressionsdaten administrieren

## Debug Tools
- [x] `/rpgadmin debug` als separates Entwickler-/Testwerkzeug
- [x] Zugriff über Admin-Berechtigung
- [x] gezielte Diagnose/Testmöglichkeiten für PixelRPG-Systeme
- [x] Debug-Funktionen sind kein normales Gameplay-System

---

# 21 – Data / Persistence — 🟢 98 % ABGESCHLOSSEN
- [x] Player Persistence
- [x] Quest Persistence
- [x] Profession Persistence
- [x] Economy Persistence
- [x] Party Persistence
- [x] NPC Persistence
- [x] Equipment Persistence
- [x] Companion Persistence
- [x] MySQL
- [x] YAML Fallback / Backup
- [x] Economy / Handelsdepot Persistence
- [x] Scoreboard-Einstellung Persistence
- [x] Boss-/Event-relevante Persistenz
- [x] Gilden-Persistence
- [x] Gildenbank-Persistence

---

# 22 – Testing / QA — 🟡 IN ARBEIT
- [x] CI-Build grün für 17 – Bosses
- [x] CI-Build grün nach vollständiger Worldboss-Implementierung
- [x] CI-Build grün für 19 – Gilden
- [x] CI-Build grün für 20 – Moderation / Admin
- [ ] vollständige Runtime-Tests für Biom-Boss-Spawnchance
- [ ] vollständige Runtime-Tests für Worldboss-Events
- [ ] vollständige Runtime-Tests für Boss-Phasen / Adds / Loot
- [ ] vollständige Runtime-Tests für Worldboss-Umgebungsschutz
- [ ] vollständige Runtime-Tests für Gilden-Gründung / Einladungen / Gildenbank
- [ ] vollständige Runtime-Tests für Questdetail-Dialog / Quest-Abbruch
- [ ] vollständige Regressionstests aller bisherigen Systeme

---

# Statusübersicht

| Punkt | Status |
|---|---|
| 01 – Player | 🟢 98 % |
| 02 – Combat | 🟢 98 % |
| 03 – Progression / Stats | 🟢 98 % |
| 04 – Companions | 🟢 98 % Core |
| 05 – Quests | 🟡 90 % |
| 06 – NPC / Dialogue | 🟢 98 % |
| 07 – Items | 🟢 98 % |
| 08 – Equipment | 🟢 98 % |
| 09 – Crafting | 🟢 98 % |
| 10 – Professions | 🟢 98 % |
| 11 – Economy / Gold | 🟢 98 % |
| 12 – Party | 🟢 98 % |
| 13 – Travel / Waypoints | 🟢 98 % |
| 14 – Shops / Auction House | 🟢 98 % |
| 15 – UI / HUD | 🟢 98 % |
| 16 – Resource Pack / Custom Items | 🔵 Vorbereitet / Zukunft |
| 17 – Bosses | 🟢 98 % |
| 18 – Regions / Mob Scaling | ⚪ Verworfen |
| 19 – Gilden | 🟢 98 % |
| 20 – Moderation / Admin | 🟢 98 % |
| 21 – Data / Persistence | 🟢 98 % |
| 22 – Testing / QA | 🟡 In Arbeit |
