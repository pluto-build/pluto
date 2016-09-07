package build.pluto.tracing;

import build.pluto.util.SystemUtils;
import org.sugarj.common.Exec;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Manuel Weiel on 9/7/16.
 */
public class Tracer {

    private Tracer() {

    }

    private static Tracer instance;

    public static Tracer getInstance() {
        if (instance == null)
            instance = new Tracer();
        return instance;
    }


    public class TracingException extends Exception {
        public TracingException(String msg) {
            super(msg);
        }
    }


    Exec.NonBlockingExecutionResult result;

    /**
     * Starts tracing file dependencies. Starts strace if necessary and attaches it to the current process
     */
    public void start() throws TracingException {
        int pid = SystemUtils.getCurrentProcessID();

        if (pid == -1) {
            throw new TracingException("Current Process PID could not be retrieved.");
        }

        // TODO: check if stracing is possible, not just parse correct lines in stop().
        result = Exec.runNonBlocking("strace", "-f", "-q", "-e", "trace=open", "-t", "-p", Integer.toString(pid));
    }

    /**
     * Stops tracing and returns all traced file dependencies
     */
    public List<FileDependency> stop() {
        // TODO: check here
        result.kill();
        STraceParser p = new STraceParser(result.errMsgs);
        return p.readDependencies();
    }

}
