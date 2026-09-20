# PixelRPG – Rezeptkatalog der Berufe

**Branch:** `test`  
**Quelle:** `src/main/resources/data/recipes/crafting-recipes.json`  

## Verbindliche Regeln

- Ausschließlich echte **Minecraft-Vanilla-Items** als Input und Output.
- Keine Custom-Items oder erfundenen Zwischenprodukte.
- PixelRPG-Rezepte dürfen beliebige Kombinationen und Mengen verwenden.
- Der Beruf bestimmt die Zuständigkeit des Rezepts.
- Kategorien halten den Katalog übersichtlich.
- `FISHERMAN` und `WOODCUTTER` bleiben passive Berufe ohne normale Crafting-Rezepte.
- Der Gelehrte verwendet bei verzauberten Büchern jeweils die **höchste Vanilla-Verzauberungsstufe**.
- Der Alchemist verwendet bei aufwertbaren Tränken jeweils die **höchste verfügbare Trankstufe** (Stufe II).
- Die Dialogoberfläche verwendet für Titel, Beschreibungen, Kategorien, Zustände und Aktionen deutsche Texte.

## Berufsübersicht

| Beruf | Rezepte | Kategorien |
|---|---:|---|
| SCHMIED | 61 | `TOOLS`, `WEAPONS`, `ARMOR`, `UTILITY`, `MATERIALS`, `CRAFTING` |
| GELEHRTER | 29 | `BOOKS`, `KNOWLEDGE`, `ARCANE`, `UTILITY` |
| LANDWIRT | 25 | `CROPS`, `PROCESSING`, `PRODUCE`, `SPECIALTIES`, `UTILITY` |
| KOCH | 23 | `BASIC_FOOD`, `MEALS`, `COOKED_FOOD`, `SPECIALTIES` |
| SCHNEIDER | 26 | `TEXTILES`, `DECORATION`, `LEATHER`, `CRAFTING` |
| ALCHEMIST | 39 | `POTIONS`, `SPECIAL_MATERIALS` |
| STEINMETZ | 58 | `STONE_BLOCKS`, `STAIRS_SLABS`, `CHISELED` |

---

# SCHMIED

**Domäne:** Werkzeuge, Waffen, Rüstung und metallische Werkstücke.

## TOOLS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 3x COPPER_INGOT + 2x STICK | COPPER_PICKAXE | 1 | COMMON |
| 3x COPPER_INGOT + 2x STICK | COPPER_AXE | 1 | COMMON |
| 1x COPPER_INGOT + 2x STICK | COPPER_SHOVEL | 1 | COMMON |
| 2x COPPER_INGOT + 2x STICK | COPPER_HOE | 1 | COMMON |
| 3x IRON_INGOT + 2x STICK | IRON_PICKAXE | 10 | COMMON |
| 3x IRON_INGOT + 2x STICK | IRON_AXE | 10 | COMMON |
| 1x IRON_INGOT + 2x STICK | IRON_SHOVEL | 10 | COMMON |
| 2x IRON_INGOT + 2x STICK | IRON_HOE | 10 | COMMON |
| 3x GOLD_INGOT + 2x STICK | GOLD_PICKAXE | 25 | UNCOMMON |
| 3x GOLD_INGOT + 2x STICK | GOLD_AXE | 25 | UNCOMMON |
| 1x GOLD_INGOT + 2x STICK | GOLD_SHOVEL | 25 | UNCOMMON |
| 2x GOLD_INGOT + 2x STICK | GOLD_HOE | 25 | UNCOMMON |
| 3x DIAMOND_INGOT + 2x STICK | DIAMOND_PICKAXE | 45 | RARE |
| 3x DIAMOND_INGOT + 2x STICK | DIAMOND_AXE | 45 | RARE |
| 1x DIAMOND_INGOT + 2x STICK | DIAMOND_SHOVEL | 45 | RARE |
| 2x DIAMOND_INGOT + 2x STICK | DIAMOND_HOE | 45 | RARE |
| 2x IRON_INGOT | SHEARS | 20 | COMMON |

