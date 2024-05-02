package net.kyrptonaught.customportalapi;

import net.kyrptonaught.customportalapi.interfaces.EntityInCustomPortal;
import net.kyrptonaught.customportalapi.portal.frame.PortalFrameTester;
import net.kyrptonaught.customportalapi.util.CustomPortalHelper;
import net.kyrptonaught.customportalapi.util.CustomTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;


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
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case Z -> Z_SHAPE;
            case Y -> Y_SHAPE;
            default -> X_SHAPE;
        };
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState newState, LevelAccessor world, BlockPos pos, BlockPos posFrom) {
        var block = getPortalBase((Level) world, pos);
        var link = CustomPortalApiRegistry.getPortalLinkFromBase(block);
        if (link != null) {
            PortalFrameTester portalFrameTester = link.getFrameTester().createInstanceOfPortalFrameTester().init(world,
                    pos, CustomPortalHelper.getAxisFrom(state), block);
            if (portalFrameTester.isAlreadyLitPortalFrame())
                return super.updateShape(state, direction, newState, world, pos, posFrom);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0)
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);

        for (var i = 0; i < 4; ++i) {
            var d = (double) pos.getX() + random.nextDouble();
            var e = (double) pos.getY() + random.nextDouble();
            var f = (double) pos.getZ() + random.nextDouble();
            var g = ((double) random.nextFloat() - 0.5D) * 0.5D;
            var h = ((double) random.nextFloat() - 0.5D) * 0.5D;
            var j = ((double) random.nextFloat() - 0.5D) * 0.5D;
            int k = random.nextInt(2) * 2 - 1;
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                d = (double) pos.getX() + 0.5D + 0.25D * (double) k;
                g = random.nextFloat() * 2.0F * (float) k;
            } else {
                f = (double) pos.getZ() + 0.5D + 0.25D * (double) k;
                j = random.nextFloat() * 2.0F * (float) k;
            }
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,
                    getPortalBase(level, pos).defaultBlockState()), d, e, f, g, h, j);
        }
    }


    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        var entityInPortal = (EntityInCustomPortal) entity;
        entityInPortal.tickInPortal(pos.immutable());
        if (!entityInPortal.didTeleport()) {
            if (entityInPortal.getTimeInPortal() >= entity.getPortalWaitTime()) {
                entityInPortal.setDidTP(true);
                if (!world.isClientSide()) CustomTeleporter.TPToDim(world, entity, getPortalBase(world, pos), pos);
            }
        }
    }

    public Block getPortalBase(Level world, BlockPos pos) {
        return CustomPortalHelper.getPortalBaseDefault(world, pos);
    }
}