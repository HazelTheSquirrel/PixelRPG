# PixelRPG – Resourcepack-Itemliste

Stand: **Branch `test`**  
Quelle: `src/main/resources/data/item-definitions.json`, `boss-reward-items.json` und `food-definitions.json`.

Diese Datei listet alle **konkret im Plugin definierten `resourcepackId`-Werte**. Genau diese IDs sind die relevanten Namespaced Keys für die Item-Modelle des Resourcepacks.

**Gesamt: 85 eindeutige Resourcepack-IDs**
- 30 normale ItemDefinitionen
- 32 Boss-Rewards
- 23 Food-Definitionen

> Hinweis: Dynamisch über `RPGItemBuilder` erzeugte Items sind nicht als einzelne konkrete Resourcepack-IDs gelistet, weil deren Item-ID/Level-Kombinationen generiert werden. Ebenfalls keine Resourcepack-ID besitzen derzeit z. B. Guild-Währung, Guild-Compass und Story-Bücher; diese verwenden ihre jeweilige Vanilla-Basis bzw. eigene PDC-Markierungen.

## 1. Normale Items

| # | Name | Resourcepack-ID | Plugin-Item-ID | Material | Level | Typ |
|---:|---|---|---|---|---:|---|
| 1 | Eisenschwert | `pixelrpg:weapons/iron_sword` | `pixelrpg:weapons/iron_sword/common_3` | `IRON_SWORD` | 3 | Waffe |
| 2 | Goldklinge | `pixelrpg:weapons/gold_sword` | `pixelrpg:weapons/gold_sword/uncommon_25` | `GOLDEN_SWORD` | 25 | Waffe |
| 3 | Diamantklinge | `pixelrpg:weapons/diamond_sword` | `pixelrpg:weapons/diamond_sword/rare_40` | `DIAMOND_SWORD` | 40 | Waffe |
| 4 | Netheritklinge | `pixelrpg:weapons/netherite_sword` | `pixelrpg:weapons/netherite_sword/legendary_90` | `NETHERITE_SWORD` | 60 | Waffe |
| 5 | Donnerwacht-Helm | `pixelrpg:armor/copper_helmet` | `pixelrpg:armor/copper_helmet/donnerwacht_10` | `COPPER_HELMET` | 10 | Rüstung |
| 6 | Donnerwacht-Brustplatte | `pixelrpg:armor/copper_chestplate` | `pixelrpg:armor/copper_chestplate/donnerwacht_10` | `COPPER_CHESTPLATE` | 10 | Rüstung |
| 7 | Donnerwacht-Beinschutz | `pixelrpg:armor/copper_leggings` | `pixelrpg:armor/copper_leggings/donnerwacht_10` | `COPPER_LEGGINGS` | 10 | Rüstung |
| 8 | Donnerwacht-Stiefel | `pixelrpg:armor/copper_boots` | `pixelrpg:armor/copper_boots/donnerwacht_10` | `COPPER_BOOTS` | 10 | Rüstung |
| 9 | Schattengeflecht-Helm | `pixelrpg:armor/chainmail_helmet` | `pixelrpg:armor/chainmail_helmet/schattengeflecht_20` | `CHAINMAIL_HELMET` | 20 | Rüstung |
| 10 | Schattengeflecht-Panzer | `pixelrpg:armor/chainmail_chestplate` | `pixelrpg:armor/chainmail_chestplate/schattengeflecht_20` | `CHAINMAIL_CHESTPLATE` | 20 | Rüstung |
| 11 | Schattengeflecht-Beinschutz | `pixelrpg:armor/chainmail_leggings` | `pixelrpg:armor/chainmail_leggings/schattengeflecht_20` | `CHAINMAIL_LEGGINGS` | 20 | Rüstung |
| 12 | Schattengeflecht-Stiefel | `pixelrpg:armor/chainmail_boots` | `pixelrpg:armor/chainmail_boots/schattengeflecht_20` | `CHAINMAIL_BOOTS` | 20 | Rüstung |
| 13 | Stahlwall-Helm | `pixelrpg:armor/iron_helmet` | `pixelrpg:armor/iron_helmet/stahlwall_30` | `IRON_HELMET` | 30 | Rüstung |
| 14 | Stahlwall-Brustplatte | `pixelrpg:armor/iron_chestplate` | `pixelrpg:armor/iron_chestplate/stahlwall_30` | `IRON_CHESTPLATE` | 30 | Rüstung |
| 15 | Stahlwall-Beinschutz | `pixelrpg:armor/iron_leggings` | `pixelrpg:armor/iron_leggings/stahlwall_30` | `IRON_LEGGINGS` | 30 | Rüstung |
| 16 | Stahlwall-Stiefel | `pixelrpg:armor/iron_boots` | `pixelrpg:armor/iron_boots/stahlwall_30` | `IRON_BOOTS` | 30 | Rüstung |
| 17 | Sonnengewand-Helm | `pixelrpg:armor/gold_helmet` | `pixelrpg:armor/gold_helmet/sonnengewand_45` | `GOLDEN_HELMET` | 45 | Rüstung |
| 18 | Sonnengewand-Brustplatte | `pixelrpg:armor/gold_chestplate` | `pixelrpg:armor/gold_chestplate/sonnengewand_45` | `GOLDEN_CHESTPLATE` | 45 | Rüstung |
| 19 | Sonnengewand-Beinschutz | `pixelrpg:armor/gold_leggings` | `pixelrpg:armor/gold_leggings/sonnengewand_45` | `GOLDEN_LEGGINGS` | 45 | Rüstung |
| 20 | Sonnengewand-Stiefel | `pixelrpg:armor/gold_boots` | `pixelrpg:armor/gold_boots/sonnengewand_45` | `GOLDEN_BOOTS` | 45 | Rüstung |
| 21 | Kristallwache-Helm | `pixelrpg:armor/diamond_helmet` | `pixelrpg:armor/diamond_helmet/kristallwache_60` | `DIAMOND_HELMET` | 60 | Rüstung |
| 22 | Kristallwache-Brustplatte | `pixelrpg:armor/diamond_chestplate` | `pixelrpg:armor/diamond_chestplate/kristallwache_60` | `DIAMOND_CHESTPLATE` | 60 | Rüstung |
| 23 | Kristallwache-Beinschutz | `pixelrpg:armor/diamond_leggings` | `pixelrpg:armor/diamond_leggings/kristallwache_60` | `DIAMOND_LEGGINGS` | 60 | Rüstung |
| 24 | Kristallwache-Stiefel | `pixelrpg:armor/diamond_boots` | `pixelrpg:armor/diamond_boots/kristallwache_60` | `DIAMOND_BOOTS` | 60 | Rüstung |
| 25 | Höllenschmiede-Helm | `pixelrpg:armor/netherite_helmet` | `pixelrpg:armor/netherite_helmet/hoellenschmiede_80` | `NETHERITE_HELMET` | 60 | Rüstung |
| 26 | Höllenschmiede-Brustplatte | `pixelrpg:armor/netherite_chestplate` | `pixelrpg:armor/netherite_chestplate/hoellenschmiede_80` | `NETHERITE_CHESTPLATE` | 60 | Rüstung |
| 27 | Höllenschmiede-Beinschutz | `pixelrpg:armor/netherite_leggings` | `pixelrpg:armor/netherite_leggings/hoellenschmiede_80` | `NETHERITE_LEGGINGS` | 60 | Rüstung |
| 28 | Höllenschmiede-Stiefel | `pixelrpg:armor/netherite_boots` | `pixelrpg:armor/netherite_boots/hoellenschmiede_80` | `NETHERITE_BOOTS` | 60 | Rüstung |
| 29 | Admin-Relikt | `pixelrpg:unique/admin_relic` | `pixelrpg:unique/admin_relic` | `MACE` | 60 | Unique/Admin |
| 30 | Feuerball | `pixelrpg:weapons/fireball` | `pixelrpg:weapons/fireball/alchemist_35` | `FIRE_CHARGE` | 35 | Waffe |

