package build.pluto.tracing;

/**
 * Created by manuel on 09.11.16.
 */
public class TracingProvider {

    private static ITracer tracer;

    public static ITracer getTracer() {
        if (tracer == null)
            tracer = new SynchronizedTracer(new TracerCommonsExec());
        return tracer;
    }

}
