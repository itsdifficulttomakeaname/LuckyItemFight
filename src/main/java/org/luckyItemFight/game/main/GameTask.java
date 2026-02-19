package org.luckyItemFight.game.main;

import cn.jason31416.planetlib.util.Config;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.Audiences;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.event.AbstractEvent;
import org.luckyItemFight.game.event.BlockIsNotAllowedEvent;
import org.luckyItemFight.game.event.WitherEvent;
import org.luckyItemFight.manager.ShopManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("deprecation")
public class GameTask implements Runnable {
    private final GameInstance gameInstance;
    private int tick;
    private int secondCounter = 20;
    @Getter private int currentGameTime = 0;
    @Getter private AbstractEvent event = null;
    private int totalWeight = 0;
    private boolean hasAppearedArrow = false;
    private boolean hasAppearedNull = false;
    private boolean hasAppearedBlockIsNotAllowed = false;
    private boolean hasNightEvent = false;
    private boolean hasTimedEvent = false;

    public GameTask(GameInstance gameInstance) {
        this.gameInstance = gameInstance;
        this.tick = gameInstance.getWorldConfig().getInt("time-per-round");
        GameInstance.getGameTaskInstances().put(gameInstance, this);
    }

    @Override
    public void run() {
        if (gameInstance.getState() == GameState.WAITING) {
            if (gameInstance.getWaitingTime() != Integer.MAX_VALUE) {
                for (var player : gameInstance.getPlayers().keySet()) {
                    Lang.getMessage("actionbar.countdown").add("time", gameInstance.getWaitingTime()).sendActionbar(player);
                }
                --secondCounter;
                if (secondCounter == 0) {
                    gameInstance.decrementWaitingTime();
                    secondCounter = 20;
                }
                if (gameInstance.getWaitingTime() <= 0) {
                    gameInstance.setState(GameState.RUNNING);
                    gameInstance.setWaitingTime(Integer.MAX_VALUE);
                    GameInstance.setStartingInstance(null);
                    gameInstance.getPlayers().keySet().forEach(p -> {
                        p.getPlayer().setGameMode(GameMode.SURVIVAL);
                        p.getPlayer().setLevel(0);
                        p.getPlayer().setExp(0);
                        p.getPlayer().setFoodLevel(20);
                        p.getPlayer().setHealth(20);
                        p.getPlayer().getInventory().clear();
                    });
                    secondCounter = 20;

                    gameInstance.getPlayers().forEach((p,s) -> {
                        if(!(s == PlayerState.ALIVE)) return;
                        var material = ShopManager.get(p);
                        if(material == null) return;
                        material.forEach((m,a) -> {
                            ItemStack item = new ItemStack(m);
                            item.setAmount(a);
                            p.getPlayer().getInventory().addItem(item);
                        });
                        ShopManager.playerData.remove(p);
                    });

                    // KillerLeaderboard去重
                    LinkedHashMap<SimplePlayer, GameInstance.Killer> map = new LinkedHashMap<>();
                    GameInstance.getKillerLeaderboard().get(gameInstance).forEach(k -> map.put(k.player(), k));
                    GameInstance.getKillerLeaderboard().put(gameInstance, new ArrayList<>(map.values()));
                }
            } else {
                for (var player : gameInstance.getPlayers().keySet()) {
                    Lang.getMessage("actionbar.waiting-more-player")
                            .add("current", gameInstance.getAlivePlayers())
                            .add("least", gameInstance.getWorldConfig().getInt("least-player"))
                            .sendActionbar(player);
                }
            }
        } else if (gameInstance.getState() == GameState.RUNNING) {
            if (gameInstance.getAlivePlayers() <= 1) {
                gameInstance.endGame();
                event.stop();
                return;
            }

            if (tick == 0) {
                if ((event == null || event.isStopping())) {
                    AbstractEvent newEvent;
                    do {
                        newEvent = randomEvent();
                    } while (compare(newEvent, event));
                    event = newEvent;

                    String id = event.getID();
                    boolean enabled = AbstractEvent.getEvents().getBoolean(id + ".end-in-duration.enable");
                    int duration = AbstractEvent.getEvents().getInt(id + ".end-in-duration.duration");
                    if (!enabled) event.execute(0);
                    else event.execute(duration);

                    if (!AbstractEvent.getEvents().getString(event.getID() + ".sound").equalsIgnoreCase("null")) {
                        gameInstance.getPlayers().keySet().forEach(p ->
                                p.getPlayer().playSound(
                                        p.getPlayer(),
                                        Sound.valueOf(AbstractEvent.getEvents().getString(event.getID() + ".sound")),
                                        SoundCategory.PLAYERS,
                                        1.0f,
                                        1.0f
                                )
                        );
                    }
                }

                for (var kv : gameInstance.getPlayers().entrySet()) {
                    if (!kv.getValue().equals(PlayerState.ALIVE)) continue;
                    kv.getKey().getPlayer().getInventory().addItem(new ItemStack(
                            event instanceof BlockIsNotAllowedEvent ? randomSpecialItem() : randomItem())
                    );
                }
                tick = gameInstance.getWorldConfig().getInt("time-per-round");
                gameInstance.getPlayers().forEach((player, state) -> {
                    if (state.equals(PlayerState.ALIVE)) {
                        gameInstance.incrementScore(player, Config.getInt("worlds-config.alive-award"));
                        Lang.getMessage("in-game.alive-award")
                                .add("award", Config.getInt("worlds-config.alive-award"))
                                .sendActionbar(player);
                    }
                });
            }
            -- tick;
            if(secondCounter == 0) {
                ++ currentGameTime;
                secondCounter = 20;
            }
            -- secondCounter;

            if(currentGameTime >= AbstractEvent.getEvents().getInt("WitherEvent.run-after") / 20 && !hasTimedEvent) {
                event = new WitherEvent(gameInstance);
                hasTimedEvent = true;
                event.execute(Integer.MAX_VALUE);
            }
        }
    }