## WEAPONS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 2x COPPER_INGOT + 1x STICK | COPPER_SWORD | 5 | COMMON |
| 2x IRON_INGOT + 1x STICK | IRON_SWORD | 15 | COMMON |
| 2x GOLD_INGOT + 1x STICK | GOLD_SWORD | 30 | UNCOMMON |
| 2x DIAMOND_INGOT + 1x STICK | DIAMOND_SWORD | 50 | RARE |
| 2x NETHERITE_INGOT + 1x STICK | DIAMOND_SWORD | 85 | EPIC |
| 2x COPPER_INGOT + 1x STICK | COPPER_SPEAR | 5 | COMMON |
| 2x IRON_INGOT + 1x STICK | IRON_SPEAR | 15 | COMMON |
| 2x GOLD_INGOT + 1x STICK | GOLD_SPEAR | 30 | UNCOMMON |
| 2x DIAMOND_INGOT + 1x STICK | DIAMOND_SPEAR | 50 | RARE |
| 2x NETHERITE_INGOT + 1x STICK | DIAMOND_SPEAR | 85 | EPIC |
| 6x PRISMARINE_SHARD + 2x DIAMOND + 1x STICK | TRIDENT | 95 | LEGENDARY |

## ARMOR

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 5x COPPER_INGOT | COPPER_HELMET | 20 | COMMON |
| 8x COPPER_INGOT | COPPER_CHESTPLATE | 20 | COMMON |
| 7x COPPER_INGOT | COPPER_LEGGINGS | 20 | COMMON |
| 4x COPPER_INGOT | COPPER_BOOTS | 20 | COMMON |
| 5x IRON_INGOT | IRON_HELMET | 30 | COMMON |
| 8x IRON_INGOT | IRON_CHESTPLATE | 30 | COMMON |
| 7x IRON_INGOT | IRON_LEGGINGS | 30 | COMMON |
| 4x IRON_INGOT | IRON_BOOTS | 30 | COMMON |
| 5x GOLD_INGOT | GOLD_HELMET | 40 | UNCOMMON |
| 8x GOLD_INGOT | GOLD_CHESTPLATE | 40 | UNCOMMON |
| 7x GOLD_INGOT | GOLD_LEGGINGS | 40 | UNCOMMON |
| 4x GOLD_INGOT | GOLD_BOOTS | 40 | UNCOMMON |
| 5x DIAMOND_INGOT | DIAMOND_HELMET | 60 | RARE |
| 8x DIAMOND_INGOT | DIAMOND_CHESTPLATE | 60 | RARE |
| 7x DIAMOND_INGOT | DIAMOND_LEGGINGS | 60 | RARE |
| 4x DIAMOND_INGOT | DIAMOND_BOOTS | 60 | RARE |
| 1x OAK_LOG + 2x IRON_INGOT | SHIELD | 25 | COMMON |
| 7x COPPER_INGOT | COPPER_HORSE_ARMOR | 65 | RARE |
| 7x COPPER_INGOT + 2x NAUTILUS_SHELL | COPPER_NAUTILUS_ARMOR | 75 | EPIC |
| 7x DIAMOND | DIAMOND_HORSE_ARMOR | 80 | EPIC |
| 7x GOLD_INGOT | GOLDEN_HORSE_ARMOR | 55 | RARE |
| 7x IRON_INGOT | IRON_HORSE_ARMOR | 40 | UNCOMMON |

## UTILITY

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 3x IRON_INGOT | BUCKET | 20 | COMMON |
| 1x IRON_INGOT + 1x FLINT | FLINT_AND_STEEL | 25 | COMMON |
| 7x IRON_INGOT | CAULDRON | 35 | UNCOMMON |
| 8x IRON_NUGGET + 1x TORCH | LANTERN | 40 | UNCOMMON |
| 3x IRON_BLOCK + 4x IRON_INGOT | ANVIL | 80 | EPIC |
| 5x IRON_INGOT + 1x CHEST | HOPPER | 70 | EPIC |

