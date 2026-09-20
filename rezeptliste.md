# PixelRPG – Rezeptideen für die 9 Berufe

Stand: 20.09.2026  
Branch: `test`  
Status: **Konzept / Vorschlagsliste – keine dieser Ideen wird automatisch implementiert**

## Zweck

Diese Datei sammelt mögliche PixelRPG-Crafting-Rezepte für alle neun Berufe. Sie dient ausschließlich als Designgrundlage.

Wichtig: PixelRPG ist **kein Vanilla-Crafting-Table-Klon**. Ein Rezept darf deshalb bewusst eine Kombination verwenden, die im normalen Minecraft-Crafting nicht existiert. Entscheidend ist:

- Die beteiligten Items passen thematisch zum Beruf.
- Die Kombination ergibt spielerisch Sinn.
- Berufe können voneinander abhängig werden.
- Mengen dürfen frei gewählt werden.
- Ein Rezept kann ein Zwischenprodukt eines anderen Berufs als Eingang verwenden.
- Vanilla-Items bleiben die Grundlage; neue Custom-Items werden hier bewusst nicht vorgeschlagen.
- Die beiden passiven Berufe FISCHERMAN und WOODCUTTER erhalten hier keine normalen Crafting-Rezepte.

---

# 1. SCHMIED (BLACKSMITH)

**Domäne:** Metall, Werkzeuge, Waffen, Rüstung, Metallverarbeitung, technische Gegenstände.

**Kategorien:**
- `TOOLS`
- `WEAPONS`
- `ARMOR`
- `MATERIALS`
- `UTILITY`
- `CRAFTING`

### Früh – Metallverarbeitung und Werkzeuge

| Idee | Kombination | Kategorie | Gedanke |
|---|---|---|---|
| Eisenstange | 2 IRON_INGOT → 4 IRON_NUGGET | MATERIALS | Metallzerlegung als Schmiedearbeit |
| Eisenwerkzeug | IRON_INGOT + STICK → IRON_SHOVEL | TOOLS | bewusst keine Vanilla-Menge nötig |
| Verstärkte Spitzhacke | 2 IRON_INGOT + STICK → IRON_PICKAXE | TOOLS | alternative PixelRPG-Kombination |
| Kupferwerkzeug | COPPER_INGOT + STICK → COPPER_PICKAXE | TOOLS | Kupfer als eigene Metallstufe |
| Metallplatte | 4 IRON_INGOT → IRON_BLOCK | MATERIALS | Rohmetall → kompakter Werkstoff |
| Eisenkette | 3 IRON_NUGGET → CHAIN | MATERIALS | Kettenfertigung |
| Schildrahmen | IRON_INGOT + 4 OAK_PLANKS → SHIELD | ARMOR | Holzfäller + Schmied |
| Reparaturmaterial | IRON_NUGGET + IRON_INGOT → IRON_INGOT | MATERIALS | einfache Metallverarbeitung |

### Mittel – Waffen und Rüstung

| Idee | Kombination | Kategorie | Gedanke |
|---|---|---|---|
| Stahlklinge | 3 IRON_INGOT + STICK → IRON_SWORD | WEAPONS | Schmiedekern |
| Verstärkte Axt | 2 IRON_INGOT + STICK → IRON_AXE | TOOLS | Werkzeugbau |
| Kettenrüstung | IRON_INGOT → CHAINMAIL_HELMET | ARMOR | bewusste PixelRPG-Vereinfachung |
| Eisenrüstung | 5 IRON_INGOT → IRON_CHESTPLATE | ARMOR | alternatives Mengenmodell |
| Goldklinge | 2 GOLD_INGOT + STICK → GOLDEN_SWORD | WEAPONS | Edelmetall |
| Diamantklinge | DIAMOND + IRON_SWORD → DIAMOND_SWORD | WEAPONS | Upgrade statt Neubau |
| Diamantspitzhacke | 2 DIAMOND + IRON_PICKAXE → DIAMOND_PICKAXE | TOOLS | Upgradepfad |
| Diamantrüstung | 5 DIAMOND + IRON_CHESTPLATE → DIAMOND_CHESTPLATE | ARMOR | Zwischenstufe verwenden |

### Spät – hochwertige Schmiedearbeit

