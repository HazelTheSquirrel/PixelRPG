package de.pixelrpg.rpg.quest;

public final class QuestProgress {

    private final String questId;
    private int currentAmount;
    private long expiryTimestampMillis;
    private Runnable dirtyCallback;

    public QuestProgress(String questId, int currentAmount, long expiryTimestampMillis) {
        this.questId = questId;
        this.currentAmount = currentAmount;
        this.expiryTimestampMillis = expiryTimestampMillis;
    }

    public String getQuestId() { return questId; }
    public int getCurrentAmount() { return currentAmount; }

    public void setCurrentAmount(int currentAmount) {
        if (this.currentAmount == currentAmount) return;
        this.currentAmount = currentAmount;
        if (dirtyCallback != null) dirtyCallback.run();
    }

    public long getExpiryTimestampMillis() { return expiryTimestampMillis; }
    public boolean hasExpiry() { return expiryTimestampMillis > 0L; }
    public boolean isExpired() { return hasExpiry() && System.currentTimeMillis() >= expiryTimestampMillis; }

    public void setDirtyCallback(Runnable dirtyCallback) {
        this.dirtyCallback = dirtyCallback;
    }
}