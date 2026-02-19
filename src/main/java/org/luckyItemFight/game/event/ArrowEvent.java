package org.luckyItemFight.game.event;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.util.MapTree;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.game.main.PlayerState;

import java.util.concurrent.TimeUnit;

public class ArrowEvent extends AbstractEvent{
    private GameInstance gameInstance;
    private WrappedTask task;
    private WrappedTask gameTask;
    private final static MapTree config = getEvents().getSection("ArrowEvent");

    public ArrowEvent(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
    }

    @Override
    void execute() {
        double range = config.getDouble("range");
        double height = config.getDouble("height");
        gameTask = PlanetLib.getScheduler().runTimer(() -> gameInstance.getPlayers().forEach((p, s) -> {
            if (!s.equals(PlayerState.ALIVE)) return;
            // 在玩家头顶固定高度生成箭矢，并赋予向下的速度
            Location playerLoc = p.getPlayer().getLocation();
            double x = playerLoc.getX() + (Math.random() * 2 * range - range);
            double y = playerLoc.getY() + height;
            double z = playerLoc.getZ() + (Math.random() * 2 * range - range);

            Location spawnLoc = new Location(gameInstance.getGameWorld(), x, y, z);
            Arrow arrow = (Arrow) gameInstance.getGameWorld().spawnEntity(spawnLoc, EntityType.ARROW);
            arrow.setVelocity(new Vector(0, -1.0, 0)); // 给箭矢向下的速度
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED); // 禁止捡起
        }), config.getInt("duration"), config.getInt("duration"));

        send(config, gameInstance);
    }

    /**
     * 在duration后定时结束事件
     * @param duration 时间，单位 tick
     */
    @Override
    public void execute(int duration) {
        this.execute();
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
            gameTask = null;
        }
        gameInstance = null;
    }

    @Override
    public String getID() {
        return "ArrowEvent";
    }
}
