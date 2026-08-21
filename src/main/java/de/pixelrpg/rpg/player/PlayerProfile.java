package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.profession.Profession;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerProfile {
    private final UUID uuid;
    private boolean registeredInGuild;
    private long experience;
    private PlayerClass playerClass;
    private double money;
    private boolean receivedStartBonus;
    private final Map<PlayerAttribute, Integer> attributePoints = new EnumMap<>(PlayerAttribute.class);
    private final Map<Profession, Integer> professionLevels = new EnumMap<>(Profession.class);
    private final Set<String> unlockedWaypoints = new HashSet<>();
    private int storyChapterIndex;
    private final Map<String, de.pixelrpg.rpg.quest.QuestProgress> activeQuests = new HashMap<>();
    private final Set<String> completedQuests = new HashSet<>();
    private final Map<String, Long> statistics = new HashMap<>();
    private boolean scoreboardEnabled;
    private boolean partyHudEnabled;
    private boolean questTrackerEnabled;
    private long playtimeMillis;
    private boolean dirty;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.registeredInGuild = false;
        this.experience = 0L;
        this.playerClass = PlayerClass.NONE;
        this.money = 0.0;
        this.receivedStartBonus = false;
        this.storyChapterIndex = -1;
        this.scoreboardEnabled = true;
        this.partyHudEnabled = true;
        this.questTrackerEnabled = true;
        this.playtimeMillis = 0L;
        this.dirty = false;
        for (PlayerAttribute attribute : PlayerAttribute.values()) attributePoints.put(attribute, 0);
        for (Profession profession : Profession.values()) professionLevels.put(profession, Profession.MIN_LEVEL);
    }

    public UUID getUuid() { return uuid; }
    public boolean isRegisteredInGuild() { return registeredInGuild; }
    public void setRegisteredInGuild(boolean value) { registeredInGuild = value; dirty = true; }
    public long getExperience() { return experience; }
    public void setExperience(long value) { experience = Math.max(0L, value); dirty = true; }
    public void addExperience(long amount) { if (amount > 0L) { experience += amount; dirty = true; } }
    public int getLevel() { return Level.fromExperience(experience); }

    public PlayerClass getPlayerClass() { return playerClass; }
    public void setPlayerClass(PlayerClass value) { playerClass = value; dirty = true; }
    public double getMoney() { return money; }
    public void setMoney(double value) { money = Math.max(0.0, value); dirty = true; }
    public void addMoney(double amount) { if (amount > 0.0) { money += amount; dirty = true; } }
    public boolean removeMoney(double amount) { if (amount <= 0.0 || money < amount) return false; money -= amount; dirty = true; return true; }
    public boolean hasReceivedStartBonus() { return receivedStartBonus; }
    public void setReceivedStartBonus(boolean value) { receivedStartBonus = value; dirty = true; }
    public int getAttributePoints(PlayerAttribute attribute) { return attributePoints.getOrDefault(attribute, 0); }
    public void setAttributePoints(PlayerAttribute attribute, int value) { attributePoints.put(attribute, Math.max(0, value)); dirty = true; }
    public void addAttributePoint(PlayerAttribute attribute) { attributePoints.merge(attribute, 1, Integer::sum); dirty = true; }

    public int getProfessionLevel(Profession profession) {
        return professionLevels.getOrDefault(profession, Profession.MIN_LEVEL);
    }

    public void setProfessionLevel(Profession profession, int level) {
        int clamped = Math.max(Profession.MIN_LEVEL, Math.min(Profession.MAX_LEVEL, level));
        professionLevels.put(profession, clamped);
        dirty = true;
    }

    public Map<Profession, Integer> getProfessionLevels() {
        return Collections.unmodifiableMap(professionLevels);
    }

    public Set<String> getUnlockedWaypoints() { return Collections.unmodifiableSet(unlockedWaypoints); }
    public boolean hasUnlockedWaypoint(String id) { return unlockedWaypoints.contains(id); }
    public void unlockWaypoint(String id) { if (unlockedWaypoints.add(id)) dirty = true; }
    public void setUnlockedWaypoints(Set<String> ids) { unlockedWaypoints.clear(); unlockedWaypoints.addAll(ids); dirty = true; }
    public int getStoryChapterIndex() { return storyChapterIndex; }
    public void setStoryChapterIndex(int value) { storyChapterIndex = value; dirty = true; }
    public Map<String, de.pixelrpg.rpg.quest.QuestProgress> getActiveQuests() { return activeQuests; }
    public boolean hasActiveQuest(String id) { return activeQuests.containsKey(id); }
    public void startQuest(de.pixelrpg.rpg.quest.QuestProgress progress) { activeQuests.put(progress.getQuestId(), progress); dirty = true; }
    public void removeActiveQuest(String id) { if (activeQuests.remove(id) != null) dirty = true; }
    public Set<String> getCompletedQuests() { return Collections.unmodifiableSet(completedQuests); }
    public boolean hasCompletedQuest(String id) { return completedQuests.contains(id); }
    public void markQuestCompleted(String id) { if (completedQuests.add(id)) dirty = true; }
    public void setCompletedQuests(Set<String> ids) { completedQuests.clear(); completedQuests.addAll(ids); dirty = true; }
    public long getStatistic(String key) { return statistics.getOrDefault(key, 0L); }
    public void setStatistic(String key, long value) { statistics.put(key, value); dirty = true; }
    public void incrementStatistic(String key, long amount) { statistics.merge(key, amount, Long::sum); dirty = true; }
    public Map<String, Long> getAllStatistics() { return Collections.unmodifiableMap(statistics); }
    public boolean isScoreboardEnabled() { return scoreboardEnabled; }
    public void setScoreboardEnabled(boolean value) { scoreboardEnabled = value; dirty = true; }
    public boolean isPartyHudEnabled() { return partyHudEnabled; }
    public void setPartyHudEnabled(boolean value) { partyHudEnabled = value; dirty = true; }
    public boolean isQuestTrackerEnabled() { return questTrackerEnabled; }
    public void setQuestTrackerEnabled(boolean value) { questTrackerEnabled = value; dirty = true; }
    public long getPlaytimeMillis() { return playtimeMillis; }
    public void addPlaytimeMillis(long amount) { if (amount > 0L) { playtimeMillis += amount; dirty = true; } }
    public void setPlaytimeMillis(long value) { playtimeMillis = value; dirty = true; }
    public boolean isDirty() { return dirty; }
    public void markClean() { dirty = false; }

    public void resetProgress() {
        registeredInGuild = false;
        experience = 0L;
        playerClass = PlayerClass.NONE;
        money = 0.0;
        storyChapterIndex = -1;
        unlockedWaypoints.clear();
        activeQuests.clear();
        completedQuests.clear();
        for (PlayerAttribute attribute : PlayerAttribute.values()) attributePoints.put(attribute, 0);
        for (Profession profession : Profession.values()) professionLevels.put(profession, Profession.MIN_LEVEL);
        dirty = true;
    }
}
