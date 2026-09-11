# Berufe – Herstellungsrezepte

Diese Datei bildet den **aktuellen Stand** aus `src/main/resources/data/recipes/crafting-recipes.json` ab und dient als manuell bearbeitbare Übersicht. Änderungen hier werden **nicht automatisch** in die Rezeptdaten übernommen.

## Farmer

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Saatgutbündel (`farmer:seed_bundle`) | 1× WHEAT_SEEDS | 2× WHEAT | – | 0 | COMMON |
| 20 | Erntebündel (`farmer:crop_bundle`) | 1× HAY_BLOCK | 6× WHEAT, 4× WHEAT_SEEDS | 2× seed_bundle | 400 | COMMON |
| 40 | Obstkiste (`farmer:orchard_crate`) | 1× CHEST | 4× APPLE, 8× OAK_PLANKS | 2× crop_bundle, 1× woodcutter:plank_bundle | 1200 | UNCOMMON |
| 60 | Goldhonig (`farmer:golden_honey`) | 1× HONEY_BOTTLE | 4× HONEYCOMB, 2× GOLD_NUGGET | 1× orchard_crate | 2500 | RARE |
| 80 | Meistersaat (`farmer:master_seed`) | 8× WHEAT_SEEDS | 12× WHEAT, 4× GOLD_NUGGET | 1× golden_honey | 5000 | EPIC |
| 100 | Kern der Ernte (`farmer:harvest_core`) | 1× GOLDEN_CARROT | 8× GOLD_NUGGET, 8× CARROT | 2× master_seed | 10000 | LEGENDARY |

## Holzfäller

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Holzbündel (`woodcutter:log_bundle`) | 1× OAK_LOG | 3× OAK_LOG | – | 0 | COMMON |
| 20 | Bretterbündel (`woodcutter:plank_bundle`) | 4× OAK_PLANKS | 2× OAK_LOG | 2× log_bundle | 400 | COMMON |
| 40 | Werkstoffkiste (`woodcutter:work_crate`) | 1× CHEST | 8× OAK_PLANKS, 4× STICK | 2× plank_bundle | 1200 | UNCOMMON |
| 60 | Verstärkte Kiste (`woodcutter:reinforced_crate`) | 1× BARREL | 8× OAK_PLANKS, 4× IRON_NUGGET | 1× work_crate | 2500 | RARE |
| 80 | Zimmermannssatz (`woodcutter:carpenter_kit`) | 1× CRAFTING_TABLE | 6× OAK_PLANKS, 4× STICK | 1× reinforced_crate | 5000 | EPIC |
| 100 | Meisterrahmen (`woodcutter:master_frame`) | 2× BARREL | 16× OAK_PLANKS, 2× IRON_INGOT | 1× carpenter_kit | 10000 | LEGENDARY |

## Schmied

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Eisenbeschlag (`blacksmith:iron_fitting`) | 1× IRON_NUGGET | 1× IRON_INGOT | – | 0 | COMMON |
| 20 | Eisenwerkzeugkopf (`blacksmith:iron_toolhead`) | 1× IRON_INGOT | 2× IRON_INGOT, 1× COAL | 2× iron_fitting | 400 | COMMON |
| 40 | Stahlkern (`blacksmith:steel_core`) | 1× IRON_INGOT | 3× IRON_INGOT, 2× COAL | 2× iron_toolhead | 1200 | UNCOMMON |
| 60 | Geschmiedete Spitzhacke (`blacksmith:forged_pickaxe`) | 1× IRON_PICKAXE | 2× STICK | 2× steel_core, 1× woodcutter:plank_bundle | 2500 | RARE |
| 80 | Geschmiedetes Schwert (`blacksmith:forged_sword`) | 1× IRON_SWORD | 1× STICK | 2× steel_core | 5000 | EPIC |
| 100 | Rüstung des Schmiedemeisters (`blacksmith:master_armor`) | 1× DIAMOND_CHESTPLATE | 4× DIAMOND, 4× IRON_INGOT | 1× forged_sword | 10000 | LEGENDARY |

