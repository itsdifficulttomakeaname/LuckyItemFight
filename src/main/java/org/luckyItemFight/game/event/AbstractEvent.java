package org.luckyItemFight.game.event;

import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.util.MapTree;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import lombok.Getter;
import net.kyori.adventure.title.Title;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.main.GameInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.luckyItemFight.Main.instance;

public abstract class AbstractEvent{
    @Getter private static MapTree eventsConfig;
    @Getter boolean stopping = false;
    static final String TITLE = "title";
    static final String SUBTITLE = "subtitle";
    static final String CHAT = "chat";
    public static AbstractEvent fromString(String name,GameInstance gameInstance) {
        return switch (name) {
            case "ArrowEvent" -> new ArrowEvent(gameInstance);
            case "NullEvent" -> new NullEvent(gameInstance);
            case "BlockIsNotAllowedEvent" -> new BlockIsNotAllowedEvent(gameInstance);
            case "NightEvent" -> new NightEvent(gameInstance);
            case "BlockDisAppearEvent" -> new BlockDisAppearEvent(gameInstance);
            default -> null;
        };
    }
    /**
     * 发送提示消息的工具方法
     * @param config 对应事件的配置段
     * @param gameInstance 该事件的源游戏实例
     * */
    void send(MapTree config, GameInstance gameInstance) {
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
     * 分类方式发送信息的方法
     * @param p 发送的目标玩家
     * @param type 目标方式
     * @param text 发送的消息
     * @param run 是否发送
     * */
    private void send(SimplePlayer p, String type, String text, boolean run) {
        if(!run) return;
        if (type.equalsIgnoreCase(TITLE))
            p.getPlayer().showTitle(Title.title(Message.of(text).toComponent(), Message.of("").toComponent()));
        else if (type.equalsIgnoreCase(SUBTITLE))
            p.getPlayer().showTitle(Title.title(Message.of("").toComponent(), Message.of(text).toComponent()));
        else if (type.equalsIgnoreCase(CHAT)) p.sendMessage(Message.of(text));
    }


    public static void init() {
        try {
            eventsConfig = MapTree.fromYaml(Files.readString(new File(instance.getDataFolder(), "__event__.yml").toPath()));
        } catch (IOException e) {
            throw new PluginException("ERROR While Loading","Cannot initialize __event__.yml",e);
        }
    }

    abstract void execute();
    /**
     * 在duration后定时结束事件
     * @param duration 时间，单位 tick
     *
     */
    public abstract void execute(int duration);
    public abstract void stop();
    public abstract String getID();
}
