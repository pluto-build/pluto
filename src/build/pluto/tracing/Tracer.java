package build.pluto.tracing;

import build.pluto.util.SystemUtils;
import org.sugarj.common.Exec;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.StringCommands;
import org.sugarj.common.path.AbsolutePath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Manuel Weiel on 9/7/16.
 */
public class Tracer implements ITracer {

    final File logFile;

    Exec.NonBlockingExecutionResult result;

    public Tracer() {
        this.logFile = null;
    }

    public Tracer(File logFile) {
        this.logFile = logFile;
    }

    @Override
    public void start() throws TracingException {

        if (logFile != null && logFile.exists())
            logFile.delete();


        int pid = SystemUtils.getCurrentProcessID();

        if (pid == -1) {
            throw new TracingException("Current Process PID could not be retrieved.");
        }

        // TODO: check if stracing is possible, not just parse correct lines in stop().
        //result = Exec.runNonBlocking("strace", "-f", "-q", "-e", "trace=open", "-ttt", "-p", Integer.toString(pid));
        result = new Exec(false).runNonBlockingWithPrefix("strace", null, "strace", "-f", "-q", "-e", "trace=open", "-ttt", "-p", Integer.toString(pid));
    }

    /**
     * Starts tracing file dependencies. Starts strace if necessary and attaches it to the current process
     */
    @Override
    public void ensureStarted() throws TracingException {
        /*try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        if (result == null)
            start();
        popDependencies();
    }

    public HashSet<FileDependency> getAllDependencies() throws TracingException {
        if (result == null)
            throw new TracingException("Trace was not running...");
        List<String> resultList = result.peekErrMsgs();
        STraceParser p = new STraceParser(resultList.toArray(new String[resultList.size()]));
        return p.readDependencies();
    }

    int readCount = 0;

    @Override
    public HashSet<FileDependency> popDependencies() throws TracingException {
        /*try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        if (result == null)
            throw new TracingException("Trace was not running...");
        /*List<String> errMsgs = new ArrayList<>(result.peekErrMsgs());
        List<String> newMsgs = errMsgs.subList(readCount, errMsgs.size());
        STraceParser p = new STraceParser(newMsgs.toArray(new String[newMsgs.size()]));
        readCount = errMsgs.size();
        return p.readDependencies();*/
        List<String> newMsgs = new ArrayList<>(result.popErrMsgs());

        if (logFile != null && newMsgs.size() > 0) {
            try {
                FileCommands.appendToFile(new AbsolutePath(logFile.getAbsolutePath()), StringCommands.printListSeparated(newMsgs, "\n") + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        STraceParser p = new STraceParser(newMsgs.toArray(new String[newMsgs.size()]));

        return p.readDependencies();
    }

    /**
     * Stops tracing and returns all traced file dependencies
     */
    @Override
    public void stop() {
        // TODO: check here
        if (result != null) {
            Log.log.log("Stopping tracer...", Log.DETAIL);
            result.kill();
            readCount = 0;
            result = null;
            Log.log.log("Tracer stopped...", Log.DETAIL);
        }
    }

    @Override
    public boolean isRunning() {
        return result != null;
    }

}
