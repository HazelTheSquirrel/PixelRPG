package de.pixelrpg.rpg.content;

import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public final class ContentCatalogService implements AutoCloseable {
    private final JsonContentCatalogLoader loader;
    private final AtomicReference<ContentCatalog> catalog = new AtomicReference<>(new ContentCatalog(java.util.Map.of()));

    public ContentCatalogService(Plugin plugin, JsonContentCatalogLoader loader) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public CompletableFuture<Void> loadAsync() {
        return loader.loadAsync().thenAccept(catalog::set);
    }

    public Optional<String> text(String id) {
        return catalog.get()
                .text(new ContentId(id))
                .map(content -> content.value);
    }

    public ContentCatalog snapshot() {
        return catalog.get();
    }

    @Override
    public void close() {
        loader.close();
    }
}
