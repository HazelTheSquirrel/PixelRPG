# PixelRPG – Feature Roadmap

Stand: 2026-08-24 – abgeglichen mit dem aktuellen `main`-Stand

> Diese Datei ist die verbindliche Arbeits-Roadmap. Die Reihenfolge ist von oben nach unten.
> Prozentwerte beschreiben funktionale Reife des aktuell vorhandenen Codes, nicht die Menge an Code.
> 🟢 95–98 % = abgeschlossen, 🟡 50–94 % = in Arbeit, 🔴 0–49 % = offen.
> Ein grüner Status bedeutet nicht automatisch, dass der aktuelle CI-Build oder ein Runtime-Test erfolgreich bestätigt wurde.

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
- Aktuell existiert keine verbindliche Lore.
- Spieler benötigen keine Gameplay-Commands; Interaktion erfolgt über NPCs/Dialoge. Commands bleiben primär Admin-Funktionen.
- Sprachen: Deutsch, Englisch, Spanisch, Französisch; nur dort einsetzen, wo Mehrsprachigkeit sinnvoll ist.
- Travel bleibt beim bestehenden NPC-/Waypoint-Prinzip: NPC schaltet Ziel frei, erneutes Ansprechen zeigt verfügbare Reiseziele.

---

# 01 – Player — 🟢 98 % ABGESCHLOSSEN
- [x] PlayerProfile / Grunddaten
- [x] PlayerProfileManager
- [x] PlayerProfileRepository
- [x] YAML Player Repository
- [x] MySQL Player Repository
- [x] Async Pre-Login Load
- [x] Join Activation
- [x] Quit Deactivation + Save
- [x] Registrierungsstatus
- [x] Vanilla-/PixelRPG-Spieler-Isolation
- [x] Kein Startbonus / kein Startbonus-State
- [x] Dirty-State / Save Queue
- [x] Emergency YAML Backup bei MySQL-Fehler
- [x] Level 1–99
- [x] Level-100-Grenze / reservierter Zustand
- [x] XP-Tabelle / Progressionskurve
- [x] Level-99-Transzendenz-Grind
- [x] XP Clamp / Overflow-Sicherheit
- [x] Level-Up Event
- [x] Player-Level API
- [x] Level → Balancing-Grundlage für Gear/Companions
- [x] Klassen vollständig entfernt
- [x] alte Player-Attribute vollständig entfernt
- [ ] veraltete `isRegisteredInGuild()`-Kompatibilitätsalias vollständig aus allen Consumer-Klassen entfernen

---

# 02 – Combat — 🟢 98 % ABGESCHLOSSEN
- [x] zentrale Damage-Logik
- [x] Player Combat
- [x] Mob Combat
- [x] MMORPG-nahes Damage Scaling
- [x] Vanilla-Schaden bleibt Basisbestandteil
- [x] Vanilla-Attack-Intervalle unverändert
- [x] Stat Scaling
- [x] Crit Chance mit 0 % Basis
- [x] Crit Damage mit ×2 Basis
- [x] Lifesteal
- [x] eigene Armor-Mitigation
- [x] Combat State / Target State
- [x] Combat Cleanup
- [x] Combat Events
- [x] Vanilla-/PixelRPG-Mischbetrieb sauber abgegrenzt
- [x] PvP nutzt bei tatsächlicher Aktivierung normales PixelRPG-Combat-Verhalten
- [x] Weapon Skills / Cooldowns / Level Requirements
- [x] materialbasierte Weapon Skills
- [x] Bogen-/Armbrust-Skillpfad mit Shift+Rechtsklick
- [x] Mob-Level = aktives Spielerlevel
- [x] HP/Damage Scaling und Spieler-Parity
- [x] Original-Mob-Attribute Tracking und Wiederherstellung
- [x] Region/Danger Scaling entfernt
- [x] Mob Experience / Loot / Gold / Death Integration

---

# 03 – Progression / Stats — 🟢 98 % ABGESCHLOSSEN
- [x] Level 1–99 normale Progression
- [x] Level 99 → 100 astronomischer Transzendenz-Grind
- [x] feste Mob-XP unabhängig von Spieler-/Gear-Scaling
- [x] Quest-XP definition-driven
- [x] XP Clamp / Overflow-Sicherheit
- [x] deterministische Progression
- [x] Player-Level API / Level-Up Events
- [x] deterministische Stat-Berechnung / Cached Player Stats
- [x] HP / Armor / Movement Speed / Reach / Damage
- [x] Crit / Crit Damage / Lifesteal / Attack Power
- [x] Equipment → Stats → Combat Pipeline
- [x] aktive Companion-Passivstats in Character-Stat-Pfad
- [x] Companion-Boni nur solange aktiv/gespawnt
- [x] Despawn entfernt Companion-Boni automatisch
- [x] alte Player-Attribute- und alte Stat-Pfade entfernt
- [x] berechnete Stats werden nicht persistiert

