package de.pixelrpg.rpg.combat.scaling;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.Map;

public final class MobScalingConfig {
    private double hpPerLevel=2.5D,damagePerLevel=.15D,xpPerMaxHealth=4.0D,playerParityMultiplier=1.05D,minGear=0.75D,maxGear=1.50D;
    private int nameplateDurationTicks=120;
    public void load(FileConfiguration config){hpPerLevel=config.getDouble("mob-scaling.hp-per-level",2.5D);damagePerLevel=config.getDouble("mob-scaling.damage-per-level",.15D);xpPerMaxHealth=config.getDouble("mob-scaling.xp-per-max-health",4.0D);playerParityMultiplier=config.getDouble("mob-scaling.player-parity-multiplier",1.05D);nameplateDurationTicks=config.getInt("mob-scaling.nameplate-duration-ticks",120);}
    public LevelBaseStats getBaseStats(int level){int safe=Math.clamp(level,1,99);return new LevelBaseStats(20.0D+safe*hpPerLevel,2.0D+safe*damagePerLevel);}
    public double getXpPerMaxHealth(){return xpPerMaxHealth;} public double getPlayerParityMultiplier(){return playerParityMultiplier;} public double getMinimumGearMultiplier(){return minGear;} public double getMaximumGearMultiplier(){return maxGear;} public int getNameplateDurationTicks(){return nameplateDurationTicks;}
    public record LevelBaseStats(double hp,double damage){}
}
