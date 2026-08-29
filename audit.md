# 🕵️‍♂️ Forensischer 100-%-Audit-Bericht: PixelRPG

**Repository:** `HazelTheSquirrel/PixelRPG`  
**Branch:** `refactor/central-content-pipeline-v3`  
**Audit-Stand:** 29.08.2026  
**Aktueller Branch-Stand bei Beginn der Begutachtung:** `e8366de33e9aeae80af5f4ad9e0d2d58c6770b59`  
**Historische Referenz:** `9fadeadefba378361722a16fad66be7f62a70560`

> Dieser Bericht dokumentiert den aktuellen Branch-Zustand und trennt historische Befunde vom heutigen Refactor-Stand. Die Begutachtung dient als technische und inhaltliche Grundlage für `lore.md`. Es werden keine neuen Gameplay-Systeme aus der Lore abgeleitet.

---

# 1. Prüfmethodik

Der Repository-Tree des Branches wurde direkt über die Git-Datenstruktur abgefragt. Der Tree wurde vollständig aufgelöst und war laut GitHub nicht abgeschnitten (`truncated=false`). Dadurch wurden Projektwurzel, Build-/CI-Dateien, sämtliche Java-Pakete und Ressourcen als Prüfbestand erfasst.

Die Begutachtung erfolgte anschließend entlang der tatsächlichen Modulgrenzen und ihrer Verbindungen:

- Bootstrap / Plugin-Lifecycle
- API
- Player / Profil / Persistenz
- Core / Level
- Stats
- Combat / Skills / Loot / Scaling
- Items / Equipment
- Quests
- NPC / Verhalten
- Dialogue / Quick Actions
- Professions / Crafting
- Companions
- Boss-System
- Guild / Party
- Economy / Shop / Trade / Travel
- GUI
- Story
- Ressourcen / JSON / YAML
- Build / Paper Plugin Descriptor / CI

Die Prüfung wurde bewusst **nicht als GitHub-DeepSearch** durchgeführt. Code- und Datenbefunde wurden aus dem tatsächlichen Branch-Inhalt und den konkreten Dateien abgeleitet.

---

# 2. Technische Gesamtstruktur

Der aktuelle Branch besitzt eine klar erkennbare modulare RPG-Struktur. Die Plugin-Initialisierung verbindet PlayerProfile, Stats, Professionen, Items, Equipment, Crafting, Quests, Bosse, NPCs, Dialoge, Party, Story, Companion-Service, Scoreboard und weitere Dienste in einem zentralen Lifecycle. `PixelRPGPlugin` registriert diese Komponenten beim Start und besitzt korrespondierende Shutdown-Pfade. fileciteturn337file0L2-L2

Die vorhandenen Module sind keine voneinander unabhängigen Prototypen. Mehrere Systeme greifen über Services und Events ineinander. Daraus ergibt sich bereits ein funktionaler RPG-Kern, dem vor allem eine verbindende Welt-/Lore-Ebene fehlt.

---

# 3. Player / Einstieg / Progression

`PixelRPG.md` definiert ausdrücklich:

- neue Spieler werden zunächst wie Vanilla-Spieler behandelt,
- PixelRPG-Systeme greifen erst nach Registrierung,
- Level/XP bilden die zentrale Progression,
- es gibt keine Klassen,
- es gibt keine frei verteilbaren Player-Attribute.

Damit ist die technische Grundlage für einen **opt-in RPG-Layer** bereits vorhanden. Die Lore muss daher keine künstliche Klassenwahl oder Charaktererstellung erfinden.

Forensisches Urteil: Der technische Registrierungsvorgang sollte narrativ als Aufnahme in eine Organisation bzw. als Anerkennung eines Abenteurers dargestellt werden, nicht als bürokratisches „RPG aktivieren“ im Rathaus.

---

# 4. NPC / Dialogue

Die Plugin-Initialisierung zeigt ein eigenständiges NPC-System mit `NpcManager`, Chunk-Lifecycle, Look-Task, Interaktionslistener und `NpcBehaviorRegistry`. Registriert werden unter anderem Reception, Quest, Shop, Travel, Story, Banker, Filler und vier getrennte Profession-Trainer. fileciteturn337file0L2-L2

Das ist für die Lore besonders relevant: NPCs sind bereits echte Weltschnittstellen. Die Welt muss daher nicht über Commands erklärt werden; die vorhandene Architektur erlaubt, gesellschaftliche Rollen über NPCs/Dialoge abzubilden.

Vorhandene Dialogbereiche umfassen Registrierung/Empfang, Quests, Shop, Travel, Story, Bank, Begleiter, Berufe, Charakterkarte und weitere Funktionen.

