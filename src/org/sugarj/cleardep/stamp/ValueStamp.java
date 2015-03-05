package org.sugarj.cleardep.stamp;

import java.util.Objects;

public class ValueStamp<T> implements Stamp {
  private static final long serialVersionUID = 3600975022352030761L;
  
  public final Stamper stamper;
  public final T val;
  
  public ValueStamp(Stamper stamper, T val) {
    this.stamper = stamper;
    this.val = val;
  }
  
  @Override
  public Stamper getStamper() {
    return stamper;
  }
  
  @Override
  public boolean equals(Object o) {
    return o instanceof ValueStamp && Objects.equals(val, ((ValueStamp<?>) o).val);
  }

}
