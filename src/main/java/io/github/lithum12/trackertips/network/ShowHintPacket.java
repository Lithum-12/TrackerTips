package io.github.lithum12.trackertips.network;

import io.github.lithum12.trackertips.client.ClientHintManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ShowHintPacket {

    private final ResourceLocation id;
    private final String textJson;
    private final String titleJson;
    private final String icon;
    private final int duration;
    private final int priority;
    private final int accent;
    private final String sound;
    private final String theme;

    public ShowHintPacket(ResourceLocation id, String textJson, String titleJson, String icon,
                          int duration, int priority, int accent, String sound, String theme) {
        this.id = id;
        this.textJson = textJson;
        this.titleJson = titleJson;
        this.icon = icon;
        this.duration = duration;
        this.priority = priority;
        this.accent = accent;
        this.sound = sound;
        this.theme = theme == null || theme.isBlank() ? "trackertips:default" : theme;
    }

    public static void encode(ShowHintPacket packet, FriendlyByteBuf buf) {
        buf.writeResourceLocation(packet.id);
        buf.writeUtf(packet.textJson, 32767);
        buf.writeUtf(packet.titleJson, 32767);
        buf.writeUtf(packet.icon, 256);
        buf.writeVarInt(packet.duration);
        buf.writeVarInt(packet.priority);
        buf.writeVarInt(packet.accent);
        buf.writeUtf(packet.sound, 256);
        buf.writeUtf(packet.theme, 256);
    }

    public static ShowHintPacket decode(FriendlyByteBuf buf) {
        return new ShowHintPacket(
                buf.readResourceLocation(),
                buf.readUtf(32767),
                buf.readUtf(32767),
                buf.readUtf(256),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(256),
                buf.readUtf(256)
        );
    }

    public static void handle(ShowHintPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHintManager.show(packet);
            }
        });
        ctx.setPacketHandled(true);
    }

    public ResourceLocation id() { return id; }
    public String textJson() { return textJson; }
    public String titleJson() { return titleJson; }
    public String icon() { return icon; }
    public int duration() { return duration; }
    public int priority() { return priority; }
    public int accent() { return accent; }
    public String sound() { return sound; }
    public String theme() { return theme; }
}