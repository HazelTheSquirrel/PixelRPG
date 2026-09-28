# PixelRPG Season 1 — Dungeon & World Content Plan

## Dungeon principles

Dungeons are handcrafted locations, not procedural arenas. Each dungeon must have:
- a clear level range,
- a story purpose,
- normal combat,
- elite encounters,
- a boss encounter,
- at least one unique material/reward,
- a reason to revisit it,
- a connection to an overworld quest or landmark.

## Season 1 dungeon list

| Dungeon | Level | Region | Story purpose | Core reward |
|---|---:|---|---|---|
| Verlassene Mine | 12–18 | Overworld mountain | Shows the collapse of old mining settlements | Bergkern |
| Archiv der Asche | 20–26 | Overworld ruins | Introduces contradictory historical records | Archivfragment |
| Versunkene Festung | 28–34 | River/lake | Connects trade routes to the old military order | Festungssiegel |
| Nether-Expedition I | 34–40 | Nether | First organized expedition into abandoned Nether infrastructure | Netherkern-Splitter |
| Zerbrochenes Observatorium | 40–46 | Overworld highlands | Connects astronomy, Strongholds and the old seal | Sternenlinse |
| Nether-Expedition II | 46–52 | Nether | Reveals deeper evidence of the historical conflict | Karmesinfragment |
| Das versiegelte Archiv | 52–58 | Stronghold region | Provides the strongest Season-1 evidence about the End seal | Siegelbruchstück |
| Weltenbruch | 58–60 | End-adjacent sealed site | Endgame Season-1 revelation without opening normal End access | Weltenfragment |

## Boss encounters

Dungeon bosses use the existing boss framework. New boss IDs and exact mechanics should be introduced through the existing BossRepository format instead of a parallel boss system.

Suggested encounters:
- Minenmeister
- Aschehüter
- Festungskommandant
- Nether-Expeditionswächter
- Observatoriumswächter
- Karmesinjäger
- Siegelhüter
- Weltenwächter

## Overworld landmarks

Season 1 should include:
- abandoned farms,
- collapsed roads,
- ruined workshops,
- old border towers,
- archive ruins,
- expedition camps,
- sealed Stronghold entrances.

These landmarks should be physically built in the world and connected to quests.

## Nether landmarks

The Nether should contain:
- abandoned expedition camps,
- broken supply depots,
- old mining tunnels,
- sealed research rooms,
- signs of repeated failed expeditions.

## Runtime boundary

The plugin provides the data/progression hooks and existing boss/quest systems. Actual handcrafted structures, exact coordinates and world editing require the test server/world and therefore remain server-side deployment work.
