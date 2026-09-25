package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.economy.Money;
import de.pixelrpg.rpg.equipment.EquipmentSlot;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.quest.QuestProgress;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID uuid;
    private boolean registered;
    private long experience;
    private long moneyMinorUnits;
    private final Map<Profession, Integer> professionLevels = new EnumMap<>(Profession.class);
    private final Map<Profession, Long> professionExperience = new EnumMap<>(Profession.class);
    private final Set<Profession> learnedProfessions = new HashSet<>();
    private final Set<String> unlockedRecipes = new HashSet<>();
    private final Set<String> unlockedWaypoints = new HashSet<>();
    private int storyChapterIndex;
    private final Map<String, QuestProgress> activeQuests = new HashMap<>();
    private final Map<String, NavigationTarget> questNavigationTargets = new HashMap<>();
    private final Set<String> completedQuests = new HashSet<>();
    private final Map<String, Long> statistics = new HashMap<>();
    private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    private boolean scoreboardEnabled;
    private boolean partyHudEnabled;
    private boolean questTrackerEnabled;
    private long playtimeMillis;
    private long persistenceRevision;
    private long mutationRevision;
    private boolean dirty;
    private Runnable dirtyCallback;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.registered = false;
        this.experience = 0L;
        this.moneyMinorUnits = 0L;
        this.storyChapterIndex = -1;
        this.scoreboardEnabled = true;
        this.partyHudEnabled = false;
        this.questTrackerEnabled = false;
        this.playtimeMillis = 0L;
        this.persistenceRevision = 0L;
        this.mutationRevision = 0L;
        this.dirty = false;
        for (Profession profession : Profession.values()) {
            professionLevels.put(profession, Profession.MIN_LEVEL);
            professionExperience.put(profession, 0L);
        }
    }

    public UUID getUuid() { return uuid; }
    public synchronized long getPersistenceRevision() { return persistenceRevision; }
    synchronized void setPersistenceRevision(long revision) { persistenceRevision = Math.max(0L, revision); }
    public synchronized long getMutationRevision() { return mutationRevision; }
    public synchronized boolean isRegistered() { return registered; }
    public void setRegistered(boolean value) { mutate(() -> registered = value); }
    @Deprecated(forRemoval = true) public synchronized boolean isRegisteredInGuild() { return isRegistered(); }
    @Deprecated(forRemoval = true) public synchronized void setRegisteredInGuild(boolean value) { setRegistered(value); }
    public synchronized long getExperience() { return experience; }
    public void setExperience(long value) {
        long capped = Math.clamp(value, 0L, Level.getRequiredExperience(Level.MAX_NORMAL_LEVEL));
        mutate(() -> experience = capped);
    }
    public void addExperience(long amount) {
        if (amount <= 0L) return;
        mutate(() -> {
            long cap = Level.getRequiredExperience(Level.MAX_NORMAL_LEVEL);
            long next = amount > Long.MAX_VALUE - experience ? Long.MAX_VALUE : experience + amount;
            experience = Math.min(cap, next);
        });
    }
    public synchronized int getLevel() { return Level.fromExperience(experience); }
    public synchronized double getMoney() { return Money.toMajor(moneyMinorUnits); }
    public synchronized long getMoneyMinorUnits() { return moneyMinorUnits; }
    public void setMoney(double value) { mutate(() -> moneyMinorUnits = Money.fromMajor(value)); }
    public void setMoneyMinorUnits(long value) { if (value < 0L) throw new IllegalArgumentException("Money minor units must be non-negative"); mutate(() -> moneyMinorUnits = value); }
    public void addMoney(double amount) { if (!Double.isFinite(amount) || amount <= 0.0D) return; long delta = Money.fromMajor(amount); mutate(() -> moneyMinorUnits = delta > Long.MAX_VALUE - moneyMinorUnits ? Long.MAX_VALUE : moneyMinorUnits + delta); }
    public boolean removeMoney(double amount) { if (!Double.isFinite(amount) || amount <= 0.0D) return false; long delta = Money.fromMajor(amount); synchronized (this) { if (delta > moneyMinorUnits) return false; moneyMinorUnits -= delta; } notifyDirty(); return true; }
    public synchronized int getProfessionLevel(Profession profession) { return professionLevels.getOrDefault(profession, Profession.MIN_LEVEL); }
    public void setProfessionLevel(Profession profession, int level) { if (profession == null) return; mutate(() -> professionLevels.put(profession, Math.clamp(level, Profession.MIN_LEVEL, Profession.MAX_LEVEL))); }
    public synchronized Map<Profession, Integer> getProfessionLevels() { return Collections.unmodifiableMap(new EnumMap<>(professionLevels)); }
    public synchronized long getProfessionExperience(Profession profession) { return professionExperience.getOrDefault(profession, 0L); }
    public void setProfessionExperience(Profession profession, long value) { if (profession == null) return; mutate(() -> professionExperience.put(profession, Math.clamp(value, 0L, Profession.MAX_EXPERIENCE))); }
    public void addProfessionExperience(Profession profession, long amount) { if (profession == null || amount <= 0L) return; mutate(() -> { long current = professionExperience.getOrDefault(profession, 0L); long next = amount > Long.MAX_VALUE - current ? Long.MAX_VALUE : current + amount; professionExperience.put(profession, Math.min(Profession.MAX_EXPERIENCE, next)); }); }
    public synchronized Map<Profession, Long> getProfessionExperiences() { return Collections.unmodifiableMap(new EnumMap<>(professionExperience)); }
    public synchronized boolean hasLearnedProfession(Profession profession) { return profession != null && learnedProfessions.contains(profession); }
    public void learnProfession(Profession profession) { if (profession != null) mutateIfChanged(() -> learnedProfessions.add(profession)); }
    public synchronized Set<Profession> getLearnedProfessions() { return Collections.unmodifiableSet(new HashSet<>(learnedProfessions)); }
    public synchronized boolean hasUnlockedRecipe(String recipeId) { return recipeId != null && unlockedRecipes.contains(recipeId.toLowerCase()); }
    public void unlockRecipe(String recipeId) { if (recipeId != null) mutateIfChanged(() -> unlockedRecipes.add(recipeId.toLowerCase())); }
    public synchronized Set<String> getUnlockedRecipes() { return Collections.unmodifiableSet(new HashSet<>(unlockedRecipes)); }
    public void setUnlockedRecipes(Set<String> recipeIds) { mutate(() -> { unlockedRecipes.clear(); if (recipeIds != null) recipeIds.stream().filter(java.util.Objects::nonNull).map(String::toLowerCase).forEach(unlockedRecipes::add); }); }
    public synchronized Set<String> getUnlockedWaypoints() { return Collections.unmodifiableSet(new HashSet<>(unlockedWaypoints)); }
    public synchronized boolean hasUnlockedWaypoint(String id) { return id != null && unlockedWaypoints.contains(id); }
    public void unlockWaypoint(String id) { if (id != null) mutateIfChanged(() -> unlockedWaypoints.add(id)); }
    public void setUnlockedWaypoints(Set<String> ids) { mutate(() -> { unlockedWaypoints.clear(); if (ids != null) unlockedWaypoints.addAll(ids); }); }
    public synchronized int getStoryChapterIndex() { return storyChapterIndex; }
    public void setStoryChapterIndex(int value) { mutate(() -> storyChapterIndex = value); }
    public synchronized Map<String, QuestProgress> getActiveQuests() { return Collections.unmodifiableMap(new HashMap<>(activeQuests)); }
    public synchronized boolean hasActiveQuest(String id) { return id != null && activeQuests.containsKey(id); }
    public synchronized NavigationTarget getQuestNavigationTarget(String questId) { return questId == null ? null : questNavigationTargets.get(questId); }
    public synchronized Map<String, NavigationTarget> getQuestNavigationTargets() { return Collections.unmodifiableMap(new HashMap<>(questNavigationTargets)); }
    public void setQuestNavigationTarget(String questId, NavigationTarget target) {
        if (questId == null || questId.isBlank() || target == null) return;
        mutate(() -> questNavigationTargets.put(questId, target));
    }
    public void clearQuestNavigationTarget(String questId) { if (questId == null) return; mutateIfChanged(() -> questNavigationTargets.remove(questId) != null); }
    public void startQuest(QuestProgress progress) { if (progress == null) return; synchronized (this) { progress.setDirtyCallback(this::markDirty); activeQuests.put(progress.getQuestId(), progress); } notifyDirty(); }
    public void removeActiveQuest(String id) { if (id == null) return; synchronized (this) { if (activeQuests.remove(id) == null) return; } notifyDirty(); }
    public synchronized Set<String> getCompletedQuests() { return Collections.unmodifiableSet(new HashSet<>(completedQuests)); }
    public synchronized boolean hasCompletedQuest(String id) { return id != null && completedQuests.contains(id); }
    public void markQuestCompleted(String id) { if (id != null) mutateIfChanged(() -> completedQuests.add(id)); }
    public void setCompletedQuests(Set<String> ids) { mutate(() -> { completedQuests.clear(); if (ids != null) completedQuests.addAll(ids); }); }
    public synchronized long getStatistic(String key) { return statistics.getOrDefault(key, 0L); }
    public void setStatistic(String key, long value) { if (key == null) return; mutate(() -> statistics.put(key, value)); }
    public void incrementStatistic(String key, long amount) { if (key == null || amount == 0L) return; mutate(() -> { long current = statistics.getOrDefault(key, 0L); statistics.put(key, amount > 0L && current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : amount < 0L && current < Long.MIN_VALUE - amount ? Long.MIN_VALUE : current + amount); }); }
    public synchronized Map<String, Long> getAllStatistics() { return Collections.unmodifiableMap(new HashMap<>(statistics)); }
    public synchronized Map<EquipmentSlot, ItemStack> getEquipment() { Map<EquipmentSlot, ItemStack> copy = new EnumMap<>(EquipmentSlot.class); equipment.forEach((slot, item) -> copy.put(slot, item.clone())); return Collections.unmodifiableMap(copy); }
    public void setEquipment(Map<EquipmentSlot, ItemStack> values) { mutate(() -> { equipment.clear(); if (values != null) values.forEach((slot, item) -> { if (slot != null && item != null && !item.isEmpty()) equipment.put(slot, item.clone()); }); }); }
    public synchronized boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public void setScoreboardEnabled(boolean value) { mutate(() -> scoreboardEnabled = value); }
    public synchronized boolean isPartyHudEnabled() { return partyHudEnabled; }
    public void setPartyHudEnabled(boolean value) { mutate(() -> partyHudEnabled = value); }
    public synchronized boolean isQuestTrackerEnabled() { return questTrackerEnabled; }
    public void setQuestTrackerEnabled(boolean value) { mutate(() -> questTrackerEnabled = value); }
    public synchronized long getPlaytimeMillis() { return playtimeMillis; }
    public void addPlaytimeMillis(long amount) { if (amount <= 0L) return; mutate(() -> playtimeMillis = amount > Long.MAX_VALUE - playtimeMillis ? Long.MAX_VALUE : playtimeMillis + amount); }
    public void setPlaytimeMillis(long value) { mutate(() -> playtimeMillis = Math.max(0L, value)); }
    public synchronized boolean isDirty() { return dirty; }

    public synchronized PlayerProfile snapshotForSave() {
        if (!dirty) return null;
        PlayerProfile snapshot = new PlayerProfile(uuid);
        snapshot.registered = registered;
        snapshot.experience = experience;
        snapshot.moneyMinorUnits = moneyMinorUnits;
        snapshot.professionLevels.clear();
        snapshot.professionLevels.putAll(professionLevels);
        snapshot.professionExperience.clear();
        snapshot.professionExperience.putAll(professionExperience);
        snapshot.learnedProfessions.addAll(learnedProfessions);
        snapshot.unlockedRecipes.addAll(unlockedRecipes);
        snapshot.unlockedWaypoints.addAll(unlockedWaypoints);
        snapshot.storyChapterIndex = storyChapterIndex;
        for (QuestProgress progress : activeQuests.values()) snapshot.activeQuests.put(progress.getQuestId(), new QuestProgress(progress.getQuestId(), progress.getCurrentAmount(), progress.getExpiryTimestampMillis()));
        snapshot.questNavigationTargets.putAll(questNavigationTargets);
        snapshot.completedQuests.addAll(completedQuests);
        snapshot.statistics.putAll(statistics);
        equipment.forEach((slot, item) -> snapshot.equipment.put(slot, item.clone()));
        snapshot.scoreboardEnabled = scoreboardEnabled;
        snapshot.partyHudEnabled = partyHudEnabled;
        snapshot.questTrackerEnabled = questTrackerEnabled;
        snapshot.playtimeMillis = playtimeMillis;
        snapshot.persistenceRevision = persistenceRevision;
        snapshot.mutationRevision = mutationRevision;
        snapshot.dirty = false;
        return snapshot;
    }

    public synchronized void setDirtyCallback(Runnable callback) { dirtyCallback = callback; }
    public void markDirty() { synchronized (this) { dirty = true; } notifyDirty(); }
    public synchronized void markClean() { dirty = false; }
    public synchronized void markCleanIfRevision(long expectedRevision) { if (mutationRevision == expectedRevision) dirty = false; }
    public void resetProgress() {
        synchronized (this) {
            registered = false;
            experience = 0L;
            moneyMinorUnits = 0L;
            storyChapterIndex = -1;
            scoreboardEnabled = true;
            partyHudEnabled = false;
            questTrackerEnabled = false;
            learnedProfessions.clear();
            unlockedRecipes.clear();
            unlockedWaypoints.clear();
            activeQuests.clear();
            completedQuests.clear();
            questNavigationTargets.clear();
            equipment.clear();
            for (Profession profession : Profession.values()) {
                professionLevels.put(profession, Profession.MIN_LEVEL);
                professionExperience.put(profession, 0L);
            }
        }
        notifyDirty();
    }

    private void mutate(Runnable mutation) { synchronized (this) { mutation.run(); dirty = true; } notifyDirty(); }
    private void mutateIfChanged(java.util.function.BooleanSupplier mutation) { boolean changed; synchronized (this) { changed = mutation.getAsBoolean(); if (changed) dirty = true; } if (changed) notifyDirty(); }
    public record NavigationTarget(UUID worldId, double x, double y, double z, String structureKey) { }
    private void notifyDirty() { Runnable callback; synchronized (this) { mutationRevision++; callback = dirtyCallback; } if (callback != null) callback.run(); }
}