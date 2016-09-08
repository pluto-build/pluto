package build.pluto.tracing;

import build.pluto.util.SystemUtils;
import org.sugarj.common.Exec;
import org.sugarj.common.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Manuel Weiel on 9/7/16.
 */
public class Tracer {
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
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (result == null)
            throw new TracingException("Trace was not running...");
        List<String> errMsgs = new ArrayList<>(result.peekErrMsgs());
        List<String> newMsgs = errMsgs.subList(readCount, errMsgs.size());
        STraceParser p = new STraceParser(newMsgs.toArray(new String[newMsgs.size()]));
        readCount = errMsgs.size();
        Log.log.log("New readCount: "+ readCount, Log.ALWAYS);
        return p.readDependencies();
    }

    /**
     * Stops tracing and returns all traced file dependencies
     */
    public List<FileDependency> stop() {
        // TODO: check here
        if (result != null) {
            result.kill();
            readCount = 0;
            STraceParser p = new STraceParser(result.errMsgs);
            result = null;
            return p.readDependencies();
        }
        else
            return new ArrayList<>();
    }

}
