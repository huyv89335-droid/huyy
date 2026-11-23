package vn.autobuilder.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Gui thong bao ra chat + thanh action bar (khong render them gi ca).
 */
public final class ChatUtil {

    private ChatUtil() {
    }

    public static void say(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("\u00A7a[AutoBuilder]\u00A7r " + message), false);
        }
    }

    public static void error(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("\u00A7c[AutoBuilder]\u00A7r " + message), false);
        }
    }

    /** Action bar (phia duoi tam ngam) - rat nhe, thay cho HUD render. */
    public static void hud(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(message), true);
        }
    }
}
