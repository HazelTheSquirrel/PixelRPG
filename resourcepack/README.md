# PixelRPG Resource Pack

This resource pack directory contains the custom item models for PixelRPG.

## Texture resolutions

The resource pack supports **16x16 and 32x32 PNG textures at the same time**.

Both resolutions can be mixed freely:

- `16x16` for classic Minecraft-style pixel art.
- `32x32` for more detailed PixelRPG item art.
- Different items may use different resolutions.
- No separate resource-pack or model configuration is required for either resolution.

Minecraft determines the texture resolution from the PNG itself. The item model only references the texture path.

## Food items

The item model paths below are prepared for the 23 PixelRPG food items. Add the corresponding PNG textures to the indicated `textures/item/` paths.

Each model uses `minecraft:item/generated` and points to its own texture. The referenced PNG files may be **16x16 or 32x32 pixels**.

Namespace: `pixelrpg`