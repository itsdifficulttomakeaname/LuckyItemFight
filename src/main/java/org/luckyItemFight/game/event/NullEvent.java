package org.luckyItemFight.game.event;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.util.MapTree;
import lombok.Getter;
import org.luckyItemFight.game.main.GameInstance;

import java.util.concurrent.TimeUnit;

public class NullEvent extends AbstractEvent{
    @Getter private final static MapTree config = getEventsConfig().getSection("NullEvent");
    private GameInstance gameInstance;
    private WrappedTask task;

    public NullEvent(GameInstance gameInstance) {
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

        if(gameInstance != null) {
            gameInstance = null;
        }
    }

    @Override
    public String getID() {
        return "NullEvent";
    }
}
