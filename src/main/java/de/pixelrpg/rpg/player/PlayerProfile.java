package de.pixelrpg.rpg.player;
import de.pixelrpg.rpg.core.Level;
import java.util.UUID;
public final class PlayerProfile {
 private final UUID uniqueId; private boolean registered; private long experience; private long moneyMinorUnits; private int storyChapter=-1; private long revision;
 public PlayerProfile(UUID id){uniqueId=id;} public UUID uniqueId(){return uniqueId;} public boolean registered(){return registered;} public long experience(){return experience;} public long moneyMinorUnits(){return moneyMinorUnits;} public double money(){return moneyMinorUnits/100.0D;} public int level(){return Level.fromExperience(experience);} public int storyChapter(){return storyChapter;} public long revision(){return revision;}
 public void registered(boolean v){registered=v;} public void experience(long v){experience=Math.max(0,v);} public void moneyMinorUnits(long v){moneyMinorUnits=Math.max(0,v);} public void storyChapter(int v){storyChapter=v;} public void revision(long v){revision=Math.max(0,v);}
 public boolean depositMinorUnits(long v){if(v<0||Long.MAX_VALUE-moneyMinorUnits<v)return false;moneyMinorUnits+=v;return true;} public boolean withdrawMinorUnits(long v){if(v<0||moneyMinorUnits<v)return false;moneyMinorUnits-=v;return true;}
}
