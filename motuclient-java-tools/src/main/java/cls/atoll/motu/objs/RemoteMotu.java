package cls.atoll.motu.objs;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.net.URI;

@Value.Immutable
@Value.Style(jdkOnly = true, redactedMask = "***")
public interface RemoteMotu {
    @Value.Parameter
    URI getRootUri();

    @Value.Parameter
    @Nullable
    String getUsername();

    @Value.Redacted
    @Value.Parameter
    @Nullable
    String getPassword();
}
