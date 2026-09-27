# PixelRPG – RP Phase 9 – Abschlussaudit

**Status:** technisch abgeschlossen  
**Branch:** `test`  
**Referenz:** `main` ausschließlich als Soll-/Vergleichszustand  
**Prüfstand:** 27.09.2026  
**Basis:** Java 25 + Paper 26.2 + Mojang-Mappings

## 1. Prüfziel

Phase 9 aus `RP.md` wurde als forensischer Abschlussaudit durchgeführt:

- XP-Exploits
- Crafting-Exploits
- Goldinflation / Economy-Flows
- Item-Ausreißer
- Quest-Ausreißer
- Mob-Ausreißer
- Boss-Ausreißer
- Meta-/Zwangspfade
- Legacy-API-Grenzen
- Paper-26.2-Kompatibilität
- Build und Verifikation

Es wurden nur repository-auswertbare Fakten verwendet. Reale XP/Stunde, Gold/Stunde, Killzeit, Spawnrate und Marktnachfrage sind ohne laufenden Serverbetrieb nicht seriös aus dem Repository ableitbar und wurden deshalb nicht erfunden.

## 2. Charakter-XP

Aktueller verbindlicher Stand:

- Charakterlevel: 1–60
- harte XP-Obergrenze: 3.379.400
- `PlayerProfile` begrenzt gespeicherte Charakter-XP auf die Level-60-Schwelle
- `Level.fromExperience()` begrenzt das berechnete Level auf 60
- Mob-XP verwendet die gespeicherte ursprüngliche Max-Health und kann dadurch nicht durch mehrfaches Re-Skalieren vervielfacht werden
- Boss-Entities werden aus der normalen Mob-XP-Pipeline ausgeschlossen
- Berufs-XP wird nicht als Charakter-XP verbucht

### Audit-Ergebnis

**KEIN statisch nachgewiesener Charakter-XP-Overflow.**

Die tatsächliche XP/Stunde bleibt ein Live-Messwert.

## 3. Quest-Audit

Geprüft:

- 149 eindeutige Questdefinitionen
- keine doppelten Quest-IDs
- keine fehlenden Prerequisite-/Follow-up-Referenzen
- Questlevel 1–60
- Storyanforderungen 1–60
- Quest-XP bleibt über die bestehende Reward-Pipeline angebunden
- Abschluss ist an aktiven Questfortschritt und Questgeber gebunden
- abgeschlossene Quests werden persistiert
- zeitbegrenzte Quests werden serverseitig abgebrochen
- Navigation wird beim Queststatuswechsel aktualisiert

### Audit-Ergebnis

**KEIN statischer Quest-Reward-Duplikationspfad gefunden.**

Die 149 Quests bleiben Contentbasis; ihre Laufzeitdauer und XP/Stunde benötigen Live-Telemetrie.

## 4. Crafting-Audit

329 Rezepte wurden erneut maschinell geprüft:

- 329 Definitionen
- 0 doppelte IDs
- 0 Rezepte ohne Kosten
- 0 Rezepte ohne positive Berufs-XP
- Berufslevel maximal 60
- explizite Berufs-XP: 20–410
- Unlockkosten bleiben validiert
- keine Admin-/Unique-Items als normale Rezeptkosten
- keine Admin-/Unique-Items als normale Rezeptausgabe
- keine zirkulären Custom-Item-Rezeptabhängigkeiten
- FISHERMAN und WOODCUTTER bleiben rezeptlos/passiv

### Audit-Ergebnis

**KEIN kostenloser Custom-Crafting-Zyklus statisch nachgewiesen.**

Wiederholbare Vanilla-Aktivitäten bleiben bewusst Bestandteil der Berufssysteme und sind kein automatischer Exploit; ihre Effizienz muss über Laufzeitmessung bewertet werden.

## 5. Berufs-XP-Audit

Der Masterplan definiert neun Berufe.

Der Code verwendet inzwischen ebenfalls neun reale Enum-Berufe:

