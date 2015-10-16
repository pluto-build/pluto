package build.pluto.test.dependency;

import java.io.File;

import build.pluto.builder.BuildUnitProvider;
import build.pluto.dependency.RemoteRequirement;

public class MockRemoteRequirement extends RemoteRequirement {

    private static final long serialVersionUID = 6725526956528720781L;
    
    private boolean isConsistentWithRemote = false;
    private long ts = 0;

    public MockRemoteRequirement(
            File persistentPath,
            long consistencyCheckInterval) {
        super(persistentPath, consistencyCheckInterval);
        this.ts = ts;
    }

    public void setIsConsistentWithRemote(boolean value) {
        this.isConsistentWithRemote = value;
    }

    public void setTs(long ts) {
        this.ts = ts;
    }

    @Override
    protected boolean isConsistentWithRemote() {
        return this.isConsistentWithRemote;
    }

    @Override 
    protected long getStartingTimestamp() {
        return this.ts;
    }

    @Override
    public boolean tryMakeConsistent(BuildUnitProvider provider) {
        return this.isConsistent();
    }
}
