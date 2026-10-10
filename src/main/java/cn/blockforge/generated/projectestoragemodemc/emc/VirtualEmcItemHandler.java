package cn.blockforge.generated.projectestoragemodemc.emc;

import cn.blockforge.generated.projectestoragemodemc.EmcLinkConfig;
import cn.blockforge.generated.projectestoragemodemc.block.RefinedLinkBlockEntity;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * 动态代理槽的虚拟物品处理器：
 * - 槽位数随“记录了多少个物品”自动扩展（9 个一行，最少 {@link #MIN_SLOTS}，上限 maxVisibleSlots），
 *   转化桌记录再多也不会把终端显示截断；
 * - getStackInSlot 按“已学习清单 + floor(余额/单价)”动态生成显示数量：
 *   槽内数量直接显示“当前 EMC 余额下可购买该物品的总量”（可超过堆叠上限），
 *   总量大于 Integer.MAX_VALUE（2,147,483,647）时按该最大值封顶显示；
 * - insertItem 把物品折成 EMC 存回绑定账户（投入即学习），并先过 {@link EmcNbtFilter}：
 *   含附魔、耐久消耗等 NBT 标签的物品直接拒绝入账；
 * - extractItem（默认 ENABLED，参考 emc-interface/ProjectExpansion 的可用提取设计）：
 *   simulate 与实取口径一致，实取原子扣 EMC——这是终端"一键合成"能工作的前提：
 *   AE2 合成 CPU / RS 合成计算先用 simulate 预演材料，再从本处理器整批提取消耗；
 *   任何在此处返回 EMPTY 的实现都会让合成判定"材料不足"而拒绝启动合成任务；
 *   提取按 EMC 计价、余额多少决定取多少，不构成免费复制；
 *   配置 extractionMode=DISABLED 可退回严格只进不出（代价是一键合成不可用）。
 * - getSlotLimit 返回 Integer.MAX_VALUE（同 emc-interface）：虚拟槽容量不是物理概念，
 *   否则合成 CPU 会把大批量材料拆成 64 个一批反复试探，批量合成容易中途卡住。
 * 清单缓存按 offerRefreshTicks 配置节流刷新，余额每次投入后立即失效重算；
 * 槽位数跨行变化时通过 be.onSlotsChanged() 通知邻接终端与比较器重扫。
 */
public final class VirtualEmcItemHandler implements IItemHandler {

    /** 最小槽位数：9 的倍数里不小于 27，兼容按箱子规模设计的终端。 */
    public static final int MIN_SLOTS = 27;

    private final RefinedLinkBlockEntity be;
    private List<EmcOffer> cache = List.of();
    private long cacheBalance = Long.MIN_VALUE;
    private long cacheTick = Long.MIN_VALUE;
    private int slotCount = MIN_SLOTS;

    public VirtualEmcItemHandler(RefinedLinkBlockEntity be) {
        this.be = be;
    }

    private EmcAccount account() {
        return be.getAccount();
    }

    private void refresh(ServerLevel level) {
        long now = level == null ? 0 : level.getGameTime();
        int period = EmcLinkConfig.OFFER_REFRESH_TICKS.get();
        long bal = account().balance();
        if (level == null || cacheTick == Long.MIN_VALUE || bal != cacheBalance || now - cacheTick >= period) {
            cacheTick = now;
            cacheBalance = bal;
            int cap = Math.max(MIN_SLOTS, EmcLinkConfig.MAX_VISIBLE_SLOTS.get());
            List<EmcOffer> list = account().offers();
            cache = list.size() > cap ? List.copyOf(list.subList(0, cap)) : List.copyOf(list);
            // 槽位数按记录数扩展：向上取整到 9 的倍数，且不小于 MIN_SLOTS。
            int rows = (cache.size() + 8) / 9;
            int newSlots = Math.max(MIN_SLOTS, Math.min(cap, rows * 9));
            if (newSlots != slotCount) {
                slotCount = newSlots;
                be.onSlotsChanged();
            }
        }
    }

    /** 外部（提取/投入后）强制失效缓存。 */
    public void invalidateCache() {
        cacheTick = Long.MIN_VALUE;
        cacheBalance = Long.MIN_VALUE;
    }

    /** 当前“买得起至少 1 个”的条目数（供状态显示与比较器）。 */
    public int currentListableCount() {
        refresh(be.getLevel() instanceof ServerLevel s ? s : null);
        int n = 0;
        for (EmcOffer o : cache) {
            if (displayCount(o) > 0) {
                n++;
            }
        }
        return n;
    }

    /** 记录条目总数（触发一次节流刷新后读取）。 */
    public int recordedCount() {
        refresh(be.getLevel() instanceof ServerLevel s ? s : null);
        return cache.size();
    }

    /** 记录条目总数（只读缓存、不触发刷新，供 NBT 持久化）。 */
    public int cachedRecordedCount() {
        return cache.size();
    }

    /** 当前对外暴露的虚拟槽位数（9 的倍数）。 */
    public int virtualSlots() {
        return slotCount;
    }

    private EmcOffer offerAt(int slot) {
        return slot >= 0 && slot < cache.size() ? cache.get(slot) : null;
    }

    /**
     * 显示数量 = 可购买该物品的总量 = floor(EMC 余额 / 单位价格)，不再受单槽堆叠上限裁剪。
     * 可购买总量超过 32 位整型最大值（Integer.MAX_VALUE = 2,147,483,647）时，
     * 终端里仅以此最大值为数量显示（count 字段本身是 int，饱和后不会溢出回绕）。
     */
    private int displayCount(EmcOffer offer) {
        if (offer == null || offer.unitPrice <= 0 || cacheBalance <= 0) {
            return 0;
        }
        long maxAffordable = cacheBalance / offer.unitPrice;
        if (maxAffordable <= 0) {
            return 0;
        }
        return maxAffordable > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxAffordable;
    }

    @Override
    public int getSlots() {
        return slotCount;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        refresh(be.getLevel() instanceof ServerLevel s ? s : null);
        EmcOffer offer = offerAt(slot);
        int count = displayCount(offer);
        if (offer == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack display = offer.template.copyWithCount(count);
        return display;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= getSlots()) {
            return stack;
        }
        // NBT 过滤：含附魔、耐久消耗等标签的物品不放入转化桌，整堆退回来源。
        if (EmcNbtFilter.rejected(stack)) {
            if (!simulate) {
                be.playFeedback(false, 0); // 被过滤：红粒子 + 错误音（按冷却节流）。
            }
            return stack;
        }
        // 漏洞/管道投料入口：物品折算成 EMC 存入绑定账户（任何槽位都吸收，保持第 3 轮已验证的输入路径）。
        EmcAccount acc = account();
        long unit = acc.price(stack);
        if (unit <= 0) {
            return stack; // 无价物品拒绝入账。
        }
        if (!simulate) {
            long credited = acc.deposit(stack);
            if (credited > 0) {
                invalidateCache();
                be.onLedgerChanged();
                acc.syncDisplay(be.getLevel() instanceof ServerLevel s ? s : null);
                be.playFeedback(true, stack.getCount());
            }
        }
        return ItemStack.EMPTY; // 整堆吸收为 EMC。
    }

    /**
     * 提取（终端一键合成的材料来源）：
     * ① DISABLED 模式静默返回空（终端轮询高频，刻意不播失败反馈）；
     * ② 数量 = min(请求量, 余额/单价, maxExtractPerCall>0 时的封顶)；
     * ③ simulate 与真实提取返回口径一致——合成预演拿到非空才会被排进合成任务；
     * ④ 真实提取先原子扣费再出货，扣费失败返回空，绝不先给货后扣费。
     */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!EmcLinkConfig.extractionEnabled()) {
            return ItemStack.EMPTY;
        }
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        refresh(be.getLevel() instanceof ServerLevel s ? s : null);
        EmcOffer offer = offerAt(slot);
        if (offer == null || offer.unitPrice <= 0) {
            return ItemStack.EMPTY;
        }
        int cap = EmcLinkConfig.MAX_EXTRACT_PER_CALL.get();
        int requested = cap > 0 ? Math.min(amount, cap) : amount;
        long affordable = cacheBalance / offer.unitPrice;
        // 允许单次提取超过堆叠上限：合成 CPU 常一次索取整批材料（emc-interface 同样不做 64 裁剪）。
        int take = (int) Math.min((long) requested, Math.min(affordable, Integer.MAX_VALUE));
        if (take <= 0) {
            if (!simulate) {
                be.playFeedback(false, 0); // 买不起：红粒子 + 错误音。
            }
            return ItemStack.EMPTY;
        }
        long cost = offer.unitPrice * (long) take;
        if (simulate) {
            return offer.template.copyWithCount(take);
        }
        EmcAccount acc = account();
        if (!acc.withdraw(cost)) {
            invalidateCache();
            be.playFeedback(false, 0);
            return ItemStack.EMPTY;
        }
        invalidateCache();
        be.onLedgerChanged();
        acc.syncDisplay(be.getLevel() instanceof ServerLevel s ? s : null);
        be.playFeedback(true, take);
        return offer.template.copyWithCount(take);
    }

    @Override
    public int getSlotLimit(int slot) {
        // 虚拟槽没有物理容量：与 emc-interface 一致返回整型最大值，
        // 让终端按"槽里有多少就能取多少"处理，一键合成的批量提取不被 64 拆批。
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // 终端在真正 insert 前通常先问这里：过滤口径与 insertItem 保持一致。
        return !stack.isEmpty()
                && !EmcNbtFilter.rejected(stack)
                && account().price(stack) > 0;
    }
}
