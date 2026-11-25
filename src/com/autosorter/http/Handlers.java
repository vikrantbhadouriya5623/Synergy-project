package com.autosorter.http;

import com.autosorter.exception.InvalidCommandException;
import com.autosorter.service.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All concrete endpoint handlers. Each one EXTENDS BaseHandler and OVERRIDES route()
 * (inheritance + runtime polymorphism: ApiServer treats them all as BaseHandler).
 */
public final class Handlers {
    private Handlers() {}

    private static Map<String, Object> ok(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("message", message);
        return m;
    }

    // ---------- /api/health (public) ----------
    public static class Health extends BaseHandler {
        public Health(AuthService a) { super(a); }
        @Override protected String basePath() { return "/api/health"; }
        @Override protected boolean requiresAuth() { return false; }
        @Override protected Object route(ApiRequest r) {
            Map<String, Object> m = ok("AutoSorter backend online");
            m.put("protocol", "HTTP/1.1 + JSON (REST)");
            m.put("firmware", "v2.4.1-OOP");
            return m;
        }
    }

    // ---------- /api/auth ----------
    public static class Auth extends BaseHandler {
        public Auth(AuthService a) { super(a); }
        @Override protected String basePath() { return "/api/auth"; }
        @Override protected boolean requiresAuth() { return false; }     // login itself is public
        @Override protected Object route(ApiRequest r) {
            if (r.method().equals("POST") && r.path().equals("/login"))
                return auth.login(r.bodyString("email"), r.bodyString("password"));
            if (r.method().equals("POST") && r.path().equals("/logout")) {
                auth.logout(r.token());
                return ok("Signed out");
            }
            if (r.method().equals("GET") && r.path().equals("/me")) {
                var u = auth.validate(r.token());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("operatorId", u.getOperatorId());
                m.put("role", u.getRole());
                m.put("calibrationPrivilege", u.hasCalibrationPrivilege());
                return m;
            }
            throw notFound(r);
        }
    }

    // ---------- /api/dashboard, /api/colour, /api/sensors, /api/components, /api/logs ----------
    public static class Dashboard extends BaseHandler {
        private final DashboardService svc;
        public Dashboard(AuthService a, DashboardService s) { super(a); this.svc = s; }
        @Override protected String basePath() { return "/api/dashboard"; }
        @Override protected Object route(ApiRequest r) { return svc.dashboard(); }
    }

    public static class Colour extends BaseHandler {
        private final DashboardService svc;
        public Colour(AuthService a, DashboardService s) { super(a); this.svc = s; }
        @Override protected String basePath() { return "/api/colour"; }
        @Override protected Object route(ApiRequest r) { return svc.colourDetection(); }
    }

    public static class Sensors extends BaseHandler {
        private final DashboardService svc;
        public Sensors(AuthService a, DashboardService s) { super(a); this.svc = s; }
        @Override protected String basePath() { return "/api/sensors"; }
        @Override protected Object route(ApiRequest r) { return svc.sensorTelemetry(); }
    }

    public static class Components extends BaseHandler {
        public Components(AuthService a) { super(a); }
        @Override protected String basePath() { return "/api/components"; }
        @Override protected Object route(ApiRequest r) { return ComponentCatalog.all(); }
    }

    public static class Logs extends BaseHandler {
        private final DashboardService svc;
        public Logs(AuthService a, DashboardService s) { super(a); this.svc = s; }
        @Override protected String basePath() { return "/api/logs"; }
        @Override protected Object route(ApiRequest r) {
            int limit = 20;
            try { limit = Integer.parseInt(r.query().getOrDefault("limit", "20")); }
            catch (NumberFormatException e) { throw new InvalidCommandException("limit must be a number"); }
            return svc.logs(Math.max(1, Math.min(50, limit)));
        }
    }

    // ---------- /api/machine : status + control commands ----------
    public static class Machine extends BaseHandler {
        private final SortingMachine machine;
        private final SimulationEngine engine;
        private final DashboardService svc;

        public Machine(AuthService a, SortingMachine m, SimulationEngine e, DashboardService s) {
            super(a);
            this.machine = m; this.engine = e; this.svc = s;
        }

        @Override protected String basePath() { return "/api/machine"; }

        @Override protected Object route(ApiRequest r) {
            String p = r.path();
            if (r.method().equals("GET") && p.equals("/status")) return svc.machineStatus();

            if (!r.method().equals("POST")) throw notFound(r);

            switch (p) {
                case "/start":     machine.startMachine();        return ok("Sorting started");
                case "/pause":     machine.pauseMachine();        return ok("Feed paused");
                case "/estop":     machine.emergencyStop();       return ok("EMERGENCY STOP engaged");
                case "/reset":     machine.resetEmergencyStop();  return ok("Emergency stop reset - operation resumed");
                case "/feed-next": engine.requestManualFeed();    return ok("Item queued for sorting");
                case "/calibrate": {
                    auth.requireCalibrationPrivilege(r.user());
                    Map<String, Object> m = ok("Calibration complete");
                    m.put("steps", machine.calibrateAll());
                    return m;
                }
                default:
                    if (p.startsWith("/actuators/") && p.endsWith("/jog")) {
                        String id = p.substring("/actuators/".length(), p.length() - "/jog".length());
                        return ok(machine.jogActuator(id));
                    }
                    throw notFound(r);
            }
        }
    }
}
