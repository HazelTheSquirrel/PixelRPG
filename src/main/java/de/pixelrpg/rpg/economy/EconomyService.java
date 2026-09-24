package de.pixelrpg.rpg.economy;

import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import java.util.UUID;

public final class EconomyService implements EconomyAPI {
    private final PlayerProfileManager profiles;
    public EconomyService(PlayerProfileManager profiles) { this.profiles = profiles; }
    public double getBalance(UUID id) { PlayerProfile p = profiles.get(id); return p == null ? 0.0D : Money.toMajor(p.moneyMinorUnits()); }
    public void deposit(UUID id, double amount) {
        long minor = Money.fromMajor(amount);
        if (minor == 0L) return;
        PlayerProfile p = require(id);
        if (!p.depositMinorUnits(minor)) throw new IllegalArgumentException("Balance overflow");
    }
    public boolean withdraw(UUID id, double amount) {
        long minor = Money.fromMajor(amount);
        if (minor == 0L) return true;
        PlayerProfile p = require(id);
        return p.withdrawMinorUnits(minor);
    }
    private PlayerProfile require(UUID id) {
        PlayerProfile p = profiles.get(id);
        if (p == null) throw new IllegalStateException("Profile not loaded: " + id);
        return p;
    }
}
