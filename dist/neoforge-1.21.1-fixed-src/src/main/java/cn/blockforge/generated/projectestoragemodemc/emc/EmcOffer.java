package cn.blockforge.generated.projectestoragemodemc.emc;

import cn.blockforge.generated.projectestoragemodemc.GeneratedMod;
import java.util.Comparator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 一条“可购买清单”条目：虚拟槽位映射的物品模板 + 单位 EMC 价格。
 */
public final class EmcOffer {
    public final ItemStack template;
    public final long unitPrice;

    public EmcOffer(ItemStack template, long unitPrice) {
        this.template = template;
        this.unitPrice = unitPrice;
    }

    /** 稳定排序：单价升序，再按注册名，保证动态代理槽的显示顺序不抖动。 */
    public static final Comparator<EmcOffer> BY_PRICE_ID = Comparator
            .comparingLong((EmcOffer o) -> o.unitPrice)
            .thenComparing(o -> {
                ResourceLocation rl = BuiltInRegistries.ITEM.getKey(o.template.getItem());
                return rl == null ? "" : rl.toString();
            });

    public static String idOf(ItemStack stack) {
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return rl == null ? GeneratedMod.MOD_ID + ":invalid" : rl.toString();
    }
}
