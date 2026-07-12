// src/main/java/de/pixelrpg/rpg/api/TitleAPI.java
package de.pixelrpg.rpg.api;

import java.util.Set;
import java.util.UUID;

public interface TitleAPI {

    String getSelectedTitle(UUID uuid);

    Set<String> getUnlockedTitles(UUID uuid);

    boolean hasTitle(UUID uuid, String title);

    void unlockTitle(UUID uuid, String title);

    boolean selectTitle(UUID uuid, String title);
}