package de.pixelrpg.rpg.profession;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import java.util.*;
/** Executes validated profession crafting using vanilla Minecraft item stacks. */
public final class CraftingService{
 private final ProfessionService professions;private final PlayerProfileManager profiles;private final CraftingRecipeRegistry registry;
 public CraftingService(ProfessionService professions,PlayerProfileManager profiles,CraftingRecipeRegistry registry){this.professions=Objects.requireNonNull(professions);this.profiles=Objects.requireNonNull(profiles);this.registry=Objects.requireNonNull(registry);}
 public Optional<CraftRecipe> find(String id){return registry.find(id);} public List<CraftRecipe> recipes(Profession p){return registry.getRecipes(p);}
 public ProfessionService.UnlockResult unlockRecipe(Player p,CraftRecipe r){return professions.unlockRecipe(p,r);}
 public boolean isUnlocked(Player p,CraftRecipe r){return r.unlockedByDefault()||profiles.getProfile(p.getUniqueId()).map(x->x.hasUnlockedRecipe(r.id())).orElse(false);}
 public CraftResult craft(Player player,String id){PlayerProfile profile=profiles.getProfile(player.getUniqueId()).orElse(null);if(profile==null||!profile.isRegistered())return CraftResult.failure("Du musst registriertes Rathausmitglied sein.");CraftRecipe r=find(id).orElse(null);if(r==null)return CraftResult.failure("Dieses Rezept existiert nicht.");if(!profile.hasLearnedProfession(r.profession()))return CraftResult.failure("Du hast diesen Beruf noch nicht erlernt.");if(professions.getLevel(player.getUniqueId(),r.profession())<r.requiredProfessionLevel())return CraftResult.failure("Dein Berufslevel ist für dieses Rezept zu niedrig.");if(!isUnlocked(player,r))return CraftResult.failure("Dieses Rezept wurde noch nicht freigeschaltet.");if(!hasCosts(player,r.costs()))return CraftResult.failure("Dir fehlen die benötigten Materialien.");removeCosts(player,r.costs());ItemStack result=createResult(r);player.getInventory().addItem(result).values().forEach(stack->player.getWorld().dropItemNaturally(player.getLocation(),stack));long xp=craftExperience(r);professions.addExperience(player,r.profession(),xp);return CraftResult.success(result,xp,"Herstellung erfolgreich.");}
 private boolean hasCosts(Player p,Map<Material,Integer> costs){return costs.entrySet().stream().allMatch(e->p.getInventory().contains(e.getKey(),e.getValue()));}
 private void removeCosts(Player p,Map<Material,Integer> costs){costs.forEach((m,a)->p.getInventory().removeItem(new ItemStack(m,a)));}
 private ItemStack createResult(CraftRecipe r){ItemStack result=new ItemStack(r.resultMaterial(),r.resultAmount());if(!r.potionType().isBlank()){PotionType t=PotionType.valueOf(r.potionType().toUpperCase(Locale.ROOT));PotionMeta meta=(PotionMeta)result.getItemMeta();meta.setBasePotionType(t);result.setItemMeta(meta);}if(!r.enchantment().isBlank()){var registry=RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);var enchant=registry.get(NamespacedKey.minecraft(r.enchantment().toLowerCase(Locale.ROOT)));if(enchant==null)throw new IllegalStateException("Unknown enchantment: "+r.enchantment());EnchantmentStorageMeta meta=(EnchantmentStorageMeta)result.getItemMeta();meta.addStoredEnchant(enchant,r.enchantmentLevel(),false);result.setItemMeta(meta);}return result;}
 private long craftExperience(CraftRecipe r){long count=r.costs().values().stream().mapToLong(value -> value.longValue()).sum();return Math.max(20L,r.requiredProfessionLevel()*6L+count*5L);}
 public record CraftResult(boolean success,String message,ItemStack result,long experience){static CraftResult success(ItemStack r,long xp,String m){return new CraftResult(true,m,r.clone(),xp);}static CraftResult failure(String m){return new CraftResult(false,m,null,0);}}
}
