package build.pluto.util;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by Manuel Weiel on 9/7/16.
 */
public class SystemUtils {

    /**
     * Retrieve the current process id.
     * @return the pid, or -1 if retrieval fails
     */
    public static int getCurrentProcessID() {
        try {
            java.lang.management.RuntimeMXBean runtime =
                    java.lang.management.ManagementFactory.getRuntimeMXBean();
            java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            sun.management.VMManagement mgmt =
                    (sun.management.VMManagement) jvm.get(runtime);
            java.lang.reflect.Method pid_method =
                    mgmt.getClass().getDeclaredMethod("getProcessId");
            pid_method.setAccessible(true);

            int pid = (Integer) pid_method.invoke(mgmt);
            return pid;
        }
        catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            return -1;
        }
    }
}