## Gelehrter

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Papierbündel (`scholar:paper_bundle`) | 1× PAPER | 3× SUGAR_CANE | – | 0 | COMMON |
| 20 | Waffenhandbuch (`scholar:weapon_manual`) | 1× BOOK | 4× PAPER, 2× FEATHER, 1× INK_SAC | 2× paper_bundle | 400 | COMMON |
| 40 | Forschungsmappe (`scholar:research_notes`) | 1× BOOK | 6× PAPER, 2× LAPIS_LAZULI | 1× weapon_manual | 1200 | UNCOMMON |
| 60 | Arkaner Foliant (`scholar:arcane_tome`) | 1× BOOK | 2× BOOK, 3× AMETHYST_SHARD, 1× GLOW_INK_SAC | 2× research_notes | 2500 | RARE |
| 80 | Meisterhandbuch (`scholar:master_manual`) | 1× ENCHANTED_BOOK | 5× LAPIS_LAZULI, 1× BOOK | 1× arcane_tome | 5000 | EPIC — Efficiency V |
| 100 | Weltarchiv (`scholar:world_archive`) | 1× WRITABLE_BOOK | 2× DIAMOND, 2× GLOW_INK_SAC | 1× master_manual | 10000 | LEGENDARY |

## Koch

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Grundteig (`cook:basic_dough`) | 1× BREAD | 3× WHEAT | 1× farmer:seed_bundle | 0 | COMMON |
| 20 | Herzhafte Mahlzeit (`cook:hearty_meal`) | 1× COOKED_BEEF | 2× BEEF, 1× CARROT | 1× basic_dough, 1× farmer:crop_bundle | 400 | COMMON |
| 40 | Fischplatte (`cook:fishermans_platter`) | 1× COOKED_COD | 2× COD, 2× POTATO | 1× hearty_meal, 1× fisherman:fish_crate | 1200 | UNCOMMON |
| 60 | Goldenes Festmahl (`cook:golden_feast`) | 1× GOLDEN_CARROT | 6× GOLD_NUGGET, 2× CARROT | 1× fishermans_platter, 1× farmer:golden_honey | 2500 | RARE |
| 80 | Heldenmahl (`cook:hero_feast`) | 1× GOLDEN_APPLE | 4× GOLD_INGOT, 1× APPLE | 1× golden_feast, 1× farmer:orchard_crate | 5000 | EPIC |
| 100 | Legendenmahl (`cook:legendary_feast`) | 1× ENCHANTED_GOLDEN_APPLE | 1× GOLD_BLOCK, 2× APPLE | 1× hero_feast, 1× alchemist:master_elixir, 1× scholar:world_archive | 10000 | LEGENDARY |

## Schneider

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Garnbündel (`tailor:thread_bundle`) | 1× STRING | 3× STRING | – | 0 | COMMON |
| 20 | Stoffrolle (`tailor:cloth_roll`) | 2× WHITE_WOOL | 4× STRING, 2× WHITE_WOOL | 2× thread_bundle | 400 | COMMON |
| 40 | Lederkit (`tailor:leather_kit`) | 1× LEATHER | 3× LEATHER, 2× STRING | 1× cloth_roll | 1200 | UNCOMMON |
| 60 | Verstärkter Stoff (`tailor:reinforced_cloth`) | 2× WHITE_WOOL | 6× STRING, 2× LEATHER | 1× leather_kit | 2500 | RARE |
| 80 | Reisendenrüstung (`tailor:traveler_armor`) | 1× LEATHER_CHESTPLATE | 8× LEATHER | 2× reinforced_cloth, 1× scholar:research_notes | 5000 | EPIC |
| 100 | Meisterumhang (`tailor:master_cloak`) | 1× LEATHER_CHESTPLATE | 8× LEATHER, 4× GOLD_NUGGET | 1× traveler_armor, 1× scholar:master_manual | 10000 | LEGENDARY |

## Alchemist

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Kräuterextrakt (`alchemist:herbal_extract`) | 1× HONEY_BOTTLE | 2× DANDELION, 1× SUGAR | 1× farmer:seed_bundle | 0 | COMMON |
| 20 | Heilelixier (`alchemist:healing`) | 1× POTION | 1× GLISTERING_MELON_SLICE | 1× herbal_extract | 400 | COMMON — Healing |
| 40 | Arkaner Katalysator (`alchemist:arcane_catalyst`) | 1× POTION | 2× AMETHYST_SHARD, 1× GLOW_INK_SAC | 1× healing, 1× scholar:research_notes | 1200 | UNCOMMON — Water |
| 60 | Starkes Heilelixier (`alchemist:strong_healing`) | 1× POTION | 2× GLISTERING_MELON_SLICE, 2× GOLD_NUGGET | 1× arcane_catalyst | 2500 | RARE — Strong Healing |
| 80 | Meisterelixier (`alchemist:master_elixir`) | 1× POTION | 1× DIAMOND, 4× GLOWSTONE_DUST | 2× strong_healing, 1× scholar:arcane_tome | 5000 | EPIC — Strength |
| 100 | Großes Elixier (`alchemist:grand_elixir`) | 1× POTION | 2× DIAMOND, 8× GLOWSTONE_DUST | 1× master_elixir, 1× farmer:golden_honey | 10000 | LEGENDARY — Strength |

