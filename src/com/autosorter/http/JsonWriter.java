package com.autosorter.http;

import com.autosorter.model.JsonSerializable;

import java.util.Collection;
import java.util.Map;

/** Minimal JSON serialiser (no external libraries). Handles maps, lists, numbers, strings, enums, arrays. */
public final class JsonWriter {
    private JsonWriter() {}

    public static String write(Object o) {
        StringBuilder sb = new StringBuilder();
        append(sb, o);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void append(StringBuilder sb, Object o) {
        if (o == null) { sb.append("null"); return; }
        if (o instanceof JsonSerializable js) { append(sb, js.toJson()); return; }      // polymorphic call
        if (o instanceof String || o instanceof Enum || o instanceof java.time.Instant) { quote(sb, o.toString()); return; }
        if (o instanceof Boolean) { sb.append(o); return; }
        if (o instanceof Number n) {
            double d = n.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) sb.append("null");
            else if (o instanceof Double || o instanceof Float) sb.append(d);
            else sb.append(n.longValue());
            return;
        }
        if (o instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(sb, String.valueOf(e.getKey()));
                sb.append(':');
                append(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (o instanceof Collection<?> c) {
            sb.append('[');
            boolean first = true;
            for (Object x : c) {
                if (!first) sb.append(',');
                first = false;
                append(sb, x);
            }
            sb.append(']');
            return;
        }
        if (o instanceof int[] arr) {
            sb.append('[');
            for (int i = 0; i < arr.length; i++) { if (i > 0) sb.append(','); sb.append(arr[i]); }
            sb.append(']');
            return;
        }
        quote(sb, o.toString());
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch)); else sb.append(ch); }
            }
        }
        sb.append('"');
    }
}
