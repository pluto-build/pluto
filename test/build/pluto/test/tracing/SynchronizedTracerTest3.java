package build.pluto.test.tracing;

import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.tracing.*;
import org.junit.Test;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.util.Predicate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static build.pluto.test.build.Validators.list;
import static build.pluto.test.build.Validators.validateThat;

/**
 * Created by manuel on 9/21/16.
 */
public class SynchronizedTracerTest3 extends ScopedBuildTest {

    @Test
    public void testSynchronized500() throws ITracer.TracingException, IOException {
        Log.log.setLoggingLevel(Log.ALWAYS);

        for (int i = 0; i < 500; i++) {
            final File f = new File("/tmp/" + i + ".notexisting");
            Log.log.log("Reading: " + f, Log.DETAIL);
            try {
                FileCommands.readFileLines(f);
            } catch (IOException e) {}
        }
        Runtime.getRuntime().gc();
        for (int i = 500; i < 1000; i++) {
            final File f = new File("/tmp/" + i + ".notexisting");
            Log.log.log("Reading: " + f, Log.DETAIL);
            try {
                FileCommands.readFileLines(f);
            } catch (IOException e) {}
        }
    }
}
