package build.pluto.stamp;

import java.io.Serializable;

import build.pluto.output.OutputStamp;
import build.pluto.output.OutputStamper;

public class IgnoreOutputStamper implements OutputStamper<Serializable> {

  /**
   * 
   */
  private static final long serialVersionUID = 4432267738282131473L;

  public static final IgnoreOutputStamper instance = new IgnoreOutputStamper();

  private static final OutputStamp<Serializable> IGNORE_OUTPUT_STAMP = new IgnoreOutputStamp();

  private static class IgnoreOutputStamp implements OutputStamp<Serializable> {

    /**
     * 
     */
    private static final long serialVersionUID = 8496940185179778842L;

    @Override
    public OutputStamper<Serializable> getStamper() {
      return IgnoreOutputStamper.instance;
    }

    public boolean equals(Object o) {
      return o instanceof IgnoreOutputStamp;
    };

  };

  @Override
  public OutputStamp<Serializable> stampOf(Serializable p) {
    return IGNORE_OUTPUT_STAMP;
  }

}
