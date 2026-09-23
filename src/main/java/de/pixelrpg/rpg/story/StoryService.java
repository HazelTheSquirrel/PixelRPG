package de.pixelrpg.rpg.story;

import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class StoryService {
    private final PlayerProfileManager profileManager;
    private final AtomicReference<List<StoryChapter>> chapters = new AtomicReference<>(List.of());

    public StoryService(PlayerProfileManager profileManager) {
        this.profileManager = Objects.requireNonNull(profileManager, "profileManager");
    }

    public void replace(List<StoryChapter> definitions) {
        List<StoryChapter> snapshot = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
        chapters.set(snapshot);
    }

    public Optional<StoryChapter> nextChapter(UUID uuid) {
        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        if (profile == null) return Optional.empty();

        int nextOrder = profile.getStoryChapterIndex() + 1;
        return chapters.get().stream()
                .filter(chapter -> chapter.order() == nextOrder)
                .findFirst();
    }

    public boolean completeChapter(UUID uuid, StoryChapter chapter) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(chapter, "chapter");

        PlayerProfile profile = profileManager.getProfile(uuid).orElse(null);
        if (profile == null || profile.getStoryChapterIndex() + 1 != chapter.order()) {
            return false;
        }

        profile.setStoryChapterIndex(chapter.order());
        if (chapter.expReward() > 0L) {
            profileManager.addExperience(uuid, chapter.expReward());
        }
        profileManager.saveProfileAsync(uuid);
        return true;
    }

    public boolean isLoaded() {
        return !chapters.get().isEmpty();
    }

    public List<StoryChapter> chapters() {
        return chapters.get();
    }

    public boolean isRegistered(Player player) {
        return profileManager.isRegistered(player.getUniqueId());
    }
}
