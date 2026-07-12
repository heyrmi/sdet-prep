package ra.hul.sdet.linux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Execute Shell Commands - Run OS commands via ProcessBuilder, capturing stdout, stderr, exit code and timeouts.
 * Common SDET question: "Run a shell command from Java and capture output, handling hangs and non-zero exits".
 *
 * Self-contained: runs only local, always-present commands (echo, ls, a bogus command, and a sleep for timeout).
 * No network. main() self-verifies with PASS/FAIL. Assumes a Unix-like shell (macOS/Linux).
 */
public class Ques1_ExecuteShellCommands {

    /** Result of a command execution. timedOut=true means it was killed after exceeding the timeout. */
    public record CommandResult(int exitCode, String stdout, String stderr, boolean timedOut) {}

    public static CommandResult run(List<String> command, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();

        // Read stdout and stderr on separate threads so a full pipe buffer can't deadlock us.
        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();
        Thread tOut = streamReader(process, true, out);
        Thread tErr = streamReader(process, false, err);
        tOut.start();
        tErr.start();

        boolean finished = process.waitFor(timeout, unit);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor();
            tOut.join();
            tErr.join();
            return new CommandResult(-1, out.toString(), err.toString(), true);
        }
        tOut.join();
        tErr.join();
        return new CommandResult(process.exitValue(), out.toString(), err.toString(), false);
    }

    private static Thread streamReader(Process p, boolean stdout, StringBuilder sink) {
        return new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(
                    stdout ? p.getInputStream() : p.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sink.append(line).append('\n');
                }
            } catch (IOException ignored) {
                // stream closed on process teardown
            }
        });
    }

    static void main() throws IOException, InterruptedException {
        boolean pass = true;

        // 1) Successful command with stdout.
        CommandResult echo = run(List.of("echo", "hello-sdet"), 5, TimeUnit.SECONDS);
        System.out.println("echo -> exit=" + echo.exitCode() + " stdout=" + echo.stdout().trim());
        pass &= echo.exitCode() == 0 && echo.stdout().trim().equals("hello-sdet") && !echo.timedOut();

        // 2) Non-zero exit + stderr (ls on a path that does not exist).
        CommandResult bad = run(List.of("ls", "/no/such/path/xyz"), 5, TimeUnit.SECONDS);
        System.out.println("ls(bad) -> exit=" + bad.exitCode() + " stderrPresent=" + !bad.stderr().isBlank());
        pass &= bad.exitCode() != 0 && !bad.stderr().isBlank() && !bad.timedOut();

        // 3) Timeout handling (sleep longer than the allowed timeout).
        CommandResult slow = run(List.of("sleep", "5"), 300, TimeUnit.MILLISECONDS);
        System.out.println("sleep -> timedOut=" + slow.timedOut() + " exit=" + slow.exitCode());
        pass &= slow.timedOut();

        System.out.println(pass ? "PASS: stdout, stderr/exit-code, and timeout all handled."
                : "FAIL: command execution behaviour mismatch.");
    }
}