## Maurer

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Steinziegelkern (`mason:stone_brick`) | 1× STONE_BRICKS | 4× STONE | – | 0 | COMMON |
| 20 | Behauener Stein (`mason:cut_stone`) | 1× STONE_BRICKS | 4× STONE | 2× stone_brick | 400 | COMMON |
| 40 | Verstärkter Ziegel (`mason:reinforced_brick`) | 1× STONE_BRICKS | 4× STONE_BRICKS, 2× IRON_NUGGET | 2× cut_stone, 1× blacksmith:iron_fitting | 1200 | UNCOMMON |
| 60 | Prismarrahmen (`mason:prismarine_frame`) | 1× PRISMARINE_BRICKS | 6× PRISMARINE_SHARD | 2× reinforced_brick, 1× fisherman:rare_scale | 2500 | RARE |
| 80 | Obsidianpfeiler (`mason:obsidian_pillar`) | 1× OBSIDIAN | 4× OBSIDIAN, 2× QUARTZ | 1× prismarine_frame, 1× scholar:arcane_tome | 5000 | EPIC |
| 100 | Meistermonument (`mason:master_monument`) | 4× PRISMARINE_BRICKS | 16× PRISMARINE_SHARD, 2× GOLD_INGOT | 2× obsidian_pillar, 2× fisherman:rare_scale | 10000 | LEGENDARY |

## Fischer

| Level | Rezept | Ergebnis | Materialkosten | Rezeptkosten | Freischaltung | Seltenheit |
|---:|---|---|---|---|---:|---|
| 1 | Fischkiste (`fisherman:fish_crate`) | 1× COD | 3× COD, 2× OAK_PLANKS | 1× woodcutter:plank_bundle | 0 | COMMON |
| 20 | Lachspaket (`fisherman:salmon_pack`) | 1× SALMON | 3× SALMON | 1× fish_crate | 400 | COMMON |
| 40 | Prismarinzahn (`fisherman:prismarine_fang`) | 1× PRISMARINE_SHARD | 2× PRISMARINE_SHARD, 2× COD | 1× salmon_pack | 1200 | UNCOMMON |
| 60 | Seltene Schuppe (`fisherman:rare_scale`) | 1× PRISMARINE_CRYSTALS | 2× PRISMARINE_CRYSTALS, 1× TROPICAL_FISH | 2× prismarine_fang | 2500 | RARE |
| 80 | Tiefseefang (`fisherman:deep_catch`) | 1× NAUTILUS_SHELL | 1× NAUTILUS_SHELL, 4× COD | 1× rare_scale, 1× scholar:research_notes | 5000 | EPIC |
| 100 | Legendärer Fang (`fisherman:legendary_catch`) | 1× HEART_OF_THE_SEA | 1× HEART_OF_THE_SEA, 2× NAUTILUS_SHELL | 1× deep_catch, 1× alchemist:master_elixir | 10000 | LEGENDARY |

## Bearbeitungshinweise

- Rezept-IDs, `resultItemId` und `itemCosts` sind die technischen Schlüssel.
- `requiredProfessionLevel` liegt aktuell bei **1 / 20 / 40 / 60 / 80 / 100** je Beruf.
- `unlockPrice` ist der Preis für die Freischaltung eines nicht standardmäßig freigeschalteten Rezepts.
- `rarity` ist die maximale Zielseltenheit für das Crafting-System.
- Besondere Ergebnisse können zusätzlich `potionType`, `enchantment` und `enchantmentLevel` besitzen.
- Die `resultItemId` ist zugleich die Grundlage für die zukünftige Resource-Pack-Verknüpfung der hergestellten Gegenstände.
