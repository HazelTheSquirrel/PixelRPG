# PixelRPG – RP- & Progressions-Rework

**Status:** Masterplan / Zielarchitektur
**Branch:** test
**Referenz:** main ausschließlich als Soll-/Vergleichszustand
**Prüfstand:** 25.09.2026

---

# 1. Ziel

PixelRPG wird nicht als klassisches Themepark-RPG behandelt, das dem Spieler eine vorgeschriebene Karriere vorgibt.

Das Plugin ist das technische Regelwerk für eine Minecraft-RP-Welt.

Es soll zwei Spielweisen gleichzeitig ermöglichen:

1. **RPG-/Grind-Spieler:** Quests, Mobs, Berufe, Crafting, Gear, Bosse und Level 60.
2. **RP-Spieler:** Charakter verkörpern, handeln, Berufe ausüben, Gilden aufbauen, reisen, Beziehungen zu anderen Spielern pflegen und eigene Geschichten erzeugen.

Beide Spielweisen verwenden dasselbe System.

**Leitprinzip:**
> Das Plugin unterstützt Roleplay. Es ersetzt das Roleplay nicht.

---

# 2. Was ausdrücklich nicht das Ziel ist

Nicht geplant als Kernsystem:

- Dungeon-Zwang
- automatische Dungeon-Kette
- Questpflicht
- Berufspflicht
- Monstergrindpflicht
- künstliche Daily-Quest-Schleifen
- ein einziger optimaler XP-Pfad
- Elytra als zentrale Mobilität
- Regionen als Levelgates
- World-Bosse als Pflichtprogression
- ein System, das jede soziale Handlung in XP umwandelt

Die Welt soll offen bleiben.

---

# 3. Aktueller Ausgangspunkt

Der test-Branch besitzt bereits einen großen RPG-Kern:

- Charakterlevel und XP
- PlayerProfile
- 149 Questdefinitionen
- Story-Kampagne
- 9 Berufe
- 329 Crafting-Rezepte
- 30 Itemdefinitionen
- 23 Food-Definitionen
- 6 Equipment-Sets
- 32 Bossdefinitionen
- 26 Biome-Bosse
- 6 World-Bosse
- 32 Boss-Reward-Items
- 28 Companions
- NPC-/Mannequin-System
- native Dialoge
- Party
- Guild
- Bank/Economy
- Shop
- Trade Depot
- Combat
- Mob Scaling
- Weapon Abilities
- Regionen
- Statistics
- Scoreboard
- Playtime
- Resourcepack
- YAML-/MySQL-Persistenz

Der Rework soll vorhandene Systeme möglichst weiterverwenden.

---

# 4. Zielarchitektur

Die Progression wird in vier große Ebenen aufgeteilt:

1. Charakter 1–60
2. Berufe 1–60
3. Ausrüstung / Item-Tiers
4. Welt- und Spielerwirtschaft

Diese Systeme greifen ineinander, sind aber nicht identisch.

Beispiel:

Charakter 37
Schmied 42
gutes Gear
Gildenmitglied
viel Vermögen

ist ein vollständig valider Charakter.

Ein anderer Spieler kann:

Charakter 37
Schmied 8
Jäger/Bergbauer hoch

haben.

Es gibt keine Pflicht zur Gleichverteilung.

---

# 5. Charakterprogression 1–60

## Ziel

Das normale Charakterlevel ist 1–60.

Level 60 ist das normale Ende der numerischen Charakterprogression.

Die bestehende XP-Kurve wird zunächst nicht blind geändert.

Aktueller Referenzpunkt:
- ungefähr 3,67 Mio. Gesamt-XP bis Level 60
- historische 1–99-Progression wird nicht automatisch übernommen

## Grundregel

Vor einer Änderung der XP-Kurve wird gemessen:

XP benötigt / reale XP pro Stunde = reale Levelzeit.

---

# 6. XP-Matrix

Es werden vier Referenzspielweisen vermessen:

## A – Questspieler

- Story
- normale Quests
- NPCs
- Reisen

## B – Monsterjäger

- normale Mobs
- Biome-Bosse
- Loot
- Reisen
- Verkauf

## C – Berufsspieler

- Sammeln
- Crafting
- Produktion
- Handel

## D – Mixed Player

- Quest
- Kampf
- Crafting
- Handel
- normale Weltaktivität

Für jeden Stil werden erfasst:

- XP pro Aktivität
- XP pro Stunde
- Gold pro Stunde
- Materialwert pro Stunde
- Gear-Fortschritt
- Berufsfortschritt
- Zeit bis Level 10
- Zeit bis Level 20
- Zeit bis Level 30
- Zeit bis Level 40
- Zeit bis Level 50
- Zeit bis Level 60

---

# 7. XP ist nicht das gesamte Fortschrittssystem

Nicht jede Tätigkeit braucht Charakter-XP.

Kein XP-Zwang für:

- RP-Gespräche
- Stadtbau
- Gildenorganisation
- normale Spielerbeziehungen
- Pferdereisen
- Handel als soziale Handlung

Diese Aktivitäten können trotzdem Fortschritt erzeugen über:

- Geld
- Besitz
- Gear
- Gildenstatus
- Reputation
- Zugang
- Spielerbeziehungen
- wirtschaftliche Position

---

# 8. Quests

Die 149 vorhandenen Quests bleiben als Contentbasis erhalten.

Quests bleiben eine legitime Charakter-XP-Quelle.

Sie werden aber nicht zur einzigen Progressionsquelle.

Für jede Quest wird später geprüft:

- Level
- XP
- Dauer
- Reiseaufwand
- Kampfaufwand
- Dialog-/RP-Anteil
- Wiederholbarkeit
- Belohnung
- Storywert

Story-XP darf sich durch Bedeutung unterscheiden, ohne die gesamte Levelkurve zu zerstören.

---

# 9. Story

Die vorhandene Story-Kampagne bleibt ein eigener Contentpfad.

Sie ist nicht die einzige Tür zum Endgame.

Ein Spieler darf:

- Story vollständig spielen
- Story teilweise spielen
- primär andere Systeme spielen

Die Story soll Charakterentwicklung unterstützen, nicht den kompletten Serverfortschritt monopolisieren.

---

# 10. Monster-XP

Das aktuelle Mob-Scaling wird als Baseline verwendet.

Aktuelle Referenz:

- Basis-HP ungefähr 20 + Level × 2,5
- XP ungefähr MaxHealth × 4,0

Beispielwerte liegen grob bei:
- Level 1: 90 XP
- Level 10: 180 XP
- Level 20: 280 XP
- Level 30: 380 XP
- Level 40: 480 XP
- Level 50: 580 XP
- Level 60: 680 XP

Diese Zahlen werden nicht isoliert geändert.

Gemessen werden:

- Killzeit
- Kills/Stunde
- XP/Stunde
- Gefahr
- Lootwert
- Spawnrate
- Gearabhängigkeit

---

# 11. Monsterjäger

Der Monsterjäger braucht kein künstliches Spezial-Minispiel.

Der Spieler kann einfach:

- reisen
- Monster töten
- Loot sammeln
- verkaufen
- Materialien weitergeben
- Ausrüstung verbessern
- Biome-Bosse suchen
- Spieleraufträge annehmen

Der RP-Kontext entsteht durch den Spieler.

Das Plugin muss diese Rolle nicht künstlich erzwingen.

---

# 12. Biome-Bosse

Biome-Bosse und World-Bosse sind strikt getrennt.

Biome-Bosse sind:

- seltene Weltbegegnungen
- automatisch/zufällig
- Teil der normalen Welt
- keine Dungeons
- keine Admin-Events

Sie sollen das Gefühl erzeugen:

> Man ist unterwegs und plötzlich passiert etwas.

Sie dürfen starke Belohnungen besitzen.

Sie dürfen aber nicht die einzige effiziente Levelquelle werden.

---

# 13. World-Bosse

World-Bosse sind:

- Eventbosse
- Adminspawn
- serverweite Ereignisse
- nicht normale Grindquelle

Die sechs vorhandenen World-Bosse bleiben konzeptionell getrennt.

World-Bosse erhalten:

- außergewöhnliches Event-Loot
- seltene Materialien
- Prestige
- besondere Items
- moderate XP

Sie dürfen nicht notwendig sein, um Level 60 zu erreichen.

---

# 14. Berufe 1–60

Alle neun Berufe werden langfristig auf 1–60 ausgerichtet.

Aktiv:
1. BLACKSMITH
2. SCHOLAR
3. FARMER
4. COOK
5. TAILOR
6. ALCHEMIST
7. MASON

Passiv:
8. FISHERMAN
9. WOODCUTTER