Forensisches Urteil: Die Abenteurergilde kann als verbindender Ort dienen, ohne eine neue technische Organisation implementieren zu müssen.

---

# 5. Begleiter – technische und sichtbare Trennung

Die interne Java-Struktur verwendet bewusst `Companion`. Der Branch besitzt zahlreiche miteinander verbundene Companion-Komponenten für Definition, Instanz, Registry, Runtime-Registry, Service, Stats, Progression, Combat-Ausnahmefälle, Equipment, Mounts und Lifecycle.

Die Projektregeln definieren normale Companions als passive, unbesiegbare Runtime-Entitäten mit Utility und passiven RPG-Boni. Sie sind keine persistenten Welt-NPCs.

**Wichtig:** `Companion` bleibt als interne technische Bezeichnung bestehen. Die sichtbare Bezeichnung im Spiel ist dagegen **„Begleiter“**. Eine interne Umbenennung ist weder notwendig noch Teil der Lore-/Language-Bereinigung.

Die Datenquelle `src/main/resources/data/companions.json` ist vorhanden und enthält die Companion-Definitionen. Die technische Struktur ist damit ausreichend ausgeprägt, um Begleiter als Teil der Welt zu erklären, ohne ein neues System zu erfinden.

---

# 6. Berufe / Crafting

Die vier bestehenden Professionen sind:

- `BLACKSMITH`
- `PROVISIONER`
- `ALCHEMIST`
- `SCHOLAR`

Die Plugin-Initialisierung registriert für jeden dieser Berufe einen eigenen `ProfessionTrainerBehavior`. fileciteturn337file0L2-L2

Die Rezeptdaten sind zentral in `crafting-recipes.json` hinterlegt und werden über Registry und Crafting-Service verarbeitet.

Forensischer Befund aus dem Runtime-Test: Die Berufslehrer hatten zuvor eine fehlerhafte Darstellung, bei der die Trainer nicht sauber auf ihren jeweiligen Beruf begrenzt waren. Die Ursache wurde bereits identifiziert und die bestehende Trainer-/Rezeptverknüpfung korrigiert. Ein abschließender Runtime-Nachtest nach dieser Korrektur bleibt erforderlich.

Lore-Folgerung: Die Handwerker können glaubwürdig als Fachleute einer bestehenden Gesellschaft auftreten. Kein neues Berufssystem erforderlich.

---

# 7. Quests

Das Quest-Modul besitzt Repository, Manager, Progress, Navigation, globale Events, passive Checks und Mob-Kill-Verarbeitung. Die Projektregeln definieren Open World, Questketten, echte NPCs/Orte, maximal fünf aktive Quests und einen begrenzten Questtypen-Pool.

Forensisches Urteil: Das vorhandene Quest-System ist bereits geeignet, die Welt zu erzählen. Die Questlogik muss nicht durch ein neues Story-System ersetzt werden.

---

# 8. Items / Equipment

Items besitzen eine eigene PDC-/Metadaten-Identität. Das Vanilla-Material alleine ist ausdrücklich keine PixelRPG-Identität.

Vorhanden sind Item-Definitionen, Item-Service, Builder, Raritäten, Economy-Konfiguration, Soulbound und Unique-Item-Verarbeitung sowie Equipment-Services.

Die definierten Raritäten sind:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Unique

Forensisches Urteil: Die vorhandene Item-Schicht unterstützt die geplante Koexistenz von Vanilla und PixelRPG. Die Lore muss nicht versuchen, Vanilla-Gegenstände umzudeuten.

Bekannter UI-Befund: Im nativen `minecraft:quick_actions`-Charakterprofil wurden englische Stat-Namen und englische Item-Bezeichnungen festgestellt. Das ist ein sichtbarer Deutsch-only-Befund und bleibt als technische Bereinigung offen.

---

# 9. Combat / Stats

Das Projekt besitzt eine zentrale Damage-Logik, Weapon-Abilities, Cooldowns, Mob Scaling, Loot, Boss Damage, RPG-Stats und Soulbound-Death-Schutz.

Aktive Character Stats umfassen HP, Armor, Movement Speed, Reach, Damage, Crit, Crit-Schaden, Lifesteal und Attack Power.

Es gibt keine Mana-Kosten für Weapon-Abilities und keine frei verteilbaren Player-Attribute.

Forensisches Urteil: Die vorhandene Combat-/Stat-Schicht liefert bereits die mechanische Grundlage für einen RPG-Charakter. Eine Lore-Ebene sollte diese Werte erklären, nicht neue Werte hinzufügen.

