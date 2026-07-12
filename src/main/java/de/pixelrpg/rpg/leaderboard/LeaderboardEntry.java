// src/main/java/de/pixelrpg/rpg/leaderboard/LeaderboardEntry.java
package de.pixelrpg.rpg.leaderboard;

import java.util.UUID;

public record LeaderboardEntry(UUID uuid, double value) {
}