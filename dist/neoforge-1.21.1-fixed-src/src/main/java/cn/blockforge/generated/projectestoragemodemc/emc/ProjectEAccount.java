package cn.blockforge.generated.projectestoragemodemc.emc;

import cn.blockforge.generated.projectestoragemodemc.EmcLinkConfig;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * ProjectE 桥接账户：全部走 ProjectEReflect 的 MethodHandle，反射调用官方 API，
 * 读取“绑定玩家（账户主人）”的转化网络 EMC、已学习清单与单价。
 */
public final class ProjectEAccount implements EmcAccount {

    private final UUID owner;
    @Nullable
    private MinecraftServer server;

    public ProjectEAccount(UUID owner) {
        this.owner = owner;
    }

    public void attachServer(MinecraftServer server) {
        this.server = server;
    }

    private Object provider() {
        return ProjectEReflect.providerOf(owner);
    }

    @Override
    public long balance() {
        Object p = provider();
        if (p == null) {
            return 0L;
        }
        try {
            return ProjectEReflect.cap((BigInteger) ProjectEReflect.getEmc.invoke(p));
        } catch (Throwable t) {
            return 0L;
        }
    }

    @Override
    public boolean withdraw(long cost) {
        if (cost <= 0) {
            return true;
        }
        Object p = provider();
        if (p == null) {
            return false;
        }
        try {
            BigInteger have = (BigInteger) ProjectEReflect.getEmc.invoke(p);
            BigInteger need = BigInteger.valueOf(cost);
            if (have == null || have.compareTo(need) < 0) {
                return false;
            }
            ProjectEReflect.setEmc.invoke(p, have.subtract(need));
            return true;
        } catch (Throwable t) {
            // 主人离线时 provider 只读，或 EMC 不足：统一按失败处理（红粒子+错误音）。
            return false;
        }
    }

    @Override
    public long deposit(ItemStack stack) {
        long unit = price(stack);
        if (unit <= 0) {
            return 0L;
        }
        long amount = unit * stack.getCount();
        Object p = provider();
        if (p == null) {
            return 0L;
        }
        try {
            BigInteger have = (BigInteger) ProjectEReflect.getEmc.invoke(p);
            if (have == null) {
                have = BigInteger.ZERO;
            }
            ProjectEReflect.setEmc.invoke(p, have.add(BigInteger.valueOf(amount)));
            // 顺便记入学习清单：投入即“已记录”，动态清单里立刻可见（对应“每个人记录不同清单不同”）。
            try {
                ProjectEReflect.addKnowledge.invoke(p, stack.copyWithCount(1));
            } catch (Throwable ignore) {
                // 只读 provider（离线）时忽略，EMC 已入账。
            }
            return amount;
        } catch (Throwable t) {
            return 0L;
        }
    }

    @Override
    public long price(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        try {
            if (!(boolean) ProjectEReflect.emcHasValueStack.invoke(stack)) {
                return 0L;
            }
            return Math.max(0L, (long) ProjectEReflect.emcGetValueStack.invoke(stack));
        } catch (Throwable t) {
            return 0L;
        }
    }

    @Override
    public boolean learned(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Object p = provider();
        if (p == null) {
            return false;
        }
        try {
            if ((boolean) ProjectEReflect.hasFullKnowledge.invoke(p)) {
                return true;
            }
            return (boolean) ProjectEReflect.hasKnowledge.invoke(p, stack.copyWithCount(1));
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public List<EmcOffer> offers() {
        List<EmcOffer> out = new ArrayList<>();
        Object p = provider();
        if (p == null) {
            return out;
        }
        try {
            if ((boolean) ProjectEReflect.hasFullKnowledge.invoke(p)) {
                // 全知（书模式）：枚举所有有 EMC 值的物品。
                for (Item item : BuiltInRegistries.ITEM) {
                    if (item == Items.AIR) {
                        continue;
                    }
                    ItemStack probe = new ItemStack(item);
                    if (probe.isEmpty()) {
                        continue;
                    }
                    long v = price(probe);
                    if (v > 0) {
                        out.add(new EmcOffer(probe, v));
                    }
                }
            } else {
                Set<?> known = (Set<?>) ProjectEReflect.getKnowledge.invoke(p);
                if (known != null) {
                    for (Object info : known) {
                        try {
                            long v = (long) ProjectEReflect.emcGetValueInfo.invoke(info);
                            if (v <= 0) {
                                continue;
                            }
                            ItemStack st = (ItemStack) ProjectEReflect.itemInfoCreateStack.invoke(info);
                            if (st == null || st.isEmpty()) {
                                continue;
                            }
                            st.setCount(1);
                            out.add(new EmcOffer(st, v));
                        } catch (Throwable ignore) {
                            // 单条坏数据跳过。
                        }
                    }
                }
            }
        } catch (Throwable t) {
            out.clear();
        }
        out.sort(EmcOffer.BY_PRICE_ID);
        return out;
    }

    @Override
    public void syncDisplay(@Nullable ServerLevel level) {
        try {
            MinecraftServer s = level != null ? level.getServer() : server;
            if (s == null) {
                return;
            }
            ServerPlayer sp = s.getPlayerList().getPlayer(owner);
            Object p = provider();
            if (sp != null && p != null) {
                ProjectEReflect.syncEmcHandle.invoke(p, sp);
            }
        } catch (Throwable ignore) {
            // 同步失败不影响扣费结果。
        }
    }
}
