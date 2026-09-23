package de.pixelrpg.rpg.profession;
import de.pixelrpg.rpg.api.events.QuestCompletedEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import java.util.*;
/** Connects normal Minecraft activities with profession progression. */
public final class ProfessionActivityListener implements Listener{
 private static final int MAX_TREE_LOGS=64;
 private static final Set<Material> TREE_GROUND=Set.of(Material.GRASS_BLOCK,Material.DIRT,Material.COARSE_DIRT,Material.PODZOL,Material.MYCELIUM,Material.SAND,Material.RED_SAND,Material.MUD,Material.MOSS_BLOCK,Material.ROOTED_DIRT);
 private static final List<Material> FISHING_TREASURE=List.of(Material.BOW,Material.FISHING_ROD,Material.NAME_TAG,Material.NAUTILUS_SHELL,Material.SADDLE);
 private final ProfessionService service;private final Set<BlockPosition> automated=new HashSet<>();private final Random random=new Random();
 public ProfessionActivityListener(ProfessionService service){this.service=service;}
 // Vergibt Berufs-XP für Bergbau, Feldarbeit, Holzfällen, Steinabbau und pflanzliche Alchemie.
 @EventHandler public void onBlockBreak(BlockBreakEvent e){if(e.isCancelled())return;Player p=e.getPlayer();Material m=e.getBlock().getType();BlockPosition pos=BlockPosition.of(e.getBlock());boolean auto=automated.remove(pos);Profession prof=switch(m){
 case COAL_ORE,DEEPSLATE_COAL_ORE,IRON_ORE,DEEPSLATE_IRON_ORE,COPPER_ORE,DEEPSLATE_COPPER_ORE,GOLD_ORE,DEEPSLATE_GOLD_ORE,REDSTONE_ORE,DEEPSLATE_REDSTONE_ORE,LAPIS_ORE,DEEPSLATE_LAPIS_ORE,DIAMOND_ORE,DEEPSLATE_DIAMOND_ORE,EMERALD_ORE,DEEPSLATE_EMERALD_ORE,NETHER_GOLD_ORE,NETHER_QUARTZ_ORE,ANCIENT_DEBRIS,RAW_IRON_BLOCK,RAW_COPPER_BLOCK,RAW_GOLD_BLOCK,IRON_BLOCK,COPPER_BLOCK,GOLD_BLOCK->Profession.BLACKSMITH;
 case WHEAT,CARROTS,POTATOES,BEETROOTS,NETHER_WART,COCOA,SWEET_BERRY_BUSH,GLOW_BERRIES,KELP,SEAGRASS,TALL_SEAGRASS,SUGAR_CANE,CACTUS,BAMBOO,VINE,GLOW_LICHEN,MOSS_BLOCK,PUMPKIN,MELON->Profession.FARMER;
 case OAK_LOG,SPRUCE_LOG,BIRCH_LOG,JUNGLE_LOG,ACACIA_LOG,DARK_OAK_LOG,MANGROVE_LOG,CHERRY_LOG,PALE_OAK_LOG,CRIMSON_STEM,WARPED_STEM,OAK_WOOD,SPRUCE_WOOD,BIRCH_WOOD,JUNGLE_WOOD,ACACIA_WOOD,DARK_OAK_WOOD,MANGROVE_WOOD,CHERRY_WOOD,PALE_OAK_WOOD,CRIMSON_HYPHAE,WARPED_HYPHAE,STRIPPED_OAK_LOG,STRIPPED_SPRUCE_LOG,STRIPPED_BIRCH_LOG,STRIPPED_JUNGLE_LOG,STRIPPED_ACACIA_LOG,STRIPPED_DARK_OAK_LOG,STRIPPED_MANGROVE_LOG,STRIPPED_CHERRY_LOG,STRIPPED_PALE_OAK_LOG,STRIPPED_CRIMSON_STEM,STRIPPED_WARPED_STEM->Profession.WOODCUTTER;
 case STONE,COBBLESTONE,DEEPSLATE,COBBLED_DEEPSLATE,GRANITE,DIORITE,ANDESITE,TUFF,CALCITE,SANDSTONE,RED_SANDSTONE,BLACKSTONE,BASALT,NETHERRACK,BRICKS,STONE_BRICKS,DEEPSLATE_BRICKS,DEEPSLATE_TILES,MUD_BRICKS,PRISMARINE,PRISMARINE_BRICKS,DARK_PRISMARINE,QUARTZ_BLOCK->Profession.MASON;
 case BOOKSHELF,CHISELED_BOOKSHELF->Profession.SCHOLAR;
 case RED_MUSHROOM,BROWN_MUSHROOM,CRIMSON_FUNGUS,WARPED_FUNGUS,FLOWERING_AZALEA,AZALEA,DANDELION,POPPY,BLUE_ORCHID,ALLIUM,AZURE_BLUET,RED_TULIP,ORANGE_TULIP,WHITE_TULIP,PINK_TULIP,OXEYE_DAISY,CORNFLOWER,LILY_OF_THE_VALLEY,WITHER_ROSE,SUNFLOWER,LILAC,ROSE_BUSH,PEONY->Profession.ALCHEMIST; default->null;};
 if(prof==null)return;long xp=switch(prof){case BLACKSMITH->miningXp(m);case FARMER,WOODCUTTER,MASON,ALCHEMIST->8L;case SCHOLAR->5L;default->0L;};if(xp>0)service.addExperience(p,prof,xp);
 if(!auto&&prof==Profession.WOODCUTTER&&service.getLevel(p.getUniqueId(),Profession.WOODCUTTER)>=60&&Tag.ITEMS_AXES.isTagged(p.getInventory().getItemInMainHand().getType())&&Tag.OVERWORLD_NATURAL_LOGS.isTagged(m))fellSafeTree(p,e.getBlock());}
 // Verstärkt Holzertrag passiv abhängig vom Holzfäller-Level, ohne Vanilla-Drops zu ersetzen.
 @EventHandler public void onWoodDrop(BlockDropItemEvent e){Player p=e.getPlayer();if(!Tag.OVERWORLD_NATURAL_LOGS.isTagged(e.getBlockState().getType()))return;int level=service.getLevel(p.getUniqueId(),Profession.WOODCUTTER);if(level<20)return;double chance=level>=100?.40:level>=80?.30:level>=60?.25:.20;if(random.nextDouble()>=chance)return;Material log=e.getBlockState().getType();Item item=e.getItems().stream().filter(i->i.getItemStack().getType()==log).findFirst().orElse(null);if(item==null)return;ItemStack stack=item.getItemStack();if(stack.getAmount()<stack.getMaxStackSize())stack.setAmount(stack.getAmount()+1);}
 // Vergibt Fischer-XP und verbessert Fangmenge sowie Vanilla-Schatzchance rein passiv.
 @EventHandler public void onPlayerFish(PlayerFishEvent e){if(e.getState()!=PlayerFishEvent.State.CAUGHT_FISH)return;Player p=e.getPlayer();int level=service.getLevel(p.getUniqueId(),Profession.FISHERMAN);service.addExperience(p,Profession.FISHERMAN,18);if(!(e.getCaught() instanceof Item caught))return;ItemStack s=caught.getItemStack();if(level>=20&&random.nextDouble()<fishingDoubleChance(level)&&s.getAmount()<s.getMaxStackSize())s.setAmount(s.getAmount()+1);if(level>=40&&random.nextDouble()<fishingTreasureChance(level))caught.setItemStack(new ItemStack(FISHING_TREASURE.get(random.nextInt(FISHING_TREASURE.size()))));}
 // Vergibt Koch-XP für das Erlegen von Tieren, deren Drops als Nahrung genutzt werden können.
 @EventHandler public void onEntityDeath(EntityDeathEvent e){Player p=e.getEntity().getKiller();if(p==null)return;switch(e.getEntityType()){case COW,MOOSHROOM,PIG,CHICKEN,RABBIT,GOAT,CAMEL,HOGLIN->service.addExperience(p,Profession.COOK,12);case SHEEP->{service.addExperience(p,Profession.COOK,8);service.addExperience(p,Profession.TAILOR,8);}default->{}}}
 // Vergibt Gelehrten-XP für erfolgreiches Verzaubern von Gegenständen.
 @EventHandler public void onEnchantItem(EnchantItemEvent e){service.addExperience(e.getEnchanter(),Profession.SCHOLAR,20);}
 // Vergibt Schmied-XP, wenn ein gültiges Ergebnis aus einem Amboss genommen wird.
 @EventHandler public void onAnvilResult(InventoryClickEvent e){if(e.getView().getTopInventory().getType()!=InventoryType.ANVIL||e.getRawSlot()!=2||e.getCurrentItem()==null||e.getCurrentItem().isEmpty()||!(e.getWhoClicked() instanceof Player p))return;service.addExperience(p,Profession.BLACKSMITH,15);}
 // Vergibt berufsbezogene XP für abgeschlossene Quests anhand stabiler Quest-ID-Präfixe.
 @EventHandler public void onQuestCompleted(QuestCompletedEvent e){Profession p=professionForQuest(e.getQuestId());if(p==null||p==Profession.WOODCUTTER||p==Profession.FISHERMAN)return;service.addExperience(e.getPlayer(),p,40);}
 private long miningXp(Material m){return switch(m){case DIAMOND_ORE,DEEPSLATE_DIAMOND_ORE,EMERALD_ORE,DEEPSLATE_EMERALD_ORE,ANCIENT_DEBRIS->35;case GOLD_ORE,DEEPSLATE_GOLD_ORE,NETHER_GOLD_ORE,REDSTONE_ORE,DEEPSLATE_REDSTONE_ORE,LAPIS_ORE,DEEPSLATE_LAPIS_ORE->20;case IRON_ORE,DEEPSLATE_IRON_ORE,COPPER_ORE,DEEPSLATE_COPPER_ORE->12;default->8;};}
 private void fellSafeTree(Player p,org.bukkit.block.Block start){List<org.bukkit.block.Block> logs=collectTreeLogs(start);if(logs.size()<3||!looksLikeNaturalTree(start,logs))return;for(org.bukkit.block.Block log:logs){BlockPosition pos=BlockPosition.of(log);if(!pos.equals(BlockPosition.of(start)))automated.add(pos);}for(org.bukkit.block.Block log:logs){if(log.equals(start)||log.getType().isAir())continue;BlockPosition pos=BlockPosition.of(log);if(!automated.contains(pos))continue;if(p.breakBlock(log))automated.remove(pos);}}
 private List<org.bukkit.block.Block> collectTreeLogs(org.bukkit.block.Block start){ArrayDeque<org.bukkit.block.Block> q=new ArrayDeque<>();Set<BlockPosition> seen=new HashSet<>();List<org.bukkit.block.Block> logs=new ArrayList<>();q.add(start);seen.add(BlockPosition.of(start));while(!q.isEmpty()&&logs.size()<MAX_TREE_LOGS){var cur=q.removeFirst();if(!Tag.OVERWORLD_NATURAL_LOGS.isTagged(cur.getType()))continue;logs.add(cur);for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){if(dx==0&&dy==0&&dz==0)continue;var next=cur.getRelative(dx,dy,dz);if(seen.add(BlockPosition.of(next))&&Tag.OVERWORLD_NATURAL_LOGS.isTagged(next.getType()))q.addLast(next);}}return logs;}
 private boolean looksLikeNaturalTree(org.bukkit.block.Block start,List<org.bukkit.block.Block> logs){var base=logs.stream().min(Comparator.comparingInt(org.bukkit.block.Block::getY)).orElse(start);if(!TREE_GROUND.contains(base.getRelative(org.bukkit.block.BlockFace.DOWN).getType()))return false;int leaves=0;Set<BlockPosition> counted=new HashSet<>();for(var log:logs)for(int dx=-3;dx<=3;dx++)for(int dy=-3;dy<=3;dy++)for(int dz=-3;dz<=3;dz++){if(Math.abs(dx)+Math.abs(dy)+Math.abs(dz)>4)continue;var nearby=log.getRelative(dx,dy,dz);if(counted.add(BlockPosition.of(nearby))&&Tag.LEAVES.isTagged(nearby.getType()))leaves++;}return leaves>=3;}
 private double fishingDoubleChance(int l){if(l>=100)return .30;if(l>=80)return .20;if(l>=60)return .15;if(l>=40)return .10;return .05;}
 private double fishingTreasureChance(int l){if(l>=100)return .25;if(l>=80)return .18;if(l>=60)return .12;return .06;}
 private Profession professionForQuest(String id){if(id==null||id.isBlank())return null;String v=id.toLowerCase(Locale.ROOT);for(var e:Map.ofEntries(Map.entry("blacksmith",Profession.BLACKSMITH),Map.entry("provisioner",Profession.COOK),Map.entry("cook",Profession.COOK),Map.entry("farmer",Profession.FARMER),Map.entry("tailor",Profession.TAILOR),Map.entry("alchemist",Profession.ALCHEMIST),Map.entry("mason",Profession.MASON),Map.entry("fisherman",Profession.FISHERMAN),Map.entry("woodcutter",Profession.WOODCUTTER),Map.entry("scholar",Profession.SCHOLAR)).entrySet())if(v.startsWith(e.getKey()+".")||v.startsWith(e.getKey()+"_"))return e.getValue();return null;}
 private record BlockPosition(UUID worldId,int x,int y,int z){static BlockPosition of(org.bukkit.block.Block b){return new BlockPosition(b.getWorld().getUID(),b.getX(),b.getY(),b.getZ());}}
}
