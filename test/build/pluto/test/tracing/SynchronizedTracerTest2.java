package build.pluto.test.tracing;

import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.tracing.*;
import org.junit.AfterClass;
import org.junit.Test;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.util.Predicate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static build.pluto.test.build.Validators.validateThat;
import static build.pluto.test.build.Validators.list;

/**
 * Created by manuel on 9/21/16.
 */
public class SynchronizedTracerTest2 extends ScopedBuildTest {

    private boolean find(List<FileDependency> deps, Predicate<FileDependency> predicate) {
        for (FileDependency d : deps) {
            if (predicate.isFullfilled(d))
                return true;
        }
        return false;
    }

    @ScopedPath("test.txt")
    private File testFile;

    @ScopedPath("test2.txt")
    private File testFile2;

    @ScopedPath("tracerLog.txt")
    private File tracerLog;


    private class DummyTracer implements ITracer {

        private List<List<FileDependency>> popList;

        public DummyTracer(List<List<FileDependency>> popList) {
            this.popList = popList;
        }

        @Override
        public void ensureStarted() throws TracingException {

        }

        @Override
        public void start() throws TracingException {

        }

        @Override
        public List<FileDependency> popDependencies() throws TracingException {
            if (popList.size() == 0)
                throw new TracingException("No more elements");
            return popList.remove(0);
        }

        @Override
        public void stop() {

        }

        @Override
        public boolean isRunning() {
            return true;
        }
    }

    @Test
    public void testSynchronize() throws ITracer.TracingException {
        List<FileDependency> files = new ArrayList<>();
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/some"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/files"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/1synchronize.plutodummy"), new Date()));
        List<List<FileDependency>> popList = new ArrayList<>();
        popList.add(files);

        SynchronizedTracer tracer = new SynchronizedTracer(new DummyTracer(popList));
        validateThat(list(tracer.synchronize(new File("/tmp/1synchronize.plutodummy"))).containsSameElements(files.get(0), files.get(1)));
    }

    @Test
    public void testSynchronizeIgnoreOtherDummies() throws ITracer.TracingException {
        List<FileDependency> files = new ArrayList<>();
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/some"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/files"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/102034053969495synchronize.plutodummy"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/1synchronize.plutodummy"), new Date()));
        List<List<FileDependency>> popList = new ArrayList<>();
        popList.add(files);

        SynchronizedTracer tracer = new SynchronizedTracer(new DummyTracer(popList));
        validateThat(list(tracer.synchronize(new File("/tmp/1synchronize.plutodummy"))).containsSameElements(files.get(0), files.get(1)));
    }

    @Test
    public void testSynchronizeDontIgnoreFilesAfterDummy() throws ITracer.TracingException {
        List<FileDependency> files = new ArrayList<>();
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/some"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/files"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/1synchronize.plutodummy"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/other"), new Date()));
        files.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/other2"), new Date()));

        List<List<FileDependency>> popList = new ArrayList<>();
        popList.add(files);

        List<FileDependency> files2 = new ArrayList<>();
        files2.add(new FileDependency(FileAccessMode.READ_MODE, new File("/tmp/2synchronize.plutodummy"), new Date()));
        popList.add(files2);

        SynchronizedTracer tracer = new SynchronizedTracer(new DummyTracer(popList));
        validateThat(list(tracer.synchronize(new File("/tmp/1synchronize.plutodummy"))).containsSameElements(files.get(0), files.get(1)));
        validateThat(list(tracer.synchronize(new File("/tmp/2synchronize.plutodummy"))).containsSameElements(files.get(3), files.get(4)));
    }

    @Test
    public void testSynchronized500() throws ITracer.TracingException, IOException, InterruptedException {
        Log.log.setLoggingLevel(Log.ALWAYS);
        ITracer tracer = TracingProvider.getTracer();

        tracer.ensureStarted();

        for (int i = 0; i < 500; i++) {
            final File f = new File("/tmp/" + i + ".notexisting");
            Log.log.log("Reading: " + f, Log.DETAIL);
            try {
                FileCommands.readFileLines(f);
            } catch (IOException e) {
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
        /*Thread.sleep(2000);
        List<FileDependency> deps = tracer.popDependencies();
        Log.log.log(deps, Log.DETAIL);*/
    }

    @AfterClass
    public static void stopTracer() {
        TracingProvider.getTracer().stop();
    }
}
