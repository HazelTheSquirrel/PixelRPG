# Berufe & Crafting-Rezepte

Diese Datei ist eine **Bauanleitung für die Rezeptdaten von PixelRPG**.

Die eigentliche Datenquelle ist:

`src/main/resources/data/recipes/crafting-recipes.json`

Die Einträge werden vom Crafting-System als `CraftRecipe` geladen und verarbeitet. Wenn du neue Rezepte oder ganze Rezeptketten bauen möchtest, musst du die JSON-Struktur in `crafting-recipes.json` einhalten.

---

## 1. Grundstruktur der Datei

Die Datei besitzt genau einen Wurzelknoten `recipes`:

```json
{
  "recipes": [
    {
      "id": "farmer:seed_bundle",
      "label": "Saatgutbündel",
      "profession": "FARMER",
      "result": "WHEAT_SEEDS",
      "resultItemId": "pixelrpg:farmer:seed_bundle",
      "costs": {
        "WHEAT": 2
      },
      "requiredProfessionLevel": 1,
      "unlockPrice": 0,
      "rarity": "COMMON",
      "unlockedByDefault": true
    }
  ]
}
```

**Wichtig:** Die JSON-Datei ist die technische Wahrheit. Diese Dokumentation ist eine Hilfe zum Erstellen und Planen von Rezepten.

---

## 2. Felder eines Rezeptes

| Feld | Pflicht | Bedeutung |
|---|---|---|
| `id` | ja | Eindeutige Rezept-ID, z. B. `farmer:seed_bundle` |
| `label` | ja | Anzeigename des Rezeptes |
| `profession` | ja | Beruf als Enum, z. B. `FARMER` |
| `result` | ja | Bukkit-Material des Ergebnisses |
| `resultItemId` | ja | PixelRPG-ID des hergestellten Gegenstandes |
| `costs` | ja | Normale Minecraft-Materialkosten |
| `itemCosts` | nein | Bereits hergestellte PixelRPG-Gegenstände als Kosten |
| `resultAmount` | nein | Anzahl des erzeugten Ergebnisses; Standard ist 1 |
| `requiredProfessionLevel` | ja | Benötigtes Berufslevel |
| `unlockPrice` | ja | Preis zum Freischalten |
| `rarity` | ja | Maximale Zielseltenheit |
| `unlockedByDefault` | ja | `true` = sofort verfügbar, `false` = muss freigeschaltet werden |
| `potionType` | nein | Basistrank für spezielle Tränke |
| `enchantment` | nein | Verzauberung für ein Enchanted Book |
| `enchantmentLevel` | nein | Level der Verzauberung |

---

## 3. IDs richtig aufbauen

### Rezept-ID

Das Schema ist:

```text
beruf:rezept_name
```

Beispiele:

```text
farmer:seed_bundle
woodcutter:plank_bundle
blacksmith:steel_core
alchemist:master_elixir
```

Die ID sollte klein geschrieben und stabil bleiben. Wenn andere Rezepte auf dieses Rezept verweisen, darfst du sie später nicht einfach ändern.

### Ergebnis-ID

`resultItemId` ist die eindeutige PixelRPG-ID des erzeugten Gegenstandes:

```json
"resultItemId": "pixelrpg:farmer:seed_bundle"
```

Das ist wichtig für spätere Rezeptketten und für die Resource-Pack-Verknüpfung.

---

## 4. Normale Materialkosten: `costs`

Normale Minecraft-Materialien kommen in `costs`:

```json
"costs": {
  "IRON_INGOT": 3,
  "COAL": 2
}
```

Die Schlüssel sind Bukkit-Materialnamen.

Beispiel:

```json
"costs": {
  "WHEAT": 6,
  "WHEAT_SEEDS": 4
}
```

Das bedeutet: 6 Weizen und 4 Weizensamen werden aus dem Inventar entfernt.

---

## 5. Rezeptketten: `itemCosts`

Hier entsteht die eigentliche Rezeptkette.

Ein Rezept kann als Material ein **bereits hergestelltes PixelRPG-Item** verlangen:

```json
"itemCosts": {
  "pixelrpg:farmer:seed_bundle": 2
}
```

Das bedeutet:

```text
farmer:seed_bundle
        ↓ 2 Stück
farmer:crop_bundle
```

Mehrere vorherige Rezepte sind möglich:

```json
"itemCosts": {
  "pixelrpg:farmer:crop_bundle": 2,
  "pixelrpg:woodcutter:plank_bundle": 1
}
```

