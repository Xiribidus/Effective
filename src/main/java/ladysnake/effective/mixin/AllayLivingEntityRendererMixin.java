package ladysnake.effective.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import ladysnake.effective.client.Effective;
import ladysnake.effective.client.EffectiveConfig;
import ladysnake.effective.client.contracts.AllayParticleInitialData;
import lodestar.lodestone.helpers.EntityHelper;
import lodestar.lodestone.helpers.util.Color;
import lodestar.lodestone.setup.LodestoneRenderLayerRegistry;
import lodestar.lodestone.systems.easing.Easing;
import lodestar.lodestone.systems.rendering.VFXBuilders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(LivingEntityRenderer.class)
public abstract class AllayLivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> {
	ArrayList<EntityHelper.PastPosition> pastPositions = new ArrayList<>();

	protected AllayLivingEntityRendererMixin(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	private static HashMap<RenderLayer, BufferBuilder> BUFFERS = new HashMap<>();
	private static VertexConsumerProvider.Immediate DELAYED_RENDER = VertexConsumerProvider.immediate(BUFFERS, new BufferBuilder(256));

	private static final Identifier STAR_TRAIL = new Identifier(Effective.MODID, "textures/vfx/heavy_light_trail.png");
	private static final RenderLayer STAR_TRAIL_TYPE = LodestoneRenderLayerRegistry.ADDITIVE_TEXTURE_TRIANGLE.apply(STAR_TRAIL);

	// allay trail and twinkle
	@Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
	public void render(T livingEntity, float entityYaw, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int packedLight, CallbackInfo ci) {
		if (EffectiveConfig.enableAllayTrails && livingEntity instanceof AllayEntity allayEntity && allayEntity.getX() != allayEntity.prevX && allayEntity.getY() != allayEntity.prevY && allayEntity.getZ() != allayEntity.prevZ && !MinecraftClient.getInstance().isPaused()) {
			AllayParticleInitialData data = new AllayParticleInitialData(allayEntity.getUuid().hashCode() % 2 == 0 && EffectiveConfig.goldenAllays ? 0xFFC200 : 0x22CFFF);

			allayEntity.world.addParticle(Effective.ALLAY_TRAIL.setData(data),
					allayEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).x,
					allayEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).y - 0.2f,
					allayEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).z,
					0, 0, 0);

			if ((allayEntity.getRandom().nextInt(100) + 1) <= EffectiveConfig.allayTwinkleDensity) {
				allayEntity.world.addParticle(Effective.ALLAY_TWINKLE.setData(data),
						allayEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).x + allayEntity.getRandom().nextGaussian() / 2f,
						allayEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).y - 0.2f + allayEntity.getRandom().nextGaussian() / 2f,
						allayEntity.getClientCameraPosVec(MinecraftClient.getInstance().getTickDelta()).z + allayEntity.getRandom().nextGaussian() / 2f,
						0, 0, 0);
			}
		}

		EntityHelper.trackPastPositions(pastPositions, livingEntity.getPos(), 1f);

		List<EntityHelper.PastPosition> positions = new ArrayList<>(pastPositions);
		if (positions.size() > 1) {
			for (int i = 0; i < positions.size() - 2; i++) {
				EntityHelper.PastPosition position = positions.get(i);
				EntityHelper.PastPosition nextPosition = positions.get(i + 1);
				float x = (float) MathHelper.lerp(partialTicks, position.position.x, nextPosition.position.x);
				float y = (float) MathHelper.lerp(partialTicks, position.position.y, nextPosition.position.y);
				float z = (float) MathHelper.lerp(partialTicks, position.position.z, nextPosition.position.z);
				positions.set(i, new EntityHelper.PastPosition(new Vec3d(x, y, z), position.time));
			}
		}
		float x = (float) MathHelper.lerp(partialTicks, livingEntity.prevX, livingEntity.getX());
		float y = (float) MathHelper.lerp(partialTicks, livingEntity.prevY, livingEntity.getY());
		float z = (float) MathHelper.lerp(partialTicks, livingEntity.prevZ, livingEntity.getZ());
		if (positions.size() > 1) {
			positions.set(positions.size() - 1, new EntityHelper.PastPosition(new Vec3d(x, y, z).add(livingEntity.getVelocity().multiply(partialTicks, partialTicks, partialTicks)), 0));
		}
		List<Vector4f> mappedPastPositions = positions.stream().map(p -> p.position).map(p -> new Vector4f((float) p.x, (float) p.y, (float) p.z, 1)).collect(Collectors.toList());

		Color color = new Color(219, 88, 239);
		VFXBuilders.WorldVFXBuilder trailBuilder = VFXBuilders.createWorld().setPosColorTexLightmapDefaultFormat().setColor(color).setOffset(-x, -y, -z);
		VFXBuilders.WorldVFXBuilder builder = VFXBuilders.createWorld().setPosColorTexLightmapDefaultFormat().setColor(color);
		matrixStack.push();
		RenderSystem.enableBlend();
		trailBuilder.renderTrail(DELAYED_RENDER.getBuffer(STAR_TRAIL_TYPE), matrixStack, mappedPastPositions, f -> 0.25f, f -> trailBuilder.setAlpha(Math.max(0, Easing.SINE_IN.ease(f, 0, 0.5f, 1))));
		trailBuilder.renderTrail(DELAYED_RENDER.getBuffer(STAR_TRAIL_TYPE), matrixStack, mappedPastPositions, f -> 0.1f, f -> trailBuilder.setAlpha(Math.max(0, Easing.SINE_IN.ease(f, 0, 0.75f, 1))));

		RenderSystem.disableBlend();
		matrixStack.pop();
	}

}