Die vorhandene Rezeptstruktur wird nicht unnötig ersetzt.

---

# 15. Beruf als soziale Rolle

Ein Beruf ist nicht nur:

Berufs-XP → Level.

Er ist:

> Was kann mein Charakter anderen Spielern anbieten?

Beispiel Schmied:

Kann:
- Waffen
- Rüstung
- Werkzeuge
- Aufträge
- Spezialprodukte

Braucht:
- Erz
- Kohle
- Leder
- seltene Materialien
- ggf. Wissen

Dadurch entstehen Spielerbeziehungen.

---

# 16. Solo und sozial

Grundregel:

**Solo möglich, sozial attraktiver.**

Ein Spieler darf alles selbst erledigen.

Aber Spezialisierung soll soziale Vorteile erzeugen.

Beispiel:

Solo:
Bergbau → Schmelzen → Schmieden → Verkaufen

Sozial:
Bergbauer → verkauft Erz → Schmied → fertigt → Händler/Krieger

Beide Wege funktionieren.

Der zweite erzeugt mehr RP.

---

# 17. Schmied 1–60

Die Schmiedeprogression wird auf sechs Zehnerstufen ausgerichtet:

| Berufslevel | Material |
|---|---|
| 1–10 | Leder/Holz |
| 11–20 | Kupfer |
| 21–30 | Eisen |
| 31–40 | Diamant |
| 41–50 | Netherite |
| 51–60 | Custom-Endgame |

Innerhalb einer Stufe werden Gegenstände sinnvoll freigeschaltet:

- einfache Werkzeuge zuerst
- Waffen danach
- Rüstung schrittweise
- starke Rüstungsteile zuletzt
- Bonuslevel möglich

Die konkrete Rezeptliste wird separat als Schmiede-Matrix erstellt.

---

# 18. Alle Berufe

Für jeden Beruf wird eine eigene Matrix erstellt:

- Level 1–60
- Rezepte
- Materialtier
- XP pro Aktivität
- Produktionszeit
- Materialkosten
- Unlockkosten
- Verkaufspreis
- Nachfrage
- soziale Rolle
- Abhängigkeiten zu anderen Berufen

---

# 19. Rezeptsystem

Die 329 vorhandenen Rezepte bleiben die Ausgangsbasis.

Jedes Rezept wird geprüft auf:

1. Beruf
2. Berufslevel
3. Material
4. Materialmenge
5. Custom-Item-Kosten
6. Unlockkosten
7. Questvoraussetzung
8. Output
9. Rarity
10. Produktionswert
11. XP
12. Wirtschaftswert
13. Spielerbedarf

Keine Rezeptänderung erfolgt nur nach Gefühl.

---

# 20. Crafting-XP

Crafting-XP ist primär Berufs-XP.

Es muss nicht automatisch riesige Charakter-XP erzeugen.

Damit bleibt:

Beruf = Spezialisierung

und

Charakter = allgemeine Weltprogression.

Exploitprüfung:

- XP pro Material
- XP pro Gold
- XP pro Minute
- wiederholbare Billigrezepte
- Massenproduktion

---

# 21. Materialwirtschaft

Materialien sind das Bindeglied der Berufe.

Beispiele:

Bergbauer → Erz → Schmied → Gear

Jäger → Leder → Schneider → Gear

Farmer → Nahrung → Koch → Food

Fischer → Fisch → Koch → Food

Holzfäller → Holz → Handwerk/Verkauf

Gelehrter → Wissen/seltene Komponenten → Spezialproduktion

Das erzeugt natürliche Nachfrage.

---

# 22. Spielerhandel

Vorhanden und weiterverwenden:

- Geld
- Bank
- Shop
- Trade Depot
- Guild Bank

Der Schwerpunkt soll trotzdem auf Spielerhandel liegen.

NPC-Shops:
- Basisversorgung
- Starter
- Standardwaren

Spielerhandel:
- Spezialwaren
- seltene Materialien
- hochwertige Craftingprodukte
- Dienstleistungen
- individuelle Preise

---

# 23. Trade Depot

Das Trade Depot bleibt für asynchronen Handel.

Es ergänzt den direkten Spielerhandel.

Es ersetzt nicht:

- Verhandlungen
- persönliche Übergabe
- Aufträge
- Gildenverträge

---

# 24. Gilden

Das Guild-System bleibt zentrale soziale Infrastruktur.

