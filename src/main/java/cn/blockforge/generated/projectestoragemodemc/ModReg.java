package cn.blockforge.generated.projectestoragemodemc;

import cn.blockforge.generated.projectestoragemodemc.block.RefinedLinkBlock;
import cn.blockforge.generated.projectestoragemodemc.block.RefinedLinkBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 注册表：方块 / 物品 / 方块实体。 */
public final class ModReg {
    private ModReg() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, GeneratedMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GeneratedMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GeneratedMod.MOD_ID);

    public static final RegistryObject<RefinedLinkBlock> REFINED_LINK = BLOCKS.register("refined_link",
            () -> new RefinedLinkBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 9.0F)
                    .lightLevel(state -> 4)));

    public static final RegistryObject<Item> REFINED_LINK_ITEM = ITEMS.register("refined_link",
            () -> new BlockItem(REFINED_LINK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<RefinedLinkBlockEntity>> REFINED_LINK_BE = BE_TYPES.register("refined_link",
            () -> BlockEntityType.Builder.of(RefinedLinkBlockEntity::new, REFINED_LINK.get()).build(null));
}
