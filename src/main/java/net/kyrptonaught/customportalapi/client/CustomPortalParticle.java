package net.kyrptonaught.customportalapi.client;

import net.kyrptonaught.customportalapi.CustomPortalApiRegistry;
import net.kyrptonaught.customportalapi.util.ColorUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.BlockParticleOption;

public class CustomPortalParticle extends PortalParticle {
	protected CustomPortalParticle(ClientLevel clientWorld, double d, double e, double f, double g, double h, double i) {
		super(clientWorld, d, e, f, g, h, i);
	}

	public static class Factory implements ParticleProvider<BlockParticleOption> {
		private final SpriteSet spriteProvider;

		public Factory(SpriteSet spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		public Particle createParticle(BlockParticleOption blockStateParticleEffect, ClientLevel clientWorld, double d, double e, double f, double g, double h, double i) {
			var portalParticle = new CustomPortalParticle(clientWorld, d, e, f, g, h, i);
			portalParticle.pickSprite(this.spriteProvider);
			var block = blockStateParticleEffect.getState().getBlock();
			var link = CustomPortalApiRegistry.getPortalLinkFromBase(block);
			if (link != null) {
				float[] rgb = ColorUtil.getColorForBlock(link.colorID);
				portalParticle.setColor(rgb[0], rgb[1], rgb[2]);
			}
			return portalParticle;
		}
	}
}
