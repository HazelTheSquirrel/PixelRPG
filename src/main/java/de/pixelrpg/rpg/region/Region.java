// src/main/java/de/pixelrpg/rpg/region/Region.java
package de.pixelrpg.rpg.region;

import de.pixelrpg.rpg.core.Rank;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public final class Region {

    private final String id;
    private String displayName;
    private RegionCategory category;
    private Rank minRank;
    private Rank maxRank;
    private int priority;
    private final List<CuboidBounds> boxes = new ArrayList<>();

    public Region(String id, String displayName, RegionCategory category) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.minRank = Rank.F;
        this.maxRank = Rank.S;
        this.priority = 0;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public RegionCategory getCategory() {
        return category;
    }

    public void setCategory(RegionCategory category) {
        this.category = category;
    }

    public Rank getMinRank() {
        return minRank;
    }

    public void setMinRank(Rank minRank) {
        this.minRank = minRank;
    }

    public Rank getMaxRank() {
        return maxRank;
    }

    public void setMaxRank(Rank maxRank) {
        this.maxRank = maxRank;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<CuboidBounds> getBoxes() {
        return boxes;
    }

    public void addBox(CuboidBounds box) {
        boxes.add(box);
    }

    public boolean contains(org.bukkit.Location location) {
        for (CuboidBounds box : boxes) {
            if (box.contains(location)) {
                return true;
            }
        }
        return false;
    }

    public long getTotalVolume() {
        long total = 0L;
        for (CuboidBounds box : boxes) {
            total += box.volume();
        }
        return total;
    }

    public Component titleComponent() {
        return Component.text(displayName, category.getColor());
    }

    public Component subtitleComponent() {
        return Component.text(category.name(), net.kyori.adventure.text.format.NamedTextColor.GRAY);
    }
}