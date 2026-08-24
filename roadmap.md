# PixelRPG – Feature Roadmap

Stand: 2026-08-25 – abgeglichen mit dem aktuellen `main`-Stand

> Diese Datei ist die verbindliche Arbeits-Roadmap. Die Reihenfolge ist von oben nach unten.
> Prozentwerte beschreiben funktionale Reife des aktuell vorhandenen Codes, nicht die Menge an Code.
> 🟢 95–98 % = abgeschlossen, 🟡 50–94 % = in Arbeit, 🔴 0–49 % = offen, 🔵 = vorbereitet / Zukunft.

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
- Gilden werden nicht als vollständiges Gameplay-System umgesetzt.
- Partys unterstützen gemeinsame XP, Loot-Verteilung, Party-Buffs und gemeinsamen Questfortschritt.
- Alte Default-Bosse Forest Tyrant, Frost Sovereign und Void Reaper werden nicht als finales Bosskonzept fortgeführt.
- Biom-Bosse sind regionale Spezialmonster und keine Worldbosse. Sie sind an ein bestimmtes Minecraft-Biom gebunden, können dort über die interne 10-%-Spawnprüfung erscheinen und verbrennen nicht durch Tageslicht.
- Worldbosse sind ausschließlich Admin-gestartete Events. Sie haben feste Level, individuelle Belohnungen für aktive Teilnehmer, mehrere Phasen und dürfen Adds beschwören. Tageslicht verbrennt sie nicht. Es gibt keinen automatischen Worldboss-Respawn.
- Aktuell existiert keine verbindliche Lore.
- Spieler benötigen keine Gameplay-Commands; Interaktion erfolgt über NPCs/Dialoge. Commands bleiben primär Admin-Funktionen.
- Sprachen: Deutsch, Englisch, Spanisch, Französisch; nur dort einsetzen, wo Mehrsprachigkeit sinnvoll ist.
- Travel bleibt beim bestehenden NPC-/Waypoint-Prinzip: NPC schaltet Ziel frei, erneutes Ansprechen zeigt verfügbare Reiseziele.

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

## Worldbosse
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

## Loot / Teilnahme
- [x] datengetriebene Loot-Tables
- [x] garantierte Drops
- [x] Chance-Drops
- [x] Gold / XP Rewards
- [x] normale Bosse verwenden Party-System für Loot
- [x] Worldbosse belohnen aktive Teilnehmer individuell
- [x] Party bleibt beim Worldboss unabhängig vom individuellen Loot

## Altbestand / Content
- [x] Forest Tyrant entfernt
- [x] Frost Sovereign entfernt
- [x] Void Reaper entfernt
- [x] keine alten Testbosse als finales Bosskonzept weitergeführt
- [x] bestehende Boss-Engine auf Biom-Bosse und Worldboss-Events ausgerichtet

---

# 18 – Regions / Mob Scaling — 🔴 OFFEN
- [ ] Region System
- [ ] Region Definitions
- [ ] Mob Level Scaling
- [ ] Danger Zones
- [ ] Region Rewards

---

# 19 – Gilden — 🔴 OFFEN
- [ ] Gilden Core
- [ ] Gilden erstellen
- [ ] Mitglieder
- [ ] Gilden-Ränge
- [ ] Gildenbank
- [ ] Gilden-Quests
- [ ] Gilden-Level

---

# 20 – Moderation / Admin — 🟡 IN ARBEIT
- [ ] Admin Command System
- [ ] NPC Management
- [ ] Item Management
- [ ] Quest Management
- [x] Boss Management / Worldboss-Event-Start
- [ ] Player Management
- [ ] Debug Tools

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

---

# 22 – Testing / QA — 🟡 IN ARBEIT
- [x] CI-Build grün für 17 – Bosses
- [ ] vollständige Runtime-Tests für Biom-Boss-Spawnchance
- [ ] vollständige Runtime-Tests für Worldboss-Events
- [ ] vollständige Runtime-Tests für Boss-Phasen / Adds / Loot
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
| 18 – Regions / Mob Scaling | 🔴 Offen |
| 19 – Gilden | 🔴 Offen |
| 20 – Moderation / Admin | 🟡 In Arbeit |
| 21 – Data / Persistence | 🟢 98 % |
| 22 – Testing / QA | 🟡 In Arbeit |
