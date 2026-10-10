package cn.blockforge.generated.projectestoragemodemc;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** 通用配置：刷新频率、批量限额、反馈冷却、桥接开关、提取模式、组件过滤、动态槽位上限。 */
public final class EmcLinkConfig {
    private EmcLinkConfig() {}

    /** nbtFilterMode 允许的取值。 */
    public static final List<String> NBT_FILTER_MODES = List.of("ANY_NBT", "ENCHANT_DURABILITY", "OFF");

    /** extractionMode 允许的取值。 */
    public static final List<String> EXTRACTION_MODES = List.of("ENABLED", "DISABLED");

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_PE_BRIDGE;
    public static final ModConfigSpec.IntValue OFFER_REFRESH_TICKS;
    public static final ModConfigSpec.IntValue MAX_EXTRACT_PER_CALL;
    public static final ModConfigSpec.IntValue SOUND_COOLDOWN_TICKS;
    public static final ModConfigSpec.ConfigValue<String> EXTRACTION_MODE;
    public static final ModConfigSpec.IntValue MAX_VISIBLE_SLOTS;
    public static final ModConfigSpec.ConfigValue<String> NBT_FILTER_MODE;

    /** 提取是否可用（ENABLED＝终端一键合成/提取可用；DISABLED＝严格只进不出）。 */
    public static boolean extractionEnabled() {
        String v = EXTRACTION_MODE.get();
        // 宽容解析：只有显式写了 DISABLED 才关闭；null/陌生值一律按默认值 ENABLED 处理，
        // 避免旧配置文件里的残留值或大小写差异让终端一键合成静默失效。
        return v == null || !EXTRACTION_MODES.get(1).equalsIgnoreCase(v.trim());
    }

    /**
     * 加固说明：两个字符串键刻意不用 ModConfigSpec.Builder#defineInList——它生成的校验器
     * 直接对不可变列表 List.of(...) 调用 contains(value)，旧配置文件缺该键时以 null 调用，
     * contains(null) 抛 NullPointerException，游戏在开档 Config loading 阶段崩溃
     * （用户 r7/r8 日志实锤）。改为无校验器 define，合法性在读取端宽容匹配，陌生值回退默认。
     */

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        ENABLE_PE_BRIDGE = b
                .comment("在安装 ProjectE 时，尝试通过 ProjectE 官方 API 桥接玩家 EMC 账户与已学习清单。",
                         "关闭或检测失败时使用方块内置的本地 EMC 账簿。")
                .define("enableProjectEBridge", true);
        OFFER_REFRESH_TICKS = b
                .comment("链接方块清单（可购买条目与余额显示）的重建间隔，单位游戏刻。")
                .defineInRange("offerRefreshTicks", 10, 1, 100);
        MAX_EXTRACT_PER_CALL = b
                .comment("批量限额：单次 extractItem 最多提取的物品数；0＝不限（默认，终端一键合成按配方需求整批提取最顺）。",
                        "设为正数时每批封顶该数量，余额不足时该批返回空，用于给提取加保守上限。")
                .defineInRange("maxExtractPerCall", 0, 0, Integer.MAX_VALUE);
        SOUND_COOLDOWN_TICKS = b
                .comment("同一个链接方块两次反馈音效之间的最小间隔，单位游戏刻。粒子另有 2 刻节流。")
                .defineInRange("soundCooldownTicks", 4, 0, 100);
        EXTRACTION_MODE = b
                .comment("输出模式（参考 emc-interface / ProjectExpansion 的 EMC 接口做法）：",
                        "ENABLED（默认）＝提取可用：终端清单里显示的可购买物品可被一键合成/提取消耗，每次提取实时扣减 EMC，投料(漏洞输入)照旧；",
                        "DISABLED＝严格只进不出：任何提取一律返回空，虚拟槽纯显示，代价是终端一键合成会因取不到材料而失败。",
                        "注：本键取代旧版 allowExtraction，旧配置文件里残留的 allowExtraction 会被自动忽略。")
                .define("extractionMode", "ENABLED");
        MAX_VISIBLE_SLOTS = b
                .comment("虚拟容器槽位数按“转化桌中记录了多少个物品”自动扩展（9 个一行，最少 27），修复记录一多终端显示不全的问题。",
                        "这里是扩展上限，超出上限的记录仍会被截断（性能保护）。")
                .defineInRange("maxVisibleSlots", 2304, 27, 8192);
        NBT_FILTER_MODE = b
                .comment("入库过滤（决定哪些物品不允许被放入转化桌/链接方块折算 EMC）：",
                        "ANY_NBT（默认）＝带任何数据组件改动（1.21 起取代 NBT 标签：附魔、耐久消耗、药水、装有物的潜影盒、重命名物品等）的物品都拒绝，防止 EMC 套利与内容走私；",
                        "ENCHANT_DURABILITY＝只拒绝已附魔或已消耗耐久值的物品；OFF＝不做过滤。")
                .define("nbtFilterMode", "ANY_NBT");
        SPEC = b.build();
    }
}
