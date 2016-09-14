package build.pluto.tracing;

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

    List<FileDependency> popDependencies() throws TracingException;

    void stop();
}
