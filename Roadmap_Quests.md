# PixelRPG – Quest & Companion Roadmap

> Verbindliche Planungsgrundlage für Quest- und Begleiter-System.
>
> Zielplattform: Paper 26.x, aktuell Paper 26.2. Diese Datei beschreibt das gewünschte Design und die Reihenfolge der späteren Umsetzung. Sie ist keine Aufforderung, alle Punkte sofort zu implementieren.

---

# 1. Grundprinzip

PixelRPG ist eine Open-World-RPG-Welt. Quests sollen den Spieler führen, ohne die Welt in eine reine GPS-/Marker-Map zu verwandeln.

Nicht gewünscht:

- sinnlose Koordinatenquests
- künstliche Gebietsgrenzen nur wegen Quest-Leveln
- Reputation-System
- Titel-System
- endlose Fetch-Quests ohne Zusammenhang
- ein Begleiter-System, das den Spieler ersetzt

Gewünscht sind erkennbare NPCs, Dörfer, Berufe, Gegner, Geschichten, Erkundung und nachvollziehbare Ziele.

---

# 2. Spielerprogression

```text
Level 1 → 99
Level 100 → Grind-/Endgame-Bereich
```

Level 100 ist kein normaler Quest-Level. Die reguläre Questprogression endet bei Level 99.

Langfristig benötigen wir deshalb sehr viele Quests. Wir bauen aber nicht stumpf „X Quests pro Level“, sondern mehrere Questketten über Levelbereiche.

Beispiel:

```text
1–5     Einführung
5–10    lokale Questkette
10–20   Dorf / Berufe / erste größere Konflikte
20–30   regionale Handlung
30–50   mehrere miteinander verbundene Quest-Hubs
50–70   größere Weltkonflikte
70–90   fortgeschrittene Questketten
90–99   späte Haupt-/Nebeninhalte
100+    Grind / spätere Endgame-Systeme
```

Die exakte XP-Balance wird durch Tests bestimmt.

---

# 3. Maximal 5 aktive Quests

Ein Spieler kann maximal **5 aktive Quests gleichzeitig** besitzen.

Eine sechste Quest darf nicht angenommen werden.

Eine mehrstufige Quest zählt weiterhin als eine aktive Quest, solange ihre Schritte Teil derselben Questinstanz sind.

Ziel:

```text
wenige bewusste Aufgaben
        ↓
Spieler entscheidet, was er verfolgt
        ↓
Questliste bleibt übersichtlich
```

---

# 4. Quest-Freischaltung

Eine Quest mit empfohlenem Level `X` wird grundsätzlich bereits bei `X - 5` freigeschaltet.

Beispiel:

```text
Quest-Level 20
      ↓
freigeschaltet ab Level 15
      ↓
empfohlen für Level 20
```

Die fünf Level Vorlauf sind ein Designwert und müssen später mit realem Gameplay getestet werden.

Zusätzliche Voraussetzungen sind möglich, beispielsweise eine vorherige Quest, ein NPC-Gespräch oder ein benötigter Gegenstand.

---

# 5. Bestehende Quest-NPCs

Die aktuellen Quest-NPCs bleiben die normalen Questgeber.

Grundstruktur:

```text
Quest-NPC
   ↓
Spielerlevel prüfen
   ↓
verfügbare Levelbereiche
   ↓
Quest auswählen
   ↓
Details / Dialog
   ↓
Annehmen
```

Die NPCs sollen erkennbare Rollen besitzen, zum Beispiel:

- Dorfvorsteher
- Wache
- Jäger
- Schmied
- Versorger
- Alchemist
- Gelehrter
- Händler
- Abenteurer

Sie sollen nicht zu anonymen Koordinatengebern werden.

---

# 6. Filler-/Reise-NPC

Wir benötigen einen generischen **Filler-/Reise-/Orts-NPC**.

Dieser NPC ist kein zweites großes Quest-Framework. Er dient als bewusst gesetzter Zielpunkt innerhalb einer Questkette.

Beispiele:

```text
„Reise nach Sonnenhain und sprich mit Mira.“
„Gehe zum Dorf Eisenfels und suche den dortigen Schmied.“
„Finde den Verwalter des Dorfes und frage ihn nach dem verschwundenen Händler.“
```

