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
        // 已有的 ShowHintPacket 注册
        CHANNEL.registerMessage(0, ShowHintPacket.class,
                ShowHintPacket::encode, ShowHintPacket::decode, ShowHintPacket::handle);

        // 新增：注册 HideHintPacket，ID 使用 2（避开 0）
        CHANNEL.registerMessage(2, HideHintPacket.class,
                HideHintPacket::encode, HideHintPacket::decode, HideHintPacket::handle);
    }

    // 原有的发送方法（只针对 ShowHintPacket）
    public static void sendToPlayer(ServerPlayer player, ShowHintPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // 可选：添加一个发送 HideHintPacket 的便捷方法
    public static void sendToPlayer(ServerPlayer player, HideHintPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}