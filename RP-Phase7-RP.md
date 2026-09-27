# PixelRPG – RP Phase 7 Audit

**Status:** abgeschlossen auf test  
**Phase:** 7 – RP  
**Referenz:** RP.md  
**Technischer Stand:** Java 25 + Paper 26.2 + Mojang-Mappings

## Erfüllte Phase-7-Punkte

### Spielerhandel
- Direkter Spielerhandel ergänzt.
- Verkäufer bietet den aktuell gehaltenen RPG-Gegenstand gegen Gold an.
- Der Gegenstand wird während des offenen Angebots sicher in Escrow gehalten.
- Käufer muss das Angebot ausdrücklich annehmen.
- Preis wird über die bestehende Money-Minor-Unit-Logik normalisiert.
- Soulbound/Unique/Admin-Items werden über die bestehende Economy-Sicherheitsprüfung ausgeschlossen.
- Inventarplatz wird vor Zahlung geprüft.
- Abbruch, Ablehnung, Timeout, Spieler-Disconnect und Plugin-Shutdown geben das Item zurück.
- Bestehendes Trade Depot bleibt unverändert und ergänzt den direkten Handel.

Befehle:
- /pixelrpg trade offer <Spieler> <Preis>
- /pixelrpg trade accept
- /pixelrpg trade decline
- /pixelrpg trade cancel

### Berufsspezialisierung
- Alle neun vorhandenen Berufe bleiben frei wählbar.
- Es gibt keinen globalen Berufszwang und keine künstliche Ein-Beruf-Sperre.
- Spezialisierung entsteht weiterhin durch die vom Spieler gewählte und ausgebaute Berufskombination.
- Bestehende Berufslevel-, Rezept- und XP-Systeme bleiben die technische Grundlage.
- Keine zusätzlichen XP-Zwangsmechaniken wurden eingeführt.

### Gilden
- Bestehende Gildenmitgliedschaft, Einladungen, Gildenkasse und Persistenz bleiben erhalten.
- Gilden können jetzt eine GUILD_CITY-Region dauerhaft beanspruchen.
- Die Gildenstadt wird in guilds.yml persistiert.
- Gildenmitglieder werden automatisch als Regionsmitglieder synchronisiert.
- Freigabe und Gildenauflösung entfernen die Gildenstadt-Zuordnung.
- Eine Gildenstadt kann nicht gleichzeitig mehreren Gilden gehören.
- Nur der Gildenleiter darf beanspruchen/freigeben.

Befehle:
- /pixelrpg guild city claim <Region>
- /pixelrpg guild city release

### NPC-Rollen
- Quest, Story, Beruf, Shop, Bank, Travel, Reception und Filler sind weiterhin getrennte NPC-Funktionen.
- Berufstrainer sind weiterhin an die neun vorhandenen Berufe gekoppelt.
- Travel-NPCs bleiben echte Weltpunkte statt eines globalen Teleportmenüs.

### Reisen
- Reisepunkte werden durch tatsächliches Besuchen freigeschaltet.
- Danach sind sie über das native Dialogsystem nutzbar.
- Die Guild-Compass-Funktion weist weiterhin auf bereits entdeckte Travel-NPCs hin.
- Reiseziele verwenden sichere Ankunftspositionen.
- Es wurde kein Elytra-Zwang oder künstliches Regions-Level-Gating eingeführt.

### Pferde
- Vanilla-Pferde bleiben unverändert nutzbar.
- Es gibt keine Plugin-Mechanik, die Pferdereisen durch Teleport, Elytra-Override oder XP-Gates entwertet.
- Die bestehende Reisearchitektur unterstützt damit die im Masterplan geforderte Bedeutung von Pferdereisen.

### Regionen / Gildenstädte
- GUILD_CITY bleibt ein eigener Regionstyp.
- Regionen sind kein Charakter-Level-Gate.
- Die vorhandenen Admin-Regionen bleiben technische Infrastruktur.
- Gildenstädte nutzen die bestehende Polygon-/Flag-/Member-Infrastruktur statt eines zweiten Regionssystems.

## Sicherheits-/Forensikprüfung

- Direkter Handel verwendet keine ungesicherten Itemdefinitionen.
- Keine statischen Player-/Entity-/World-Referenzen eingeführt.
- Keine Legacy-Chat-API eingeführt.
- Keine Legacy-NMS-/CraftBukkit-API eingeführt.
- Bestehende Build-Verifikation bleibt erhalten.
- main wurde nicht verändert.

## Ergebnis

Phase 7 erfüllt die im Masterplan für RP definierten technischen DoD-Punkte:

- Spielerhandel unterstützt
- Berufsspezialisierung ohne Berufszwang möglich
- Gilden können Identität und Gildenstädte entwickeln
- NPC-Rollen getrennt
- Reisen bleibt weltbezogen und entdeckungsbasiert
- Pferde bleiben normale relevante Mobilität
- Regionen bleiben ohne Levelzwang

Konkrete Weltkoordinaten für Städte/NPCs bleiben bewusst server-/Admin-Content und werden nicht erfunden.
