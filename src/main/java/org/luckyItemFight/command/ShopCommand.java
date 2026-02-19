package org.luckyItemFight.command;

import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.MapTree;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.luckyItemFight.exception.PluginException;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.manager.ShopManager;
import org.luckyItemFight.util.GUI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.luckyItemFight.Main.instance;

public class ShopCommand implements CommandExecutor {
    private static MapTree shopConfig = null;
    @Getter private static GUI gui = null;
    public ShopCommand() {
        try {
            shopConfig = MapTree.fromYaml(Files.readString(new File(instance.getDataFolder(), "shop.yml").toPath()));
            gui = new GUI(6, Message.of((String) shopConfig.get("title")).toComponent());
            shopConfig.getKeys().forEach(k -> {
                if(k.equalsIgnoreCase("title")) return;
                shopConfig.getStringList(k + ".loc").forEach(l -> {
                    String[] loc = l.split(",");
                    if(loc.length < 2) return;
                    int x,y;
                    x = Integer.parseInt(loc[0]);
                    y = Integer.parseInt(loc[1]);
                    gui.set(x, y, GUI.createItem(
                                    new ItemStack(Material.valueOf(shopConfig.getString(k + ".material"))),
                                    shopConfig.getString(k + ".name"),
                                    shopConfig.getStringList(k + ".lore")
                            )
                    );
                    if(shopConfig.getInt(k + ".price") == -1) return;
                    gui.leftClick(x,y,(p,e) -> {
                        e.setCancelled(true);
                        if(ShopManager.get(SimplePlayer.of(p), Material.valueOf(shopConfig.getString(k + ".material"))) > 0) {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.cannot-buy-too-many"));
                        } else if (!ShopManager.add(SimplePlayer.of(p),Material.valueOf(shopConfig.getString(k + ".material")),1, shopConfig.getInt(k + ".price"))) {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.coin-not-enough"));
                        } else {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.buy-success").add("amount",1).add("material", Material.valueOf(shopConfig.getString(k + ".material")).name()));
                        }
                    }).rightClick(x,y,(p,e) -> {
                        e.setCancelled(true);
                        if(ShopManager.get(SimplePlayer.of(p), Material.valueOf(shopConfig.getString(k + ".material"))) > 0) {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.cannot-buy-too-many"));
                        } else if(!ShopManager.add(SimplePlayer.of(p),Material.valueOf(shopConfig.getString(k + ".material")),1, shopConfig.getInt(k + ".price"))) {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.coin-not-enough"));
                        } else {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.buy-success").add("amount",1).add("material", Material.valueOf(shopConfig.getString(k + ".material")).name()));
                        }
                    }).shiftLeftClick(x,y,(p,e) -> {
                        e.setCancelled(true);
                        if(ShopManager.get(SimplePlayer.of(p), Material.valueOf(shopConfig.getString(k + ".material"))) > 0) {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.cannot-buy-too-many"));
                        } else if(!ShopManager.add(SimplePlayer.of(p),Material.valueOf(shopConfig.getString(k + ".material")),10, shopConfig.getInt(k + ".price"))) {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.coin-not-enough"));
                        } else {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.buy-success").add("amount",10).add("material", Material.valueOf(shopConfig.getString(k + ".material")).name()));
                        }
                    }).shiftRightClick(x,y,(p,e) -> {
                        e.setCancelled(true);
                        if(ShopManager.get(SimplePlayer.of(p), Material.valueOf(shopConfig.getString(k + ".material"))) > 0) {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.cannot-buy-too-many"));
                        } else if(!ShopManager.add(SimplePlayer.of(p),Material.valueOf(shopConfig.getString(k + ".material")),10, shopConfig.getInt(k + ".price"))) {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.coin-not-enough"));
                        } else {
                            SimplePlayer.of(p).sendMessage(Lang.getMessage("command.buy-success").add("amount",10).add("material", Material.valueOf(shopConfig.getString(k + ".material")).name()));
                        }
                    });
                });
            });
        } catch (IOException e) {
            throw new PluginException("ERROR While Loading","Cannot initialize shop.yml",e);
        }
    }
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if(!(commandSender instanceof Player)) {
            commandSender.sendMessage("该指令只能由玩家执行!");
            return true;
        }
        if(GameInstance.getPlayerInstances().get(SimplePlayer.of(commandSender)) != null) {
            SimplePlayer.of(commandSender).sendMessage(Lang.getMessage("command.cannot-open-shop-menu-in-game"));
            return true;
        }
        gui.show((Player) commandSender);
        return true;
    }
}
