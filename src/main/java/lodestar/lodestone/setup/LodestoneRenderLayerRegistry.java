package lodestar.lodestone.setup;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormats;
import ladysnake.effective.mixin.RenderLayerAccessor;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

public class LodestoneRenderLayerRegistry extends RenderPhase {
	public LodestoneRenderLayerRegistry(String p_110161_, Runnable p_110162_, Runnable p_110163_) {
		super(p_110161_, p_110162_, p_110163_);
	}

	public static final RenderLayerProvider ADDITIVE_TEXTURE_TRIANGLE = new RenderLayerProvider((texture) -> createGenericRenderLayer(texture.getNamespace(), "additive_texture_triangle", VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, VertexFormat.DrawMode.QUADS, RenderPhase.ENTITY_TRANSLUCENT_EMISSIVE_SHADER, RenderPhase.ADDITIVE_TRANSPARENCY, texture));

	/**
	 * Creates a custom render type with a texture.
	 */
	private static RenderLayer createGenericRenderLayer(String modId, String name, VertexFormat format, VertexFormat.DrawMode mode, Shader shader, Transparency transparency, Identifier texture) {
		return createGenericRenderLayer(modId + ":" + name, format, mode, shader, transparency, new RenderPhase.Texture(texture, false, false));
	}

	public static RenderLayer createGenericRenderLayer(String modId, String name, VertexFormat format, VertexFormat.DrawMode mode, RenderPhase.Shader shader, RenderPhase.Transparency transparency, RenderPhase.TextureBase texture) {
		return createGenericRenderLayer(modId + ":" + name, format, mode, shader, transparency, texture);
	}

	/**
	 * Creates a custom render type with an empty texture.
	 */
	public static RenderLayer createGenericRenderLayer(String modId, String name, VertexFormat format, VertexFormat.DrawMode mode, RenderPhase.Shader shader, RenderPhase.Transparency transparency) {
		return createGenericRenderLayer(modId + ":" + name, format, mode, shader, transparency, RenderPhase.NO_TEXTURE);
	}

	/**
	 * Creates a custom render type and creates a buffer builder for it.
	 */
	public static RenderLayer createGenericRenderLayer(String name, VertexFormat format, VertexFormat.DrawMode mode, RenderPhase.Shader shader, RenderPhase.Transparency transparency, RenderPhase.TextureBase texture) {
		RenderLayer type = RenderLayerAccessor.invokeOf(
				name, format, mode, 256, false, false, RenderLayer.MultiPhaseParameters.builder()
						.shader(shader)
						.writeMaskState(new WriteMaskState(true, true))
						.lightmap(new RenderPhase.Lightmap(false))
						.transparency(transparency)
						.texture(texture)
						.cull(new RenderPhase.Cull(true))
						.build(true)
		);
		return type;
	}

	public static class RenderLayerProvider {
		private final Function<Identifier, RenderLayer> function;
		private final Function<Identifier, RenderLayer> memorizedFunction;

		public RenderLayerProvider(Function<Identifier, RenderLayer> function) {
			this.function = function;
			this.memorizedFunction = Util.memoize(function);
		}

		public RenderLayer apply(Identifier texture) {
			return function.apply(texture);
		}

		public RenderLayer applyAndCache(Identifier texture) {
			return this.memorizedFunction.apply(texture);
		}
	}
}