Damit entsteht:

```text
farmer:crop_bundle ─────┐
                        ├──> farmer:orchard_crate
woodcutter:plank_bundle ┘
```

### Wichtig bei `itemCosts`

Verwende die **kanonische Item-ID** des vorherigen Rezeptes. Die Rezept-ID selbst wird vom System ebenfalls aufgelöst, aber für neue Einträge solltest du die vollständige Form verwenden:

```text
pixelrpg:<profession>:<recipe>
```

Beispiel:

```text
"itemCosts": {
  "pixelrpg:blacksmith:steel_core": 2
}
```

---

## 6. Eine komplette Rezeptkette selbst bauen

Eine einfache Kette kann so aussehen:

```text
Stufe 1
Rohstoff
  ↓
Stufe 20
Zwischenprodukt I
  ↓
Stufe 40
Zwischenprodukt II
  ↓
Stufe 60
Fortgeschrittenes Produkt
  ↓
Stufe 80
Meisterprodukt
  ↓
Stufe 100
Endprodukt
```

### Beispiel: eigene Schmiedekette

#### 1. Grundprodukt

```json
{
  "id": "blacksmith:iron_fitting",
  "label": "Eisenbeschlag",
  "profession": "BLACKSMITH",
  "result": "IRON_NUGGET",
  "resultItemId": "pixelrpg:blacksmith:iron_fitting",
  "costs": {
    "IRON_INGOT": 1
  },
  "requiredProfessionLevel": 1,
  "unlockPrice": 0,
  "rarity": "COMMON",
  "unlockedByDefault": true
}
```

#### 2. Erstes Zwischenprodukt

```json
{
  "id": "blacksmith:iron_toolhead",
  "label": "Eisenwerkzeugkopf",
  "profession": "BLACKSMITH",
  "result": "IRON_INGOT",
  "resultItemId": "pixelrpg:blacksmith:iron_toolhead",
  "costs": {
    "IRON_INGOT": 2,
    "COAL": 1
  },
  "itemCosts": {
    "pixelrpg:blacksmith:iron_fitting": 2
  },
  "requiredProfessionLevel": 20,
  "unlockPrice": 400,
  "rarity": "COMMON",
  "unlockedByDefault": false
}
```

#### 3. Nächste Stufe

```json
{
  "id": "blacksmith:steel_core",
  "label": "Stahlkern",
  "profession": "BLACKSMITH",
  "result": "IRON_INGOT",
  "resultItemId": "pixelrpg:blacksmith:steel_core",
  "costs": {
    "IRON_INGOT": 3,
    "COAL": 2
  },
  "itemCosts": {
    "pixelrpg:blacksmith:iron_toolhead": 2
  },
  "requiredProfessionLevel": 40,
  "unlockPrice": 1200,
  "rarity": "UNCOMMON",
  "unlockedByDefault": false
}
```

#### 4. Produkt aus der Kette

```json
{
  "id": "blacksmith:forged_pickaxe",
  "label": "Geschmiedete Spitzhacke",
  "profession": "BLACKSMITH",
  "result": "IRON_PICKAXE",
  "resultItemId": "pixelrpg:blacksmith:forged_pickaxe",
  "costs": {
    "STICK": 2
  },
  "itemCosts": {
    "pixelrpg:blacksmith:steel_core": 2,
    "pixelrpg:woodcutter:plank_bundle": 1
  },
  "requiredProfessionLevel": 60,
  "unlockPrice": 2500,
  "rarity": "RARE",
  "unlockedByDefault": false
}
```

Hier sieht man auch eine **berufsübergreifende Kette**:

```text
WOODCUTTER
plank_bundle
     │
     ├──────────────┐
     │              │
BLACKSMITH          │
iron_fitting        │
     ↓              │
iron_toolhead       │
     ↓              │
steel_core ─────────┘
     ↓
forged_pickaxe
```

---

## 7. `resultAmount`

Wenn mehr als ein Item erzeugt werden soll:

```json
"resultAmount": 4
```

Beispiel:

```json
{
  "id": "woodcutter:plank_bundle",
  "label": "Bretterbündel",
  "profession": "WOODCUTTER",
  "result": "OAK_PLANKS",
  "resultItemId": "pixelrpg:woodcutter:plank_bundle",
  "costs": {
    "OAK_LOG": 2
  },
  "itemCosts": {
    "pixelrpg:woodcutter:log_bundle": 2
  },
  "resultAmount": 4,
  "requiredProfessionLevel": 20,
  "unlockPrice": 400,
  "rarity": "COMMON",
  "unlockedByDefault": false
}
```

