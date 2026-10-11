package dev.jesse.doorcollector;

import dev.jesse.doorcollector.config.ConfigManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class CollectorHud {
    private final CollectorState state;
    public CollectorHud(CollectorState state) { this.state = state; }

    public void init() { HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("better_routes_door_collector", "status"), this::render); }

    private void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!ConfigManager.get().showHud || !state.active()) return;
        Minecraft client = Minecraft.getInstance();
        graphics.text(client.font, "Door Collector: " + state.roomId(), 6, 6, 0xFF55FF55, true);
        graphics.text(client.font, state.lastMessage(), 6, 17, 0xFFFFFFFF, true);
    }
}
