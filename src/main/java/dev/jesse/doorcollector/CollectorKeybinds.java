package dev.jesse.doorcollector;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class CollectorKeybinds {
    private CollectorKeybinds() {}

    public static void init(CollectorState state) {
        KeyMapping mark = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.better_routes_door_collector.mark", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (mark.consumeClick()) {
                String result = state.mark(client, null);
                client.gui.setOverlayMessage(Component.literal("Door Collector: " + result), false);
            }
        });
    }
}
