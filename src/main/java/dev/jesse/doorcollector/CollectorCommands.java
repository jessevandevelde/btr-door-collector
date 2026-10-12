package dev.jesse.doorcollector;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.jesse.doorcollector.config.ConfigManager;
import dev.jesse.doorcollector.model.Direction;
import dev.jesse.doorcollector.model.Rotation;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CollectorCommands {
    private CollectorCommands() {}

    public static void init(CollectorState state) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(ClientCommands.literal("doorcollector")
            .executes(ctx -> reply(ctx, state.status()))
            .then(ClientCommands.literal("begin")
                .then(ClientCommands.argument("rotation", StringArgumentType.word())
                    .then(ClientCommands.argument("room", StringArgumentType.greedyString()).executes(ctx -> {
                        try { return reply(ctx, state.begin(Minecraft.getInstance(), Rotation.parse(StringArgumentType.getString(ctx, "rotation").toLowerCase()), StringArgumentType.getString(ctx, "room"))); }
                        catch (IllegalArgumentException e) { return reply(ctx, e.getMessage()); }
                    }))))
            .then(ClientCommands.literal("size")
                .then(ClientCommands.argument("width", IntegerArgumentType.integer(1, 6))
                    .then(ClientCommands.argument("length", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> reply(ctx, state.setSize(IntegerArgumentType.getInteger(ctx, "width"), IntegerArgumentType.getInteger(ctx, "length")))))))
            .then(ClientCommands.literal("origin").executes(ctx -> reply(ctx, state.setOrigin(Minecraft.getInstance()))))
            .then(ClientCommands.literal("floor").executes(ctx -> reply(ctx, state.setFloor(Minecraft.getInstance()))))
            .then(ClientCommands.literal("originy").then(ClientCommands.argument("y", IntegerArgumentType.integer(-64, 320))
                .executes(ctx -> reply(ctx, state.setOriginY(IntegerArgumentType.getInteger(ctx, "y"))))))
            .then(ClientCommands.literal("shape")
                .then(ClientCommands.literal("rectangle").executes(ctx -> reply(ctx, state.rectangleShape())))
                .then(ClientCommands.literal("clear").executes(ctx -> reply(ctx, state.clearShape()))))
            .then(ClientCommands.literal("cell")
                .then(ClientCommands.literal("add").executes(ctx -> reply(ctx, state.changeCurrentCell(Minecraft.getInstance(), true))))
                .then(ClientCommands.literal("remove").executes(ctx -> reply(ctx, state.changeCurrentCell(Minecraft.getInstance(), false)))))
            .then(ClientCommands.literal("ready").executes(ctx -> reply(ctx, state.ready(Minecraft.getInstance()))))
            .then(ClientCommands.literal("mark").executes(ctx -> reply(ctx, state.mark(Minecraft.getInstance(), null)))
                .then(ClientCommands.argument("direction", StringArgumentType.word()).executes(ctx -> {
                    try { return reply(ctx, state.mark(Minecraft.getInstance(), Direction.parse(StringArgumentType.getString(ctx, "direction")))); }
                    catch (IllegalArgumentException e) { return reply(ctx, "direction must be north, east, south, or west"); }
                })))
            .then(ClientCommands.literal("auto").executes(ctx -> {
                ConfigManager.get().automaticCapture = !ConfigManager.get().automaticCapture;
                ConfigManager.save();
                return reply(ctx, "automatic capture " + (ConfigManager.get().automaticCapture ? "enabled" : "disabled"));
            }))
            .then(ClientCommands.literal("export").executes(ctx -> reply(ctx, state.export())))
            .then(ClientCommands.literal("status").executes(ctx -> reply(ctx, state.status())))
            .then(ClientCommands.literal("stop").executes(ctx -> reply(ctx, state.stop())))
            .then(ClientCommands.literal("clear").executes(ctx -> reply(ctx, state.clear())))
            .then(ClientCommands.literal("help").executes(ctx -> reply(ctx,
                "begin <rotation> <room>; size <w> <l>; origin; floor; shape <rectangle|clear>; cell <add|remove>; ready; mark [direction]; export; stop")))));
    }

    private static int reply(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Component.literal("[Door Collector] " + message));
        return 1;
    }
}
