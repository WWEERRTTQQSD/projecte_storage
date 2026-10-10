package cn.blockforge.generated.projectestoragemodemc.block;

import cn.blockforge.generated.projectestoragemodemc.EmcLinkConfig;
import cn.blockforge.generated.projectestoragemodemc.ModReg;
import cn.blockforge.generated.projectestoragemodemc.emc.EmcAccount;
import cn.blockforge.generated.projectestoragemodemc.emc.LocalLedgerAccount;
import cn.blockforge.generated.projectestoragemodemc.emc.ProjectEAccount;
import cn.blockforge.generated.projectestoragemodemc.emc.ProjectEReflect;
import cn.blockforge.generated.projectestoragemodemc.emc.VirtualEmcItemHandler;
import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/**
 * 链接方块实体：持有一个账户（ProjectE 桥接或本地账簿）与动态槽位虚拟容器。
 * NBT 中持久化：Owner（UUID）、OwnerName（绑定时记录的玩家名，显示用）、
 * Mode（BRIDGE/LEDGER）、Ledger（账簿数据）、
 * Recorded（记录的条目数）与 VirtualSlots（对外暴露的虚拟槽位数）——
 * 后两者即“记录了几个物品、占几个物品槽”，供状态查看与 /nbt 审计。
 */
public class RefinedLinkBlockEntity extends BlockEntity {

    private static final String TAG_OWNER = "Owner";
    private static final String TAG_OWNER_NAME = "OwnerName";
    private static final String TAG_MODE = "Mode";
    private static final String TAG_LEDGER = "Ledger";
    private static final String TAG_RECORDED = "Recorded";
    private static final String TAG_SLOTS = "VirtualSlots";
    private static final String MODE_BRIDGE = "BRIDGE";
    private static final String MODE_LEDGER = "LEDGER";

    @Nullable
    private UUID owner;
    /** 绑定时刻记录的玩家名（显示用兜底）；解析优先级见 {@link #getOwnerDisplayName()}。 */
    @Nullable
    private String ownerName;
    private boolean bridge;
    private EmcAccount account;
    private final VirtualEmcItemHandler handler = new VirtualEmcItemHandler(this);
    @Nullable
    private LazyOptional<IItemHandler> cap;

    private long lastFeedbackTick = Long.MIN_VALUE;

