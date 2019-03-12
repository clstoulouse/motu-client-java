package cls.atoll.motu.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@ConfigurationProperties("motu")
@PropertySource("classpath:/motu.properties")
@Component
public class MotuProperties {
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 10;
    private String casLoginFormSelector = "#authentification";

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(final int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(final int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public String getCasLoginFormSelector() {
        return casLoginFormSelector;
    }

    public void setCasLoginFormSelector(final String casLoginFormSelector) {
        this.casLoginFormSelector = casLoginFormSelector;
    }
}