Der Admin kann den NPC in Dörfern, Spielerprojekten oder anderen wichtigen Orten platzieren.

Damit können auch später von Spielern gebaute Dörfer in Questketten aufgenommen werden.

---

# 7. Dorf-zu-Dorf-Questketten

Dörfer können eigene Quest-Hubs sein.

```text
Dorf A
  ↓
Questgeber A
  ↓
Reise nach Dorf B
  ↓
Filler-/Ziel-NPC B
  ↓
neue lokale Handlung
  ↓
Schmied / Händler / Wache / Gelehrter
  ↓
weitere Questkette
```

Dorf B muss nicht einfach die Fortsetzung von Dorf A sein. Es kann eine eigene Geschichte und andere Aufgaben besitzen.

Dadurch können auch Spielerdörfer später sinnvoll eingebunden werden.

---

# 8. Koordinatenquests

Keine regulären Quests nach dem Muster:

```text
Gehe zu X:123 Y:64 Z:456.
```

wenn der Spieler nicht weiß, was dort ist.

Akzeptabel:

```text
„Reise zum Dorf Sonnenhain.“
„Sprich dort mit dem Dorfvorsteher.“
```

Intern dürfen natürlich Koordinaten gespeichert werden. Die technische Position ersetzt aber nicht die verständliche Questbeschreibung.

---

# 9. Recovery Compass

Der `minecraft:recovery_compass` soll eine echte Quest-Funktion bekommen.

```text
Questziel bekannt
      ↓
Recovery Compass
      ↓
Richtung zum Ziel
      ↓
Spieler findet den Ort selbst
```

Er soll kein vollständiges GPS sein. Die Welt bleibt offen und erkundbar.

Besonders geeignet für:

- Dorfziele
- NPC-Ziele
- Erkundungsziele
- definierte Quest-Orte

Bei mehreren aktiven Quests muss später eindeutig geregelt werden, welches Ziel der Compass verfolgt.

---

# 10. Questtypen

Der langfristige Quest-Pool soll mehrere Typen kombinieren:

- Gegner besiegen
- bestimmte Gegner untersuchen
- Gegenstände sammeln
- Ressourcen beschaffen
- Gegenstände liefern
- NPCs aufsuchen
- Dialog-/Storyquests
- Reise zu bekannten Dörfern/Orten
- Dorf-zu-Dorf-Ketten
- Erkundungsquests
- Berufsquests
- mehrstufige Questketten
- Begleiter-bezogene Quests
- besondere Welt-/Eventquests

Ein Questtyp darf mit anderen kombiniert werden.

Beispiel:

```text
NPC
 ↓
Dialog
 ↓
Mine untersuchen
 ↓
Erz sammeln
 ↓
Gegner besiegen
 ↓
Dorf B besuchen
 ↓
Gelehrten sprechen
 ↓
Belohnung
```

---

# 11. Questbeispiele

## Kleine Quest

```text
„Die verschwundenen Werkzeuge“
Level 8

Ein Schmied vermisst seine Werkzeuge.
Der Spieler untersucht die Werkstatt,
findet Spuren und holt die Werkzeuge zurück.

Belohnung:
XP + Geld + Item
```

## Dorfquest

```text
„Der Weg nach Sonnenhain“
Level 15

Der Spieler soll nach Sonnenhain reisen
und dort Mira finden.

Recovery Compass hilft bei der Richtung.

Bei Mira beginnt eine neue lokale Quest.
```

## Größere Kette

```text
„Das Schweigen von Eisenfels“
Level 25

Wache befragen
 ↓
Mine untersuchen
 ↓
Erzproben sammeln
 ↓
Ursache der Störung finden
 ↓
Schmied informieren
 ↓
Dorf B besuchen
 ↓
Gelehrten sprechen
 ↓
Folgequest freischalten
```

---

# 12. Questbelohnungen

Mögliche Belohnungen:

- Spieler-XP
- Geld
- Items
- Verbrauchsgegenstände
- Berufs-/Craftingmaterialien
- Questgegenstände
- Folgequests
- Begleiter
- Begleiter-Freischaltungen

