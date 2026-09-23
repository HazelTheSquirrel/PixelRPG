package de.pixelrpg.rpg.companion;

import org.bukkit.Input;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AbstractNautilus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SaddledMountInventory;
import org.bukkit.util.Vector;

public final class CompanionMountController {
    public boolean tryMount(Player player, LivingEntity entity, CompanionDefinition definition) {
        if (!definition.mount().enabled() || !entity.getPassengers().isEmpty()) return false;
        if (definition.mount().requiresSaddle() && !hasSaddle(entity)) return false;
        return player.isSneaking() && entity.addPassenger(player);
    }

    public void prepare(LivingEntity entity, CompanionDefinition definition, Player owner) {
        if (!definition.mount().enabled()) return;
        entity.setInvulnerable(true);
        if (entity instanceof AbstractHorse horse) {
            horse.setTamed(true);
            horse.setOwner(owner);
        }
        if (entity instanceof AbstractNautilus nautilus) {
            nautilus.setTamed(true);
            nautilus.setOwner(owner);
        }
        if (definition.mount().requiresSaddle()) setSaddle(entity);
    }

    public void tick(Player owner, LivingEntity mount, CompanionDefinition definition) {
        if (!definition.mount().enabled() || !mount.getPassengers().contains(owner)) return;
        Input input = owner.getCurrentInput();
        Vector direction = movementDirection(owner, input);
        double speed = definition.mount().speed();
        switch (definition.mount().type()) {
            case FLYING, UNDERWATER -> {
                double vertical = input.isJump() ? speed * 0.75D : input.isSprint() ? -speed * 0.75D : 0.0D;
                if (direction.lengthSquared() > 0.0001D) direction.normalize().multiply(speed);
                direction.setY(vertical);
                mount.setGravity(false);
            }
            case GROUND -> {
                Vector horizontal = direction.clone();
                if (horizontal.lengthSquared() > 0.0001D) horizontal.normalize().multiply(speed);
                direction = new Vector(horizontal.getX(), mount.isOnGround() && input.isJump() ? 0.55D : mount.getVelocity().getY(), horizontal.getZ());
                mount.setGravity(true);
            }
        }
        mount.setVelocity(direction);
        mount.setRotation(owner.getYaw(), 0.0F);
        mount.setFallDistance(0.0F);
    }

    private static Vector movementDirection(Player player, Input input) {
        Vector forward = player.getLocation().getDirection().setY(0.0D);
        if (forward.lengthSquared() < 0.0001D) forward = new Vector(0.0D, 0.0D, 1.0D);
        forward.normalize();
        Vector right = new Vector(-forward.getZ(), 0.0D, forward.getX());
        Vector direction = new Vector();
        if (input.isForward()) direction.add(forward);
        if (input.isBackward()) direction.subtract(forward);
        if (input.isRight()) direction.add(right);
        if (input.isLeft()) direction.subtract(right);
        return direction;
    }

    private static void setSaddle(Entity entity) {
        if (entity instanceof Pig pig) {
            pig.setSaddle(true);
        } else if (entity instanceof AbstractHorse horse) {
            setInventorySaddle(horse.getInventory());
        } else if (entity instanceof AbstractNautilus nautilus) {
            setInventorySaddle(nautilus.getInventory());
        }
    }

    private static boolean hasSaddle(Entity entity) {
        if (entity instanceof Pig pig) return pig.hasSaddle();
        if (entity instanceof AbstractHorse horse) return hasInventorySaddle(horse.getInventory());
        if (entity instanceof AbstractNautilus nautilus) return hasInventorySaddle(nautilus.getInventory());
        return true;
    }

    private static void setInventorySaddle(Inventory inventory) {
        if (inventory instanceof SaddledMountInventory saddled) saddled.setSaddle(new ItemStack(Material.SADDLE));
    }

    private static boolean hasInventorySaddle(Inventory inventory) {
        return inventory instanceof SaddledMountInventory saddled
                && saddled.getSaddle() != null && !saddled.getSaddle().isEmpty();
    }
}
