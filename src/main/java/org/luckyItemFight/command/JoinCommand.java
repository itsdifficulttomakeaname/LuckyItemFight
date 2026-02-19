package org.luckyItemFight.command;

import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.game.main.PlayerState;

public class JoinCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("该指令只能由玩家执行!");
            return true;
        }
        if(GameInstance.getPlayerInstances().get(SimplePlayer.of(commandSender)) != null) {
            SimplePlayer.of(commandSender).sendMessage(Lang.getMessage("command.join-but-already-in-a-room"));
            return true;
        }
        GameInstance gameInstance = GameInstance.getStartingInstance();
        if(gameInstance == null) throw new PluginException("ERROR While Playing", "GameInstance absolutely isn't null but it is");
        gameInstance.join(SimplePlayer.of(commandSender), PlayerState.ALIVE);
        return true;
    }
}