| Idee | Kombination | Kategorie | Gedanke |
|---|---|---|---|
| Netherite-Klinge | NETHERITE_INGOT + DIAMOND_SWORD → NETHERITE_SWORD | WEAPONS | klassisches Upgrade |
| Netherite-Spitzhacke | NETHERITE_INGOT + DIAMOND_PICKAXE → NETHERITE_PICKAXE | TOOLS | klassisches Upgrade |
| Netherite-Rüstung | NETHERITE_INGOT + DIAMOND_CHESTPLATE → NETHERITE_CHESTPLATE | ARMOR | hochwertiges Upgrade |
| Amboss | 3 IRON_BLOCK + 4 IRON_INGOT → ANVIL | CRAFTING | Meisterstück |
| Trichter | 5 IRON_INGOT + CHEST → HOPPER | UTILITY | Schmied + Holzfäller |
| Eimer | 3 IRON_INGOT → BUCKET | UTILITY | Metallbehälter |
| Schere | 2 IRON_INGOT → SHEARS | TOOLS | Metallwerkzeug |
| Feuerzeug | IRON_INGOT + FLINT → FLINT_AND_STEEL | TOOLS | Metall + Feuerstein |
| Glocke | GOLD_INGOT + IRON_INGOT → BELL | UTILITY | hochwertige Metallkomponente |
| Kessel | 5 IRON_INGOT → CAULDRON | UTILITY | Metallbehälter |

### Bewusst interessante PixelRPG-Kombinationen

- DIAMOND + IRON_INGOT + STICK → DIAMOND_SWORD
- NETHERITE_INGOT + DIAMOND_SWORD → NETHERITE_SWORD
- IRON_INGOT + OAK_PLANKS → SHIELD
- IRON_INGOT + CHEST → HOPPER
- GOLD_INGOT + IRON_INGOT → BELL

---

# 2. GELEHRTER (SCHOLAR)

**Domäne:** Bücher, Papier, Wissen, Orientierung, magische/arkane Gegenstände, Redstone-nahe Wissensgeräte.

**Kategorien:**
- `BOOKS`
- `KNOWLEDGE`
- `ARCANE`
- `UTILITY`

### Bücher und Schriften

| Idee | Kombination | Kategorie |
|---|---|---|
| Papier | 3 SUGAR_CANE → PAPER | BOOKS |
| Buch | PAPER + LEATHER → BOOK | BOOKS |
| Bücherstapel | 3 BOOK → BOOKSHELF | BOOKS |
| Schreibmaterial | FEATHER + INK_SAC + PAPER → BOOK | BOOKS |
| Kartensammlung | PAPER + COMPASS → MAP | KNOWLEDGE |
| Leere Karte | PAPER + COMPASS → MAP | KNOWLEDGE |
| Beschriftetes Wissen | BOOK + INK_SAC + FEATHER → BOOK | BOOKS |
| Bibliothekskern | BOOKSHELF + LECTERN → LECTERN | BOOKS |

### Orientierung und Utility

| Idee | Kombination | Kategorie |
|---|---|---|
| Kompass | IRON_INGOT + REDSTONE → COMPASS | KNOWLEDGE |
| Uhr | GOLD_INGOT + REDSTONE → CLOCK | KNOWLEDGE |
| Karte | PAPER + COMPASS → MAP | KNOWLEDGE |
| Fernorientierung | MAP + COMPASS → MAP | KNOWLEDGE |
| Recovery-Kompass | COMPASS + ECHO_SHARD → RECOVERY_COMPASS | ARCANE |
| Ender-Auge | ENDER_PEARL + BLAZE_POWDER → ENDER_EYE | ARCANE |
| Endertruhe | ENDER_EYE + OBSIDIAN → ENDER_CHEST | ARCANE |
| Verzauberungstisch | BOOK + DIAMOND + OBSIDIAN → ENCHANTING_TABLE | ARCANE |
| Erfahrung | GLASS_BOTTLE + EXPERIENCE_BOTTLE → EXPERIENCE_BOTTLE | ARCANE |

### Spät – Wissen und seltene Gegenstände

| Idee | Kombination | Kategorie |
|---|---|---|
| Beacon | GLASS + NETHER_STAR + OBSIDIAN → BEACON | ARCANE |
| Endkristall | GLASS + GHAST_TEAR + EYE_OF_ENDER → END_CRYSTAL | ARCANE |
| Respawn-Anker | GLOWSTONE + CRYING_OBSIDIAN → RESPAWN_ANCHOR | ARCANE |
| Feuerwerksstern | GUNPOWDER + DYE → FIREWORK_STAR | ARCANE |
| Feuerwerksrakete | PAPER + GUNPOWDER + FIREWORK_STAR → FIREWORK_ROCKET | UTILITY |
| Beobachterwissen | REDSTONE + QUARTZ + COBBLESTONE → OBSERVER | UTILITY |
| Redstone-Komparator | REDSTONE_TORCH + QUARTZ + STONE → REDSTONE_COMPARATOR | UTILITY |
| Redstone-Repeater | REDSTONE_TORCH + REDSTONE + STONE → REPEATER | UTILITY |

### Cross-Profession-Ideen

