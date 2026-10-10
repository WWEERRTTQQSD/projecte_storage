package cn.blockforge.generated.projectestoragemodemc.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.joml.Vector3f;

/**
 * 粒子与音效反馈：
 * - 成功（提取/合成扣费成功）：方块顶部青色 EMC 粒子 + 短促确认音；
 * - 失败（EMC 不足）：红色粒子 + 错误音。
 * 全部用原版粒子/音符盒音效实现，资源包可整体替换。
 */
public final class EmcFeedback {
    private EmcFeedback() {}

    private static final Vector3f EMC_CYAN = new Vector3f(0.15F, 0.9F, 0.85F);
    private static final Vector3f EMC_RED = new Vector3f(0.92F, 0.18F, 0.15F);

    public static void emit(ServerLevel level, BlockPos pos, boolean success) {
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 1.05D;
        double cz = pos.getZ() + 0.5D;
        if (success) {
            level.sendParticles(new DustParticleOptions(EMC_CYAN, 1.1F), cx, cy, cz, 14, 0.34D, 0.1D, 0.34D, 0.012D);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 6, 0.3D, 0.18D, 0.3D, 0.02D);
            level.playSound(null, cx, cy, cz, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.6F, 1.55F);
        } else {
            level.sendParticles(new DustParticleOptions(EMC_RED, 1.25F), cx, cy, cz, 12, 0.34D, 0.1D, 0.34D, 0.012D);
            level.playSound(null, cx, cy, cz, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6F, 0.55F);
        }
    }
}
