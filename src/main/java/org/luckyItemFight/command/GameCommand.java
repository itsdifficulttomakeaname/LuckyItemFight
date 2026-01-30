package org.luckyItemFight.command;

import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.game.main.GameState;
import org.luckyItemFight.game.main.PlayerState;

import java.util.List;

public class GameCommand implements CommandExecutor, TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(args.length < 1) return false;
        String subCommand_0 = args[0];
        if(subCommand_0.equalsIgnoreCase("join")) {
            GameInstance gameInstance = GameInstance.getStartingInstance();
            assert gameInstance != null;
            if(gameInstance.getState() == GameState.WAITING) gameInstance.join(SimplePlayer.of(commandSender), PlayerState.ALIVE);
            else gameInstance.join(SimplePlayer.of(commandSender), PlayerState.SPECTATOR);
        } else if(subCommand_0.equalsIgnoreCase("leave")) {
            GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(commandSender));
            if(gameInstance == null) {
                SimplePlayer.of(commandSender).sendMessage(Lang.getMessage("command.leave-but-not-in-a-game"));
                return true;
            }
            gameInstance.leave(SimplePlayer.of(commandSender));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of("join", "leave");
    }
}