- FARMER liefert SUGAR_CANE → SCHOLAR verarbeitet zu PAPER.
- MASON liefert OBSIDIAN/STONE → SCHOLAR baut arkane Geräte.
- ALCHEMIST liefert EXPERIENCE_BOTTLE bzw. alchemistische Vorprodukte.
- BLACKSMITH liefert IRON_INGOT → COMPASS/CLOCK.

---

# 3. LANDWIRT (FARMER)

**Domäne:** Pflanzen, Feldfrüchte, Samen, Zuckerrohr, Blumen, landwirtschaftliche Rohstoffe und einfache Verarbeitung.

**Kategorien:**
- `CROPS`
- `PROCESSING`
- `PRODUCE`

### Feldfrüchte und Verarbeitung

| Idee | Kombination | Kategorie |
|---|---|---|
| Brotgetreide | 3 WHEAT → WHEAT | CROPS |
| Brotmehl | WHEAT → WHEAT | PROCESSING |
| Zucker | SUGAR_CANE → SUGAR | PROCESSING |
| Kürbiskerne | PUMPKIN → PUMPKIN_SEEDS | CROPS |
| Melonenkerne | MELON → MELON_SEEDS | CROPS |
| Rote-Bete-Samen | BEETROOT → BEETROOT_SEEDS | CROPS |
| Weizensamen | WHEAT → WHEAT_SEEDS | CROPS |
| Karottensaat | CARROT → CARROT | CROPS |
| Kartoffel-Saatgut | POTATO → POTATO | CROPS |
| Kakaoanbau | COCOA_BEANS → COCOA_BEANS | CROPS |

### Pflanzenprodukte

| Idee | Kombination | Kategorie |
|---|---|---|
| Brotgrundlage | WHEAT + WHEAT → BREAD | PRODUCE |
| Kürbisblock | 4 PUMPKIN_SEEDS → PUMPKIN | PRODUCE |
| Heuballen | 9 WHEAT → HAY_BLOCK | PRODUCE |
| Zucker | 3 SUGAR_CANE → SUGAR | PROCESSING |
| Knochenmehl-Verarbeitung | BONE → BONE_MEAL | PROCESSING |
| Farbstoff aus Blume | DANDELION → YELLOW_DYE | PRODUCE |
| Farbstoff aus Blume | POPPY → RED_DYE | PRODUCE |
| Farbstoff aus Blume | BLUE_ORCHID → LIGHT_BLUE_DYE | PRODUCE |
| Farbstoff aus Pflanze | CACTUS → GREEN_DYE | PRODUCE |
| Trockene Pflanze | KELP → DRIED_KELP | PROCESSING |

### Höhere Landwirtschaft

| Idee | Kombination | Kategorie |
|---|---|---|
| Getrockneter Kelpblock | 9 DRIED_KELP → DRIED_KELP_BLOCK | PRODUCE |
| Honigverarbeitung | HONEY_BOTTLE → HONEYCOMB | PRODUCE |
| Wachsverarbeitung | HONEYCOMB → HONEYCOMB_BLOCK | PRODUCE |
| Pilzverarbeitung | RED_MUSHROOM + BROWN_MUSHROOM → MUSHROOM_STEW | PRODUCE |
| Beerenverarbeitung | SWEET_BERRIES → SWEET_BERRIES | PRODUCE |
| Glühbeeren | GLOW_BERRIES → GLOW_BERRIES | PRODUCE |
| Kakaoverarbeitung | COCOA_BEANS → BROWN_DYE | PRODUCE |
| Seetang | KELP → DRIED_KELP | PROCESSING |

**Wichtig:** Farmer soll nicht zum zweiten Koch werden. Fertige Mahlzeiten bleiben primär beim KOCH. Farmer produziert die landwirtschaftlichen Ausgangsstoffe.

---

# 4. KOCH (COOK)

**Domäne:** fertige Nahrung, gekochte Nahrung, Mahlzeiten, hochwertige Food-Kombinationen.

**Kategorien:**
- `BASIC_FOOD`
- `COOKED_FOOD`
- `MEALS`
- `SPECIALTIES`

### Grundnahrung

| Idee | Kombination | Kategorie |
|---|---|---|
| Brot | WHEAT + WHEAT → BREAD | BASIC_FOOD |
| Gebratenes Rind | BEEF → COOKED_BEEF | COOKED_FOOD |
| Gebratenes Schwein | PORKCHOP → COOKED_PORKCHOP | COOKED_FOOD |
| Gebratenes Huhn | CHICKEN → COOKED_CHICKEN | COOKED_FOOD |
| Gebratenes Kaninchen | RABBIT → COOKED_RABBIT | COOKED_FOOD |
| Gebratener Kabeljau | COD → COOKED_COD | COOKED_FOOD |
| Gebratener Lachs | SALMON → COOKED_SALMON | COOKED_FOOD |
| Ofenkartoffel | POTATO → BAKED_POTATO | COOKED_FOOD |

