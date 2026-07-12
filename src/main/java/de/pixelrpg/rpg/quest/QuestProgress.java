// src/main/java/de/pixelrpg/rpg/quest/QuestProgress.java
package de.pixelrpg.rpg.quest;

public final class QuestProgress {

    private final String questId;
    private int currentAmount;
    private long expiryTimestampMillis;

    public QuestProgress(String questId, int currentAmount, long expiryTimestampMillis) {
        this.questId = questId;
        this.currentAmount = currentAmount;
        this.expiryTimestampMillis = expiryTimestampMillis;
    }

    public String getQuestId() {
        return questId;
    }

    public int getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
    }

    public long getExpiryTimestampMillis() {
        return expiryTimestampMillis;
    }

    public boolean hasExpiry() {
        return expiryTimestampMillis > 0L;
    }

    public boolean isExpired() {
        return hasExpiry() && System.currentTimeMillis() >= expiryTimestampMillis;
    }
}