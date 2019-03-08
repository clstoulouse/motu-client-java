package cls.motu.objs;

import java.time.Instant;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableProductProduction.class)
@JsonDeserialize(as = ImmutableProductProduction.class)
public interface ProductProduction {

    @Nullable
    Instant getBeginDate();

    @Nullable
    Instant getEndDate();

    @Nullable
    String getCenter();
}
