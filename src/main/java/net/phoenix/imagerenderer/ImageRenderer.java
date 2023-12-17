package net.phoenix.imagerenderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

public class ImageRenderer implements ClientModInitializer {

    public static HashMap<Integer, ID> imageCache = new HashMap<>();
    public static int current = 0;
    public static HashMap<Identifier, Image> images = new HashMap<>();


    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            while (true) {
                try {
                    GlStateManager._disableDepthTest();
                    render(drawContext);
                    GlStateManager._enableDepthTest();
                    break;
                } catch (ConcurrentModificationException ignored) {
                }
            }
        });
    }

    private void render(DrawContext drawContext) {
        for (Map.Entry<Identifier, Image> entry : images.entrySet()) {
            int x = entry.getValue().x;
            int y = entry.getValue().y;
            int width = entry.getValue().width;
            int height = entry.getValue().height;

            MatrixStack matrixStack = drawContext.getMatrices();
            Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();

            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            buffer.vertex(positionMatrix, x, y, 0).color(1f, 1f, 1f, 1f).texture(0f, 0f).next();
            buffer.vertex(positionMatrix, x, y + height, 0).color(1f, 1f, 1f, 1f).texture(0f, 1f).next();
            buffer.vertex(positionMatrix, x + width, y + height, 0).color(1f, 1f, 1f, 1f).texture(1f, 1f).next();
            buffer.vertex(positionMatrix, x + width, y, 0).color(1f, 1f, 1f, 1f).texture(1f, 0f).next();

            RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
            RenderSystem.setShaderTexture(0, entry.getKey());
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            tessellator.draw();
            ImageRenderer.images.remove(entry.getKey());
        }
    }

    public static class Image {
        public int x;
        public int y;
        public int width;
        public int height;

        public Image(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static class ID {
        public Identifier identifier;
        public int width;
        public int height;

        public ID(Identifier identifier, int width, int height) {
            this.identifier = identifier;
            this.width = width;
            this.height = height;
        }
    }

}
