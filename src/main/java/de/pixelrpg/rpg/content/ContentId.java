package de.pixelrpg.rpg.content;

import java.util.Objects;

public record ContentId(String value) {
    public ContentId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || !value.matches("[a-z0-9][a-z0-9._/-]*")) {
            throw new IllegalArgumentException("Invalid content id: " + value);
        }
    }
}
