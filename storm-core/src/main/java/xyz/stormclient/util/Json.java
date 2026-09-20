package xyz.stormclient.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny dependency free JSON reader/writer.
 * Storm ships no third party libraries so the jar stays small and there is
 * nothing to clash with whatever the mod loader already put on the classpath.
 */
public final class Json {

    private Json() { }

    // ------------------------------------------------------------------
    //  writing
    // ------------------------------------------------------------------
    public static String write(Object value) { return write(value, 0); }

    @SuppressWarnings("unchecked")
    private static String write(Object value, int indent) {
        StringBuilder sb = new StringBuilder();
        String pad = repeat("  ", indent);
        String padIn = repeat("  ", indent + 1);

        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (map.isEmpty()) return "{}";
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                sb.append(padIn).append('"').append(escape(e.getKey())).append("\": ")
                  .append(write(e.getValue(), indent + 1));
                if (++i < map.size()) sb.append(',');
                sb.append('\n');
            }
            sb.append(pad).append('}');
            return sb.toString();
        }
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            if (list.isEmpty()) return "[]";
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(padIn).append(write(list.get(i), indent + 1));
                if (i < list.size() - 1) sb.append(',');
                sb.append('\n');
            }
            sb.append(pad).append(']');
            return sb.toString();
        }
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return '"' + escape(String.valueOf(value)) + '"';
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(s);
        return sb.toString();
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    //  reading
    // ------------------------------------------------------------------
    public static Object read(String json) {
        return new Parser(json).parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readObject(String json) {
        Object o = read(json);
        return o instanceof Map ? (Map<String, Object>) o : new LinkedHashMap<String, Object>();
    }

    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) { this.src = src; }

        Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': pos += 4; return Boolean.TRUE;
                case 'f': pos += 5; return Boolean.FALSE;
                case 'n': pos += 4; return null;
                default:  return parseNumber();
            }
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            pos++;                                   // {
            skipWhitespace();
            if (peek() == '}') { pos++; return map; }
            while (pos < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (peek() == ':') pos++;
                map.put(key, parseValue());
                skipWhitespace();
                char c = peek();
                pos++;
                if (c == '}' || c == '\0') break;
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<Object>();
            pos++;                                   // [
            skipWhitespace();
            if (peek() == ']') { pos++; return list; }
            while (pos < src.length()) {
                list.add(parseValue());
                skipWhitespace();
                char c = peek();
                pos++;
                if (c == ']' || c == '\0') break;
            }
            return list;
        }

        String parseString() {
            StringBuilder sb = new StringBuilder();
            if (peek() == '"') pos++;
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < src.length()) {
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Object parseNumber() {
            int start = pos;
            while (pos < src.length() && "-+.eE0123456789".indexOf(src.charAt(pos)) >= 0) pos++;
            String raw = src.substring(start, pos);
            try {
                if (raw.contains(".") || raw.contains("e") || raw.contains("E")) return Double.parseDouble(raw);
                return Long.parseLong(raw);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        char peek() { return pos < src.length() ? src.charAt(pos) : '\0'; }

        void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
        }
    }
}
