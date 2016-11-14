package build.pluto.test.tracing;

import build.pluto.tracing.*;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by manuel on 9/28/16.
 */
public class TraceNonExistingFileAccessesTest {

    @Test
    public void testNonExistingFileAccesses() throws Exception {
        ITracer t = TracingProvider.getTracer();
        t.ensureStarted();

        // test read nonexisting file
        File nonExistent = new File("/this/does/probably/not/exist");
        assert(!nonExistent.exists());

        try {
            FileCommands.readFileAsString(nonExistent);
        } catch (IOException e) {

        }

        List<FileDependency> deps = t.popDependencies();

        assert(deps.size() > 0);
        boolean foundFile = false;
        for (FileDependency d: deps) {
            if (d.getFile().getAbsoluteFile().equals(nonExistent.getAbsoluteFile()) && d.getMode() == FileAccessMode.READ_MODE && !d.getFileExisted())
                foundFile = true;
        }
        if (!foundFile) throw new Exception("File that didn't exist was not traced...");

        t.stop();
    }
}
