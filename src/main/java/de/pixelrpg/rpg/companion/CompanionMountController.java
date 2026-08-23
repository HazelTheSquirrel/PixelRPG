package de.pixelrpg.rpg.companion;

import org.bukkit.Input;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AbstractNautilus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.SaddledMountInventory;
import org.bukkit.util.Vector;

/** Controls custom riding for passive companion mounts using Paper's current player-input API. */
public final class CompanionMountController {
    public void prepare(LivingEntity entity, CompanionDefinition.CompanionMountDefinition mount, Player owner) {
        if (!mount.enabled()) return;

        entity.setInvulnerable(true);
        entity.setGravity(true);
        entity.setFallDistance(0.0F);

        if (entity instanceof AbstractHorse horse) {
            horse.setTamed(true);
            horse.setOwner(owner);
        }
        if (entity instanceof AbstractNautilus nautilus) {
            nautilus.setTamed(true);
            nautilus.setOwner(owner);
        }
        if (mount.requiresSaddle()) setSaddle(entity);
    }

    public boolean isMounted(Player owner, LivingEntity mount) {
        return mount.getPassengers().contains(owner);
    }

    public void tick(Player owner, LivingEntity mount, CompanionDefinition.CompanionMountDefinition definition) {
        if (!definition.enabled() || !isMounted(owner, mount)) return;

        Input input = owner.getCurrentInput();
        Vector direction = movementDirection(owner, input);
        double speed = definition.movementSpeed();

        if (definition.type() == CompanionDefinition.CompanionMountDefinition.Type.FLYING
                || definition.type() == CompanionDefinition.CompanionMountDefinition.Type.UNDERWATER) {
            double vertical = 0.0D;
            if (input.isJump()) vertical += speed * 0.75D;
            if (input.isSneak()) vertical -= speed * 0.75D;
            direction.setY(vertical + direction.getY());
            if (direction.lengthSquared() > 0.0001D) {
                direction.normalize().multiply(speed);
            } else {
                direction = new Vector(0.0D, vertical, 0.0D);
            }
            mount.setGravity(false);
        } else {
            direction.setY(mount.isOnGround() && input.isJump() ? 0.55D : Math.max(-0.35D, mount.getVelocity().getY() * 0.90D));
            if (direction.lengthSquared() > 0.0001D) {
                Vector horizontal = direction.clone().setY(0.0D);
                if (horizontal.lengthSquared() > 0.0001D) horizontal.normalize().multiply(speed);
                direction = new Vector(horizontal.getX(), direction.getY(), horizontal.getZ());
            }
            mount.setGravity(true);
        }

        mount.setVelocity(direction);
        mount.setRotation(owner.getYaw(), 0.0F);
        mount.setFallDistance(0.0F);
    }

    public boolean tryMount(Player player, LivingEntity entity, CompanionDefinition definition) {
        if (!definition.mount().enabled() || !entity.getPassengers().isEmpty()) return false;
        if (definition.mount().requiresSaddle() && !hasSaddle(entity)) return false;
        if (!player.isSneaking()) return entity.addPassenger(player);
        return false;
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
            return;
        }
        if (entity instanceof AbstractHorse horse) {
            setInventorySaddle(horse.getInventory());
            return;
        }
        if (entity instanceof AbstractNautilus nautilus) {
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
        return inventory instanceof SaddledMountInventory saddled && saddled.getSaddle() != null
                && !saddled.getSaddle().isEmpty();
    }
}
