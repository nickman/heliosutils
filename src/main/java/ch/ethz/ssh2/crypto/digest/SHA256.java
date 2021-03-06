/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.crypto.digest;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @version $Id: SHA256.java 152 2014-04-28 11:02:23Z dkocher@sudo.ch $
 */
public final class SHA256 implements Digest {

    private MessageDigest md;

    public SHA256() {
        try {
            md = MessageDigest.getInstance("SHA-256");
        }
        catch(NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public final int getDigestLength() {
        return md.getDigestLength();
    }

    public final void reset() {
        md.reset();
    }

    public final void update(byte b[]) {
        this.update(b, 0, b.length);
    }

    public final void update(byte b[], int off, int len) {
        md.update(b, off, len);
    }

    public final void update(byte b) {
        md.update(b);
    }

    public final void digest(byte[] out) {
        this.digest(out, 0);
    }

    public final void digest(byte[] out, int off)  {
    	try {
    		md.digest(out, off, out.length);
    	} catch (Exception ex) {
    		throw new RuntimeException(ex);
    	}
    }
}
