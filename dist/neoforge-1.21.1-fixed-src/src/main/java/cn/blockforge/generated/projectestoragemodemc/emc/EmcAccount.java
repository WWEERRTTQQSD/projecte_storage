package cn.blockforge.generated.projectestoragemodemc.emc;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * EMC 账户抽象：ProjectE 桥接（反射调用官方 API）与本地 EMC 账簿两种实现。
 * 所有方法都只在服务端主线程调用。
 */
public interface EmcAccount {

    /** 当前余额（本地账簿：账本余额；桥接模式：玩家转化网络的 EMC，读取失败返回 0）。 */
    long balance();

    /** 原子扣费：余额不足返回 false 且不产生任何副作用。 */
    boolean withdraw(long cost);

    /**
     * 存入物品换取 EMC（“投料”）：返回实际入账的 EMC，0 表示拒绝（无价格或离线只读等）。
     * 调用方负责在返回 0 时把物品还给来源。
     */
    long deposit(ItemStack stack);

    /** 单个物品的 EMC 单价；无价（0）表示不可交易条目。 */
    long price(ItemStack stack);

    /** 账户是否“认识”该物品（桥接模式=玩家已学习；账簿模式=曾入库）。 */
    boolean learned(ItemStack stack);

    /**
     * 动态清单：已学习且单价 &gt; 0 的全部条目，按单价/名称稳定排序。
     * “买得起”由显示端按 floor(余额/单价) 折算，余额为 0 的条目数量为 0。
     */
    List<EmcOffer> offers();

    /** 扣费/入账后刷新玩家客户端显示（桥接模式调用 ProjectE 的同步；账簿模式空操作）。 */
    void syncDisplay(@Nullable ServerLevel level);
}
