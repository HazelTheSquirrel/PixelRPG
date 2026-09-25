# PixelRPG – RP Phase 4 – Economy

**Status:** Phase 4 abgeschlossen  
**Branch:** `test`  
**Referenz:** `main`  
**Prüfstand:** 25.09.2026

## Phase-Ziel

RP.md definiert Phase 4 als:

- Goldquellen
- Goldsenken
- NPC-Shops
- Trade Depot
- Spielerhandel
- Guild Economy

Die vorhandene Economy wurde forensisch geprüft und nur dort erweitert, wo ein konkreter Integritäts-/Funktionsfehler oder eine fehlende Kernfunktion vorlag.

## Goldquellen

### Quests

Quest-Rewards können direkt Gold vergeben. Die vorhandene Queststruktur bleibt erhalten.

Die Economy unterscheidet damit weiterhin:

- Charakter-XP
- Gold
- Item-Rewards

Gold wird nicht künstlich an jede RP-Aktion gekoppelt.

### Bosse

Boss-Rewards verwenden die vorhandene `EconomyAPI` und schreiben Gold auf das Spielerprofil.

### Normale Hostile Mobs

Die vorhandene Economy-Drop-Mechanik erzeugt physische Goldtaler.

Forensischer Befund:
Die Drop-Quelle existierte bereits, der Pickup-Listener wurde jedoch im Plugin-Lifecycle nicht registriert.

Das wurde behoben.

Damit ist der Kreislauf jetzt:

`Mob-Kill → Goldtaler → Pickup → Wallet`

und nicht mehr:

`Mob-Kill → physisches Item ohne Economy-Einlösung`.

## Goldsenken

Aktiv vorhanden:

- NPC-/Vanilla-Shop-Käufe
- Gildengründung: 2.500 Gold
- Trade-Depot-Verkaufsgebühr: 5 %
- zukünftige datengetriebene Shoppreise

Shop-Verkäufe erzeugen dagegen bewusst Gold aus NPC-Nachfrage und bleiben deshalb eine kontrollierte Goldquelle.

## Geldrepräsentation

Die Wallet-Persistenz verwendet Minor Units.

- 1 Gold = 100 Minor Units
- Speicherung als `long`
- externe API bleibt kompatibel als `double`
- Konvertierung erfolgt über `Money`
- nicht-finite Werte werden abgewiesen
- negative Walletwerte sind nicht zulässig

Damit werden typische Floating-Point-Rundungsfehler an der Persistenzgrenze vermieden.

## NPC-Shops

Der bestehende ShopManager bleibt datengetrieben über `shops.yml`.

Unterstützt werden:

- Buy Price
- Sell Price
- Legacy-Preis-Migration
- serialisierte Itemdaten
- Editor
- NPC-spezifische Shoplisten

Die Default-Building-Block-Waren bleiben als Basisversorgung erhalten.

Spieler-Spezialwaren werden nicht automatisch mit NPC-Waren überschrieben.

Das bewahrt die im RP.md geforderte Trennung:

**NPC = Basisversorgung**

**Spieler = Spezialwaren / individuelle Dienstleistungen**

## Shop-Transaktionen

Käufe prüfen:

1. registriertes Profil
2. Inventarkapazität
3. ausreichendes Guthaben
4. anschließend Abbuchung und Itemausgabe

Verkäufe prüfen zuerst das tatsächlich vorhandene Item und zahlen anschließend den exakten Minor-Unit-Betrag aus.

## Trade Depot

Das Trade Depot bleibt der asynchrone Spielerhandel.

Aktuelle Regeln:

- 7 Tage Laufzeit
- 5 % Verkaufsgebühr
- eigener Verkauf darf nicht gekauft werden
- Ware wird beim Einstellen aus dem Inventar entfernt
- Kauf legt Ware ins Handelsfach des Käufers
- abgelaufene Ware geht zurück ins Handelsfach des Verkäufers
- nicht zustellbare Ware bleibt als Listing bestehen
- ausstehende Verkäuferauszahlungen werden persistiert

### Forensische Korrektur

