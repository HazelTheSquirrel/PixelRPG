package de.pixelrpg.rpg.profession;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.util.Optional;
import java.util.UUID;
/** Handles profession learning, recipe unlocking, XP and level progression. */
public final class ProfessionService{
 private final PlayerProfileManager profiles;
 public ProfessionService(PlayerProfileManager profiles){this.profiles=profiles;}
 public long getExperience(UUID id,Profession p){return profiles.getProfile(id).map(x->x.getProfessionExperience(p)).orElse(0L);}
 public int getLevel(UUID id,Profession p){return profiles.getProfile(id).map(x->x.getProfessionLevel(p)).orElse(Profession.MIN_LEVEL);}
 public boolean hasLearned(UUID id,Profession p){return profiles.getProfile(id).map(x->x.hasLearnedProfession(p)).orElse(false);}
 public boolean hasRecipe(UUID id,String recipe){return profiles.getProfile(id).map(x->x.hasUnlockedRecipe(recipe)).orElse(false);}
 public boolean learn(Player player,Profession p){Optional<PlayerProfile> o=profiles.getProfile(player.getUniqueId());if(o.isEmpty()||!o.get().isRegistered()||o.get().hasLearnedProfession(p))return false;o.get().learnProfession(p);profiles.saveProfileAsync(player.getUniqueId());player.sendMessage(Component.text("Beruf erlernt: ",NamedTextColor.GREEN).append(p.displayComponent()));return true;}
 public UnlockResult unlockRecipe(Player player,CraftRecipe r){Optional<PlayerProfile> o=profiles.getProfile(player.getUniqueId());if(o.isEmpty()||!o.get().isRegistered())return UnlockResult.failure("Du bist noch nicht registriert.");PlayerProfile p=o.get();if(!p.hasLearnedProfession(r.profession()))return UnlockResult.failure("Du musst diesen Beruf zuerst erlernen.");if(p.getProfessionLevel(r.profession())<r.requiredProfessionLevel())return UnlockResult.failure("Dein Berufslevel ist noch nicht hoch genug.");if(r.unlockedByDefault()||p.hasUnlockedRecipe(r.id()))return UnlockResult.success(0);if(!r.requiredQuestId().isBlank()&&p.hasCompletedQuest(r.requiredQuestId())){p.unlockRecipe(r.id());profiles.saveProfileAsync(player.getUniqueId());return UnlockResult.success(0);}if(r.unlockPrice()>0&&p.removeMoney(r.unlockPrice())){p.unlockRecipe(r.id());profiles.saveProfileAsync(player.getUniqueId());return UnlockResult.success(r.unlockPrice());}return UnlockResult.failure(!r.requiredQuestId().isBlank()?"Benötigt Quest: "+r.requiredQuestId():"Benötigt: "+r.unlockPrice()+" Gold.");}
 public void addExperience(Player player,Profession p,long amount){if(amount<=0)return;Optional<PlayerProfile> o=profiles.getProfile(player.getUniqueId());if(o.isEmpty())return;PlayerProfile profile=o.get();if(!profile.isRegistered()||!profile.hasLearnedProfession(p))return;int before=profile.getProfessionLevel(p);long current=Math.min(profile.getProfessionExperience(p),experienceForLevel(Profession.MAX_LEVEL));long gained=Math.min(amount,experienceForLevel(Profession.MAX_LEVEL)-current);if(gained<=0)return;profile.setProfessionExperience(p,current+gained);int after=professionLevelForExperience(current+gained);if(after!=before){profile.setProfessionLevel(p,after);player.sendMessage(Component.text("Beruf ",NamedTextColor.GRAY).append(p.displayComponent()).append(Component.text(" erreicht Level "+after+"!",NamedTextColor.YELLOW)));}profiles.saveProfileAsync(player.getUniqueId());}
 public static long experienceForLevel(int level){int l=Math.clamp(level,Profession.MIN_LEVEL,Profession.MAX_LEVEL);if(l<=1)return 0;long n=l-1L;return 50L*n*n+150L*n;}
 public static int professionLevelForExperience(long xp){if(xp<=0)return 1;int low=1,high=Profession.MAX_LEVEL;while(low<high){int mid=(low+high+1)>>>1;if(xp>=experienceForLevel(mid))low=mid;else high=mid-1;}return low;}
 public record UnlockResult(boolean success,String message,long pricePaid){static UnlockResult success(long p){return new UnlockResult(true,"Rezept freigeschaltet.",p);}static UnlockResult failure(String m){return new UnlockResult(false,m,0);}}
}
