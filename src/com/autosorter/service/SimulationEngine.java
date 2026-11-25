package com.autosorter.service;

import com.autosorter.exception.AutoSorterException;
import com.autosorter.exception.InvalidCommandException;
import com.autosorter.exception.MachineStateException;
import com.autosorter.model.Colour;
import com.autosorter.model.MachineState;

import java.util.Random;
import java.util.concurrent.*;

/**
 * MULTITHREADING (producer / consumer):
 *   - feeder task   (every 3 s)  : puts a pseudo item on the queue while the machine is RUNNING
 *   - sensor poller (every 250ms): reads all sensors so the live waveform keeps moving
 *   - worker thread              : takes items from the queue and runs the 5-stage pipeline
 * A BlockingQueue hands work between threads safely; no manual wait/notify needed.
 */
public class SimulationEngine implements AutoCloseable {
    private static final long FEED_INTERVAL_MS = 3000;
    private static final long POLL_INTERVAL_MS = 250;
    private static final double[] WEIGHTS = {0.34, 0.28, 0.22, 0.16};      // RED, GREEN, YELLOW, OTHER

    private final SortingMachine machine;
    private final BlockingQueue<Colour> feedQueue = new LinkedBlockingQueue<>(3);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "sim-scheduler");
        t.setDaemon(true);
        return t;
    });
    private final Random rng = new Random();
    private Thread worker;

    public SimulationEngine(SortingMachine machine) { this.machine = machine; }

    public void start() {
        worker = new Thread(this::workerLoop, "sorting-worker");
        worker.setDaemon(true);
        worker.start();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (machine.getState() == MachineState.RUNNING && feedQueue.isEmpty()) feedQueue.offer(pickColour());
            } catch (RuntimeException e) { System.err.println("[feeder] " + e); }
        }, 1500, FEED_INTERVAL_MS, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            try { machine.pollSensors(); } catch (RuntimeException e) { System.err.println("[poller] " + e); }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void workerLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Colour expected = feedQueue.take();
                if (machine.getState() == MachineState.EMERGENCY_HALT) continue;    // drop stale items
                machine.processItem(expected);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (AutoSorterException e) {
                System.out.println("[worker] " + e.getMessage());                  // e.g. cycle aborted by E-stop
            } catch (RuntimeException e) {
                System.err.println("[worker] unexpected: " + e);
            }
        }
    }

    /** "SIMULATE NEXT ITEM" button. */
    public void requestManualFeed() {
        if (machine.getState() == MachineState.EMERGENCY_HALT)
            throw new MachineStateException("Emergency stop engaged - reset before feeding items");
        if (!feedQueue.offer(pickColour()))
            throw new InvalidCommandException("Feed queue is full - wait for the current cycle");
    }

    private Colour pickColour() {
        double x = rng.nextDouble(), acc = 0;
        Colour[] all = Colour.values();
        for (int i = 0; i < all.length; i++) {
            acc += WEIGHTS[i];
            if (x < acc) return all[i];
        }
        return Colour.OTHER;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        if (worker != null) worker.interrupt();
    }
}
