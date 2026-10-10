package cn.blockforge.generated.projectestoragemodemc.emc;

import cn.blockforge.generated.projectestoragemodemc.GeneratedMod;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * ProjectE（等价交换）API 反射桥接：全部签名对照 ProjectE mc1.21.1 分支（NeoForge 1.21.1）源码
 * （与 mc1.20.x 分支的 API 形态一致，包名/方法名/返回类型均已核对）：
 *
 * - moze_intel.projecte.api.proxy.IEMCProxy#INSTANCE：
 *     boolean hasValue(ItemStack) / long getValue(ItemStack) / long getValue(ItemInfo)
 * - moze_intel.projecte.api.proxy.ITransmutationProxy#INSTANCE：
 *     IKnowledgeProvider getKnowledgeProviderFor(UUID)
 * - moze_intel.projecte.api.capabilities.IKnowledgeProvider：
 *     BigInteger getEmc() / void setEmc(BigInteger)
 *     boolean hasKnowledge(ItemStack) / boolean addKnowledge(ItemStack) / boolean hasFullKnowledge()
 *     Set&lt;ItemInfo&gt; getKnowledge() / void syncEmc(ServerPlayer)
 * - moze_intel.projecte.api.ItemInfo：ItemStack createStack()
 *
 * 本模组编译期与运行期都不硬依赖 ProjectE：未安装或 API 结构对不上时，
 * detect() 失败后自动落到本地 EMC 账簿，模组照常可编译可玩。
 */
public final class ProjectEReflect {
    private ProjectEReflect() {}

    /** 0=未检测 1=可用 2=不可用 */
    private static volatile int state = 0;

    public static Object emcProxy;
    public static MethodHandle knowledgeProviderFor; // (UUID) -> provider
    public static MethodHandle getEmc;               // (provider) -> BigInteger
    public static MethodHandle setEmc;               // (provider, BigInteger) -> void
    public static MethodHandle hasKnowledge;         // (provider, ItemStack) -> boolean
    public static MethodHandle hasFullKnowledge;     // (provider) -> boolean
    public static MethodHandle getKnowledge;         // (provider) -> Set<ItemInfo>
    public static MethodHandle addKnowledge;         // (provider, ItemStack) -> boolean
    public static MethodHandle syncEmcHandle;        // (provider, ServerPlayer) -> void
    public static MethodHandle emcHasValueStack;     // (ItemStack) -> boolean   bound
    public static MethodHandle emcGetValueStack;     // (ItemStack) -> long      bound
    public static MethodHandle emcGetValueInfo;      // (ItemInfo) -> long       bound
    public static MethodHandle itemInfoCreateStack;  // (ItemInfo) -> ItemStack

    public static boolean isAvailable() {
        if (state == 0) {
            init();
        }
        return state == 1;
    }

    private static synchronized void init() {
        if (state != 0) {
            return;
        }
        try {
            if (!ModList.get().isLoaded("projecte")) {
                state = 2;
                return;
            }
            MethodHandles.Lookup lk = MethodHandles.lookup();

            Class<?> ikpCls = Class.forName("moze_intel.projecte.api.capabilities.IKnowledgeProvider");
            Class<?> tproxyCls = Class.forName("moze_intel.projecte.api.proxy.ITransmutationProxy");
            Object tproxy = tproxyCls.getField("INSTANCE").get(null);
            knowledgeProviderFor = lk.findVirtual(tproxyCls, "getKnowledgeProviderFor",
                    MethodType.methodType(ikpCls, UUID.class)).bindTo(tproxy);

            getEmc = lk.findVirtual(ikpCls, "getEmc", MethodType.methodType(BigInteger.class));
            setEmc = lk.findVirtual(ikpCls, "setEmc", MethodType.methodType(void.class, BigInteger.class));
            hasKnowledge = lk.findVirtual(ikpCls, "hasKnowledge", MethodType.methodType(boolean.class, ItemStack.class));
            hasFullKnowledge = lk.findVirtual(ikpCls, "hasFullKnowledge", MethodType.methodType(boolean.class));
            getKnowledge = lk.findVirtual(ikpCls, "getKnowledge", MethodType.methodType(java.util.Set.class));
            addKnowledge = lk.findVirtual(ikpCls, "addKnowledge", MethodType.methodType(boolean.class, ItemStack.class));
            syncEmcHandle = lk.findVirtual(ikpCls, "syncEmc", MethodType.methodType(void.class,
                    Class.forName("net.minecraft.server.level.ServerPlayer")));

            Class<?> emcCls = Class.forName("moze_intel.projecte.api.proxy.IEMCProxy");
            emcProxy = emcCls.getField("INSTANCE").get(null);
            emcHasValueStack = lk.findVirtual(emcCls, "hasValue", MethodType.methodType(boolean.class, ItemStack.class)).bindTo(emcProxy);
            emcGetValueStack = lk.findVirtual(emcCls, "getValue", MethodType.methodType(long.class, ItemStack.class)).bindTo(emcProxy);
            Class<?> infoCls = Class.forName("moze_intel.projecte.api.ItemInfo");
            emcGetValueInfo = lk.findVirtual(emcCls, "getValue", MethodType.methodType(long.class, infoCls)).bindTo(emcProxy);
            itemInfoCreateStack = lk.findVirtual(infoCls, "createStack", MethodType.methodType(ItemStack.class));

            state = 1;
            GeneratedMod.LOGGER.info("[EmcLink] ProjectE API 桥接解析成功，链接方块将直连玩家转化网络 EMC 账户。");
        } catch (Throwable t) {
            state = 2;
            GeneratedMod.LOGGER.warn("[EmcLink] ProjectE API 桥接不可用（{}），使用本地 EMC 账簿。", t.toString());
        }
    }

    public static Object providerOf(UUID owner) {
        try {
            return knowledgeProviderFor.invoke(owner);
        } catch (Throwable t) {
            return null;
        }
    }

    /** BigInteger 余额折算成 long（超大值饱和到 Long 的 1/4，避免后续乘价溢出）。 */
    public static long cap(BigInteger emc) {
        if (emc == null || emc.signum() <= 0) {
            return 0L;
        }
        if (emc.bitLength() >= 60) {
            return Long.MAX_VALUE >>> 4;
        }
        return emc.longValue();
    }
}