## Finaler Character-Stat-Satz
```text
HP
Armor
Movement Speed
Reach
Damage
Crit
Crit-Schaden
Lifesteal
Attack Power
```

---

# 04 – Companions — 🟢 98 % CORE ABGESCHLOSSEN
- [x] Registry / Definitions / Ownership / Unlocks
- [x] Unique Unlocks
- [x] Aktivieren / Deaktivieren
- [x] Spawn / Despawn
- [x] Logout = Despawn
- [x] Login = kein automatisches Wiederbeschwören
- [x] Follow / Movement
- [x] Passive Stats nur bei aktivem Companion
- [x] Leveling / XP
- [x] normale Companion-Level an Spielerlevel gekoppelt
- [x] Unique Companion eigenes Level
- [x] Combat-Regeln
- [x] Companion Equipment / Persistence
- [x] Runtime Registry / Cleanup
- [x] Boss → Companion Unlock
- [x] passive Boni für alle Character Stats
- [x] Bonus wird bei Despawn/Logout entfernt
- [x] CompanionAbilityEngine-Grundstruktur / Cooldowns / Definitions
- [ ] aktive Companion-Abilities mit tatsächlichen Gameplay-Effekten
- [ ] passive Ability Definitions mit tatsächlichen Gameplay-Effekten
- [ ] vollständige Runtime-Integration der Abilities

---

# 05 – Quests — 🟡 90 % IN ARBEIT
- [x] HUNT
- [x] COLLECT
- [x] TALK_TO_NPC
- [x] REACH_LOCATION
- [x] GLOBAL_EVENT
- [x] Quest Definition / JSON Repository / Validierung
- [x] Quest-ID / Titel / Beschreibung / Ziele / Rewards
- [x] Navigation / Prerequisites / Follow-Up IDs
- [x] Quest Manager / Progress / Completion / Rewards / XP
- [x] maximal 5 aktive Quests
- [x] Quest-Annahme / Abbruch / Expiry
- [x] QuestCompletedEvent
- [x] Abschluss nur bei erfülltem Ziel und Rückkehr zum Quest-Giver
- [x] Questziel und Fortschritt sichtbar
- [x] Locator-Bar / Quest-Marker
- [x] Tracking aller fünf Questtypen
- [x] Party-Propagation für HUNT
- [x] Quest Persistence YAML/MySQL
- [x] Questketten-Datenmodell
- [x] Party Quest Share Grundintegration
- [ ] produktive `quests_v2.json`-Definitionen vollständig befüllen
- [ ] vollständige Runtime-Validierung
- [ ] Quest-XP-Scaling final definieren
- [ ] vollständige Questketten-/Follow-Up-Logik
- [ ] Party-Verhalten für alle Questtypen vollständig vereinheitlichen und testen

---

# 06 – NPC / Dialogue — 🟢 98 % ABGESCHLOSSEN
- [x] bestehendes NPC-Erstellungs-/Spawn-System
- [x] NPCs manuell durch Admins gespawnt
- [x] bestehende NPC-Namen / Titel / Rollen / Skins
- [x] NPCs dauerhaft sichtbar
- [x] normale Minecraft-NPC-Interaktion als einziger Interaktionsweg
- [x] G-Taste bleibt ausschließlich Spieler-Interaktion
- [x] definierte NPC-Typen/Funktionen
- [x] mehrere passende Funktionen pro NPC
- [x] native Minecraft/Paper-26.2 Dialogsystem als Basis
- [x] Vanilla-nahe Dialogdarstellung
- [x] verzweigte Dialoge / Antwortmöglichkeiten
- [x] Folge-Dialoge / direkte Gameplay-Aktionen
- [x] Quest / Profession / Shop / Bank / Travel / Story / Companion / Reception / Quick Actions
- [x] Dialog Conditions & Progression
- [x] gesehene und abgeschlossene Dialogschritte persistent
- [x] einmalige Dialoge/Dialogschritte
- [x] Mehrsprachigkeit Deutsch / Englisch / Spanisch / Französisch
- [x] CI-Build grün