### Mahlzeiten

| Idee | Kombination | Kategorie |
|---|---|---|
| Pilzsuppe | RED_MUSHROOM + BROWN_MUSHROOM + BOWL → MUSHROOM_STEW | MEALS |
| Kanincheneintopf | COOKED_RABBIT + CARROT + BAKED_POTATO + BROWN_MUSHROOM + BOWL → RABBIT_STEW | MEALS |
| Rote-Bete-Suppe | BEETROOT + BOWL → BEETROOT_SOUP | MEALS |
| Suspicious Stew | BOWL + RED_MUSHROOM + FLOWER → SUSPICIOUS_STEW | MEALS |
| Kürbiskuchen | PUMPKIN + SUGAR + EGG → PUMPKIN_PIE | MEALS |
| Kuchen | WHEAT + SUGAR + MILK_BUCKET + EGG → CAKE | SPECIALTIES |
| Kekse | WHEAT + COCOA_BEANS → COOKIE | BASIC_FOOD |
| Honigflasche | GLASS_BOTTLE + HONEYCOMB → HONEY_BOTTLE | SPECIALTIES |

### Spezialitäten

| Idee | Kombination | Kategorie |
|---|---|---|
| Goldene Karotte | CARROT + GOLD_NUGGET → GOLDEN_CARROT | SPECIALTIES |
| Goldener Apfel | APPLE + GOLD_INGOT → GOLDEN_APPLE | SPECIALTIES |
| Verzauberter goldener Apfel | GOLDEN_APPLE + GOLD_BLOCK → ENCHANTED_GOLDEN_APPLE | SPECIALTIES |
| Melonenstück | MELON → MELON_SLICE | BASIC_FOOD |
| Glitzernde Melone | MELON_SLICE + GOLD_NUGGET → GLISTERING_MELON_SLICE | SPECIALTIES |
| Gebackene Kartoffelmahlzeit | BAKED_POTATO + CARROT → BAKED_POTATO | MEALS |
| Fischmahlzeit | COOKED_COD + BOWL → COOKED_COD | MEALS |
| Lachsmahlzeit | COOKED_SALMON + KELP → COOKED_SALMON | MEALS |

**Cross-Profession:** Farmer liefert Pflanzen/Feldfrüchte, Fisherman Fisch, der Koch macht daraus die fertige Nahrung.

---

# 5. SCHNEIDER (TAILOR)

**Domäne:** Wolle, Fäden, Leder, Textilien, tragbare Ausrüstung, Betten und textile Dekoration.

**Kategorien:**
- `TEXTILES`
- `LEATHER`
- `DECORATION`
- `CRAFTING`

### Faser und Textilien

| Idee | Kombination | Kategorie |
|---|---|---|
| Wolle aus Fäden | 4 STRING → WHITE_WOOL | TEXTILES |
| Faden aus Wolle | WHITE_WOOL → 4 STRING | TEXTILES |
| Teppich | 2 WHITE_WOOL → WHITE_CARPET | TEXTILES |
| Banner | 6 WHITE_WOOL + STICK → WHITE_BANNER | DECORATION |
| Wolle färben | WHITE_WOOL + DYE → gefärbte Wolle | TEXTILES |
| Teppich färben | WHITE_CARPET + DYE → gefärbter Teppich | TEXTILES |
| Bett | 3 WHITE_WOOL + 3 OAK_PLANKS → WHITE_BED | CRAFTING |
| Gemälde | WOOL + STICK → PAINTING | DECORATION |

### Lederverarbeitung

| Idee | Kombination | Kategorie |
|---|---|---|
| Leder | RABBIT_HIDE → LEATHER | LEATHER |
| Lederrüstung | LEATHER → LEATHER_CHESTPLATE | LEATHER |
| Lederstiefel | LEATHER → LEATHER_BOOTS | LEATHER |
| Lederhelm | LEATHER → LEATHER_HELMET | LEATHER |
| Ledergamaschen | LEATHER → LEATHER_LEGGINGS | LEATHER |
| Ledertasche | LEATHER + STRING → BUNDLE | LEATHER |
| Lederpferderüstung | LEATHER → LEATHER_HORSE_ARMOR | LEATHER |
| Buchbinderleder | LEATHER + PAPER → BOOK | LEATHER |

### Fortgeschritten

