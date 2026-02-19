package org.luckyItemFight.manager;

import cn.jason31416.planetlib.wrapper.SimplePlayer;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
    实际传入的行号为 1 -> 15
    实际展示的顺序是 15 -> 1
*/
public class ScoreboardManager {
    @Getter private static final Map<SimplePlayer, ScoreboardManager> playerScoreboard = new ConcurrentHashMap<>(); // 计分板注册表， 玩家 -> 计分板实例
    private final Player player;
    @Getter private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, String> lines = new HashMap<>();
    private static final List<String> colorCode = new ArrayList<>(List.of("&1", "&2", "&3", "&4", "&5", "&6", "&7", "&8", "&9", "&a", "&b", "&c", "&d", "&e", "&f"));

    private final List<String> currents = new ArrayList<>();

    public ScoreboardManager(SimplePlayer player, Component title) {
        this.player = player.getPlayer();
        // 创建或获取新的计分板
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, title);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        for(int i=1;i<=15;i++) lines.put(i, ""); // 初始化

        // 设置给玩家
        player.getPlayer().setScoreboard(scoreboard);
    }

    /**
     * 更新行内容
     * @param line 行号 (0-15)
     * @param content 内容 (会自动加上颜色代码防止覆盖)
     */
    public void setLine(int line, String content) {
        int exact_line = 16 - line;
        if(exact_line < 0 || exact_line > 15) throw new IllegalArgumentException("行号只能在 [1,15] 上,而你使用了 " + line); // 避免超出范围

        content = randomProcess(content);

        lines.put(exact_line, content);
    }

    public void flushChanges() {
        currents.forEach(s -> objective.getScore(s).resetScore());
        currents.clear();
        for (Map.Entry<Integer, String> entry : lines.entrySet()) {
            int line = entry.getKey();
            String str = entry.getValue();
            objective.getScore(str).setScore(line);
            currents.add(str);
        }
    }

    /**
     * 防重
     * @param str 源字符串
     * @return 添加了无效色号防重的字符串
     * */
    private String randomProcess(String str) {
        Random random = new Random();
        StringBuilder ret = new StringBuilder(str);
        for(int i=1;i<=12;i++) {
            ret.append(colorCode.get(random.nextInt(colorCode.toArray().length)));
        }
        return ret.toString().replace("&", "§");
    }

    public void clear() {
        lines.clear();
    }
}