package vn.autobuilder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import vn.autobuilder.builder.BuilderEngine;
import vn.autobuilder.gui.BuilderScreen;

/**
 * Client: dang ky phim tat K + chay engine moi tick.
 * Kiem tra 2 thu nay neu khong hien trong Controls:
 *   1) KeyBinding.Category.create(...) co the phai doi thanh KeyBinding.Category.MISC
 *   2) hoac constructor cu: new KeyBinding(name, type, key, "key.category.autobuilder.main")
 */
public class AutoBuilderClient implements ClientModInitializer {

    public static final Identifier CATEGORY_ID = Identifier.of(AutoBuilderMod.MOD_ID, "main");
    public static KeyBinding OPEN_KEY;

    @Override
    public void onInitializeClient() {
        OPEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autobuilder.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyBinding.Category.create(CATEGORY_ID)
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_KEY.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new BuilderScreen(null));
                }
            }
            BuilderEngine.get().tick(client);
        });

        AutoBuilderMod.LOGGER.info("[AutoBuilder] Client san sang. Phim tat: K");
    }
}
