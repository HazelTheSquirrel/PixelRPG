# PixelRPG Resource Pack

Stand: 2026-08-23

> Aktuelle technische Dokumentation des Resourcepacks. Der Gesamtprojektstand steht in `../audit.md`.

## Status

Das Resourcepack ist ein laufender Arbeitsbereich und **nicht abgeschlossen**.

Es dient ausschließlich als visuelle Ebene für PixelRPG. Gameplay-Logik gehört nicht hier hinein.

## Art Direction

- Hand-painted Pixel-Art
- Medieval European Fantasy
- Dark Oak, aged iron, leather, brass and stone
- klare Silhouetten und gut erkennbare Item-Icons
- keine Sci-Fi-, modernen oder Neon-Optiken
- Rarity-Darstellung bleibt bewusst dezent und wird primär über Spiel-/Item-UI vermittelt

## Struktur

`assets/minecraft/` enthält nur bewusst gewählte Vanilla-Overrides.

`assets/pixelrpg/` enthält PixelRPG-eigene Assets und Custom-Model-/Texture-Dateien.

`source/` enthält editierbare Ausgangsdaten. Minecraft Java lädt SVG nicht direkt; finale PNG-Dateien gehören in die entsprechenden `assets/*/textures/` Verzeichnisse.

## Aktueller Prüfpunkt

Nach dem Gameplay-Cleanup müssen vorhandene Assets gegen die aktuelle Runtime geprüft werden.

Insbesondere:

- Mana-bezogene Assets klassifizieren.
- keine verwaisten Gameplay-Referenzen behalten.
- neue PixelRPG-Items nur zusammen mit ihrer tatsächlichen Gameplay-Identität dokumentieren.
- Vanilla-Overrides nur bewusst und begründet hinzufügen.

## Trennung

```text
resourcepack/
    ↓
Visuals / Models / Textures / Language

src/
    ↓
Gameplay / Java / Daten / Serverlogik
```

Keine Java-, YAML-, Datenbank- oder Serverlogik in diesem Verzeichnis.

## Arbeitsregel

Neue Assets werden erst als Teil des aktuellen Systems betrachtet, wenn ihre Verwendung im Plugin nachvollziehbar ist.
