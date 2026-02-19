package org.luckyItemFight.game.event;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.util.MapTree;
import org.bukkit.Location;
import org.bukkit.Material;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.game.main.PlayerState;
import org.luckyItemFight.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BlockDisAppearEvent extends AbstractEvent{
    private GameInstance gameInstance;
    private WrappedTask task;
    private WrappedTask gameTask;
    private final MapTree config = getEvents().getSection("BlockDisAppearEvent");

    public BlockDisAppearEvent(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    @Override
    void execute() {
        int times = config.getInt("executeTimes");
        int[] cnt = new int[]{0};
        gameTask = PlanetLib.getScheduler().runTimer(() -> {
            ++ cnt[0];
            if(cnt[0] > times) gameTask.cancel();
            Random random = ThreadLocalRandom.current();
            gameInstance.getPlayers().forEach((p,s) -> {
                if(s != PlayerState.ALIVE) return;
                Location loc = p.getPlayer().getLocation().clone();
                gameInstance.getGameWorld().getBlockAt(loc.add(random.nextInt(5) - 2, random.nextInt(3), random.nextInt(5) - 2)).setType(Material.AIR);
            });
        }, config.getInt("executePerXTick"), config.getInt("executePerXTick"));
        send(config, gameInstance);
    }

    @Override
    public void execute(int duration) {
        execute();
        task = PlanetLib.getScheduler().runLater(this::stop, duration*50L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if(stopping) return;
        stopping = true;

        if(task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }

        if(gameTask != null && !gameTask.isCancelled()) {
            gameTask.cancel();
        }
        gameTask = null;

        gameInstance = null;
    }

    @Override
    public String getID() {
        return "BlockDisAppearEvent";
    }
}