Trade-Depot-Preise werden beim Einstellen und Laden auf die exakte Economy-Auflösung von 0,01 Gold normalisiert.

Die Verkaufsgebühr wird jetzt ebenfalls auf Minor Units berechnet.

Damit ist beispielsweise ein Preis von 10,01 Gold nicht mehr einer abweichenden Double-Berechnung zwischen Käufer und Verkäufer ausgesetzt.

## Guild Economy

Die bisherige Gildeninfrastruktur besaß:

- Mitgliedschaft
- Gildenbank für Items
- Gildenwährung
- Gründungskosten

Es fehlte jedoch eine echte persistierte Gildenkasse.

Diese wurde ergänzt.

### Gildenkasse

Jede Gilde besitzt jetzt:

- persistentes Treasury
- Einzahlung durch jedes Mitglied
- Auszahlung durch den Gildenmeister
- Minor-Unit-basierte Speicherung
- Anzeige im nativen Gildendialog

Geldfluss:

`Spieler-Wallet → Gildenkasse`

oder

`Gildenkasse → Gildenmeister-Wallet`

### Sicherheitsregeln

- nur Gildenmitglieder dürfen einzahlen
- nur der Gildenmeister darf auszahlen
- negative/ungültige Beträge werden abgewiesen
- Auszahlung oberhalb des Treasury-Bestands wird abgewiesen
- Gildenauflösung mit nicht leerer Gildenkasse wird verhindert

Damit kann kein Treasury-Gold beim Disband versehentlich verschwinden.

## Spielerhandel

Direkter Spielerhandel bleibt bewusst von NPC-Shops getrennt.

Die vorhandene Infrastruktur:

- Trade Depot
- persönliches Handelsfach
- Spielerpreise
- individuelle Angebote

wird weiterverwendet.

Es wurde kein NPC-Auktionshaus als Ersatz für Spielerhandel eingeführt.

## Economy-Kreislauf

Die aktuelle Zielstruktur ist:

`Quest → Gold → Shop / Gilde / Spielerhandel`

`Mob → Goldtaler → Wallet`

`Boss → Gold → Wallet`

`Crafting → Produkt → Spielerhandel`

`Spielerhandel → Goldtransfer zwischen Spielern`

`Gildenkasse → gemeinsame Finanzierung`

Damit existieren mehrere unabhängige Goldquellen und mehrere Goldsenken.

## Bewusst nicht geändert

Keine pauschale Inflation-/Deflation-Anpassung.

Keine erfundenen NPC-Shops für nicht vorhandene NPC-IDs.

Keine willkürliche Änderung von 329 Crafting-Rezepten.

Keine künstlichen Reparaturkosten.

Keine XP-Kopplung an soziale Goldtransaktionen.

Keine automatische Steuer auf jeden Spielerhandel.

Keine Entfernung des Trade Depots.

Keine Abschaffung der bestehenden Shop-Editor-Struktur.

## Definition of Done

| Punkt | Status |
|---|---|
| Goldquellen geprüft | ERFÜLLT |
| Quest-Gold | ERFÜLLT |
| Boss-Gold | ERFÜLLT |
| Mob-Gold | ERFÜLLT |
| Mob-Gold-Pickup | ERFÜLLT |
| Goldsenken geprüft | ERFÜLLT |
| NPC-Shops | ERFÜLLT |
| Trade Depot | ERFÜLLT |
| Spielerhandel | ERFÜLLT |
| Guild Economy | ERFÜLLT |
| persistente Gildenkasse | ERFÜLLT |
| Minor-Unit-Geldmodell | ERFÜLLT |
| Trade-Depot-Rundung | ERFÜLLT |
| main verändert | NEIN |
| test verändert | JA |

## Abgrenzung

Die Wirtschaft ist technisch geschlossen, aber eine endgültige quantitative Inflations-/Deflationsbalance benötigt reale Spielzeitdaten.

Deshalb werden bestehende Preiswerte nicht blind skaliert.

Die nächste Combat-Phase kann die Wechselwirkung aus:

- Killzeit
- Loot
- Gold/Stunde
- Gearkosten
- Bossreward
- Charakterlevel

weiter vermessen.
