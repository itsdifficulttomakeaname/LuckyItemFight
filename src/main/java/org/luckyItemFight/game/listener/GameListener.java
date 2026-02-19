package org.luckyItemFight.game.listener;

import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.TimedHashMap;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.*;
import org.luckyItemFight.Main;
import org.luckyItemFight.game.event.BlockIsNotAllowedEvent;
import org.luckyItemFight.game.main.GameInstance;
import org.luckyItemFight.game.main.GameState;
import org.luckyItemFight.manager.ScoreboardManager;
import org.luckyItemFight.util.SpawnEggTypeGetter;

import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class GameListener implements Listener {
    private static final TimedHashMap<SimplePlayer, SimplePlayer> killSource = new TimedHashMap<>(15000); // {this -> source}
    @Getter private static final HashMap<UUID, SimplePlayer> entityBelongings = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.joinMessage(null);
        e.getPlayer().getInventory().clear();
        e.getPlayer().setGameMode(GameMode.ADVENTURE);
        e.getPlayer().setExp(0);
        e.getPlayer().setLevel(0);

        UUID uuid = e.getPlayer().getUniqueId();

        if(Main.dataBaseManager.query(String.valueOf(uuid), "play") == null) Main.dataBaseManager.insert(String.valueOf(uuid));

        ScoreboardManager.getPlayerScoreboard().put(
                SimplePlayer.of(e.getPlayer()),
                new ScoreboardManager(
                        SimplePlayer.of(e.getPlayer()),
                        Lang.getMessage("scoreboard.title").toComponent()
                )
        );
        e.getPlayer().teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(e.getPlayer()));
        ScoreboardManager.getPlayerScoreboard().remove(SimplePlayer.of(e.getPlayer()));
        e.quitMessage(null);
        if (gameInstance != null) {
            gameInstance.leave(SimplePlayer.of(e.getPlayer()));
            gameInstance.getPlayers().remove(SimplePlayer.of(e.getPlayer()));
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
        if(GameInstance.getPlayerInstances().get(SimplePlayer.of(player)) == null) {
            e.setDamage(0);
            return;
        }
        if(damager instanceof Player p) {
            killSource.put(SimplePlayer.of(player), SimplePlayer.of(p));
        }else if(entityBelongings.containsKey(damager.getUniqueId())) {
            killSource.put(SimplePlayer.of(player), entityBelongings.get(damager.getUniqueId()));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if(e.getCause() == EntityDamageEvent.DamageCause.VOID) e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        e.deathMessage(null);
        Player player = e.getEntity();
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(player));
        if (gameInstance != null) {
            if(!killSource.containsKey(SimplePlayer.of(player))) {
                gameInstance.eliminate(SimplePlayer.of(player));
                return;
            }
            SimplePlayer finalPlayer = killSource.get(SimplePlayer.of(player));
            if (finalPlayer == null) {
                gameInstance.eliminate(SimplePlayer.of(player));
            } else {
                if(finalPlayer.equals(SimplePlayer.of(player))) gameInstance.eliminate(finalPlayer);
                else gameInstance.eliminate(finalPlayer, SimplePlayer.of(player));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if(e.getPlayer().getInventory().getItemInMainHand().getType().name().toUpperCase().endsWith("_SPAWN_EGG")) {
            GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(e.getPlayer()));
            if(gameInstance == null) return;
            EntityType type = SpawnEggTypeGetter.getEntityType(e.getPlayer().getItemInHand());
            Location loc = e.getInteractionPoint();
            if(loc == null) return;
            if(type == null) return;
            Entity entity;
            entity = gameInstance.getGameWorld().spawnEntity(loc.clone(), type);
            entityBelongings.put(entity.getUniqueId(), SimplePlayer.of(e.getPlayer()));
            e.getPlayer().getInventory().getItemInMainHand().setAmount(e.getPlayer().getInventory().getItemInMainHand().getAmount()-1);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        SimplePlayer simplePlayer = SimplePlayer.of(e.getPlayer());
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(simplePlayer);
        if(gameInstance == null) return;
        if(gameInstance.getState() == GameState.WAITING) {
            Location from = e.getFrom();
            Location to = e.getTo();

            if(!compare(from, to)) e.setCancelled(true);
        }
    }

    private boolean compare(Location l1, Location l2) {
        return l1.getX() == l2.getX() && l1.getZ() == l2.getZ();
    }

    @EventHandler
    public void onPlayerInventory(InventoryInteractEvent event){
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(event.getWhoClicked()));
        if(gameInstance == null ||  gameInstance.getState() == GameState.WAITING) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlayerInventory(InventoryClickEvent event){
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(event.getWhoClicked()));
        if(gameInstance == null ||   gameInstance.getState() == GameState.WAITING) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlayerInventory(PlayerSwapHandItemsEvent event){
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(event.getPlayer()));
        if(gameInstance == null ||   gameInstance.getState() == GameState.WAITING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event){
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(event.getPlayer()));
        if(gameInstance == null ||   gameInstance.getState() == GameState.WAITING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHungerLevelChange(FoodLevelChangeEvent e) {
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(e.getEntity()));
        if(gameInstance == null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(e.getPlayer()));
        if(gameInstance == null) {
            e.setCancelled(true);
        } else if(GameInstance.getGameTaskInstances().get(gameInstance).getEvent() instanceof BlockIsNotAllowedEvent) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        GameInstance gameInstance = GameInstance.getPlayerInstances().get(SimplePlayer.of(e.getPlayer()));
        if(gameInstance == null) {
            e.setCancelled(true);
        } else if(GameInstance.getGameTaskInstances().get(gameInstance).getEvent() instanceof BlockIsNotAllowedEvent) {
            e.setCancelled(true);
        }
    }
}
