package cls.motu.objs;

import java.net.URI;
import java.util.Objects;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableProductAttachment.class)
@JsonDeserialize(as = ImmutableProductAttachment.class)
public interface ProductAttachment {
    @Value.Parameter
    ProductAttachmentType getType();

    @Value.Parameter
    @Nullable
    ResourceProtocol getProtocol();

    @Value.Parameter
    URI getUrl();

    @Value.Parameter
    @Nullable
    String getDescription();

    default boolean matches(final ProductAttachment other) {
        return (null != other) && Objects.equals(this.getType(), other.getType()) && Objects.equals(this.getProtocol(), other.getProtocol())
                && Objects.equals(this.getUrl(), other.getUrl());

    }
}