Ohne `resultAmount` wird effektiv 1 Stück erzeugt.

---

## 8. Seltenheit

Aktuell verwendete Werte:

```text
COMMON
UNCOMMON
RARE
EPIC
LEGENDARY
UNIQUE
```

`UNIQUE` kann laut Crafting-Service nicht hergestellt werden. Für normale herstellbare Rezepte verwendest du daher höchstens `LEGENDARY`.

Beispiel:

```json
"rarity": "EPIC"
```

---

## 9. Freischaltung und Berufslevel

Ein typisches Rezept sieht so aus:

```json
"requiredProfessionLevel": 60,
"unlockPrice": 2500,
"unlockedByDefault": false
```

Bedeutung:

- Der Spieler braucht Berufslevel 60.
- Das Rezept muss zusätzlich freigeschaltet werden.
- Die Freischaltung kostet 2500.

Ein Startrezept:

```json
"requiredProfessionLevel": 1,
"unlockPrice": 0,
"unlockedByDefault": true
```

Die aktuellen vorhandenen Rezeptketten verwenden überwiegend die Stufen:

```text
1 → 20 → 40 → 60 → 80 → 100
```

Das ist eine bestehende Struktur, keine technische Pflicht. Für neue Rezepte kannst du andere Level verwenden, sofern sie zum gewünschten Progressionsaufbau passen.

---

## 10. Spezialrezepte: Tränke

Ein Trank kann zusätzlich `potionType` verwenden:

```json
{
  "id": "alchemist:healing",
  "label": "Heilelixier",
  "profession": "ALCHEMIST",
  "result": "POTION",
  "resultItemId": "pixelrpg:alchemist:healing",
  "costs": {
    "GLISTERING_MELON_SLICE": 1
  },
  "itemCosts": {
    "pixelrpg:alchemist:herbal_extract": 1
  },
  "requiredProfessionLevel": 20,
  "unlockPrice": 400,
  "rarity": "COMMON",
  "unlockedByDefault": false,
  "potionType": "HEALING"
}
```

Der `potionType`-Wert wird als `PotionType` aufgelöst und muss deshalb ein gültiger Wert sein.

---

## 11. Spezialrezepte: Verzaubertes Buch

Für ein Enchanted Book wird zusätzlich `enchantment` und `enchantmentLevel` verwendet:

```json
{
  "id": "scholar:master_manual",
  "label": "Meisterhandbuch",
  "profession": "SCHOLAR",
  "result": "ENCHANTED_BOOK",
  "resultItemId": "pixelrpg:scholar:master_manual",
  "costs": {
    "LAPIS_LAZULI": 5,
    "BOOK": 1
  },
  "itemCosts": {
    "pixelrpg:scholar:arcane_tome": 1
  },
  "requiredProfessionLevel": 80,
  "unlockPrice": 5000,
  "rarity": "EPIC",
  "unlockedByDefault": false,
  "enchantment": "efficiency",
  "enchantmentLevel": 5
}
```

Für diese Variante muss `result` bzw. das tatsächliche Ergebnis `ENCHANTED_BOOK` sein.

---

## 12. Berufsbezeichnungen

Die aktuell in den Rezeptdaten verwendeten Profession-Enums sind:

| Anzeigename | JSON-Wert |
|---|---|
| Farmer | `FARMER` |
| Holzfäller | `WOODCUTTER` |
| Schmied | `BLACKSMITH` |
| Gelehrter | `SCHOLAR` |
| Koch | `COOK` |
| Schneider | `TAILOR` |
| Alchemist | `ALCHEMIST` |
| Maurer | `MASON` |
| Fischer | `FISHERMAN` |

Beim Erstellen eines Rezeptes muss `profession` exakt dem technischen Enum-Wert entsprechen.

---

## 13. Bestehende Rezeptketten

### Farmer

```text
farmer:seed_bundle
  ↓
farmer:crop_bundle
  ↓
farmer:orchard_crate
  ↓
farmer:golden_honey
  ↓
farmer:master_seed
  ↓
farmer:harvest_core
```

`farmer:orchard_crate` verwendet zusätzlich `woodcutter:plank_bundle`.

### Holzfäller

