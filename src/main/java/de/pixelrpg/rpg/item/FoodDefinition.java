package de.pixelrpg.rpg.item;

import java.util.Locale;
import java.util.Objects;

public record FoodDefinition(String id,int nutrition,float saturation,boolean canAlwaysEat,String effectType,int effectDurationSeconds,int effectAmplifier) {
    public FoodDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(effectType);
        id=normalize(id); effectType=effectType.trim().toUpperCase(Locale.ROOT);
        if(nutrition<0||nutrition>20||saturation<0||effectDurationSeconds<0||effectAmplifier<0) throw new IllegalArgumentException("Invalid food definition");
        if(effectType.isBlank()&&(effectDurationSeconds!=0||effectAmplifier!=0)) throw new IllegalArgumentException("Food effect requires effectType");
    }
    private static String normalize(String value){String n=value.trim().toLowerCase(Locale.ROOT);return n.startsWith("pixelrpg:")?n:"pixelrpg:"+n;}
}