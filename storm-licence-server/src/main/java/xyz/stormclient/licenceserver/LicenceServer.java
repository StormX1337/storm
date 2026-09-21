package xyz.stormclient.licenceserver;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * The licence server: it sells nothing and knows nothing about Minecraft, it
 * only turns a key a customer paid for into a signed licence their client will
 * accept.
 *
 * <p>Two ideas hold it together. The customer's key is a short bearer token
 * this server can revoke at any time. The thing the client checks is a signed
 * blob with a short lease, so revoking a key takes effect the next time the
 * launcher refreshes rather than never.
 *
 * <p>It speaks plain HTTP on the loopback address and expects nginx in front
 * of it for TLS. See README.md next to this file.
 */
public final class LicenceServer {

    private final Config config;
    private final Store store;
    private final Signer signer;
    private final RateLimit publicLimit;

    public LicenceServer(Config config) throws Exception {
        this.config = config;
        this.store = new Store(config.storeFile);
        this.signer = new Signer(config.privateKeyFile);
        this.publicLimit = new RateLimit(config.burst, config.perSecond);
    }

    // ==================================================================
    public static void main(String[] args) throws Exception {
        File configFile = new File(args.length > 0 ? args[0] : "storm-licences.properties");
        Config config = Config.load(configFile);
        new LicenceServer(config).start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.host, config.port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        server.createContext("/api/activate", guard(this::activate));
        server.createContext("/api/status", guard(this::status));
        server.createContext("/api/claim", guard(this::claim));
        server.createContext("/api/issue", admin(this::issue));
        server.createContext("/api/revoke", admin(this::revoke));
        server.createContext("/api/unbind", admin(this::unbind));
        server.createContext("/api/keys", admin(this::keys));
        server.createContext("/hook/stripe", wrap(this::stripe));
        server.createContext("/health", wrap(exchange -> Http.send(exchange, 200, Http.ok())));

        server.start();
        log("listening on " + config.host + ":" + config.port
                + ", " + store.all().size() + " keys, lease " + config.leaseDays + " days");
    }

    // ==================================================================
    //  public endpoints
    // ==================================================================

    /**
     * Swaps a customer's key for a signed licence and binds it to the machine
     * it was first used on. The launcher calls this on every launch, so this is
     * also where a revoked key stops working.
     */
    private void activate(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            Http.error(exchange, 405, "use POST");
            return;
        }
        Map<String, Object> request = Http.json(exchange);
        String key = ShortKey.normalise(Http.string(request, "key"));
        String machine = Http.string(request, "machine");

        if (key == null) { Http.error(exchange, 400, "that is not a Storm key"); return; }
        if (machine.isEmpty()) { Http.error(exchange, 400, "no machine id was sent"); return; }

        Store.Record record = store.get(key);
        if (record == null)  { Http.error(exchange, 403, "unknown key"); return; }
        if (record.revoked)  { Http.error(exchange, 403, "this key has been revoked"); return; }

        long now = System.currentTimeMillis();

        // the first activation starts the clock and claims the machine
        if (record.until == 0 && record.days > 0) record.until = now + record.days * 86400000L;
        if (record.machine.isEmpty()) record.machine = machine;
        else if (!record.machine.equals(machine)) {
            Http.error(exchange, 403, "this key is already in use on another computer");
            return;
        }

        if (record.until > 0 && record.until < now) {
            Http.error(exchange, 403, "this key ran out on " + date(record.until));
            return;
        }

        record.lastSeen = now;
        record.activations++;
        store.update(record);

        long lease = config.leaseDays <= 0 ? record.until : now + config.leaseDays * 86400000L;
        long expires = record.until <= 0 ? lease : Math.min(lease, record.until);

        String blob;
        try {
            blob = signer.sign(id(key), record.holder, record.plan, machine,
                    record.issued, expires, record.until);
        } catch (Exception e) {
            log("could not sign for " + key + ": " + e);
            Http.error(exchange, 500, "could not issue a licence, try again later");
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("licence", blob);
        payload.put("holder", record.holder);
        payload.put("plan", record.plan);
        payload.put("until", record.until);
        payload.put("refreshAfter", expires);
        Http.send(exchange, 200, payload);
        log("activated " + key + " for " + record.holder + " on " + machine);
    }

    /** What a customer or a support page may see about a key. */
    private void status(HttpExchange exchange) throws IOException {
        String key = ShortKey.normalise(Http.query(exchange).get("key"));
        if (key == null) { Http.error(exchange, 400, "that is not a Storm key"); return; }

        Store.Record record = store.get(key);
        if (record == null) { Http.error(exchange, 404, "unknown key"); return; }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("state", state(record));
        payload.put("holder", record.holder);
        payload.put("plan", record.plan);
        payload.put("until", record.until);
        payload.put("bound", !record.machine.isEmpty());
        Http.send(exchange, 200, payload);
    }