Reputation und Titel gehören ausdrücklich nicht dazu.

---

# 13. Begleiter – Grundsatz

Begleiter werden ein eigenes Progressionssystem.

Der wichtigste Grundsatz:

> **Begleiter unterstützen den Spieler. Sie ersetzen ihn nicht.**

Die Balance wird immer als Kombination betrachtet:

```text
Spieler + Begleiter
        ↓
Gegner / Elite / Boss
```

Ein Legendary-Begleiter darf keinen Boss trivial machen.

---

# 14. Begleiter-Pool – nicht auf wenige Mobs beschränken

Wolf, Biene, Zombie, Schwein und Warden sind **nur Beispiele**.

Das System soll grundsätzlich möglichst viele tatsächlich vorhandene und technisch geeignete Minecraft-/Mojang-Entities der verwendeten Version unterstützen.

Die Architektur darf deshalb nicht auf eine feste Liste von fünf oder zehn Mobs zugeschnitten werden.

Ziel:

```text
Entity Registry / vorhandene Entity-Typen
              ↓
technische Prüfung
              ↓
Begleiterdefinition
              ↓
Seltenheit
              ↓
Basiswerte + Skalierung
              ↓
Freischaltung
              ↓
Begleiter
```

Die Minecraft-Wiki-Mob-Liste kann als Inspirationsquelle dienen. Für den tatsächlichen Code gilt ausschließlich, was in der Zielversion vorhanden und technisch nutzbar ist.

---

# 15. April-Fools- und ungewöhnliche Mobs

Auch Entities, die ursprünglich als April-Fools-, Test-, Spaß- oder ungewöhnliches Feature eingeführt wurden, sollen nicht automatisch ausgeschlossen werden.

Wenn eine solche Entity in der tatsächlich verwendeten Version weiterhin vorhanden und technisch nutzbar ist, wird sie wie jede andere mögliche Begleiter-Entity geprüft.

Entscheidend sind:

- tatsächlich in der Zielversion vorhanden
- sicher spawnbar/darstellbar
- kontrollierbar
- persistent handhabbar
- keine gefährlichen Weltmechaniken
- vertretbare Performance
- sinnvolle Darstellung
- vertretbare Balance

Damit bleibt das Spektrum bewusst groß.

Nicht jede Entity muss Begleiter werden. Reine Projektile, kurzlebige Effekte oder technisch unkontrollierbare Entity-Typen dürfen ausgeschlossen werden.

---

# 16. Begleiter-Kategorien

Mögliche Kategorien:

- Haustiere
- Tiere
- neutrale Mobs
- feindliche Mobs
- fliegende Mobs
- aquatische Mobs
- humanoide Mobs
- große Mobs
- kleine Mobs
- seltene/ungewöhnliche Mobs
- Mannequin-/Spielermodelle
- Custom-/Unique-Begleiter

Das Aussehen bestimmt nicht automatisch das Verhalten.

Ein Zombie-Begleiter muss beispielsweise nicht automatisch Spieler angreifen.

Darstellung und Verhalten werden getrennt behandelt.

---

# 17. Seltenheiten

Geplante Seltenheiten:

| Seltenheit | Beispiel | Grundidee |
|---|---|---|
| Common | Biene | einfacher, schwacher Begleiter |
| Uncommon | Wolf | solider früher Begleiter |
| Rare | Zombie | stärkerer Begleiter |
| Epic | Schwein | besonderer, perspektivisch reitbarer Begleiter |
| Legendary | Warden | extrem selten, stark, aber kontrolliert |
| Unique | Custom/Mannequin | ausschließlich individuell durch Admin |

Diese Beispiele sind keine feste Zuordnung aller Mobs. Jeder geeignete Mob kann abhängig von Rolle, Verfügbarkeit und Balance einer passenden Seltenheit zugeordnet werden.

---

# 18. Begleiter-Level

Begleiter haben eigene Level:

```text
Begleiter Level 1 → 99
Level 100 → Grind-Bereich
```

Der Begleiter wird nicht automatisch auf das Spielerlevel gesetzt.

