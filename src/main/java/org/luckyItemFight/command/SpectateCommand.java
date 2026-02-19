package org.luckyItemFight.command;

import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.game.main.GameState;
import org.luckyItemFight.game.main.PlayerState;

import java.util.ArrayList;
import java.util.List;

public class SpectateCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("该指令只能由玩家执行!");
            return true;
        }
        if(args.length < 1) {
            SimplePlayer.of(commandSender).sendMessage(Message.of("<#F72828>/spectate <world>"));
            return true;
        }
        String worldName = args[0];
        GameInstance gameInstance = GameInstance.getWorldInstances().get(worldName);
        if(gameInstance == null) {
            SimplePlayer.of(commandSender).sendMessage(Message.of("<#F72828>world is invalid (目标世界无效)"));
            return true;
        }
        gameInstance.join(SimplePlayer.of(commandSender), PlayerState.SPECTATOR);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        final ArrayList<String> list = new ArrayList<>();
        GameInstance.getWorldInstances().forEach((string, gameInstance) -> {
            if(gameInstance.getState().equals(GameState.RUNNING)) return;
            list.add(string);
        });
        return list;
    }
}
