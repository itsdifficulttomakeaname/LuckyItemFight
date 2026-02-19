package org.luckyItemFight;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.util.Config;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.luckyItemFight.command.*;
import org.luckyItemFight.game.event.AbstractEvent;
import org.luckyItemFight.game.listener.GameListener;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.manager.DataBaseManager;

import java.io.File;
import java.util.Objects;

public final class Main extends JavaPlugin {
    public static Main instance;
    public static GameListener gameListener;
    public static DataBaseManager dataBaseManager;

    @Override public void onEnable() {
        instance = this;

        PlanetLib.initialize(this);
        Util.savePluginResource("config.yml");
        Util.savePluginResource("__randomlist__.yml");
        Util.savePluginResource("__event__.yml");
        Util.savePluginResource("shop.yml");
        Util.saveFolder("lang");

        AbstractEvent.init();
        GameInstance.init();

        Config.start(this);
        gameListener = new GameListener();
        dataBaseManager = new DataBaseManager();
        GameInstance.scoreboardTask();
        GameInstance.worldsCleanTask();
        Bukkit.getPluginManager().registerEvents(gameListener, instance);
        Lang.init("lang/" + instance.getConfig().getString("lang") + ".yml");
        GameInstance.initRandomItemList();

        World world = Bukkit.getWorlds().getFirst();
        if(world != null) {
            world.setTime(6000L);
            world.setStorm(false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        }

        Objects.requireNonNull(Bukkit.getPluginCommand("join")).setExecutor(new JoinCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("leave")).setExecutor(new LeaveCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("spectate")).setExecutor(new SpectateCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("spectate")).setTabCompleter(new SpectateCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("shop")).setExecutor(new ShopCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("database")).setExecutor(new DatabaseCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("database")).setTabCompleter(new DatabaseCommand());
    }

    @Override public void onDisable() {
        for(var gi: GameInstance.getWorldInstances().values()){
            gi.resetWorld();
        }

        if(GameInstance.scoreboardTask != null && !GameInstance.scoreboardTask.isCancelled()) {
            GameInstance.scoreboardTask.cancel();
            GameInstance.scoreboardTask = null;
        }

        if(GameInstance.worldsCleanTask != null && !GameInstance.worldsCleanTask.isCancelled()) {
            GameInstance.worldsCleanTask.cancel();
            GameInstance.worldsCleanTask = null;
        }

        dataBaseManager.close();
        ShopCommand.getGui().destroy();
    }

    public File getInstanceFile() { return getFile(); }
}
