package de.pixelrpg.rpg.command;

import de.pixelrpg.rpg.boss.BossDefinition;
import de.pixelrpg.rpg.boss.BossKind;
import de.pixelrpg.rpg.boss.BossManager;
import de.pixelrpg.rpg.boss.BossRepository;
import de.pixelrpg.rpg.companion.CompanionService;
import de.pixelrpg.rpg.core.Level;
import de.pixelrpg.rpg.item.ItemDefinition;
import de.pixelrpg.rpg.item.ItemService;
import de.pixelrpg.rpg.player.PlayerProfile;
import de.pixelrpg.rpg.player.PlayerProfileManager;
import de.pixelrpg.rpg.profession.Profession;
import de.pixelrpg.rpg.npc.NpcRuntimeManager;
import de.pixelrpg.rpg.npc.NpcType;
import de.pixelrpg.rpg.npc.RPGNpc;
import de.pixelrpg.rpg.region.RegionEditor;
import de.pixelrpg.rpg.region.RegionManager;
import de.pixelrpg.rpg.shop.ShopService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class PixelRPGCommand implements BasicCommand {
    private final PlayerProfileManager profiles;
    private final ItemService items;
    private final CompanionService companions;
    private final NpcRuntimeManager npcs;
    private final RegionManager regions;
    private final RegionEditor regionEditor;
    private final ShopService shops;
    private final BossRepository bosses;
    private final BossManager bossManager;

    public PixelRPGCommand(PlayerProfileManager profiles, ItemService items, CompanionService companions,
                           NpcRuntimeManager npcs, RegionManager regions, RegionEditor regionEditor,
                           ShopService shops, BossRepository bosses, BossManager bossManager) {
        this.profiles = profiles;
        this.items = items;
        this.companions = companions;
        this.npcs = npcs;
        this.regions = regions;
        this.regionEditor = regionEditor;
        this.shops = shops;
        this.bosses = bosses;
        this.bossManager = bossManager;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        Player player = source.getExecutor() instanceof Player p ? p : null;
        if (args.length == 0) {
            help(source.getSender());
            return;
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        if (root.equals("questlog")) { questLog(source.getSender(), player); return; }
        if (root.equals("companion") || root.equals("npc") || root.equals("region") || root.equals("boss") || root.equals("item") || root.equals("player") || root.equals("shop")) {
            if (!source.getSender().hasPermission("rpg.admin")) {
                source.getSender().sendMessage(Component.text("Keine Berechtigung.", NamedTextColor.RED));
                return;
            }
            String[] rest = Arrays.copyOfRange(args, 1, args.length);
            switch (root) {
                case "companion" -> companion(source.getSender(), rest);
                case "npc" -> npc(source.getSender(), player, rest);
                case "region" -> region(source.getSender(), player, rest);
                case "boss" -> boss(source.getSender(), player, rest);
                case "item" -> item(source.getSender(), rest);
                case "player" -> player(source.getSender(), rest);
                case "shop" -> shop(source.getSender(), rest);
                default -> help(source.getSender());
            }
            return;
        }
        help(source.getSender());
    }

    @Override
    public String permission() { return "rpg.member"; }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        List<String> roots = source.getSender().hasPermission("rpg.admin")
                ? List.of("questlog","companion","npc","region","boss","item","player","shop")
                : List.of("questlog");
        if (args.length == 1) return prefix(roots, args[0]);
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "companion" -> args.length == 2 ? prefix(List.of("grant","list"), args[1]) : args.length == 3 ? onlinePlayers(args[2]) : args.length == 4 ? prefix(companions.definitionIds(), args[3]) : List.of();
            case "boss" -> args.length == 2 ? prefix(List.of("event","list","reload"), args[1]) : args.length == 3 && args[1].equalsIgnoreCase("event") ? prefix(bosses.getWorldBosses().stream().map(b -> b.getId()).toList(), args[2]) : List.of();
            case "item" -> args.length == 2 ? prefix(List.of("list","give"), args[1]) : args.length == 3 && args[1].equalsIgnoreCase("give") ? onlinePlayers(args[2]) : List.of();
            case "player" -> args.length == 2 ? prefix(List.of("info","set-level","set-xp","set-gold","set-profession","reset"), args[1]) : args.length == 3 ? onlinePlayers(args[2]) : args.length == 4 && args[1].equalsIgnoreCase("set-profession") ? prefix(Arrays.stream(Profession.values()).map(p -> p.name()).toList(), args[3]) : List.of();
            case "npc" -> args.length == 2 ? prefix(List.of("create","create-id","filler","remove","rename","skin","list"), args[1]) : args.length == 3 && args[1].equalsIgnoreCase("create") ? prefix(Arrays.stream(NpcType.values()).map(n -> n.name()).toList(), args[2]) : args.length == 4 && args[1].equalsIgnoreCase("create-id") ? prefix(Arrays.stream(NpcType.values()).map(Enum::name).toList(), args[3]) : List.of();
            case "region" -> args.length == 2 ? prefix(List.of("create","finish","confirm","cancel","delete","list"), args[1]) : List.of();
            case "shop" -> args.length == 2 ? prefix(List.of("list"), args[1]) : List.of();
            default -> List.of();
        };
    }

    private void help(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(Component.text("PixelRPG", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/pixelrpg questlog", NamedTextColor.YELLOW));
        if (sender.hasPermission("rpg.admin")) {
            sender.sendMessage(Component.text("/pixelrpg companion <grant|list> ...", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/pixelrpg npc <create|create-id|filler|remove|rename|skin|list> ...", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/pixelrpg region <create|finish|confirm|cancel|delete|list> ...", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/pixelrpg boss <event|list|reload> ...", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/pixelrpg item <list|give> ...", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/pixelrpg player <info|set-level|set-xp|set-gold|set-profession|reset> ...", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/pixelrpg shop list <npc-id>", NamedTextColor.YELLOW));
        }
    }

    private void questLog(org.bukkit.command.CommandSender sender, Player player) {
        if (player == null) { sender.sendMessage(Component.text("Nur Spieler können das Questlog öffnen.", NamedTextColor.RED)); return; }
        PlayerProfile profile = profiles.getProfile(player.getUniqueId()).orElse(null);
        if (profile == null || !profile.isRegistered()) { sender.sendMessage(Component.text("Kein registriertes PixelRPG-Profil.", NamedTextColor.RED)); return; }
        sender.sendMessage(Component.text("Aktive Quests: " + profile.getActiveQuests().size(), NamedTextColor.GOLD));
        profile.getActiveQuests().values().forEach(q -> sender.sendMessage(Component.text(q.getQuestId()+" "+q.getCurrentAmount(), NamedTextColor.YELLOW)));
    }

    private void companion(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length < 1) { sender.sendMessage(Component.text("Usage: /pixelrpg companion <grant|list> ...", NamedTextColor.RED)); return; }
        if (args[0].equalsIgnoreCase("list")) { companions.definitionIds().forEach(id -> sender.sendMessage(Component.text(id, NamedTextColor.YELLOW))); return; }
        if (args.length != 3 || !args[0].equalsIgnoreCase("grant")) { sender.sendMessage(Component.text("Usage: /pixelrpg companion grant <player> <id>", NamedTextColor.RED)); return; }
        Player target=Bukkit.getPlayerExact(args[1]);
        boolean ok=target!=null && companions.adminGrant(target.getUniqueId(), args[2]);
        sender.sendMessage(Component.text(ok ? "Companion freigeschaltet: "+args[2] : "Companion konnte nicht freigeschaltet werden.", ok ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void npc(org.bukkit.command.CommandSender sender, Player player, String[] args) {
        if (args.length==0) { sender.sendMessage(Component.text("Usage: /pixelrpg npc <create|create-id|filler|remove|rename|skin|list>", NamedTextColor.RED)); return; }
        switch(args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> npcs.getAll().forEach(n -> sender.sendMessage(Component.text(n.id()+" "+n.type()+" "+n.name(), NamedTextColor.YELLOW)));
            case "create" -> { if(player==null||args.length<3||args.length>4){sender.sendMessage(Component.text("Usage: /pixelrpg npc create <type> <name> [skin]",NamedTextColor.RED));return;} try{NpcType type=NpcType.valueOf(args[1].toUpperCase(Locale.ROOT));String name=args[2];String skin=args.length==4?args[3]:null;RPGNpc n=npcs.create(type,name,player.getLocation(),skin,null);sender.sendMessage(Component.text("NPC erstellt: "+n.id(),NamedTextColor.GREEN));}catch(IllegalArgumentException e){sender.sendMessage(Component.text(e.getMessage(),NamedTextColor.RED));} }
            case "create-id" -> { if(player==null||args.length<4||args.length>5){sender.sendMessage(Component.text("Usage: /pixelrpg npc create-id <id> <type> <name> [skin]",NamedTextColor.RED));return;} try{NpcType type=NpcType.valueOf(args[2].toUpperCase(Locale.ROOT));String skin=args.length==5?args[4]:null;RPGNpc n=npcs.createWithId(args[1],type,args[3],player.getLocation(),skin,null);sender.sendMessage(Component.text("NPC erstellt: "+n.id(),NamedTextColor.GREEN));}catch(IllegalArgumentException e){sender.sendMessage(Component.text(e.getMessage(),NamedTextColor.RED));} }
            case "filler" -> { if(player==null||args.length<3||args.length>4){sender.sendMessage(Component.text("Usage: /pixelrpg npc filler <id> <name> [skin]",NamedTextColor.RED));return;} try{String skin=args.length==4?args[3]:null;RPGNpc n=npcs.createWithId(args[1],NpcType.FILLER,args[2],player.getLocation(),skin,null);sender.sendMessage(Component.text("Filler-NPC erstellt: "+n.id(),NamedTextColor.GREEN));}catch(IllegalArgumentException e){sender.sendMessage(Component.text(e.getMessage(),NamedTextColor.RED));} }
            case "remove" -> { RPGNpc n=lookNpc(player); if(n==null){sender.sendMessage(Component.text("Schaue einen PixelRPG-NPC an.",NamedTextColor.RED));return;} npcs.removeById(n.id()); sender.sendMessage(Component.text("NPC entfernt: "+n.id(),NamedTextColor.GREEN)); }
            case "rename" -> { if(player==null||args.length<2)return; RPGNpc n=lookNpc(player); if(n!=null&&npcs.rename(n.id(),String.join(" ",Arrays.copyOfRange(args,1,args.length)))) sender.sendMessage(Component.text("NPC umbenannt.",NamedTextColor.GREEN)); }
            case "skin" -> { if(player==null||args.length!=2)return; RPGNpc n=lookNpc(player); if(n!=null&&npcs.updateSkin(n.id(),args[1])) sender.sendMessage(Component.text("NPC-Skin aktualisiert.",NamedTextColor.GREEN)); }
            default -> sender.sendMessage(Component.text("Unbekannte NPC-Aktion.",NamedTextColor.RED));
        }
    }

    private void region(org.bukkit.command.CommandSender sender, Player player, String[] args) {
        if(args.length==0){sender.sendMessage(Component.text("Usage: /pixelrpg region <create|finish|confirm|cancel|delete|list>",NamedTextColor.RED));return;}
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "list" -> regions.all().forEach(r->sender.sendMessage(Component.text(r.id()+" "+r.name(),NamedTextColor.YELLOW)));
            case "create" -> {if(player==null||args.length<2)return;regionEditor.begin(player,String.join(" ",Arrays.copyOfRange(args,1,args.length)));sender.sendMessage(Component.text("Region-Editor gestartet.",NamedTextColor.GREEN));}
            case "finish" -> {if(player!=null)regionEditor.finish(player);}
            case "confirm" -> {if(player!=null)regionEditor.confirm(player);}
            case "cancel" -> {if(player!=null)regionEditor.cancel(player);}
            case "delete" -> {if(args.length!=2)return;try{boolean ok=regions.delete(UUID.fromString(args[1]));sender.sendMessage(Component.text(ok?"Region gelöscht.":"Region nicht gefunden.",ok?NamedTextColor.GREEN:NamedTextColor.RED));}catch(IllegalArgumentException e){sender.sendMessage(Component.text("Ungültige Region-ID.",NamedTextColor.RED));}}
            default -> sender.sendMessage(Component.text("Unbekannte Region-Aktion.",NamedTextColor.RED));
        }
    }

    private void boss(org.bukkit.command.CommandSender sender, Player player, String[] args) {
        if(args.length==0){sender.sendMessage(Component.text("Usage: /pixelrpg boss <event|list|reload>",NamedTextColor.RED));return;}
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "list" -> bosses.getAll().forEach(b->sender.sendMessage(Component.text(b.getId()+" "+b.getDisplayName(),NamedTextColor.YELLOW)));
            case "reload" -> {bosses.load();sender.sendMessage(Component.text("Boss-Definitionen neu geladen.",NamedTextColor.GREEN));}
            case "event" -> {if(player==null||args.length!=2)return;BossDefinition b=bosses.get(args[1]);if(b==null||b.getKind()!=BossKind.WORLD_EVENT){sender.sendMessage(Component.text("World-Boss nicht gefunden.",NamedTextColor.RED));return;}if(bossManager.hasActiveBossOfType(b.getId())){sender.sendMessage(Component.text("Dieser World-Boss ist bereits aktiv.",NamedTextColor.RED));return;}bossManager.spawnWorldBoss(b,player.getLocation());sender.sendMessage(Component.text("World-Boss gestartet.",NamedTextColor.GREEN));}
            default -> sender.sendMessage(Component.text("Unbekannte Boss-Aktion.",NamedTextColor.RED));
        }
    }

    private void item(org.bukkit.command.CommandSender sender, String[] args) {
        if(args.length==0){sender.sendMessage(Component.text("Usage: /pixelrpg item <list|give>",NamedTextColor.RED));return;}
        if(args[0].equalsIgnoreCase("list")){items.definitions().stream().map(d -> d.id()).sorted().forEach(id->sender.sendMessage(Component.text(id,NamedTextColor.YELLOW)));return;}
        if(args.length<3||!args[0].equalsIgnoreCase("give")){sender.sendMessage(Component.text("Usage: /pixelrpg item give <player> <item-id> [amount]",NamedTextColor.RED));return;}
        Player target=Bukkit.getPlayerExact(args[1]);if(target==null){sender.sendMessage(Component.text("Spieler ist nicht online.",NamedTextColor.RED));return;}
        ItemDefinition d=items.definitions().stream().filter(x->x.id().equalsIgnoreCase(args[2])||x.id().equalsIgnoreCase("pixelrpg:"+args[2])).findFirst().orElse(null);
        if(d==null){sender.sendMessage(Component.text("Unbekannte Item-ID.",NamedTextColor.RED));return;}
        int amount=1;if(args.length>3){try{amount=Integer.parseInt(args[3]);}catch(NumberFormatException e){sender.sendMessage(Component.text("Ungültige Anzahl.",NamedTextColor.RED));return;}if(amount<1||amount>64){sender.sendMessage(Component.text("Anzahl muss zwischen 1 und 64 liegen.",NamedTextColor.RED));return;}}if(d.unique()&&amount>1){sender.sendMessage(Component.text("UNIQUE-Items können nur einzeln vergeben werden.",NamedTextColor.RED));return;} final int requestedAmount=amount; items.createAdminItem(d.id()).ifPresentOrElse(stack->{stack.setAmount(requestedAmount);target.getInventory().addItem(stack).values().forEach(x->target.getWorld().dropItemNaturally(target.getLocation(),x));sender.sendMessage(Component.text("Item vergeben.",NamedTextColor.GREEN));},()->sender.sendMessage(Component.text("Item konnte nicht erstellt werden.",NamedTextColor.RED)));
    }

    private void player(org.bukkit.command.CommandSender sender, String[] args) {
        if(args.length<2){sender.sendMessage(Component.text("Usage: /pixelrpg player <info|set-level|set-xp|set-gold|set-profession|reset> <player> ...",NamedTextColor.RED));return;}
        Player target=Bukkit.getPlayerExact(args[1]);if(target==null){sender.sendMessage(Component.text("Spieler muss online sein.",NamedTextColor.RED));return;}
        PlayerProfile p=profiles.getProfile(target.getUniqueId()).orElse(null);if(p==null){sender.sendMessage(Component.text("Kein Profil.",NamedTextColor.RED));return;}
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "info" -> {sender.sendMessage(Component.text(target.getName()+" Level "+p.getLevel()+" XP "+p.getExperience()+" Gold "+p.getMoney(),NamedTextColor.YELLOW));}
            case "set-level" -> {if(args.length<3)return;try{int l=Integer.parseInt(args[2]);if(!Level.isValidNormalLevel(l))throw new IllegalArgumentException();p.setExperience(Level.getRequiredExperience(l));profiles.saveProfileAsync(target.getUniqueId());sender.sendMessage(Component.text("Level gesetzt.",NamedTextColor.GREEN));}catch(IllegalArgumentException e){sender.sendMessage(Component.text("Ungültiges Level.",NamedTextColor.RED));}}
            case "set-xp" -> {if(args.length<3)return;try{long x=Long.parseLong(args[2]);if(x<0)throw new IllegalArgumentException();p.setExperience(x);profiles.saveProfileAsync(target.getUniqueId());sender.sendMessage(Component.text("XP gesetzt.",NamedTextColor.GREEN));}catch(IllegalArgumentException e){sender.sendMessage(Component.text("Ungültige XP.",NamedTextColor.RED));}}
            case "set-gold" -> {if(args.length<3)return;try{double x=Double.parseDouble(args[2]);if(!Double.isFinite(x)||x<0)throw new IllegalArgumentException();p.setMoney(x);profiles.saveProfileAsync(target.getUniqueId());sender.sendMessage(Component.text("Gold gesetzt.",NamedTextColor.GREEN));}catch(IllegalArgumentException e){sender.sendMessage(Component.text("Ungültiger Goldbetrag.",NamedTextColor.RED));}}
            case "set-profession" -> {if(args.length<4)return;try{Profession prof=Profession.valueOf(args[2].toUpperCase(Locale.ROOT));int l=Integer.parseInt(args[3]);p.setProfessionLevel(prof,l);p.learnProfession(prof);profiles.saveProfileAsync(target.getUniqueId());sender.sendMessage(Component.text("Beruf gesetzt.",NamedTextColor.GREEN));}catch(IllegalArgumentException e){sender.sendMessage(Component.text("Ungültiger Beruf oder Level.",NamedTextColor.RED));}}
            case "reset" -> {p.resetProgress();profiles.saveProfileAsync(target.getUniqueId());sender.sendMessage(Component.text("Profil zurückgesetzt.",NamedTextColor.GREEN));}
            default -> sender.sendMessage(Component.text("Unbekannter Player-Befehl.",NamedTextColor.RED));
        }
    }

    private void shop(org.bukkit.command.CommandSender sender, String[] args) {
        if(args.length==2&&args[0].equalsIgnoreCase("list")){shops.entries(args[1]).forEach((e)->sender.sendMessage(Component.text(e.item().getType()+" buy="+e.buyPrice()+" sell="+e.sellPrice(),NamedTextColor.YELLOW)));return;}
        sender.sendMessage(Component.text("Usage: /pixelrpg shop list <npc-id>",NamedTextColor.RED));
    }

    private RPGNpc lookNpc(Player player) {
        if(player==null)return null;
        RayTraceResult hit=player.rayTraceEntities(8);
        if(hit==null)return null;
        Entity entity=hit.getHitEntity();
        return entity==null?null:npcs.getByEntity(entity.getUniqueId()).orElse(null);
    }

    private static List<String> onlinePlayers(String prefix){return prefix(Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).toList(),prefix);}
    private static List<String> prefix(List<String> values,String prefix){String p=prefix==null?"":prefix.toLowerCase(Locale.ROOT);return values.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(p)).sorted().toList();}
}
