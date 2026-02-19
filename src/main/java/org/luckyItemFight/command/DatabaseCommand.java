package org.luckyItemFight.command;

import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.message.MessageList;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luckyItemFight.Main;

import java.util.List;
import java.util.Map;

public class DatabaseCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("该指令只能由玩家执行!");
            return true;
        }
        if(!commandSender.hasPermission("luckyitemfight.database")) {
            SimplePlayer.of(commandSender).sendMessage(Message.of("<#F72828>你必须拥有 luckyitemfight.database 权限才能使用这个指令"));
            return true;
        }
        SimplePlayer player = SimplePlayer.of(commandSender);
        if (args.length < 1) {
            player.sendMessage(sendHelp());
            return true;
        }
        String sub1 = args[0];
        if(sub1.equalsIgnoreCase("update")) {
            if(args.length < 4) {
                player.sendMessage(Message.of("<#F72828>Too few arguments!"));
                return true;
            }
            player.sendMessage(handleUpdate(args[1],args[2],args[3]));
        } else if (sub1.equalsIgnoreCase("query")) {
            if(args.length < 3) {
                player.sendMessage(Message.of("<#F72828>Too few arguments!"));
                return true;
            }
            player.sendMessage(handleQuery(args[1],args[2]));
        } else if (sub1.equalsIgnoreCase("list")) {
            if(args.length < 2) {
                player.sendMessage(Message.of("<#F72828>Too few arguments!"));
                return true;
            }
            player.sendMessage(handleList(args[1]));
        } else {
            player.sendMessage(sendHelp());
        }
        return true;
    }

    Message sendHelp() {
        return Message.of("""
        \n<#F72828>/db update <player> <placeholder> <val> - set <player>'s <placeholder> to <val>
        
        <#F72828>/db query <player> <placeholder> - query <player>'s <placeholder>'s value
        
        <#F72828>/db list <player> - list all value which <player> has
        """);
    }

    Message handleUpdate(String p,String placeholder,String val) {
        SimplePlayer player = SimplePlayer.of(p);
        String uuid = player.getUUID().toString();
        if(Main.dataBaseManager.query(uuid, "coin") == null) return Lang.getMessage("command.no-such-player");
        Main.dataBaseManager.update(uuid, placeholder, Integer.parseInt(val)); // 现有项目设计中数据库中的所有键的类型都是整数
        return Lang.getMessage("command.update-success").add("player",p).add("placeholder",placeholder).add("value",val);
    }

    Message handleQuery(String p,String placeholder) {
        SimplePlayer player = SimplePlayer.of(p);
        String uuid = player.getUUID().toString();
        if(Main.dataBaseManager.query(uuid, "coin") == null) return Lang.getMessage("command.no-such-player");
        return Lang.getMessage("command.query-success").add("player",p).add("placeholder",placeholder)
                .add("value",Main.dataBaseManager.query(uuid, placeholder));
    }

    Message handleList(String p) {
        SimplePlayer player = SimplePlayer.of(p);
        String uuid = player.getUUID().toString();
        if(Main.dataBaseManager.query(uuid, "coin") == null) return Lang.getMessage("command.no-such-player");
        Map<String,Object> mp = Main.dataBaseManager.getPlayer(uuid);
        return Lang.getMessageList("command.list-success")
                .add("player",p)
                .add("coin",mp.get("coin"))
                .add("play",mp.get("play"))
                .add("win",mp.get("win"))
                .add("kill",mp.get("kill"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length <= 1) return List.of("update","query","list");
        return List.of();
    }
}
