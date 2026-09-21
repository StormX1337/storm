package xyz.stormclient.licence;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * What Storm was started with.
 *
 * <p>A licence is only ever produced by {@link LicenceVerifier}, which means an
 * instance that reports {@link #valid()} has already had its signature checked
 * against the build's public key. Everything else on it is display data.
 */
public final class Licence {

    private final boolean valid;
    private final String reason;
    private final String holder;
    private final String plan;
    private final String keyId;
    private final long issued;
    private final long expires;
    private final long until;

    private Licence(boolean valid, String reason, String holder, String plan,
                    String keyId, long issued, long expires, long until) {
        this.valid = valid;
        this.reason = reason;
        this.holder = holder;
        this.plan = plan;
        this.keyId = keyId;
        this.issued = issued;
        this.expires = expires;
        this.until = until;
    }

    static Licence valid(String holder, String plan, String keyId,
                         long issued, long expires, long until) {
        return new Licence(true, "", holder, plan, keyId, issued, expires, until);
    }

    static Licence invalid(String reason) {
        return new Licence(false, reason, "-", "-", "-", 0, 0, 0);
    }

    /** The state a build with no public key runs in: licensing is simply off. */
    static Licence unenforced() {
        return new Licence(true, "no licence key is enforced in this build",
                "this machine", "self hosted", "-", 0, 0, 0);
    }

    public boolean valid()  { return valid; }
    public String reason()  { return reason; }
    public String holder()  { return holder; }
    public String plan()    { return plan; }
    public String keyId()   { return keyId; }
    public long issued()    { return issued; }
    /** When the client stops accepting this blob. May be a short lease. */
    public long expires()   { return expires; }

    /**
     * When the purchase itself runs out, which is what a customer cares about.
     * A licence server hands out short lived blobs so it can cut access off,
     * and puts the real end date here; zero means it never ends.
     */
    public long until()     { return until > 0 ? until : expires; }

    /** False for a key that never runs out. */
    public boolean hasExpiry() { return until() > 0; }

    public String expiryText() {
        long end = until();
        if (end <= 0) return "never";
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(end));
    }

    /** One line for the menu and the log. */
    public String statusLine() {
        if (!valid) return reason;
        long end = until();
        if (end <= 0) return "key " + keyId + ", no expiry";
        long days = (end - System.currentTimeMillis()) / 86400000L;
        return "key " + keyId + ", " + Math.max(0, days) + " days left";
    }

    @Override public String toString() {
        return (valid ? "valid" : "invalid") + " licence (" + statusLine() + ")";
    }
}
