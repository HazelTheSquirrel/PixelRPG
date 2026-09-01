package de.pixelrpg.rpg.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Coordinates deterministic, idempotent shutdown of runtime resources. */
public final class LifecycleCoordinator implements AutoCloseable {
    private final List<AutoCloseable> resources = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public synchronized <T extends AutoCloseable> T register(T resource) {
        if (closed.get()) throw new IllegalStateException("LifecycleCoordinator is already closed");
        resources.add(resource);
        return resource;
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (AutoCloseable resource : resources.reversed()) {
            try { resource.close(); }
            catch (Exception exception) { java.util.logging.Logger.getLogger(LifecycleCoordinator.class.getName()).log(java.util.logging.Level.SEVERE, "Failed to close PixelRPG runtime resource " + resource.getClass().getName(), exception); }
        }
        resources.clear();
    }
}
