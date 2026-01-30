package org.luckyItemFight.game.listener;

import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.TimedHashMap;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.luckyItemFight.game.main.GameInstance;

import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class GameListener implements Listener {
    private static final TimedHashMap<SimplePlayer, SimplePlayer> killSource = new TimedHashMap<>(30000); // {this -> source}
    @Getter private static final HashMap<UUID, SimplePlayer> entityBelongings = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        GameInstance.getPlayerInstances().put(SimplePlayer.of(e.getPlayer()), null);
        e.joinMessage(null);
        for(var kv : GameInstance.getPlayerInstances().entrySet()) {
            if(kv.getValue() == null) {
                kv.getKey().sendMessage(Lang.getMessage("common.player-join").add("player", e.getPlayer().getName()));
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(e.getPlayer()));
        e.quitMessage(null);
        if (gameInstance != null) {
            gameInstance.leave(SimplePlayer.of(e.getPlayer()));
        } else {
            GameInstance.getPlayerInstances().forEach((k, v) -> {
                if(v == null) k.sendMessage(Lang.getMessage("common.player-leave").add("player", e.getPlayer().getName()));
            });
        }
    }

    @EventHandler
    public void onShoot(ProjectileLaunchEvent e) {
        Projectile projectile = e.getEntity();
        Entity shooter = (Entity) projectile.getShooter();
        if(shooter == null) return;
        if(shooter instanceof Player p) {
            entityBelongings.put(projectile.getUniqueId(), SimplePlayer.of(p));
        }else if(entityBelongings.containsKey(shooter.getUniqueId())) {
            entityBelongings.put(projectile.getUniqueId(), entityBelongings.get(shooter.getUniqueId()));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if(!(e.getEntity() instanceof Player player)) return;
        Entity damager = e.getDamager();
        if(damager instanceof Player p) {
            killSource.put(SimplePlayer.of(player), SimplePlayer.of(p));
        }else if(entityBelongings.containsKey(damager.getUniqueId())) {
            killSource.put(SimplePlayer.of(player), entityBelongings.get(damager.getUniqueId()));
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e){
        if(!(e.getEntity() instanceof Player player)) return;
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(player));
        if(gameInstance != null) {
            SimplePlayer finalPlayer = killSource.get(SimplePlayer.of(player));
            if(finalPlayer == null) {
                gameInstance.eliminate(SimplePlayer.of(player));
            }
            else {
                gameInstance.eliminate(finalPlayer, SimplePlayer.of(player));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if(e.getPlayer().getInventory().getItemInMainHand().getType().name().toUpperCase().endsWith("_SPAWN_EGG")) {
            GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(e.getPlayer()));
            if(gameInstance == null) return;
            EntityType type = ((SpawnEggMeta) e.getPlayer().getItemInHand()).getCustomSpawnedType();
            Location loc = e.getInteractionPoint();
            if(loc == null || type == null) return;
            Entity entity = gameInstance.getGameWorld().spawnEntity(loc.clone().add(0,1,0), type);
            entityBelongings.put(entity.getUniqueId(), SimplePlayer.of(e.getPlayer()));
            e.getPlayer().getInventory().getItemInMainHand().setAmount(e.getPlayer().getInventory().getItemInMainHand().getAmount()-1);
            e.setCancelled(true);
        }
    }
}
