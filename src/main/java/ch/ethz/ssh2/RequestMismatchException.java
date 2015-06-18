package ch.ethz.ssh2;

import java.io.IOException;

/**
 * @version $Id: RequestMismatchException.java 99 2014-04-10 09:04:31Z dkocher@sudo.ch $
 */
public class RequestMismatchException extends IOException {

    public RequestMismatchException() {
        super("The server sent an invalid id field.");
    }

    public RequestMismatchException(final String message) {
        super(message);
    }
}
