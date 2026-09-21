package xyz.stormclient.licenceserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import xyz.stormclient.util.Json;

/**
 * Every key this server has issued, kept in one JSON file.
 *
 * <p>A licence server for a Minecraft client handles a few thousand rows and a
 * handful of requests a minute, so a file the operator can read, grep and back
 * up with scp beats a database they have to install and maintain. Writes go to
 * a temporary file and are moved into place, so a crash mid-write cannot leave
 * a half written store behind.
 */
public final class Store {

    /** One issued key. */
    public static final class Record {
        public String key = "";
        public String holder = "";
        public String plan = "standard";
        public String note = "";
        public String machine = "";
        public long issued;
        public long days;          // licence length, counted from first activation
        public long until;         // resolved end date, set on first activation
        public long lastSeen;
        public long activations;
        public boolean revoked;

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("holder", holder);
            map.put("plan", plan);
            map.put("note", note);
            map.put("machine", machine);
            map.put("issued", issued);
            map.put("days", days);
            map.put("until", until);
            map.put("lastSeen", lastSeen);
            map.put("activations", activations);
            map.put("revoked", revoked);
            return map;
        }

        static Record fromMap(String key, Map<String, Object> map) {
            Record record = new Record();
            record.key = key;
            record.holder = string(map, "holder");
            record.plan = string(map, "plan");
            record.note = string(map, "note");
            record.machine = string(map, "machine");
            record.issued = number(map, "issued");
            record.days = number(map, "days");
            record.until = number(map, "until");
            record.lastSeen = number(map, "lastSeen");
            record.activations = number(map, "activations");
            record.revoked = Boolean.TRUE.equals(map.get("revoked"));
            return record;
        }

        private static String string(Map<String, Object> map, String name) {
            Object value = map.get(name);
            return value == null ? "" : String.valueOf(value);
        }

        private static long number(Map<String, Object> map, String name) {
            Object value = map.get(name);
            return value instanceof Number ? ((Number) value).longValue() : 0;
        }
    }

    private final File file;
    private final Map<String, Record> records = new LinkedHashMap<String, Record>();
    /** Lets the Stripe hook answer "what key did this checkout get?" without a scan. */
    private final Map<String, String> bySession = new LinkedHashMap<String, String>();

    public Store(File file) {
        this.file = file;
        load();
    }

    @SuppressWarnings("unchecked")
    private synchronized void load() {
        if (!file.isFile()) return;
        try {
            String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Map<String, Object> root = Json.readObject(raw);
            Object keys = root.get("keys");
            if (!(keys instanceof Map)) return;
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) keys).entrySet()) {
                if (!(entry.getValue() instanceof Map)) continue;
                Record record = Record.fromMap(entry.getKey(), (Map<String, Object>) entry.getValue());
                records.put(record.key, record);
                if (!record.note.isEmpty()) bySession.put(record.note, record.key);
            }
        } catch (Exception e) {
            throw new IllegalStateException("could not read " + file + ": " + e, e);
        }
    }

    private synchronized void save() {
        Map<String, Object> keys = new LinkedHashMap<String, Object>();
        for (Record record : records.values()) keys.put(record.key, record.toMap());
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("keys", keys);

        try {
            File temp = new File(file.getAbsolutePath() + ".tmp");
            Files.write(temp.toPath(), Json.write(root).getBytes(StandardCharsets.UTF_8));
            Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("could not write " + file + ": " + e, e);
        }
    }

    public synchronized Record get(String key) { return records.get(key); }

    public synchronized Record bySession(String session) {
        String key = bySession.get(session);
        return key == null ? null : records.get(key);
    }

    public synchronized List<Record> all() {
        return new ArrayList<Record>(records.values());
    }

    public synchronized Record issue(String holder, String plan, long days, String note) {
        String key;
        do { key = ShortKey.generate(); } while (records.containsKey(key));

        Record record = new Record();
        record.key = key;
        record.holder = holder == null ? "" : holder;
        record.plan = plan == null || plan.isEmpty() ? "standard" : plan;
        record.note = note == null ? "" : note;
        record.days = Math.max(0, days);
        record.issued = System.currentTimeMillis();
        records.put(key, record);
        if (!record.note.isEmpty()) bySession.put(record.note, key);
        save();
        return record;
    }

    public synchronized void update(Record record) {
        records.put(record.key, record);
        save();
    }

    public synchronized boolean revoke(String key, boolean revoked) {
        Record record = records.get(key);
        if (record == null) return false;
        record.revoked = revoked;
        save();
        return true;
    }

    /** Clears the machine binding so the customer can move to another computer. */
    public synchronized boolean unbind(String key) {
        Record record = records.get(key);
        if (record == null) return false;
        record.machine = "";
        save();
        return true;
    }
}