## MATERIALS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 6x IRON_INGOT | IRON_BARS | 30 | COMMON |
| 2x IRON_NUGGET | IRON_CHAIN | 35 | UNCOMMON |

## CRAFTING

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 2x IRON_INGOT + 4x OAK_PLANKS | SMITHING_TABLE | 50 | RARE |
| 1x STONE_SLAB + 2x STONE + 2x STICK | GRINDSTONE | 45 | RARE |
| 5x IRON_INGOT + 2x REDSTONE + 1x CHEST | CRAFTER | 90 | LEGENDARY |

---

# GELEHRTER

**Domäne:** Bücher, Wissen, Orientierung, arkane Gegenstände und Redstone-Technik.

## BOOKS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 3x SUGAR_CANE | PAPER | 1 | COMMON |
| 3x PAPER + 1x LEATHER | BOOK | 5 | COMMON |
| 3x BOOK + 3x OAK_PLANKS | BOOKSHELF | 15 | COMMON |
| 1x BOOK + 5x LAPIS_LAZULI + 1x DIAMOND | ENCHANTED_BOOK | 70 | RARE |
| 1x BOOK + 6x LAPIS_LAZULI + 2x EMERALD | ENCHANTED_BOOK | 80 | EPIC |
| 1x BOOK + 5x LAPIS_LAZULI + 2x IRON_INGOT | ENCHANTED_BOOK | 75 | RARE |
| 1x BOOK + 5x LAPIS_LAZULI + 1x DIAMOND | ENCHANTED_BOOK | 75 | RARE |

## KNOWLEDGE

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x BOOKSHELF + 4x OAK_SLAB | LECTERN | 35 | UNCOMMON |
| 4x IRON_INGOT + 1x REDSTONE | COMPASS | 20 | COMMON |
| 4x GOLD_INGOT + 1x REDSTONE | CLOCK | 25 | COMMON |
| 8x PAPER + 1x COMPASS | MAP | 30 | UNCOMMON |
| 2x COPPER_INGOT + 1x AMETHYST_SHARD | SPYGLASS | 45 | UNCOMMON |
| 8x CHISELED_STONE_BRICKS + 1x IRON_INGOT | LODESTONE | 65 | RARE |

## ARCANE

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x COMPASS + 8x ECHO_SHARD | RECOVERY_COMPASS | 80 | EPIC |
| 1x ENDER_PEARL + 1x BLAZE_POWDER | ENDER_EYE | 70 | RARE |
| 8x OBSIDIAN + 1x ENDER_EYE | ENDER_CHEST | 85 | EPIC |
| 1x BOOK + 2x DIAMOND + 4x OBSIDIAN | ENCHANTING_TABLE | 75 | RARE |
| 5x GLASS + 3x OBSIDIAN + 1x NETHER_STAR | BEACON | 95 | EPIC |
| 7x GLASS + 1x GHAST_TEAR + 1x ENDER_EYE | END_CRYSTAL | 90 | EPIC |
| 6x GLOWSTONE + 3x CRYING_OBSIDIAN | RESPAWN_ANCHOR | 85 | EPIC |
| 1x GLASS_BOTTLE + 2x GLOWSTONE_DUST + 2x REDSTONE | EXPERIENCE_BOTTLE | 90 | EPIC |

## UTILITY

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x PAPER + 2x GUNPOWDER | FIREWORK_ROCKET | 50 | RARE |
| 1x GUNPOWDER + 1x GLOWSTONE_DUST | FIREWORK_STAR | 55 | RARE |
| 1x REDSTONE + 1x STICK | REDSTONE_TORCH | 15 | COMMON |
| 6x COBBLESTONE + 2x REDSTONE + 1x QUARTZ | OBSERVER | 70 | RARE |
| 3x STONE + 3x REDSTONE_TORCH + 1x QUARTZ | COMPARATOR | 65 | RARE |
| 7x COBBLESTONE + 1x BOW + 1x REDSTONE | DISPENSER | 60 | RARE |
| 7x COBBLESTONE + 1x REDSTONE | DROPPER | 55 | RARE |
| 1x HAY_BLOCK + 4x REDSTONE | TARGET | 50 | RARE |

