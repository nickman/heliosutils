package ch.ethz.ssh2;

import java.io.IOException;

/**
 * @version $Id: PacketTypeException.java 99 2014-04-10 09:04:31Z dkocher@sudo.ch $
 */
public class PacketTypeException extends IOException {

    public PacketTypeException() {
    }

    public PacketTypeException(final String message) {
        super(message);
    }

    public PacketTypeException(final int packet) {
        super(String.format("The SFTP server sent an unexpected packet type (%d)", packet));
    }
}