Beispiel:

```text
Spieler Level 50
Wolf Level 27
```

Der Begleiter muss seinen eigenen Fortschritt machen.

---

# 19. Begleiter-XP

Ein Begleiter erhält XP nur, wenn er aktiv beim Spieler ist.

```text
Begleiter aktiv
    ↓
XP möglich

Begleiter weggeschickt
    ↓
keine Begleiter-XP
```

Mögliche spätere XP-Quellen:

- Questabschluss
- besiegte Gegner
- Erkundung
- Weltaktivitäten
- spezielle Begleiterquests

Die exakte Verteilung bleibt bis zu realen Tests offen.

Wichtig: AFK-XP, Duplizierung und XP ohne aktive Begleiterinstanz müssen verhindert werden.

---

# 20. Feste Werte und Skalierung

Begleiter werden keine zweiten frei konfigurierbaren Spielercharaktere.

Grundmodell:

```text
Entity-Typ
 ↓
Seltenheit
 ↓
Basiswerte
 ↓
Level-Skalierung 1–99
 ↓
finale Begleiterwerte
```

Die Werte werden pro Begleiterdefinition kontrolliert.

Seltenheit darf nicht einfach bedeuten „10× alles“. Die Progressionskurven müssen so gewählt werden, dass Spieler + Begleiter weiterhin mit Gegnern und Bossen harmonieren.

---

# 21. Größe und Darstellung

Die Größe eines Begleiters soll perspektivisch unabhängig von seinen Kampfstärken angepasst werden können, sofern die aktuelle Paper-/Minecraft-API dies für die jeweilige Entity ermöglicht.

Beispiele:

```text
Wolf → 50 % → Mini-Wolf

Warden → kleiner → Mini Warden

Biene → größer → ungewöhnlich großer Begleiter
```

Darstellungsgröße und Kampfbalance sind getrennt.

Ein kleiner Warden ist nicht automatisch schwächer.
Ein großer Wolf ist nicht automatisch stärker.

Die Größe darf nicht unbemerkt als Damage-/HP-Multiplikator dienen.

---

# 22. Passive Begleiter zuerst

Phase 1 des Begleitersystems ist bewusst passiv.

Zuerst müssen funktionieren:

- Besitz
- Freischaltung
- Persistenz
- Auswahl
- Rufen
- Wegschicken
- Folgen
- Name
- Level
- XP
- UI
- Unique-Ausnahme

Noch keine komplexe Kampf-KI.

Erst wenn dieses Fundament stabil ist, kommen Angriffe und Fähigkeiten.

---

# 23. Begleiter-Befehle

Normale Begleiter:

```text
Rufen
Wegschicken
Umbenennen
```

Unique-Begleiter:

```text
Rufen
Wegschicken
```

Der feste Name eines Unique-Begleiters darf nicht verändert werden.

---

# 24. Unique-Begleiter

Unique ist keine normale Farm-/Drop-Seltenheit.

Unique-Begleiter werden **ausschließlich durch einen Admin vergeben**.

Mögliche Gründe:

- persönliches Geschenk
- Event
- Community-Meilenstein
- besondere Leistung
- Story-/Sonderbelohnung
- individueller Spielerbegleiter

Beispiel:

```text
Unique: „Arven“
Modell: Mannequin / Spieler
Skin: fest
Name: fest
Umbenennen: verboten
Verhalten: passiv
Vergabe: Admin
```

Unique bedeutet nicht automatisch „stärker als alles andere“. Auch Unique muss zur Balance passen.

---

# 25. Freischaltung normaler Begleiter

Common bis Legendary sollen mehrere nachvollziehbare Freischaltungswege besitzen.

Mögliche Quellen:

## Questbelohnung

```text
Questkette
 ↓
Abschluss
 ↓
Begleiter freigeschaltet
```

## Seltenes Drop-/Loot-System

Ein passender Begleiter kann eine sehr seltene Dropchance besitzen.

Das darf aber nicht der einzige Weg sein.

## Besonderer NPC

Ein Händler, Züchter, Forscher oder anderer NPC kann einen Begleiter unter bestimmten Voraussetzungen freischalten.

