package org.luckyItemFight.game.event;

import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.util.MapTree;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import lombok.Getter;
import net.kyori.adventure.title.Title;
import org.luckyItemFight.Main;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.main.GameInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.luckyItemFight.Main.instance;

public abstract class AbstractEvent{
    @Getter private static MapTree events;
    @Getter boolean stopping = false;
    static final String TITLE = "title";
    static final String SUBTITLE = "subtitle";
    static final String CHAT = "chat";
    public static AbstractEvent fromString(String name,GameInstance gameInstance) {
        return switch (name) {
//            case "WitherEvent" -> new WitherEvent(gameInstance);
            case "ArrowEvent" -> new ArrowEvent(gameInstance);
            case "NullEvent" -> new NullEvent(gameInstance);
            case "BlockIsNotAllowedEvent" -> new BlockIsNotAllowedEvent(gameInstance);
            case "NightEvent" -> new NightEvent(gameInstance);
            default -> null;
        };
    }

    void send(SimplePlayer p, String type, String text, boolean run) {
        if(!run) return;
        if (type.equalsIgnoreCase(TITLE))
            p.getPlayer().showTitle(Title.title(Message.of(text).toComponent(), Message.of("").toComponent()));
        else if (type.equalsIgnoreCase(SUBTITLE))
            p.getPlayer().showTitle(Title.title(Message.of("").toComponent(), Message.of(text).toComponent()));
        else if (type.equalsIgnoreCase(CHAT)) p.sendMessage(Message.of(text));
    }


    public static void init() {
        try {
//            events = MapTree.fromYaml(new String(Main.class.getClassLoader().getResourceAsStream("__event__.yml").readAllBytes()));
            events = MapTree.fromYaml(Files.readString(new File(instance.getDataFolder(), "__event__.yml").toPath()));
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
