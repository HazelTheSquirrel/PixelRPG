package de.pixelrpg.rpg.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns runtime resources and closes them in reverse registration order.
 */
public final class LifecycleCoordinator implements AutoCloseable {
    private final Logger logger;
    private final List<AutoCloseable> resources = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public LifecycleCoordinator(Logger logger) {
        this.logger = logger;
    }

    public synchronized <T extends AutoCloseable> T register(T resource) {
        if (closed.get()) {
            throw new IllegalStateException("LifecycleCoordinator is already closed");
        }
        resources.add(resource);
        return resource;
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        for (AutoCloseable resource : resources.reversed()) {
            try {
                resource.close();
            } catch (Exception exception) {
                logger.log(
                        Level.SEVERE,
                        "Failed to close PixelRPG runtime resource " + resource.getClass().getName(),
                        exception
                );
            }
        }
        resources.clear();
    }
}
