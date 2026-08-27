# PixelRPG

## Development baseline

`main` is the functional reference and the intended gameplay state of PixelRPG.

`refactor/central-content-pipeline-v3` is an architectural refactor. Its purpose is to preserve the behavior and content of `main` while removing duplication, dead paths, obsolete implementations, and fragmented content/fallback logic.

## Refactoring rules

1. **main is IST and SOLL.** If behavior in `main` is intentional and functional, v3 must preserve it.
2. **No feature creep during refactoring.** Refactoring must not introduce new gameplay features.
3. **Unfinished systems are not completed by assumption.** If a system in `main` is only partially built, its intended completion is discussed before implementation. The refactor may clean its architecture, but it must not invent missing gameplay behavior.
4. **Preserve the Vanilla/PixelRPG coexistence model.** Unregistered players remain vanilla players. Registered players enter the PixelRPG systems. PixelRPG mechanics must not leak into the vanilla-player path unless `main` already does so intentionally.
5. **When implementations overlap, compare behavior before deleting anything.** Keep the correct parts, merge them into the appropriate owner, update all callers, and remove obsolete implementations only after their responsibilities are covered.
6. **Centralize, do not reinterpret.** A central service or data source may replace multiple implementations only when it produces the same intended result as `main`.
7. **Tests and builds are mandatory after structural changes.** A refactor is not considered complete while the branch does not build cleanly and the affected execution paths have not been checked.

## Content ownership

Central registries and services are preferred for shared content. However, centralization must not change intentional mechanics such as the custom PixelRPG crafting pipeline, PixelRPG monster loot in addition to vanilla loot, or the existing item/stat/lore construction behavior.
