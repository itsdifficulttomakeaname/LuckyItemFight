package org.luckyItemFight.exception;

public class PluginException extends RuntimeException{
    public PluginException(String code,String message,Exception e) {
        super(format(code,message,e));
    }

    private static String format(String code,String message,Exception e) {
        StackTraceElement[] stackTraceList = e.getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n");
        stringBuilder.append("§c").append("╔════════════════════════════════════════════════════════════════════════════════════════\n");
        stringBuilder.append("§c").append("║ [X] LuckyItemFight 抛出了一个异常\n");
        stringBuilder.append("§c").append("╠════════════════════════════════════════════════════════════════════════════════════════\n");
        stringBuilder.append("§c").append("║ ").append("§6").append(String.format("异常代码:        %s\n", "§e" + code));
        stringBuilder.append("§c").append("║ ").append("§2").append(String.format("异常类型:        %s\n", "§a" + e.getClass().getSimpleName()));
        stringBuilder.append("§c").append("║ ").append("§1").append(String.format("错误信息:        %s\n", "§9" + message));
        if (stackTraceList != null) {
            stringBuilder.append("§c").append("║ ").append("§f").append("(Caused By)栈轨迹: \n");
            for(int i = 0;i < Math.min(stackTraceList.length, 15);++ i) {
                stringBuilder.append("§c").append("║ ");
                stringBuilder.append("§e").append(stackTraceList[i]);
                stringBuilder.append("\n");
            }
            stringBuilder.append("§c").append("║ ").append("§f").append(String.format("共 %d 条栈轨迹(可能部分被截断)\n", stackTraceList.length));
        }
        stringBuilder.append("§c").append("╚════════════════════════════════════════════════════════════════════════════════════════\n");
        return stringBuilder.toString();
    }

    public PluginException(String code,String message) {
        super(format(code, message));
    }

    private static String format(String code, String message) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n");
        stringBuilder.append("§c").append("╔════════════════════════════════════════════════════════════════════════════════════════\n");
        stringBuilder.append("§c").append("║ [X] LuckyItemFight 抛出了一个异常\n");
        stringBuilder.append("§c").append("╠════════════════════════════════════════════════════════════════════════════════════════\n");
        stringBuilder.append("§c").append("║ ").append("§6").append(String.format("异常代码:        %s\n", "§e" + code));
        stringBuilder.append("§c").append("║ ").append("§1").append(String.format("错误信息:        %s\n", "§9" + message));
        stringBuilder.append("§c").append("╚════════════════════════════════════════════════════════════════════════════════════════\n");

        return stringBuilder.toString();
    }
}