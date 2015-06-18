package build.pluto.output;

import java.io.Serializable;

public class IgnoreOutputStamper implements OutputStamper<Serializable> {

  private static final long serialVersionUID = 4432267738282131473L;

  public static final IgnoreOutputStamper instance = new IgnoreOutputStamper();

  private static final OutputStamp<Serializable> IGNORE_OUTPUT_STAMP = new IgnoreOutputStamp();

  private static class IgnoreOutputStamp implements OutputStamp<Serializable> {

    private static final long serialVersionUID = 8496940185179778842L;

    @Override
    public OutputStamper<Serializable> getStamper() {
      return IgnoreOutputStamper.instance;
    }

    @Override
    public boolean isConsistent(OutputStamp<?> o) {
      return o instanceof IgnoreOutputStamp;
    };

    @Override
    public boolean isConsistentInBuild(OutputStamp<?> o) {
      return o instanceof IgnoreOutputStamp;
    };

  };

  @Override
  public OutputStamp<Serializable> stampOf(Serializable p) {
    return IGNORE_OUTPUT_STAMP;
  }

}
