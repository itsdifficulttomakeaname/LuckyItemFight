package org.luckyItemFight.manager;

import cn.jason31416.planetlib.wrapper.SimplePlayer;
import org.bukkit.Material;
import org.luckyItemFight.Main;

import java.util.HashMap;
import java.util.Map;

public class ShopManager {
    public static final Map<SimplePlayer, Map<Material,Integer>> playerData = new HashMap<>();

    public static boolean add(SimplePlayer player,Material material,int amount,int price) {
        int coin = (int) Main.dataBaseManager.query(player.getPlayer().getUniqueId().toString(), "coin");
        if(coin >= amount * price) { // 钱足够
            playerData.putIfAbsent(player, new HashMap<>());
            playerData.get(player).put(material,playerData.get(player).get(material) == null ? 0 : playerData.get(player).get(material) + amount);
            Main.dataBaseManager.update(player.getUUID().toString(), "coin", coin - amount * price);
            return true;
        }
        return false;
    }

    public static Map<Material, Integer> get(SimplePlayer player) {
        return playerData.get(player);
    }

    public static int get(SimplePlayer player,Material material) {
        if(playerData.get(player) == null) return -1;
        if(playerData.get(player).get(material) == null) return 0;
        return playerData.get(player).get(material);
    }
}
