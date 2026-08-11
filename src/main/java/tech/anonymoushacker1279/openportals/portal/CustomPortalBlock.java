package tech.anonymoushacker1279.openportals.portal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import tech.anonymoushacker1279.openportals.OpenPortals;
import tech.anonymoushacker1279.openportals.portal.frame.PortalFrameTester;
import tech.anonymoushacker1279.openportals.portal.teleport.PortalTeleporter;
import tech.anonymoushacker1279.openportals.util.PortalConstants;
import tech.anonymoushacker1279.openportals.util.PortalUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class CustomPortalBlock extends Block implements Portal {

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
	protected static final VoxelShape X_SHAPE = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
	protected static final VoxelShape Z_SHAPE = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);
	protected static final VoxelShape Y_SHAPE = Block.box(0.0D, 6.0D, 0.0D, 16.0D, 10.0D, 16.0D);
	private static final Cache<BlockPos, PortalInfo> PORTAL_CACHE = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.of(10, ChronoUnit.MINUTES))
			.maximumSize(250)
			.build();

	public CustomPortalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	/**
	 * Invalidate the cache for a specific position. Called when a portal block is destroyed or modified.
	 *
	 * @param pos the position to invalidate
	 */
	public static void invalidateCache(BlockPos pos) {
		PORTAL_CACHE.invalidate(pos);
	}

	/**
	 * Get cached portal information for a position. If not cached, computes and caches the portal base and link.
	 *
	 * @param level the level
	 * @param pos   the portal block position
	 * @return cached portal information
	 */
	private PortalInfo getPortalInfo(Level level, BlockPos pos) {
		try {
			return PORTAL_CACHE.get(pos, () -> {
				Block portalBase = PortalUtils.getPortalBase(level, pos);
				PortalLink link = OpenPortals.getPortalManager().getPortalLinkFromBase(portalBase);
				return new PortalInfo(portalBase, link);
			});
		} catch (Exception e) {
			// Fallback: compute without caching if cache fails
			Block portalBase = PortalUtils.getPortalBase(level, pos);
			PortalLink link = OpenPortals.getPortalManager().getPortalLinkFromBase(portalBase);
			return new PortalInfo(portalBase, link);
		}
	}

	/**
	 * Get the portal base block for this portal position. Uses cached value if available.
	 *
	 * @param level the level
	 * @param pos   the portal block position
	 * @return the portal base block
	 */
	public Block getCachedPortalBase(Level level, BlockPos pos) {
		return getPortalInfo(level, pos).portalBase;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(AXIS)) {
			case Z -> Z_SHAPE;
			case Y -> Y_SHAPE;
			default -> X_SHAPE;
		};
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return ItemStack.EMPTY;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos,
	                                 Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {

		PortalInfo info = getPortalInfo((Level) level, pos);
		if (info.link != null) {
			// init() returns PortalValidator, but implementations are PortalFrameTester
			PortalFrameTester portalFrameTester = (PortalFrameTester) info.link.getFrameTester().init((LevelAccessor) level,
					pos,
					PortalUtils.getAxisFrom(state),
					info.portalBase);

			if (portalFrameTester.isAlreadyLitPortalFrame()) {
				return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
			}
		}

		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
		super.destroy(level, pos, state);
		invalidateCache(pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		PortalInfo info = getPortalInfo(level, pos);

		if (info.link == null) {
			return;
		}

		if (random.nextInt(PortalConstants.AMBIENT_SOUND_CHANCE) == 0) {
			SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(info.link.getAmbientSoundIdentifier());
			if (event != null) {
				level.playLocalSound(
						pos.getX() + 0.5D,
						pos.getY() + 0.5D,
						pos.getZ() + 0.5D,
						event,
						SoundSource.BLOCKS,
						info.link.getAmbientSound().volumeFunction().apply(level),
						info.link.getAmbientSound().pitchFunction().apply(level),
						false
				);
			}
		}

		for (int i = 0; i < PortalConstants.PARTICLES_PER_TICK; i++) {
			double dX = pos.getX() + random.nextDouble();
			double dY = pos.getY() + random.nextDouble();
			double dZ = pos.getZ() + random.nextDouble();
			double sX = (random.nextFloat() - 0.5d) * 0.5d;
			double sY = (random.nextFloat() - 0.5d) * 0.5d;
			double sZ = (random.nextFloat() - 0.5d) * 0.5d;
			int mod = random.nextInt(2) * 2 - 1;
			if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
				dX = pos.getX() + 0.5f + 0.25f * mod;
				sX = random.nextFloat() * 2.0f * mod;
			} else {
				dZ = pos.getZ() + 0.5f + 0.25f * mod;
				sZ = random.nextFloat() * 2.0f * mod;
			}

			level.addParticle(info.link.getPortalParticle().apply(level, pos), dX, dY, dZ, sX, sY, sZ);
		}
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
		if (entity.canUsePortal(false)) {
			entity.setAsInsidePortal(this, pos);
		}
	}

	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity) {
		if (entity instanceof Player playerEntity) {
			return Math.max(1, playerEntity.isCreative()
					? level.getGameRules().get(GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY)
					: level.getGameRules().get(GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY));
		}

		return 0;
	}

	@Override
	@Nullable
	public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
		PortalInfo info = getPortalInfo(level, pos);
		return PortalTeleporter.attemptTeleport(level, entity, info.portalBase, pos);
	}

	@Override
	public Transition getLocalTransition() {
		return Transition.CONFUSION;
	}

	/**
	 * Cached portal information for a position.
	 */
	private record PortalInfo(Block portalBase, @Nullable PortalLink link) {
	}
}