```text
woodcutter:log_bundle
  ↓
woodcutter:plank_bundle
  ↓
woodcutter:work_crate
  ↓
woodcutter:reinforced_crate
  ↓
woodcutter:carpenter_kit
  ↓
woodcutter:master_frame
```

### Schmied

```text
blacksmith:iron_fitting
  ↓
blacksmith:iron_toolhead
  ↓
blacksmith:steel_core
  ├──> blacksmith:forged_pickaxe
  └──> blacksmith:forged_sword
          ↓
      blacksmith:master_armor
```

Die Spitzhacke benötigt außerdem `woodcutter:plank_bundle`.

### Gelehrter

```text
scholar:paper_bundle
  ↓
scholar:weapon_manual
  ↓
scholar:research_notes
  ↓
scholar:arcane_tome
  ↓
scholar:master_manual
  ↓
scholar:world_archive
```

### Koch

```text
cook:basic_dough
  ↓
cook:hearty_meal
  ↓
cook:fishermans_platter
  ↓
cook:golden_feast
  ↓
cook:hero_feast
  ↓
cook:legendary_feast
```

Die Kette greift zusätzlich auf Farmer-, Fischer-, Alchemist- und Gelehrten-Produkte zu.

### Schneider

```text
tailor:thread_bundle
  ↓
tailor:cloth_roll
  ↓
tailor:leather_kit
  ↓
tailor:reinforced_cloth
  ↓
tailor:traveler_armor
  ↓
tailor:master_cloak
```

### Alchemist

```text
alchemist:herbal_extract
  ↓
alchemist:healing
  ↓
alchemist:arcane_catalyst
  ↓
alchemist:strong_healing
  ↓
alchemist:master_elixir
  ↓
alchemist:grand_elixir
```

### Maurer

```text
mason:stone_brick
  ↓
mason:cut_stone
  ↓
mason:reinforced_brick
  ↓
mason:prismarine_frame
  ↓
mason:obsidian_pillar
  ↓
mason:master_monument
```

### Fischer

```text
fisherman:fish_crate
  ↓
fisherman:salmon_pack
  ↓
fisherman:prismarine_fang
  ↓
fisherman:rare_scale
  ↓
fisherman:deep_catch
  ↓
fisherman:legendary_catch
```

---

## 14. Neue Rezeptkette erstellen – Kurzvorlage

Wenn du selbst eine neue Kette bauen willst, kannst du diese Vorlage kopieren:

```json
{
  "id": "profession:recipe_name",
  "label": "Anzeigename",
  "profession": "PROFESSION",
  "result": "MINECRAFT_MATERIAL",
  "resultItemId": "pixelrpg:profession:recipe_name",
  "costs": {
    "MATERIAL_A": 1,
    "MATERIAL_B": 2
  },
  "itemCosts": {
    "pixelrpg:profession:previous_recipe": 1
  },
  "resultAmount": 1,
  "requiredProfessionLevel": 20,
  "unlockPrice": 400,
  "rarity": "COMMON",
  "unlockedByDefault": false
}
```

Wenn kein fertiges PixelRPG-Item benötigt wird, lässt du `itemCosts` weg.

Wenn nur ein Stück erzeugt wird, kannst du `resultAmount` weglassen.

---

## 15. Checkliste vor dem Speichern

- [ ] `id` ist eindeutig.
- [ ] `profession` ist ein gültiger Profession-Enum-Wert.
- [ ] `result` ist ein gültiges Material.
- [ ] `resultItemId` ist eindeutig und folgt `pixelrpg:<profession>:<name>`.
- [ ] Alle `costs` sind echte Materialnamen.
- [ ] Alle `itemCosts` verweisen auf existierende PixelRPG-Rezeptprodukte.
- [ ] Keine versehentliche Endlosschleife in der Rezeptkette.
- [ ] `requiredProfessionLevel` passt zur gewünschten Progression.
- [ ] `unlockPrice` passt zur gewünschten Freischaltung.
- [ ] `rarity` ist gültig.
- [ ] `unlockedByDefault` ist bewusst gesetzt.
- [ ] Bei Tränken ist `potionType` gültig.
- [ ] Bei Enchanted Books sind `enchantment` und `enchantmentLevel` korrekt.

**Wichtig:** Eine Rezeptkette ist technisch ein gerichteter Verweis über `itemCosts`. Das vorherige Rezept muss also ein Item mit passender `resultItemId` erzeugen. Genau diese ID wird anschließend als Eingabe des nächsten Rezeptes verwendet.
