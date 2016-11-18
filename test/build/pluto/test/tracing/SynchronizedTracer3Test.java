package build.pluto.test.tracing;

import build.pluto.test.build.ScopedBuildTest;
import build.pluto.test.build.ScopedPath;
import build.pluto.tracing.*;
import org.apache.commons.exec.util.StringUtils;
import org.fusesource.jansi.Ansi;
import org.junit.AfterClass;
import org.junit.Test;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.util.ArrayUtils;
import org.sugarj.common.util.Predicate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static build.pluto.test.build.Validators.list;
import static build.pluto.test.build.Validators.validateThat;

/**
 * Created by manuel on 9/21/16.
 */
public class SynchronizedTracer3Test extends ScopedBuildTest {

    private boolean find(List<FileDependency> deps, Predicate<FileDependency> predicate) {
        for (FileDependency d : deps) {
            if (predicate.isFullfilled(d))
                return true;
        }
        return false;
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
    }

    @Test
    public void testSynchronized10000Batched() throws ITracer.TracingException, IOException, InterruptedException {
        Log.log.setLoggingLevel(Log.CORE);
        ITracer tracer = TracingProvider.getTracer();

        tracer.ensureStarted();

        for (int i = 0; i < 100; i++) {
            Log.log.log("Reading: " + (i * 1000) + " to " + (i * 1000 + 999), Log.CORE);
            for (int j = 0; j < 1000; j++) {
                final File f = new File("/tmp/" + (i * 1000 + j) + ".notexisting");
                try {
                    FileCommands.readFileLines(f);
                } catch (IOException e) {
                }
            }
            List<FileDependency> deps = tracer.popDependencies();
            for (int j = 0; j < 1000; j++) {
                final File f = new File("/tmp/" + (i * 1000 + j) + ".notexisting");
                assert (find(deps, new Predicate<FileDependency>() {
                    @Override
                    public boolean isFullfilled(FileDependency fileDependency) {
                        return fileDependency.getFile().getAbsoluteFile().toString().equals(f.getAbsoluteFile().toString());
                    }
                }));
            }
        }
    }

    @Test
    public void testSynchronizedBigBatched() throws ITracer.TracingException, IOException, InterruptedException {
        Log.log.setLoggingLevel(Log.CORE);
        ITracer tracer = TracingProvider.getTracer();

        tracer.ensureStarted();

        Log.log.log("Reading: " + 0 + " to " + 99999, Log.CORE);
        for (int j = 0; j < 100000; j++) {
            final File f = new File("/tmp/" + j + ".notexisting");
            try {
                if (j % 1000 == 0)
                    Log.log.log("Reading: " + j, Log.CORE);
                FileCommands.readFileLines(f);
            } catch (IOException e) {
            }
        }
        List<FileDependency> deps = tracer.popDependencies();
        Log.log.log("Read " + deps.size() + " dependencies...", Log.CORE);
        for (int j = 0; j < 100000; j++) {
            final String filePath = "/tmp/" + j + ".notexisting";

            boolean found = false;
            for (int i = 0; i < deps.size(); i++) {
                if (deps.get(i).getFile().getAbsolutePath().equals(filePath)) {
                    deps.remove(i);
                    found = true;
                    break;
                }
            }
            assert (found);
        }
        Log.log.log("Stray dependencies: " + deps, Log.CORE, Ansi.Color.RED);
    }

    @Test
    public void testMultithreadedTracing() throws ITracer.TracingException, InterruptedException {
        final int threads = 50;
        final int par_threads = 2;
        final int files = 10;

        Log.log.setLoggingLevel(Log.ALWAYS);
        ITracer tracer = TracingProvider.getTracer();

        tracer.ensureStarted();

        CountDownLatch doneSignal = new CountDownLatch(threads);

        Executor e = new ScheduledThreadPoolExecutor(par_threads);

        for (int i = 0; i < threads; ++i) // create and start threads
            e.execute(new Thread(new Worker(doneSignal, i, files)));

        doneSignal.await();           // wait for all to finish

        List<FileDependency> deps = tracer.popDependencies();
        for (int i = 0; i < threads; i++) {
            for (int j = 0; j < files; j++) {
                boolean found = false;
                for (int k = 0; k < deps.size(); k++) {
                    final String filePath = "/tmp/thread" + i + "_" + j + ".notexisting";
                    if (deps.get(k).getFile().getAbsolutePath().equals(filePath)) {
                        deps.remove(k);
                        found = true;
                        break;
                    }
                }
                assert (found);
            }
        }
    }


    class Worker implements Runnable {
        private final CountDownLatch doneSignal;
        private final int num;
        private final int num_files;
        Worker(CountDownLatch doneSignal, int num, int num_files) {
            this.doneSignal = doneSignal;
            this.num = num;
            this.num_files = num_files;
        }
        public void run() {
            doWork();
            doneSignal.countDown();
        }

        void doWork() {
            Log.log.log("Worker " + num + " started...", Log.CORE);
            for (int i = 0; i < num_files; i++) {
                final File f = new File("/tmp/thread" + num + "_" + i + ".notexisting");
                try {
                    Log.log.log("Worker " + num + " reading: " + f.toString(), Log.CORE);
                    FileCommands.readFileLines(f);
                } catch (IOException e) {
                }
            }
            Log.log.log("Worker " + num + " done.", Log.CORE);
        }
    }

    @AfterClass
    public static void stopTracer() {
        TracingProvider.getTracer().stop();
    }
}
