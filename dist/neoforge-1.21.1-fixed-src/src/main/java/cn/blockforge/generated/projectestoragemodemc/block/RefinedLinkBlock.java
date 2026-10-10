package cn.blockforge.generated.projectestoragemodemc.block;

import cn.blockforge.generated.projectestoragemodemc.emc.ProjectEReflect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 精炼 EMC 链接方块。
 * - 玩家放置 → 绑定为该玩家的链接（桥接模式=该玩家转化桌 EMC 账户与已学习清单；
 *   ProjectE 不在场则作为独立 EMC 账簿）。
 * - 双向可用（默认 extractionMode=ENABLED）：终端既可把物品推进来折算 EMC（漏洞输入保持可用，
 *   含附魔/耐久消耗等组件改动的物品被过滤拒绝），也可从虚拟槽提取与一键合成——每次提取实时扣 EMC；
 *   配置设 DISABLED 可退回严格只进不出（代价是一键合成取不到材料）。
 *   虚拟槽位数随记录条目数自动扩展，终端显示不再截断。
 * - 虚拟槽显示数量 = 当前 EMC 余额下可购买该物品的总量（floor(余额/单价)），
 *   总量超过 2,147,483,647 时按该最大值封顶显示。
 * - 潜行右键 → 重新绑定到点击者；普通右键 → 查看账户状态（余额/记录数/槽位数与绑定主人玩家名）。
 * - 比较器：按“当前买得起的条目数”输出 1~15。
 * - 相邻同种方块隐藏公共面（标准立方体渲染）。
 */
public class RefinedLinkBlock extends Block implements EntityBlock {

    private static final String MSG = "msg.projecte_storage_mod_emc.";

    public RefinedLinkBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefinedLinkBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof RefinedLinkBlockEntity be) {
            be.bindOwner(player.getUUID(), player.getScoreboardName());
            player.displayClientMessage(Component.translatable(MSG + "placed"), true);
        }
    }

    /** 1.21.1：原版把旧的 use(...) 拆成 useWithoutItem/useItemOn；本方块空手右键查看状态。 */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof RefinedLinkBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            be.bindOwner(player.getUUID(), player.getScoreboardName());
            player.displayClientMessage(
                    Component.translatable(MSG + "rebound", player.getScoreboardName()), true);
            return InteractionResult.CONSUME;
        }
        long balance = be.getAccount().balance();
        int known = be.recordedCount();
        int listable = be.listableCount();
        int slots = be.virtualSlots();
        String key = be.isBridge() ? MSG + "status.bridge" : MSG + "status.ledger";
        player.displayClientMessage(Component.translatable(key, balance, known, listable, slots), false);
        if (be.getOwner() != null) {
            // 绑定主人显示玩家名（在线→当前ID；离线→usercache；兜底→绑定时缓存名；再兜底→UUID短码）
            player.displayClientMessage(
                    Component.translatable(MSG + "status.owner", be.getOwnerDisplayName()), true);
        }
        if (!be.isBridge() && !ProjectEReflect.isAvailable()) {
            player.displayClientMessage(Component.translatable(MSG + "status.no_projecte"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RefinedLinkBlockEntity be) {
            int n = be.listableCount();
            if (n <= 0) {
                return 0;
            }
            return Math.max(1, Math.min(15, (n * 15 + 13) / 14));
        }
        return 0;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this);
    }
}