Vorhanden:

- Create
- Invite
- Accept
- Leave
- Info
- Disband
- Guild Bank
- Guild Storage
- Guild Currency
- Guild Compass

Langfristiges Ziel:

Gilde = echte Spielerorganisation.

Nicht nur Chatgruppe.

---

# 25. Gildenstädte

Gilden können eigene Städte besitzen/aufbauen.

Das Plugin soll dabei technische Regeln unterstützen.

Die Spieler bestimmen:

- Architektur
- Identität
- Organisation
- Bewohner
- Wirtschaft
- Regeln

Das ist kein automatischer Dungeon.

---

# 26. Regionen

Das Region-System wird aktuell als Admin-/Testwerkzeug behandelt.

Der Admin zieht Regionen selbst.

Daher:

- keine Levelpflicht
- kein automatisches Dungeon-Gating
- keine XP-Abhängigkeit
- keine Pflichtregionen

Regionen bleiben technische Infrastruktur für:

- Städte
- Gildenstädte
- Schutzbereiche
- Bossbereiche
- Gefahrenzonen
- Events

---

# 27. Reisen

Reisen ist Teil des RP.

Elytren werden nicht als zentrale Mobilitätslösung behandelt.

Pferde bleiben wichtig.

Ziel:

Stadt → Straße → Wildnis → Bergwerk → andere Stadt

statt:

Start → Elytra → Ziel.

Die Reise soll Begegnungen ermöglichen.

---

# 28. Welt als Content

Die Welt selbst soll der Contentträger sein:

- Städte
- Dörfer
- Straßen
- Wildnis
- Berge
- Wälder
- Flüsse
- Minen
- besondere Orte
- Biome-Boss-Begegnungen
- Gildenstädte

Keine künstliche Instanzwelt als Grundlage.

---

# 29. NPC-System

Das vorhandene NPC-/Mannequin-System bleibt.

NPC-Rollen:

- Quest
- Story
- Beruf
- Shop
- Bank
- Travel
- Filler
- Spezialrollen

NPCs liefern Basisservices.

Spieler sollen trotzdem Spezialisten sein können.

---

# 30. Spielerberufe dürfen NPCs übertreffen

Wenn NPC-Schmied und Spieler-Schmied exakt dasselbe anbieten, verliert der Spielerberuf Wert.

NPC:
- Basis
- Starter
- Notversorgung

Spieler:
- Spezialrezepte
- bessere Qualität
- Custom-Produkte
- Aufträge
- individuelle Preise
- soziale Beziehungen

---

# 31. Gelehrter

SCHOLAR wird als sozialer Wissensberuf behandelt.

Mögliche Funktionen:

- Forschung
- Wissen
- seltene Rezepte
- Identifikation
- Spezialkomponenten
- besondere Dienste

Nicht bloß:
Klick → XP.

---

# 32. Item-System

Das vorhandene Item-System bleibt.

Unterstützt werden bereits:

- Item-ID
- Rarity
- Item-Level
- Required Level
- Gearscore
- RPG Stats
- Soulbound
- Unique
- Admin-only
- Equipment Slots
- Sets
- Weapon Abilities
- Resourcepack IDs

Der nächste Schritt ist eine einheitliche mathematische Progression.

---

# 33. Stat-System

Das bisherige chaotische Zusammenspiel aus festen Werten und Stat-Würfen soll nicht die Grundlage der langfristigen Balance sein.

Ziel:

**deterministische Basiswerte + kontrollierte Varianz.**

Ein Item soll wegen seines Tiers nachvollziehbar stärker sein.

Zufall darf Loot interessant machen, aber nicht die komplette Balance zerstören.

---

# 34. Item-Tiers

Grundstruktur:

Materialtier
→ Itemlevel
→ Basiswerte
→ Rarity
→ optionale Affixe
→ Setbonus/Ability

Basiswerte werden planbar.

Affixe dürfen begrenzte Variation hinzufügen.

---

# 35. Equipment-Sets

Die sechs vorhandenen Sets bleiben.

Vorhandene Struktur:

- 2-Teile-Bonus
- 3-Teile-Bonus
- 4-Teile-Bonus

Jedes Set wird später geprüft auf:

- Levelbereich
- Basiswerte
- Setbonus
- Erwerbsweg
- Rolle
- Boss-/Questbezug
- Berufskopplung

---

# 36. Rarity

Bestehend:

