package xyz.stormclient.licenceserver;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import xyz.stormclient.util.Json;

/**
 * Verifies and reads Stripe's webhook calls.
 *
 * <p>Anyone can POST to a webhook URL, so the body only counts once its
 * signature checks out. Stripe sends a {@code Stripe-Signature} header holding
 * a timestamp and one or more HMAC-SHA256 digests of
 * {@code <timestamp>.<raw body>}; we recompute it with the endpoint secret and
 * compare in constant time. The timestamp is checked too, otherwise a captured
 * request could be replayed forever.
 */
public final class StripeHook {

    /** How far out of date a signed request may be. */
    private static final long TOLERANCE_SECONDS = 300;

    private StripeHook() { }

    public static boolean verify(String header, byte[] body, String secret) {
        if (header == null || secret == null || secret.isEmpty()) return false;

        String timestamp = null;
        boolean matched = false;
        for (String part : header.split(",")) {
            int equals = part.indexOf('=');
            if (equals <= 0) continue;
            String name = part.substring(0, equals).trim();
            String value = part.substring(equals + 1).trim();
            if ("t".equals(name)) timestamp = value;
        }
        if (timestamp == null) return false;

        long sent;
        try { sent = Long.parseLong(timestamp); } catch (NumberFormatException e) { return false; }
        if (Math.abs(System.currentTimeMillis() / 1000 - sent) > TOLERANCE_SECONDS) return false;

        byte[] expected = hmac(secret, (timestamp + ".").getBytes(StandardCharsets.UTF_8), body);
        for (String part : header.split(",")) {
            int equals = part.indexOf('=');
            if (equals <= 0) continue;
            if (!"v1".equals(part.substring(0, equals).trim())) continue;
            byte[] candidate = hex(part.substring(equals + 1).trim());
            if (candidate != null && MessageDigest.isEqual(expected, candidate)) matched = true;
        }
        return matched;
    }

    private static byte[] hmac(String secret, byte[] prefix, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(prefix);
            mac.update(body);
            return mac.doFinal();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 is missing from this JVM", e);
        }
    }

    private static byte[] hex(String value) {
        if (value.length() % 2 != 0) return null;
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) return null;
            out[i] = (byte) ((high << 4) | low);
        }
        return out;
    }

    /** What the server needs out of a checkout event. */
    public static final class Checkout {
        public final String sessionId;
        public final String email;
        public final String plan;

        Checkout(String sessionId, String email, String plan) {
            this.sessionId = sessionId;
            this.email = email;
            this.plan = plan;
        }
    }

    /**
     * Pulls the session out of a {@code checkout.session.completed} event, or
     * returns null for any other event so the server can answer 200 and move on.
     */
    @SuppressWarnings("unchecked")
    public static Checkout checkout(byte[] body) {
        Map<String, Object> root = Json.readObject(new String(body, StandardCharsets.UTF_8));
        if (!"checkout.session.completed".equals(String.valueOf(root.get("type")))) return null;

        Object data = root.get("data");
        if (!(data instanceof Map)) return null;
        Object object = ((Map<String, Object>) data).get("object");
        if (!(object instanceof Map)) return null;
        Map<String, Object> session = (Map<String, Object>) object;

        String id = text(session.get("id"));
        if (id.isEmpty()) return null;

        String email = text(session.get("customer_email"));
        if (email.isEmpty()) {
            Object details = session.get("customer_details");
            if (details instanceof Map) email = text(((Map<String, Object>) details).get("email"));
        }

        // set a "plan" key in the Checkout Session metadata to pick the plan
        String plan = "";
        Object metadata = session.get("metadata");
        if (metadata instanceof Map) plan = text(((Map<String, Object>) metadata).get("plan"));

        return new Checkout(id, email, plan);
    }

    private static String text(Object value) {
        return value == null || "null".equals(String.valueOf(value)) ? "" : String.valueOf(value).trim();
    }
}
