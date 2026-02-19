package org.luckyItemFight.game.main;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.util.Config;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.MapTree;
import cn.jason31416.planetlib.wrapper.SimpleLocation;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import cn.jason31416.planetlib.wrapper.SimpleWorld;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.luckyItemFight.Main;
import org.luckyItemFight.util.Pair;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.event.AbstractEvent;
import org.luckyItemFight.game.listener.GameListener;
import org.luckyItemFight.manager.ScoreboardManager;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.luckyItemFight.Main.instance;

@SuppressWarnings({"unchecked"})
public class GameInstance {
    @Getter private static final Map<GameInstance, GameTask> gameTaskInstances = new HashMap<>();
    @Getter private static final Map<String, GameInstance> worldInstances = new HashMap<>();
    @Getter private static final Map<SimplePlayer, GameInstance> playerInstances = new HashMap<>();

    public void decrementWaitingTime() {
        waitingTime--;
    }

    public void incrementScore(SimplePlayer player, int amount) {
        scores.put(player, scores.getOrDefault(player, 0) + amount);
    }
    private static final Map<String, GameInstance> everUsedWorlds = new HashMap<>();
    private static MapTree randomItems;
    @Setter private static GameInstance startingInstance = null;

    public static GameInstance getStartingInstance(){
        if(startingInstance == null) return createGame();
        return startingInstance;
    }

    @Getter public static final Map<String, Integer> events = new HashMap<>();
    @Getter private static final Set<Material> BlockIsNotAllowedEventMaterial = new HashSet<>();

