package de.pixelrpg.rpg.command.impl;

import de.pixelrpg.rpg.command.SubCommand;
import de.pixelrpg.rpg.guild.GuildManager;
import de.pixelrpg.rpg.guild.KingdomMaintenanceService;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.npc.ProfessionNpcProgressionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import java.util.List;
import java.util.UUID;

/** Administrative visibility into kingdom maintenance review flags. */
public final class KingdomAdminSubCommand implements SubCommand {
    private final GuildManager guilds; private final KingdomMaintenanceService maintenance; private final ProfessionNpcProgressionService npcProgression;
    public KingdomAdminSubCommand(GuildManager guilds, KingdomMaintenanceService maintenance, ProfessionNpcProgressionService npcProgression){this.guilds=guilds;this.maintenance=maintenance;this.npcProgression=npcProgression;}
    @Override public String name(){return "kingdom";} @Override public String permission(){return "rpg.admin";} @Override public String description(){return "Königreich-Administration";} @Override public String usage(){return "/pixelrpg kingdom maintenance";}
    @Override public boolean execute(CommandSender sender,String[] args){
        if(args.length<1||args.length>2){sender.sendMessage(Component.text("Verwendung: /pixelrpg kingdom <maintenance|grandmasters|repair> [guild-uuid]",NamedTextColor.YELLOW));return true;}
        if(args[0].equalsIgnoreCase("repair")) {
            if(args.length != 2){sender.sendMessage(Component.text("Verwendung: /pixelrpg kingdom repair <guild-uuid>",NamedTextColor.YELLOW));return true;}
            try {
                UUID guildId=UUID.fromString(args[1]);
                var guild=guilds.getGuildById(guildId).orElse(null);
                if(guild==null){sender.sendMessage(Component.text("Königreich nicht gefunden.",NamedTextColor.RED));return true;}
                int repaired=npcProgression.repairSpecializationAllocations(guildId);
                sender.sendMessage(Component.text("Spezialisierungsbelegung geprüft: "+repaired+" NPC-Ränge repariert.",NamedTextColor.GREEN));
            } catch(IllegalArgumentException exception) {
                sender.sendMessage(Component.text("Ungültige Guild-UUID.",NamedTextColor.RED));
            }
            return true;
        }
        if(args[0].equalsIgnoreCase("grandmasters")) { for(Profession p:Profession.values()) if(p.isMain()) sender.sendMessage(Component.text(p.displayName()+": "+npcProgression.globalGrandmasters(p)+"/"+de.pixelrpg.rpg.PixelRPGPlugin.getInstance().getConfig().getInt("kingdom.grandmaster.main-profession-global-cap",2),NamedTextColor.GRAY)); return true; }
        if(!args[0].equalsIgnoreCase("maintenance")){sender.sendMessage(Component.text("Verwendung: /pixelrpg kingdom <maintenance|grandmasters>",NamedTextColor.YELLOW));return true;}
        var review=maintenance.reviewRequired(); sender.sendMessage(Component.text("Königreiche mit Wartungsprüfung: "+review.size(),NamedTextColor.GOLD));
        review.keySet().forEach(id->guilds.getGuildById(id).ifPresent(g->sender.sendMessage(Component.text("- "+g.name()+" (Level "+g.cityLevel()+")",NamedTextColor.RED)))); return true;
    }
    @Override public List<String> tabComplete(CommandSender sender,String[] args){return args.length<=1?List.of("maintenance","grandmasters","repair"):List.of();}
}