---

# LANDWIRT

**Domäne:** Saatgut, Pflanzen, landwirtschaftliche Verarbeitung, Erzeugnisse und Farbstoffe.

## CROPS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x WHEAT | WHEAT_SEEDS | 1 | COMMON |
| 1x BEETROOT | BEETROOT_SEEDS | 5 | COMMON |
| 1x PUMPKIN | PUMPKIN_SEEDS | 5 | COMMON |
| 1x MELON | MELON_SEEDS | 10 | COMMON |
| 1x TORCHFLOWER | TORCHFLOWER_SEEDS | 40 | RARE |
| 1x PITCHER_PLANT | PITCHER_POD | 45 | RARE |

## PROCESSING

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x SUGAR_CANE | SUGAR | 10 | COMMON |
| 3x SUGAR_CANE | PAPER | 15 | COMMON |
| 1x BONE | BONE_MEAL | 10 | COMMON |
| 1x KELP | DRIED_KELP | 15 | COMMON |
| 1x BONE_MEAL | WHITE_DYE | 1 | COMMON |
| 1x DANDELION | YELLOW_DYE | 5 | COMMON |
| 1x POPPY | RED_DYE | 5 | COMMON |
| 1x LAPIS_LAZULI | BLUE_DYE | 15 | COMMON |
| 1x CACTUS | GREEN_DYE | 15 | COMMON |
| 1x GREEN_DYE + 1x WHITE_DYE | LIME_DYE | 20 | UNCOMMON |
| 1x RED_DYE + 1x YELLOW_DYE | ORANGE_DYE | 20 | UNCOMMON |
| 1x RED_DYE + 1x BLUE_DYE | PURPLE_DYE | 20 | UNCOMMON |

## PRODUCE

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 9x WHEAT | HAY_BLOCK | 20 | UNCOMMON |
| 9x DRIED_KELP | DRIED_KELP_BLOCK | 30 | UNCOMMON |
| 4x PUMPKIN_SEEDS | PUMPKIN | 25 | UNCOMMON |
| 9x MELON_SEEDS | MELON | 30 | UNCOMMON |
| 2x MOSS_CARPET + 1x BONE_MEAL | MOSS_BLOCK | 35 | UNCOMMON |

## SPECIALTIES

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 4x SUGAR + 1x HONEYCOMB | HONEY_BLOCK | 55 | RARE |

## UTILITY

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 7x OAK_SLAB | COMPOSTER | 20 | UNCOMMON |

---

# KOCH

**Domäne:** Grundnahrung, gekochte Nahrung, Mahlzeiten und besondere Lebensmittel.

## BASIC_FOOD

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 3x WHEAT | BREAD | 1 | COMMON |
| 2x WHEAT + 1x COCOA_BEANS | COOKIE | 10 | COMMON |
| 1x KELP | DRIED_KELP | 15 | COMMON |
| 2x SWEET_BERRIES + 1x SUGAR | SWEET_BERRIES | 20 | UNCOMMON |

## MEALS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x PUMPKIN + 1x SUGAR + 1x EGG | PUMPKIN_PIE | 20 | UNCOMMON |
| 3x WHEAT + 2x SUGAR + 3x MILK_BUCKET + 1x EGG | CAKE | 45 | RARE |
| 1x COOKED_RABBIT + 1x CARROT + 1x POTATO + 1x BOWL | RABBIT_STEW | 35 | UNCOMMON |
| 1x RED_MUSHROOM + 1x BROWN_MUSHROOM + 1x BOWL | MUSHROOM_STEW | 10 | COMMON |
| 6x BEETROOT + 1x BOWL | BEETROOT_SOUP | 20 | UNCOMMON |