- COMMON
- UNCOMMON
- RARE
- EPIC
- LEGENDARY
- UNIQUE

Rarity soll nicht nur eine größere Zufallszahl bedeuten.

Sie muss einen klaren Qualitäts-/Contentwert besitzen.

---

# 37. Soulbound

Soulbound wird nicht pauschal für alle starken Items verwendet.

Ein RP-/Wirtschaftssystem braucht handelbare hochwertige Waren.

Soulbound ist für:

- persönliche Belohnungen
- besondere Storyitems
- einzigartige Charaktergegenstände
- ausgewählte Progressionsitems

geeignet.

---

# 38. Unique Items

Unique bedeutet:

- besondere Identität
- besondere Fähigkeit
- besondere Herkunft
- begrenzte Verfügbarkeit

Nicht einfach:
„größere Zahl“.

---

# 39. Weapon Abilities

Die vorhandenen Weapon Abilities bleiben.

Sie werden gemeinsam mit:

- Itemtier
- Damage
- Cooldown
- Utility
- Rolle

balanciert.

---

# 40. Boss-Loot

Biome-Boss:

- selten
- spannend
- gute Beute
- Material/Item
- nicht Pflicht

World-Boss:

- Event
- Prestige
- besondere Items
- seltene Materialien
- moderate XP

Normaler Mob:

- zuverlässige Grundprogression

---

# 41. Companion-System

Die 28 vorhandenen Companions bleiben.

Sie dienen:

- Charakteridentität
- Komfort
- Kampfunterstützung
- passive Stats
- kosmetischer Identität

Companions dürfen nicht zur zwingenden Meta werden.

---

# 42. Economy

Die gesamte Economy wird als Kreislauf betrachtet.

Goldquellen:

- Quests
- Mobs
- Bosse
- Verkauf
- Spielerhandel

Goldsenken:

- Shops
- Crafting
- Unlocks
- Services
- Gilden
- ggf. Reparatur

Ziel:

Gold soll Wert behalten.

---

# 43. Quest-, Kampf- und Berufsprogression zusammenführen

Die drei Systeme dürfen nicht unabhängig explodieren.

Beispiel:

Quest gibt XP + Gold.

Mob gibt XP + Loot.

Crafting gibt Berufs-XP + Produkt.

Produkt kann verkauft werden.

Das Geld kann für andere Aktivitäten genutzt werden.

Damit entsteht ein Kreislauf statt drei getrennten Grindspielen.

---

# 44. Level-Tiers

Sechs Haupttiers:

| Tier | Charakter | Thema |
|---|---:|---|
| I | 1–10 | Starter |
| II | 11–20 | Kupfer |
| III | 21–30 | Eisen |
| IV | 31–40 | Diamant |
| V | 41–50 | Netherite |
| VI | 51–60 | Custom-Endgame |

Tier ist kein Regionszwang.

---

# 45. Endgame

Level 60 beendet die numerische Charakterprogression.

Es beendet nicht:

- Berufe
- Crafting
- Handel
- Gilden
- Bosses
- Story
- Exploration
- Events
- RP

Endgame ist Content/RP, nicht nur XP.

---

# 46. Grind-Ziel

Das Ziel ist ein langfristiges Gefühl ähnlich dem Prinzip klassischer MMORPG-Progression:

- Level haben Gewicht
- neue Stufen fühlen sich relevant an
- Gear bleibt länger sinnvoll
- Berufe haben Bedeutung
- Welt bleibt relevant
- Fortschritt braucht Zeit

Nicht Ziel:

„Alles maximal langsam machen.“

Guter Grind bedeutet:
- wiederholbare Aktivitäten
- mehrere Wege
- kleine Fortschritte
- Zwischenziele
- langfristige Ziele

---

# 47. RP und Grind dürfen nebeneinander existieren

Ein Spieler darf sagen:

„Ich spiele meinen Schmied.“

Ein anderer:

„Ich will heute nur zwei Stunden Monster töten.“

Ein dritter:

„Ich mache die Story.“

Ein vierter:

„Ich handle nur mit Spielern.“

Das System soll alle vier akzeptieren.

---

# 48. Kein Meta-Zwang

Das wichtigste Balancingziel ist nicht exakt gleiche XP pro Minute.

Das Ziel ist:

**Kein offensichtlicher Zwangspfad.**

