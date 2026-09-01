package io.github.lithum12.trackertips.network;

import io.github.lithum12.trackertips.TrackerTips;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class TTNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TrackerTips.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        // Existing ShowHintPacket registration
        CHANNEL.registerMessage(0, ShowHintPacket.class,
                ShowHintPacket::encode, ShowHintPacket::decode, ShowHintPacket::handle);

        // Added: registers HideHintPacket, using ID 2 (avoiding 0)
        CHANNEL.registerMessage(2, HideHintPacket.class,
                HideHintPacket::encode, HideHintPacket::decode, HideHintPacket::handle);
    }

    // Original send method (ShowHintPacket only)
    public static void sendToPlayer(ServerPlayer player, ShowHintPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // Optional: a convenience method for sending HideHintPacket
    public static void sendToPlayer(ServerPlayer player, HideHintPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}