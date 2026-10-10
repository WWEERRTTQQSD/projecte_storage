package cn.blockforge.generated.projectestoragemodemc;

import cn.blockforge.generated.projectestoragemodemc.block.RefinedLinkBlock;
import cn.blockforge.generated.projectestoragemodemc.block.RefinedLinkBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 注册表：方块 / 物品 / 方块实体（NeoForge DeferredRegister 风格）。 */
public final class ModReg {
    private ModReg() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GeneratedMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GeneratedMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GeneratedMod.MOD_ID);

    public static final DeferredBlock<RefinedLinkBlock> REFINED_LINK = BLOCKS.registerBlock("refined_link",
            RefinedLinkBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 9.0F)
                    .lightLevel(state -> 4));

    public static final DeferredItem<BlockItem> REFINED_LINK_ITEM =
            ITEMS.registerSimpleBlockItem(REFINED_LINK, new Item.Properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RefinedLinkBlockEntity>> REFINED_LINK_BE =
            BE_TYPES.register("refined_link",
                    () -> BlockEntityType.Builder.of(RefinedLinkBlockEntity::new, REFINED_LINK.get()).build(null));
}