Wenn eine Aktivität dauerhaft massiv effizienter ist als alle anderen, wird geprüft, ob sie den Rest des Spiels entwertet.

Wenn eine RP-Rolle dauerhaft so ineffizient ist, dass Spieler sie nur aus Spaß spielen können, wird ebenfalls geprüft.

---

# 49. RP-Wert ist ein legitimer Wert

Nicht jede Mechanik muss maximal XP-effizient sein.

Ein System darf bewusst weniger XP geben, wenn sein Hauptwert:

- soziale Interaktion
- Wirtschaft
- Weltgefühl
- Charakteridentität
- Exploration

ist.

Das wird nicht automatisch als „schlechter“ betrachtet.

---

# 50. Spielerabhängigkeit

Ein wichtiges Designziel ist:

**Spezialisierung erzeugt Nachfrage.**

Beispiel:

Bergbauer → Erz

Schmied → Waffen

Schneider → Rüstung

Jäger → Leder

Farmer → Lebensmittel

Koch → Food

Gelehrter → Wissen/Spezialkomponenten

Händler → Verteilung

Dadurch entstehen echte Abhängigkeiten zwischen Spielern.

---

# 51. Aufträge

Langfristig kann das System Spieleraufträge technisch unterstützen.

Beispiel:

Schmied:
„20 Eisenäxte.“

Kunde:
bezahlt.

Schmied:
produziert.

Übergabe:
Spieler zu Spieler.

Das ist optional und kein Zwangssystem.

---

# 52. Quest vs. RP

Eine Quest darf einen Spieler zu einem NPC führen.

Sie soll aber nicht jede soziale Handlung ersetzen.

Beispiel:

Quest:
„Finde heraus, wer Eisen liefert.“

Danach kann der Spieler tatsächlich einen Bergbauer suchen.

Das Plugin gibt den Anlass.

Die Spieler erzeugen die Szene.

---

# 53. Dialogsystem

Das native aktuelle Paper-Dialogsystem bleibt verbindlich.

Keine alten Dialog-Workarounds.

Dialoge werden genutzt für:

- Story
- Quests
- Berufe
- Shops
- Bank
- Travel
- NPC-Information

Aber Dialoge sollen Spielerinteraktion nicht vollständig ersetzen.

---

# 54. Party

Party bleibt Gruppenkomfort.

Vorhanden:

- Invite
- Accept
- Leave
- Kick
- Transfer
- Disband
- Info
- Quest Sharing

Die bestehende PartyGUI-Restinkonsistenz muss auf das aktuelle Dialogsystem bereinigt werden.

---

# 55. Guild + Party

Party ist kurzfristige Gruppe.

Guild ist langfristige Organisation.

Diese Rollen dürfen nicht vermischt werden.

Party:
„Wir machen heute gemeinsam X.“

Guild:
„Wir gehören dauerhaft zu dieser Organisation.“

---

# 56. Scoreboard / XP-Bar

Die permanente Levelanzeige darf nicht wichtige Quest-/Navigationsinformationen verdrängen.

Priorität:

1. wichtige Navigation / Quest
2. temporäre Systemmeldung
3. permanente Level-/XP-Anzeige

Die vorhandene EXP-Bar-Nutzung wird bei der UI-Überarbeitung berücksichtigt.

---

# 57. NPC-/Shop-Deployment

Forensischer Befund:

Das NPC-System erwartet Runtime-npcs.yml.

Das Shop-System erwartet Runtime-shops.yml.

Die Engine ist vorhanden, aber ein vollständig vorausgefüllter Repository-Default ist nicht in allen Fällen vorhanden.

Das wird im Rahmen der Contentbereitstellung geprüft.

---

# 58. Food

Die 23 Food-Definitionen werden vollständig auf Erwerbswege geprüft.

Jedes Food braucht einen nachvollziehbaren Pfad:

- Farm
- Kochen
- Drop
- NPC
- Crafting
- Quest
- Spielerhandel

Kein dauerhaft unerreichbares oder ungenutztes Food.

---

# 59. Content-Integrität

Für jeden Contenttyp wird künftig geprüft:

Quelle
→ Registry
→ Service
→ Runtime
→ Spielerinteraktion
→ Belohnung
→ Persistenz

Ein Datensatz allein gilt nicht als „fertiger Content“.

---

# 60. World Boss / Biome Boss / Region klare Trennung

Diese drei Dinge dürfen nicht verwechselt werden.

