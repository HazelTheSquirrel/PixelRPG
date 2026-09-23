package de.pixelrpg.rpg.quest;

public final class QuestProgress {
    private final String questId;
    private int currentAmount;
    private final long expiryTimestampMillis;
    private Runnable dirtyCallback;

    public QuestProgress(String questId,int currentAmount,long expiryTimestampMillis){
        if(questId==null||questId.isBlank()) throw new IllegalArgumentException("questId must not be blank");
        this.questId=questId;
        this.currentAmount=Math.max(0,currentAmount);
        this.expiryTimestampMillis=Math.max(0L,expiryTimestampMillis);
    }
    public String getQuestId(){return questId;}
    public synchronized int getCurrentAmount(){return currentAmount;}
    public void setCurrentAmount(int amount){Runnable callback;synchronized(this){if(currentAmount==amount)return;currentAmount=Math.max(0,amount);callback=dirtyCallback;}if(callback!=null)callback.run();}
    public long getExpiryTimestampMillis(){return expiryTimestampMillis;}
    public boolean hasExpiry(){return expiryTimestampMillis>0L;}
    public boolean isExpired(){return hasExpiry()&&System.currentTimeMillis()>=expiryTimestampMillis;}
    public synchronized void setDirtyCallback(Runnable callback){dirtyCallback=callback;}
}
