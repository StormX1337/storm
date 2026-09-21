package xyz.stormclient.licence;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks a licence blob against the public key baked into this build.
 *
 * <p>A blob looks like {@code STORM1.<payload>.<signature>}, both parts base64
 * url encoded. The payload is a flat {@code key=value;key=value} list, and the
 * signature covers exactly those payload bytes, so changing a single character
 * of it invalidates the licence.
 *
 * <p>{@code expires} is when this blob stops being accepted and {@code until}
 * is when the purchase behind it ends. A licence server signs short lived
 * blobs so a refund or a chargeback takes effect within days, while the
 * customer still sees their real end date.
 *
 * <h2>What this does and does not buy you</h2>
 * The signature cannot be forged without the private key, so nobody can mint
 * themselves a licence. It is still a check that runs on the user's own
 * machine, in a jar they can edit, so a determined user can always remove it.
 * Treat it as a way to hand out and expire keys, not as protection: anything
 * that really must not be copied has to live on a server you run.
 */
public final class LicenceVerifier {

    /**
     * The X.509 public key of whoever signs licences for this build, base64
     * encoded and with no PEM header.
     *
     * <p>An empty value means this build does not enforce licences at all;
     * {@code tools/licence/LicenceTool.java genkey} prints the value to paste
     * in here. Never put the private key in the client.
     */
    public static final String PUBLIC_KEY = "";

    private static final String PREFIX = "STORM1.";

    private LicenceVerifier() { }

    /** True when this build was compiled with a signing key to check against. */
    public static boolean enforced() { return !PUBLIC_KEY.isEmpty(); }

    /**
     * Turns a blob into a licence. A build with no public key always returns an
     * unenforced licence, so a self hosted copy is never locked out of itself.
     */
    public static Licence verify(String blob) {
        if (!enforced()) return Licence.unenforced();
        if (blob == null || blob.isEmpty()) return Licence.invalid("no licence key was supplied");
        if (!blob.startsWith(PREFIX)) return Licence.invalid("licence key is not in Storm's format");

        String body = blob.substring(PREFIX.length());
        int dot = body.indexOf('.');
        if (dot <= 0 || dot == body.length() - 1) {
            return Licence.invalid("licence key is truncated");
        }

        byte[] payload;
        byte[] signature;
        try {
            payload = Base64.getUrlDecoder().decode(body.substring(0, dot));
            signature = Base64.getUrlDecoder().decode(body.substring(dot + 1));
        } catch (IllegalArgumentException e) {
            return Licence.invalid("licence key is damaged");
        }

        if (!signatureMatches(payload, signature)) {
            return Licence.invalid("licence key was not issued for this build");
        }

        Map<String, String> fields = parse(new String(payload, java.nio.charset.Charset.forName("UTF-8")));
        long expires = number(fields.get("expires"));
        if (expires > 0 && expires < System.currentTimeMillis()) {
            return Licence.invalid("licence expired on "
                    + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(expires)));
        }

        String machine = fields.get("machine");
        if (machine != null && !machine.isEmpty() && !machine.equals(MachineId.get())) {
            return Licence.invalid("licence key belongs to a different machine");
        }

        return Licence.valid(
                value(fields.get("holder"), "unknown"),
                value(fields.get("plan"), "standard"),
                value(fields.get("id"), "-"),
                number(fields.get("issued")),
                expires,
                number(fields.get("until")));
    }

    private static boolean signatureMatches(byte[] payload, byte[] signature) {
        try {
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY)));
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(key);
            rsa.update(payload);
            return rsa.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private static Map<String, String> parse(String payload) {
        Map<String, String> fields = new HashMap<String, String>();
        for (String pair : payload.split(";")) {
            int equals = pair.indexOf('=');
            if (equals <= 0) continue;
            fields.put(pair.substring(0, equals).trim(), pair.substring(equals + 1).trim());
        }
        return fields;
    }

    private static long number(String raw) {
        if (raw == null) return 0;
        try { return Long.parseLong(raw.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static String value(String raw, String fallback) {
        return raw == null || raw.isEmpty() ? fallback : raw;
    }
}
