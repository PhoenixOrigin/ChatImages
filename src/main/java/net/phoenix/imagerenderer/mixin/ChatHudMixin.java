package net.phoenix.imagerenderer.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.phoenix.imagerenderer.ImageRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
                NativeImage image = NativeImage.read(inputStream);
                Identifier e = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("image", new NativeImageBackedTexture(image));
                int current = ImageRenderer.current;
                ImageRenderer.imageCache.put(current, e);
                ImageRenderer.current++;
                return Text.of("[pictureimg][" + author + "][" + current + "]");
            } catch (IOException e) {
                System.out.println("e");
                e.printStackTrace();
            }

        }
        return message;
    }


}