## Event / Weltaktivität

Besondere Ereignisse können Begleiter vergeben.

## Begleiterbezogene Quest

Beispiel:

```text
verletzten Wolf finden
 ↓
Materialien beschaffen
 ↓
Wolf retten
 ↓
Wolf-Begleiter freigeschaltet
```

## Mehrere Voraussetzungen

Spätere seltene Begleiter können mehrere Bedingungen verlangen:

```text
Level 50+
+
Questkette abgeschlossen
+
bestimmtes Item
+
NPC besucht
=
Begleiter
```

Die Freischaltung soll möglichst verständlich und thematisch begründet sein.

---

# 26. Begleiter als Questbelohnung – Beispiele

## Common – Biene

```text
„Der leere Bienenstock“
Level 8

Imker helfen
 ↓
Bienenstöcke reparieren
 ↓
verlorene Biene finden
 ↓
Biene zurückbringen

Belohnung:
XP + Geld + Biene
```

## Uncommon – Wolf

```text
„Die Wölfe von Silberhain“
Level 15–20

Angriffe auf Holzfäller untersuchen
 ↓
Spuren verfolgen
 ↓
verletzten Wolf finden
 ↓
Ursache des Konflikts lösen

Belohnung:
Wolf-Begleiter
```

## Rare – Zombie

```text
„Die Nacht unter dem Friedhof“
Level 30–35

ungewöhnliche Untote untersuchen
 ↓
mehrere NPCs befragen
 ↓
besonderen Zombie finden
 ↓
Hintergrund aufklären

Belohnung:
Rare Zombie-Begleiter
```

## Epic – Schwein

```text
„Der legendäre Hof“
Level 45+

lokale Geschichte um ein außergewöhnliches Schwein
 ↓
mehrere Aufgaben
 ↓
Abschluss

Belohnung:
Epic Schwein-Begleiter
```

Perspektivisch kann ein Epic-Schwein reitbar sein, wenn das Reitsystem stabil ist.

## Legendary – Warden

Der Warden soll nicht einfach durch einen normalen Warden-Kill droppen.

Beispiel:

```text
lange Questkette
 ↓
Deep-Dark-/Ancient-City-Bezug
 ↓
mehrere Voraussetzungen
 ↓
gefährliche Questabschnitte
 ↓
Abschluss
 ↓
Legendary Warden-Begleiter
```

---

# 27. Begleiter müssen zur Geschichte passen

Schlecht:

```text
Töte 10 Zombies.
Belohnung: Zombie.
```

Besser:

```text
Ein ungewöhnlicher Zombie bewacht nachts einen Ort.
Der Spieler untersucht sein Verhalten.
Die Quest erklärt den Hintergrund.
Am Ende kann dieser Zombie Begleiter werden.
```

Der Begleiter soll sich wie ein Teil der Welt anfühlen, nicht wie ein beliebiges Item.

---

# 28. Begleiter als Teil einer Questkette

Begleiter können selbst zum Inhalt einer Quest werden:

```text
NPC
 ↓
„Finde das verletzte Tier.“
 ↓
Tier finden
 ↓
Materialien beschaffen
 ↓
Tier heilen
 ↓
Begleiter freigeschaltet
```

Danach kann derselbe Begleiter weitere XP sammeln und den Spieler durch andere Quests begleiten.

---

# 29. Keine Begleiter-Inflation

Begleiter sollen selten genug bleiben, damit neue Begleiter langfristig interessant sind.

Ziel:

```text
wenige frühe Begleiter
 ↓
mehr Auswahl im Midgame
 ↓
seltene Endgame-Begleiter
 ↓
Unique als Sonderkategorie
```

Nicht jeder zweite Questabschluss muss einen Begleiter vergeben.

---

# 30. Mehrere besitzen, nur einer aktiv

Langfristig soll ein Spieler mehrere Begleiter sammeln können.

Aktiv ist grundsätzlich nur **einer gleichzeitig**.

Beispiel:

```text
Besitz:
Biene
Wolf
Zombie
Schwein

Aktiv:
Wolf
```

Das hält die Kampfbalance und Entity-Anzahl kontrollierbar.

