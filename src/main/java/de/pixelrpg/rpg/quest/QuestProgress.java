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
    public synchronized int getCurrentAmount() { return currentAmount; }

    public void setCurrentAmount(int currentAmount) {
        Runnable callback;
        synchronized (this) {
            if (this.currentAmount == currentAmount) return;
            this.currentAmount = currentAmount;
            callback = dirtyCallback;
        }
        if (callback != null) callback.run();
    }

    public synchronized long getExpiryTimestampMillis() { return expiryTimestampMillis; }
    public synchronized boolean hasExpiry() { return expiryTimestampMillis > 0L; }
    public synchronized boolean isExpired() { return hasExpiry() && System.currentTimeMillis() >= expiryTimestampMillis; }

    public synchronized void setDirtyCallback(Runnable dirtyCallback) {
        this.dirtyCallback = dirtyCallback;
    }
}