1. BLACKSMITH
2. SCHOLAR
3. FARMER
4. COOK
5. TAILOR
6. ALCHEMIST
7. MASON
8. FISHERMAN
9. WOODCUTTER

Der historische `MOUNTAIN_MINER` bleibt ausschließlich als Daten-/Java-Kompatibilitätsalias erhalten.

### Forensischer Fix

Ein alter separater `PROFESSION_MOUNTAIN_MINER`-NPC wurde entfernt. Historische NPC-Daten mit diesem Typ werden beim Laden auf den BLACKSMITH-Trainer migriert.

Zusätzlich wurden wiederholbare Metallblock-Abbauquellen aus der Blacksmith-XP-Vergabe entfernt. Rohstoff-XP kommt für den Blacksmith-Pfad damit aus den vorgesehenen Erz-/Metallrohstoffblöcken, nicht aus frei platzierbaren Metallblöcken.

Historische unerreichbare Level-80/100-Zweige in den passiven Berufen wurden entfernt; die Laufzeitlogik entspricht jetzt vollständig dem Berufslevel-Cap 60.

## 6. Item-Audit

Geprüfter Content:

- 30 normale Itemdefinitionen
- 32 dedizierte Boss-Reward-Items
- 23 Food-Definitionen
- 6 Equipment-Sets

Normale Itemdefinitionen:

- Levelbereich 3–60
- keine doppelten IDs
- Rarity bleibt COMMON bis UNIQUE
- deterministische Stat-Erzeugung über den aktuellen `RPGItemBuilder`
- Iteminstanzen besitzen eindeutige Instance-IDs

Zwei normale Items besitzen bewusst ein niedrigeres Required-Level als ihr Item-Level. Das ist ein Contentwert und kein Overflow.

## 7. Boss-Audit

Normale Mobs, Biome-Bosse und World-Bosse bleiben getrennte Belohnungspfade.

Bereits geschlossen:

- keine normale Mob-XP zusätzlich zu Boss-XP
- keine normale Mob-Gildenwährung zusätzlich zur Bossbelohnung

### Neuer Phase-9-Fix

Die 32 vorhandenen Boss-Reward-Items waren zwar im zentralen Item-Katalog registriert, wurden aber im Boss-Loot-Pfad bisher als `pixelrpg:`-Loot herausgefiltert.

Das ist jetzt korrigiert:

- `BossLootConfig` besitzt ein dediziertes `customItemReward`
- `BossRepository` persistiert `loot.custom-item`
- bestehende Boss-Konfigurationen werden auf die vorgesehenen 32 Boss-Reward-Items migriert
- neue Default-Bossdefinitionen schreiben die Custom-Belohnung direkt
- `BossManager` gibt das dedizierte Boss-Item genau einmal pro berechtigtem Reward-Empfänger aus
- normale Vanilla-Lootlisten bleiben weiterhin von Custom-Items getrennt

Damit ist die vorhandene Boss-Reward-Datenbasis tatsächlich an die Boss-Belohnungspipeline angeschlossen.

## 8. Economy-Audit

Geprüfte Sicherheitsregeln:

- Geld wird intern in Minor Units gespeichert
- Eingaben werden auf endliche, positive Werte begrenzt
- Überläufe werden abgefangen
- Trade-Depot-Preise werden auf Cent-Auflösung normalisiert
- Verkaufsgebühr wird auf Minor Units berechnet
- Spielerhandel verwendet Escrow
- Käufer zahlt erst nach Inventarprüfung
- Abbruch/Timeout/Disconnect gibt Escrow zurück
- Gildenkasse verhindert negative Bestände
- Gildenauflösung mit nicht leerer Kasse wird blockiert
- NPC-Shop-Verkäufe zahlen nur den konfigurierten Sell-Preis
- Shop-Rückkauf ist nicht inflationsfördernd, da der Sell-Preis unter dem Buy-Preis liegt

### Audit-Ergebnis

**Kein statischer Gold-Overflow oder kostenloser Transferpfad gefunden.**

Die quantitative Inflationsrate kann nur mit realen Transaktionsdaten bewertet werden.

