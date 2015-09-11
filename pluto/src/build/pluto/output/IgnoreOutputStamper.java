package build.pluto.output;


public class IgnoreOutputStamper implements OutputStamper<Output> {

  private static final long serialVersionUID = 4432267738282131473L;

  public static final IgnoreOutputStamper instance = new IgnoreOutputStamper();

  public static final OutputStamp IGNORE_OUTPUT_STAMP = new IgnoreOutputStamp();

  private static class IgnoreOutputStamp implements OutputStamp {

    private static final long serialVersionUID = 8496940185179778842L;

    @Override
    public OutputStamper<Output> getStamper() {
      return IgnoreOutputStamper.instance;
    }

    @Override
    public boolean equals(OutputStamp o) {
      return o instanceof IgnoreOutputStamp;
    };
  };

  @Override
  public OutputStamp stampOf(Output p) {
    return IGNORE_OUTPUT_STAMP;
  }

}
