package cls.motu.exceptions;

/**
 * Exception raised when a call to motu returns an error HTTP status code.
 */
public class MotuHttpStatusCodeException extends MotuException {
    private final int statusCode;

    /**
     * Constructs a new exception with {@code null} as its detail message. The cause is not initialized, and
     * may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param statusCode Status code of the response.
     */
    public MotuHttpStatusCodeException(final int statusCode) {
        super("Non-success http status code '" + statusCode + "' returned by remote motu server");
        this.statusCode = statusCode;
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message    the detail message. The detail message is saved for later retrieval by the
     *                   {@link #getMessage()} method.
     * @param statusCode Status code of the response.
     */
    public MotuHttpStatusCodeException(final String message, final int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param message    the detail message (which is saved for later retrieval by the {@link #getMessage()}
     *                   method).
     * @param cause      the cause (which is saved for later retrieval by the {@link #getCause()} method). (A
     *                   <tt>null</tt> value is permitted, and
     * @param statusCode Status code of the response.
     */
    public MotuHttpStatusCodeException(final String message, final Throwable cause, final int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Constructs a new exception with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message
     * of <tt>cause</tt>). This constructor is useful for exceptions that are little more than wrappers for
     * other throwables (for example, {@link java.security.PrivilegedActionException}).
     *
     * @param cause      the cause (which is saved for later retrieval by the {@link #getCause()} method). (A
     *                   <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @param statusCode Status code of the response.
     * @since 1.4
     */
    public MotuHttpStatusCodeException(final Throwable cause, final int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    /**
     * @return HTTP status code returned by the server.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
