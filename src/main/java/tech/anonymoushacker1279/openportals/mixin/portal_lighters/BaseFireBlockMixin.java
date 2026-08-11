package tech.anonymoushacker1279.openportals.mixin.portal_lighters;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.anonymoushacker1279.openportals.portal.PortalIgniter;
import tech.anonymoushacker1279.openportals.portal.PortalIgnitionSource;

@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {

	@Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
	public void detectCustomPortal(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
		if (PortalIgniter.attemptPortalLight(level, pos, PortalIgnitionSource.FIRE)) {
			ci.cancel();
		}
	}
}