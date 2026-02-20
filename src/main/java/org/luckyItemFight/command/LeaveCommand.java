package org.luckyItemFight.command;

import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.luckyItemFight.game.main.GameInstance;

public class LeaveCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("该指令只能由玩家执行!");
            return true;
        }
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(commandSender));
        if(gameInstance == null) {
            SimplePlayer.of(commandSender).sendMessage(Lang.getMessage("command.leave-but-not-in-a-game"));
            return true;
        }
        gameInstance.leave(SimplePlayer.of(commandSender));
        return true;
    }
}
