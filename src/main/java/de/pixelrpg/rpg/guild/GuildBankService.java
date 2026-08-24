package de.pixelrpg.rpg.guild;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Opens the shared item storage for members at the Banker NPC. */
public final class GuildBankService {
    private final GuildManager guilds;
    private final GuildBankStorageService storage;

    public GuildBankService(JavaPlugin plugin, GuildManager guilds) {
        this.guilds = guilds;
        this.storage = new GuildBankStorageService(plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuildBankListener(storage, guilds), plugin);
    }

    public void open(Player player) {
        Guild guild = guilds.getGuild(player.getUniqueId()).orElse(null);
        if (guild == null) {
            player.sendMessage(Component.text("Du bist in keiner Gilde.", NamedTextColor.RED));
            return;
        }
        GuildBankHolder holder = new GuildBankHolder(guild.id());
        Inventory inventory = Bukkit.createInventory(holder, GuildBankStorageService.SIZE,
                Component.text("Gildenbank – " + guild.name(), NamedTextColor.GOLD));
        holder.inventory(inventory);
        ItemStack[] contents = storage.load(guild.id());
        for (int slot = 0; slot < contents.length; slot++) inventory.setItem(slot, contents[slot]);
        player.openInventory(inventory);
    }
}
