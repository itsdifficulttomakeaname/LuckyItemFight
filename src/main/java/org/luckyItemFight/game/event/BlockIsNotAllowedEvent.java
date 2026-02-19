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
        boolean showName = config.getBoolean("show-name.enable");
        boolean showDetail = config.getBoolean("show-detail.enable");
        String name,nameType,detail,detailType;
        if(showName) {
            name = config.getString("show-name.text");
            nameType = config.getString("show-name.type");
        } else {
            name = "";
            nameType = "";
        }
        if(showDetail) {
            detail = config.getString("show-detail.text");
            detailType = config.getString("show-detail.type");
        } else {
            detail = "";
            detailType = "";
        }
        gameInstance.getPlayers().forEach((p,s) -> {
            send(p, nameType, name, showName);
            send(p, detailType, detail, showDetail);
        });
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

        if(task != null && !task.isCancelled()) task.cancel();
        gameInstance = null;
    }

    @Override
    public String getID() {
        return "BlockIsNotAllowedEvent";
    }
}
