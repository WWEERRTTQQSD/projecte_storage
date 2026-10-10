package cn.blockforge.generated.projectestoragemodemc.emc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 本地 EMC 账簿：不依赖任何 Mod 的兜底账户，让链接方块可独立游玩。
 * - 投入物品 → 按单价表折算入账 EMC（投入即学习）。
 * - 提取 → 实时扣减账簿余额。
 * 数据存在方块实体 NBT 中（OwnerEmc + Known 列表）。
 */
public final class LocalLedgerAccount implements EmcAccount {

    /** 每个已知条目的账簿余额（学习后独立记账，避免“卖空”问题）。 */
    private long emc;
    private final Map<String, Long> known = new LinkedHashMap<>(); // itemId -> unit price

    @Override
    public long balance() {
        return emc;
    }

    @Override
    public boolean withdraw(long cost) {
        if (cost <= 0) {
            return true;
        }
        if (emc < cost) {
            return false;
        }
        emc -= cost;
        return true;
    }

    @Override
    public long deposit(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        String id = EmcOffer.idOf(stack);
        long unit = known.getOrDefault(id, FallbackEmc.of(id));
        long amount = unit * stack.getCount();
        emc += amount;
        known.put(id, unit);
        return amount;
    }

    @Override
    public long price(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        String id = EmcOffer.idOf(stack);
        return known.getOrDefault(id, FallbackEmc.of(id));
    }

    @Override
    public boolean learned(ItemStack stack) {
        return !stack.isEmpty() && known.containsKey(EmcOffer.idOf(stack));
    }

    @Override
    public List<EmcOffer> offers() {
        List<EmcOffer> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : known.entrySet()) {
            long unit = e.getValue();
            if (unit <= 0) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(e.getKey())).orElse(null);
            if (item == null) {
                continue;
            }
            ItemStack template = new ItemStack(item);
            if (template.isEmpty()) {
                continue;
            }
            out.add(new EmcOffer(template, unit));
        }
        out.sort(EmcOffer.BY_PRICE_ID);
        return out;
    }

    @Override
    public void syncDisplay(@Nullable ServerLevel level) {
        // 无外部显示需同步。
    }

    public void save(CompoundTag tag) {
        tag.putLong("EmcLedger", emc);
        ListTag list = new ListTag();
        for (Map.Entry<String, Long> e : known.entrySet()) {
            CompoundTag et = new CompoundTag();
            et.putString("Item", e.getKey());
            et.putLong("Unit", e.getValue());
            list.add(et);
        }
        tag.put("KnownLedger", list);
    }

    public void load(CompoundTag tag) {
        emc = tag.getLong("EmcLedger");
        known.clear();
        if (tag.contains("KnownLedger", Tag.TAG_LIST)) {
            ListTag list = tag.getList("KnownLedger", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag et = list.getCompound(i);
                known.put(et.getString("Item"), et.getLong("Unit"));
            }
        }
    }
}
