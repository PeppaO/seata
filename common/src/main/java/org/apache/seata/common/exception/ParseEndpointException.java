package org.apache.seata.common.exception;

public class ParseEndpointException extends RuntimeException {
    public ParseEndpointException() {
    }

    public ParseEndpointException(String message) {
        super(message);
    }

    public ParseEndpointException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseEndpointException(Throwable cause) {
        super(cause);
    }

    public ParseEndpointException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
