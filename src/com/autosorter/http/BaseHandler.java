package com.autosorter.http;

import com.autosorter.exception.AutoSorterException;
import com.autosorter.exception.InvalidCommandException;
import com.autosorter.model.User;
import com.autosorter.service.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ABSTRACTION + TEMPLATE METHOD: handle() is final and does everything that is the same for every
 * endpoint (CORS, JSON body parsing, token check, error-to-JSON conversion). Subclasses only
 * implement route(), so each handler has exactly one responsibility.
 */
public abstract class BaseHandler implements HttpHandler {
    protected final AuthService auth;

    protected BaseHandler(AuthService auth) { this.auth = auth; }

    /** Parsed request handed to subclasses. */
    protected record ApiRequest(String method, String path, Map<String, String> query,
                                Map<String, Object> body, User user, String token) {
        public String bodyString(String key) {
            Object v = body.get(key);
            return v == null ? null : v.toString();
        }
    }

    protected abstract Object route(ApiRequest req);

    /** Public endpoints (login, health) override this. */
    protected boolean requiresAuth() { return true; }

    /** The URL prefix this handler is registered on, used to compute sub-paths. */
    protected abstract String basePath();

    @Override
    public final void handle(HttpExchange ex) throws IOException {
        try {
            cors(ex);
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }

            String token = bearer(ex);
            User user = requiresAuth() ? auth.validate(token) : null;
            String full = ex.getRequestURI().getPath();
            String sub = full.length() > basePath().length() ? full.substring(basePath().length()) : "";
            Map<String, Object> body = "POST".equalsIgnoreCase(ex.getRequestMethod()) || "PUT".equalsIgnoreCase(ex.getRequestMethod())
                    ? JsonReader.parseObject(readBody(ex)) : new LinkedHashMap<>();
            ApiRequest req = new ApiRequest(ex.getRequestMethod().toUpperCase(), sub, query(ex), body, user, token);

            Object result = route(req);
            send(ex, 200, JsonWriter.write(result));
        } catch (AutoSorterException e) {
            send(ex, e.getHttpStatus(), error(e.getClass().getSimpleName(), e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, error("InternalError", "Unexpected server error"));
        } finally {
            ex.close();
        }
    }

    protected InvalidCommandException notFound(ApiRequest r) {
        return new InvalidCommandException("Unsupported request: " + r.method() + " " + basePath() + r.path());
    }

    private static String error(String type, String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", type);
        m.put("message", msg);
        return JsonWriter.write(m);
    }

    private static void cors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }

    private static String bearer(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        return (h != null && h.startsWith("Bearer ")) ? h.substring(7).trim() : null;
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> q = new LinkedHashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return q;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            q.put(kv[0], kv.length > 1 ? java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return q;
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
