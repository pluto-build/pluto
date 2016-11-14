package build.pluto.test.tracing;

import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.UnitValidators;
import build.pluto.tracing.*;
import org.junit.Test;
import org.sugarj.common.FileCommands;
import org.sugarj.common.util.Predicate;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by manuel on 9/21/16.
 */
public class SynchronizedTracerTest extends ScopedBuildTest {

    private boolean find(List<FileDependency> deps, Predicate<FileDependency> predicate) {
        for (FileDependency d: deps) {
            if (predicate.isFullfilled(d))
                return true;
        }
        return false;
    }

    @ScopedPath("test.txt")
    private File testFile;

    @ScopedPath("test2.txt")
    private File testFile2;

    @Test
    public void testSynchronized1() throws ITracer.TracingException, IOException {
        ITracer tracer = TracingProvider.getTracer();

        tracer.ensureStarted();

        FileCommands.readFileAsString(testFile);

        List<FileDependency> deps = tracer.popDependencies();

        System.out.println(deps);

        assert(find(deps, new Predicate<FileDependency>() {
            @Override
            public boolean isFullfilled(FileDependency fileDependency) {
                return fileDependency.getFile().getAbsoluteFile().equals(testFile.getAbsoluteFile());
            }
        }));

        deps = tracer.popDependencies();

        assert(!find(deps, new Predicate<FileDependency>() {
            @Override
            public boolean isFullfilled(FileDependency fileDependency) {
                return fileDependency.getFile().getAbsoluteFile().equals(testFile.getAbsoluteFile());
            }
        }));

        FileCommands.readFileAsString(testFile);

        deps = tracer.popDependencies();

        System.out.println(deps);

        assert(find(deps, new Predicate<FileDependency>() {
            @Override
            public boolean isFullfilled(FileDependency fileDependency) {
                return fileDependency.getFile().getAbsoluteFile().equals(testFile.getAbsoluteFile());
            }
        }));

        assert(!find(deps, new Predicate<FileDependency>() {
            @Override
            public boolean isFullfilled(FileDependency fileDependency) {
                return fileDependency.getFile().getAbsoluteFile().equals(testFile2.getAbsoluteFile());
            }
        }));

        tracer.stop();
    }
}
