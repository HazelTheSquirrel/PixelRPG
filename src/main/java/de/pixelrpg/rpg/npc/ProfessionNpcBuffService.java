package de.pixelrpg.rpg.npc;

import de.pixelrpg.rpg.PixelRPGPlugin;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.profession.Profession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Applies configurable profession-specific temporary NPC buffs with per-player cooldowns. */
public final class ProfessionNpcBuffService {
    private final PixelRPGPlugin plugin;
    private final Map<UUID,Long> cooldowns=new ConcurrentHashMap<>();
    public ProfessionNpcBuffService(PixelRPGPlugin plugin){this.plugin=plugin;}
    public boolean grant(Player player,RPGNpc npc){
        if(player==null||npc==null||npc.profession()==null)return false;
        PlayerProfile profile=plugin.getPlayerProfileManager().getProfile(player.getUniqueId()).orElse(null); if(profile==null||!profile.isRegistered())return false;
        Profession profession=npc.profession(); if(profession.isMain()&&profile.getMainProfession()!=profession)return false;
        String path="kingdom.profession-buffs."+profession.name(); long now=System.currentTimeMillis(); long cooldown=plugin.getConfig().getLong(path+".cooldown-seconds",300L)*1000L; long until=cooldowns.getOrDefault(player.getUniqueId(),0L); if(now<until)return false;
        PotionEffectType type=switch(profession){case FARMER,WOODCUTTER,MOUNTAIN_MINER->PotionEffectType.HASTE;case FISHERMAN->PotionEffectType.LUCK;case BLACKSMITH->PotionEffectType.RESISTANCE;case COOK->PotionEffectType.SATURATION;case TAILOR->PotionEffectType.SPEED;case ALCHEMIST->PotionEffectType.REGENERATION;case MASON->PotionEffectType.RESISTANCE;case SCHOLAR->PotionEffectType.LUCK;};
        int amplifier=Math.max(0,plugin.getConfig().getInt(path+".amplifier-by-rank."+npc.professionNpcRank().name(),npc.professionNpcRank().ordinal()/2)); int duration=(int)Math.clamp(plugin.getConfig().getLong(path+".duration-seconds",60L)*20L,20L,2_400_000L);
        player.addPotionEffect(new PotionEffect(type,duration,amplifier,true,false,true)); cooldowns.put(player.getUniqueId(),now+cooldown); player.sendMessage(Component.text("Berufssegen: "+profession.displayName()+".",NamedTextColor.GREEN)); return true;
    }
}
