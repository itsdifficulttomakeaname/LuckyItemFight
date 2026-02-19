package org.luckyItemFight.util;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.lib.folialib.wrapper.task.WrappedTask;
import cn.jason31416.planetlib.message.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.luckyItemFight.Main.instance;

/**
 * 简单的GUI
 * 不支持复用，多用
 * 只支持监听左右键
 * (SHIFT_LEFT == LEFT && SHIFT_RIGHT == RIGHT)
 * */
public class GUI implements InventoryHolder, Listener {
    private Inventory inventory;
    private final int rows;
    private final Map<Integer, ClickHandler> leftClickHandlers = new HashMap<>();
    private final Map<Integer, ClickHandler> rightClickHandlers = new HashMap<>();
    private final Map<Integer, ClickHandler> shiftLeftClickHandlers = new HashMap<>();
    private final Map<Integer, ClickHandler> shiftRightClickHandlers = new HashMap<>();
    private WrappedTask refreshTask;

    @FunctionalInterface
    public interface ClickHandler {
        void handle(Player player, InventoryClickEvent event);
    }

    public GUI(int rows, Component title) {
        this.rows = rows;
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        Bukkit.getPluginManager().registerEvents(this, instance);
    }

    public GUI set(int x, int y, ItemStack itemStack) {
        if (x < 1 || x > rows || y < 1 || y > 9) {
            throw new IllegalArgumentException("IndexOutOfBounds: x(1-" + rows +"), y(1-6)");
        }
        int slot = y + (x-1) * 9 - 1;
        inventory.setItem(slot, itemStack);
        return this;
    }

    public GUI title(Component title) {
        Inventory newInventory = Bukkit.createInventory(this, rows * 9, title);
        newInventory.setContents(inventory.getContents());
        this.inventory = newInventory;
        return this;
    }

    public GUI leftClick(int x, int y, ClickHandler handler) {
        if (x < 1 || x > rows || y < 1 || y > 9) {
            throw new IllegalArgumentException("IndexOutOfBounds: x(1-" + rows +"), y(1-6)");
        }
        int slot = y + (x-1) * 9 - 1;
        return leftClick(slot, handler);
    }

    public GUI rightClick(int x, int y, ClickHandler handler) {
        if (x < 1 || x > rows || y < 1 || y > 9) {
            throw new IllegalArgumentException("IndexOutOfBounds: x(1-" + rows +"), y(1-6)");
        }
        int slot = y + (x-1) * 9 - 1;
        return rightClick(slot, handler);
    }

    public GUI shiftLeftClick(int x, int y, ClickHandler handler) {
        if (x < 1 || x > rows || y < 1 || y > 9) {
            throw new IllegalArgumentException("IndexOutOfBounds: x(1-" + rows +"), y(1-6)");
        }
        int slot = y + (x-1) * 9 - 1;
        return shiftLeftClick(slot, handler);
    }

    public GUI shiftRightClick(int x, int y, ClickHandler handler) {
        if (x < 1 || x > rows || y < 1 || y > 9) {
            throw new IllegalArgumentException("IndexOutOfBounds: x(1-" + rows +"), y(1-6)");
        }
        int slot = y + (x-1) * 9 - 1;
        return shiftRightClick(slot, handler);
    }

    public ItemStack getItem(int x,int y) {
        if (x < 1 || x > rows || y < 1 || y > 9) {
            throw new IllegalArgumentException("IndexOutOfBounds: x(1-" + rows +"), y(1-6)");
        }
        int slot = y + (x-1) * 9 - 1;
        return inventory.getItem(slot);
    }

    GUI leftClick(int slot, ClickHandler handler) {
        leftClickHandlers.put(slot, handler);
        return this;
    }

    GUI rightClick(int slot, ClickHandler handler) {
        rightClickHandlers.put(slot, handler);
        return this;
    }

    GUI shiftLeftClick(int slot, ClickHandler handler) {
        shiftLeftClickHandlers.put(slot, handler);
        return this;
    }

    GUI shiftRightClick(int slot, ClickHandler handler) {
        shiftRightClickHandlers.put(slot, handler);
        return this;
    }

    public void close() {
        this.inventory.close();
    }

    public void show(Player p) {
        p.openInventory(inventory);
    }

    public void refresh(Player p) {
        p.updateInventory();
    }

    public GUI timedRefresh(int millis) {
        cancelRefreshTask(); // 取消现有任务
        refreshTask = PlanetLib.getScheduler().runTimer(
                () -> inventory.getViewers().forEach(p -> {
                    if (p instanceof Player pl) refresh(pl);
                }),
                millis,
                millis,
                TimeUnit.MILLISECONDS
        );
        return this;
    }

    public void cancelRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        if(!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot < 0 || slot >= inventory.getSize()) return;

        ClickHandler handler = null;
        ClickType clickType = event.getClick();

        if (clickType == ClickType.LEFT) {
            handler = leftClickHandlers.get(slot);
        } else if (clickType == ClickType.SHIFT_LEFT) {
            handler = shiftLeftClickHandlers.get(slot);
        } else if (clickType == ClickType.RIGHT) {
            handler = rightClickHandlers.get(slot);
        } else if (clickType == ClickType.SHIFT_RIGHT) {
            handler = shiftRightClickHandlers.get(slot);
        }

        if (handler != null) {
            handler.handle(player, event);
        }
    }

    public void destroy() {
        cancelRefreshTask();
        HandlerList.unregisterAll(this);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public static ItemStack createItem(ItemStack item, String sName, List<String> sLore) {
        Component name = Message.of(sName).toComponent();
//        Component[] lore = sLore.stream().map(Component::text).toArray(Component[]::new);
        List<Component> list = new ArrayList<>();
        sLore.forEach(s -> list.add(Message.of(s).toComponent()));

        item.editMeta(meta -> {
            meta.displayName(name);
            meta.lore(list);
        });
        return item;
    }
}