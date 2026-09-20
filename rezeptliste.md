# PixelRPG – Rezeptideen für die 9 Berufe

**Branch:** `test`  
**Regel:** Es werden ausschließlich existierende Minecraft-Vanilla-Items als Input und Output verwendet. Keine erfundenen Gegenstände, Zwischenprodukte oder Namen wie „Stahlklinge“, „Eisenstange“ oder „Brotmehl“.

## Verbindliche Designregeln

- Nur Vanilla-Minecraft-Items.
- Input und Output müssen als reale Minecraft-Items existieren.
- Deutsche Bezeichnungen sind nur Beschreibungen; technisch werden die echten Minecraft-Itemnamen verwendet.
- PixelRPG-Rezepte dürfen von Vanilla-Rezepten abweichen: Mengen und Kombinationen dürfen frei gewählt werden.
- Ein Rezept darf also beispielsweise `IRON_INGOT + STICK → IRON_SWORD` verwenden, obwohl die PixelRPG-Menge nicht der Vanilla-Menge entspricht.
- Keine erfundenen Zwischenprodukte.
- Keine Custom-Items.
- FISCHER und HOLZFÄLLER bleiben passive Berufe und erhalten keine normalen Crafting-Rezepte.

---

# 1. SCHMIED

**Domäne:** Metall, Werkzeuge, Waffen, Rüstung und metallische Utility-Gegenstände.

**Kategorien:** `TOOLS`, `WEAPONS`, `ARMOR`, `MATERIALS`, `UTILITY`, `CRAFTING`

| Kombination | Ergebnis | Kategorie |
|---|---|---|
| IRON_INGOT + STICK | IRON_SHOVEL | TOOLS |
| 2 IRON_INGOT + STICK | IRON_PICKAXE | TOOLS |
| 2 IRON_INGOT + STICK | IRON_AXE | TOOLS |
| IRON_INGOT + STICK | IRON_SWORD | WEAPONS |
| 2 GOLD_INGOT + STICK | GOLDEN_SWORD | WEAPONS |
| DIAMOND + IRON_SWORD | DIAMOND_SWORD | WEAPONS |
| NETHERITE_INGOT + DIAMOND_SWORD | NETHERITE_SWORD | WEAPONS |
| IRON_INGOT + OAK_PLANKS | SHIELD | ARMOR |
| 5 IRON_INGOT | IRON_CHESTPLATE | ARMOR |
| 5 DIAMOND + IRON_CHESTPLATE | DIAMOND_CHESTPLATE | ARMOR |
| NETHERITE_INGOT + DIAMOND_CHESTPLATE | NETHERITE_CHESTPLATE | ARMOR |
| 3 IRON_INGOT | BUCKET | UTILITY |
| 2 IRON_INGOT | SHEARS | TOOLS |
| IRON_INGOT + FLINT | FLINT_AND_STEEL | TOOLS |
| 5 IRON_INGOT | CAULDRON | UTILITY |
| GOLD_INGOT + IRON_INGOT | BELL | UTILITY |
| 3 IRON_BLOCK + 4 IRON_INGOT | ANVIL | CRAFTING |
| 5 IRON_INGOT + CHEST | HOPPER | UTILITY |

---

# 2. GELEHRTER

**Domäne:** Bücher, Papier, Karten, Orientierung, Redstone-Wissensgeräte und arkane Vanilla-Gegenstände.

**Kategorien:** `BOOKS`, `KNOWLEDGE`, `ARCANE`, `UTILITY`

| Kombination | Ergebnis | Kategorie |
|---|---|---|
| SUGAR_CANE | PAPER | BOOKS |
| PAPER + LEATHER | BOOK | BOOKS |
| 3 BOOK | BOOKSHELF | BOOKS |
| IRON_INGOT + REDSTONE | COMPASS | KNOWLEDGE |
| GOLD_INGOT + REDSTONE | CLOCK | KNOWLEDGE |
| PAPER + COMPASS | MAP | KNOWLEDGE |
| COMPASS + ECHO_SHARD | RECOVERY_COMPASS | ARCANE |
| ENDER_PEARL + BLAZE_POWDER | ENDER_EYE | ARCANE |
| ENDER_EYE + OBSIDIAN | ENDER_CHEST | ARCANE |
| BOOK + DIAMOND + OBSIDIAN | ENCHANTING_TABLE | ARCANE |
| GLASS + NETHER_STAR + OBSIDIAN | BEACON | ARCANE |
| GLASS + GHAST_TEAR + ENDER_EYE | END_CRYSTAL | ARCANE |
| GLOWSTONE + CRYING_OBSIDIAN | RESPAWN_ANCHOR | ARCANE |
| PAPER + GUNPOWDER | FIREWORK_ROCKET | UTILITY |
| REDSTONE_TORCH + QUARTZ + STONE | REDSTONE_COMPARATOR | UTILITY |
| REDSTONE_TORCH + REDSTONE + STONE | REPEATER | UTILITY |
| REDSTONE + QUARTZ + COBBLESTONE | OBSERVER | UTILITY |

