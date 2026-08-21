package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/** Handles profession learning, recipe purchases, XP and level progression. */
public final class ProfessionService {
    private final PlayerProfileManager profileManager;

    public ProfessionService(PlayerProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public long getExperience(UUID uuid, Profession profession) {
        return profileManager.getProfile(uuid).map(profile -> profile.getProfessionExperience(profession)).orElse(0L);
    }

    public int getLevel(UUID uuid, Profession profession) {
        return profileManager.getProfile(uuid).map(profile -> profile.getProfessionLevel(profession)).orElse(Profession.MIN_LEVEL);
    }

    public boolean hasLearned(UUID uuid, Profession profession) {
        return profileManager.getProfile(uuid).map(profile -> profile.hasLearnedProfession(profession)).orElse(false);
    }

    public boolean hasRecipe(UUID uuid, String recipeId) {
        return profileManager.getProfile(uuid).map(profile -> profile.hasUnlockedRecipe(recipeId)).orElse(false);
    }

    public long recipePrice(CraftRecipe recipe) {
        return Math.max(25L, recipe.requiredProfessionLevel() * 20L + recipe.craftSeconds() * 10L);
    }

    public boolean learn(Player player, Profession profession) {
        Optional<PlayerProfile> optional = profileManager.getProfile(player.getUniqueId());
        if (optional.isEmpty() || !optional.get().isRegisteredInGuild()) return false;
        PlayerProfile profile = optional.get();
        if (profile.hasLearnedProfession(profession)) return false;
        profile.learnProfession(profession);
        profileManager.saveProfileAsync(player.getUniqueId());
        player.sendMessage(Component.text("Beruf erlernt: ", NamedTextColor.GREEN).append(profession.displayComponent()));
        return true;
    }

    public PurchaseResult buyRecipe(Player player, CraftRecipe recipe) {
        Optional<PlayerProfile> optional = profileManager.getProfile(player.getUniqueId());
        if (optional.isEmpty() || !optional.get().isRegisteredInGuild()) return PurchaseResult.failure("Du bist noch nicht registriert.");
        PlayerProfile profile = optional.get();
        if (!profile.hasLearnedProfession(recipe.profession())) return PurchaseResult.failure("Du musst diesen Beruf zuerst erlernen.");
        if (profile.hasUnlockedRecipe(recipe.id())) return PurchaseResult.failure("Rezept bereits erlernt.");
        if (profile.getProfessionLevel(recipe.profession()) < recipe.requiredProfessionLevel()) return PurchaseResult.failure("Dein Berufslevel ist noch nicht hoch genug.");
        long price = recipePrice(recipe);
        if (!profile.removeMoney(price)) return PurchaseResult.failure("Du hast nicht genug Gold. Benötigt: " + price + " Gold.");
        profile.unlockRecipe(recipe.id());
        profileManager.saveProfileAsync(player.getUniqueId());
        return PurchaseResult.success(price);
    }

    public long experienceToNextLevel(UUID uuid, Profession profession) {
        int level = getLevel(uuid, profession);
        if (level >= Profession.MAX_LEVEL) return 0L;
        return Math.max(0L, experienceForLevel(level + 1) - getExperience(uuid, profession));
    }

    public void addExperience(Player player, Profession profession, long amount) {
        if (amount <= 0L) return;
        Optional<PlayerProfile> optional = profileManager.getProfile(player.getUniqueId());
        if (optional.isEmpty()) return;
        PlayerProfile profile = optional.get();
        if (!profile.isRegisteredInGuild() || !profile.hasLearnedProfession(profession)) return;

        int before = profile.getProfessionLevel(profession);
        long oldExperience = profile.getProfessionExperience(profession);
        profile.addProfessionExperience(profession, amount);
        long newExperience = profile.getProfessionExperience(profession);
        int after = professionLevelForExperience(newExperience);
        if (after != before) {
            profile.setProfessionLevel(profession, after);
            player.sendMessage(Component.text("Beruf ", NamedTextColor.GRAY)
                    .append(profession.displayComponent())
                    .append(Component.text(" erreicht Level " + after + "!", NamedTextColor.YELLOW)));
        }
        if (oldExperience != newExperience) profileManager.saveProfileAsync(player.getUniqueId());
    }

    public static long experienceForLevel(int level) {
        int clamped = Math.max(Profession.MIN_LEVEL, Math.min(Profession.MAX_LEVEL, level));
        if (clamped <= Profession.MIN_LEVEL) return 0L;
        long n = clamped - 1L;
        return 50L * n * n + 150L * n;
    }

    public static int professionLevelForExperience(long experience) {
        if (experience <= 0L) return Profession.MIN_LEVEL;
        int low = Profession.MIN_LEVEL;
        int high = Profession.MAX_LEVEL;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (experience >= experienceForLevel(mid)) low = mid;
            else high = mid - 1;
        }
        return low;
    }

    public record PurchaseResult(boolean success, String message, long price) {
        public static PurchaseResult success(long price) { return new PurchaseResult(true, "Rezept gekauft.", price); }
        public static PurchaseResult failure(String message) { return new PurchaseResult(false, message, 0L); }
    }
}
