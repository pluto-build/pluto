package build.pluto.test.dependency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Test;
import org.sugarj.common.FileCommands;

import build.pluto.dependency.RemoteRequirement;

public class RemoteRequirementTest {

    private final File tsPath = new File("./build.ts");

    @After
    public void destory() {
        tsPath.delete();
    }

    @Test
    public void checkToEarlyAndInconsistent() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 5000L);

        boolean consistency = writeTSOnce(req, 1000L, false);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(true, consistency);
        assertEquals(0L, contentOfFile);
    }

    @Test
    public void checkToEarlyAndConsistent() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 5000L);

        boolean consistency = writeTSOnce(req, 1000L, true);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(true, consistency);
        assertEquals(0L, contentOfFile);
    }

    @Test
    public void checkAfterIntervalAndInconsistent() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 2000L);

        boolean consistency = writeTSOnce(req, 3500L, false);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(false, consistency);
        assertEquals(0L, contentOfFile);
    }

    @Test
    public void checkAfterIntervalAndConsistent() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 1000L);

        boolean consistency = writeTSOnce(req, 6000L , true);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(true, consistency);
        assertEquals(6000L, contentOfFile);
    }

    @Test
    public void checkAfterTSWrittenOnceButToEarly() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 5000L);

        writeTSOnce(req, 6000L, true);

        boolean consistency = writeTSOnce(req, 10000L, true);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(true, consistency);
        assertEquals(6000L, contentOfFile);
    }

    @Test
    public void checkAfterTSWrittenOnceButToEarlyAndInconsistent() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 5000L);

        writeTSOnce(req, 6000L, true);

        boolean consistency = writeTSOnce(req, 10000L, false);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(true, consistency);
        assertEquals(6000L, contentOfFile);
    }

    @Test
    public void checkAfterTSWrittenOnceAfterInterval() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 5000L);
        writeTSOnce(req, 6000L, true);

        boolean consistency = writeTSOnce(req, 13000L, true);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(true, consistency);
        assertEquals(13000L, contentOfFile);
    }

    @Test
    public void checkAfterTSWrittenOnceAfterIntervalAndInconsistent() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 5000L);
        writeTSOnce(req, 6000L, true);

        boolean consistency = writeTSOnce(req, 13000L, false);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(false, consistency);
        assertEquals(6000L, contentOfFile);
    }

    @Test
    public void checkNeverTestForConsistencyWithRemote() {
        MockRemoteRequirement req = new MockRemoteRequirement(
                tsPath,
                RemoteRequirement.NEVER_CHECK);
        writeTSOnce(req, 6000L, true);

        boolean consistency = writeTSOnce(req, 13000L, false);
        assertEquals(true, consistency);

        consistency = writeTSOnce(req, Long.MAX_VALUE, false);
        assertEquals(true, consistency);
    }

    @Test(expected=IllegalArgumentException.class)
    public void checkIntervalInvalid() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, -2L);
    }

    @Test
    public void checkRemoteNotAccessibleAndLocalVersionAvailable() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 5000L);
        writeTSOnce(req, 6000L, true);
        req.setIsRemoteAccessible(false);
        req.setIsLocalAvailable(true);
        boolean consistency = writeTSOnce(req, 13000L, false);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(true, consistency);
        assertEquals(6000L, contentOfFile);
    }

    @Test
    public void checkRemoteNotAccessibleAndLocalVersionNotAvailable() {
        MockRemoteRequirement req = new MockRemoteRequirement(tsPath, 5000L);
        writeTSOnce(req, 6000L, true);
        req.setIsRemoteAccessible(false);
        req.setIsLocalAvailable(false);
        boolean consistency = writeTSOnce(req, 13000L, false);
        long contentOfFile = readTimestampFromFile(tsPath);

        assertEquals(false, consistency);
        assertEquals(6000L, contentOfFile);
    }

    private boolean writeTSOnce(
            MockRemoteRequirement req,
            long ts,
            boolean isConsistentWithRemote) {
        req.setTs(ts);
        req.setIsConsistentWithRemote(isConsistentWithRemote);
        return req.isConsistent();
    }

    private long readTimestampFromFile(File file) {
        try {
            String persistentPathContent =
                FileCommands.readFileAsString(file);
            return Long.parseLong(persistentPathContent.replace("\n", ""));
        } catch(IOException e) {
            fail("File that contains timestamp could not be read");
        } catch(NumberFormatException e) {
            fail("Timestamp was not in the correct format");
        }
        return -1L;
    }
}
