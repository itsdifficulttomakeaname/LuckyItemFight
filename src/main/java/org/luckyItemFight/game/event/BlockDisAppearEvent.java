package org.luckyItemFight.game.event;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.util.MapTree;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.game.main.PlayerState;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.luckyItemFight.Main.instance;

public class BlockDisAppearEvent extends AbstractEvent {
    private GameInstance gameInstance;
    private WrappedTask task;
    private WrappedTask gameTask;
    private static final Random R = new Random();
    private final MapTree config = getEventsConfig().getSection("BlockDisAppearEvent");

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
            gameInstance.getPlayers().forEach((p,s) -> {
                if(s != PlayerState.ALIVE) return;
                Location loc = p.getPlayer().getLocation().clone();
                int _x,_y,_z;
                _x = R.nextInt(5) - 2;
                _y = R.nextInt(3);
                _z = R.nextInt(5) - 2;
                Block b = gameInstance.getGameWorld().getBlockAt(loc.add(_x,_y,_z));
                // FIXME 查不出来为什么这个log不触发
                instance.getLogger().info(String.format("Caught block at %d %d %d,type is %s", _x, _y, _z, b.getType().name()));
                b.setType(Material.AIR);
            });
        }, config.getInt("executePerXTick")*50L, config.getInt("executePerXTick")*50L, TimeUnit.MILLISECONDS);
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
