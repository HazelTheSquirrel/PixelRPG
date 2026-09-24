package de.pixelrpg.rpg.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class LifecycleCoordinator implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    public <T extends AutoCloseable> T own(T resource) { resources.push(Objects.requireNonNull(resource)); return resource; }
    @Override public void close() {
        RuntimeException failure = null;
        while (!resources.isEmpty()) {
            try { resources.pop().close(); }
            catch (Exception e) { if (failure == null) failure = new RuntimeException("PixelRPG shutdown failed.", e); else failure.addSuppressed(e); }
        }
        if (failure != null) throw failure;
    }
}