---

# 3. LANDWIRT

**Domäne:** Feldfrüchte, Pflanzen, Samen und landwirtschaftliche Rohstoffe. Fertige Mahlzeiten gehören grundsätzlich zum KOCH.

**Kategorien:** `CROPS`, `PROCESSING`, `PRODUCE`

| Kombination | Ergebnis | Kategorie |
|---|---|---|
| WHEAT | WHEAT_SEEDS | CROPS |
| PUMPKIN | PUMPKIN_SEEDS | CROPS |
| MELON | MELON_SEEDS | CROPS |
| BEETROOT | BEETROOT_SEEDS | CROPS |
| SUGAR_CANE | SUGAR | PROCESSING |
| 9 WHEAT | HAY_BLOCK | PRODUCE |
| BONE | BONE_MEAL | PROCESSING |
| DANDELION | YELLOW_DYE | PRODUCE |
| POPPY | RED_DYE | PRODUCE |
| BLUE_ORCHID | LIGHT_BLUE_DYE | PRODUCE |
| CACTUS | GREEN_DYE | PRODUCE |
| KELP | DRIED_KELP | PROCESSING |
| 9 DRIED_KELP | DRIED_KELP_BLOCK | PRODUCE |
| 4 SWEET_BERRIES | SWEET_BERRIES | PRODUCE |
| GLOW_BERRIES | GLOW_BERRIES | PRODUCE |
| COCOA_BEANS | BROWN_DYE | PRODUCE |
| HONEYCOMB | HONEYCOMB_BLOCK | PRODUCE |

---

# 4. KOCH

**Domäne:** fertige Nahrung, gekochte Nahrung, Suppen, Eintöpfe und hochwertige Food-Items.

**Kategorien:** `BASIC_FOOD`, `COOKED_FOOD`, `MEALS`, `SPECIALTIES`

| Kombination | Ergebnis | Kategorie |
|---|---|---|
| WHEAT + WHEAT | BREAD | BASIC_FOOD |
| BEEF | COOKED_BEEF | COOKED_FOOD |
| PORKCHOP | COOKED_PORKCHOP | COOKED_FOOD |
| CHICKEN | COOKED_CHICKEN | COOKED_FOOD |
| RABBIT | COOKED_RABBIT | COOKED_FOOD |
| COD | COOKED_COD | COOKED_FOOD |
| SALMON | COOKED_SALMON | COOKED_FOOD |
| POTATO | BAKED_POTATO | COOKED_FOOD |
| RED_MUSHROOM + BROWN_MUSHROOM + BOWL | MUSHROOM_STEW | MEALS |
| COOKED_RABBIT + CARROT + BAKED_POTATO + BROWN_MUSHROOM + BOWL | RABBIT_STEW | MEALS |
| BEETROOT + BOWL | BEETROOT_SOUP | MEALS |
| BOWL + RED_MUSHROOM + FLOWER | SUSPICIOUS_STEW | MEALS |
| PUMPKIN + SUGAR + EGG | PUMPKIN_PIE | MEALS |
| WHEAT + SUGAR + MILK_BUCKET + EGG | CAKE | SPECIALTIES |
| WHEAT + COCOA_BEANS | COOKIE | BASIC_FOOD |
| CARROT + GOLD_NUGGET | GOLDEN_CARROT | SPECIALTIES |
| APPLE + GOLD_INGOT | GOLDEN_APPLE | SPECIALTIES |
| GOLDEN_APPLE + GOLD_BLOCK | ENCHANTED_GOLDEN_APPLE | SPECIALTIES |
| MELON | MELON_SLICE | BASIC_FOOD |
| MELON_SLICE + GOLD_NUGGET | GLISTERING_MELON_SLICE | SPECIALTIES |
| GLASS_BOTTLE + HONEYCOMB | HONEY_BOTTLE | SPECIALTIES |

---

# 5. SCHNEIDER

**Domäne:** Wolle, Fäden, Leder, Betten, Banner und textile/dekorative Vanilla-Items.

**Kategorien:** `TEXTILES`, `LEATHER`, `DECORATION`, `CRAFTING`

