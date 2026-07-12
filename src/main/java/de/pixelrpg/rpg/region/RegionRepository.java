// src/main/java/de/pixelrpg/rpg/region/RegionRepository.java
package de.pixelrpg.rpg.region;

import java.util.Collection;
import java.util.List;

public interface RegionRepository {

    void init() throws Exception;

    List<Region> loadAll() throws Exception;

    void saveAll(Collection<Region> regions) throws Exception;
}