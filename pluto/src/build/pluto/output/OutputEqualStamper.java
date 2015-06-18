package build.pluto.output;

import java.io.Serializable;
import java.util.Objects;

public class OutputEqualStamper implements OutputStamper<Output> {

  private static final long serialVersionUID = -820125647502953082L;
  
  private final static OutputEqualStamper instance = new OutputEqualStamper();
  
  public static OutputEqualStamper instance() { 
    return instance;
  }
  
  private OutputEqualStamper() { }
  
  @Override
  public OutputStamp<Output> stampOf(Output out) {
    return new OutputEqualStamp(out);
  }
  
  public static class OutputEqualStamp implements OutputStamp<Output> {

    private static final long serialVersionUID = -5404621797279839303L;

    private final Serializable out;
    public OutputEqualStamp(Serializable out) {
      this.out = out;
    }
    
    @Override
    public OutputStamper<Output> getStamper() {
      return instance();
    }
    
    @Override
    public boolean equals(OutputStamp<?> o) {
      if (o instanceof OutputEqualStamp) {
        Serializable oout = ((OutputEqualStamp) o).out;
        return Objects.equals(out, oout);
      }
      return false;
    }

    @Override
    public String toString() {
      return "OutputEqualsStamp(" + out + ")";
    }
  }
}
