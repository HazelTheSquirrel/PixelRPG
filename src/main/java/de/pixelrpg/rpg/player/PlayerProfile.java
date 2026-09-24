package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.profession.Profession;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID uniqueId;
    private boolean registered;
    private long experience;
    private long moneyMinorUnits;
    private int storyChapter = -1;
    private final EnumMap<Profession, Integer> professionLevels = new EnumMap<>(Profession.class);
    private final EnumMap<Profession, Long> professionExperience = new EnumMap<>(Profession.class);
    private final Set<Profession> learnedProfessions = new HashSet<>();
    private final Set<String> unlockedRecipes = new HashSet<>();
    private long persistenceRevision;
    private long mutationRevision;
    private boolean dirty;
    private Runnable dirtyCallback;

    public PlayerProfile(UUID id) {
        uniqueId = id;
        for (Profession profession : Profession.values()) {
            professionLevels.put(profession, Profession.MIN_LEVEL);
            professionExperience.put(profession, 0L);
        }
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
        mutate(() -> experience = amount > Long.MAX_VALUE - experience ? Long.MAX_VALUE : experience + amount);
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

    public double getMoney() { return money(); }
    public void setMoney(double value) { moneyMinorUnits(de.pixelrpg.rpg.economy.Money.fromMajor(value)); }
    public void addMoney(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) return;
        depositMinorUnits(de.pixelrpg.rpg.economy.Money.fromMajor(amount));
    }
    public boolean removeMoney(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) return false;
        return withdrawMinorUnits(de.pixelrpg.rpg.economy.Money.fromMajor(amount));
    }

    public synchronized int getProfessionLevel(Profession profession) {
        return profession == null ? Profession.MIN_LEVEL : professionLevels.getOrDefault(profession, Profession.MIN_LEVEL);
    }

    public void setProfessionLevel(Profession profession, int level) {
        if (profession == null) return;
        mutate(() -> professionLevels.put(profession, Math.clamp(level, Profession.MIN_LEVEL, Profession.MAX_LEVEL)));
    }

    public synchronized long getProfessionExperience(Profession profession) {
        return profession == null ? 0L : professionExperience.getOrDefault(profession, 0L);
    }

    public void setProfessionExperience(Profession profession, long value) {
        if (profession == null) return;
        mutate(() -> professionExperience.put(profession, Math.max(0L, value)));
    }

    public synchronized boolean hasLearnedProfession(Profession profession) {
        return profession != null && learnedProfessions.contains(profession);
    }

    public void learnProfession(Profession profession) {
        if (profession == null) return;
        boolean changed;
        synchronized (this) {
            changed = learnedProfessions.add(profession);
            if (changed) dirty = true;
        }
        if (changed) notifyDirty();
    }

    public synchronized Set<Profession> getLearnedProfessions() {
        return Set.copyOf(learnedProfessions);
    }

    public synchronized boolean hasUnlockedRecipe(String recipeId) {
        return recipeId != null && unlockedRecipes.contains(recipeId.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public void unlockRecipe(String recipeId) {
        if (recipeId == null || recipeId.isBlank()) return;
        String normalized = recipeId.trim().toLowerCase(java.util.Locale.ROOT);
        boolean changed;
        synchronized (this) {
            changed = unlockedRecipes.add(normalized);
            if (changed) dirty = true;
        }
        if (changed) notifyDirty();
    }

    public synchronized Set<String> getUnlockedRecipes() {
        return Set.copyOf(unlockedRecipes);
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
            if (mutationRevision == expectedMutationRevision) dirty = false;
        }
    }

    public synchronized PlayerProfile snapshotForSave() {
        if (!dirty) return null;
        PlayerProfile snapshot = new PlayerProfile(uniqueId);
        snapshot.registered = registered;
        snapshot.experience = experience;
        snapshot.moneyMinorUnits = moneyMinorUnits;
        snapshot.storyChapter = storyChapter;
        snapshot.professionLevels.putAll(professionLevels);
        snapshot.professionExperience.putAll(professionExperience);
        snapshot.learnedProfessions.addAll(learnedProfessions);
        snapshot.unlockedRecipes.addAll(unlockedRecipes);
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
            learnedProfessions.clear();
            unlockedRecipes.clear();
            for (Profession profession : Profession.values()) {
                professionLevels.put(profession, Profession.MIN_LEVEL);
                professionExperience.put(profession, 0L);
            }
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
