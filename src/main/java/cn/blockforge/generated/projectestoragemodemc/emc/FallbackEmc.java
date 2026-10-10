package cn.blockforge.generated.projectestoragemodemc.emc;

import java.util.HashMap;
import java.util.Map;

/**
 * 无 ProjectE 时的兜底 EMC 单价表（数值参考等价交换社区常用 EMC 表，仅用于独立游玩测试；
 * 安装 ProjectE 后一律以官方 API 数值为准）。未收录物品默认 1 EMC。
 */
public final class FallbackEmc {
    private FallbackEmc() {}

    private static final Map<String, Long> TABLE = new HashMap<>();

    private static void put(String id, long v) {
        TABLE.put(id, v);
    }

    static {
        put("minecraft:dirt", 1);
        put("minecraft:grass_block", 2);
        put("minecraft:cobblestone", 1);
        put("minecraft:stone", 1);
        put("minecraft:gravel", 1);
        put("minecraft:sand", 1);
        put("minecraft:oak_log", 16);
        put("minecraft:oak_planks", 4);
        put("minecraft:stick", 2);
        put("minecraft:coal", 8);
        put("minecraft:charcoal", 8);
        put("minecraft:iron_ingot", 32);
        put("minecraft:raw_iron", 32);
        put("minecraft:iron_ore", 32);
        put("minecraft:gold_ingot", 64);
        put("minecraft:raw_gold", 64);
        put("minecraft:copper_ingot", 8);
        put("minecraft:redstone", 16);
        put("minecraft:lapis_lazuli", 16);
        put("minecraft:diamond", 256);
        put("minecraft:emerald", 256);
        put("minecraft:netherite_ingot", 1664);
        put("minecraft:wheat", 4);
        put("minecraft:bread", 24);
        put("minecraft:apple", 4);
        put("minecraft:sugar", 4);
        put("minecraft:glowstone_dust", 16);
        put("minecraft:ender_pearl", 256);
        put("minecraft:blaze_rod", 64);
        put("minecraft:gunpowder", 16);
        put("minecraft:slime_ball", 16);
        put("minecraft:leather", 16);
        put("minecraft:string", 4);
        put("minecraft:feather", 4);
        put("minecraft:netherrack", 1);
        put("minecraft:soul_sand", 4);
        put("minecraft:obsidian", 128);
        put("minecraft:water_bucket", 64);
        put("minecraft:lava_bucket", 112);
        put("minecraft:glowstone", 64);
        put("minecraft:torch", 6);
    }

    public static long of(String itemId) {
        return TABLE.getOrDefault(itemId, 1L);
    }
}
