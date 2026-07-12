// src/main/java/de/pixelrpg/rpg/player/PlayerProfile.java (VOLLSTÄNDIG, ersetzt alte Datei)
package de.pixelrpg.rpg.player;

import de.pixelrpg.rpg.core.Rank;

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
    private final Set<String> unlockedWaypoints = new HashSet<>();
    private int storyChapterIndex;
    private final Map<String, de.pixelrpg.rpg.quest.QuestProgress> activeQuests = new HashMap<>();
    private final Set<String> completedQuests = new HashSet<>();
    private final Map<String, Long> statistics = new HashMap<>();
    private final Set<String> unlockedAchievements = new HashSet<>();
    private final Set<String> unlockedTitles = new HashSet<>();
    private String selectedTitle;
    private boolean scoreboardEnabled;
    private boolean partyHudEnabled;
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
        this.selectedTitle = null;
        this.scoreboardEnabled = true;
        this.partyHudEnabled = true;
        this.playtimeMillis = 0L;
        this.dirty = false;
        for (PlayerAttribute attribute : PlayerAttribute.values()) {
            attributePoints.put(attribute, 0);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isRegisteredInGuild() {
        return registeredInGuild;
    }

    public void setRegisteredInGuild(boolean registeredInGuild) {
        this.registeredInGuild = registeredInGuild;
        this.dirty = true;
    }

    public long getExperience() {
        return experience;
    }

    public void setExperience(long experience) {
        this.experience = Math.max(0L, experience);
        this.dirty = true;
    }

    public void addExperience(long amount) {
        if (amount <= 0L) {
            return;
        }
        this.experience += amount;
        this.dirty = true;
    }

    public Rank getRank() {
        return Rank.fromExperience(experience);
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
        this.dirty = true;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = Math.max(0.0, money);
        this.dirty = true;
    }

    public void addMoney(double amount) {
        if (amount <= 0.0) {
            return;
        }
        this.money += amount;
        this.dirty = true;
    }

    public boolean removeMoney(double amount) {
        if (amount <= 0.0 || money < amount) {
            return false;
        }
        this.money -= amount;
        this.dirty = true;
        return true;
    }

    public boolean hasReceivedStartBonus() {
        return receivedStartBonus;
    }

    public void setReceivedStartBonus(boolean receivedStartBonus) {
        this.receivedStartBonus = receivedStartBonus;
        this.dirty = true;
    }

    public int getAttributePoints(PlayerAttribute attribute) {
        return attributePoints.getOrDefault(attribute, 0);
    }

    public void setAttributePoints(PlayerAttribute attribute, int value) {
        attributePoints.put(attribute, Math.max(0, value));
        this.dirty = true;
    }

    public void addAttributePoint(PlayerAttribute attribute) {
        attributePoints.merge(attribute, 1, Integer::sum);
        this.dirty = true;
    }

    public Set<String> getUnlockedWaypoints() {
        return Collections.unmodifiableSet(unlockedWaypoints);
    }

    public boolean hasUnlockedWaypoint(String waypointId) {
        return unlockedWaypoints.contains(waypointId);
    }

    public void unlockWaypoint(String waypointId) {
        if (unlockedWaypoints.add(waypointId)) {
            this.dirty = true;
        }
    }

    public void setUnlockedWaypoints(Set<String> waypointIds) {
        unlockedWaypoints.clear();
        unlockedWaypoints.addAll(waypointIds);
        this.dirty = true;
    }

    public int getStoryChapterIndex() {
        return storyChapterIndex;
    }

    public void setStoryChapterIndex(int storyChapterIndex) {
        this.storyChapterIndex = storyChapterIndex;
        this.dirty = true;
    }

    public Map<String, de.pixelrpg.rpg.quest.QuestProgress> getActiveQuests() {
        return activeQuests;
    }

    public boolean hasActiveQuest(String questId) {
        return activeQuests.containsKey(questId);
    }

    public void startQuest(de.pixelrpg.rpg.quest.QuestProgress progress) {
        activeQuests.put(progress.getQuestId(), progress);
        this.dirty = true;
    }

    public void removeActiveQuest(String questId) {
        if (activeQuests.remove(questId) != null) {
            this.dirty = true;
        }
    }

    public Set<String> getCompletedQuests() {
        return Collections.unmodifiableSet(completedQuests);
    }

    public boolean hasCompletedQuest(String questId) {
        return completedQuests.contains(questId);
    }

    public void markQuestCompleted(String questId) {
        if (completedQuests.add(questId)) {
            this.dirty = true;
        }
    }

    public void setCompletedQuests(Set<String> questIds) {
        completedQuests.clear();
        completedQuests.addAll(questIds);
        this.dirty = true;
    }

    public long getStatistic(String key) {
        return statistics.getOrDefault(key, 0L);
    }

    public void setStatistic(String key, long value) {
        statistics.put(key, value);
        this.dirty = true;
    }

    public void incrementStatistic(String key, long amount) {
        statistics.merge(key, amount, Long::sum);
        this.dirty = true;
    }

    public Map<String, Long> getAllStatistics() {
        return Collections.unmodifiableMap(statistics);
    }

    public Set<String> getUnlockedAchievements() {
        return Collections.unmodifiableSet(unlockedAchievements);
    }

    public boolean hasAchievement(String achievementId) {
        return unlockedAchievements.contains(achievementId);
    }

    public void unlockAchievement(String achievementId) {
        if (unlockedAchievements.add(achievementId)) {
            this.dirty = true;
        }
    }

    public void setUnlockedAchievements(Set<String> achievementIds) {
        unlockedAchievements.clear();
        unlockedAchievements.addAll(achievementIds);
        this.dirty = true;
    }

    public Set<String> getUnlockedTitles() {
        return Collections.unmodifiableSet(unlockedTitles);
    }

    public boolean hasTitle(String title) {
        return unlockedTitles.contains(title);
    }

    public void unlockTitle(String title) {
        if (unlockedTitles.add(title)) {
            this.dirty = true;
        }
    }

    public void setUnlockedTitles(Set<String> titles) {
        unlockedTitles.clear();
        unlockedTitles.addAll(titles);
        this.dirty = true;
    }

    public String getSelectedTitle() {
        return selectedTitle;
    }

    public boolean setSelectedTitle(String title) {
        if (title != null && !unlockedTitles.contains(title)) {
            return false;
        }
        this.selectedTitle = title;
        this.dirty = true;
        return true;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }

    public void setScoreboardEnabled(boolean scoreboardEnabled) {
        this.scoreboardEnabled = scoreboardEnabled;
        this.dirty = true;
    }

    public boolean isPartyHudEnabled() {
        return partyHudEnabled;
    }

    public void setPartyHudEnabled(boolean partyHudEnabled) {
        this.partyHudEnabled = partyHudEnabled;
        this.dirty = true;
    }

    public long getPlaytimeMillis() {
        return playtimeMillis;
    }

    public void addPlaytimeMillis(long amount) {
        if (amount <= 0L) {
            return;
        }
        this.playtimeMillis += amount;
        this.dirty = true;
    }

    public void setPlaytimeMillis(long playtimeMillis) {
        this.playtimeMillis = playtimeMillis;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void resetProgress() {
        this.registeredInGuild = false;
        this.experience = 0L;
        this.playerClass = PlayerClass.NONE;
        this.money = 0.0;
        this.storyChapterIndex = -1;
        this.unlockedWaypoints.clear();
        this.activeQuests.clear();
        this.completedQuests.clear();
        for (PlayerAttribute attribute : PlayerAttribute.values()) {
            attributePoints.put(attribute, 0);
        }
        this.dirty = true;
    }
}