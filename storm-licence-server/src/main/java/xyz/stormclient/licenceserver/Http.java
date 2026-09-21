package xyz.stormclient.licenceserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import xyz.stormclient.util.Json;

/** The bits of request handling every endpoint repeats. */
public final class Http {

    /** Bodies above this are refused outright rather than buffered. */
    private static final int MAX_BODY = 64 * 1024;

    private Http() { }

    public static byte[] body(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
            if (out.size() > MAX_BODY) throw new IOException("request body too large");
        }
        return out.toByteArray();
    }

    public static Map<String, Object> json(HttpExchange exchange) throws IOException {
        byte[] raw = body(exchange);
        if (raw.length == 0) return new LinkedHashMap<String, Object>();
        return Json.readObject(new String(raw, StandardCharsets.UTF_8));
    }

    public static String string(Map<String, Object> map, String name) {
        Object value = map.get(name);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static long number(Map<String, Object> map, String name, long fallback) {
        Object value = map.get(name);
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(String.valueOf(value).trim()); }
        catch (Exception e) { return fallback; }
    }

    public static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new HashMap<String, String>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null) return result;
        for (String pair : raw.split("&")) {
            int equals = pair.indexOf('=');
            if (equals <= 0) continue;
            try {
                result.put(java.net.URLDecoder.decode(pair.substring(0, equals), "UTF-8"),
                           java.net.URLDecoder.decode(pair.substring(equals + 1), "UTF-8"));
            } catch (Exception ignored) { }
        }
        return result;
    }

    public static void send(HttpExchange exchange, int status, Map<String, Object> payload)
            throws IOException {
        byte[] bytes = Json.write(payload).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream out = exchange.getResponseBody();
        try { out.write(bytes); } finally { out.close(); }
    }

    public static void error(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("error", message);
        send(exchange, status, payload);
    }

    public static Map<String, Object> ok() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ok", true);
        return payload;
    }

    /**
     * Compares two secrets without leaking how much of them matched. A plain
     * equals returns as soon as a byte differs, which is enough to guess a
     * token one character at a time over enough requests.
     */
    public static boolean secretEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                                     b.getBytes(StandardCharsets.UTF_8));
    }

    /** True when the request carries the admin token. */
    public static boolean admin(HttpExchange exchange, String token) {
        if (token == null || token.isEmpty()) return false;
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return false;
        return secretEquals(header.substring(7).trim(), token);
    }

    /** The caller's address, honouring the proxy header when one is configured. */
    public static String client(HttpExchange exchange, boolean behindProxy) {
        if (behindProxy) {
            String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                int comma = forwarded.indexOf(',');
                return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
            }
        }
        return exchange.getRemoteAddress() == null
                ? "?" : exchange.getRemoteAddress().getAddress().getHostAddress();
    }
}