## 2. Boss-Rewards

| # | Name | Resourcepack-ID | Material | Level | Typ |
|---:|---|---|---|---:|---|
| 1 | Plünderer-Siegel | `pixelrpg:boss/pluenderer_siegel` | `BRUSH` | 12 | Boss-Reward |
| 2 | Bienenkönigin-Siegel | `pixelrpg:boss/bienenkoenigin` | `BRUSH` | 16 | Boss-Reward |
| 3 | Hexenkessel | `pixelrpg:boss/hexenkessel` | `BRUSH` | 22 | Boss-Reward |
| 4 | Knarzendes Herzstück | `pixelrpg:boss/knarzendes_herzstueck` | `BRUSH` | 26 | Boss-Reward |
| 5 | Dschungel-Amulett | `pixelrpg:boss/dschungel_amulett` | `BRUSH` | 30 | Boss-Reward |
| 6 | Sumpftrank | `pixelrpg:boss/sumpftrank` | `BRUSH` | 32 | Boss-Reward |
| 7 | Husk-Siegel | `pixelrpg:boss/husk_siegel` | `BRUSH` | 34 | Boss-Reward |
| 8 | Ravager-Trophäe | `pixelrpg:boss/ravager_trophaee` | `BRUSH` | 38 | Boss-Reward |
| 9 | Goldenes Fossil | `pixelrpg:boss/goldenes_fossil` | `BRUSH` | 40 | Boss-Reward |
| 10 | Frostwolf-Fang | `pixelrpg:boss/frostwolf_fang` | `BRUSH` | 42 | Boss-Reward |
| 11 | Wildhorn | `pixelrpg:boss/wildhorn` | `BRUSH` | 44 | Boss-Reward |
| 12 | Frostpfeil-Köcher | `pixelrpg:boss/frostpfeil_koecher` | `BRUSH` | 45 | Boss-Reward |
| 13 | Spinnenauge des Jägers | `pixelrpg:boss/spinnenauge_des_jaegers` | `BRUSH` | 46 | Boss-Reward |
| 14 | Horn des Berges | `pixelrpg:boss/horn_des_berges` | `BRUSH` | 48 | Boss-Reward |
| 15 | Blütenhonig | `pixelrpg:boss/bluetenhonig` | `BRUSH` | 50 | Boss-Reward |
| 16 | Flusskiesel | `pixelrpg:boss/flusskiesel` | `BRUSH` | 50 | Boss-Reward |
| 17 | Kapitäns-Nautilus | `pixelrpg:boss/kapitaens_nautilus` | `BRUSH` | 52 | Boss-Reward |
| 18 | Myzelkern | `pixelrpg:boss/myzelkern` | `BRUSH` | 55 | Boss-Reward |
| 19 | Auge der Tiefe | `pixelrpg:boss/auge_der_tiefe` | `BRUSH` | 58 | Boss-Reward |
| 20 | Echoherz | `pixelrpg:boss/echoherz` | `BRUSH` | 60 | Boss-Reward |
| 21 | Netherkern | `pixelrpg:boss/netherkern` | `BRUSH` | 60 | Boss-Reward |
| 22 | Karmesinherz | `pixelrpg:boss/karmesinherz` | `BRUSH` | 60 | Boss-Reward |
| 23 | Gebundene Enderperle | `pixelrpg:boss/gebundene_enderperle` | `BRUSH` | 60 | Boss-Reward |
| 24 | Seelenfragment | `pixelrpg:boss/seelenfragment` | `BRUSH` | 60 | Boss-Reward |
| 25 | Magmaherz | `pixelrpg:boss/magmaherz` | `BRUSH` | 60 | Boss-Reward |
| 26 | Shulkerkern | `pixelrpg:boss/shulkerkern` | `BRUSH` | 60 | Boss-Reward |
| 27 | Risskern | `pixelrpg:boss/risskern` | `BRUSH` | 60 | Boss-Reward |
| 28 | Sturmherz | `pixelrpg:boss/sturmherz` | `BRUSH` | 60 | Boss-Reward |
| 29 | Abgrundkern | `pixelrpg:boss/abgrundkern` | `BRUSH` | 60 | Boss-Reward |
| 30 | Seelenkrone | `pixelrpg:boss/seelenkrone` | `BRUSH` | 60 | Boss-Reward |
| 31 | Endriss | `pixelrpg:boss/endriss` | `BRUSH` | 60 | Boss-Reward |
| 32 | Weltenherz | `pixelrpg:boss/weltenherz` | `BRUSH` | 60 | Boss-Reward |

