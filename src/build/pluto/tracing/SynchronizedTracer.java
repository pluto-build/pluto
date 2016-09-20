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

    private void clearUpToDummy() {
        if (bufferContainsDummy()) {
            while (!buffer.get(buffer.size()-1).getFile().getAbsoluteFile().equals(dummyFile.getAbsoluteFile())) {
                buffer.remove(buffer.size()-1);
            }
            buffer.remove(buffer.size()-1);
            if (bufferContainsDummy())
                clearUpToDummy();
        }
    }

    @Override
    public void ensureStarted() throws TracingException {
        baseTracer.ensureStarted();
        this.buffer = new ArrayList<>();
        synchronize();
    }

    private void synchronize() throws TracingException {
        try {
            FileCommands.writeToFile(dummyFile, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!bufferContainsDummy()) {
            buffer.addAll(baseTracer.popDependencies());
        }
        clearUpToDummy();
    }

    @Override
    public List<FileDependency> popDependencies() throws TracingException {
        buffer.addAll(baseTracer.popDependencies());

        synchronize();

        return buffer;
    }

    @Override
    public void stop() {
        baseTracer.stop();
    }
}
