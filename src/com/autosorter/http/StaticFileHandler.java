package com.autosorter.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Serves the frontend (index.html + JS) from the /public folder, so ONE server hosts both UI and API. */
public class StaticFileHandler implements HttpHandler {
    private static final Map<String, String> TYPES = Map.of(
            ".html", "text/html; charset=utf-8", ".js", "application/javascript; charset=utf-8",
            ".css", "text/css; charset=utf-8", ".png", "image/png", ".svg", "image/svg+xml", ".ico", "image/x-icon");
    private final Path root;

    public StaticFileHandler(Path root) { this.root = root.toAbsolutePath().normalize(); }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String p = ex.getRequestURI().getPath();
            if (p.equals("/")) p = "/index.html";
            Path file = root.resolve(p.substring(1)).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {      // blocks ../ path traversal
                byte[] nf = "404 Not Found".getBytes();
                ex.sendResponseHeaders(404, nf.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(nf); }
                return;
            }
            String name = file.getFileName().toString();
            String type = TYPES.getOrDefault(name.substring(name.lastIndexOf('.') >= 0 ? name.lastIndexOf('.') : 0), "application/octet-stream");
            byte[] data = Files.readAllBytes(file);
            ex.getResponseHeaders().set("Content-Type", type);
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        } finally {
            ex.close();
        }
    }
}
