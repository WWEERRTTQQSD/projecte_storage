package cn.blockforge.generated.projectestoragemodemc;

import cn.blockforge.generated.projectestoragemodemc.block.RefinedLinkBlockEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EMC 链接方块（Refined EMC Link，NeoForge 1.21.1 版）：
 * 把转化桌的 EMC 账户包装成标准 IItemHandler 虚拟容器，供 AE2 / 精致存储 / Tom's Storage
 * 等终端类 Mod 通过存储总线、连接器或导入器直接识别。
 *
 * - 安装了 ProjectE 时：反射走 ProjectE 官方 API（IEMCProxy / IKnowledgeProvider），
 *   槽位数随“已学习记录数”动态扩展，显示该绑定玩家“已学习且买得起”的清单。
 *   默认提取可用（extractionMode=ENABLED）：终端可推进物品折算 EMC（含附魔/耐久等组件改动的物品被过滤拒绝），
 *   也可从虚拟槽提取/一键合成，每次提取实时扣 EMC；设 DISABLED 可退回严格只进不出。
 * - 未安装 ProjectE 时：自动退化为方块内置的本地 EMC 账簿，模组可独立游玩测试。
 * - 合成配方按是否安装 ProjectE 自动切换：装了用暗物质（四角）+能量汇聚器（中间），
 *   没装回退到铁锭+钻石的基础配方。
 */
@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "projecte_storage_mod_emc";
    public static final Logger LOGGER = LoggerFactory.getLogger(GeneratedMod.class);

    public GeneratedMod(IEventBus modEventBus, ModContainer modContainer) {
        ModReg.BLOCKS.register(modEventBus);
        ModReg.ITEMS.register(modEventBus);
        ModReg.BE_TYPES.register(modEventBus);
        // NeoForge 1.21.1 新能力系统：BlockEntity 的 IItemHandler 在模块总线集中注册，
        // 旧的 getCapability/LazyOptional 覆写方式在本版本已移除。
        modEventBus.addListener(RefinedLinkBlockEntity::registerCapabilities);
        modEventBus.addListener(this::onTabBuild);
        modEventBus.addListener(this::onCommonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, EmcLinkConfig.SPEC, MOD_ID + "-common.toml");
    }

    private void onTabBuild(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.BUILDING_BLOCKS.equals(event.getTabKey())) {
            event.accept(ModReg.REFINED_LINK_ITEM);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 桥接检测推迟到服务器运行期首次使用时进行（ProjectE 的 ServiceLoader 实例需在其注册完成后才稳定）。
        LOGGER.info("[EmcLink] 初始化完成：链接方块提供标准 IItemHandler（槽位随记录数动态扩展，提取/一键合成默认可用且实时扣 EMC，组件过滤入库），无需硬依赖终端类 Mod。");
    }
}
