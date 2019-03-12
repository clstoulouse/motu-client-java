package cls.atoll.motu.utils;

import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkHttpLogger implements HttpLoggingInterceptor.Logger {
    private final Logger log;

    public OkHttpLogger() {
        this(LoggerFactory.getLogger(OkHttpLogger.class));
    }

    public OkHttpLogger(final Logger log) {
        this.log = log;
    }

    @Override
    public void log(final String s) {
        log.debug(s);
    }
}