| Kombination | Ergebnis | Kategorie |
|---|---|---|
| 4 STRING | WHITE_WOOL | TEXTILES |
| WHITE_WOOL | 4 STRING | TEXTILES |
| 2 WHITE_WOOL | WHITE_CARPET | TEXTILES |
| 6 WHITE_WOOL + STICK | WHITE_BANNER | DECORATION |
| 3 WHITE_WOOL + 3 OAK_PLANKS | WHITE_BED | CRAFTING |
| WHITE_WOOL + STICK | PAINTING | DECORATION |
| RABBIT_HIDE | LEATHER | LEATHER |
| LEATHER | LEATHER_HELMET | LEATHER |
| LEATHER | LEATHER_CHESTPLATE | LEATHER |
| LEATHER | LEATHER_LEGGINGS | LEATHER |
| LEATHER | LEATHER_BOOTS | LEATHER |
| LEATHER + STRING | BUNDLE | LEATHER |
| LEATHER | LEATHER_HORSE_ARMOR | LEATHER |
| WHITE_BANNER + SHIELD | SHIELD | DECORATION |
| WHITE_BANNER + DYE | WHITE_BANNER | DECORATION |

---

# 6. ALCHEMIST

**Domäne:** Tränke, alchemistische Zutaten und Trank-Modifikationen.

**Kategorien:** `POTIONS`, `PROCESSING`, `SPECIAL_MATERIALS`

| Kombination | Ergebnis | Kategorie |
|---|---|---|
| GLASS_BOTTLE | POTION | POTIONS |
| SPIDER_EYE + SUGAR + BROWN_MUSHROOM | FERMENTED_SPIDER_EYE | PROCESSING |
| BLAZE_ROD | BLAZE_POWDER | PROCESSING |
| MAGMA_CREAM | MAGMA_CREAM | PROCESSING |
| MELON_SLICE + GOLD_NUGGET | GLISTERING_MELON_SLICE | PROCESSING |
| POTION + NETHER_WART + SPIDER_EYE | POTION | POTIONS |
| POTION + NETHER_WART + GLISTERING_MELON_SLICE | POTION | POTIONS |
| POTION + NETHER_WART + PUFFERFISH | POTION | POTIONS |
| POTION + NETHER_WART + MAGMA_CREAM | POTION | POTIONS |
| POTION + NETHER_WART + SUGAR | POTION | POTIONS |
| POTION + NETHER_WART + BLAZE_POWDER | POTION | POTIONS |
| POTION + NETHER_WART + GHAST_TEAR | POTION | POTIONS |
| POTION + NETHER_WART + GOLDEN_CARROT | POTION | POTIONS |
| POTION + NETHER_WART + RABBIT_FOOT | POTION | POTIONS |
| POTION + NETHER_WART + PHANTOM_MEMBRANE | POTION | POTIONS |
| POTION + NETHER_WART + TURTLE_SCUTE | POTION | POTIONS |
| POTION + FERMENTED_SPIDER_EYE | POTION | POTIONS |
| POTION + REDSTONE | POTION | POTIONS |
| POTION + GLOWSTONE_DUST | POTION | POTIONS |
| POTION + GUNPOWDER | SPLASH_POTION | POTIONS |
| SPLASH_POTION + DRAGON_BREATH | LINGERING_POTION | POTIONS |

> Die konkrete Potion-Art wird bei der späteren Implementierung über die vorhandene Potion-Schema-Logik festgelegt. `POTION` ist hier kein erfundenes Item, sondern der Vanilla-Itemtyp als Platzhalter für die jeweilige Trankvariante.

---

# 7. STEINMETZ

**Domäne:** Stein, Deepslate, Tuff, Ziegel, Quartz, Blackstone und geologische Baublöcke.

**Kategorien:** `STONE_BLOCKS`, `STAIRS_SLABS`, `CHISELED`, `SPECIAL_MATERIALS`, `DECORATION`

| Kombination | Ergebnis | Kategorie |
|---|---|---|
| COBBLESTONE | STONE | STONE_BLOCKS |
| 4 STONE | STONE_BRICKS | STONE_BLOCKS |
| COBBLED_DEEPSLATE | DEEPSLATE | STONE_BLOCKS |
| 4 BRICK | BRICKS | STONE_BLOCKS |
| NETHER_BRICK | NETHER_BRICKS | STONE_BLOCKS |
| BLACKSTONE | POLISHED_BLACKSTONE | STONE_BLOCKS |
| BASALT | POLISHED_BASALT | STONE_BLOCKS |
| QUARTZ | QUARTZ_BLOCK | SPECIAL_MATERIALS |
| STONE | STONE_SLAB | STAIRS_SLABS |
| STONE | STONE_STAIRS | STAIRS_SLABS |
| STONE | STONE_WALL | STONE_BLOCKS |
| STONE_BRICKS | STONE_BRICK_SLAB | STAIRS_SLABS |
| STONE_BRICKS | STONE_BRICK_STAIRS | STAIRS_SLABS |
| STONE_BRICKS | STONE_BRICK_WALL | STONE_BLOCKS |
| STONE_BRICK_SLAB + STONE_BRICK_SLAB | CHISELED_STONE_BRICKS | CHISELED |
| DEEPSLATE | CHISELED_DEEPSLATE | CHISELED |
| TUFF | CHISELED_TUFF | CHISELED |
| GRANITE | POLISHED_GRANITE | STONE_BLOCKS |
| DIORITE | POLISHED_DIORITE | STONE_BLOCKS |
| ANDESITE | POLISHED_ANDESITE | STONE_BLOCKS |
| QUARTZ_BLOCK | QUARTZ_PILLAR | SPECIAL_MATERIALS |
| BLACKSTONE + NETHER_BRICK | POLISHED_BLACKSTONE_BRICKS | SPECIAL_MATERIALS |
| END_STONE | END_STONE_BRICKS | SPECIAL_MATERIALS |
| PRISMARINE | PRISMARINE_BRICKS | SPECIAL_MATERIALS |

