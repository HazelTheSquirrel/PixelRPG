# PixelRPG Berufssystem

## Ziel

Die neun Berufe sind keine voneinander isolierten Minispiele. Jeder Spieler kann alle neun Berufe lernen und jeden Beruf von Level 1 bis 100 entwickeln.

Die Berufe sind bewusst asymmetrisch: Sammelberufe liefern Rohstoffe, Verarbeitungsberufe veredeln sie und Herstellungsberufe machen daraus Ausrüstung, Verbrauchsgüter oder Infrastruktur.

## Berufe

| Beruf | Kernrolle | Primäre Aktivität | Hauptabnehmer |
|---|---|---|---|
| Schmied | Metall, Werkzeuge, Waffen, Rüstung | Bergbau, Amboss, Metallverarbeitung | Holzfäller, Gelehrter, Steinmetz |
| Gelehrter | Bücher, Karten, Verzauberungen, Wissen | Bücher, Kartografie, Verzaubern | Schmied, Schneider, alle Endgame-Systeme |
| Landwirt | Pflanzen, Feldfrüchte, Honig, Saatgut | Landwirtschaft | Koch, Alchemist, Fischer |
| Koch | Nahrung und Mahlzeiten | Kochen, Tierdrops, Landwirtschaft | Spieler, Gruppen, Endgame |
| Schneider | Leder, Wolle, Stoffe, leichte Ausrüstung | Tierhaltung, Textilverarbeitung | Gelehrter, Spieler |
| Alchemist | Tränke, Elixiere, Reagenzien | Pflanzen, Brauen, seltene Zutaten | Koch, Gelehrter, Kampf |
| Steinmetz | Stein, Baublöcke, Architektur | Steinabbau und Verarbeitung | Schmied, Spieler, Bau-System |
| Fischer | Fisch, Meeresmaterialien, seltene Fänge | Angeln | Koch, Alchemist, Steinmetz, Gelehrter |
| Holzfäller | Holz, Bretter, Holzprodukte | Holzfällen und Holzverarbeitung | Schmied, Fischer, Gelehrter, Bau-System |

## Progression

Berufslevel und Rezeptanzahl sind bewusst voneinander entkoppelt. Ein Beruf kann jederzeit neue Rezepte erhalten, vorhandene Rezepte verlieren oder zusätzliche Zwischenstufen bekommen, ohne dass die Berufsprogression angepasst werden muss.

Rezepte werden über ihre eigene `requiredProfessionLevel`-Angabe in die Progression eingeordnet. Es gibt keine feste Mindest- oder Höchstanzahl an Rezepten pro Beruf.

Raritäten steigen grundsätzlich mit der Wertigkeit der Produktkette. `Unique` bleibt außerhalb des normalen Craftings und ist für besondere Drops, Bossbelohnungen und einzigartige Inhalte reserviert.

## Produktionsketten

### Landwirt → Koch

Landwirt liefert Weizen, Karotten, Kartoffeln, Kürbisse, Melonen, Äpfel und Honig. Der Koch verarbeitet diese zusammen mit Tier- und Fisch-Drops zu Nahrung.

### Landwirt → Alchemist

Melonen, Karotten, Zucker, Honig und weitere Pflanzen bilden einen Teil der alchemistischen Reagenzien.

### Holzfäller → Infrastruktur

Holz wird nicht als wertloser Zwischenstoff behandelt. Bretter, Kisten, Fässer, Werkbänke, Leitern, Türen, Schilder und Boote bilden die sichtbare Produktionslinie des Holzfällers.

### Holzfäller → andere Berufe

Fischkisten und verstärkte Lagerobjekte benötigen Holz. Der Schmied liefert Metallkomponenten, wenn Holzprodukte höherwertig werden.

### Fischer → Koch

Kabeljau, Lachs und Tropenfisch werden zu Mahlzeiten und höherwertigen Gerichten weiterverarbeitet.

### Fischer → Alchemist

Kugelfisch und seltene Meeresfänge werden als alchemistische Reagenzien eingesetzt.

### Fischer → Steinmetz

Seltene Meeresmaterialien können in hochwertigen Prismarin- und Monumentprodukten des Steinmetzes landen.

### Schmied → Holzfäller / Steinmetz

Metallkomponenten und Werkzeuge sind für höherwertige Holz- und Steinprodukte erforderlich.

### Gelehrter → Schmied / Schneider

Verzauberungsbücher, Kodizes und Archive bilden die Endgame-Verbindung zu Ausrüstung und Spezialgegenständen.

### Alchemist → Gelehrter / Koch

Hochwertige Elixiere können als Zwischenprodukt für Wissensgegenstände und besondere Mahlzeiten dienen.

## Rezeptregeln

1. Jedes Rezept gehört genau einem Beruf.
2. Ein Rezept darf Vanilla-Materialien und bereits gefertigte PixelRPG-Gegenstände kombinieren.
3. Cross-Profession-Abhängigkeiten sind ausdrücklich erlaubt und erwünscht, wenn sie eine echte Wirtschaftskette erzeugen.
4. Rezept-IDs und Item-IDs verwenden eine einheitliche `pixelrpg:profession:recipe`-Notation.
5. Das Vanilla-Crafting-System bleibt unangetastet. PixelRPG-Berufsrezepte werden ausschließlich über das PixelRPG-Crafting-System hergestellt.
6. Ein Spieler muss den Beruf gelernt haben, das erforderliche Berufslevel besitzen und das Rezept freigeschaltet haben.
7. Rezeptfreischaltungen können über Gold oder Quests erfolgen.
8. Crafting vergibt Berufs-XP und damit echte Progression.

## G / Quick Actions

Der Einstieg über `minecraft:quick_actions` bleibt unverändert. Die Berufsseite liest die neun Werte aus `Profession.values()` und zeigt dieselben Rezeptdaten wie die Berufstrainer-NPCs.

Damit existiert nur eine fachliche Quelle:

`G → Berufe → Beruf → Rezepte → Rezeptdetail → Materialien → Herstellung`

Der Berufstrainer verwendet dieselbe Kette. Es gibt keine zweite Rezeptlogik für NPCs.

## Inhaltliche Leitlinie

Die Rezepte sollen nicht bloß zufällige Vanilla-Items über die Berufslevel verteilen. Jeder Schritt sollte entweder:

- einen neuen Rohstoff erschließen,
- einen bestehenden Rohstoff veredeln,
- einen anderen Beruf einbinden,
- eine neue Produktklasse eröffnen oder
- einen klaren Endgame-Meilenstein darstellen.

Die Rezeptmenge pro Beruf ist dabei flexibel. Neue Inhalte dürfen jederzeit ergänzt werden, ohne eine künstliche feste Rezeptanzahl einhalten zu müssen.
