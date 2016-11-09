package build.pluto.tracing;

import build.pluto.util.SystemUtils;
import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;
import org.fusesource.jansi.Ansi;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.AbsolutePath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Manuel Weiel on 9/7/16.
 */
public class TracerNuProcess implements ITracer {

    final File logFile;

    NuProcess process;
    ProcessHandler handler;
    StringBuffer msgs = new StringBuffer();

    class ProcessHandler extends NuAbstractProcessHandler {
        private NuProcess nuProcess;

        public final ByteArrayOutputStream stdoutBytes = new ByteArrayOutputStream();
        public final WritableByteChannel stdoutBytesChannel = Channels.newChannel(stdoutBytes);

        @Override
        public void onStart(NuProcess nuProcess) {
            this.nuProcess = nuProcess;
            Log.log.log("Started process: " + nuProcess.getPID(), Log.DETAIL);
            Log.log.log("Pending writes: " + nuProcess.hasPendingWrites(), Log.DETAIL);
            Log.log.log("Process running: " + nuProcess.isRunning(), Log.DETAIL);
        }

        @Override
        public boolean onStdinReady(ByteBuffer buffer) {
            return false; // false means we have nothing else to write at this time
        }

        @Override
        public void onStderr(ByteBuffer buffer, boolean closed) {
            if (!closed) {
                byte[] bytes = new byte[buffer.remaining()];
                // You must update buffer.position() before returning (either implicitly,
                // like this, or explicitly) to indicate how many bytes your handler has consumed.
                buffer.get(bytes);
                String s = new String(bytes);
                Log.log.log(s, Log.DETAIL, Ansi.Color.MAGENTA);
                //synchronized (msgs) {
                msgs.append(s);
                //}
            }
        }
    }

    public TracerNuProcess() {
        this.logFile = null;
    }

    public TracerNuProcess(File logFile) {
        this.logFile = logFile;
    }

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


        NuProcessBuilder pb = new NuProcessBuilder(Arrays.asList("echo", "test", ";", "strace", "-f", "-q", "-e", "trace=open", "-ttt", "-p", Integer.toString(pid)));
        handler = new ProcessHandler();
        pb.setProcessListener(handler);
        process = pb.start();
        process.writeStdin(ByteBuffer.wrap("Hello".getBytes()));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.log.log("Tracer started...", Log.DETAIL);


        // TODO: check if stracing is possible, not just parse correct lines in stop().
        //result = Exec.runNonBlocking("strace", "-f", "-q", "-e", "trace=open", "-ttt", "-p", Integer.toString(pid));
        //result = new Exec(false).runNonBlockingWithPrefix("strace", null, "strace", "-f", "-q", "-e", "trace=open", "-ttt", "-p", Integer.toString(pid));
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
        if (process == null)
            start();
        popDependencies();
    }

    @Override
    public List<FileDependency> popDependencies() throws TracingException {
        /*try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        if (process == null)
            throw new TracingException("Trace was not running...");
        /*List<String> errMsgs = new ArrayList<>(result.peekErrMsgs());
        List<String> newMsgs = errMsgs.subList(readCount, errMsgs.size());
        STraceParser p = new STraceParser(newMsgs.toArray(new String[newMsgs.size()]));
        readCount = errMsgs.size();
        return p.readDependencies();*/
        if (logFile != null && msgs.length() > 0) {
            try {
                FileCommands.appendToFile(new AbsolutePath(logFile.getAbsolutePath()), msgs.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        STraceParser p = new STraceParser(msgs.toString().split("\n"));
        msgs = new StringBuffer();

        return p.readDependencies();
    }

    /**
     * Stops tracing and returns all traced file dependencies
     */
    @Override
    public void stop() {
        // TODO: check here
        if (process != null) {
            Log.log.log("Stopping tracer...", Log.DETAIL);
            process.destroy(true);
            process = null;
            handler = null;
            Log.log.log("Tracer stopped...", Log.DETAIL);
        }
    }

    @Override
    public boolean isRunning() {
        return process != null;
    }

}
