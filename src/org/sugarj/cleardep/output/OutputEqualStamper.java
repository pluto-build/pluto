package org.sugarj.cleardep.output;

import java.io.Serializable;
import java.util.Objects;

public class OutputEqualStamper<Out extends Serializable> implements OutputStamper<Out> {

  private static final long serialVersionUID = -820125647502953082L;
  
  private final static OutputEqualStamper<?> instance = new OutputEqualStamper<>();
  @SuppressWarnings("unchecked")
  public static <Out extends Serializable> OutputEqualStamper<Out> instance() { 
    return (OutputEqualStamper<Out>) instance;
  }
  
  private OutputEqualStamper() { }
  
  @Override
  public OutputStamp stampOf(Out out) {
    return new OutputEqualStamp(out);
  }
  
  public static class OutputEqualStamp implements OutputStamp {

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