| Idee | Kombination | Kategorie |
|---|---|---|
| Bundle | RABBIT_HIDE + STRING → BUNDLE | LEATHER |
| Schildmuster | WHITE_BANNER + SHIELD → SHIELD | DECORATION |
| Lederrüstung färben | LEATHER_CHESTPLATE + DYE → LEATHER_CHESTPLATE | LEATHER |
| Farbiges Banner | WHITE_BANNER + DYE → BANNER | DECORATION |
| Kettenverzierung | CHAIN + STRING → CHAIN | TEXTILES |
| Wolle für Bibliothek | STRING + DYE → WHITE_WOOL | TEXTILES |

**Wichtiger Designpunkt:** Der Schneider darf bewusst Umwandlungen besitzen, die Vanilla nicht anbietet. STRING → WOOL und WOOL → STRING sind gute Beispiele für das PixelRPG-Prinzip.

---

# 6. ALCHEMIST (ALCHEMIST)

**Domäne:** Tränke, alchemistische Zwischenprodukte und seltene Wirkstoffe.

**Kategorien:**
- `POTIONS`
- `PROCESSING`
- `SPECIAL_MATERIALS`

### Grundstoffe

| Idee | Kombination | Kategorie |
|---|---|---|
| Wasserflasche | GLASS_BOTTLE → POTION | POTIONS |
| Fermentiertes Auge | SPIDER_EYE + SUGAR + BROWN_MUSHROOM → FERMENTED_SPIDER_EYE | PROCESSING |
| Magmacreme | MAGMA_CREAM → MAGMA_CREAM | PROCESSING |
| Glitzernde Melone | MELON_SLICE + GOLD_NUGGET → GLISTERING_MELON_SLICE | PROCESSING |
| Blaze Powder | BLAZE_ROD → BLAZE_POWDER | PROCESSING |
| Redstone-Pulver | REDSTONE → REDSTONE | PROCESSING |
| Glowstone-Pulver | GLOWSTONE → GLOWSTONE_DUST | PROCESSING |

### Trankfamilien

| Idee | Kombination | Kategorie |
|---|---|---|
| Gift | WATER_POTION + NETHER_WART + SPIDER_EYE → POTION | POTIONS |
| Heilung | WATER_POTION + NETHER_WART + GLISTERING_MELON_SLICE → POTION | POTIONS |
| Wasseratmung | WATER_POTION + NETHER_WART + PUFFERFISH → POTION | POTIONS |
| Feuerresistenz | WATER_POTION + NETHER_WART + MAGMA_CREAM → POTION | POTIONS |
| Schnelligkeit | WATER_POTION + NETHER_WART + SUGAR → POTION | POTIONS |
| Stärke | WATER_POTION + NETHER_WART + BLAZE_POWDER → POTION | POTIONS |
| Regeneration | WATER_POTION + NETHER_WART + GHAST_TEAR → POTION | POTIONS |
| Nachtsicht | WATER_POTION + NETHER_WART + GOLDEN_CARROT → POTION | POTIONS |
| Springen | WATER_POTION + NETHER_WART + RABBIT_FOOT → POTION | POTIONS |
| Langsamer Fall | WATER_POTION + NETHER_WART + PHANTOM_MEMBRANE → POTION | POTIONS |
| Schildkrötenmeister | WATER_POTION + NETHER_WART + TURTLE_SCUTE → POTION | POTIONS |
| Schwäche | WATER_POTION + FERMENTED_SPIDER_EYE → POTION | POTIONS |

### Verstärkung

| Idee | Kombination | Kategorie |
|---|---|---|
| Langer Trank | POTION + REDSTONE → POTION | POTIONS |
| Starker Trank | POTION + GLOWSTONE_DUST → POTION | POTIONS |
| Splash-Trank | POTION + GUNPOWDER → SPLASH_POTION | POTIONS |
| Lingering-Trank | SPLASH_POTION + DRAGON_BREATH → LINGERING_POTION | POTIONS |

**Cross-Profession:** Farmer liefert SUGAR/GOLDEN_CARROT bzw. Pflanzen; Fisherman liefert PUFFERFISH; Koch kann GOLDEN_CARROT erzeugen; Endgame-Items liefern seltene Wirkstoffe.

---

# 7. STEINMETZ (MASON)

**Domäne:** Stein, Fels, Deepslate, Tuff, Ziegel, Quartz, Blackstone und dekorative Baublöcke.

**Kategorien:**
- `STONE_BLOCKS`
- `STAIRS_SLABS`
- `CHISELED`
- `SPECIAL_MATERIALS`
- `DECORATION`

### Grundmaterial

