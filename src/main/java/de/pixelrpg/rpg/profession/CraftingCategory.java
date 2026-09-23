package de.pixelrpg.rpg.profession;
import org.bukkit.Material;
import java.util.Locale;
public enum CraftingCategory{
 TOOLS("Werkzeuge",Material.IRON_PICKAXE),WEAPONS("Waffen",Material.IRON_SWORD),ARMOR("Rüstung",Material.IRON_CHESTPLATE),MATERIALS("Materialien",Material.IRON_INGOT),CROPS("Pflanzen",Material.WHEAT),PROCESSING("Verarbeitung",Material.SUGAR),PRODUCE("Erzeugnisse",Material.HONEYCOMB),BASIC_FOOD("Grundnahrung",Material.BREAD),COOKED_FOOD("Gekochtes",Material.COOKED_BEEF),MEALS("Mahlzeiten",Material.SUSPICIOUS_STEW),SPECIALTIES("Spezialitäten",Material.GOLDEN_CARROT),TEXTILES("Textilien",Material.WHITE_WOOL),LEATHER("Leder",Material.LEATHER),DECORATION("Dekoration",Material.WHITE_BANNER),CRAFTING("Handwerk",Material.SHEARS),POTIONS("Tränke",Material.POTION),BOOKS("Bücher & Schriften",Material.BOOK),KNOWLEDGE("Wissen & Orientierung",Material.COMPASS),ARCANE("Arkane Gegenstände",Material.EXPERIENCE_BOTTLE),UTILITY("Werkzeuge & Hilfsmittel",Material.CHEST),STONE_BLOCKS("Stein & Blöcke",Material.STONE),STAIRS_SLABS("Stufen & Platten",Material.STONE_BRICK_STAIRS),CHISELED("Gemeißelte Formen",Material.CHISELED_STONE_BRICKS),SPECIAL_MATERIALS("Sondermaterialien",Material.QUARTZ_BLOCK);
 private final String displayName; private final Material icon;
 CraftingCategory(String displayName,Material icon){this.displayName=displayName;this.icon=icon;}
 public String displayName(){return displayName;} public Material icon(){return icon;}
 public static CraftingCategory from(String value,String recipeId){try{return valueOf(value.trim().toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){throw new IllegalStateException("Invalid category for "+recipeId+": "+value,e);}}
}
