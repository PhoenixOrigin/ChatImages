package net.phoenix.imagerenderer.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.phoenix.imagerenderer.ImageRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Unique
    private static String toString(OrderedText orderedText) {
        StringBuilder builder = new StringBuilder();

        orderedText.accept((index, style, codePoint) -> {
            builder.append(Character.toChars(codePoint));
            return true;
        });

        return builder.toString();
    }

    @Inject(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I", at = @At("RETURN"), cancellable = true)
    public void render(TextRenderer textRenderer, OrderedText text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {

        String raw = toString(text);
        if (raw.startsWith("[pictureimg]")) {
            try {
                String[] split = raw.split("]");
                int id = Integer.parseInt(split[2].replace("[", ""));
                ImageRenderer.ID d = ImageRenderer.imageCache.get(id);
                ImageRenderer.images.put(d.identifier, new ImageRenderer.Image(x, y, d.width, d.height));

                cir.setReturnValue(20);
            } catch (Exception ignored) {

            }
        }
    }

}