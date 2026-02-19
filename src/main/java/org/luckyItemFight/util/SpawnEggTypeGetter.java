package org.luckyItemFight.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SpawnEggTypeGetter {
    private static final Map<Material, EntityType> EGG_TO_ENTITY = new HashMap<>();

    static {
        // 1.20+ 标准生成蛋
        EGG_TO_ENTITY.put(Material.AXOLOTL_SPAWN_EGG, EntityType.AXOLOTL);
        EGG_TO_ENTITY.put(Material.BAT_SPAWN_EGG, EntityType.BAT);
        EGG_TO_ENTITY.put(Material.BEE_SPAWN_EGG, EntityType.BEE);
        EGG_TO_ENTITY.put(Material.BLAZE_SPAWN_EGG, EntityType.BLAZE);
        EGG_TO_ENTITY.put(Material.CAT_SPAWN_EGG, EntityType.CAT);
        EGG_TO_ENTITY.put(Material.CAVE_SPIDER_SPAWN_EGG, EntityType.CAVE_SPIDER);
        EGG_TO_ENTITY.put(Material.CHICKEN_SPAWN_EGG, EntityType.CHICKEN);
        EGG_TO_ENTITY.put(Material.COD_SPAWN_EGG, EntityType.COD);
        EGG_TO_ENTITY.put(Material.COW_SPAWN_EGG, EntityType.COW);
        EGG_TO_ENTITY.put(Material.CREEPER_SPAWN_EGG, EntityType.CREEPER);
        EGG_TO_ENTITY.put(Material.DOLPHIN_SPAWN_EGG, EntityType.DOLPHIN);
        EGG_TO_ENTITY.put(Material.DONKEY_SPAWN_EGG, EntityType.DONKEY);
        EGG_TO_ENTITY.put(Material.DROWNED_SPAWN_EGG, EntityType.DROWNED);
        EGG_TO_ENTITY.put(Material.ELDER_GUARDIAN_SPAWN_EGG, EntityType.ELDER_GUARDIAN);
        EGG_TO_ENTITY.put(Material.ENDERMAN_SPAWN_EGG, EntityType.ENDERMAN);
        EGG_TO_ENTITY.put(Material.ENDERMITE_SPAWN_EGG, EntityType.ENDERMITE);
        EGG_TO_ENTITY.put(Material.EVOKER_SPAWN_EGG, EntityType.EVOKER);
        EGG_TO_ENTITY.put(Material.FOX_SPAWN_EGG, EntityType.FOX);
        EGG_TO_ENTITY.put(Material.FROG_SPAWN_EGG, EntityType.FROG);
        EGG_TO_ENTITY.put(Material.GHAST_SPAWN_EGG, EntityType.GHAST);
        EGG_TO_ENTITY.put(Material.GLOW_SQUID_SPAWN_EGG, EntityType.GLOW_SQUID);
        EGG_TO_ENTITY.put(Material.GOAT_SPAWN_EGG, EntityType.GOAT);
        EGG_TO_ENTITY.put(Material.GUARDIAN_SPAWN_EGG, EntityType.GUARDIAN);
        EGG_TO_ENTITY.put(Material.HOGLIN_SPAWN_EGG, EntityType.HOGLIN);
        EGG_TO_ENTITY.put(Material.HORSE_SPAWN_EGG, EntityType.HORSE);
        EGG_TO_ENTITY.put(Material.HUSK_SPAWN_EGG, EntityType.HUSK);
        EGG_TO_ENTITY.put(Material.IRON_GOLEM_SPAWN_EGG, EntityType.IRON_GOLEM);
        EGG_TO_ENTITY.put(Material.LLAMA_SPAWN_EGG, EntityType.LLAMA);
        EGG_TO_ENTITY.put(Material.MAGMA_CUBE_SPAWN_EGG, EntityType.MAGMA_CUBE);
        EGG_TO_ENTITY.put(Material.MOOSHROOM_SPAWN_EGG, EntityType.MUSHROOM_COW);
        EGG_TO_ENTITY.put(Material.MULE_SPAWN_EGG, EntityType.MULE);
        EGG_TO_ENTITY.put(Material.OCELOT_SPAWN_EGG, EntityType.OCELOT);
        EGG_TO_ENTITY.put(Material.PANDA_SPAWN_EGG, EntityType.PANDA);
        EGG_TO_ENTITY.put(Material.PARROT_SPAWN_EGG, EntityType.PARROT);
        EGG_TO_ENTITY.put(Material.PHANTOM_SPAWN_EGG, EntityType.PHANTOM);
        EGG_TO_ENTITY.put(Material.PIG_SPAWN_EGG, EntityType.PIG);
        EGG_TO_ENTITY.put(Material.PIGLIN_SPAWN_EGG, EntityType.PIGLIN);
        EGG_TO_ENTITY.put(Material.PIGLIN_BRUTE_SPAWN_EGG, EntityType.PIGLIN_BRUTE);
        EGG_TO_ENTITY.put(Material.PILLAGER_SPAWN_EGG, EntityType.PILLAGER);
        EGG_TO_ENTITY.put(Material.POLAR_BEAR_SPAWN_EGG, EntityType.POLAR_BEAR);
        EGG_TO_ENTITY.put(Material.PUFFERFISH_SPAWN_EGG, EntityType.PUFFERFISH);
        EGG_TO_ENTITY.put(Material.RABBIT_SPAWN_EGG, EntityType.RABBIT);
        EGG_TO_ENTITY.put(Material.RAVAGER_SPAWN_EGG, EntityType.RAVAGER);
        EGG_TO_ENTITY.put(Material.SALMON_SPAWN_EGG, EntityType.SALMON);
        EGG_TO_ENTITY.put(Material.SHEEP_SPAWN_EGG, EntityType.SHEEP);
        EGG_TO_ENTITY.put(Material.SHULKER_SPAWN_EGG, EntityType.SHULKER);
        EGG_TO_ENTITY.put(Material.SILVERFISH_SPAWN_EGG, EntityType.SILVERFISH);
        EGG_TO_ENTITY.put(Material.SKELETON_SPAWN_EGG, EntityType.SKELETON);
        EGG_TO_ENTITY.put(Material.SKELETON_HORSE_SPAWN_EGG, EntityType.SKELETON_HORSE);
        EGG_TO_ENTITY.put(Material.SLIME_SPAWN_EGG, EntityType.SLIME);
        EGG_TO_ENTITY.put(Material.SNOW_GOLEM_SPAWN_EGG, EntityType.SNOWMAN);
        EGG_TO_ENTITY.put(Material.SPIDER_SPAWN_EGG, EntityType.SPIDER);
        EGG_TO_ENTITY.put(Material.SQUID_SPAWN_EGG, EntityType.SQUID);
        EGG_TO_ENTITY.put(Material.STRAY_SPAWN_EGG, EntityType.STRAY);
        EGG_TO_ENTITY.put(Material.STRIDER_SPAWN_EGG, EntityType.STRIDER);
        EGG_TO_ENTITY.put(Material.TADPOLE_SPAWN_EGG, EntityType.TADPOLE);
        EGG_TO_ENTITY.put(Material.TRADER_LLAMA_SPAWN_EGG, EntityType.TRADER_LLAMA);
        EGG_TO_ENTITY.put(Material.TROPICAL_FISH_SPAWN_EGG, EntityType.TROPICAL_FISH);
        EGG_TO_ENTITY.put(Material.TURTLE_SPAWN_EGG, EntityType.TURTLE);
        EGG_TO_ENTITY.put(Material.VEX_SPAWN_EGG, EntityType.VEX);
        EGG_TO_ENTITY.put(Material.VILLAGER_SPAWN_EGG, EntityType.VILLAGER);
        EGG_TO_ENTITY.put(Material.VINDICATOR_SPAWN_EGG, EntityType.VINDICATOR);
        EGG_TO_ENTITY.put(Material.WANDERING_TRADER_SPAWN_EGG, EntityType.WANDERING_TRADER);
        EGG_TO_ENTITY.put(Material.WARDEN_SPAWN_EGG, EntityType.WARDEN);
        EGG_TO_ENTITY.put(Material.WITCH_SPAWN_EGG, EntityType.WITCH);
        EGG_TO_ENTITY.put(Material.WITHER_SKELETON_SPAWN_EGG, EntityType.WITHER_SKELETON);
        EGG_TO_ENTITY.put(Material.WOLF_SPAWN_EGG, EntityType.WOLF);
        EGG_TO_ENTITY.put(Material.ZOGLIN_SPAWN_EGG, EntityType.ZOGLIN);
        EGG_TO_ENTITY.put(Material.ZOMBIE_SPAWN_EGG, EntityType.ZOMBIE);
        EGG_TO_ENTITY.put(Material.ZOMBIE_HORSE_SPAWN_EGG, EntityType.ZOMBIE_HORSE);
        EGG_TO_ENTITY.put(Material.ZOMBIE_VILLAGER_SPAWN_EGG, EntityType.ZOMBIE_VILLAGER);
        EGG_TO_ENTITY.put(Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, EntityType.ZOMBIFIED_PIGLIN);
    }

    public static EntityType getEntityType(ItemStack itemStack) {
        return EGG_TO_ENTITY.get(itemStack.getType());
    }
}