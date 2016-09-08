package build.pluto.tracing;

import build.pluto.util.SystemUtils;
import org.sugarj.common.Exec;

import java.io.File;
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

    private void runTracer() throws TracingException {
        int pid = SystemUtils.getCurrentProcessID();

        if (pid == -1) {
            throw new TracingException("Current Process PID could not be retrieved.");
        }

        // TODO: check if stracing is possible, not just parse correct lines in stop().
        result = Exec.runNonBlocking("strace", "-f", "-q", "-e", "trace=open", "-t", "-p", Integer.toString(pid));
    }

    /**
     * Starts tracing file dependencies. Starts strace if necessary and attaches it to the current process
     */
    public void start() throws TracingException {
        if (result == null)
            runTracer();
    }

    public List<FileDependency> getAllDependencies() throws TracingException {
        if (result == null)
            throw new TracingException("Trace was not running...");
        List<String> resultList = result.peekErrMsgs();
        STraceParser p = new STraceParser(resultList.toArray(new String[resultList.size()]));
        return p.readDependencies();
    }

    int readCount = 0;

    public List<FileDependency> popDependencies() throws TracingException {
        if (result == null)
            throw new TracingException("Trace was not running...");
        List<String> errMsgs = new ArrayList<>(result.peekErrMsgs());
        List<String> newMsgs = errMsgs.subList(readCount, errMsgs.size());
        STraceParser p = new STraceParser(newMsgs.toArray(new String[newMsgs.size()]));
        readCount = errMsgs.size();
        return p.readDependencies();
    }

    /**
     * Stops tracing and returns all traced file dependencies
     */
    public List<FileDependency> stop() {
        // TODO: check here
        result.kill();
        STraceParser p = new STraceParser(result.errMsgs);
        result = null;
        return p.readDependencies();
    }

}