---

# 31. Begleiter-UI

Die bestehende native Dialog-Struktur soll genutzt werden.

Die Oberfläche soll mindestens zeigen:

- Name
- Seltenheit
- Level
- XP
- Status
- Rufen
- Wegschicken
- Umbenennen bei normalen Begleitern

Bei Unique wird „Umbenennen“ nicht angeboten.

---

# 32. Persistenz und Besitz

Begleiter müssen dauerhaft gespeichert werden.

Mindestens benötigt:

- Besitzer / Player UUID
- Begleiter-ID
- Entity-/Model-Typ
- Seltenheit
- Level
- XP
- Name
- Unique-Daten
- Skin-/Modellinformationen, falls relevant
- aktiver Zustand bzw. eindeutige aktive Instanz

Serverrestart darf keinen Begleiterfortschritt zerstören.

Zielmodell:

```text
Player UUID
 ↓
Companion Ownership
 ↓
Companion Definition
 ↓
Companion Instance
 ↓
Level / XP / Name / Status
```

---

# 33. Exploit-Schutz

Das System muss später mindestens gegen Folgendes getestet werden:

- doppelte Begleiter
- doppelte XP
- XP ohne aktiven Begleiter
- AFK-XP
- mehrere aktive Begleiter
- falscher Besitzer
- Unique-Umbenennung
- Begleiter nach Logout mehrfach aktiv
- Weltwechsel
- Chunkwechsel
- Tod
- Serverrestart
- Entity manuell getötet
- Entity durch andere Plugins verändert

---

# 34. Kampfbegleiter – erst später

Aktive Kampfbegleiter kommen erst nach dem stabilen passiven Fundament.

Spätere Möglichkeiten:

- automatischer Angriff
- Targeting
- Schaden
- Verteidigung
- Spezialfähigkeiten
- Cooldowns
- Rollen
- Aggro-Regeln
- Boss-Interaktion

Die Vanilla-Werte eines Mobs werden nicht einfach 1:1 als PixelRPG-Begleiterwerte übernommen.

```text
Entity-Darstellung
        ≠
Vanilla-Kampfwerte
```

Ein Warden kann wie ein Warden aussehen und trotzdem kontrollierte PixelRPG-Werte besitzen.

---

# 35. Begleiterrollen

Später können Begleiter unterschiedliche Rollen erhalten:

- offensiv
- defensiv
- unterstützend
- Utility
- Erkundung
- schnell
- reitbar
- kosmetisch

Dadurch kann ein Common-Begleiter interessant bleiben, obwohl er niemals die Rohwerte eines Legendary erreicht.

---

# 36. Ungewöhnliche Begleiter sind ausdrücklich erlaubt

Minecraft, Mojang und Paper bieten viele Möglichkeiten.

Beispiele:

```text
Mini Warden
Mini Zombie
Riesige Biene
Mini-Skelett
Spieler-Mannequin
fliegender Begleiter
aquatischer Begleiter
ungewöhnlicher Sonder-/April-Fools-Mob
```

Das System soll solche Konzepte nicht durch unnötige Architekturgrenzen verhindern.

Technische Machbarkeit und Balance bleiben die einzigen echten Grenzen.

---

# 37. Quest + Begleiter gemeinsam planen

Questdesign und Begleiterdesign sollen nicht getrennt entstehen.

Beispiel:

```text
Quest
 ↓
Begleiter finden
 ↓
Begleiter retten
 ↓
Begleiter freischalten
 ↓
weitere Quest
 ↓
Begleiter sammelt XP
 ↓
Begleiter entwickelt sich mit dem Spieler
```

So entstehen persönliche Geschichten statt bloßer Sammelobjekte.

---

# 38. Große Questkette – Beispiel

```text
Level 25

„Das Schweigen von Eisenfels“
        ↓
Wache befragen
        ↓
Mine untersuchen
        ↓
Erzproben sammeln
        ↓
Störung untersuchen
        ↓
Gegner besiegen
        ↓
Schmied informieren
        ↓
nach Dorf B reisen
        ↓
Gelehrten aufsuchen
        ↓
alten Hinweis finden
        ↓
Questabschluss
        ↓
XP + Geld + Item
        ↓
Folgequest
```