Biome-Boss:
normale Weltbegegnung.

World-Boss:
Admin-/Serverevent.

Region:
technische Weltgeometrie bzw. zukünftige Weltlogik.

Region ist kein Dungeon.

World-Boss ist kein normales Mob-Grind.

Biome-Boss ist kein World-Event.

---

# 61. Admin-Testsysteme

Admin-/Testmechaniken werden nicht als normale Spielerprogression bewertet.

Dazu gehören:

- Region-Editor
- Debug
- Adminspawn
- Admin-Item
- Testregionen
- Editoren

Sie bleiben für Entwicklung und Administration verfügbar.

---

# 62. Technische Arbeitsweise

Vor jeder Änderung:

1. test-Branch lesen
2. main vergleichen
3. vorhandene Architektur verstehen
4. Dependencies prüfen
5. aktuelle Paper-26.2-API prüfen
6. nur notwendige Dateien ändern
7. bestehende Funktion schützen
8. Verifikation erhalten
9. Build ausführen
10. Ergebnis prüfen

Kein unnötiges Refactoring.

---

# 63. Implementierungsphase 0 – Baseline

Noch keine Balanceänderung.

Erstellen:

- Charakter-XP-Matrix
- Quest-XP-Matrix
- Mob-XP-Matrix
- Boss-XP-Matrix
- Berufs-XP-Matrix
- Crafting-Matrix
- Gold-Matrix
- Item-Stat-Matrix

Ziel:
vollständige Ist-Aufnahme.

---

# 64. Implementierungsphase 1 – Charakter

Danach:

- Level 1–60 finalisieren
- XP-Kurve validieren
- Meilensteine definieren
- XP-Quellen abstimmen
- Levelzeit bestimmen

---

# 65. Implementierungsphase 2 – Berufe

Danach:

- 9 Berufe 1–60
- Rezepte zuordnen
- Materialtiers
- Berufs-XP
- Unlocks
- Produktionsökonomie

---

# 66. Implementierungsphase 3 – Items

Danach:

- Tiermatrix
- Slotmatrix
- Basiswerte
- Rarity
- Setboni
- Abilities
- Unique Items
- Boss Items

---

# 67. Implementierungsphase 4 – Economy

Danach:

- Goldquellen
- Goldsenken
- NPC-Shops
- Trade Depot
- Spielerhandel
- Guild Economy

---

# 68. Implementierungsphase 5 – Combat

Danach:

- Mob HP
- Mob Damage
- Mob XP
- Loot
- Player Parity
- Biome-Bosses
- World-Bosses

---

# 69. Implementierungsphase 6 – Quest/Story

Danach:

- Quest-XP
- Levelanforderungen
- Story-XP
- Rewards
- Followups
- Weltanbindung

---

# 70. Implementierungsphase 7 – RP

Danach:

- Spielerhandel
- Berufsspezialisierung
- Gilden
- Gildenstädte
- NPC-Rollen
- Reisen
- Pferde
- Weltverteilung

---

# 71. Implementierungsphase 8 – UI

Danach:

- Questtracking
- Navigation
- Levelanzeige
- XP-Bar
- Dialoge
- Party
- Scoreboard

---

# 72. Implementierungsphase 9 – Abschlussaudit

Prüfen:

- XP-Exploits
- Crafting-Exploits
- Goldinflation
- Item-Ausreißer
- Quest-Ausreißer
- Mob-Ausreißer
- Boss-Ausreißer
- Meta-Pfade
- Legacy-API
- Paper-26.2-Kompatibilität
- Build
- Verifikation

---

# 73. Definition of Done

## Charakter
- [ ] Level 1–60 konsistent
- [ ] XP-Kurve gemessen
- [ ] alle XP-Quellen dokumentiert
- [ ] kein dominanter Meta-XP-Weg

## Berufe
- [ ] alle 9 Berufe 1–60
- [ ] Rezepte zugeordnet
- [ ] Materialtiers definiert
- [ ] Produktionsökonomie geprüft

## Items
- [ ] jedes Item einem Tier zugeordnet
- [ ] Basiswerte deterministisch
- [ ] Rarity sinnvoll
- [ ] Sets geprüft
- [ ] Unique Items geprüft

## Combat
- [ ] Mob-XP gemessen
- [ ] Mob-HP/Damage geprüft
- [ ] Loot geprüft
- [ ] Biome-Bosse separat
- [ ] World-Bosse separat

