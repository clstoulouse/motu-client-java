package cls.motu.objs;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableMotuDownloadProductParameters.class)
@JsonDeserialize(as = ImmutableMotuDownloadProductParameters.class)
public interface MotuDownloadProductParameters {
    List<String> getVariables();

    @Nullable
    Instant getLowestTime();

    @Nullable
    Instant getHighestTime();

    @Value.Default
    default double getLowestLatitude() {
        return -90D;
    }

    @Value.Default
    default double getHighestLatitude() {
        return 90D;
    }

    @Value.Default
    default double getLowestLongitude() {
        return -180D;
    }

    @Value.Default
    default double getHighestLongitude() {
        return 180D;
    }

    @Value.Default
    default double getLowestDepth() {
        return 0D;
    }

    @Value.Default
    default double getHighestDepth() {
        return 180D;
    }

    static MotuDownloadProductParameters of(@Nullable
    final Instant lowestTime, @Nullable
    final Instant highestTime) {
        return ImmutableMotuDownloadProductParameters.builder().lowestTime(lowestTime).highestTime(highestTime).build();
    }
}
