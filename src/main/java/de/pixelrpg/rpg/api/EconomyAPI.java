package de.pixelrpg.rpg.api;

import java.util.UUID;

public interface EconomyAPI {
    double getBalance(UUID uuid);
    void deposit(UUID uuid,double amount);
    boolean withdraw(UUID uuid,double amount);
}