## Quests
- [ ] 149 Quests geprüft
- [ ] XP geprüft
- [ ] Levelanforderungen geprüft
- [ ] Story unabhängig vom Grind nutzbar

## Economy
- [ ] Goldquellen geprüft
- [ ] Goldsenken geprüft
- [ ] Shops geprüft
- [ ] Trade Depot geprüft
- [ ] Spielerhandel unterstützt

## RP
- [ ] Spezialisierung möglich
- [ ] kein Berufszwang
- [ ] Spielerhandel sinnvoll
- [ ] Gilden können Identität entwickeln
- [ ] Reisen bleibt relevant
- [ ] Pferde bleiben sinnvoll
- [ ] Regionen sind kein Levelzwang

## Technik
- [ ] Java 25
- [ ] Paper 26.2
- [ ] Mojang-Mappings
- [ ] native Dialoge
- [ ] keine Legacy-APIs
- [ ] Verifikation erhalten
- [ ] test bleibt Entwicklungsbranch
- [ ] main bleibt Referenz

---

# 74. Designregel für neuen Content

Jeder neue Content wird anhand dieser Fragen bewertet:

1. Was kann der Spieler damit tun?
2. Warum ist es für die Welt interessant?
3. Welche andere Aktivität wird dadurch relevant?
4. Welche Spielerbeziehung kann entstehen?
5. Welche Progressionsstufe wird unterstützt?
6. Welche Wirtschaft entsteht daraus?
7. Ist es solo nutzbar?
8. Ist es sozial nutzbar?
9. Erzeugt es echten Content oder nur eine weitere XP-Zahl?

Ein System, das nur „mehr XP“ erzeugt, ohne einen weiteren Zweck zu besitzen, soll kritisch hinterfragt werden.

---

# 75. Endzustand

Der gewünschte Endzustand ist:

Minecraft-Welt
→ PixelRPG-Regelwerk
→ Charakterprogression
→ Berufsprogression
→ Ausrüstung
→ Wirtschaft
→ Gilden
→ Quests
→ Bosse
→ Welt
→ Spieler
→ eigene Entscheidungen
→ eigenes Roleplay

Der Spieler entscheidet, ob er:

- Schmied
- Bergbauer
- Gelehrter
- Koch
- Farmer
- Jäger
- Händler
- Abenteurer
- Gildenmitglied
- Einzelgänger
- Questspieler
- Grindspieler
- RP-Spieler

sein möchte.

Er darf zwischen diesen Rollen wechseln.

---

# 76. Verbindliche Kernentscheidung

PixelRPG bekommt **keine vorgeschriebene Spielerkarriere**.

Es bekommt ein gemeinsames Regelwerk, in dem unterschiedliche Lebensweisen funktionieren.

Die XP-Kurve soll langfristig und grindlastig sein, aber nicht das RP verdrängen.

Die Berufe sollen soziale Nachfrage erzeugen.

Die Welt soll Reisen und Begegnungen fördern.

Bosse sollen besondere Ereignisse sein.

Quests sollen Geschichten und Fortschritt liefern.

Items sollen nachvollziehbar skalieren.

Spieler sollen miteinander handeln können.

Gilden sollen eigene Identitäten entwickeln können.

Das Plugin liefert die Mechanik.

**Die Spieler schreiben die Geschichten.**

---

# 77. Nächster konkreter Schritt

Vor einer Implementierung wird die vollständige Ist-Matrix erstellt:

CHARACTER
- Level 1–60
- XP pro Level
- XP pro Stunde

QUEST
- Quest-ID
- Level
- XP
- Aufwand
- Wiederholbarkeit

COMBAT
- Mob-Level
- HP
- Damage
- XP
- Loot
- Killzeit

BOSS
- Typ
- Level
- HP
- XP
- Loot
- Spawnmodell

PROFESSION
- Beruf
- Level
- XP
- Rezepte
- Material
- Produktionszeit

ITEM
- Tier
- Level
- Slot
- Stats
- Rarity
- Gearscore

ECONOMY
- Goldquelle
- Goldsenke
- Wert pro Stunde

RP
- Spielerabhängigkeit
- Handel
- Spezialisierung
- Gildenbezug

Erst nach dieser Messung werden konkrete Zahlen geändert.

**Diese RP.md ist damit der Masterplan für den gesamten Rework und nicht nur eine XP-Anpassung.**
