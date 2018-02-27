package build.pluto.tracing;

import org.fusesource.jansi.Ansi;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

import static build.pluto.builder.Builder.PLUTO_HOME;

/**
 * Created by manuel on 9/20/16.
 */
public class SynchronizedTracer implements ITracer {

    private ITracer baseTracer;
    private HashSet<FileDependency> buffer;
    //private File lastDummyFile;
    private static int TIMEOUT = 10000;
    private static int MAX_RETRIES = 3;

    public SynchronizedTracer(ITracer baseTracer) {
        this.baseTracer = baseTracer;
        this.buffer = new HashSet<>();
    }

    Random r = new Random();

    private File newDummyFile() {
        return new File(PLUTO_HOME + "/" + r.nextInt() + "synchronize.plutodummy");
    }

    @Override
    public void ensureStarted() throws TracingException {
        File dummy = newDummyFile();
        try {
            FileCommands.readFileLines(dummy);
        } catch (IOException e) {
        }
        this.buffer = new HashSet<>();
        int tries = 0;
        while (tries < MAX_RETRIES) {
            try {
                baseTracer.ensureStarted();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                synchronize(newDummyFile());
                tries = MAX_RETRIES;
            }
            catch (TracingException te) {
                Log.log.log("Couldn't synchronize tracer... Trying again.", Log.DETAIL, Ansi.Color.RED);
                tries++;
                if (tries == MAX_RETRIES)
                    throw te;
            }
        }
        this.buffer = new HashSet<>();
        Log.log.log("Tracer started and synchronized...", Log.DETAIL);
    }

    @Override
    public void start() throws TracingException {
        baseTracer.start();
    }

    public HashSet<FileDependency> synchronize(File dummy) throws TracingException {
        try {
            FileCommands.readFileAsString(dummy);
        } catch (IOException e) {
        }

        HashSet<FileDependency> result = new HashSet<>();
        result.addAll(buffer);
        buffer.clear();

        long start = System.currentTimeMillis();

        boolean foundDummy = false;
        while (!foundDummy) {
            for (FileDependency fd : baseTracer.popDependencies()) {
                if (foundDummy)
                    buffer.add(fd);
                else if (fd.getFile().getAbsolutePath().equals(dummy.getAbsolutePath())) {
                    foundDummy = true;
                } else if (!fd.getFile().getAbsolutePath().endsWith("synchronize.plutodummy")) {
                    result.add(fd);
                }
            }
            if (System.currentTimeMillis() - start > TIMEOUT)
                throw new TracingException("Could not synchronize tracer... Maybe tracer is not running anymore?");
        }
        return result;
    }

    @Override
    public HashSet<FileDependency> popDependencies() throws TracingException {
        return synchronize(newDummyFile());
    }

    @Override
    public void stop() {
        this.buffer = new HashSet<>();
        baseTracer.stop();
    }

    @Override
    public boolean isRunning() {
        return baseTracer.isRunning();
    }
}
