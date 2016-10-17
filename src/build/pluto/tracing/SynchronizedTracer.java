package build.pluto.tracing;

import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static build.pluto.builder.Builder.PLUTO_HOME;

/**
 * Created by manuel on 9/20/16.
 */
public class SynchronizedTracer implements ITracer {

    private ITracer baseTracer;
    private List<FileDependency> buffer;
    private File dummyFile = new File(PLUTO_HOME + "/dummy.tmp");
    private static int TIMEOUT = 1000;
    private static int MAX_RETRIES = 5;

    public SynchronizedTracer(ITracer baseTracer) {
        this.baseTracer = baseTracer;
        this.buffer = new ArrayList<>();
    }

    private boolean bufferContainsDummy() {
        for (FileDependency d: buffer) {
            if (d.getFile().getAbsoluteFile().equals(dummyFile.getAbsoluteFile())) {
                return true;
            }
        }
        return false;
    }

    private List<FileDependency> clearUpToDummy() {
        List<FileDependency> deps = new ArrayList<>();

        if (bufferContainsDummy()) {
            while (!buffer.get(0).getFile().getAbsoluteFile().equals(dummyFile.getAbsoluteFile())) {
                deps.add(buffer.remove(0));
            }
            buffer.remove(0);
            if (bufferContainsDummy())
                deps.addAll(clearUpToDummy());
        }
        return deps;
    }

    @Override
    public void ensureStarted() throws TracingException {
        try {
            FileCommands.writeToFile(dummyFile, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        baseTracer.ensureStarted();
        this.buffer = new ArrayList<>();
        int tries = 0;
        while (tries < MAX_RETRIES) {
            try {
                synchronize();
                tries = MAX_RETRIES;
            }
            catch (TracingException te) {
                Log.log.log("Couldn't synchronize tracer... Trying again.", Log.DETAIL);
                tries++;
                if (tries == MAX_RETRIES)
                    throw te;
            }
        }
        this.buffer = new ArrayList<>();
    }

    private List<FileDependency> synchronize() throws TracingException {
        try {
            FileCommands.readFileAsString(dummyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        long start = System.currentTimeMillis();
        while (!bufferContainsDummy()) {
            buffer.addAll(baseTracer.popDependencies());
            if (System.currentTimeMillis() - start > TIMEOUT)
                throw new RuntimeException("Could not synchronize tracer... Maybe tracer is not running anymore?");
        }
        return clearUpToDummy();
    }

    @Override
    public List<FileDependency> popDependencies() throws TracingException {
        buffer.addAll(baseTracer.popDependencies());

        return synchronize();
    }

    @Override
    public void pause() throws TracingException {
        buffer.addAll(synchronize());
        baseTracer.pause();
    }

    @Override
    public void unpause() throws TracingException {
        synchronize();
        baseTracer.unpause();
    }

    @Override
    public void stop() {
        this.buffer = new ArrayList<>();
        baseTracer.stop();
    }

    @Override
    public boolean isRunning() {
        return baseTracer.isRunning();
    }
}
