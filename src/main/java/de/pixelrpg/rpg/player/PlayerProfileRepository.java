package de.pixelrpg.rpg.player;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
public interface PlayerProfileRepository extends AutoCloseable { CompletableFuture<PlayerProfile> load(UUID uuid); CompletableFuture<Void> save(PlayerProfile profile); @Override default void close(){} }
