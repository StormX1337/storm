package xyz.stormclient.launcher.core;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import xyz.stormclient.licence.MachineId;
import xyz.stormclient.util.Json;

/**
 * Talks to the licence server.
 *
 * <p>The customer holds a short key; the client holds a signed licence. This is
 * where one becomes the other. The launcher asks again on every launch, which
 * is what makes revoking a key take effect: the signed licence it gets back
 * only lasts a few days, so a key pulled on the server stops working shortly
 * after, without the client ever needing the server to start.
 */
public final class LicenceClient {

    /** Long enough for a slow VPS, short enough not to hold up a launch. */
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 8000;
    private static final int MAX_RESPONSE = 32 * 1024;

    private LicenceClient() { }

    /** What came back, which is either a licence or a reason there is none. */
    public static final class Result {
        public final String licence;
        public final String holder;
        public final String plan;
        public final String error;
        /** True when the server said no, as opposed to not answering at all. */
        public final boolean refused;

        private Result(String licence, String holder, String plan, String error, boolean refused) {
            this.licence = licence;
            this.holder = holder;
            this.plan = plan;
            this.error = error;
            this.refused = refused;
        }

        public boolean ok() { return licence != null && !licence.isEmpty(); }

        static Result licence(String blob, String holder, String plan) {
            return new Result(blob, holder, plan, "", false);
        }
        /** The server answered and said no. The cached licence should go. */
        static Result refused(String reason) { return new Result(null, "", "", reason, true); }
        /** We could not ask. Whatever is cached stays valid until it runs out. */
        static Result unreachable(String reason) { return new Result(null, "", "", reason, false); }
    }

    /**
     * Swaps the customer's key for a signed licence.
     *
     * @param server base URL of the licence server, e.g. https://keys.example.com
     * @param key    the short key, as the customer typed it
     */
    public static Result activate(String server, String key) {
        if (server == null || server.trim().isEmpty()) {
            return Result.unreachable("no licence server is configured");
        }
        if (key == null || key.trim().isEmpty()) {
            return Result.refused("no key was entered");
        }

        Map<String, Object> request = new java.util.LinkedHashMap<String, Object>();
        request.put("key", key.trim());
        request.put("machine", MachineId.get());

        try {
            HttpURLConnection connection = open(server, "/api/activate");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            OutputStream out = connection.getOutputStream();
            try {
                out.write(Json.write(request).getBytes(StandardCharsets.UTF_8));
            } finally {
                out.close();
            }

            int status = connection.getResponseCode();
            String body = read(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            Map<String, Object> response = Json.readObject(body);

            if (status == 200) {
                String blob = text(response.get("licence"));
                if (blob.isEmpty()) return Result.refused("the server sent no licence back");
                return Result.licence(blob, text(response.get("holder")), text(response.get("plan")));
            }
            if (status == 429) {
                return Result.unreachable("the server asked us to slow down, try again in a minute");
            }
            String reason = text(response.get("error"));
            // 4xx is the server making a decision; 5xx is the server having a
            // bad day, and a paying customer should not be locked out for that
            if (status >= 500) {
                return Result.unreachable(reason.isEmpty() ? "the server is having trouble" : reason);
            }
            return Result.refused(reason.isEmpty() ? "the server refused the key" : reason);
        } catch (Exception e) {
            return Result.unreachable("could not reach the licence server: " + e.getMessage());
        }
    }

    private static HttpURLConnection open(String server, String path) throws Exception {
        String base = server.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        HttpURLConnection connection = (HttpURLConnection) new URL(base + path).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent",
                "StormLauncher/" + xyz.stormclient.launcher.StormLauncher.VERSION);
        return connection;
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        try {
            while ((count = in.read(buffer)) > 0) {
                out.write(buffer, 0, count);
                if (out.size() > MAX_RESPONSE) break;
            }
        } finally {
            in.close();
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