## COOKED_FOOD

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x BEEF | COOKED_BEEF | 5 | COMMON |
| 1x CHICKEN | COOKED_CHICKEN | 5 | COMMON |
| 1x PORKCHOP | COOKED_PORKCHOP | 5 | COMMON |
| 1x MUTTON | COOKED_MUTTON | 10 | COMMON |
| 1x RABBIT | COOKED_RABBIT | 15 | COMMON |
| 1x COD | COOKED_COD | 10 | COMMON |
| 1x SALMON | COOKED_SALMON | 15 | COMMON |
| 1x POTATO | BAKED_POTATO | 5 | COMMON |

## SPECIALTIES

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x RED_MUSHROOM + 1x BROWN_MUSHROOM + 1x DANDELION + 1x BOWL | SUSPICIOUS_STEW | 55 | RARE |
| 1x CARROT + 8x GOLD_NUGGET | GOLDEN_CARROT | 35 | UNCOMMON |
| 1x APPLE + 8x GOLD_INGOT | GOLDEN_APPLE | 70 | EPIC |
| 1x GOLDEN_APPLE + 8x GOLD_BLOCK | ENCHANTED_GOLDEN_APPLE | 100 | EPIC |
| 1x MELON_SLICE + 8x GOLD_NUGGET | GLISTERING_MELON_SLICE | 30 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x HONEYCOMB | HONEY_BOTTLE | 25 | UNCOMMON |

---

# SCHNEIDER

**Domäne:** Textilien, Teppiche, Betten, Banner, Leder und textile Verarbeitung.

## TEXTILES

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 4x STRING | WHITE_WOOL | 1 | COMMON |
| 1x WHITE_WOOL + 1x BLACK_DYE | BLACK_WOOL | 10 | COMMON |
| 1x WHITE_WOOL + 1x RED_DYE | RED_WOOL | 15 | COMMON |
| 1x WHITE_WOOL + 1x BLUE_DYE | BLUE_WOOL | 20 | UNCOMMON |
| 2x WHITE_WOOL | 3x WHITE_CARPET | 10 | COMMON |
| 2x BLACK_WOOL | 3x BLACK_CARPET | 20 | UNCOMMON |
| 2x RED_WOOL | 3x RED_CARPET | 25 | UNCOMMON |
| 2x BLUE_WOOL | 3x BLUE_CARPET | 30 | UNCOMMON |

## DECORATION

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 3x WHITE_WOOL + 3x OAK_PLANKS | WHITE_BED | 25 | UNCOMMON |
| 3x RED_WOOL + 3x OAK_PLANKS | RED_BED | 30 | UNCOMMON |
| 3x BLUE_WOOL + 3x OAK_PLANKS | BLUE_BED | 35 | RARE |
| 6x WHITE_WOOL + 1x STICK | WHITE_BANNER | 35 | RARE |
| 6x BLACK_WOOL + 1x STICK | BLACK_BANNER | 40 | RARE |
| 6x RED_WOOL + 1x STICK | RED_BANNER | 45 | RARE |
| 6x BLUE_WOOL + 1x STICK | BLUE_BANNER | 50 | RARE |
| 8x STICK + 1x WHITE_WOOL | PAINTING | 40 | RARE |

## LEATHER

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 4x RABBIT_HIDE | LEATHER | 10 | COMMON |
| 5x LEATHER | LEATHER_HELMET | 20 | UNCOMMON |
| 8x LEATHER | LEATHER_CHESTPLATE | 30 | UNCOMMON |
| 7x LEATHER | LEATHER_LEGGINGS | 35 | RARE |
| 4x LEATHER | LEATHER_BOOTS | 25 | UNCOMMON |
| 1x LEATHER + 6x STRING | BUNDLE | 45 | RARE |
| 7x LEATHER | LEATHER_HORSE_ARMOR | 55 | EPIC |
| 4x ROTTEN_FLESH | LEATHER | 10 | COMMON |

## CRAFTING

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 2x STRING + 2x OAK_PLANKS | LOOM | 50 | RARE |
| 1x PAPER + 2x STRING | NAME_TAG | 65 | EPIC |

---

# ALCHEMIST

