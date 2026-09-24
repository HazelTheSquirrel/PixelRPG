package de.pixelrpg.rpg.economy;
import de.pixelrpg.rpg.api.EconomyAPI;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import java.util.UUID;
public final class EconomyService implements EconomyAPI {
 private final PlayerProfileManager profiles; public EconomyService(PlayerProfileManager p){profiles=p;}
 public double getBalance(UUID id){var p=profiles.get(id);return p==null?0:p.money();}
 public void deposit(UUID id,double amount){if(!Double.isFinite(amount)||amount<0)throw new IllegalArgumentException("amount must be non-negative");var p=require(id);if(!p.depositMinorUnits(Math.round(amount*100)))throw new IllegalArgumentException("Balance overflow");}
 public boolean withdraw(UUID id,double amount){if(!Double.isFinite(amount)||amount<0)return false;var p=require(id);return p.withdrawMinorUnits(Math.round(amount*100));}
 private de.pixelrpg.rpg.player.PlayerProfile require(UUID id){var p=profiles.get(id);if(p==null)throw new IllegalStateException("Profile not loaded: "+id);return p;}
}
