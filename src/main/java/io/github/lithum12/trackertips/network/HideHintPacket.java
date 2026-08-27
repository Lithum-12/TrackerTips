package io.github.lithum12.trackertips.network;

import io.github.lithum12.trackertips.client.ClientHintManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HideHintPacket(ResourceLocation id) {

    public static HideHintPacket decode(FriendlyByteBuf buf) {
        return new HideHintPacket(buf.readResourceLocation());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
    }

    // 【新增】处理网络包的方法，解决找不到 handle 的编译错误
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // 【安全机制】使用 DistExecutor 确保只在客户端执行，防止服务端崩溃
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHintManager.hide(id));
        });
        ctx.setPacketHandled(true);
    }
}