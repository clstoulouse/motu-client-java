package cls.atoll.motu.objs;

import java.net.URI;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.cls.atoll.motu.api.message.xml.ErrorType;
import fr.cls.atoll.motu.api.message.xml.StatusModeType;

@Value.Immutable
@Value.Style(jdkOnly = true)
@JsonSerialize(as = ImmutableMotuRequestStatus.class)
@JsonDeserialize(as = ImmutableMotuRequestStatus.class)
public interface MotuRequestStatus {
    @Nullable
    String getRequestId();

    @Nullable
    String getMessage();

    @Nullable
    String getCode();

    @Nullable
    StatusModeType getStatusType();

    @Nullable
    Long getSize();

    @Nullable
    URI getRemoteUri();

    @Value.Derived
    @Nullable
    default Integer getErrorTypeAsInteger() {
        if (null == this.getCode()) {
            return null;
        }
        final String[] parts = this.getCode().split("-", 2);
        if (2 != parts.length) {
            return null;
        }
        final String strErrorCode = StringUtils.trim(parts[1]);
        try {
            return Integer.parseInt(strErrorCode);
        } catch (final NumberFormatException ex) {
            return null;
        }
    }

    @Value.Derived
    @Nullable
    default ErrorType getErrorType() {
        if (null == this.getErrorTypeAsInteger()) {
            return null;
        }

        try {
            return ErrorType.fromValue(this.getErrorTypeAsInteger());
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }
}