## 9. Spielerhandel / Trade Depot

Direkter Spielerhandel:

- Item wird vor dem Angebot aus dem Inventar entfernt
- Escrow verhindert parallele Nutzung
- Preis ist Minor-Unit-basiert
- Inventar wird vor Zahlung geprüft
- Zahlung und Itemtransfer laufen synchron
- Timeout/Disconnect/Shutdown gibt das Item zurück

Trade Depot:

- 7 Tage Laufzeit
- 5 % Verkaufsgebühr
- eigener Kauf verboten
- Ware wird beim Einstellen entfernt
- Ablauf führt zur Rückgabe
- ausstehende Verkäuferauszahlungen werden persistiert

## 10. Meta-Pfad-Audit

Der Code erzwingt keinen einzigen Charakterpfad.

Parallel existieren:

- Quests
- normale Mobs
- Bosse
- Berufe
- Crafting
- Spielerhandel
- Gilden
- Exploration/Reisen

Keine neue Phase-9-Mechanik koppelt RP-Handlungen pauschal an Charakter-XP.

Eine mathematische Aussage darüber, welcher Pfad live am effizientesten ist, wäre ohne reale Servermessung unbelegt und wurde deshalb nicht getroffen.

## 11. Legacy-/API-Audit

Build-Konfiguration geprüft:

- Java 25 Toolchain
- Paper Dev Bundle 26.2.build.121-stable
- paperweight 2.0.0-beta.21
- Shadow 9.6.1
- Gson 2.13.1
- HikariCP 7.0.2
- MySQL Connector/J 9.7.0
- Mojang-Mappings
- paper-plugin.yml
- keine CraftBukkit-/Legacy-NMS-Abhängigkeit
- keine ChatColor-Nutzung
- bestehende Source-/Shadow-Verifikation bleibt aktiv

Die native Dialogarchitektur bleibt auf der aktuellen Paper-API.

## 12. Verifikation

Die bestehende Buildprüfung bleibt unverändert aktiv:

- Source API boundaries
- Legacy-Bukkit/NMS-Grenzen
- statische Live-Server-Referenzen
- Third-Party-Relocations
- JDBC-Service-Relocation
- ShadowJar
- Plugin-Artefakt

`check` behält seine bisherigen Verifikationsabhängigkeiten.

## 13. Phase-9-Definition-of-Done

| Punkt | Status |
|---|---|
| XP-Exploits geprüft | ERFÜLLT |
| Crafting-Exploits geprüft | ERFÜLLT |
| Goldinflation statisch geprüft | ERFÜLLT |
| Item-Ausreißer geprüft | ERFÜLLT |
| Quest-Ausreißer geprüft | ERFÜLLT |
| Mob-Ausreißer geprüft | ERFÜLLT |
| Boss-Ausreißer geprüft | ERFÜLLT |
| Meta-/Zwangspfade geprüft | ERFÜLLT |
| Legacy-API-Grenzen geprüft | ERFÜLLT |
| Paper-26.2-Basis geprüft | ERFÜLLT |
| Build-Verifikation erhalten | ERFÜLLT |
| Boss-Reward-Pipeline geschlossen | ERFÜLLT |
| alter Mountain-Miner-NPC bereinigt | ERFÜLLT |
| Metallblock-XP-Exploit geschlossen | ERFÜLLT |
| main verändert | NEIN |
| test verändert | JA |

## 14. Grenzen der forensischen Aussage

Nicht aus Repositorydaten ableitbar:

- reale XP/Stunde
- reale Gold/Stunde
- reale Killzeit
- reale Spawnrate
- reale Produktionszeit
- reale Spieler-Nachfrage
- tatsächliche Live-Meta

Dafür existiert die Charakter-Level-Telemetrie bereits als technische Messbasis. Es wurden keine erfundenen Livewerte als Auditfakten verwendet.

## Ergebnis

**Phase 9 ist technisch abgeschlossen.**

Der aktuelle `test`-Stand ist damit der geprüfte Abschlussstand des RP-Reworks. `main` bleibt unverändert.
