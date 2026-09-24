package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.core.Level;

import java.util.UUID;

/**
 * Mutable in-memory player state. Persistence uses immutable snapshots so async I/O never observes
 * a partially mutated live profile.
 */
public final class PlayerProfile {
    private final UUID uniqueId;
    private boolean registered;
    private long experience;
    private long moneyMinorUnits;
    private int storyChapter = -1;
    private long persistenceRevision;
    private long mutationRevision;
    private boolean dirty;
    private Runnable dirtyCallback;

    public PlayerProfile(UUID id) {
        uniqueId = id;
    }

    public synchronized UUID uniqueId() { return uniqueId; }
    public synchronized boolean registered() { return registered; }
    public synchronized long experience() { return experience; }
    public synchronized long moneyMinorUnits() { return moneyMinorUnits; }
    public synchronized double money() { return moneyMinorUnits / 100.0D; }
    public synchronized int level() { return Level.fromExperience(experience); }
    public synchronized int storyChapter() { return storyChapter; }
    public synchronized long revision() { return persistenceRevision; }
    public synchronized long mutationRevision() { return mutationRevision; }
    public synchronized boolean isDirty() { return dirty; }

    public void registered(boolean value) { mutate(() -> registered = value); }
    public void experience(long value) { mutate(() -> experience = Math.max(0L, value)); }
    public void moneyMinorUnits(long value) { mutate(() -> moneyMinorUnits = Math.max(0L, value)); }
    public void storyChapter(int value) { mutate(() -> storyChapter = value); }
    public void revision(long value) { synchronized (this) { persistenceRevision = Math.max(0L, value); } }

    public void addExperience(long amount) {
        if (amount <= 0L) return;
        mutate(() -> experience = amount > Long.MAX_VALUE - experience
                ? Long.MAX_VALUE : experience + amount);
    }

    public boolean depositMinorUnits(long amount) {
        if (amount < 0L) return false;
        boolean changed = false;
        synchronized (this) {
            if (Long.MAX_VALUE - moneyMinorUnits >= amount) {
                moneyMinorUnits += amount;
                dirty = true;
                changed = amount != 0L;
            }
        }
        if (changed) notifyDirty();
        return amount == 0L || changed;
    }

    public boolean withdrawMinorUnits(long amount) {
        if (amount < 0L) return false;
        boolean changed = false;
        synchronized (this) {
            if (moneyMinorUnits >= amount) {
                moneyMinorUnits -= amount;
                dirty = true;
                changed = amount != 0L;
            }
        }
        if (changed) notifyDirty();
        return amount == 0L || changed;
    }

    public void setDirtyCallback(Runnable callback) {
        synchronized (this) { dirtyCallback = callback; }
    }

    public void markDirty() {
        synchronized (this) { dirty = true; }
        notifyDirty();
    }

    public void markClean() {
        synchronized (this) { dirty = false; }
    }

    public void markCleanIfRevision(long expectedMutationRevision) {
        synchronized (this) {
            if (mutationRevision == expectedMutationRevision) {
                dirty = false;
            }
        }
    }

    public synchronized PlayerProfile snapshotForSave() {
        if (!dirty) return null;
        PlayerProfile snapshot = new PlayerProfile(uniqueId);
        snapshot.registered = registered;
        snapshot.experience = experience;
        snapshot.moneyMinorUnits = moneyMinorUnits;
        snapshot.storyChapter = storyChapter;
        snapshot.persistenceRevision = persistenceRevision;
        snapshot.mutationRevision = mutationRevision;
        snapshot.dirty = false;
        return snapshot;
    }

    public void resetProgress() {
        synchronized (this) {
            registered = false;
            experience = 0L;
            moneyMinorUnits = 0L;
            storyChapter = -1;
            dirty = true;
        }
        notifyDirty();
    }

    private void mutate(Runnable mutation) {
        synchronized (this) {
            mutation.run();
            dirty = true;
        }
        notifyDirty();
    }

    private void notifyDirty() {
        Runnable callback;
        synchronized (this) {
            mutationRevision++;
            callback = dirtyCallback;
        }
        if (callback != null) callback.run();
    }
}
