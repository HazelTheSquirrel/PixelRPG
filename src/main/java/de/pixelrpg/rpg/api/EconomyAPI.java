// src/main/java/de/pixelrpg/rpg/api/EconomyAPI.java
package de.pixelrpg.rpg.api;

import java.util.UUID;

public interface EconomyAPI {

    double getBalance(UUID uuid);

    void deposit(UUID uuid, double amount);

    boolean withdraw(UUID uuid, double amount);
}