package cls.atoll.motu.objs;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableMotuProductReference.class)
@JsonDeserialize(as = ImmutableMotuProductReference.class)
public interface MotuProductReference {
    String getService();

    String getProduct();
}
