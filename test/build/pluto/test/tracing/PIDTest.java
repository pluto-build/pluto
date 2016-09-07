package build.pluto.test.tracing;

import build.pluto.util.SystemUtils;
import org.junit.Test;

/**
 * Created by manuel on 9/7/16.
 */
public class PIDTest{
    @Test
    public void testGetCurrentPID() {
        int pid = SystemUtils.getCurrentProcessID();
        System.out.println(pid);
        assert(pid > -1);
    }
}