## 3. Food

| # | Name | Resourcepack-ID | Material | Level | Typ |
|---:|---|---|---|---:|---|
| 1 | Mehl | `pixelrpg:food/mehl` | `CLOCK` | 1 | Food |
| 2 | Speck (roh) | `pixelrpg:food/speck_roh` | `CLOCK` | 1 | Food |
| 3 | Speck (gebraten) | `pixelrpg:food/speck_gebraten` | `CLOCK` | 1 | Food |
| 4 | Karottensuppe | `pixelrpg:food/karottensuppe` | `CLOCK` | 1 | Food |
| 5 | Kartoffel mit Speck | `pixelrpg:food/kartoffel_mit_speck` | `CLOCK` | 1 | Food |
| 6 | Hühnersuppe | `pixelrpg:food/huehnersuppe` | `CLOCK` | 1 | Food |
| 7 | Pilzfanne | `pixelrpg:food/pilzfanne` | `CLOCK` | 1 | Food |
| 8 | Apfelmus | `pixelrpg:food/apfelmus` | `CLOCK` | 1 | Food |
| 9 | Kürbisbrot | `pixelrpg:food/kuerbisbrot` | `CLOCK` | 1 | Food |
| 10 | Beeren-Muffin | `pixelrpg:food/beeren_muffin` | `CLOCK` | 1 | Food |
| 11 | Keks mit Speck | `pixelrpg:food/keks_mit_speck` | `CLOCK` | 1 | Food |
| 12 | Süßbeeren-Marmelade | `pixelrpg:food/suessbeeren_marmelade` | `CLOCK` | 1 | Food |
| 13 | Getrocknete Melone | `pixelrpg:food/getrocknete_melone` | `CLOCK` | 1 | Food |
| 14 | Tasse heißen Kakao | `pixelrpg:food/heisser_kakao` | `CLOCK` | 1 | Food |
| 15 | Melonensaft | `pixelrpg:food/melonensaft` | `CLOCK` | 1 | Food |
| 16 | Leuchtbeerentee | `pixelrpg:food/leuchtbeerentee` | `CLOCK` | 1 | Food |
| 17 | Kürbis-Latte | `pixelrpg:food/kuerbis_latte` | `CLOCK` | 1 | Food |
| 18 | Spiegelei mit Speck | `pixelrpg:food/spiegelei_mit_speck` | `CLOCK` | 1 | Food |
| 19 | Honig mit Milch | `pixelrpg:food/honig_mit_milch` | `CLOCK` | 1 | Food |
| 20 | Kaktus-Saft | `pixelrpg:food/kaktus_saft` | `CLOCK` | 1 | Food |
| 21 | Apfelkuchen | `pixelrpg:food/apfelkuchen` | `CLOCK` | 1 | Food |
| 22 | Spiegelei | `pixelrpg:food/spiegelei` | `CLOCK` | 1 | Food |
| 23 | Käse | `pixelrpg:food/kaese` | `CLOCK` | 1 | Food |

