package build.pluto.tracing;

import build.pluto.util.SystemUtils;
import org.apache.commons.exec.*;
import org.sugarj.common.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Manuel Weiel on 9/7/16.
 */
public class TracerCommonsExec implements ITracer {

    final File logFile;

    public TracerCommonsExec() {
        this.logFile = null;
    }

    public TracerCommonsExec(File logFile) {
        this.logFile = logFile;
    }


    LogOutputStream errStream;
    DefaultExecutor executor;

    List<FileDependency> buffer = new ArrayList<>();

    @Override
    public void start() throws TracingException {

        if (logFile != null && logFile.exists())
            logFile.delete();

        if (isRunning())
            stop();


        int pid = SystemUtils.getCurrentProcessID();

        if (pid == -1) {
            throw new TracingException("Current Process PID could not be retrieved.");
        }

        DefaultExecuteResultHandler resultHandler
                = new DefaultExecuteResultHandler();
        executor = new DefaultExecutor();
        errStream = new LogOutputStream() {

            @Override
            protected void processLine(String line, int logLevel) {
                synchronized (buffer) {
                    STraceParser p = new STraceParser(new String[]{line});
                    buffer.addAll(p.readDependencies());
                }
            }
        };
        PumpStreamHandler streamHandler = new PumpStreamHandler(null, errStream);
        executor.setStreamHandler(streamHandler);
        executor.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
        try {
            executor.execute(CommandLine.parse("strace -f -q -e trace=open -ttt -p"+ Integer.toString(pid)),resultHandler);
            Log.log.log("Started tracer...", Log.DETAIL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts tracing file dependencies. Starts strace if necessary and attaches it to the current process
     */
    @Override
    public void ensureStarted() throws TracingException {
        if (!isRunning())
            start();
        popDependencies();
    }

    @Override
    public List<FileDependency> popDependencies() throws TracingException {
        if (!isRunning())
            throw new TracingException("Trace was not running...");

        List<FileDependency> result;
        synchronized (buffer) {
            result = new ArrayList<>(buffer);
            buffer.clear();
        }

        return result;
    }

    /**
     * Stops tracing and returns all traced file dependencies
     */
    @Override
    public void stop() {
        // TODO: check here
        if (executor != null) {
            executor.getWatchdog().destroyProcess();
            executor = null;
            errStream = null;
            Log.log.log("Tracer stopped...", Log.DETAIL);
        }
    }

    @Override
    public boolean isRunning() {
        return executor != null;
    }

}
