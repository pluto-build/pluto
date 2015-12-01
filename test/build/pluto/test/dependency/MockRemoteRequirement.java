package build.pluto.test.dependency;

import java.io.File;

import build.pluto.builder.BuildUnitProvider;
import build.pluto.dependency.RemoteRequirement;

public class MockRemoteRequirement extends RemoteRequirement {

    private static final long serialVersionUID = 6725526956528720781L;

    private boolean isConsistentWithRemote = false;
    private long ts = 0;
    private boolean isRemoteAccessible = true;
    private boolean isLocalAvailable = false;

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

    public void setIsRemoteAccessible(boolean value) {
        this.isRemoteAccessible = value;
    }

    public void setIsLocalAvailable(boolean value) {
        this.isLocalAvailable = value;
    }

    @Override
    protected boolean isRemoteResourceAccessible() {
        return this.isRemoteAccessible;
    }

    @Override
    protected boolean isLocalResourceAvailable() {
        return this.isLocalAvailable;
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
