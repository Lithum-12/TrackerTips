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

    // Handles the packet; this method was added to fix a "cannot find handle" compile error.
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // Safety mechanism: uses DistExecutor to guarantee this only runs on the client, preventing a server crash.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHintManager.hide(id));
        });
        ctx.setPacketHandled(true);
    }
}