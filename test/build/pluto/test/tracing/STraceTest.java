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
}
