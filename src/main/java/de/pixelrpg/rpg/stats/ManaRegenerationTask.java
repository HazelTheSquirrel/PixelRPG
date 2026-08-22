package de.pixelrpg.rpg.stats;

/**
 * Compatibility shim for the current bootstrap. Mana is no longer a PixelRPG system;
 * this class intentionally performs no work and exists only until the bootstrap reference is removed.
 */
@Deprecated(forRemoval = true)
public final class ManaRegenerationTask {
    public ManaRegenerationTask(Object ignoredPlugin, StatEngine ignoredStatEngine, Object ignoredProfileManager) {
    }

    public void start() {
    }

    public void stop() {
    }
}