    public RefinedLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModReg.REFINED_LINK_BE.get(), pos, state);
        this.account = new LocalLedgerAccount(); // 占位，onLoad/绑定后重建。
    }

    /** 放置或交互时确定账户来源。切换到桥接会保留已加载的本地账簿以便降级。 */
    public void bindOwner(@Nullable UUID owner, @Nullable String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
        boolean wantBridge = owner != null
                && EmcLinkConfig.ENABLE_PE_BRIDGE.get()
                && ProjectEReflect.isAvailable();
        this.bridge = wantBridge;
        if (wantBridge) {
            ProjectEAccount pe = new ProjectEAccount(owner);
            if (level != null && level.getServer() != null) {
                pe.attachServer(level.getServer());
            }
            this.account = pe;
        } else if (!(account instanceof LocalLedgerAccount)) {
            this.account = new LocalLedgerAccount();
        }
        handler.invalidateCache();
        onLedgerChanged();
    }

    /** 当前清单中“显示得出来”（买得起至少 1 个）的条目数。 */
    public int listableCount() {
        return handler.currentListableCount();
    }

    /** 转化桌里记录了多少个物品（触发一次节流刷新）。 */
    public int recordedCount() {
        return handler.recordedCount();
    }

    /** 虚拟容器当前对外暴露多少个物品槽（9 的倍数，随记录数扩展）。 */
    public int virtualSlots() {
        return handler.virtualSlots();
    }

    /** 虚拟槽位数跨行变化：标脏并通知邻接（终端重扫槽位规模、比较器重读信号）。 */
    public void onSlotsChanged() {
        setChanged();
        if (level instanceof ServerLevel sl && !sl.isClientSide()) {
            sl.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    public void bindLedgerMode() {
        this.owner = null;
        this.ownerName = null;
        this.bridge = false;
        this.account = new LocalLedgerAccount();
        handler.invalidateCache();
        setChanged();
    }

    public EmcAccount getAccount() {
        return account;
    }

    public boolean isBridge() {
        return bridge;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    @Nullable
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * 主人显示名解析，参考 ProjectExpansion 的 OwnerName 持久化并加强：
     * ① 主人在线 → 取其当前 ID（改名后自动跟新）；
     * ② 否则查服务端 usercache（GameProfileCache，离线也会命中曾登录过的玩家）；
     * ③ 否则用绑定时持久化在 NBT 的名字（离线模式服务器等无档案缓存的场合）；
     * ④ 全都没有才退化为 UUID 短码 "#xxxxxxxx"。
     * 解析到新名字时顺带回写缓存字段，让 /nbt 审计里的 OwnerName 自愈。
     */
    public String getOwnerDisplayName() {
        if (owner == null) {
            return "";
        }
        MinecraftServer server = level != null ? level.getServer() : null;
        if (server != null) {
            ServerPlayer sp = server.getPlayerList().getPlayer(owner);
            if (sp != null) {
                return refreshOwnerName(sp.getScoreboardName());
            }
            Optional<GameProfile> profile = server.getProfileCache().get(owner);
            if (profile.isPresent()) {
                return refreshOwnerName(profile.get().getName());
            }
        }
        if (ownerName != null && !ownerName.isEmpty()) {
            return ownerName;
        }
        return "#" + owner.toString().substring(0, 8);
    }

    private String refreshOwnerName(String resolved) {
        if (!resolved.equals(ownerName)) {
            ownerName = resolved;
            setChanged();
        }
        return resolved;
    }

    public IItemHandler getItemHandler() {
        return handler;
    }

    public void onLedgerChanged() {
        setChanged();
        if (level instanceof ServerLevel sl) {
            sl.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    /** 提取/投入反馈：成功青色粒子+确认音，失败红色粒子+错误音，按 soundCooldownTicks 节流。 */
    public void playFeedback(boolean success, int amount) {
        Level lvl = level;
        if (!(lvl instanceof ServerLevel sl)) {
            return;
        }
        long now = sl.getGameTime();
        if (now - lastFeedbackTick < Math.max(1, EmcLinkConfig.SOUND_COOLDOWN_TICKS.get())) {
            return;
        }
        lastFeedbackTick = now;
        EmcFeedback.emit(sl, worldPosition, success);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> c, @Nullable Direction side) {
        if (!remove && c == ForgeCapabilities.ITEM_HANDLER) {
            if (cap == null) {
                cap = LazyOptional.of(this::getItemHandler);
            }
            return cap.cast();
        }
        return super.getCapability(c, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (cap != null) {
            cap.invalidate();
            cap = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID(TAG_OWNER, owner);
        }
        if (ownerName != null && !ownerName.isEmpty()) {
            tag.putString(TAG_OWNER_NAME, ownerName);
        }
        tag.putString(TAG_MODE, bridge ? MODE_BRIDGE : MODE_LEDGER);
        if (account instanceof LocalLedgerAccount ledger) {
            CompoundTag led = new CompoundTag();
            ledger.save(led);
            tag.put(TAG_LEDGER, led);
        }
        // 统计快照：记录了几个物品、对外暴露几个虚拟槽（只读缓存，保存时不触发清单重建）。
        tag.putInt(TAG_RECORDED, handler.cachedRecordedCount());
        tag.putInt(TAG_SLOTS, handler.virtualSlots());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID(TAG_OWNER)) {
            this.owner = tag.getUUID(TAG_OWNER);
        } else {
            this.owner = null;
        }
        String name = tag.getString(TAG_OWNER_NAME);
        this.ownerName = name.isEmpty() ? null : name;
        String mode = tag.getString(TAG_MODE);
        // 加载时若此前是桥接但桥接现在不可用（比如 ProjectE 被移除），降级到本地账簿。
        this.bridge = MODE_BRIDGE.equals(mode) && ProjectEReflect.isAvailable();
        if (this.bridge && owner != null) {
            ProjectEAccount pe = new ProjectEAccount(owner);
            if (level != null && level.getServer() != null) {
                pe.attachServer(level.getServer());
            }
            this.account = pe;
        } else {
            LocalLedgerAccount ledger = new LocalLedgerAccount();
            if (tag.contains(TAG_LEDGER)) {
                ledger.load(tag.getCompound(TAG_LEDGER));
            }
            this.account = ledger;
            this.bridge = false;
        }
        handler.invalidateCache();
    }
}
