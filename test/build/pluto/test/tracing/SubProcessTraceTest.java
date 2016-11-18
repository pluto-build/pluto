package build.pluto.test.tracing;

import build.pluto.tracing.FileDependency;
import build.pluto.tracing.ITracer;
import build.pluto.tracing.TracingProvider;
import org.junit.Test;
import org.sugarj.common.Exec;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.util.Predicate;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by manuel on 18.11.16.
 */
public class SubProcessTraceTest {

    private boolean find(List<FileDependency> deps, Predicate<FileDependency> predicate) {
        for (FileDependency d : deps) {
            if (predicate.isFullfilled(d))
                return true;
        }
        return false;
    }

    @Test
    public void testSubProcess1() throws ITracer.TracingException, IOException, InterruptedException {
        Log.log.setLoggingLevel(Log.ALWAYS);
        ITracer tracer = TracingProvider.getTracer();

        tracer.ensureStarted();

        for (int i = 0; i < 10; i++) {
            final File f = new File("/tmp/" + i + ".notexisting");
            Log.log.log("Reading: " + f, Log.DETAIL);
            try {
                Exec.run("cat", f.getAbsolutePath());
            } catch (Exec.ExecutionError ex) {

            }

            List<FileDependency> deps = tracer.popDependencies();
            Log.log.log(deps, Log.DETAIL);
            assert (find(deps, new Predicate<FileDependency>() {
                @Override
                public boolean isFullfilled(FileDependency fileDependency) {
                    return fileDependency.getFile().getAbsoluteFile().toString().equals(f.getAbsoluteFile().toString());
                }
            }));
        }

        tracer.stop();
    }
}
