package xyz.stormclient.licenceserver;

import java.security.SecureRandom;

/**
 * The key a customer actually gets: {@code STORM-4K7M-9QX2-JH3D}.
 *
 * <p>Short enough to paste from an email, and never the thing the client
 * trusts. It is a bearer token the server swaps for a signed licence, so its
 * only jobs are to be unguessable and to survive being read out loud.
 *
 * <p>The alphabet leaves out the characters people confuse for each other
 * (I, L, O, U, 0, 1), and the last character is a checksum, so a typo is
 * rejected before it ever reaches the store.
 */
public final class ShortKey {

    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "STORM";
    private static final int BODY = 12;          // characters, checksum included

    private ShortKey() { }

    /** A fresh key. 11 random characters out of 30 is about 54 bits. */
    public static String generate() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < BODY - 1; i++) {
            body.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        body.append(checksum(body.toString()));
        return format(body.toString());
    }

    /** Groups the body into {@code STORM-XXXX-XXXX-XXXX}. */
    private static String format(String body) {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < body.length(); i += 4) {
            sb.append('-').append(body, i, Math.min(body.length(), i + 4));
        }
        return sb.toString();
    }

    /**
     * Accepts whatever the customer pasted and returns the canonical form, or
     * null when it is not a key at all. Case, spaces and missing dashes are
     * all forgiven; a wrong checksum is not.
     */
    public static String normalise(String raw) {
        if (raw == null) return null;
        String cleaned = raw.toUpperCase().replaceAll("[^0-9A-Z]", "");
        if (cleaned.startsWith(PREFIX)) cleaned = cleaned.substring(PREFIX.length());
        if (cleaned.length() != BODY) return null;
        for (int i = 0; i < cleaned.length(); i++) {
            if (ALPHABET.indexOf(cleaned.charAt(i)) < 0) return null;
        }
        if (cleaned.charAt(BODY - 1) != checksum(cleaned.substring(0, BODY - 1))) return null;
        return format(cleaned);
    }

    private static char checksum(String body) {
        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            sum = sum * 31 + ALPHABET.indexOf(body.charAt(i)) + 7;
        }
        return ALPHABET.charAt(Math.floorMod(sum, ALPHABET.length()));
    }
}
