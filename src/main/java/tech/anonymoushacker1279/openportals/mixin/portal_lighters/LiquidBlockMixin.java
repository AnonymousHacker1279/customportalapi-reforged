package tech.anonymoushacker1279.openportals.mixin.portal_lighters;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.anonymoushacker1279.openportals.portal.PortalIgniter;
import tech.anonymoushacker1279.openportals.portal.PortalIgnitionSource;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {

	@Inject(method = "onPlace", at = @At("HEAD"))
	public void fluidPlacedAttemptPortalLight(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
		if (state.getFluidState().isSource()) {
			PortalIgniter.attemptPortalLight(level, pos, PortalIgnitionSource.fromFluid(state.getFluidState().getType()));
		}
	}
}