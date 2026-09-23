package de.pixelrpg.rpg.content;

import java.util.Objects;

public record TextContent(ContentId id, String value) {
    public TextContent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
    }
}
