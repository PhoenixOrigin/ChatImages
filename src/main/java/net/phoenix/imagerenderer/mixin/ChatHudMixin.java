package net.phoenix.imagerenderer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.phoenix.imagerenderer.ImageRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Unique
    Pattern urlPattern = Pattern.compile("\\b((http|https|ftp):\\/\\/\\S*)");

    @Shadow
    public abstract void addMessage(Text message);

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At("HEAD"), argsOnly = true)
    private Text replaceUrlsInIncomingMessageWithSpecialLinkObjects(Text value) {
        return processMessage(value);
    }

    @Unique
    private Text processMessage(Text message) {
        String raw = message.getString();
        String clean = raw.replaceAll("(§[0-9a-fklmnor])", "{$1}");
        int index = clean.indexOf(">");
        if (index == -1) index = clean.indexOf(":");
        String author = clean.substring(0, index - 1).replace("<", "").replace(":", "");
        Matcher matcher = urlPattern.matcher(clean);
        if (matcher.find()) {
            String url = matcher.group(1);
            try {
                URL temp = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) temp.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                InputStream inputStream = connection.getInputStream();

                BufferedImage inp = ImageIO.read(inputStream);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(inp, "PNG", outputStream);
                inputStream = new ByteArrayInputStream(outputStream.toByteArray());

                NativeImage image = NativeImage.read(inputStream);

                GameOptions options = MinecraftClient.getInstance().options;

                Identifier e = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("image", new NativeImageBackedTexture(image));
                int current = ImageRenderer.current;

                double chatWidth = options.getChatWidth().getValue();
                double chatHeight = options.getChatHeightFocused().getValue();
                Window window = MinecraftClient.getInstance().getWindow();
                int windowWidth = window.getScaledWidth();
                int windowHeight = window.getScaledHeight();

                int chatWidthInPixels = (int) (windowWidth * chatWidth);
                int chatHeightInPixels = (int) (windowHeight * chatHeight);

                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                int lineHeight = textRenderer.fontHeight;

                int maxLines = chatHeightInPixels / lineHeight;

                int rawWidth = image.getWidth();
                int rawHeight = image.getHeight();

                double widthScale = (double) chatWidthInPixels / rawWidth;
                double heightScale = (double) maxLines / (double) (rawHeight / lineHeight); // Scale height based on lines, not pixels

                double scale = Math.min(widthScale, heightScale);

                int newWidth = (int) (rawWidth * scale);
                int newHeight = (int) (rawHeight * scale);
                ImageRenderer.imageCache.put(current, new ImageRenderer.ID(e, newWidth, newHeight));
                ImageRenderer.current++;

                int scaledHeightInLines = (int) Math.ceil((double) newHeight / lineHeight);
                return Text.of(clean.substring(0, index + 1) + "\n[pictureimg][start][" + current + "]" + new String(new char[scaledHeightInLines]).replace("\0", "\n") + "[pictureimg][end][" + current + "]");
            } catch (IOException e) {
                System.out.println("e");
                e.printStackTrace();
            }

        }
        return message;
    }


}
