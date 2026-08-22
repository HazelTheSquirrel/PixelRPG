# PixelRPG Resource Pack

Medieval/fantasy visual layer for PixelRPG.

## Art direction

- Hand-painted pixel-art appearance
- Medieval European fantasy
- Dark oak, aged iron, leather, brass and stone as the core material language
- Strong silhouettes and readable item icons
- No sci-fi, modern or neon aesthetic
- Rarity presentation is intentionally subtle and handled by the game UI/item presentation

## Structure

`assets/minecraft/` contains only deliberate vanilla overrides when required by the visual direction.

`assets/pixelrpg/` contains PixelRPG-owned assets and future custom-model textures.

`source/` contains editable vector/pixel-art source material. Minecraft Java does not load SVG directly; final PNG exports belong in the corresponding `assets/*/textures/` directories.

## Important

This pack is intentionally isolated from the plugin source. No Java, YAML gameplay configuration, database code, or server logic belongs in this directory.