    public static void init() {
        try {
            randomItems = MapTree.fromYaml(Files.readString(new File(instance.getDataFolder(), "__randomlist__.yml").toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter private final World gameWorld;
    @Getter private static final Map<String,List<Material>> materials = new HashMap<>();
    @Getter private static final Map<String, Integer> chances = new HashMap<>();


    @Getter @Setter private int waitingTime = Integer.MAX_VALUE;
    @Getter @Setter private GameState state = GameState.WAITING;
    @Getter private final Map<SimplePlayer, @NotNull PlayerState> players = new HashMap<>();
    @Getter private int alivePlayers = 0;
    private final Map<SimplePlayer, Integer> scores = new HashMap<>();
    private final List<Pair<SimpleLocation, SimplePlayer>> spawnLocations = new ArrayList<>();
    private final List<SimplePlayer> leaderboard = new ArrayList<>();

    static final class Killer {
        public static final Map<SimplePlayer, Killer> killerInstance = new HashMap<>();
        private final SimplePlayer player;
        private int kill;

        private Killer(SimplePlayer player, int kill) {
            killerInstance.put(player, this);
            this.player = player;
            this.kill = kill;
        }

        public SimplePlayer player() {
            return player;
        }

        public int kill() {
            return kill;
        }
    }
    @Getter private static final Map<GameInstance, List<Killer>> killerLeaderboard = new HashMap<>();
    private final WrappedTask tickTask;
    private final Object ending = new Object();


    public MapTree getWorldConfig() {
        return Config.getSection("worlds-config");
    }

    public GameInstance(World world) {
        gameWorld = world;
        worldInstances.put(world.getName(), this);
        killerLeaderboard.put(this, new ArrayList<>());
        startingInstance = this;
        AbstractEvent.getEvents().getKeys().forEach(k -> events.put(k, AbstractEvent.getEvents().getInt(k + ".possibility")));
        AbstractEvent.getEvents().getStringList("BlockIsNotAllowedEvent.items").forEach(s -> BlockIsNotAllowedEventMaterial.add(Material.valueOf(s)));
        loadConfig();

        // 实例主循环(生命周期持续到本局游戏结束)
        tickTask = PlanetLib.getScheduler().runTimer(new GameTask(this), 1, 1);
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
                this.spawnLocations.add(new Pair<>(SimpleLocation.of(_x, _y, _z, SimpleWorld.of(gameWorld)), null));
            } catch (NumberFormatException e) {
                throw new PluginException("ERROR While Loading", "couldn't parse the key to double", e);
            }
        }
    }

    public static void initRandomItemList() {
        MapTree section = randomItems.getSection("items");
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
            if(alivePlayers >= spawnLocations.size()) {
                player.sendMessage(Lang.getMessage("command.room-is-full"));
                return;
            }
            players.put(player, playerState);
            if (playerState == PlayerState.ALIVE) {
                killerLeaderboard.get(this).add(new Killer(player, 0));
                scores.put(player, 0);

                for(int i = alivePlayers;i < spawnLocations.size();i ++) {
                    Pair<SimpleLocation, SimplePlayer> p = spawnLocations.get(i);
                    if(p.second() == null) {
                        player.teleport(p.first());
                        spawnLocations.set(i, new Pair<>(p.first(), player));
                        break;
                    }
                }

                ++alivePlayers;

                players.forEach((p,state) -> p.sendMessage(Lang.getMessage("common.player-join").add("player", player.getName())));

                adjustNewTime();
            } else {
                player.getPlayer().setGameMode(GameMode.SPECTATOR);
                String[] stringLoc = getWorldConfig().getString("spectator-spawn-location").split(",");
                if(stringLoc.length < 3) throw new PluginException("ERROR While Playing", "Cannot correctly get spectator's spawn location[1]");
                Location loc = new Location(
                        gameWorld,
                        Double.parseDouble(stringLoc[0]),
                        Double.parseDouble(stringLoc[1]),
                        Double.parseDouble(stringLoc[2])
                );

                player.teleport(SimpleLocation.of(loc));
            }
        } else if(state == GameState.RUNNING) {
            if(playerState.equals(PlayerState.ALIVE)) throw new PluginException("impossible", "impossible(GameInstance::join),player shouldn't join in a RUNNING state room with PlayerState of \"ALIVE\"");
            players.put(player, playerState);
            player.getPlayer().setGameMode(GameMode.SPECTATOR);
            String[] stringLoc = getWorldConfig().getString("spectator-spawn-location").split(",");
            if(stringLoc.length < 3) throw new PluginException("ERROR While Playing", "Cannot correctly get spectator's spawn location[2]");
            Location loc = new Location(
                    gameWorld,
                    Double.parseDouble(stringLoc[0]),
                    Double.parseDouble(stringLoc[1]),
                    Double.parseDouble(stringLoc[2])
            );

            player.teleport(SimpleLocation.of(loc));
        }
        playerInstances.put(player, this);
    }

    public void leave(SimplePlayer player) {
        PlayerState _state = players.get(player);
        GameListener.getEntityBelongings().entrySet().removeIf(entry -> entry.getValue().equals(player));
        players.remove(player);
        if(_state != null && _state.equals(PlayerState.ALIVE)) --alivePlayers;
        if(state.equals(GameState.WAITING)) adjustNewTime();
        for(SimplePlayer p : players.keySet()) p.sendMessage(Lang.getMessage("common.player-leave").add("player", player.getName()));

        for(int i = 0;i < spawnLocations.size();i ++) {
            Pair<SimpleLocation, SimplePlayer> p = spawnLocations.get(i);
            if(p.second().equals(player)) {
                spawnLocations.set(i, new Pair<>(p.first(), null));
                break;
            }
        }

        playerInstances.remove(player);
        if(player.isOnline()) {
            player.teleport(SimpleLocation.of(Bukkit.getWorlds().get(0).getSpawnLocation()));
            player.getPlayer().setGameMode(GameMode.ADVENTURE);
        }
    }

    public void eliminate(SimplePlayer player) {
        -- alivePlayers;
        players.put(player, PlayerState.DEAD);
        player.getPlayer().setGameMode(GameMode.SPECTATOR);
        if (player.isOnline()) {
            String[] ret = getWorldConfig().getString("spectator-spawn-location").split(",");
            if(ret.length < 3) throw new PluginException("ERROR While Playing", "Spectator's spawn-location has fewer than 3 arguments!");
            SimpleLocation loc = SimpleLocation.of(
                    Double.parseDouble(ret[0]),
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
            if(ret.length < 3) throw new PluginException("ERROR While Playing", "Spectator's spawn-location has fewer than 3 arguments!");
            SimpleLocation loc = SimpleLocation.of(
                    Double.parseDouble(ret[0]),
                    Double.parseDouble(ret[1]),
                    Double.parseDouble(ret[2]),
                    SimpleWorld.of(gameWorld));
            victim.teleport(loc);
        }
        for(SimplePlayer p : players.keySet()) {
            p.sendMessage(Lang.getMessage("in-game.eliminate-by-other").add("killer", killer.getName()).add("victim", victim.getName()));
        }
        scores.put(killer, scores.get(killer) + Config.getInt("worlds-config.kill-award"));
        killer.getPlayer().playSound(killer.getPlayer(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 2f, 2f);
        killer.sendMessage(Lang.getMessage("in-game.kill-award").add("award", Config.getInt("worlds-config.kill-award")));

        Killer.killerInstance.get(killer).kill ++;

        leaderboard.add(victim);
    }

    private void adjustNewTime() {
        if(alivePlayers < getWorldConfig().getInt("least-player")) {
            waitingTime = Integer.MAX_VALUE;
            return;
        }
        for(String key : getWorldConfig().getSection("time-map").getKeys()) {
            if(alivePlayers < Integer.parseInt(key)){
                break;
            }
            waitingTime = Math.min(waitingTime, getWorldConfig().getInt("time-map."+key));
        }
    }

    public void endGame() {
        synchronized (ending){
            if (alivePlayers == 1) {
                players.forEach((p, s) -> {
                    if (s.equals(PlayerState.ALIVE)) leaderboard.add(p);
                });
            }

            Collections.reverse(leaderboard);
            showLeaderBoard();

            destroy();
        }
    }

    private void showLeaderBoard() {
        players.keySet().forEach(k -> {
            k.sendMessage(Lang.getMessage("in-game.leaderboard.header"));
            final int[] i = {1};
            leaderboard.forEach(p -> {
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
        everUsedWorlds.put(gameWorld.getName(), null);
        killerLeaderboard.remove(this);
        if(!tickTask.isCancelled()) tickTask.cancel();
        gameTaskInstances.remove(this);
    }

    @SneakyThrows
    public void resetWorld() {
        GameListener.getEntityBelongings().entrySet().removeIf(entry -> GameInstance.getPlayerInstances().get(entry.getValue()).equals(this));

        players.forEach((player,state) -> {
            Player p = player.getPlayer();
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.setFoodLevel(20);
            p.setExp(0);
            p.setLevel(0);
            p.setHealth(20);
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            ScoreboardManager.getPlayerScoreboard().get(SimplePlayer.of(p)).clear();

            playerInstances.remove(SimplePlayer.of(p));
            String uuidOfPlayer = p.getUniqueId().toString();
            Map<String,Object> mp = Main.dataBaseManager.getPlayer(uuidOfPlayer);
            Main.dataBaseManager.update(uuidOfPlayer, "play", (int) mp.get("play") + 1);
            Main.dataBaseManager.update(uuidOfPlayer, "coin", (int) mp.get("coin") + scores.get(SimplePlayer.of(p)));
            if (leaderboard.get(0).equals(SimplePlayer.of(p))) Main.dataBaseManager.update(uuidOfPlayer, "win", (int) mp.get("win") + 1);
            Main.dataBaseManager.update(uuidOfPlayer, "kill", (int) mp.get("kill") + Killer.killerInstance.get(SimplePlayer.of(p)).kill());
        });
        File worldFolder = new File(gameWorld.getName());
        if(Bukkit.unloadWorld(gameWorld, true)) {
            Bukkit.getScheduler().runTaskLater(instance, () -> {
                try {
                    FileUtils.deleteDirectory(worldFolder);
                } catch (IOException ignored) {}
            },20L);
        }
    }

    @SneakyThrows
    private static void copyWorld(String worldName) {
        File pluginFile = instance.getInstanceFile();
        File targetDir = new File(instance.getServer().getWorldContainer(), worldName);

        targetDir.mkdirs();

        try (JarFile jar = new JarFile(pluginFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith("battle-1/") && !name.equals("battle-1/")) {
                    String relPath = name.substring("battle-1/".length());
                    File destFile = new File(targetDir, relPath);

                    if (entry.isDirectory()) {
                        destFile.mkdirs();
                    } else {
                        destFile.getParentFile().mkdirs();
                        try (InputStream in = jar.getInputStream(entry);
                             OutputStream out = new FileOutputStream(destFile)) {
                            in.transferTo(out);
                        }
                    }
                }
            }
        }
    }

    @SneakyThrows
    private static World createWorld(String worldName) {
        copyWorld(worldName);

        World world = Bukkit.getWorld(worldName);

        if(world == null) {
            try {
                WorldCreator worldCreator = new WorldCreator(worldName);

                File levelDat =  new File(new File(instance.getServer().getWorldContainer(), worldName), "level.dat");
                if(levelDat.exists()) {
                    worldCreator.copy(new WorldCreator(worldName));
                }

                world = worldCreator.createWorld();
                if(world != null) {
                    world.setAutoSave(false);
                    world.setDifficulty(Difficulty.NORMAL);
                    world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
                }
                return world;
            } catch (Exception ex) {
                throw new PluginException("ERROR While Loading", "GameWorld is null");
            }
        }
        return world;
    }

    public static GameInstance createGame() {
        StringBuilder sb = new StringBuilder();
        sb.append("world_itemfight_");
        String psb = "qwertyuiopasdfghjklzxcvbnm1234567890";
        Random random = ThreadLocalRandom.current();
        for(int i=0;i<12;i++) {
            sb.append(psb.charAt(random.nextInt(psb.length())));
        }
        if(Bukkit.getWorld(sb.toString()) != null) return createGame(); // 防重(尽管可能性 实例数/4738381338321616896)
        return new GameInstance(createWorld(sb.toString()));
    }

    public static WrappedTask scoreboardTask;

    // 本方法存在大量本地数据库读(没有"写"这个操作)
    @SneakyThrows
    public static void scoreboardTask() {
        scoreboardTask = PlanetLib.getScheduler().runTimer(() ->
                ScoreboardManager.getPlayerScoreboard().forEach((player, scoreboardManager) -> {
                    GameInstance gameInstance = GameInstance.getPlayerInstances().get(player);
                    if(gameInstance == null || gameInstance.getState().equals(GameState.WAITING)) {
                        String uuidOfPlayer = player.getPlayer().getUniqueId().toString();
                        Map<String, Object> mp = Main.dataBaseManager.getPlayer(uuidOfPlayer);
                        List<String> lines = Lang.getMessageList("scoreboard.lobby.main")
                                .add("player", player.getName())
                                .add("coin", mp.get("coin"))
                                .add("play",mp.get("play"))
                                .add("win", mp.get("win"))
                                .add("kill", mp.get("kill"))
                                .asList();
                        int i=1;
                        for(String line : lines) {
                            scoreboardManager.setLine(i, line);
                            ++ i;
                        }
                        scoreboardManager.flushChanges();
                    } else {
                        List<Killer> topKillers = killerLeaderboard.get(GameInstance.getPlayerInstances().get(player));
                        if(topKillers == null) return;
                        topKillers.sort((a, b) -> b.kill - a.kill);
                        int time = gameTaskInstances.get(GameInstance.getPlayerInstances().get(player)).getCurrentGameTime();
                        List<String> lines = Lang.getMessageList("scoreboard.in-game.main")
                                .add("player", player.getName())
                                .add("1stKiller", !topKillers.isEmpty() ? topKillers.get(0).player().getName() : "null")
                                .add("1stAmount", !topKillers.isEmpty() ? topKillers.get(0).kill() : "null")
                                .add("2ndKiller", topKillers.size() > 1 ? topKillers.get(1).player().getName() : "null")
                                .add("2ndAmount", topKillers.size() > 1 ? topKillers.get(1).kill() : "null")
                                .add("3rdKiller", topKillers.size() > 2 ? topKillers.get(2).player().getName() : "null")
                                .add("3rdAmount", topKillers.size() > 2 ? topKillers.get(2).kill() : "null")
                                .add("kill", Killer.killerInstance.get(player).kill())
                                .add("score", GameInstance.getPlayerInstances().get(player).scores.get(player))
                                .add("time", time)
                                .add("TimedEvent", AbstractEvent.getEvents().getInt("WitherEvent.run-after") / 20 < time ? "&c已生成" : "&a" + (AbstractEvent.getEvents().getInt("WitherEvent.run-after") / 20 - time))
                                .asList();
                        int i=1;
                        for(String line : lines) {
                            scoreboardManager.setLine(i, line);
                            ++ i;
                        }
                        scoreboardManager.flushChanges();
                    }
        }),1,1, TimeUnit.SECONDS);
    }

    public static WrappedTask worldsCleanTask;

    public static void worldsCleanTask() {
        worldsCleanTask = PlanetLib.getScheduler().runTimer(() -> {
            final Set<String> canDelete = new HashSet<>();
            for(Map.Entry<String, GameInstance> entry : everUsedWorlds.entrySet()) {
                String world = entry.getKey();
                GameInstance gameInstance = entry.getValue();
                if (gameInstance != null && gameInstance.getAlivePlayers() > 0) continue; // 实例存活(且有玩家存在)

                if (Bukkit.getWorld(world) == null && !new File(world).exists()) { // 实例死亡/已卸载/数据文件夹不存在
                    canDelete.add(world);
                    continue;
                }

                if (Bukkit.unloadWorld(world, false)) {
                    Bukkit.getScheduler().runTaskLater(instance, () -> {
                        try {
                            FileUtils.deleteDirectory(new File(world));
                        } catch (IOException e) {
                            throw new PluginException("ERROR While Playing", "Cannot delete deprecated gameWorld folder", e);
                        }
                    }, 20L);
                } else {
                    World w = Bukkit.getWorld(world);
                    if(w == null) return;
                    w.getPlayers().forEach(p -> {
                        p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                        GameInstance.getPlayerInstances().remove(SimplePlayer.of(p));
                        p.setGameMode(GameMode.ADVENTURE);
                        p.getInventory().clear();
                    });

                }

                if(!new File(world).exists()) canDelete.add(world);
            }

            canDelete.forEach(everUsedWorlds::remove);
        },1,1,TimeUnit.SECONDS);
    }
}
