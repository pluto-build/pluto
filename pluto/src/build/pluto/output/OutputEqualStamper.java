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
    public boolean equals(Object o) {
      return o instanceof OutputEqualStamp && Objects.equals(out, ((OutputEqualStamp) o).out);
    }
  }
}
