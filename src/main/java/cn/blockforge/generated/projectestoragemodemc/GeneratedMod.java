package cn.blockforge.generated.projectestoragemodemc;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EMC 链接方块（Refined EMC Link）：
 * 把转化桌的 EMC 账户包装成标准 IItemHandler 虚拟容器，供 AE2 / 精致存储 / Tom's Storage
 * 等终端类 Mod 通过存储总线、连接器或导入器直接识别。
 *
 * - 安装了 ProjectE 时：反射走 ProjectE 官方 API（IEMCProxy / IKnowledgeProvider），
 *   槽位数随“已学习记录数”动态扩展，显示该绑定玩家“已学习且买得起”的清单。
 *   默认提取可用（extractionMode=ENABLED）：终端可推进物品折算 EMC（含附魔/耐久等 NBT 的物品被过滤拒绝），
 *   也可从虚拟槽提取/一键合成，每次提取实时扣 EMC；设 DISABLED 可退回严格只进不出。
 * - 未安装 ProjectE 时：自动退化为方块内置的本地 EMC 账簿，模组可独立游玩测试。
 * - 合成配方按是否安装 ProjectE 自动切换：装了用暗物质（四角）+能量汇聚器（中间），
 *   没装回退到铁锭+钻石的基础配方。
 */
@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "projecte_storage_mod_emc";
    public static final Logger LOGGER = LogManager.getLogger();

    public GeneratedMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModReg.BLOCKS.register(bus);
        ModReg.ITEMS.register(bus);
        ModReg.BE_TYPES.register(bus);
        bus.addListener(this::onTabBuild);
        bus.addListener(this::onCommonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EmcLinkConfig.SPEC, MOD_ID + "-common.toml");
    }

    private void onTabBuild(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            event.accept(ModReg.REFINED_LINK_ITEM);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 桥接检测推迟到服务器运行期首次使用时进行（ProjectE 的 ServiceLoader 实例需在其注册完成后才稳定）。
        LOGGER.info("[EmcLink] 初始化完成：链接方块提供标准 IItemHandler（槽位随记录数动态扩展，提取/一键合成默认可用且实时扣 EMC，NBT 过滤入库），无需硬依赖终端类 Mod。");
    }
}
