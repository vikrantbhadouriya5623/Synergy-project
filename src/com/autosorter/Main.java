package com.autosorter;

import com.autosorter.http.Handlers;
import com.autosorter.http.StaticFileHandler;
import com.autosorter.service.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/** Composition root: builds all objects once and wires them together (dependency injection by constructor). */
public class Main {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(
        System.getenv().getOrDefault("PORT", "8080")
);
        AuthService auth = new AuthService();
        SortingStatistics stats = new SortingStatistics();
        SortingRepository repo = new CsvSortingRepository(Path.of("logs", "sorting_log.csv"));
        SortingMachine machine = new SortingMachine(new ThresholdColourClassifier(), stats, repo, 450);
        SimulationEngine engine = new SimulationEngine(machine);
        DashboardService dashboard = new DashboardService(machine);

        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/api/health", new Handlers.Health(auth));
        server.createContext("/api/auth", new Handlers.Auth(auth));
        server.createContext("/api/dashboard", new Handlers.Dashboard(auth, dashboard));
        server.createContext("/api/machine", new Handlers.Machine(auth, machine, engine, dashboard));
        server.createContext("/api/colour", new Handlers.Colour(auth, dashboard));
        server.createContext("/api/sensors", new Handlers.Sensors(auth, dashboard));
        server.createContext("/api/components", new Handlers.Components(auth));
        server.createContext("/api/logs", new Handlers.Logs(auth, dashboard));
        server.createContext("/", new StaticFileHandler(Path.of("public")));
        server.setExecutor(Executors.newFixedThreadPool(8));          // one thread per concurrent request

        engine.start();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { engine.close(); server.stop(0); }));

        System.out.println("==============================================");
        System.out.println(" AutoSorter backend running (HTTP + JSON REST)");
        System.out.println(" Open  : http://localhost:" + port + "/");
        System.out.println(" Login : engineer@autosorter.lab / viva2025");
        System.out.println("==============================================");
    }
}
