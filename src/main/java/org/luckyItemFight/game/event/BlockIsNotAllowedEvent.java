package org.luckyItemFight.game.event;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.util.MapTree;
import org.luckyItemFight.game.main.GameInstance;

import java.util.concurrent.TimeUnit;

public class BlockIsNotAllowedEvent extends AbstractEvent{
    private GameInstance gameInstance;
    private WrappedTask task;
    private static final MapTree config = getEvents().getSection("BlockIsNotAllowedEvent");

    public BlockIsNotAllowedEvent(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    @Override
    void execute() {
        send(config, gameInstance);
    }

    /**
     * 在duration后定时结束事件
     * @param duration 时间，单位 tick
     */
    @Override
    public void execute(int duration) {
        task = PlanetLib.getScheduler().runLater(this::stop, duration * 50L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if(stopping) return;
        stopping = true;

        if(task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
        gameInstance = null;
    }

    @Override
    public String getID() {
        return "BlockIsNotAllowedEvent";
    }
}
