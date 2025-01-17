package net.kyrptonaught.customportalapi;

import java.util.Random;

import javax.swing.text.html.BlockView;

import net.kyrptonaught.customportalapi.init.ParticleInit;
import net.kyrptonaught.customportalapi.interfaces.EntityInCustomPortal;
import net.kyrptonaught.customportalapi.portal.frame.PortalFrameTester;
import net.kyrptonaught.customportalapi.util.CustomPortalHelper;
import net.kyrptonaught.customportalapi.util.CustomTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class CustomPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    protected static final VoxelShape X_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
    protected static final VoxelShape Y_SHAPE = Block.box(0.0D, 6.0D, 0.0D, 16.0D, 10.0D, 16.0D);

    public CustomPortalBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case Z -> Z_SHAPE;
            case Y -> Y_SHAPE;
            default -> X_SHAPE;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockGetter world, @NotNull BlockPos pos, @NotNull BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState newState, @NotNull LevelAccessor world, @NotNull BlockPos pos, @NotNull BlockPos posFrom) {        var block = getPortalBase((Level) world, pos);
        var link = CustomPortalApiRegistry.getPortalLinkFromBase(block);
        if (link != null) {
            PortalFrameTester portalFrameTester = link.getFrameTester().createInstanceOfPortalFrameTester().init(world, pos, CustomPortalHelper.getAxisFrom(state), block);
            if (portalFrameTester.isAlreadyLitPortalFrame())
                return super.updateShape(state, direction, newState, world, pos, posFrom);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0)
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);

        for (var i = 0; i < 4; ++i) {
            var d = pos.getX() + random.nextDouble();
            var e = pos.getY() + random.nextDouble();
            var f = pos.getZ() + random.nextDouble();
            var g = (random.nextFloat() - 0.5D) * 0.5D;
            var h = (random.nextFloat() - 0.5D) * 0.5D;
            var j = (random.nextFloat() - 0.5D) * 0.5D;
            int k = random.nextInt(2) * 2 - 1;
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                d = pos.getX() + 0.5D + 0.25D * k;
                g = random.nextFloat() * 2.0F * k;
            } else {
                f = pos.getZ() + 0.5D + 0.25D * k;
                j = random.nextFloat() * 2.0F * k;
            }
            level.addParticle(new BlockParticleOption(ParticleInit.CUSTOMPORTALPARTICLE.get(), getPortalBase(level, pos).defaultBlockState()), d, e, f, g, h, j);
        }
    }


    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level world, BlockPos pos, @NotNull Entity entity) {
        var entityInPortal = (EntityInCustomPortal) entity;
        entityInPortal.tickInPortal(pos.immutable());
        if (!entityInPortal.didTeleport() && entityInPortal.getTimeInPortal() >= entity.getPortalWaitTime()) {
            entityInPortal.setDidTP(true);
            if (!world.isClientSide())
                CustomTeleporter.TPToDim(world, entity, getPortalBase(world, pos), pos);
        }
    }

    public Block getPortalBase(Level world, BlockPos pos) {
        return CustomPortalHelper.getPortalBaseDefault(world, pos);
    }
}