Die Quest bleibt eine aktive Questinstanz, obwohl sie mehrere Schritte besitzt.

---

# 39. Questdesign-Fragen

Eine gute Quest sollte möglichst beantworten:

```text
Warum?
Wohin?
Zu wem?
Was passiert dort?
Warum mache ich das?
```

Nicht nur:

```text
Wo sind die Koordinaten?
```

Das ist ein zentraler Designgrundsatz von PixelRPG.

---

# 40. Entwicklungsreihenfolge

## Phase A – Quest-Grundstruktur

- Quest-NPCs bereinigen
- Levelbereiche sauber darstellen
- 5-Quest-Limit erzwingen
- Questdetails verbessern
- Dialoge vereinheitlichen
- Recovery Compass vorbereiten
- Filler-/Reise-NPC vorsehen

## Phase B – Quest-Inhalte

- Level 1–20 vollständig ausarbeiten
- danach 21–40
- danach 41–60
- danach 61–80
- danach 81–99
- Questketten verbinden
- Dörfer einbinden
- Spielerdörfer über Admin-NPCs integrierbar machen

## Phase C – Begleiter-Fundament

- Datenmodell
- Besitz
- Persistenz
- Freischaltung
- Rufen
- Wegschicken
- Umbenennen
- Unique-Ausnahme
- Level
- XP
- passive Darstellung
- Größen-/Modellkonzept

## Phase D – Begleiter-Inhalte

- viele geeignete Entity-Typen
- Seltenheiten
- Questbelohnungen
- Drops
- NPC-Freischaltungen
- Events
- besondere Begleiter
- Unique-Begleiter

## Phase E – Kampfbegleiter

Erst nach stabilen Tests:

- Kampf-KI
- Angriff
- Targeting
- Skills
- defensive Rollen
- Boss-Balance
- Performance

---

# 41. Teststrategie

## Questtests

- Quest annehmen
- sechste Quest blockieren
- Quest abschließen
- Level-Freischaltung prüfen
- fünf Level Vorlauf prüfen
- Questketten testen
- NPC-Wechsel testen
- Dorf-zu-Dorf testen
- Recovery Compass testen
- Serverrestart testen

## Begleitertests

- freischalten
- speichern
- laden
- rufen
- wegschicken
- umbenennen
- Unique-Umbenennung blockieren
- XP erhalten
- XP ohne aktiven Begleiter blockieren
- Levelaufstieg
- mehrere Begleiter besitzen
- nur einen aktiv
- Weltwechsel
- Tod
- Logout/Login
- Serverrestart
- Entity-Manipulation

---

# 42. Balance

Alle Zahlen sind zunächst Testwerte.

Wir benötigen reale Tests mit:

```text
Spielerlevel
Begleiterlevel
Seltenheit
Gegnerlevel
Bosslevel
Schaden
Überlebensfähigkeit
XP-Gewinn
```

Die Balance darf nicht allein aus theoretischen Tabellen entstehen.

Ziel:

```text
Spieler + Begleiter
        ↓
spürbarer Vorteil
        ↓
aber keine Verdopplung der Spielerleistung
        ↓
Boss bleibt relevant
```

---

# 43. Datengetriebene Architektur

Das spätere System muss erweiterbar sein.

Ein neuer Begleiter darf nicht verlangen, dass viele Java-Klassen angepasst werden.

Zielmodell:

```text
CompanionDefinition
    ├─ Entity-/Model-Typ
    ├─ Seltenheit
    ├─ Basiswerte
    ├─ Level-Skalierung
    ├─ XP-Kurve
    ├─ Größe / Darstellung
    ├─ Namensregeln
    ├─ Freischaltungsquellen
    └─ Verhalten
```

Ebenso für Quests:

```text
QuestDefinition
    ├─ ID
    ├─ empfohlenes Level
    ├─ Freischaltungslevel
    ├─ Questgeber
    ├─ Schritte
    ├─ Ziele
    ├─ Dialoge
    ├─ Belohnungen
    └─ Folgequests
```

