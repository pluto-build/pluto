package build.pluto.util;

import java.io.Serializable;

public class TraceData implements Serializable {
  private static final long serialVersionUID = -5140871031711169966L;

  /**
   * Local build execution duration in ms (not including times of required builds).
   */
  public final int localDuration;
  
  /**
   * Total build execution duration in ms (including times of required builds).
   */
  public final int totalDuration;
  
  public TraceData(int localDuration, int totalDuration) {
    this.localDuration = localDuration;
    this.totalDuration = totalDuration;
  }
}
