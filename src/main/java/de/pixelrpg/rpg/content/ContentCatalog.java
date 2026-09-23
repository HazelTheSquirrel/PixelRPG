package de.pixelrpg.rpg.content;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ContentCatalog {
    private final Map<ContentId, TextContent> texts;

    public ContentCatalog(Map<ContentId, TextContent> texts) {
        this.texts = Map.copyOf(Objects.requireNonNull(texts, "texts"));
    }

    public Optional<TextContent> text(ContentId id) {
        return Optional.ofNullable(texts.get(id));
    }

    public Map<ContentId, TextContent> texts() {
        return texts;
    }
}
