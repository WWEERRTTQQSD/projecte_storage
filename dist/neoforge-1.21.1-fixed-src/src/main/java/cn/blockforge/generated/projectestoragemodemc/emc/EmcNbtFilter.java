package cn.blockforge.generated.projectestoragemodemc.emc;

import cn.blockforge.generated.projectestoragemodemc.EmcLinkConfig;
import net.minecraft.world.item.ItemStack;

/**
 * 入库过滤器：决定物品是否因携带自定义数据而被拒绝放入转化桌
 * （即被推进链接方块折算成 EMC）。
 * 1.21 起原版用"数据组件（DataComponent）"取代了旧 NBT 标签，本过滤器语义不变：
 * 模式由配置 nbtFilterMode 控制（键名沿用旧配置，含义平移）：
 * - ANY_NBT（默认）：只要组件相对默认值有改动（getComponentsPatch 非空）就拒绝——附魔、耐久消耗、药水、
 *   装有物品的潜影盒、重命名物品等全部拦截，防止 EMC 套利与内容走私；
 * - ENCHANT_DURABILITY：只拒绝已附魔或已消耗耐久值的物品；
 * - OFF：不过滤。
 */
public final class EmcNbtFilter {

    public static final String MODE_ANY_NBT = "ANY_NBT";
    public static final String MODE_ENCHANT_DURABILITY = "ENCHANT_DURABILITY";
    public static final String MODE_OFF = "OFF";

    private EmcNbtFilter() {}

    /** true＝该物品被过滤拒绝，不允许入库。判断本身无副作用、可高频调用。 */
    public static boolean rejected(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // 宽容归一化：null/空/陌生值一律按默认模式 ANY_NBT 处理。
        String raw = EmcLinkConfig.NBT_FILTER_MODE.get();
        String mode = (raw == null || raw.isBlank()) ? MODE_ANY_NBT : raw.trim().toUpperCase(java.util.Locale.ROOT);
        if (MODE_OFF.equals(mode)) {
            return false;
        }
        // 附魔（含附魔书的光泽）与耐久消耗在任何启用模式下都拒绝。
        if (stack.isEnchanted() || stack.isDamaged()) {
            return true;
        }
        // 1.21 的"带标签"判定：数据组件补丁非空即视为有自定义数据（旧 hasTag 的平移）。
        // 除显式的 ENCHANT_DURABILITY 外，陌生值都按 ANY_NBT 的严格语义处理。
        return !MODE_ENCHANT_DURABILITY.equals(mode) && !stack.getComponentsPatch().isEmpty();
    }
}
