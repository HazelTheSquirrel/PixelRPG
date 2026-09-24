package de.pixelrpg.rpg.api;
import de.pixelrpg.rpg.item.ItemCategory;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.Optional;
public interface ItemAPI {
 Optional<ItemStack> createItem(Material material,ItemRarity rarity,int itemLevel);
 Optional<ItemStack> createItem(String itemId);
 boolean isRPGItem(ItemStack item);
 boolean isGuildItem(ItemStack item);
 Optional<String> getItemId(ItemStack item);
 Optional<ItemRarity> getRarity(ItemStack item);
 Optional<ItemCategory> getCategory(ItemStack item);
 Optional<ItemDefinition> getDefinition(ItemStack item);
 Optional<Integer> getRequiredLevel(ItemStack item);
 Optional<Double> getGearscore(ItemStack item);
}
