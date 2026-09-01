package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.core.Level;
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
    private double money;
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
    private boolean dirty;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.registered = false;
        this.experience = 0L;
        this.money = 0.0;
        this.storyChapterIndex = -1;
        this.scoreboardEnabled = true;
        this.partyHudEnabled = false;
        this.questTrackerEnabled = false;
        this.playtimeMillis = 0L;
        this.dirty = false;
        for (Profession profession : Profession.values()) {
            professionLevels.put(profession, Profession.MIN_LEVEL);
            professionExperience.put(profession, 0L);
        }
    }

    public UUID getUuid() { return uuid; }
    public synchronized boolean isRegistered() { return registered; }
    public synchronized void setRegistered(boolean value) { registered = value; dirty = true; }
    /** Legacy naming retained temporarily for non-guild systems already compiled against the old profile API. */
    @Deprecated(forRemoval = true)
    public synchronized boolean isRegisteredInGuild() { return isRegistered(); }
    /** Legacy naming retained temporarily; this does not represent guild membership. */
    @Deprecated(forRemoval = true)
    public synchronized void setRegisteredInGuild(boolean value) { setRegistered(value); }
    public synchronized long getExperience() { return experience; }
    public synchronized void setExperience(long value) { experience = Math.max(0L, value); dirty = true; }
    public synchronized void addExperience(long amount) {
        if (amount <= 0L) return;
        experience = amount > Long.MAX_VALUE - experience ? Long.MAX_VALUE : experience + amount;
        dirty = true;
    }
    public synchronized int getLevel() { return Level.fromExperience(experience); }
    public synchronized double getMoney() { return money; }
    public synchronized void setMoney(double value) { money = Math.max(0.0, value); dirty = true; }
    public synchronized void addMoney(double amount) { if (amount > 0.0) { money += amount; dirty = true; } }
    public synchronized boolean removeMoney(double amount) { if (amount <= 0.0 || money < amount) return false; money -= amount; dirty = true; return true; }
    public synchronized int getProfessionLevel(Profession profession) { return professionLevels.getOrDefault(profession, Profession.MIN_LEVEL); }
    public synchronized void setProfessionLevel(Profession profession, int level) { int clamped = Math.max(Profession.MIN_LEVEL, Math.min(Profession.MAX_LEVEL, level)); professionLevels.put(profession, clamped); dirty = true; }
    public synchronized Map<Profession, Integer> getProfessionLevels() { return Collections.unmodifiableMap(new EnumMap<>(professionLevels)); }
    public synchronized long getProfessionExperience(Profession profession) { return professionExperience.getOrDefault(profession, 0L); }
    public synchronized void setProfessionExperience(Profession profession, long value) { professionExperience.put(profession, Math.max(0L, value)); dirty = true; }
    public synchronized void addProfessionExperience(Profession profession, long amount) { if (amount > 0L) { professionExperience.merge(profession, amount, Long::sum); dirty = true; } }
    public synchronized Map<Profession, Long> getProfessionExperiences() { return Collections.unmodifiableMap(new EnumMap<>(professionExperience)); }
    public synchronized boolean hasLearnedProfession(Profession profession) { return learnedProfessions.contains(profession); }
    public synchronized void learnProfession(Profession profession) { if (learnedProfessions.add(profession)) dirty = true; }
    public synchronized Set<Profession> getLearnedProfessions() { return Collections.unmodifiableSet(new HashSet<>(learnedProfessions)); }
    public synchronized boolean hasUnlockedRecipe(String recipeId) { return recipeId != null && unlockedRecipes.contains(recipeId.toLowerCase()); }
    public synchronized void unlockRecipe(String recipeId) { if (recipeId != null && unlockedRecipes.add(recipeId.toLowerCase())) dirty = true; }
    public synchronized Set<String> getUnlockedRecipes() { return Collections.unmodifiableSet(new HashSet<>(unlockedRecipes)); }
    public synchronized void setUnlockedRecipes(Set<String> recipeIds) { unlockedRecipes.clear(); recipeIds.stream().map(String::toLowerCase).forEach(unlockedRecipes::add); dirty = true; }
    public synchronized Set<String> getUnlockedWaypoints() { return Collections.unmodifiableSet(new HashSet<>(unlockedWaypoints)); }
    public synchronized boolean hasUnlockedWaypoint(String id) { return unlockedWaypoints.contains(id); }
    public synchronized void unlockWaypoint(String id) { if (unlockedWaypoints.add(id)) dirty = true; }
    public synchronized void setUnlockedWaypoints(Set<String> ids) { unlockedWaypoints.clear(); unlockedWaypoints.addAll(ids); dirty = true; }
    public synchronized int getStoryChapterIndex() { return storyChapterIndex; }
    public synchronized void setStoryChapterIndex(int value) { storyChapterIndex = value; dirty = true; }
    public synchronized Map<String, QuestProgress> getActiveQuests() { return Collections.unmodifiableMap(new HashMap<>(activeQuests)); }
    public synchronized boolean hasActiveQuest(String id) { return activeQuests.containsKey(id); }
    public synchronized void startQuest(QuestProgress progress) { progress.setDirtyCallback(this::markDirty); activeQuests.put(progress.getQuestId(), progress); dirty = true; }
    public synchronized void removeActiveQuest(String id) { if (activeQuests.remove(id) != null) dirty = true; }
    public synchronized Set<String> getCompletedQuests() { return Collections.unmodifiableSet(new HashSet<>(completedQuests)); }
    public synchronized boolean hasCompletedQuest(String id) { return completedQuests.contains(id); }
    public synchronized void markQuestCompleted(String id) { if (completedQuests.add(id)) dirty = true; }
    public synchronized void setCompletedQuests(Set<String> ids) { completedQuests.clear(); completedQuests.addAll(ids); dirty = true; }
    public synchronized long getStatistic(String key) { return statistics.getOrDefault(key, 0L); }
    public synchronized void setStatistic(String key, long value) { statistics.put(key, value); dirty = true; }
    public synchronized void incrementStatistic(String key, long amount) { statistics.merge(key, amount, Long::sum); dirty = true; }
    public synchronized Map<String, Long> getAllStatistics() { return Collections.unmodifiableMap(new HashMap<>(statistics)); }

    public synchronized Map<EquipmentSlot, ItemStack> getEquipment() {
        Map<EquipmentSlot, ItemStack> copy = new EnumMap<>(EquipmentSlot.class);
        equipment.forEach((slot, item) -> copy.put(slot, item.clone()));
        return Collections.unmodifiableMap(copy);
    }

    public synchronized void setEquipment(Map<EquipmentSlot, ItemStack> values) {
        equipment.clear();
        if (values != null) values.forEach((slot, item) -> {
            if (slot != null && item != null && !item.isEmpty()) equipment.put(slot, item.clone());
        });
        dirty = true;
    }

    public synchronized boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public synchronized void setScoreboardEnabled(boolean value) { scoreboardEnabled = value; dirty = true; }
    public synchronized boolean isPartyHudEnabled() { return partyHudEnabled; }
    public synchronized void setPartyHudEnabled(boolean value) { partyHudEnabled = value; dirty = true; }
    public synchronized boolean isQuestTrackerEnabled() { return questTrackerEnabled; }
    public synchronized void setQuestTrackerEnabled(boolean value) { questTrackerEnabled = value; dirty = true; }
    public synchronized long getPlaytimeMillis() { return playtimeMillis; }
    public synchronized void addPlaytimeMillis(long amount) { if (amount > 0L) { playtimeMillis += amount; dirty = true; } }
    public synchronized void setPlaytimeMillis(long value) { playtimeMillis = value; dirty = true; }
    public synchronized boolean isDirty() { return dirty; }

    /** Creates an immutable-by-convention persistence copy and atomically acknowledges the current dirty state. */
    public synchronized PlayerProfile snapshotForSave() {
        if (!dirty) return null;
        PlayerProfile snapshot = new PlayerProfile(uuid);
        snapshot.registered = registered;
        snapshot.experience = experience;
        snapshot.money = money;
        snapshot.professionLevels.clear();
        snapshot.professionLevels.putAll(professionLevels);
        snapshot.professionExperience.clear();
        snapshot.professionExperience.putAll(professionExperience);
        snapshot.learnedProfessions.addAll(learnedProfessions);
        snapshot.unlockedRecipes.addAll(unlockedRecipes);
        snapshot.unlockedWaypoints.addAll(unlockedWaypoints);
        snapshot.storyChapterIndex = storyChapterIndex;
        for (QuestProgress progress : activeQuests.values()) {
            snapshot.activeQuests.put(progress.getQuestId(), new QuestProgress(
                    progress.getQuestId(), progress.getCurrentAmount(), progress.getExpiryTimestampMillis()));
        }
        snapshot.completedQuests.addAll(completedQuests);
        snapshot.statistics.putAll(statistics);
        equipment.forEach((slot, item) -> snapshot.equipment.put(slot, item.clone()));
        snapshot.scoreboardEnabled = scoreboardEnabled;
        snapshot.partyHudEnabled = partyHudEnabled;
        snapshot.questTrackerEnabled = questTrackerEnabled;
        snapshot.playtimeMillis = playtimeMillis;
        snapshot.dirty = false;
        dirty = false;
        return snapshot;
    }

    public synchronized void markDirty() { dirty = true; }
    public synchronized void markClean() { dirty = false; }

    public synchronized void resetProgress() {
        registered = false;
        experience = 0L;
        money = 0.0;
        storyChapterIndex = -1;
        scoreboardEnabled = true;
        partyHudEnabled = false;
        questTrackerEnabled = false;
        learnedProfessions.clear();
        unlockedRecipes.clear();
        unlockedWaypoints.clear();
        activeQuests.clear();
        completedQuests.clear();
        equipment.clear();
        for (Profession profession : Profession.values()) {
            professionLevels.put(profession, Profession.MIN_LEVEL);
            professionExperience.put(profession, 0L);
        }
        dirty = true;
    }
}
