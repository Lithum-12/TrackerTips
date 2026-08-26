package io.github.lithum12.trackertips.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerHintDataProvider implements ICapabilitySerializable<CompoundTag> {

    private final PlayerHintData data = new PlayerHintData();
    private final LazyOptional<PlayerHintData> optional = LazyOptional.of(() -> data);

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == TTCapabilities.HINT_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serialize();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserialize(nbt);
    }
}