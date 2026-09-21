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

    private Licence(boolean valid, String reason, String holder, String plan,
                    String keyId, long issued, long expires) {
        this.valid = valid;
        this.reason = reason;
        this.holder = holder;
        this.plan = plan;
        this.keyId = keyId;
        this.issued = issued;
        this.expires = expires;
    }

    static Licence valid(String holder, String plan, String keyId, long issued, long expires) {
        return new Licence(true, "", holder, plan, keyId, issued, expires);
    }

    static Licence invalid(String reason) {
        return new Licence(false, reason, "-", "-", "-", 0, 0);
    }

    /** The state a build with no public key runs in: licensing is simply off. */
    static Licence unenforced() {
        return new Licence(true, "no licence key is enforced in this build",
                "this machine", "self hosted", "-", 0, 0);
    }

    public boolean valid()  { return valid; }
    public String reason()  { return reason; }
    public String holder()  { return holder; }
    public String plan()    { return plan; }
    public String keyId()   { return keyId; }
    public long issued()    { return issued; }
    public long expires()   { return expires; }

    /** False for a key that never runs out. */
    public boolean hasExpiry() { return expires > 0; }

    public String expiryText() {
        if (expires <= 0) return "never";
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(expires));
    }

    /** One line for the menu and the log. */
    public String statusLine() {
        if (!valid) return reason;
        if (expires <= 0) return "key " + keyId + ", no expiry";
        long days = (expires - System.currentTimeMillis()) / 86400000L;
        return "key " + keyId + ", " + Math.max(0, days) + " days left";
    }

    @Override public String toString() {
        return (valid ? "valid" : "invalid") + " licence (" + statusLine() + ")";
    }
}