| Idee | Kombination | Kategorie |
|---|---|---|
| Stein | COBBLESTONE → STONE | STONE_BLOCKS |
| Steinziegel | 4 STONE → STONE_BRICKS | STONE_BLOCKS |
| Tiefenschiefer | COBBLED_DEEPSLATE → DEEPSLATE | STONE_BLOCKS |
| Tuffblock | TUFF → TUFF | STONE_BLOCKS |
| Bricks | BRICK + BRICK + BRICK + BRICK → BRICKS | STONE_BLOCKS |
| Netherziegel | NETHER_BRICK → NETHER_BRICKS | STONE_BLOCKS |
| Blackstone | BLACKSTONE → POLISHED_BLACKSTONE | STONE_BLOCKS |
| Basalt | BASALT → POLISHED_BASALT | STONE_BLOCKS |
| Quarzblock | QUARTZ → QUARTZ_BLOCK | SPECIAL_MATERIALS |

### Formen

| Idee | Kombination | Kategorie |
|---|---|---|
| Steinplatte | STONE → STONE_SLAB | STAIRS_SLABS |
| Steintreppe | STONE → STONE_STAIRS | STAIRS_SLABS |
| Steinmauer | STONE → STONE_WALL | STONE_BLOCKS |
| Steinziegelplatte | STONE_BRICKS → STONE_BRICK_SLAB | STAIRS_SLABS |
| Steinziegeltreppe | STONE_BRICKS → STONE_BRICK_STAIRS | STAIRS_SLABS |
| Steinziegelmauer | STONE_BRICKS → STONE_BRICK_WALL | STONE_BLOCKS |
| Gemeißelte Steinziegel | STONE_BRICK_SLAB + STONE_BRICK_SLAB → CHISELED_STONE_BRICKS | CHISELED |
| Gemeißelter Deepslate | DEEPSLATE → CHISELED_DEEPSLATE | CHISELED |
| Gemeißelter Tuff | TUFF → CHISELED_TUFF | CHISELED |

### Hochwertige Bausteine

| Idee | Kombination | Kategorie |
|---|---|---|
| Polierter Granit | GRANITE → POLISHED_GRANITE | STONE_BLOCKS |
| Polierter Diorit | DIORITE → POLISHED_DIORITE | STONE_BLOCKS |
| Polierter Andesit | ANDESITE → POLISHED_ANDESITE | STONE_BLOCKS |
| Quarzsäulen | QUARTZ_BLOCK → QUARTZ_PILLAR | SPECIAL_MATERIALS |
| Blackstone-Ziegel | BLACKSTONE + NETHER_BRICK → POLISHED_BLACKSTONE_BRICKS | SPECIAL_MATERIALS |
| Endsteinziegel | END_STONE → END_STONE_BRICKS | SPECIAL_MATERIALS |
| Prismarinziegel | PRISMARINE → PRISMARINE_BRICKS | SPECIAL_MATERIALS |

**Designregel:** Der Steinmetz soll kein allgemeiner Dekorateur werden. Seine Stärke bleibt mineralische/geologische Baustruktur.

---

# 8. FISCHER (FISHERMAN)

**Status:** **passiver Beruf – keine normalen Crafting-Rezepte vorgesehen.**

**Domäne:**
- Fischfang
- Fangmenge
- Schatzchance
- seltene Angelbeute

Der Fischer erzeugt wertvolle Eingangsitems für andere Berufe.

### Relevante Beute für andere Berufe

| Beute | Weiterverwendung |
|---|---|
| COD | KOCH |
| SALMON | KOCH |
| PUFFERFISH | ALCHEMIST |
| TROPICAL_FISH | KOCH / Spezialrezepte |
| NAUTILUS_SHELL | SCHOLAR / Spezialrezepte |
| BOW | SCHMIED / SCHNEIDER / Utility |
| FISHING_ROD | SCHMIED / Fischer-Gameplay |
| SADDLE | Utility / Handel |
| ENCHANTED_BOOK | SCHOLAR |

**Wichtig:** Kein künstliches „Fischer-Crafting“, nur damit der Beruf Rezepte besitzt. Der Beruf soll über seinen passiven Gameplay-Loop relevant sein.

---

# 9. HOLZFÄLLER (WOODCUTTER)

**Status:** **passiver Beruf – keine normalen Crafting-Rezepte vorgesehen.**

**Domäne:**
- Holzernte
- Baumfällung
- Holzertrag
- Holz als Rohstofflieferant

### Relevante Rohstoffe

| Rohstoff | Weiterverwendung |
|---|---|
| OAK_LOG | viele Berufe |
| SPRUCE_LOG | viele Berufe |
| BIRCH_LOG | viele Berufe |
| JUNGLE_LOG | viele Berufe |
| ACACIA_LOG | viele Berufe |
| DARK_OAK_LOG | viele Berufe |
| MANGROVE_LOG | viele Berufe |
| CHERRY_LOG | viele Berufe |
| BAMBOO | Farmer / Scholar / Mason |
| STICK | Schmied / Schneider |
| STRIPPED_LOG | Baublöcke |
| PLANKS | Schmied / Schneider / Scholar |

