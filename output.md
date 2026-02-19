# 问题分析报告

## 问题1：Null事件从未触发过

### 解释
Null事件在配置文件`__event__.yml`中设置了`possibility: 100`，权重很高，理论上应该有很高的触发概率。但实际运行中玩家感觉从未触发过。经过代码分析，发现问题可能出在以下几个方面：

1. **事件触发无感知**：Null事件的配置中`sound: null`，`show-name.enable: false`，`show-detail.enable: false`，这意味着即使事件被触发，也不会有任何声音、标题或聊天提示，玩家完全无法感知到事件的发生。

2. **事件选择逻辑潜在问题**：
   - `GameInstance.randomEvent()`方法使用`new Random(System.currentTimeMillis())`创建随机数生成器，如果在同一tick内多次调用（do-while循环），可能产生相同的随机序列，影响事件分布。
   - `events` Map的迭代顺序不确定，可能导致权重计算与预期不符。
   - `compare()`方法中的`hasAppearedNull`标志在每次Null事件被考虑时都会设置为`true`，但这对触发没有影响，因为`multiExecute: true`。

3. **事件执行无效果**：Null事件的`execute()`方法是空的，不执行任何操作，因此即使触发也没有游戏内效果。

### 建议修改
```java
// 修改 GameInstance.java 中 randomEvent() 方法，改进随机数生成
@NotNull
private AbstractEvent randomEvent() {
    int total = 0;
    for(int i : events.values()) total += i;
    int choose = ThreadLocalRandom.current().nextInt(total);  // 使用 ThreadLocalRandom 替代 new Random()
    int current = 0;
    AbstractEvent ret = null;
    for(var kv : events.entrySet()) {
        current += kv.getValue();
        if(current > choose) {
            ret = AbstractEvent.fromString(kv.getKey(), GameInstance.this);
            break;
        }
    }
    if(ret != null) return ret;
    throw new PluginException("ERROR While playing", "couldn't select a random event");
}

// 可选：为Null事件添加最小化的提示，让玩家知道事件已触发
// 修改 NullEvent.java 的 execute() 方法
@Override
void execute() {
    // 添加简单的提示，例如在动作栏显示"无事发生"
    gameInstance.getPlayers().forEach((p, s) -> {
        if (s.equals(PlayerState.ALIVE)) {
            p.getPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText("§7无事发生..."));
        }
    });
}
```

## 问题2：Arrow事件没有生成箭矢，且不显示show-name和show-detail

### 解释
1. **箭矢未生成的原因**：
   - `Arrow.java`中的`execute()`方法使用`PlanetLib.getScheduler().runTimer()`来周期性生成箭矢，但`PlanetLib`调度器可能与Bukkit调度器存在兼容性问题，导致任务未正确执行。
   - 箭矢生成代码`gameInstance.getGameWorld().spawnEntity()`创建的箭矢没有初始速度，可能立即掉落或消失。
   - 生成位置计算：`y = p.getPlayer().getY() + config.getDouble("height")`，这会在玩家当前Y坐标上加10格，而不是从固定高度落下。

2. **show-name和show-detail未显示的原因**：
   - 在`GameInstance.java`第149行`if(tick == 0)`条件中，只播放了事件音效，**没有调用显示show-name和show-detail的逻辑**。
   - `Wither`事件在其自身的`execute()`方法中处理了文本显示，但`Arrow`和`Null`事件没有实现此逻辑。
   - 事件系统的设计不一致：文本显示应该在事件基类或GameInstance中统一处理，而不是由各个事件自行处理。

3. **音效播放正常但文本缺失**：代码中只检查了`sound`配置并播放音效，但完全没有处理`show-name`和`show-detail`配置。

### 建议修改
```java
// 修改 GameInstance.java，在触发事件后统一处理show-name和show-detail显示
if (tick == 0) {
    if(event == null || event.isStopping()) {
        AbstractEvent newEvent;
        do {
            newEvent = randomEvent();
        } while(compare(newEvent, event));
        event = newEvent;
        
        // 播放音效
        if(!AbstractEvent.getEvents().getString(event.getID() + ".sound").equalsIgnoreCase("null")) {
            players.keySet().forEach(p ->
                p.getPlayer().playSound(
                    p.getPlayer(),
                    Sound.valueOf(AbstractEvent.getEvents().getString(event.getID() + ".sound")),
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
                )
            );
        }
        
        // 新增：统一显示事件文本
        showEventText(event);
        
        String id = event.getID();
        boolean enabled = AbstractEvent.getEvents().getBoolean(id + ".end-in-duration.enable");
        int duration = AbstractEvent.getEvents().getInt(id + ".end-in-duration.duration");
        if(enabled) event.execute(0);
        else event.execute(duration);
    }
    // ... 其他代码
}

// 新增方法：显示事件文本
private void showEventText(AbstractEvent event) {
    String id = event.getID();
    MapTree config = AbstractEvent.getEvents().getSection(id);
    
    // 显示show-name
    if (config.getBoolean("show-name.enable")) {
        String text = config.getString("show-name.text");
        String type = config.getString("show-name.type");
        sendToAllPlayers(type, text, true);
    }
    
    // 显示show-detail
    if (config.getBoolean("show-detail.enable")) {
        String text = config.getString("show-detail.text");
        String type = config.getString("show-detail.type");
        sendToAllPlayers(type, text, true);
    }
}

private void sendToAllPlayers(String type, String text, boolean run) {
    if (!run) return;
    for (SimplePlayer p : players.keySet()) {
        if (type.equalsIgnoreCase(AbstractEvent.TITLE))
            p.getPlayer().showTitle(Title.title(Message.of(text).toComponent(), Message.of("").toComponent()));
        else if (type.equalsIgnoreCase(AbstractEvent.SUBTITLE))
            p.getPlayer().showTitle(Title.title(Message.of("").toComponent(), Message.of(text).toComponent()));
        else if (type.equalsIgnoreCase(AbstractEvent.CHAT)) 
            p.sendMessage(Message.of(text));
    }
}
```

```java
// 修改 ArrowEvent.java，修复箭矢生成问题
@Override
void execute() {
    double range = config.getDouble("range");
    double height = config.getDouble("height");
    gameTask = Bukkit.getScheduler().runTaskTimer(instance, () -> gameInstance.getPlayers().forEach((p, s) -> {
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
}
```

## 总结
主要问题根源：
1. **事件系统设计不一致**：文本显示逻辑分散在各事件类中，GameInstance没有统一处理。
2. **调度器兼容性问题**：`PlanetLib.getScheduler()`可能无法正确执行周期性任务。
3. **Null事件缺乏可感知性**：没有声音、没有文本提示、没有游戏效果，导致玩家无法感知其触发。
4. **箭矢物理问题**：生成的箭矢没有初速度，可能无法正常下落。

建议按照上述修改方案进行修复，以解决所有问题。