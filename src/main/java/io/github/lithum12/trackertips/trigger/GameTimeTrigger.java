package io.github.lithum12.trackertips.trigger;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

import java.util.Locale;

public class GameTimeTrigger implements IHintTrigger {

    public enum Mode { AFTER, BEFORE, RANGE }

    private final Mode mode;
    private final long time;
    private final long endTime;

    public GameTimeTrigger(Mode mode, long time, long endTime) {
        this.mode = mode;
        this.time = time;
        this.endTime = endTime;
    }

    public static GameTimeTrigger fromJson(JsonObject json) {
        String modeStr = GsonHelper.getAsString(json, "mode", "after").toLowerCase(Locale.ROOT);
        long time = GsonHelper.getAsLong(json, "time", 0L);
        long endTime = GsonHelper.getAsLong(json, "end_time", 0L);

        Mode mode = switch (modeStr) {
            case "before" -> Mode.BEFORE;
            case "range" -> Mode.RANGE;
            default -> Mode.AFTER;
        };
        return new GameTimeTrigger(mode, time, endTime);
    }

    @Override
    public boolean test(ServerPlayer player) {
        long dayTime = player.level().getDayTime();
        return switch (mode) {
            case AFTER -> dayTime >= time;
            case BEFORE -> dayTime < time;
            case RANGE -> dayTime >= time && dayTime <= endTime;
        };
    }
}