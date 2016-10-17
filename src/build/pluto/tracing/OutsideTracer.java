package build.pluto.tracing;

import org.apache.commons.io.FileUtils;
import org.sugarj.common.FileCommands;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static build.pluto.builder.Builder.PLUTO_HOME;

/**
 * Created by manuel on 9/14/16.
 */
public class OutsideTracer implements ITracer {

    private File dummyFile = new File(PLUTO_HOME + "/dummy.tmp");
    private File tracingFile = new File(PLUTO_HOME + "/tracing.txt");

    public void writeDummyFile() {
        try {
            FileCommands.writeToFile(dummyFile, "dummy");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearTracingFile() {
        try {
            FileUtils.writeStringToFile(tracingFile, "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void ensureStarted() throws TracingException {
        if (tracingFile.exists())
            clearTracingFile();
        writeDummyFile();
        long start = System.currentTimeMillis();
        try {
            while (FileUtils.readLines(tracingFile).size() == 0 && System.currentTimeMillis() - start < 10000) {
                // nop
            }
        } catch (IOException e) {
            throw new TracingException("strace appears not to be running...");
        }
        try {
            if (FileUtils.readLines(tracingFile).size() == 0)
                throw new TracingException("strace appears not to be running...");
        } catch (IOException e) {
            throw new TracingException("strace appears not to be running...");
        }
        List<FileDependency> deps = null;
        boolean notReadDummy = true;
        do {
            deps = popDependencies();
            for (FileDependency d : deps) {
                if (d.getFile().getAbsoluteFile().equals(dummyFile.getAbsoluteFile()))
                    notReadDummy = false;
            }
        } while (deps != null && notReadDummy);
    }

    @Override
    public List<FileDependency> popDependencies() throws TracingException {
        List<String> lines = null;
        List<FileDependency> dependencies = new ArrayList<>();
        try {
            lines = FileCommands.readFileLines(tracingFile);
        } catch (IOException e) {
            throw new TracingException("Could not read tracing file. Check permissions.");
        }
        STraceParser p = new STraceParser(lines.toArray(new String[lines.size()]));
        for (FileDependency d: p.readDependencies())
            if (!d.getFile().getAbsoluteFile().equals(tracingFile.getAbsoluteFile()))
                dependencies.add(d);
        clearTracingFile();
        return dependencies;
    }

    @Override
    public void pause() {
        // TODO: Implement
        throw new NotImplementedException();
    }

    @Override
    public void unpause() {
        // TODO: Implement
        throw new NotImplementedException();
    }

    @Override
    public void stop() {
        // TODO: Is this right?
        if (tracingFile.exists())
            clearTracingFile();
    }

    @Override
    public boolean isRunning() {
        // TODO: Implement
        throw new NotImplementedException();
    }
}
