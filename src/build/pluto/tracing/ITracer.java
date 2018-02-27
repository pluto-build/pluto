package build.pluto.tracing;

import java.util.HashSet;
import java.util.List;

/**
 * Created by manuel on 9/14/16.
 */
public interface ITracer {
    class TracingException extends Exception {
        public TracingException(String msg) {
            super(msg);
        }
    }

    void ensureStarted() throws TracingException;
    void start() throws TracingException;

    HashSet<FileDependency> popDependencies() throws TracingException;

    void stop();

    boolean isRunning();
}
