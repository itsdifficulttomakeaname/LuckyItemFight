package org.luckyItemFight.game.main;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.util.Config;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.MapTree;
import cn.jason31416.planetlib.util.Util;
import cn.jason31416.planetlib.wrapper.SimpleLocation;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import cn.jason31416.planetlib.wrapper.SimpleWorld;
import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.luckyItemFight.Main;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.listener.GameListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.luckyItemFight.Main.instance;

@SuppressWarnings({"unchecked","deprecation"})
public class GameInstance {
    @Getter private static final Map<String, GameInstance> worldInstances = new HashMap<>();
    @Getter private static final Map<SimplePlayer, GameInstance> playerInstances = new HashMap<>();
    private static final String CONFIG_PATH = "worlds";
    private static final MapTree randomItems;
    private static GameInstance startingInstance=null;

    public static GameInstance getStartingInstance(){
        if(startingInstance==null) return createGame();
        return startingInstance;
    }

    static {
        try {
            randomItems = MapTree.fromYaml(new String(Main.class.getClassLoader().getResourceAsStream("__randomlist__.yml").readAllBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter private final World gameWorld;
    private static final Map<String,List<Material>> materials = new HashMap<>();
    private static final Map<String, Integer> chances = new HashMap<>();

    private int waitingTime = Integer.MAX_VALUE;
    @Getter private GameState state = GameState.WAITING;
    private final Map<SimplePlayer, PlayerState> players = new HashMap<>();
    private int alivePlayers = 0;
    private final Map<SimplePlayer, Integer> scores = new HashMap<>();
    private final List<SimpleLocation> spawnLocations = new ArrayList<>();
    private final List<SimplePlayer> leaderboard = new ArrayList<>();
    private final WrappedTask tickTask;

    public MapTree getWorldConfig() {
        return Config.getSection("worlds-config");
    }

    public GameInstance(World world) {
        gameWorld = world;
        worldInstances.put(world.getName(), this);
        startingInstance = this;
        loadConfig();

        // 实例主循环(生命周期持续到本局游戏结束)
        tickTask = PlanetLib.getScheduler().runTimer(new Runnable() {
            private int tick = getWorldConfig().getInt("time-per-round");
            @Override
            public void run() {
                if (state == GameState.WAITING) {
                    if (waitingTime != Integer.MAX_VALUE) {
                        for (SimplePlayer player : players.keySet()) {
                            player.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                    TextComponent.fromLegacyText(
                                            Lang.getMessage("actionbar.countdown").add("time", waitingTime).toString()
                                    ));
                        }
                        --waitingTime;
                        if (waitingTime <= 0) {
                            state = GameState.RUNNING;
                            waitingTime = Integer.MAX_VALUE;
                            startingInstance = null;
                        }
                    }
                } else if (state == GameState.RUNNING) {
                    if (alivePlayers <= 1) {
                        endGame();
                        tickTask.cancel();
                        return;
                    }

                    if (tick == 0) {
                        for (var kv : players.entrySet()) {
                            if (!kv.getValue().equals(PlayerState.ALIVE)) continue;
                            kv.getKey().getPlayer().getInventory().addItem(new ItemStack(random()));
                        }
                        tick = GameInstance.this.getWorldConfig().getInt("time-per-round");
                        players.forEach((player,state) -> {
                            if(state.equals(PlayerState.ALIVE)) {
                                scores.put(player, scores.get(player) + Config.getInt("worlds-config.alive-award"));
                                player.sendMessage(Lang.getMessage("in-game.alive-award").add("award", Config.getInt("worlds-config.alive-award")));
                            }
                        });
                    }
                    --tick;
                }
            }
            @NotNull
            private Material random() {
                int total = 0;
                for(int i : chances.values()) total += i;
                int choose = new Random(System.currentTimeMillis()).nextInt(total);
                int current = 0;
                Material ret = null;
                for(var kv : chances.entrySet()) {
                    current += kv.getValue();
                    if(current >= choose) {
                        List<Material> _materials = materials.get(kv.getKey());
                        ret = _materials.get(new Random(System.currentTimeMillis()).nextInt(_materials.size()));
                        break;
                    }
                }
                if(ret != null) return ret;
                throw new PluginException("ERROR While playing", "couldn't select a random item");
            }
        },1,1);
    }

    private YamlConfiguration loadWorldConfig(String worldName) {
        File configFile = new File(instance.getDataFolder(),
                CONFIG_PATH + File.separator + worldName + ".yml");

        if (!configFile.exists()) {
            throw new PluginException("World Doesn't Exist", "config \"" + worldName + ".yml\" doesn't exist");
        }

        return YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadConfig() {
        List<String> spawnLocations = getWorldConfig().getStringList("spawn-locations");
        if(spawnLocations == null) throw new PluginException("Config's Section Doesn't Exist", "configurationSection \"spawn-locations\" doesn't exist");
        for(String arg : spawnLocations) {
            String[] split = arg.split(",");
            if(split.length < 3) throw new PluginException("ERROR While Loading", "found a spawn-location with fewer than 3 arguments");

            try {
                double _x,_y,_z;
                _x = Double.parseDouble(split[0]);
                _y = Double.parseDouble(split[1]);
                _z = Double.parseDouble(split[2]);
                this.spawnLocations.add(SimpleLocation.of(_x, _y, _z, SimpleWorld.of(gameWorld)));
            } catch (NumberFormatException e) {
                throw new PluginException("ERROR While Loading", "couldn't parse the key to double", e);
            }
        }
    }

    public static void initRandomItemList() {
        var section = randomItems.getSection("items");
        if(section == null) throw new PluginException("ERROR While Loading", "__randomlist__.yml -> items section is null");
        for(String key : section.getKeys()) {
            List<String> items = (List<String>) section.get(key);
            if(items == null) throw new PluginException("ERROR While Loading", "items section is null");
            final List<Material> _temp = new ArrayList<>();
            items.forEach(item -> _temp.add(Material.valueOf(item)));
            materials.put(key, _temp);
            chances.put(key, randomItems.getInt("weight." + key));
        }
    }

    public void join(SimplePlayer player,PlayerState playerState) {
        if(playerState.equals(PlayerState.DEAD)) throw new PluginException("impossible", "impossible(GameInstance::join),playerState shouldn't be DEAD but it does");
        if(state == GameState.WAITING) {
            if(!playerState.equals(PlayerState.ALIVE)) throw new PluginException("impossible", "impossible(GameInstance::join),player shouldn't join in a WAITING state room PlayerState of \"not ALIVE\"");

            players.put(player, playerState);
            scores.put(player, 0);

            player.teleport(spawnLocations.get(alivePlayers));

            ++ alivePlayers;
            adjustNewTime();
        } else if(state == GameState.RUNNING) {
            if(playerState.equals(PlayerState.ALIVE)) throw new PluginException("impossible", "impossible(GameInstance::join),player shouldn't join in a RUNNING state room with PlayerState of \"ALIVE\"");
            players.put(player, playerState);
        }
    }

    public void leave(SimplePlayer player) {
        PlayerState _state = players.get(player);
        GameListener.getEntityBelongings().entrySet().removeIf(entry -> entry.getValue().equals(player));
        players.put(player, null);
        scores.remove(player);
        if(_state.equals(PlayerState.ALIVE)) --alivePlayers;
        for(SimplePlayer p : players.keySet()) {
            p.sendMessage(Lang.getMessage("in-game.leave").add("player", player.getName()));
        }
    }

    public void eliminate(SimplePlayer player) {
        -- alivePlayers;
        players.put(player, PlayerState.DEAD);
        player.getPlayer().setGameMode(GameMode.SPECTATOR);
        if (player.isOnline()) {
            String[] ret = getWorldConfig().getString("spectator-spawn-location").split(",");
            SimpleLocation loc = SimpleLocation.of(Double.parseDouble(ret[0]),
                    Double.parseDouble(ret[1]),
                    Double.parseDouble(ret[2]),
                    SimpleWorld.of(gameWorld));
            player.teleport(loc);
        }
        for(SimplePlayer p : players.keySet()) {
            p.sendMessage(Lang.getMessage("in-game.eliminate").add("player", player.getName()));
        }

        leaderboard.add(player);
    }

    public void eliminate(SimplePlayer killer,SimplePlayer victim) {
        -- alivePlayers;
        players.put(victim, PlayerState.DEAD);
        if (victim.isOnline()) {
            victim.getPlayer().setGameMode(GameMode.SPECTATOR);
            String[] ret = getWorldConfig().getString("spectator-spawn-location").split(",");
            SimpleLocation loc = SimpleLocation.of(Double.parseDouble(ret[0]),
                    Double.parseDouble(ret[1]),
                    Double.parseDouble(ret[2]),
                    SimpleWorld.of(gameWorld));
            victim.teleport(loc);
        }
        for(SimplePlayer p : players.keySet()) {
            p.sendMessage(Lang.getMessage("in-game.eliminate-by-other").add("killer",killer.getName()).add("victim",victim.getName()));
        }
        scores.put(killer, scores.get(killer) + Config.getInt("worlds-config.kill-award"));
        killer.sendMessage(Lang.getMessage("in-game.kill-award").add("award", Config.getInt("worlds-config.kill-award")));

        leaderboard.add(victim);
    }

    private void adjustNewTime() {
        if(alivePlayers < getWorldConfig().getInt("least-player")){
            waitingTime = Integer.MAX_VALUE;
            return;
        }
        for(String key: getWorldConfig().getSection("time-map").getKeys()) {
            if(alivePlayers < Integer.parseInt(key)){
                break;
            }
            waitingTime = Math.min(waitingTime, getWorldConfig().getInt("time-map."+key));
        }
    }

    public void endGame() {
        Collections.reverse(leaderboard);
        showLeaderBoard();

        destroy();
    }

    private void showLeaderBoard() {
        players.keySet().forEach(k -> {
            k.sendMessage(Lang.getMessage("in-game.leaderboard.header"));
            final int[] i = {1};
            players.forEach((p,s) -> {
                if(i[0] == 1) {
                    k.sendMessage(Lang.getMessage("in-game.leaderboard._1st").add("player", p.getName())
                            .add("score", scores.get(p))
                            .add("it-is-me", p.equals(k) ? Lang.getMessage("in-game.leaderboard.it-is-me") : Message.of("")));
                } else if(i[0] == 2) {
                    k.sendMessage(Lang.getMessage("in-game.leaderboard._2nd")
                            .add("player", p.getName()).add("score", scores.get(p))
                            .add("it-is-me", p.equals(k) ? Lang.getMessage("in-game.leaderboard.it-is-me") : Message.of("")));
                } else if(i[0] == 3) {
                    k.sendMessage(Lang.getMessage("in-game.leaderboard._3rd")
                            .add("player", p.getName()).add("score", scores.get(p))
                            .add("it-is-me", p.equals(k) ? Lang.getMessage("in-game.leaderboard.it-is-me") : Message.of("")));
                } else {
                    k.sendMessage(Lang.getMessage("in-game.leaderboard.normal")
                            .add("player", p.getName()).add("score", scores.get(p))
                            .add("it-is-me", p.equals(k) ? Lang.getMessage("in-game.leaderboard.it-is-me") : Message.of("")));
                } ++i[0];
            });
            k.sendMessage(Lang.getMessage("in-game.leaderboard.footer"));
        });
    }

    public void destroy(){
        resetWorld();
        if(!tickTask.isCancelled()) tickTask.cancel();
    }

    @SneakyThrows
    public void resetWorld() {
        GameListener.getEntityBelongings().entrySet().removeIf(entry -> GameInstance.getPlayerInstances().get(entry.getValue()).equals(this));

        gameWorld.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation()));
        File worldFolder = new File(gameWorld.getName());
        Bukkit.unloadWorld(gameWorld, true);
        Files.delete(worldFolder.toPath());
    }

    @SneakyThrows
    private static World createWorld(String worldName) {
        Util.saveFolder("battle-1");


        World world = Bukkit.getWorld(worldName);

        if(world == null) throw new PluginException("ERROR While Loading", "GameWorld is null");
        return world;
    }

    public static GameInstance createGame() {
        StringBuilder sb = new StringBuilder();
        sb.append("world_itemfight_");
        String psb = "qwertyuiopasdfghjklzxcvbnm1234567890";
        Random random = new Random();
        for(int i=0;i<12;i++) {
            sb.append(psb.charAt(random.nextInt(psb.length())));
        }
        return new GameInstance(createWorld(sb.toString()));
    }
}
