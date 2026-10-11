package dev.jesse.doorcollector;

import dev.jesse.doorcollector.config.ConfigManager;
import dev.jesse.doorcollector.data.DoorStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DoorCollectorClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("better-routes-door-collector");

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        CollectorState state = new CollectorState(new DoorStore(ConfigManager.root()));
        state.load();
        ClientTickEvents.END_CLIENT_TICK.register(state::tick);
        CollectorCommands.init(state);
        CollectorKeybinds.init(state);
        new CollectorHud(state).init();
        LOG.info("Better Routes Door Collector initialized; data stays local until manually shared");
    }
}
