package de.pixelrpg.rpg.profession;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

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

    public void addExperience(Player player, Profession profession, long amount) {
        if (amount <= 0L) return;
        Optional<PlayerProfile> optional = profileManager.getProfile(player.getUniqueId());
        if (optional.isEmpty()) return;
        PlayerProfile profile = optional.get();
        if (!profile.isRegisteredInGuild()) return;

        int before = profile.getProfessionLevel(profession);
        profile.addProfessionExperience(profession, amount);
        int after = professionLevelForExperience(profile.getProfessionExperience(profession));
        if (after != before) {
            profile.setProfessionLevel(profession, after);
            player.sendMessage(Component.text(profession.name() + " reached level " + after + "!"));
        }
        profileManager.saveProfileAsync(player.getUniqueId());
    }

    public static long experienceForLevel(int level) {
        int clamped = Math.max(Profession.MIN_LEVEL, Math.min(Profession.MAX_LEVEL, level));
        if (clamped <= Profession.MIN_LEVEL) return 0L;
        return 100L * (long) clamped * (clamped - 1L) / 2L;
    }

    public static int professionLevelForExperience(long experience) {
        if (experience <= 0L) return Profession.MIN_LEVEL;
        int level = Profession.MIN_LEVEL;
        while (level < Profession.MAX_LEVEL && experience >= experienceForLevel(level + 1)) level++;
        return level;
    }
}
