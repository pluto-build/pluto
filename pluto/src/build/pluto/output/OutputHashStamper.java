package build.pluto.output;

import java.util.Objects;

public class OutputHashStamper implements OutputStamper<Output> {

  private static final long serialVersionUID = -820125647502953082L;
  
  private final static OutputHashStamper instance = new OutputHashStamper();
  
  public static OutputHashStamper instance() { 
    return instance;
  }
  
  private OutputHashStamper() { }
  
  @Override
  public OutputStamp<Output> stampOf(Output out) {
    return new OutputHashStamp(Objects.hashCode(out));
  }
  
  public static class OutputHashStamp implements OutputStamp<Output> {

    private static final long serialVersionUID = -5404621797279839303L;

    private final int hash;
    public OutputHashStamp(int hash) {
      this.hash = hash;
    }
    
    @Override
    public OutputStamper<Output> getStamper() {
      return instance();
    }
    
    @Override
    public boolean equals(OutputStamp<?> o) {
      return o instanceof OutputHashStamp && ((OutputHashStamp) o).hash == hash;
    }

    @Override
    public String toString() {
      return "OutputHashStamp(" + hash + ")";
    }
  }
}
