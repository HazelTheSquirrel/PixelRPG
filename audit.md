# PixelRPG forensic cleanup audit

## Baseline

- Reference branch: `main`
- Baseline commit: `f27713ca803c10ecea2880c9060959c5a2c06c3e`
- Cleanup branch: `forensic-clean-rebuild`
- Scope: complete `src/` tree, build configuration, plugin descriptor, runtime wiring, recent repository history and current Paper 26.2 target assumptions.

## Runtime / architecture verification

- `paper-plugin.yml` is the active plugin descriptor; no legacy `plugin.yml` exists under `src/main/resources`.
- Current command registration is centralized through `RootCommand` plus the explicitly wired subcommands in `PixelRPGPlugin`.
- `WakeScheduler` is live through `QuestPassiveCheckTask` and was retained.
- `AsyncFileWriter` is live through `TradeDepotManager` and was retained.
- `ExternalSkinService` is live through `MannequinSkinResolver` and was retained.
- `BossRewardItemListener` is live because `BossManager` registers it and was retained.
- `QuestNavigationService` and `QuestNavigationLifecycleListener` are live through `QuestPassiveCheckTask` and were retained.

## Removed orphaned source

The following classes were present in `main` but had no active registration/integration path in the current architecture:

- `src/main/java/de/pixelrpg/rpg/command/GuildAcceptCommand.java`
- `src/main/java/de/pixelrpg/rpg/command/GuildInfoCommand.java`
- `src/main/java/de/pixelrpg/rpg/command/GuildInviteCommand.java`
- `src/main/java/de/pixelrpg/rpg/command/GuildLeaveCommand.java`
- `src/main/java/de/pixelrpg/rpg/companion/CompanionBossRewardListener.java`
- `src/main/java/de/pixelrpg/rpg/dialogue/CharacterCardScoreboardService.java`

The legacy guild commands are superseded by `GuildSubCommand`. The companion boss listener is not registered; boss defeat events remain available to the active event architecture. The character-card scoreboard service has no active owner/registration path in the current native-dialog implementation.

## Region cleanup

`RegionRepository` contained a no-op legacy flag migration hook plus obsolete pre-v3 flag inversion. The current region flag format reads current `RegionFlag` values directly. The dead migration hook and inversion were removed while retaining the generic format-version persistence mechanism.

## History cross-check

Recent repository history was used to distinguish genuinely live systems from stale leftovers. In particular, several earlier cleanup commits removed obsolete systems; the current tree was checked against those changes before deleting anything. Live replacements were followed to their current owners rather than removed solely because a class name looked legacy.

## Verification status

The branch must pass the repository's existing Gradle `check`/CI verification before this cleanup is considered complete. No existing verification task or dependency was removed or weakened by this cleanup.
