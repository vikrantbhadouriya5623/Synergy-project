# AutoSorter // Conveyor Control – Java Backend

**Protocol: HTTP/1.1 with JSON (REST API).** The browser calls the Java server with `fetch()`; the same
Java server also serves the dashboard HTML. No external libraries – only the JDK
(`com.sun.net.httpserver`, `java.util.concurrent`, `java.nio.file`).

## Run (JDK 17+)
    ./run.sh            (Windows: run.bat)
    open http://localhost:8080/
    login: engineer@autosorter.lab / viva2025      (student@autosorter.lab / student123 cannot calibrate)

## Folder layout
    src/com/autosorter/
      model/      Colour, MachineState, PipelineStage, Role, RgbReading, SortingResult, User, JsonSerializable
      exception/  AutoSorterException (base) + Authentication / Authorization / MachineState /
                  InvalidCommand / ResourceNotFound / SensorFault
      hardware/   Calibratable, Sensor (abstract) -> ColourSensor, UltrasonicSensor, PirSensor
                  Actuator (abstract) -> ServoActuator -> DiverterServo, FeederGateServo ; DcMotorActuator
      service/    Machine (abstract) -> SortingMachine, ColourClassifier -> ThresholdColourClassifier,
                  SortingStatistics, SortingRepository -> CsvSortingRepository, AuthService,
                  SimulationEngine (threads), DashboardService, ComponentCatalog
      http/       BaseHandler (abstract) -> Handlers.*, JsonWriter, JsonReader, StaticFileHandler
    public/       index.html (your frontend + 10 element ids) and autosorter-bridge.js
    logs/         sorting_log.csv is created at runtime

## API (all except health/login need  Authorization: Bearer <token>)
| Method | URL | Feeds this part of the UI |
|---|---|---|
| GET  | /api/health | – |
| POST | /api/auth/login {email,password} | Login form |
| POST | /api/auth/logout, GET /api/auth/me | Logout buttons |
| GET  | /api/dashboard | Status badge, pipeline, counters, bins, sensor cards, analytics, SVG readout |
| GET  | /api/machine/status | Machine Status tab (temp, voltages, runtime, actuator table) |
| POST | /api/machine/start · pause · estop · reset | START / PAUSE / EMERGENCY STOP / RESET buttons |
| POST | /api/machine/feed-next | SIMULATE NEXT ITEM |
| POST | /api/machine/calibrate | CALIBRATE ALL / Run White Balance (EXAMINER only) |
| POST | /api/machine/actuators/{SERVO-01 or SERVO-02 or MOTOR-DC-01}/jog | Manual Jog buttons |
| GET  | /api/colour | Colour Detection tab (R/G/B raw, ranges, decision matrix) |
| GET  | /api/sensors | Sensor Telemetry tab (waveform, spectrum, SNR) |
| GET  | /api/components | System Components tab |
| GET  | /api/logs?limit=20 | Sorting history (matches the Logs tab of your earlier IoT dashboard) |

Errors are JSON: `{"error":"MachineStateException","message":"..."}` with HTTP 400/401/403/404/409/503.

## How the pseudo data works (so you can explain it)
Every 3 s a feeder thread drops a random item (weights 34/28/22/16 %) on a `BlockingQueue`.
A worker thread takes it and runs the 5 stages (450 ms each): HC-SR04 sees the item (~8.4 cm, below the
10 cm threshold) -> TCS3200 generates RGB = palette colour + Gaussian noise + slow drift ->
`ThresholdColourClassifier` applies the exact rules from your Colour tab -> `DiverterServo` moves to
-18 / 0 / +20 deg -> counters, CSV log and accuracy update. ~1.2 % of samples are "glare" and get
misclassified, so accuracy drifts naturally around 98 %. Echo time = distance x 58.2 us (real physics).

## OOP principles -> where to point in the viva
| Principle | Example |
|---|---|
| Encapsulation | `SortingResult` immutable; `User` stores only a SHA-256 hash; `Machine.state` private, changed only by guarded methods; `SortingStatistics` private counters behind synchronized methods |
| Inheritance | `ColourSensor/UltrasonicSensor/PirSensor extends Sensor`; `SortingMachine extends Machine`; `DiverterServo extends ServoActuator extends Actuator` |
| Polymorphism | `Sensor.readRaw()` / `Actuator.jog()` overridden per type; `List<Calibratable>` mixes sensors and actuators in `calibrateAll()`; every handler is a `BaseHandler`; `JsonWriter` calls `toJson()` on any `JsonSerializable` |
| Abstraction | abstract `Sensor`, `Actuator`, `Machine`, `BaseHandler`; interfaces `Calibratable`, `ColourClassifier` (Strategy), `SortingRepository` |
| Also covered | Exception handling (custom hierarchy), Collections/generics (`EnumMap`, `Deque`, `ConcurrentHashMap`, `BlockingQueue`), File I/O (CSV), Multithreading (`ScheduledExecutorService` + worker), Networking (`HttpServer`) |

## Honest limitations
* All sensor values are simulated. To use real hardware, add a class that reads the ESP32/Arduino
  (serial or HTTP POST) and feeds `SortingMachine` instead of `SimulationEngine`.
* No JDBC: persistence is CSV through the `SortingRepository` interface, so a `JdbcSortingRepository`
  can be added without changing any other class.
* Bin #4 "OTHER" uses flap angle 0 deg (pass-through) because the UI only lists three deflection angles.
