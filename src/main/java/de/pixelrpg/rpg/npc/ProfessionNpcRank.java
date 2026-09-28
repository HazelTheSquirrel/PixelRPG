package de.pixelrpg.rpg.npc;

public enum ProfessionNpcRank {
    APPRENTICE("Lehrling", 0), JOURNEYMAN("Geselle", 1), EXPERT("Fachmann", 2), MASTER("Meister", 3), GRANDMASTER("Großmeister", 4);
    private final String displayName; private final int specializationCost;
    ProfessionNpcRank(String displayName,int specializationCost){this.displayName=displayName;this.specializationCost=specializationCost;}
    public String displayName(){return displayName;} public int specializationCost(){return specializationCost;}
    public boolean isGrandmaster(){return this==GRANDMASTER;}
    public ProfessionNpcRank next(){return this==GRANDMASTER?null:values()[ordinal()+1];}
}