---

# 07 – Items — 🟢 98 % ABGESCHLOSSEN
- [x] stabile Item IDs / Item Definitions
- [x] datengetriebene Item-Definitionen
- [x] Vanilla-Material als Standardbasis
- [x] PixelRPG-ID / Vanilla-Material / Resourcepack-Identifier getrennt
- [x] finale Item-/Gear-Kategorien
- [x] Common / Uncommon / Rare / Epic / Legendary / Unique
- [x] Item Level 1–99
- [x] Required Level unabhängig vom Item Level
- [x] Unique serverweit exakt ein konkretes Exemplar
- [x] Unique nur über Admin-Vergabe
- [x] Item Stats
- [x] Gearscore
- [x] Soulbound
- [x] Weapon Skill Mapping
- [x] bestehendes Admin-Shop-NPC-System
- [x] Resourcepack-Trennung
- [x] ItemAPI / ItemService / Registry / RPGItemBuilder
- [x] CI-Build grün

---

# 08 – Equipment — 🟢 98 % ABGESCHLOSSEN
- [x] Helm / Brust / Hose / Schuhe / Waffe / Nebenhand
- [x] finaler Slot-Pool
- [x] Vanilla-Inventar bleibt erhalten
- [x] ausgerüstete Item-Stats aggregiert
- [x] Equipment → Player Stats → Combat Pipeline
- [x] Required Level
- [x] Rarity vom konkreten Item
- [x] PlayerProfile-Persistence
- [x] Join / Quit / Registrierung berücksichtigt
- [x] Soulbound Death Protection
- [x] nicht-Soulbound-Equipment folgt Vanilla-Todesregeln
- [x] Equipment-Sets
- [x] Set-Boni in Character-Stats-Pipeline
- [x] Armor-Trim-System für Setteile
- [x] datengetriebene Trim-Zuordnung
- [x] CI-Build grün

---

# 09 – Crafting — 🟢 98 % ABGESCHLOSSEN
- [x] Recipe Registry
- [x] eindeutige Recipe IDs
- [x] Vanilla-Rezepte werden übernommen statt unnötig dupliziert
- [x] PixelRPG-eigene Rezepte für fehlende Herstellungsfälle
- [x] Crafting Definitions
- [x] Zutaten-/Mengenprüfung
- [x] frei definierbares Ergebnis-Item
- [x] Crafting Service / Crafted Item Factory
- [x] Zutaten erst bei erfolgreichem Crafting abgezogen
- [x] Ergebnis wird nach erfolgreicher Prüfung vergeben
- [x] Berufsanforderungen
- [x] Rezeptfreischaltungen über Quest und/oder Gold
- [x] Dialogue-Crafting
- [x] Crafting über NPC und Quick Actions (`G`)
- [x] bestehendes Crafting-GUI-System beibehalten und erweitert
- [x] nach „Herstellen“ bleibt die GUI geöffnet
- [x] direktes Weiter-Crafting ohne erneutes Öffnen
- [x] GUI-Zustand wird nach jedem Crafting aktualisiert
- [x] Blacksmith GUI im bestehenden System
- [x] Vanilla Crafting / Smelting / Blasting / Smoking / Campfire / Stonecutting
- [x] sinnvolle Verteilung auf die vier Professionen
- [x] CI-Build grün

---

# 10 – Professions — 🟢 98 % ABGESCHLOSSEN
- [x] BLACKSMITH
- [x] PROVISIONER
- [x] ALCHEMIST
- [x] SCHOLAR
- [x] exakt vier Berufe, keine weiteren Professionen
- [x] keine Spezialisierungen
- [x] Profession Level 1–100
- [x] getrennte Profession-XP unabhängig vom Player-Level
- [x] alle vier Professionen parallel erlernbar und levelbar
- [x] Profession Recipes / Crafting-Anbindung
- [x] Profession Trainer
- [x] YAML-/MySQL-Persistenz
- [x] Profession API / Service-Anbindung
- [x] XP durch Crafting / Gathering / Erz / Ressourcen / Landwirtschaft / Fischerei
- [x] XP durch Reparatur / Anvil / Verzaubern / Alchemie
- [x] XP durch Quests / NPC-Aufträge
- [x] XP-Balancing nach Aktivität / Schwierigkeit
- [x] Quest- oder Gold-Rezeptfreischaltungen
- [x] Quest-exklusive Rezepte möglich
- [x] bestehendes GUI-System beibehalten
- [x] keine zentrale Profession-Übersicht
- [x] keine Spieler-Commands
- [x] CI-Build grün