## 4. Resourcepack-Struktur

Die IDs können direkt als Namespaced Keys verwendet werden:

```text
pixelrpg:weapons/iron_sword
pixelrpg:armor/copper_helmet
pixelrpg:boss/pluenderer_siegel
pixelrpg:food/mehl
```

Entsprechend sollte das Resourcepack die zugehörigen Item-Modelle unter dem Namespace `pixelrpg` bereitstellen.

### Namensräume

| Namespace-Bereich | Anzahl | Verwendung |
|---|---:|---|
| `pixelrpg:weapons/*` | 5 | definierte Waffen inkl. Feuerball |
| `pixelrpg:armor/*` | 24 | definierte Rüstungsteile |
| `pixelrpg:unique/*` | 1 | Admin-/Unique-Item |
| `pixelrpg:boss/*` | 32 | Boss-Rewards |
| `pixelrpg:food/*` | 23 | Food |

**Wichtig:** Die Resourcepack-ID ist nicht identisch mit der vollständigen Plugin-Item-ID. Bei Waffen/Rüstung/Boss-Items können mehrere konkrete Plugin-Definitionen dieselbe Resourcepack-ID verwenden. Das Resourcepack-Modell beschreibt damit die visuelle Variante, während die Plugin-Item-ID die konkrete RPG-Definition identifiziert.
