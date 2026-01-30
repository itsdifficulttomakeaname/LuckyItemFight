package org.luckyItemFight.exception;

import org.bukkit.ChatColor;

import static org.luckyItemFight.Main.instance;

@SuppressWarnings("deprecation")
public class PluginException extends RuntimeException{
    public PluginException(String code,String message,Exception e) {
        super(message,e);
        StackTraceElement[] stackTraceList = e.getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n");
        stringBuilder.append(ChatColor.RED).append("╔════════════════════════════════════════════════════════════════════════════════════════\n");
        stringBuilder.append(ChatColor.RED).append("║ ❌ LuckyItemFight 抛出了一个异常\n");
        stringBuilder.append(ChatColor.RED).append("╠════════════════════════════════════════════════════════════════════════════════════════\n");
        stringBuilder.append(ChatColor.RED).append("║ ").append(ChatColor.GOLD)      .append(String.format("异常代码:        %s\n", ChatColor.YELLOW + code));
        stringBuilder.append(ChatColor.RED).append("║ ").append(ChatColor.DARK_GREEN).append(String.format("异常类型:        %s\n", ChatColor.GREEN + e.getClass().getSimpleName()));
        stringBuilder.append(ChatColor.RED).append("║ ").append(ChatColor.DARK_BLUE) .append(String.format("错误信息:        %s\n", ChatColor.BLUE + getMessage()));
        if (stackTraceList != null) {
            stringBuilder.append(ChatColor.RED).append("║ ").append(ChatColor.WHITE).append("栈轨迹: \n");
            for(int i = 0;i < Math.min(stackTraceList.length, 15);++ i) {
                stringBuilder.append(ChatColor.RED).append("║ ");
                stringBuilder.append(ChatColor.YELLOW).append(stackTraceList[i]);
                stringBuilder.append("\n");
            }
            stringBuilder.append(ChatColor.RED).append("║ ").append(ChatColor.WHITE).append(String.format("共 %d 条栈轨迹(可能部分被截断)", stackTraceList.length));
        }
        stringBuilder.append(ChatColor.RED).append("╚════════════════════════════════════════════════════════════════════════════════════════\n");
        instance.getLogger().info(stringBuilder.toString());
    }

    public PluginException(String code,String message) {
        super(message);
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n");
        stringBuilder.append(ChatColor.RED).append("╔════════════════════════════════════════════════════════════════════════════════════════\n");
        stringBuilder.append(ChatColor.RED).append("║ ❌ LuckyItemFight 抛出了一个异常\n");
        stringBuilder.append(ChatColor.RED).append("╠════════════════════════════════════════════════════════════════════════════════════════\n");
        stringBuilder.append(ChatColor.RED).append("║ ").append(ChatColor.GOLD)      .append(String.format("异常代码:        %s\n", ChatColor.YELLOW + code));
        stringBuilder.append(ChatColor.RED).append("║ ").append(ChatColor.DARK_BLUE) .append(String.format("错误信息:        %s\n", ChatColor.BLUE + getMessage()));
        stringBuilder.append(ChatColor.RED).append("╚════════════════════════════════════════════════════════════════════════════════════════\n");
        instance.getLogger().info(stringBuilder.toString());
    }
}