---

# 11 – Economy / Gold — 🟢 98 % ABGESCHLOSSEN

> Das Economy-/Gold-System ist im aktuellen `main` entsprechend der verbindlich festgelegten Regeln umgesetzt. Wallet-Gold bleibt virtuell; die speziell markierte Sonnenblume dient als physischer Goldtaler. Das Handelsdepot ersetzt das klassische Auktionshaus-Konzept durch Festpreis-Angebote. Der CI-Build ist grün.

## Gold / Wallet
- [x] Goldtaler-Kontostand
- [x] virtuelles Wallet-Guthaben
- [x] Wallet API / Economy API
- [x] Economy Persistence
- [x] Gold kann von anderen Systemen unabhängig vergeben und abgezogen werden
- [x] keine zusätzliche Währung
- [x] Wallet-Gold bleibt beim Tod erhalten

## Physischer Goldtaler
- [x] speziell markierte PixelRPG-Sonnenblume als physischer Goldtaler
- [x] normale Vanilla-Sonnenblumen sind keine Goldtaler
- [x] Goldtaler können ins Wallet eingezahlt werden
- [x] Wallet-Gold kann als physischer Goldtaler ausgezahlt werden
- [x] physisches Gold folgt den normalen Item-/Inventarregeln
- [x] physisches Gold droppt beim Tod wie ein normales Item
- [x] bestehende Goldtaler-Drops aus Mob-/Boss-Loot angebunden

## Einnahmequellen
- [x] Quests können Gold vergeben
- [x] NPC-Aufträge können Gold vergeben
- [x] Mobs können Gold vergeben
- [x] Bosse können Gold vergeben
- [x] Shops können Gold als Gegenleistung vergeben
- [x] Economy-API bleibt unabhängig von der konkreten Einnahmequelle

## Adminshop
- [x] Adminshop als zentrale NPC-Shop-Grundlage
- [x] Kaufen
- [x] Verkaufen
- [x] Preise werden durch den Adminshop vorgegeben
- [x] keine automatischen Item-Buy-/Sell-Preisfelder als allgemeine Item-System-Regel
- [x] bestehendes Shop-/Dialog-System weiterverwendet

## Handelsdepot
- [x] klassisches Auktionshaus-Konzept durch Handelsdepot ersetzt
- [x] ausschließlich PixelRPG-Items handelbar
- [x] Festpreis-Angebote statt Bietauktionen
- [x] Verkäufer legt den gewünschten Verkaufspreis selbst fest
- [x] Angebot kann vom Verkäufer zurückgenommen werden
- [x] Laufzeit eines Angebots: 7 Tage
- [x] erfolgreiche Verkäufe zahlen 95 % an den Verkäufer aus
- [x] 5 % Verkaufsgebühr werden direkt von den Einnahmen abgezogen
- [x] nicht verkaufte Items laufen nach 7 Tagen automatisch ab
- [x] abgelaufene Items werden nicht gelöscht
- [x] abgelaufene Items landen automatisch im Bankfach `Handelsware`
- [x] Handelsware ist über das bestehende Bank-/Dialog-System abholbar
- [x] persistente offene Angebote und ausstehende Verkaufserlöse
- [x] offline Verkäufer verlieren weder Item noch Verkaufserlös

## Abschlusskriterien für 11
- [x] Goldtaler-Kontostand
- [x] Sonnenblume als physischer Goldtaler
- [x] Goldtaler Pickup / physische Goldintegration
- [x] Wallet API
- [x] Economy Persistence
- [x] Adminshop
- [x] Shop Kaufen / Verkaufen
- [x] Handelsdepot
- [x] PixelRPG-Items im Handelsdepot
- [x] Festpreis-Angebote
- [x] 5 % Verkaufsgebühr
- [x] 7-Tage-Expiration
- [x] Rückgabe abgelaufener Items über Bankfach `Handelsware`
- [x] CI-Build grün

---

