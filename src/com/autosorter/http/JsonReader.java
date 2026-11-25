package com.autosorter.http;

import com.autosorter.exception.InvalidCommandException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal recursive-descent JSON parser for request bodies. Malformed input -> InvalidCommandException (HTTP 400). */
public final class JsonReader {
    private final String s;
    private int i = 0;

    private JsonReader(String s) { this.s = s; }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        if (text == null || text.isBlank()) return new LinkedHashMap<>();
        try {
            JsonReader r = new JsonReader(text);
            r.ws();
            Object v = r.value();
            if (!(v instanceof Map)) throw new InvalidCommandException("JSON body must be an object");
            return (Map<String, Object>) v;
        } catch (InvalidCommandException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InvalidCommandException("Malformed JSON body");
        }
    }

    private Object value() {
        char c = s.charAt(i);
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        int start = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        return Double.parseDouble(s.substring(start, i));
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; ws();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            String k = string();
            ws(); expect(':'); ws();
            m.put(k, value());
            ws();
            if (s.charAt(i) == ',') { i++; continue; }
            expect('}');
            return m;
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++; ws();
        if (s.charAt(i) == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            if (s.charAt(i) == ',') { i++; continue; }
            expect(']');
            return l;
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (s.charAt(i) != '"') {
            char c = s.charAt(i++);
            if (c == '\\') {
                char n = s.charAt(i++);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'u' -> { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                    default -> sb.append(n);
                }
            } else sb.append(c);
        }
        i++;
        return sb.toString();
    }

    private void expect(char c) {
        if (s.charAt(i) != c) throw new IllegalStateException("expected " + c);
        i++;
    }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
}
