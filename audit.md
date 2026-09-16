# PixelRPG forensic cleanup audit

## Baseline

- Reference branch: `main`
- Baseline commit: `f27713ca803c10ecea2880c9060959c5a2c06c3e`
- Cleanup branch: `forensic-clean-rebuild`
- Scope: complete `src/` tree, build configuration, plugin descriptor, runtime wiring, recent repository history and current Paper 26.2 target assumptions.

## Runtime / architecture verification

- `paper-plugin.yml` is the active plugin descriptor; no legacy `plugin.yml` exists under `src/main/resources`.
- Command wiring was traced beyond `RootCommand`: `GuildManager` also registers the four legacy-named guild aliases through the Paper lifecycle API. Those command classes are therefore live and were retained.
- `WakeScheduler` is live through `QuestPassiveCheckTask` and was retained.
- `AsyncFileWriter` is live through `TradeDepotManager` and was retained.
- `ExternalSkinService` is live through `MannequinSkinResolver` and was retained.
- `BossRewardItemListener` is live because `BossManager` registers it and was retained.
- `QuestNavigationService` and `QuestNavigationLifecycleListener` are live through `QuestPassiveCheckTask` and were retained.
- `CompanionBossRewardListener` is live because `CompanionService` registers it and was retained.
- `CharacterCardScoreboardService` is live because `QuickActionsDialogListener` owns, starts and stops it for the native character-card path and was retained.

## Dead-code findings

No class-level orphan from the investigated `src/main` candidates survived the integration check. Several files initially looked orphaned when viewed only from the central command/plugin entry point, but their actual lifecycle registration was found in secondary owners and they were restored before final verification.

One genuinely unused private helper was removed from `CharacterCardScoreboardService`: the unused `score(Player, String)` method and its now-unneeded `Score` import.

## Region cleanup

`RegionRepository` contained a no-op legacy flag migration hook plus obsolete pre-v3 flag inversion. The current region flag format reads current `RegionFlag` values directly. The dead migration hook and inversion were removed while retaining the generic format-version persistence mechanism.

## History cross-check

Recent repository history was used to distinguish genuinely live systems from stale leftovers. In particular, several earlier cleanup commits removed obsolete systems; the current tree was checked against those changes before deleting anything. Live replacements were followed to their current owners rather than removing classes solely because their names looked legacy.

## Build verification

CI is required to pass the repository's existing Gradle build and source/artifact boundary checks before this branch is considered complete. No existing dependency or verification task was intentionally removed or weakened.
