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
    private final Set<String> completedQuests = new HashSet<>();
    private final Map<String, Long> statistics = new HashMap<>();
    private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    private boolean scoreboardEnabled;
    private boolean partyHudEnabled;
    private boolean questTrackerEnabled;
    private long playtimeMillis;
    private long persistenceRevision;
    private boolean dirty;

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
        this.dirty = false;
        for (Profession profession : Profession.values()) {
            professionLevels.put(profession, Profession.MIN_LEVEL);
            professionExperience.put(profession, 0L);
        }
    }

    public UUID getUuid() { return uuid; }
    public synchronized long getPersistenceRevision() { return persistenceRevision; }
    synchronized void setPersistenceRevision(long revision) { persistenceRevision = Math.max(0L, revision); }
    public synchronized boolean isRegistered() { return registered; }
    public synchronized void setRegistered(boolean value) { registered = value; dirty = true; }
    @Deprecated(forRemoval = true) public synchronized boolean isRegisteredInGuild() { return isRegistered(); }
    @Deprecated(forRemoval = true) public synchronized void setRegisteredInGuild(boolean value) { setRegistered(value); }
    public synchronized long getExperience() { return experience; }
    public synchronized void setExperience(long value) { experience = Math.max(0L, value); dirty = true; }
    public synchronized void addExperience(long amount) { if (amount <= 0L) return; experience = amount > Long.MAX_VALUE - experience ? Long.MAX_VALUE : experience + amount; dirty = true; }
    public synchronized int getLevel() { return Level.fromExperience(experience); }

    public synchronized double getMoney() { return Money.toMajor(moneyMinorUnits); }
    public synchronized long getMoneyMinorUnits() { return moneyMinorUnits; }
    public synchronized void setMoney(double value) { moneyMinorUnits = Money.fromMajor(value); dirty = true; }
    public synchronized void setMoneyMinorUnits(long value) { if (value < 0L) throw new IllegalArgumentException("Money minor units must be non-negative"); moneyMinorUnits = value; dirty = true; }
    public synchronized void addMoney(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) return;
        long delta = Money.fromMajor(amount);
        moneyMinorUnits = delta > Long.MAX_VALUE - moneyMinorUnits ? Long.MAX_VALUE : moneyMinorUnits + delta;
        dirty = true;
    }
    public synchronized boolean removeMoney(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) return false;
        long delta = Money.fromMajor(amount);
        if (delta > moneyMinorUnits) return false;
        moneyMinorUnits -= delta;
        dirty = true;
        return true;
    }

    public synchronized int getProfessionLevel(Profession profession) { return professionLevels.getOrDefault(profession, Profession.MIN_LEVEL); }
    public synchronized void setProfessionLevel(Profession profession, int level) { if (profession == null) return; professionLevels.put(profession, Math.clamp(level, Profession.MIN_LEVEL, Profession.MAX_LEVEL)); dirty = true; }
    public synchronized Map<Profession, Integer> getProfessionLevels() { return Collections.unmodifiableMap(new EnumMap<>(professionLevels)); }
    public synchronized long getProfessionExperience(Profession profession) { return professionExperience.getOrDefault(profession, 0L); }
    public synchronized void setProfessionExperience(Profession profession, long value) { if (profession == null) return; professionExperience.put(profession, Math.max(0L, value)); dirty = true; }
    public synchronized void addProfessionExperience(Profession profession, long amount) { if (profession == null || amount <= 0L) return; long current = professionExperience.getOrDefault(profession, 0L); professionExperience.put(profession, amount > Long.MAX_VALUE - current ? Long.MAX_VALUE : current + amount); dirty = true; }
    public synchronized Map<Profession, Long> getProfessionExperiences() { return Collections.unmodifiableMap(new EnumMap<>(professionExperience)); }
    public synchronized boolean hasLearnedProfession(Profession profession) { return profession != null && learnedProfessions.contains(profession); }
    public synchronized void learnProfession(Profession profession) { if (profession != null && learnedProfessions.add(profession)) dirty = true; }
    public synchronized Set<Profession> getLearnedProfessions() { return Collections.unmodifiableSet(new HashSet<>(learnedProfessions)); }
    public synchronized boolean hasUnlockedRecipe(String recipeId) { return recipeId != null && unlockedRecipes.contains(recipeId.toLowerCase()); }
    public synchronized void unlockRecipe(String recipeId) { if (recipeId != null && unlockedRecipes.add(recipeId.toLowerCase())) dirty = true; }
    public synchronized Set<String> getUnlockedRecipes() { return Collections.unmodifiableSet(new HashSet<>(unlockedRecipes)); }
    public synchronized void setUnlockedRecipes(Set<String> recipeIds) { unlockedRecipes.clear(); if (recipeIds != null) recipeIds.stream().filter(java.util.Objects::nonNull).map(String::toLowerCase).forEach(unlockedRecipes::add); dirty = true; }
    public synchronized Set<String> getUnlockedWaypoints() { return Collections.unmodifiableSet(new HashSet<>(unlockedWaypoints)); }
    public synchronized boolean hasUnlockedWaypoint(String id) { return id != null && unlockedWaypoints.contains(id); }
    public synchronized void unlockWaypoint(String id) { if (id != null && unlockedWaypoints.add(id)) dirty = true; }
    public synchronized void setUnlockedWaypoints(Set<String> ids) { unlockedWaypoints.clear(); if (ids != null) unlockedWaypoints.addAll(ids); dirty = true; }
    public synchronized int getStoryChapterIndex() { return storyChapterIndex; }
    public synchronized void setStoryChapterIndex(int value) { storyChapterIndex = value; dirty = true; }
    public synchronized Map<String, QuestProgress> getActiveQuests() { return Collections.unmodifiableMap(new HashMap<>(activeQuests)); }
    public synchronized boolean hasActiveQuest(String id) { return id != null && activeQuests.containsKey(id); }
    public synchronized void startQuest(QuestProgress progress) { if (progress == null) return; progress.setDirtyCallback(this::markDirty); activeQuests.put(progress.getQuestId(), progress); dirty = true; }
    public synchronized void removeActiveQuest(String id) { if (id != null && activeQuests.remove(id) != null) dirty = true; }
    public synchronized Set<String> getCompletedQuests() { return Collections.unmodifiableSet(new HashSet<>(completedQuests)); }
    public synchronized boolean hasCompletedQuest(String id) { return id != null && completedQuests.contains(id); }
    public synchronized void markQuestCompleted(String id) { if (id != null && completedQuests.add(id)) dirty = true; }
    public synchronized void setCompletedQuests(Set<String> ids) { completedQuests.clear(); if (ids != null) completedQuests.addAll(ids); dirty = true; }
    public synchronized long getStatistic(String key) { return statistics.getOrDefault(key, 0L); }
    public synchronized void setStatistic(String key, long value) { if (key == null) return; statistics.put(key, value); dirty = true; }
    public synchronized void incrementStatistic(String key, long amount) { if (key == null || amount == 0L) return; long current = statistics.getOrDefault(key, 0L); statistics.put(key, amount > 0L && current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : amount < 0L && current < Long.MIN_VALUE - amount ? Long.MIN_VALUE : current + amount); dirty = true; }
    public synchronized Map<String, Long> getAllStatistics() { return Collections.unmodifiableMap(new HashMap<>(statistics)); }
    public synchronized Map<EquipmentSlot, ItemStack> getEquipment() { Map<EquipmentSlot, ItemStack> copy = new EnumMap<>(EquipmentSlot.class); equipment.forEach((slot, item) -> copy.put(slot, item.clone())); return Collections.unmodifiableMap(copy); }
    public synchronized void setEquipment(Map<EquipmentSlot, ItemStack> values) { equipment.clear(); if (values != null) values.forEach((slot, item) -> { if (slot != null && item != null && !item.isEmpty()) equipment.put(slot, item.clone()); }); dirty = true; }
    public synchronized boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public synchronized void setScoreboardEnabled(boolean value) { scoreboardEnabled = value; dirty = true; }
    public synchronized boolean isPartyHudEnabled() { return partyHudEnabled; }
    public synchronized void setPartyHudEnabled(boolean value) { partyHudEnabled = value; dirty = true; }
    public synchronized boolean isQuestTrackerEnabled() { return questTrackerEnabled; }
    public synchronized void setQuestTrackerEnabled(boolean value) { questTrackerEnabled = value; dirty = true; }
    public synchronized long getPlaytimeMillis() { return playtimeMillis; }
    public synchronized void addPlaytimeMillis(long amount) { if (amount <= 0L) return; playtimeMillis = amount > Long.MAX_VALUE - playtimeMillis ? Long.MAX_VALUE : playtimeMillis + amount; dirty = true; }
    public synchronized void setPlaytimeMillis(long value) { playtimeMillis = Math.max(0L, value); dirty = true; }
    public synchronized boolean isDirty() { return dirty; }

    public synchronized PlayerProfile snapshotForSave() {
        if (!dirty) return null;
        PlayerProfile snapshot = new PlayerProfile(uuid);
        snapshot.registered = registered;
        snapshot.experience = experience;
        snapshot.moneyMinorUnits = moneyMinorUnits;
        snapshot.professionLevels.clear(); snapshot.professionLevels.putAll(professionLevels);
        snapshot.professionExperience.clear(); snapshot.professionExperience.putAll(professionExperience);
        snapshot.learnedProfessions.addAll(learnedProfessions);
        snapshot.unlockedRecipes.addAll(unlockedRecipes);
        snapshot.unlockedWaypoints.addAll(unlockedWaypoints);
        snapshot.storyChapterIndex = storyChapterIndex;
        for (QuestProgress progress : activeQuests.values()) snapshot.activeQuests.put(progress.getQuestId(), new QuestProgress(progress.getQuestId(), progress.getCurrentAmount(), progress.getExpiryTimestampMillis()));
        snapshot.completedQuests.addAll(completedQuests);
        snapshot.statistics.putAll(statistics);
        equipment.forEach((slot, item) -> snapshot.equipment.put(slot, item.clone()));
        snapshot.scoreboardEnabled = scoreboardEnabled;
        snapshot.partyHudEnabled = partyHudEnabled;
        snapshot.questTrackerEnabled = questTrackerEnabled;
        snapshot.playtimeMillis = playtimeMillis;
        snapshot.persistenceRevision = persistenceRevision;
        snapshot.dirty = false;
        dirty = false;
        return snapshot;
    }

    public synchronized void markDirty() { dirty = true; }
    public synchronized void markClean() { dirty = false; }
    public synchronized void resetProgress() { registered = false; experience = 0L; moneyMinorUnits = 0L; storyChapterIndex = -1; scoreboardEnabled = true; partyHudEnabled = false; questTrackerEnabled = false; learnedProfessions.clear(); unlockedRecipes.clear(); unlockedWaypoints.clear(); activeQuests.clear(); completedQuests.clear(); equipment.clear(); for (Profession profession : Profession.values()) { professionLevels.put(profession, Profession.MIN_LEVEL); professionExperience.put(profession, 0L); } dirty = true; }
}