Der Holzfäller liefert damit bewusst die Basis für andere Berufe, ohne selbst zu einem neunten aktiven Crafting-Beruf zu werden.

---

# 10. BERUFSÜBERGREIFENDE REZEPTKETTEN

Hier wird das eigentliche PixelRPG-System interessant.

## Kette A – Landwirtschaft → Kochen

`FARMER`

WHEAT → WHEAT

↓

`COOK`

WHEAT → BREAD

---

## Kette B – Landwirtschaft → Gelehrter

`FARMER`

SUGAR_CANE → SUGAR

↓

`SCHOLAR`

SUGAR_CANE → PAPER → BOOK → BOOKSHELF

---

## Kette C – Fischer → Alchemist

`FISHERMAN`

PUFFERFISH

↓

`ALCHEMIST`

PUFFERFISH + NETHER_WART + WATER_POTION → WATER_BREATHING_POTION

---

## Kette D – Holzfäller → Schmied

`WOODCUTTER`

OAK_LOG → OAK_PLANKS

↓

`BLACKSMITH`

IRON_INGOT + OAK_PLANKS → SHIELD

---

## Kette E – Holzfäller → Schneider

`WOODCUTTER`

OAK_LOG → OAK_PLANKS

↓

`TAILOR`

OAK_PLANKS + WHITE_WOOL → WHITE_BED

---

## Kette F – Farmer → Alchemist

`FARMER`

SUGAR_CANE → SUGAR

↓

`ALCHEMIST`

SUGAR + NETHER_WART + WATER_POTION → SWIFTNESS_POTION

---

## Kette G – Koch → Alchemist

`COOK`

CARROT + GOLD_NUGGET → GOLDEN_CARROT

↓

`ALCHEMIST`

GOLDEN_CARROT + NETHER_WART + WATER_POTION → NIGHT_VISION_POTION

---

## Kette H – Schmied → Gelehrter

`BLACKSMITH`

IRON_INGOT + REDSTONE → COMPASS

↓

`SCHOLAR`

COMPASS + PAPER → MAP

---

## Kette I – Steinmetz → Gelehrter

`MASON`

OBSIDIAN

↓

`SCHOLAR`

OBSIDIAN + DIAMOND + BOOK → ENCHANTING_TABLE

---

## Kette J – Fischer → Koch → Alchemist

`FISHERMAN`

PUFFERFISH

↓

`COOK`

Fisch als Nahrungs-/Handelsware

↓

`ALCHEMIST`

PUFFERFISH → Wasseratmung

Damit bekommt derselbe Rohstoff unterschiedliche wirtschaftliche Rollen.

---

# 11. BESONDERS INTERESSANTE NICHT-VANILLA-KOMBINATIONEN

Diese Beispiele zeigen bewusst das gewünschte PixelRPG-Prinzip.

| Beruf | Kombination | Ergebnis | Warum interessant |
|---|---|---|---|
| Schmied | DIAMOND + IRON_SWORD | DIAMOND_SWORD | Upgrade statt Vanilla-Rezept |
| Schmied | NETHERITE_INGOT + DIAMOND_SWORD | NETHERITE_SWORD | mehrstufiges Upgrade |
| Schmied | IRON_INGOT + OAK_PLANKS | SHIELD | Berufe verbinden |
| Schmied | IRON_INGOT + CHEST | HOPPER | Utility statt Standard-Crafting |
| Schneider | 1 WHITE_WOOL → 4 STRING | STRING | bewusst nicht-vannilla |
| Schneider | 4 STRING → WHITE_WOOL | WHITE_WOOL | textile Verarbeitung |
| Schneider | LEATHER + PAPER | BOOK | Buchbinder-Thematik |
| Farmer | KELP → DRIED_KELP | DRIED_KELP | Verarbeitung |
| Koch | COOKED_COD + BOWL | COOKED_COD | absichtlich frei gestaltbare Mengen |
| Gelehrter | COMPASS + PAPER | MAP | Wissensverarbeitung |
| Gelehrter | ENDER_PEARL + BLAZE_POWDER | ENDER_EYE | arkane Verarbeitung |
| Alchemist | POTION + REDSTONE | POTION | Verstärkung |
| Alchemist | POTION + GUNPOWDER | SPLASH_POTION | Formänderung |
| Steinmetz | STONE → STONE_STAIRS | STONE_STAIRS | Materialformung |
| Steinmetz | STONE_BRICK_SLAB + STONE_BRICK_SLAB | CHISELED_STONE_BRICKS | bewusst freie Kombination |