**Domäne:** Tränke, alchemistische Verarbeitung und besondere Vanilla-Gegenstände.

## POTIONS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 1x GLASS_BOTTLE | POTION | 1 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x NETHER_WART | POTION | 5 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x GLISTERING_MELON_SLICE | POTION | 20 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x BLAZE_POWDER | POTION | 25 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x SUGAR | POTION | 25 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x MAGMA_CREAM | POTION | 30 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x PUFFERFISH | POTION | 35 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x GOLDEN_CARROT | POTION | 40 | RARE |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x RABBIT_FOOT | POTION | 45 | RARE |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x PHANTOM_MEMBRANE | POTION | 50 | RARE |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x SPIDER_EYE | POTION | 25 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x FERMENTED_SPIDER_EYE | POTION | 20 | UNCOMMON |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 1x GHAST_TEAR | POTION | 60 | RARE |
| 1x GLASS_BOTTLE + 1x NETHER_WART + 5x TURTLE_SCUTE | POTION | 80 | EPIC |
| 1x GLASS_BOTTLE + 1x RABBIT_FOOT | POTION | 70 | EPIC |
| 1x SPIDER_EYE + 1x SUGAR + 1x BROWN_MUSHROOM | FERMENTED_SPIDER_EYE | 10 | UNCOMMON |
| 1x BLAZE_ROD | BLAZE_POWDER | 15 | UNCOMMON |
| 1x MAGMA_BLOCK + 1x SLIME_BALL | MAGMA_CREAM | 25 | UNCOMMON |
| 1x MELON_SLICE + 8x GOLD_NUGGET | GLISTERING_MELON_SLICE | 20 | UNCOMMON |
| 1x ENDER_PEARL + 1x BLAZE_POWDER | ENDER_EYE | 65 | RARE |
| 1x POTION + 1x GUNPOWDER | SPLASH_POTION | 55 | RARE |
| 1x SPLASH_POTION + 1x DRAGON_BREATH | LINGERING_POTION | 75 | EPIC |
| 1x GLASS_BOTTLE + 1x CHORUS_FRUIT | DRAGON_BREATH | 85 | EPIC |

## SPECIAL_MATERIALS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 8x ROTTEN_FLESH + 1x IRON_INGOT | ZOMBIE_SPAWN_EGG | 30 | UNCOMMON |
| 8x BONE + 1x IRON_INGOT | SKELETON_SPAWN_EGG | 35 | UNCOMMON |
| 8x GUNPOWDER + 1x IRON_INGOT | CREEPER_SPAWN_EGG | 40 | UNCOMMON |
| 8x SPIDER_EYE + 1x IRON_INGOT | SPIDER_SPAWN_EGG | 35 | UNCOMMON |
| 10x SPIDER_EYE + 2x FERMENTED_SPIDER_EYE | CAVE_SPIDER_SPAWN_EGG | 50 | RARE |
| 8x SLIME_BALL + 1x IRON_INGOT | SLIME_SPAWN_EGG | 45 | UNCOMMON |
| 4x ENDER_PEARL + 2x IRON_INGOT | ENDERMAN_SPAWN_EGG | 65 | RARE |
| 6x BLAZE_ROD + 2x GOLD_INGOT | BLAZE_SPAWN_EGG | 70 | EPIC |
| 4x GHAST_TEAR + 8x QUARTZ | GHAST_SPAWN_EGG | 80 | EPIC |
| 8x MAGMA_CREAM + 2x GOLD_INGOT | MAGMA_CUBE_SPAWN_EGG | 75 | EPIC |
| 4x SPIDER_EYE + 4x GLASS_BOTTLE + 4x REDSTONE | WITCH_SPAWN_EGG | 80 | EPIC |
| 8x PRISMARINE_SHARD + 2x PUFFERFISH | GUARDIAN_SPAWN_EGG | 85 | EPIC |
| 4x SHULKER_SHELL + 2x ENDER_PEARL | SHULKER_SPAWN_EGG | 90 | LEGENDARY |
| 2x WITHER_SKELETON_SKULL + 8x BONE | WITHER_SKELETON_SPAWN_EGG | 95 | LEGENDARY |
| 2x SCULK_CATALYST + 8x ECHO_SHARD | WARDEN_SPAWN_EGG | 100 | LEGENDARY |
| 2x COPPER_BLOCK + 4x COPPER_INGOT | COPPER_GOLEM_SPAWN_EGG | 60 | RARE |