# 12 – Party — 🟢 98 % ABGESCHLOSSEN
- [x] Party Core
- [x] maximale Partygröße: 5 Spieler
- [x] Party Invite
- [x] Einladungen laufen nach 60 Sekunden ab
- [x] Party Leave / Kick
- [x] Leader kann Leadership übertragen
- [x] Leader verlässt Party → nächstes Mitglied wird Leader
- [x] kein Kick-Cooldown
- [x] `/rpgparty` bleibt erhalten
- [x] Party separat in der Reception erreichbar
- [x] gemeinsame XP
- [x] Party-XP-Bonus
- [x] XP-Reichweite: 50 Blöcke
- [x] Loot-Verteilung: Free-for-all
- [x] Loot wird nur innerhalb der gültigen Party-Reichweite berücksichtigt
- [x] Party-Buffs: passive kleine XP-/Regenerations-/Utility-Boni
- [x] gemeinsamer Questfortschritt innerhalb von 50 Blöcken
- [x] Quests bleiben individuell angenommen
- [x] Questbelohnungen bleiben individuell
- [x] kein Party-Chat
- [x] Offline-Mitglieder bleiben in der Party
- [x] Party Persistence
- [x] Cleanup 30 Minuten nach dem letzten aktiven Mitglied
- [x] Disconnect-/Cleanup-Verhalten
- [x] Party API / Service-Anbindung
- [x] CI-Build grün

---

# 13 – Travel / Waypoints — 🟡 IN ARBEIT
- [ ] Travel NPC
- [ ] Waypoint Registry
- [ ] Waypoint Unlock
- [ ] Reiseziele über NPC-Dialog
- [ ] erneutem NPC-Ansprechen zeigt freigeschaltete Ziele
- [ ] Travel Kosten
- [ ] Travel Cooldown

---

# 14 – Shops / Auction House — 🟡 IN ARBEIT
- [ ] Shop Core
- [ ] Shop Definitions
- [ ] Buy / Sell
- [ ] NPC Shop Dialogue
- [ ] Auktionshaus Core
- [ ] Listings
- [ ] Kaufen / Verkaufen
- [ ] Gebühren
- [ ] Expiration

---

# 15 – UI / HUD — 🟡 IN ARBEIT
- [ ] Vanilla HUD-Kompatibilität
- [ ] HP Anzeige
- [ ] Armor Anzeige
- [ ] Mana-Konzept
- [ ] Level / XP Anzeige
- [ ] Character Stats Anzeige
- [ ] Companion Anzeige
- [ ] Quest Anzeige
- [ ] Combat Feedback

---

# 16 – Resource Pack / Custom Items — 🟡 IN ARBEIT
- [ ] Resourcepack-Struktur
- [ ] Custom Item Mapping
- [ ] Item Models
- [ ] Custom Textures
- [ ] Custom Armor
- [ ] Custom Weapon Models
- [ ] Resourcepack Versionierung
- [ ] Plugin ↔ Resourcepack Item IDs

---

# 17 – Persistence / Database — 🟡 IN ARBEIT
- [x] Player Persistence Grundsystem
- [x] YAML Fallback
- [x] MySQL Repository
- [ ] Companion Persistence vollständig validieren
- [ ] Quest Persistence vollständig validieren
- [ ] Profession Persistence
- [ ] Economy Persistence
- [x] Equipment Persistence
- [ ] Schema Versioning
- [ ] Migration System

---

# 18 – Admin / Debug — 🟡 IN ARBEIT
- [ ] Admin Command Framework
- [ ] Player Debug
- [ ] Stats Debug
- [ ] Combat Debug
- [ ] Companion Debug
- [ ] Quest Debug
- [ ] Economy Debug
- [ ] Item Debug
- [ ] Reload / Registry Diagnostics

---

# 19 – Content / World — 🔴 OFFEN
- [ ] World-Struktur
- [ ] Gebiete
- [ ] Dungeons
- [ ] Bosse
- [ ] Gegner-Varianten
- [ ] NPC Content
- [ ] Quest Content
- [ ] Item Content
- [ ] Companion Content
- [ ] Profession Content
- [ ] Loot Tables
- [ ] verbindliche Lore

> Content wird erst auf Basis der technisch stabilen Systeme ausgearbeitet.

---

# 20 – Final Validation — 🔴 OFFEN
- [ ] kompletter Clean Build
- [ ] Runtime Smoke Test
- [ ] Player Lifecycle Test
- [ ] Progression Test
- [ ] Combat Test
- [ ] Companion Test
- [ ] Quest Test
- [ ] NPC / Dialogue Test
- [ ] Item / Equipment Test
- [ ] Economy Test
- [ ] Party Test
- [ ] Travel Test
- [ ] Resourcepack Test
- [ ] Persistence / Migration Test
- [ ] Performance Test
- [ ] Fehler-/Log-Review
- [ ] Release Build
