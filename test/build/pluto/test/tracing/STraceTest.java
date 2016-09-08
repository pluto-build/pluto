package build.pluto.test.tracing;

import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.test.build.cycle.fixpoint.FileUtils;
import build.pluto.tracing.FileDependency;
import build.pluto.tracing.FileReadMode;
import build.pluto.tracing.Tracer;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by manuel on 9/7/16.
 */
public class STraceTest extends ScopedBuildTest {

    @ScopedPath("test.txt")
    private File file;

    @Test
    public void testStartingAndStopping() throws Exception {
        Tracer.getInstance().start();
        assert(0 == FileUtils.readIntFromFile(file));
        Thread.sleep(100);
        List<FileDependency> deps = Tracer.getInstance().stop();
        System.out.println(deps);
        assert(deps.size() > 0);
        boolean foundFile = false;
        for (FileDependency d: deps) {
            if (d.getFile().getAbsoluteFile().equals(file.getAbsoluteFile()) && d.getMode() == FileReadMode.READ_MODE)
                foundFile = true;
        }
        if (!foundFile) throw new Exception("Did not trace read file...");
    }

    @ScopedPath("test2.txt")
    private File file2;

    @Test
    public void testPopDependencies() throws Exception {
        Tracer.getInstance().start();
        assert(0 == FileUtils.readIntFromFile(file));
        Thread.sleep(100);
        List<FileDependency> deps1 = Tracer.getInstance().popDependencies();
        System.out.println(deps1);
        assert(deps1.size() > 0);
        boolean foundFile = false;
        for (FileDependency d: deps1) {
            if (d.getFile().getAbsoluteFile().equals(file.getAbsoluteFile()) && d.getMode() == FileReadMode.READ_MODE)
                foundFile = true;
        }
        if (!foundFile) throw new Exception("Did not trace read file...");

        assert(1 == FileUtils.readIntFromFile(file2));
        Thread.sleep(100);

        List<FileDependency> deps2 = Tracer.getInstance().popDependencies();
        assert(deps2.size() > 0);
        foundFile = false;
        boolean foundWrongFile = false;
        for (FileDependency d: deps2) {
            if (d.getFile().getAbsoluteFile().equals(file2.getAbsoluteFile()) && d.getMode() == FileReadMode.READ_MODE)
                foundFile = true;
            if (d.getFile().getAbsoluteFile().equals(file.getAbsoluteFile()) && d.getMode() == FileReadMode.READ_MODE)
                foundWrongFile = true;
        }
        if (!foundFile) throw new Exception("Did not trace read file2...");
        if (foundWrongFile) throw new Exception("Did trace read file that should have been popped...");
    }
}
