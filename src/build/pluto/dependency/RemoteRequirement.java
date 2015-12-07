package build.pluto.dependency;

import java.io.File;
import java.io.IOException;

import org.sugarj.common.FileCommands;

import build.pluto.builder.BuildManager;

public abstract class RemoteRequirement implements Requirement {

  private static final long serialVersionUID = -637138545509445926L;

  public static final long CHECK_NEVER = -1L;
  public static final long CHECK_ALWAYS = 0L;

  private long consistencyCheckInterval;

  private File persistentPath;

  /**
   * @param persistentPath
   *          the file in which the timestamp of the last successful consistency
   *          check between remote and local resource was made.
   * @param consistencyCheckInterval
   *          the milliseconds how long the consistency check between remote and
   *          local resource are not made. 0L means it gets checked everytime
   *          and -1L means it does not get checked ever.
   */
  public RemoteRequirement(File persistentPath, long consistencyCheckInterval) {
    if (consistencyCheckInterval < CHECK_NEVER) {
      throw new IllegalArgumentException("consistencyCheckInterval has to be greater or equal than -1L");
    }
    this.persistentPath = persistentPath;
    this.consistencyCheckInterval = consistencyCheckInterval;
    long timestamp = getStartingTimestamp();
    this.writePersistentPath(timestamp);
  }

  /**
   * This implementation calls needsConsistencyCheck, isRemoteResourceAccessible,
   * isLocalResourceAvailable and isConsistentWithRemote
   */
  @Override
  public boolean isConsistent() {
    long timestamp = getStartingTimestamp();
    if (!needsConsistencyCheck(timestamp))
      return true;
    
    boolean accessible = isRemoteResourceAccessible();
    if (accessible && isConsistentWithRemote()) {
      writePersistentPath(timestamp);
      return true;
    }
    
    if (!accessible && isLocalResourceAvailable())
      return true;
    
    return false;
  }

  protected long getStartingTimestamp() {
    Thread currentThread = Thread.currentThread();
    return BuildManager.getStartingTimeOfBuildManager(currentThread);
  }

  /**
   * Checks if the remote resource can be accessed.
   * 
   * @return true if remote resourse can be accessed.
   */
  protected abstract boolean isRemoteResourceAccessible();

  /**
   * Checks if a version of the remote resource is available locally.
   * 
   * @return true if a version of the remote resourse is locally available.
   */
  protected abstract boolean isLocalResourceAvailable();

  /**
   * Checks if the local state is consistent with the remote state.
   * 
   * @return true if local state is consistent with remote state.
   */
  protected abstract boolean isConsistentWithRemote();

  /**
   * Checks if a consistencycheck needs to be made.
   * 
   * @param currentTime
   *          the time to check if the consistency needs to be checked.
   * @return true if a consistencycheck needs to be made.
   */
  private boolean needsConsistencyCheck(long currentTime) {
    if (consistencyCheckInterval == CHECK_NEVER)
      return false;
    
    if (!FileCommands.exists(persistentPath)) {
      writePersistentPath(currentTime);
      return true;
    }

    try {
      long lastConsistencyCheck = readPersistentPath();
      long afterInterval = lastConsistencyCheck + consistencyCheckInterval;
      // if afterInterval is non-positive overflow occured
      // can happen if consistencyCheckInterval is unusually big
      if (afterInterval > 0 && afterInterval < currentTime)
        return true;
    } catch (IOException e) {
      return true;
    }
    return false;
  }

  private long readPersistentPath() throws IOException {
    String persistentPathContent = FileCommands.readFileAsString(persistentPath);
    return Long.parseLong(persistentPathContent.replace("\n", ""));
  }

  private void writePersistentPath(long timeStamp) {
    try {
      if (!persistentPath.exists()) {
        FileCommands.createFile(persistentPath);
      }
      FileCommands.writeToFile(persistentPath, String.valueOf(timeStamp));
    } catch (IOException e) {
      throw new RuntimeException("Failed to write remote requirement time stamp.", e);
    }
  }
}
