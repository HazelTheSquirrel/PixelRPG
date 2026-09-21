# PixelRPG Resourcepack

Die Resourcepack-Basis ist auf Minecraft 26.2 / Resource Pack Version 88.0 ausgelegt.

Die visuelle Pipeline ist von der Plugin-Engine getrennt. NPC-Identität, Titel, Fraktion, Lore und Story liefern die semantischen Daten; das Resourcepack stellt die clientseitige visuelle Erweiterung bereit.

Geplante Erweiterungspunkte:
- NPC-/Fraktionssymbole
- Story- und Lore-Symbole
- Berufs- und Quest-Symbole
- individuelle NPC-Skins
- UI-Elemente

Die aktuelle Engine verwendet weiterhin die nativen Paper-26.2-Dialoge und benötigt für den funktionalen Dialogablauf kein Resourcepack.


## Integrierte Identitätsassets

Die Engine- und Resourcepack-Schicht enthält jetzt vier eigenständige 16×16-Pixel-Symbole für die semantischen Bereiche:

- `pixelrpg:gui/dialogue`
- `pixelrpg:gui/lore`
- `pixelrpg:gui/quest`
- `pixelrpg:gui/faction`

Dazu existieren passende Item-Modelle unter `assets/pixelrpg/models/item/`. Die Symbole sind damit als stabile Resourcepack-Assets vorhanden und können von zukünftigen nativen Dialog-/GUI-Komponenten referenziert werden.
