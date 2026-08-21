package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/** Handles profession XP, level progression and progression feedback. */
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
        if (!profile.isRegisteredInGuild()) return;

        int before = profile.getProfessionLevel(profession);
        long oldExperience = profile.getProfessionExperience(profession);
        profile.addProfessionExperience(profession, amount);
        long newExperience = profile.getProfessionExperience(profession);
        int after = professionLevelForExperience(newExperience);
        if (after != before) {
            profile.setProfessionLevel(profession, after);
            player.sendMessage(Component.text("Beruf ", net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .append(profession.displayComponent())
                    .append(Component.text(" erreicht Level " + after + "!", net.kyori.adventure.text.format.NamedTextColor.YELLOW)));
        }
        if (oldExperience != newExperience) profileManager.saveProfileAsync(player.getUniqueId());
    }

    /** XP required to reach a profession level. Level 1 starts at zero XP and the curve accelerates gently. */
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
}
