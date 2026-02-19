package org.luckyItemFight.game.event;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.util.MapTree;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.main.GameInstance;

import java.util.concurrent.TimeUnit;

public class WitherEvent extends AbstractEvent{
    private GameInstance gameInstance;
    private WrappedTask task;
    private final static MapTree config = getEvents().getSection("WitherEvent");

    private Entity wither;
    public WitherEvent(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    @Override
    void execute() {
        String[] location = config.getString("spawn-location").split(",");
        if(location.length < 3) throw new PluginException("ERROR While Playing", "Location of spawning wither is illegal");
        Location loc = new Location(gameInstance.getGameWorld(), Double.parseDouble(location[0]), Double.parseDouble(location[1]), Double.parseDouble(location[2]));
        wither = gameInstance.getGameWorld().spawnEntity(loc, EntityType.WITHER);
        send(config, gameInstance);
    }

    /**
     * 在duration后定时结束事件
     * @param duration 时间，单位 tick
     * */
    @Override
    public void execute(int duration) {
        execute();
        task = PlanetLib.getScheduler().runLater(this::stop, duration*50L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if(stopping) return;
        stopping = true;

        if(wither != null && wither.isValid()) {
            wither.remove();
            wither = null;
        }
        if(task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
        gameInstance = null;
    }

    @Override
    public String getID() {
        return "WitherEvent";
    }
}