---

# STEINMETZ

**Domäne:** Baublöcke, Steinvarianten, Stufen, Platten, gemeißelte Formen und Spezialbaustoffe.

## STONE_BLOCKS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 4x COBBLESTONE | STONE | 1 | COMMON |
| 4x STONE | STONE_BRICKS | 6 | COMMON |
| 4x COBBLED_DEEPSLATE | DEEPSLATE | 11 | COMMON |
| 4x DEEPSLATE | DEEPSLATE_BRICKS | 16 | COMMON |
| 4x DEEPSLATE_BRICKS | DEEPSLATE_TILES | 21 | COMMON |
| 4x BLACKSTONE | POLISHED_BLACKSTONE | 26 | COMMON |
| 4x POLISHED_BLACKSTONE | POLISHED_BLACKSTONE_BRICKS | 31 | UNCOMMON |
| 4x NETHER_BRICK | NETHER_BRICKS | 36 | UNCOMMON |
| 4x END_STONE | END_STONE_BRICKS | 41 | UNCOMMON |
| 4x PRISMARINE | PRISMARINE_BRICKS | 46 | UNCOMMON |
| 4x QUARTZ | QUARTZ_BLOCK | 51 | UNCOMMON |
| 4x QUARTZ_BLOCK | QUARTZ_BRICKS | 56 | UNCOMMON |
| 4x QUARTZ_BLOCK | SMOOTH_QUARTZ | 61 | RARE |
| 4x BRICK | BRICKS | 66 | RARE |
| 4x MUD | MUD_BRICKS | 71 | RARE |
| 4x GRANITE | POLISHED_GRANITE | 76 | RARE |
| 4x DIORITE | POLISHED_DIORITE | 81 | RARE |
| 4x ANDESITE | POLISHED_ANDESITE | 86 | RARE |
| 4x TUFF | POLISHED_TUFF | 91 | RARE |
| 4x POLISHED_TUFF | TUFF_BRICKS | 95 | RARE |

## STAIRS_SLABS

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 2x STONE | STONE_SLAB | 60 | RARE |
| 3x STONE | STONE_STAIRS | 65 | RARE |
| 3x STONE | STONE_WALL | 70 | RARE |
| 2x STONE_BRICKS | STONE_BRICK_SLAB | 60 | RARE |
| 3x STONE_BRICKS | STONE_BRICK_STAIRS | 65 | RARE |
| 3x STONE_BRICKS | STONE_BRICK_WALL | 70 | RARE |
| 2x DEEPSLATE | DEEPSLATE_SLAB | 60 | RARE |
| 3x DEEPSLATE | DEEPSLATE_STAIRS | 65 | RARE |
| 3x DEEPSLATE | DEEPSLATE_WALL | 70 | RARE |
| 2x POLISHED_BLACKSTONE | POLISHED_BLACKSTONE_SLAB | 60 | RARE |
| 3x POLISHED_BLACKSTONE | POLISHED_BLACKSTONE_STAIRS | 65 | RARE |
| 3x POLISHED_BLACKSTONE | POLISHED_BLACKSTONE_WALL | 70 | RARE |
| 2x END_STONE_BRICKS | END_STONE_BRICK_SLAB | 60 | RARE |
| 3x END_STONE_BRICKS | END_STONE_BRICK_STAIRS | 65 | RARE |
| 3x END_STONE_BRICKS | END_STONE_BRICK_WALL | 70 | RARE |
| 2x PRISMARINE | PRISMARINE_SLAB | 60 | RARE |
| 3x PRISMARINE | PRISMARINE_STAIRS | 65 | RARE |
| 3x PRISMARINE | PRISMARINE_WALL | 70 | RARE |
| 2x QUARTZ_BLOCK | QUARTZ_SLAB | 60 | RARE |
| 3x QUARTZ_BLOCK | QUARTZ_STAIRS | 65 | RARE |

