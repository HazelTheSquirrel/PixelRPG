package de.pixelrpg.rpg.combat;

import de.pixelrpg.rpg.stats.StatEngine;

public final class CombatDamageCalculator {
    private static final double ARMOR_CONSTANT = 100.0D;
    private static final double MIN_DAMAGE = 0.1D;
    private CombatDamageCalculator() {}
    public static double rawPlayerDamage(double vanillaDamage, StatEngine.CachedStats stats) { return Math.max(MIN_DAMAGE, vanillaDamage + stats.attackPower()); }
    public static double mitigate(double damage,double armor){if(damage<=0)return 0;return Math.max(MIN_DAMAGE,damage*(ARMOR_CONSTANT/(ARMOR_CONSTANT+Math.max(0,armor))));}
    public static double crit(double damage,boolean critical){return critical?damage*2.0D:damage;}
    public static double lifesteal(double dealtDamage,double percent){return dealtDamage>0&&percent>0?dealtDamage*percent/100.0D:0;}
    public static double customDamageBeforeVanillaMitigation(double desired,double rawVanilla,double vanillaFinal){if(desired<=0)return 0;if(rawVanilla<=0||vanillaFinal<=0)return desired;return desired*(rawVanilla/vanillaFinal);}
}
