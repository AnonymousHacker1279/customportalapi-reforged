package tech.anonymoushacker1279.openportals.mixin.portal_lighters;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.anonymoushacker1279.openportals.portal.PortalIgniter;
import tech.anonymoushacker1279.openportals.portal.PortalIgnitionSource;

@Mixin(AbstractThrownPotion.class)
public abstract class AbstractThrownPotionMixin extends ThrowableItemProjectile {

	public AbstractThrownPotionMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(method = "dowseFire", at = @At("HEAD"))
	public void attemptPortalLight(BlockPos pos, CallbackInfo ci) {
		PortalIgniter.attemptPortalLight(this.level(), pos, PortalIgnitionSource.WATER);
	}
}