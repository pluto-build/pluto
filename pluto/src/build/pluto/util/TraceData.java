package build.pluto.util;

import java.io.Serializable;

public class TraceData implements Serializable {
  private static final long serialVersionUID = -5140871031711169966L;

  /**
   * Number of builds.
   */
  public final int builds;
  
  /**
   * Average local build duration over last {@link #builds} in ms (not including times of required builds).
   */
  public final int localDuration;
  /**
   * Variance of local build duration over last {@link #builds} in ms (not including times of required builds).
   */
  public final double localDurationVariance;
  
  /**
   * Average total build duration over last {@link #builds} in ms (including times of required builds).
   */
  public final int totalDuration;
  /**
   * Variance of total build duration over last {@link #builds} in ms (including times of required builds).
   */
  public final double totalDurationVariance;
  
  public TraceData(int builds, int localDuration, double localDurationVariance, int totalDuration, double totalDurationVariance) {
    this.builds = builds;
    this.localDuration = localDuration;
    this.localDurationVariance = localDurationVariance;
    this.totalDuration = totalDuration;
    this.totalDurationVariance = totalDurationVariance;
  }
}
