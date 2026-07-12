package ra.hul.sdet.linux;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Process Monitor - Inspect processes via the ProcessHandle API: PID, liveness, spawn + monitor, restart-on-death.
 * Common SDET question: "Check if a process is running, read its PID, and restart it when it dies".
 *
 * Self-contained: spawns only short-lived local `sleep` processes; the restart demo is bounded so main() ends.
 * No network. Assumes a Unix-like OS (uses `sleep`). main() self-verifies with PASS/FAIL.
 */
public class Ques3_ProcessMonitor {

    /** True if a process with the given PID currently exists and is alive. */
    public static boolean isRunning(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    /** Spawns a process that lives for the given millis, returning its handle. */
    public static Process spawnShortLived(long millis) throws IOException {
        double seconds = millis / 1000.0;
        return new ProcessBuilder("sleep", String.format("%.3f", seconds))
                .inheritIO().start();
    }

    /**
     * Runs `task` (which spawns and returns a Process), then restarts it whenever it dies,
     * up to maxRestarts times. Bounded so it always terminates. Returns how many times it (re)started.
     */
    public static int superviseWithRestart(Callable<Process> task, int maxRestarts) throws Exception {
        int starts = 0;
        Process p = task.call();
        starts++;
        System.out.println("  started pid=" + p.pid());
        for (int restart = 0; restart < maxRestarts; restart++) {
            p.onExit().get();               // block until this instance dies
            System.out.println("  pid=" + p.pid() + " exited; restarting (" + (restart + 1) + ")");
            p = task.call();
            starts++;
            System.out.println("  started pid=" + p.pid());
        }
        p.onExit().get();                   // let the final instance finish so main can exit cleanly
        return starts;
    }

    static void main() throws Exception {
        boolean pass = true;

        // 1) Current process: PID and info via ProcessHandle.
        ProcessHandle self = ProcessHandle.current();
        System.out.println("Current PID = " + self.pid());
        Optional<String> cmd = self.info().command();
        System.out.println("Current command = " + cmd.orElse("(unknown)"));
        pass &= self.pid() > 0 && self.isAlive();

        // 2) Spawn and monitor a short-lived process.
        Process child = spawnShortLived(200);
        long childPid = child.pid();
        System.out.println("Spawned child pid=" + childPid + " running=" + isRunning(childPid));
        pass &= isRunning(childPid);
        child.onExit().get();
        System.out.println("After exit, running=" + isRunning(childPid));
        pass &= !isRunning(childPid);

        // 3) Restart-on-death demo (bounded to 3 restarts).
        System.out.println("Supervising with restart-on-death:");
        int totalStarts = superviseWithRestart(() -> spawnShortLived(120), 3);
        System.out.println("Total starts = " + totalStarts + " (expected 4)");
        pass &= totalStarts == 4;

        // 4) A PID that (almost certainly) does not exist.
        pass &= !isRunning(999_999_999L);

        System.out.println(pass ? "PASS: PID/liveness, spawn-monitor, and bounded restart all worked."
                : "FAIL: process monitoring mismatch.");
    }

    // Reference: ProcessHandle.allProcesses() lists every visible process; filter by info().command().
    @SuppressWarnings("unused")
    private static List<ProcessHandle> findByCommandContains(String needle) {
        return ProcessHandle.allProcesses()
                .filter(h -> h.info().command().map(c -> c.contains(needle)).orElse(false))
                .toList();
    }
}
