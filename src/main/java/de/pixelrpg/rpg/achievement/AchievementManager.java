// src/main/java/de/pixelrpg/rpg/achievement/AchievementManager.java
package de.pixelrpg.rpg.achievement;

import de.pixelrpg.rpg.api.AchievementAPI;
import de.pixelrpg.rpg.core.Rank;
import de.pixelrpg.rpg.core.StatisticType;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

public final class AchievementManager implements AchievementAPI {

    private final AchievementRepository repository;
    private final PlayerProfileManager profileManager;

    public AchievementManager(AchievementRepository repository, PlayerProfileManager profileManager) {
        this.repository = repository;
        this.profileManager = profileManager;
    }

    public void checkStatisticAchievements(Player player, StatisticType type) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        long currentValue = profile.getStatistic(type.name());

        for (AchievementDefinition definition : repository.getAll()) {
            if (definition.triggerType() != AchievementTriggerType.STATISTIC_THRESHOLD) {
                continue;
            }
            if (definition.statisticType() != type) {
                continue;
            }
            if (profile.hasAchievement(definition.id())) {
                continue;
            }
            if (currentValue >= definition.thresholdValue()) {
                unlock(player, profile, definition);
            }
        }
    }

    public void checkRankAchievements(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }
        Rank rank = profile.getRank();

        for (AchievementDefinition definition : repository.getAll()) {
            if (definition.triggerType() != AchievementTriggerType.RANK_REACHED) {
                continue;
            }
            if (definition.requiredRank() == null || profile.hasAchievement(definition.id())) {
                continue;
            }
            if (rank.isAtLeast(definition.requiredRank())) {
                unlock(player, profile, definition);
            }
        }
    }

    public void checkClassChosenAchievements(Player player) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            return;
        }

        for (AchievementDefinition definition : repository.getAll()) {
            if (definition.triggerType() != AchievementTriggerType.CLASS_CHOSEN) {
                continue;
            }
            if (profile.hasAchievement(definition.id())) {
                continue;
            }
            unlock(player, profile, definition);
        }
    }

    public void forceUnlock(Player player, String achievementId) {
        PlayerProfile profile = profileManager.getProfile(player.getUniqueId()).orElse(null);
        AchievementDefinition definition = repository.get(achievementId);
        if (profile == null || definition == null || profile.hasAchievement(achievementId)) {
            return;
        }
        unlock(player, profile, definition);
    }

    private void unlock(Player player, PlayerProfile profile, AchievementDefinition definition) {
        profile.unlockAchievement(definition.id());

        if (definition.rewardTitle() != null) {
            profile.unlockTitle(definition.rewardTitle());
        }
        if (definition.rewardMoney() > 0) {
            profile.addMoney(definition.rewardMoney());
        }
        if (definition.rewardExp() > 0) {
            profileManager.addExperience(player.getUniqueId(), definition.rewardExp());
        }
        profileManager.saveProfileAsync(player.getUniqueId());

        player.sendMessage(Component.text("Achievement Unlocked: ", NamedTextColor.GOLD)
                .append(Component.text(definition.displayName(), NamedTextColor.YELLOW)));
        player.showTitle(Title.title(
                Component.text("Achievement Unlocked!", NamedTextColor.GOLD),
                Component.text(definition.displayName(), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2000), Duration.ofMillis(300))
        ));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    @Override
    public boolean hasAchievement(UUID uuid, String achievementId) {
        return profileManager.getProfile(uuid).map(p -> p.hasAchievement(achievementId)).orElse(false);
    }

    @Override
    public Set<String> getUnlockedAchievements(UUID uuid) {
        return profileManager.getProfile(uuid).map(PlayerProfile::getUnlockedAchievements).orElse(Set.of());
    }

    @Override
    public void registerAchievement(AchievementDefinition definition) {
        repository.registerRuntime(definition);
    }

    public AchievementRepository getRepository() {
        return repository;
    }
}