---

# 12. BEWUSST NICHT ZUGEORDNETE BEREICHE

Folgende Dinge sollten nicht beliebig auf mehrere Berufe verteilt werden:

- **Fertige Mahlzeiten** → primär KOCH
- **Tränke** → primär ALCHEMIST
- **Waffen/Rüstung/Werkzeuge aus Metall** → primär SCHMIED
- **Wolle/Leder/Textilien** → primär SCHNEIDER
- **Stein/Deepslate/Tuff/Bricks/Quartz-Baublöcke** → primär STEINMETZ
- **Bücher/Karten/Kompass/Uhr/arkane Utility** → primär GELEHRTER
- **Feldfrüchte/Pflanzen/Samen** → primär LANDWIRT
- **Fischfang/Schatzfunde** → FISCHER, passiv
- **Holzernte** → HOLZFÄLLER, passiv

---

# 13. DESIGNREGELN FÜR DIE SPÄTERE REZEPTAUSWAHL

1. Nicht jedes Vanilla-Rezept muss als PixelRPG-Rezept existieren.
2. Nicht jedes PixelRPG-Rezept muss ein Vanilla-Rezept sein.
3. Ein Beruf soll eine erkennbare Identität besitzen.
4. Zwischenprodukte sollen sinnvoll zwischen Berufen fließen.
5. Passive Berufe sollen nicht künstlich mit Rezepten aufgebläht werden.
6. Hochstufige Rezepte dürfen mehrere vorher erzeugte Items kombinieren.
7. Seltene Items dürfen als Gate für starke Rezepte dienen.
8. Mengen sollen die gewünschte Progression unterstützen und nicht Vanilla-Muster imitieren müssen.
9. Ein Rezept darf bewusst „unmöglich“ im Vanilla-Crafting sein, wenn die PixelRPG-Logik es trägt.
10. Keine Custom-Items notwendig, solange sich ein interessanter Gameplay-Loop mit Vanilla-Materialien aufbauen lässt.
11. Doppelte Berufszuständigkeiten möglichst vermeiden.
12. Cross-Profession-Rezepte sollen Wirtschaft und Progression verbinden.
13. Kategorien sollen die Funktion des Ergebnisses erklären, nicht nur das Vanilla-Crafting-Rezept kopieren.
14. Die endgültigen Rezepte sollten anschließend auf Level 1–100 verteilt werden, statt stumpf 21 Rezepte pro Beruf zu erzwingen.

---

# 14. VORLÄUFIGE ZIELSTRUKTUR

| Beruf | Aktiv | Hauptkategorien | Rolle |
|---|---|---|---|
| SCHMIED | Ja | TOOLS, WEAPONS, ARMOR, MATERIALS, UTILITY, CRAFTING | Metall & Ausrüstung |
| GELEHRTER | Ja | BOOKS, KNOWLEDGE, ARCANE, UTILITY | Wissen & arkane Utility |
| LANDWIRT | Ja | CROPS, PROCESSING, PRODUCE | Rohstoffe & Pflanzen |
| KOCH | Ja | BASIC_FOOD, COOKED_FOOD, MEALS, SPECIALTIES | fertige Nahrung |
| SCHNEIDER | Ja | TEXTILES, LEATHER, DECORATION, CRAFTING | Textilien & Leder |
| ALCHEMIST | Ja | POTIONS, PROCESSING, SPECIAL_MATERIALS | Tränke & Alchemie |
| STEINMETZ | Ja | STONE_BLOCKS, STAIRS_SLABS, CHISELED, SPECIAL_MATERIALS | Stein & Bau |
| FISCHER | Nein | — | passive Fisch-/Schatzmechanik |
| HOLZFÄLLER | Nein | — | passive Holz-/Forstmechanik |

---

## Fazit

Diese Liste ist **bewusst eine Ideensammlung und noch kein endgültiger Rezeptkatalog**.

Der entscheidende Unterschied zum aktuellen Stand soll sein:

**Nicht mehr „21 Vanilla-Rezepte pro Beruf“, sondern ein miteinander verbundenes Verarbeitungssystem.**

Beispiel:

`WOODCUTTER → OAK_LOG → BLACKSMITH → SHIELD → SCHOLAR/QUEST/SHOP`

oder

`FARMER → SUGAR_CANE → SCHOLAR → PAPER → BOOK → ARCANE`

oder

`FISHERMAN → PUFFERFISH → ALCHEMIST → WATER_BREATHING_POTION`

Die nächste Designstufe wäre deshalb, aus dieser Sammlung die **wirklich sinnvollen Rezepte auszuwählen**, Überschneidungen zu entfernen und daraus anschließend einen finalen Rezeptbaum pro Beruf zu bauen.