---

# 10. Bosse

Das Boss-System besitzt:

- Boss Definitions / Registry
- Active Boss
- feste Boss-Level
- Boss Stats
- BossBar
- Attack Patterns
- Damage Contribution
- Death Handling
- datengetriebenen Loot
- optionale Companion-Rewards

Biome-Bosse sind an Minecraft-Biome gebunden. World-Bosse sind als besondere World-Events konzipiert.

Der Branch enthält eigene Boss-Klassen, Pattern-Implementierungen und datengetriebene Reward-Daten. Die technische Trennung zwischen Bossdefinition, Aktivierung, Angriffen, Schaden, Tod und Loot ist damit vorhanden.

Forensisches Urteil: Biome-Bosse können innerhalb der Lore als regionale Bedrohungen und World-Bosse als außergewöhnliche Ereignisse behandelt werden. Dafür ist keine neue Boss-Architektur erforderlich.

---

# 11. Party / Guild

Das Projekt unterscheidet technisch zwischen Party und Guild.

Partys besitzen gemeinsame XP-/Loot-/Quest-Mechaniken, Mitglieder, Einladungen, Leadership und Lifecycle-Handling.

Gilden besitzen persistente Daten, Mitglieder, Gildenmeister, Einladungen, Gildenbank und Gildenbasis/-stadt.

Forensisches Urteil: Diese technische Trennung unterstützt die Lore eindeutig:

- **Party:** kurzfristige Gruppe reisender Abenteurer.
- **Spielergilde:** dauerhafte soziale Gemeinschaft.
- **Abenteurergilde:** kann als weltinterne Organisation und Lore-Ort darüberliegen, ohne mit dem technischen Spielergilden-System identisch zu sein.

---

# 12. Economy / Travel / Story

Economy, Guild Currency, Shop, Trade Depot und Travel besitzen jeweils eigene Module. Story besitzt `StoryManager`, Kapitel und Storybook-Funktionen.

Forensisches Urteil: Das bestehende Projekt besitzt bereits genug wirtschaftliche, soziale und erzählerische Schnittstellen, um eine zusammenhängende Welt zu bilden. Die fehlende Verbindung ist primär konzeptionell, nicht technisch.

---

# 13. Native Quick Actions / Charakterkarte

Das Projekt verwendet die native Minecraft-Quick-Action-Interaktion `minecraft:quick_actions` für die Charakterkarte. Der vorhandene Service bündelt Charakterprofil, aktive Quests, Begleiter, Berufe und Gilde.

Forensischer Befund:

- Funktionalität vorhanden.
- Runtime getestet.
- Englische Stat-Namen noch sichtbar.
- Englische Item-Bezeichnungen noch sichtbar.

Diese beiden Punkte sind reine sichtbare Text-/Darstellungsbefunde und müssen getrennt von der internen `Companion`-Benennung behandelt werden.

---

# 14. Persistence / Database

Die Projektstruktur enthält MySQL- und YAML-PlayerProfile-Repositories, DatabaseManager sowie Persistenzpfade für mehrere RPG-Systeme.

Der vom Entwickler durchgeführte Runtime-Test bestätigte:

- Serverstart erfolgreich.
- Serverlog ohne Fehler.
- Datenbankverknüpfung erfolgreich.
- NPC-Systeme funktionsfähig.
- Chat-Nachrichten korrekt.
- Items, Quests, Begleiter und Dialoge praktisch geprüft.

Ein vollständiger Save/Reload-/Restart-Regressionszyklus für sämtliche Persistenzbereiche ist weiterhin als separater Abschlusscheck zu behandeln.

---

# 15. Build / CI / Plugin-Lifecycle

Der Repository-Tree enthält Build- und GitHub-Actions-Dateien sowie `build.gradle`, `settings.gradle` und `paper-plugin.yml`.

Die Plugininitialisierung verwendet `LifecycleEvents.COMMANDS` für die Command-Registrierung. Die zentrale Pluginklasse registriert zahlreiche Services, Listener und Tasks und besitzt entsprechende Lifecycle-Verantwortung. fileciteturn337file0L2-L2

Forensischer Hinweis: `RootCommand` implementiert derzeit `CommandExecutor`/`TabCompleter` und nutzt Bukkit-Command-Typen, während die Projektvorgaben moderne Paper-Lifecycle-Mechanik verlangen. Das ist ein technischer Architekturpunkt, der bei einem separaten Command-Refactor geprüft werden sollte; er ist **nicht** Bestandteil der Lore-Erstellung.

---