Neue Mobs, neue Questgeber und neue Questketten sollen später möglichst über Daten/Definitionen ergänzt werden können.

---

# 44. Offene Punkte – bewusst noch nicht festlegen

Folgende Werte werden erst nach Tests entschieden:

- exakte Spieler-XP-Kurve
- exakte Begleiter-XP-Kurve
- XP aus Kampf / Quest / Erkundung
- Werte pro Seltenheit
- Dropchancen
- maximale Anzahl besessener Begleiter
- genaue Freischaltungsbedingungen einzelner Mobs
- Reitbarkeit einzelner Begleiter
- Kampffähigkeiten
- konkrete Größen einzelner Begleiter
- Boss-/Begleiter-Balance
- Recovery-Compass-Verhalten bei mehreren Questzielen

Wir bauen diese Punkte nicht vorschnell hart in die Architektur.

---

# 45. Verbindliche Designregeln

1. Spielerprogression: Level 1–99.
2. Level 100+: Grind-/Endgame-Bereich.
3. Maximal 5 aktive Quests.
4. Quest grundsätzlich 5 Level vor dem empfohlenen Level freischalten.
5. Keine Reputation.
6. Keine Titel.
7. Keine sinnlosen Koordinatenquests.
8. Koordinaten sind intern erlaubt, aber Spielerziele brauchen Kontext.
9. Ein generischer Filler-/Reise-NPC darf für Ortsziele verwendet werden.
10. Dörfer dürfen eigene Quest-Hubs besitzen.
11. Spielerdörfer sollen später über Admin-NPCs eingebunden werden können.
12. Recovery Compass als Quest-Navigationshilfe.
13. Begleiter haben eigene Level und XP.
14. Begleiter erhalten XP nur während aktiver Begleitung.
15. Begleiter leveln unabhängig vom Spieler.
16. Begleiter besitzen feste, kontrollierte Werte.
17. Seltenheit beeinflusst Stärke/Verfügbarkeit, nicht automatisch Größe.
18. Keine Architektur auf wenige feste Mobs beschränken.
19. Möglichst viele technisch geeignete Entities der tatsächlichen Zielversion berücksichtigen.
20. Auch vorhandene ungewöhnliche bzw. ursprünglich als April-Fools eingeführte Entities dürfen geprüft werden.
21. Darstellung, Größe und Kampfbalance getrennt behandeln.
22. Passive Begleiter vor Kampfbegleitern.
23. Unique ausschließlich durch Admin.
24. Unique-Name kann fest und nicht veränderbar sein.
25. Normale Begleiter dürfen umbenannt werden.
26. Mehrere Begleiter besitzen, grundsätzlich nur einen aktiv.
27. Begleiter können sinnvolle Questbelohnungen sein.
28. Begleiter dürfen nicht inflationär verteilt werden.
29. Begleiter unterstützen Spieler, entwerten aber keine Gegner/Bosse.
30. Größere Änderungen werden real getestet.

---

# 46. Langfristiges Zielbild

PixelRPG soll sich so anfühlen:

```text
Spieler
  ↓
Person kennenlernen
  ↓
Quest erhalten
  ↓
Welt erkunden
  ↓
Dorf / NPC / Gegner / Beruf
  ↓
Problem lösen
  ↓
Belohnung erhalten
  ↓
Begleiter kennenlernen
  ↓
Begleiter aufbauen
  ↓
mit Begleiter weitere Abenteuer erleben
```

Quests sollen keine GPS-Aufgaben sein.
Begleiter sollen keine bloßen kosmetischen Entity-Spawns sein.

Beide Systeme sollen Teil derselben Welt werden.

Die große Auswahl an Minecraft-Entities ist ausdrücklich gewollt. Wolf, Biene, Zombie, Schwein und Warden sind nur Beispiele. Jede technisch geeignete Entity der tatsächlichen Zielversion soll grundsätzlich als möglicher Begleiter in Betracht gezogen werden – inklusive ungewöhnlicher oder historisch als April-Fools eingeführter Entities, sofern sie noch vorhanden und sicher nutzbar sind.

**Erst Fundament → dann Inhalte → dann Kampfsystem → dann Feintuning.**