## CHISELED

| Kombination | Ergebnis | Level | Seltenheit |
|---|---|---:|---|
| 2x STONE_BRICKS | CHISELED_STONE_BRICKS | 45 | UNCOMMON |
| 2x DEEPSLATE | CHISELED_DEEPSLATE | 55 | RARE |
| 2x TUFF | CHISELED_TUFF | 65 | RARE |
| 2x QUARTZ_BLOCK | CHISELED_QUARTZ_BLOCK | 70 | RARE |
| 2x QUARTZ_BLOCK | QUARTZ_PILLAR | 60 | RARE |
| 3x STONE | SMOOTH_STONE | 65 | RARE |
| 4x SANDSTONE | SMOOTH_SANDSTONE | 35 | UNCOMMON |
| 4x RED_SANDSTONE | SMOOTH_RED_SANDSTONE | 40 | UNCOMMON |
| 4x SANDSTONE | CUT_SANDSTONE | 30 | UNCOMMON |
| 4x RED_SANDSTONE | CUT_RED_SANDSTONE | 35 | UNCOMMON |
| 4x SAND | GLASS | 20 | UNCOMMON |
| 6x GLASS | GLASS_PANE | 25 | UNCOMMON |
| 4x CLAY | TERRACOTTA | 25 | UNCOMMON |
| 4x CLAY_BALL | BRICK | 15 | UNCOMMON |
| 4x WHITE_CONCRETE_POWDER + 1x WATER_BUCKET | WHITE_CONCRETE | 75 | RARE |
| 4x PRISMARINE_SHARD + 1x PRISMARINE_CRYSTALS | PRISMARINE | 50 | RARE |
| 4x PRISMARINE_SHARD + 5x PRISMARINE_CRYSTALS | SEA_LANTERN | 80 | EPIC |
| 4x LAVA_BUCKET + 1x WATER_BUCKET | OBSIDIAN | 90 | EPIC |

---

# FISCHER

**Status:** passiv – aktuell keine normalen Crafting-Rezepte.

Vanilla-Fänge und Beute können als Rohstoffe für andere Berufe dienen.

- COD
- SALMON
- PUFFERFISH
- TROPICAL_FISH
- NAUTILUS_SHELL
- BOW
- FISHING_ROD
- SADDLE
- ENCHANTED_BOOK

---

# HOLZFÄLLER

**Status:** passiv – aktuell keine normalen Crafting-Rezepte.

Vanilla-Holz und Holzrohstoffe können als Zutaten für andere Berufe dienen.

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
- STRIPPED_*_LOG
- *_PLANKS

---

# Besondere Rezeptänderungen

- **Gelehrter:** Effizienz V, Glück III, Schärfe V und Schutz IV werden als jeweils maximale Vanilla-Stufe hergestellt.
- **Alchemist:** Heilung II, Stärke II, Schnelligkeit II, Springen II, Gift II, Regeneration II und Schildkrötenmeister II werden als höchste verfügbare Stufe hergestellt. Tränke ohne stärkere Stufe bleiben auf ihrer normalen Vanilla-Stufe.
- **Schneider:** `4x ROTTEN_FLESH → 1x LEATHER` in der Kategorie `LEATHER`.

## Dialogsprache

Alle für die Berufs- und Rezept-Dialoge sichtbaren Texte bleiben Deutsch, insbesondere:

- Berufe und Berufsbeschreibungen
- Rezeptkategorien
- Rezeptnamen
- Freischaltungs- und Herstellungsaktionen
- Statusmeldungen wie „freigeschaltet“, „gesperrt“, „Herstellen“ und „Zurück“
- Zutaten- und Levelangaben

Diese Datei dokumentiert den aktuellen Katalog auf `test`.