# 16. Sicherheits- und Backdoor-Befunde

Keine unmittelbaren Hinweise auf:

- `Runtime.getRuntime().exec()`
- `ProcessBuilder`
- Reflection-basierte versteckte Codeausführung
- `Unsafe`
- `URLClassLoader`
- verdächtige Bytecode-Downloads
- fest verdrahtete Spieler-UUIDs als Hintertür
- API-Keys oder echte Passwörter im Repository
- versteckte Konsolenbefehlsausführung

Die MySQL-Konfiguration enthält weiterhin den bekannten Platzhalter `CHANGE_ME`; ein echtes Produktionspasswort darf nicht committed werden.

---

# 17. Forensisches Gesamturteil

Der aktuelle Branch besitzt bereits einen erheblichen funktionierenden RPG-Kern.

Die wichtigsten technischen Tatsachen sind:

1. PixelRPG kann als **zusätzliche RPG-Schicht über Vanilla Minecraft** betrieben werden.
2. Spieler starten technisch zunächst als Vanilla-Spieler.
3. Die RPG-Registrierung existiert technisch, muss aber narrativ nicht als Rathaus-Bürokratie präsentiert werden.
4. NPCs und native Dialoge sind bereits stark genug, um den RPG-Einstieg und die Weltvermittlung zu tragen.
5. Quests, Berufe, Items, Begleiter, Bosse, Party, Guild, Economy und Story existieren bereits als getrennte Systeme.
6. `Companion` ist intern eine legitime technische Bezeichnung und bleibt unverändert.
7. Ingame muss der sichtbare Begriff **„Begleiter“** verwendet werden.
8. Biome-Bosse und World-Bosse besitzen bereits eine technische Trennung, die direkt als regionale bzw. weltweite Bedrohung interpretiert werden kann.
9. Die größte Lücke ist nicht ein fehlendes Gameplay-System, sondern die fehlende gemeinsame Weltlogik zwischen den vorhandenen Systemen.

---

# 18. Lore-Grundlage aus der Forensik

Aus dem tatsächlichen Projektbestand ergibt sich folgende belastbare Weltgrundlage:

```text
Vanilla Minecraft-Welt
        ↓
PixelRPG als zusätzliche Gesellschaftsschicht
        ↓
Reisender
        ↓
Abenteurergilde
        ↓
Abenteurer
        ↓
Quests / Berufe / Ausrüstung / Begleiter
        ↓
Expeditionen / Party / Spielergilden
        ↓
Biome-Bedrohungen
        ↓
World-Events / World-Bosse
        ↓
Risse als langfristiges Mysterium
```

Die Lore wurde deshalb bewusst so geschrieben, dass sie die vorhandenen Systeme verbindet, ohne neue Gameplay-Anforderungen zu erzwingen.

---

# 19. Aktuelle offene technische Befunde

- [ ] Quick-Actions-Charakterprofil: englische Stat-Namen auf Deutsch umstellen.
- [ ] Quick-Actions-Charakterprofil: englische Item-Namen auf Deutsch umstellen.
- [ ] Vier Berufslehrer nach der Rezept-/Berufskorrektur erneut vollständig testen.
- [ ] vollständigen Save/Reload-/Restart-Persistenztest durchführen.
- [ ] vollständige Runtime-Regression der noch nicht getesteten Module durchführen.
- [ ] Command-Architektur separat gegen die verbindlichen Paper-26.x-Vorgaben prüfen.

Diese Punkte sind bewusst **nicht** durch Lore gelöst worden.

---

# 20. Dokumentationsbeziehung

- `roadmap.md` = konkrete offene Arbeit und Abarbeitung.
- `audit.md` = forensischer technischer/inhaltsbezogener Befund.
- `lore.md` = erzählerische Grundlage, die den vorhandenen RPG-Kern zusammenhängend erklärt.
- `PixelRPG.md` = technische und funktionale Projektdefinition.

Keine dieser Dateien darf als Vorwand verwendet werden, funktionierende Gameplay-Systeme ohne separate Entscheidung neu zu implementieren.

---

# Abschluss

Die forensische Begutachtung bestätigt: **PixelRPG braucht an diesem Punkt vor allem eine verbindende Identität, nicht noch mehr lose Systeme.**

Die bestehende Architektur bietet bereits die technischen Anker für eine Welt aus Reisenden, Abenteurern, Handwerkern, Begleitern, Quests, regionalen Bossen, außergewöhnlichen World-Events, Parteien und Spielergilden.

Die neue `lore.md` beschreibt genau diese Verbindung und lässt die technische Implementierung unangetastet.
