package tech.anonymoushacker1279.openportals.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.anonymoushacker1279.openportals.OpenPortals;
import tech.anonymoushacker1279.openportals.portal.CustomPortalBlock;
import tech.anonymoushacker1279.openportals.portal.PortalLink;

@Mixin(Hud.class)
public class HudMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private int openportals$lastColor = -1;

	@ModifyExpressionValue(method = "extractPortalOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;white(F)I"))
	public int changeColor(int original) {
		if (minecraft.player == null) {
			return original;
		}

		openportals$isCustomPortal(minecraft.player);
		return openportals$lastColor >= 0 ? openportals$lastColor : original;
	}

	@Redirect(method = "extractPortalOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockStateModelSet;getParticleMaterial(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/resources/model/sprite/Material$Baked;"))
	public Material.Baked renderCustomPortalOverlay(BlockStateModelSet instance, BlockState blockState) {
		if (openportals$lastColor >= 0) {
			return this.minecraft
					.getModelManager()
					.getBlockStateModelSet()
					.getParticleMaterial(OpenPortals.CUSTOM_PORTAL_BLOCK.get().defaultBlockState());
		}

		return this.minecraft
				.getModelManager()
				.getBlockStateModelSet()
				.getParticleMaterial(Blocks.NETHER_PORTAL.defaultBlockState());
	}

	@Unique
	private void openportals$isCustomPortal(LocalPlayer player) {
		PortalProcessor portalManager = player.portalProcess;
		Portal portalBlock = portalManager != null && portalManager.isInsidePortalThisTick()
				? portalManager.portal
				: null;
		BlockPos portalPos = portalManager != null && portalManager.isInsidePortalThisTick()
				? portalManager.getEntryPosition()
				: null;

		if (portalBlock == null) {
			return;
		}

		if (portalBlock instanceof CustomPortalBlock customportalblock && portalPos != null) {
			PortalLink link = OpenPortals.getPortalManager().getPortalLinkFromBase(customportalblock.getCachedPortalBase(player.level(), portalPos));
			if (link != null) {
				openportals$lastColor = link.getColor();
				return;
			}
		}

		openportals$lastColor = -1;
	}
}