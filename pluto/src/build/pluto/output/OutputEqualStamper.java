package build.pluto.output;

import java.io.Serializable;
import java.util.Objects;

public class OutputEqualStamper implements OutputStamper<Serializable> {

  private static final long serialVersionUID = -820125647502953082L;
  
  private final static OutputEqualStamper instance = new OutputEqualStamper();
  
  public static OutputEqualStamper instance() { 
    return instance;
  }
  
  private OutputEqualStamper() { }
  
  @Override
  public OutputStamp<Serializable> stampOf(Serializable out) {
    return new OutputEqualStamp(out);
  }
  
  public static class OutputEqualStamp implements OutputStamp<Serializable> {

    private static final long serialVersionUID = -5404621797279839303L;

    private final Serializable out;
    public OutputEqualStamp(Serializable out) {
      this.out = out;
    }
    
    @Override
    public OutputStamper<Serializable> getStamper() {
      return instance();
    }
    
    @Override
    public boolean isConsistent(OutputStamp<?> o) {
      if (o instanceof OutputEqualStamp) {
        Serializable oout = ((OutputEqualStamp) o).out;
        if (out instanceof OutputTransient<?> && oout instanceof OutputTransient<?>)
          return true;
        return Objects.equals(out, oout);
      }
      return false;
    }
    
    @Override
    public boolean isConsistentInBuild(OutputStamp<?> o) {
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