    /**
     * Hands the key to the page Stripe redirects to after payment. The session
     * id is unguessable and only the buyer's browser has it.
     */
    private void claim(HttpExchange exchange) throws IOException {
        String session = Http.query(exchange).get("session");
        if (session == null || session.isEmpty()) {
            Http.error(exchange, 400, "no session id");
            return;
        }
        Store.Record record = store.bySession(session);
        if (record == null) {
            // the webhook may still be in flight, so this is not a hard error
            Http.error(exchange, 404, "no key for that checkout yet");
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("key", record.key);
        payload.put("plan", record.plan);
        Http.send(exchange, 200, payload);
    }

    // ==================================================================
    //  admin endpoints
    // ==================================================================
    private void issue(HttpExchange exchange) throws IOException {
        Map<String, Object> request = Http.json(exchange);
        Store.Record record = store.issue(
                Http.string(request, "holder"),
                Http.string(request, "plan"),
                Http.number(request, "days", 0),
                Http.string(request, "note"));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("key", record.key);
        payload.put("holder", record.holder);
        payload.put("plan", record.plan);
        payload.put("days", record.days);
        Http.send(exchange, 200, payload);
        log("issued " + record.key + " for " + record.holder);
    }

    private void revoke(HttpExchange exchange) throws IOException {
        Map<String, Object> request = Http.json(exchange);
        String key = ShortKey.normalise(Http.string(request, "key"));
        boolean revoked = !"false".equalsIgnoreCase(Http.string(request, "revoked"));
        if (key == null || !store.revoke(key, revoked)) {
            Http.error(exchange, 404, "unknown key");
            return;
        }
        Http.send(exchange, 200, Http.ok());
        log((revoked ? "revoked " : "restored ") + key);
    }

    private void unbind(HttpExchange exchange) throws IOException {
        Map<String, Object> request = Http.json(exchange);
        String key = ShortKey.normalise(Http.string(request, "key"));
        if (key == null || !store.unbind(key)) {
            Http.error(exchange, 404, "unknown key");
            return;
        }
        Http.send(exchange, 200, Http.ok());
        log("unbound " + key);
    }

    private void keys(HttpExchange exchange) throws IOException {
        List<Object> rows = new ArrayList<Object>();
        for (Store.Record record : store.all()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("key", record.key);
            row.put("holder", record.holder);
            row.put("plan", record.plan);
            row.put("state", state(record));
            row.put("machine", record.machine);
            row.put("until", record.until);
            row.put("activations", record.activations);
            rows.add(row);
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("keys", rows);
        Http.send(exchange, 200, payload);
    }

    // ==================================================================
    //  payment webhook
    // ==================================================================
    private void stripe(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            Http.error(exchange, 405, "use POST");
            return;
        }
        if (config.stripeSecret.isEmpty()) {
            Http.error(exchange, 404, "no payment hook is configured");
            return;
        }

        byte[] body = Http.body(exchange);
        String signature = exchange.getRequestHeaders().getFirst("Stripe-Signature");
        if (!StripeHook.verify(signature, body, config.stripeSecret)) {
            log("rejected a webhook call with a bad signature from "
                    + Http.client(exchange, config.behindProxy));
            Http.error(exchange, 400, "bad signature");
            return;
        }

        StripeHook.Checkout checkout = StripeHook.checkout(body);
        if (checkout == null) {
            // some other event type, acknowledge it so Stripe stops retrying
            Http.send(exchange, 200, Http.ok());
            return;
        }

        Store.Record existing = store.bySession(checkout.sessionId);
        if (existing != null) {
            // Stripe retries until it gets a 200, so the same session must not
            // hand out a second key
            Map<String, Object> payload = Http.ok();
            payload.put("key", existing.key);
            Http.send(exchange, 200, payload);
            return;
        }

        String plan = checkout.plan.isEmpty() ? config.defaultPlan : checkout.plan;
        Store.Record record = store.issue(
                checkout.email.isEmpty() ? "customer" : checkout.email,
                plan,
                config.planDays(plan),
                checkout.sessionId);

        Map<String, Object> payload = Http.ok();
        payload.put("key", record.key);
        Http.send(exchange, 200, payload);
        log("paid: issued " + record.key + " (" + plan + ") for "
                + (checkout.email.isEmpty() ? "unknown email" : checkout.email));
    }

    // ==================================================================
    private String state(Store.Record record) {
        if (record.revoked) return "revoked";
        if (record.until > 0 && record.until < System.currentTimeMillis()) return "expired";
        return record.machine.isEmpty() ? "unused" : "active";
    }

    /** The short identifier the client shows, derived from the key, never the key. */
    private static String id(String key) {
        String body = key.replace("-", "");
        return body.substring(Math.max(0, body.length() - 6));
    }

    private static String date(long millis) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(millis));
    }

    private static void log(String message) {
        System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date()) + "] " + message);
    }

    // ------------------------------------------------------------------
    private interface Route { void handle(HttpExchange exchange) throws IOException; }

    /** Turns any thrown exception into a 500 instead of an empty reply. */
    private HttpHandler wrap(final Route route) {
        return new HttpHandler() {
            @Override public void handle(HttpExchange exchange) throws IOException {
                try {
                    route.handle(exchange);
                } catch (Exception e) {
                    log("failed " + exchange.getRequestURI() + ": " + e);
                    try { Http.error(exchange, 500, "server error"); } catch (Exception ignored) { }
                } finally {
                    exchange.close();
                }
            }
        };
    }

    /** A public route, behind the rate limit. */
    private HttpHandler guard(final Route route) {
        return wrap(new Route() {
            @Override public void handle(HttpExchange exchange) throws IOException {
                if (!publicLimit.allow(Http.client(exchange, config.behindProxy))) {
                    Http.error(exchange, 429, "too many requests, slow down");
                    return;
                }
                route.handle(exchange);
            }
        });
    }

    /** A route only the operator's token opens. */
    private HttpHandler admin(final Route route) {
        return wrap(new Route() {
            @Override public void handle(HttpExchange exchange) throws IOException {
                if (!Http.admin(exchange, config.adminToken)) {
                    Http.error(exchange, 401, "admin token required");
                    return;
                }
                route.handle(exchange);
            }
        });
    }

    // ==================================================================
    /** Everything the operator sets, read from a properties file. */
    public static final class Config {

        public String host = "127.0.0.1";
        public int port = 8710;
        public File storeFile = new File("licences.json");
        public File privateKeyFile = new File("licence-private.key");
        public String adminToken = "";
        public String stripeSecret = "";
        public String defaultPlan = "standard";
        public long leaseDays = 7;
        public boolean behindProxy = true;
        public int burst = 20;
        public double perSecond = 0.5;

        private final Map<String, Long> planDays = new LinkedHashMap<String, Long>();

        /** How long a plan lasts, or 0 when it never ends. */
        public long planDays(String plan) {
            Long days = planDays.get(plan.toLowerCase());
            return days == null ? 0 : days;
        }

        private static File resolve(File base, String path) {
            File named = new File(path.trim());
            return named.isAbsolute() ? named : new File(base, path.trim());
        }

        public static Config load(File file) throws IOException {
            if (!file.isFile()) {
                throw new IOException("no config at " + file.getAbsolutePath()
                        + " - copy deploy/storm-licences.properties and fill it in");
            }
            Properties properties = new Properties();
            java.io.InputStream in = new java.io.FileInputStream(file);
            try { properties.load(in); } finally { in.close(); }

            Config config = new Config();
            config.host = properties.getProperty("host", config.host).trim();
            config.port = Integer.parseInt(properties.getProperty("port", "8710").trim());
            // relative paths follow the config file, so systemd's working
            // directory cannot quietly point the server at the wrong store
            File base = file.getAbsoluteFile().getParentFile();
            config.storeFile = resolve(base, properties.getProperty("store", "licences.json"));
            config.privateKeyFile = resolve(base, properties.getProperty("privateKey", "licence-private.key"));
            config.adminToken = properties.getProperty("adminToken", "").trim();
            config.stripeSecret = properties.getProperty("stripeWebhookSecret", "").trim();
            config.defaultPlan = properties.getProperty("defaultPlan", "standard").trim();
            config.leaseDays = Long.parseLong(properties.getProperty("leaseDays", "7").trim());
            config.behindProxy = !"false".equalsIgnoreCase(properties.getProperty("behindProxy", "true").trim());
            config.burst = Integer.parseInt(properties.getProperty("rateBurst", "20").trim());
            config.perSecond = Double.parseDouble(properties.getProperty("ratePerSecond", "0.5").trim());

            // plan.pro = 30, plan.lifetime = 0
            for (String name : properties.stringPropertyNames()) {
                if (!name.startsWith("plan.")) continue;
                config.planDays.put(name.substring(5).toLowerCase(),
                        Long.parseLong(properties.getProperty(name).trim()));
            }

            if (config.adminToken.length() < 24) {
                throw new IOException("adminToken must be at least 24 characters - "
                        + "generate one with: head -c 32 /dev/urandom | base64");
            }
            if (!config.privateKeyFile.isFile()) {
                throw new IOException("no private key at " + config.privateKeyFile.getAbsolutePath()
                        + " - create one with LicenceTool genkey");
            }
            return config;
        }
    }
}