---

# 8. FISCHER

**Status:** passiv – keine normalen Crafting-Rezepte.

Der Beruf erzeugt ausschließlich Vanilla-Items als Gameplay-Beute:

- COD
- SALMON
- PUFFERFISH
- TROPICAL_FISH
- NAUTILUS_SHELL
- BOW
- FISHING_ROD
- SADDLE
- ENCHANTED_BOOK

Diese Items können als Inputs für andere Berufe dienen.

---

# 9. HOLZFÄLLER

**Status:** passiv – keine normalen Crafting-Rezepte.

Der Beruf erzeugt ausschließlich Vanilla-Items als Rohstoffe:

- OAK_LOG
- SPRUCE_LOG
- BIRCH_LOG
- JUNGLE_LOG
- ACACIA_LOG
- DARK_OAK_LOG
- MANGROVE_LOG
- CHERRY_LOG
- BAMBOO
- STICK
- STRIPPED_OAK_LOG
- STRIPPED_SPRUCE_LOG
- STRIPPED_BIRCH_LOG
- STRIPPED_JUNGLE_LOG
- STRIPPED_ACACIA_LOG
- STRIPPED_DARK_OAK_LOG
- STRIPPED_MANGROVE_LOG
- STRIPPED_CHERRY_LOG
- OAK_PLANKS
- SPRUCE_PLANKS
- BIRCH_PLANKS
- JUNGLE_PLANKS
- ACACIA_PLANKS
- DARK_OAK_PLANKS
- MANGROVE_PLANKS
- CHERRY_PLANKS

Diese Items können als Inputs für andere Berufe dienen.

---

# 10. CROSS-PROFESSION-KETTEN

Nur mit echten Vanilla-Items:

1. `FARMER: WHEAT → COOK: BREAD`
2. `FARMER: SUGAR_CANE → SCHOLAR: PAPER → BOOK → BOOKSHELF`
3. `FISHERMAN: PUFFERFISH → ALCHEMIST: WATER_BREATHING_POTION`
4. `WOODCUTTER: OAK_PLANKS + BLACKSMITH: IRON_INGOT → SHIELD`
5. `WOODCUTTER: OAK_PLANKS + TAILOR: WHITE_WOOL → WHITE_BED`
6. `FARMER: SUGAR_CANE → SUGAR → ALCHEMIST: SWIFTNESS_POTION`
7. `COOK: GOLDEN_CARROT → ALCHEMIST: NIGHT_VISION_POTION`
8. `BLACKSMITH: IRON_INGOT + REDSTONE → COMPASS → SCHOLAR: MAP`
9. `MASON: OBSIDIAN + SCHOLAR: BOOK + BLACKSMITH: DIAMOND → ENCHANTING_TABLE`
10. `FISHERMAN: PUFFERFISH → COOK/ALCHEMIST`

---

# 11. AUSDRÜCKLICH VERBOTEN

Folgende Arten von Einträgen gehören **nicht** in diese Datei und dürfen später auch nicht in `crafting-recipes.json` auftauchen:

- Stahlklinge
- Eisenstange
- Brotmehl
- Metallplatte
- Reparaturmaterial
- Schildrahmen
- Eisenwerkzeug
- Kettenrüstung
- Brotgrundlage
- Schreibmaterial
- Bibliothekskern
- Fernorientierung
- Erfahrung als erfundenes Zwischenprodukt
- oder irgendein anderer Name, der kein echtes Minecraft-Vanilla-Item bezeichnet

Auch eine bloße Beschreibung darf nicht als neues Item missverstanden werden: Ein Rezept hat immer ein reales Vanilla-Item als Output.

---

# 12. ZIEL

Das Rezeptsystem bleibt ein freies PixelRPG-Kombinationssystem:

- Vanilla-Items als vollständiger Item-Pool.
- Freie Mengen.
- Freie Kombinationen.
- Berufsspezifische Zuständigkeiten.
- Cross-Profession-Abhängigkeiten.
- Keine Custom-Items.
- Keine erfundenen Zwischenprodukte.

Diese Datei ist weiterhin eine Designliste und verändert `crafting-recipes.json` nicht.