    @NotNull
    private Material randomItem() {
        Random random = ThreadLocalRandom.current();

        if (totalWeight == 0) {
            totalWeight = GameInstance.getChances().values().stream().mapToInt(Integer::intValue).sum();
        }

        int choose = random.nextInt(totalWeight);
        int current = 0;

        for (Map.Entry<String, Integer> entry : GameInstance.getChances().entrySet()) {
            current += entry.getValue();
            if (current >= choose) {
                List<Material> materialList = GameInstance.getMaterials().get(entry.getKey());
                if (materialList != null && !materialList.isEmpty()) {
                    return materialList.get(random.nextInt(materialList.size()));
                }
                break;
            }
        }

        throw new PluginException("ERROR While playing", "couldn't select a random item");
    }

    @NotNull
    private Material randomSpecialItem() {
        Random random = ThreadLocalRandom.current();
        List<Material> list = GameInstance.getBlockIsNotAllowedEventMaterial().stream().toList();
        return list.get(random.nextInt(list.size()));
    }

    @NotNull
    private AbstractEvent randomEvent() {
        int total = 0;
        for (int i : GameInstance.getEvents().values()) total += i;
        int choose = ThreadLocalRandom.current().nextInt(total);
        int current = 0;
        AbstractEvent ret = null;
        for (var kv : GameInstance.getEvents().entrySet()) {
            current += kv.getValue();
            if (current >= choose) {
                ret = AbstractEvent.fromString(kv.getKey(), gameInstance);
                break;
            }
        }
        if (ret != null) return ret;
        throw new PluginException("ERROR While playing", "couldn't select a random event");
    }

    private boolean compare(AbstractEvent e1, AbstractEvent e2) {
        if (e2 == null) return false;
        else if (e1.getID().equalsIgnoreCase("ArrowEvent")) {
            if (!AbstractEvent.getEvents().getBoolean("ArrowEvent.multiExecute") && hasAppearedArrow) {
                return true;
            }
            hasAppearedArrow = true;
        } else if (e1.getID().equalsIgnoreCase("NullEvent")) {
            if (!AbstractEvent.getEvents().getBoolean("NullEvent.multiExecute") && hasAppearedNull) {
                return true;
            }
            hasAppearedNull = true;
        } else if (e1.getID().equalsIgnoreCase("BlockIsNotAllowedEvent")) {
            if (!AbstractEvent.getEvents().getBoolean("BlockIsNotAllowedEvent.multiExecute") && hasAppearedBlockIsNotAllowed) {
                return true;
            }
            hasAppearedBlockIsNotAllowed = true;
        } else if (e1.getID().equalsIgnoreCase("NightEvent")) {
            if(!AbstractEvent.getEvents().getBoolean("NightEvent.multiExecute") && hasNightEvent) {
                return true;
            }
            hasNightEvent = true;
        }
        return false;
    }
}
