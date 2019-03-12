package cls.atoll.motu.objs;

import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableDatasetDepth.class)
@JsonDeserialize(as = ImmutableDatasetDepth.class)
public interface DatasetDepth {
    @Nullable
    Double getMin();

    @Nullable
    Double getMax();

    @Nullable
    String getCrs();

    @Nullable
    Integer getNbLevels();

    List<Double> getValues();
}
