// src/main/java/de/pixelrpg/rpg/api/PartyAPI.java
package de.pixelrpg.rpg.api;

import java.util.Set;
import java.util.UUID;

public interface PartyAPI {

    boolean isInParty(UUID uuid);

    Set<UUID> getPartyMembers(UUID uuid);
}