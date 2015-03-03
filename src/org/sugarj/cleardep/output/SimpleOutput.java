package org.sugarj.cleardep.output;

import java.io.Serializable;

public class SimpleOutput<T extends Serializable> implements BuildOutput {
  private static final long serialVersionUID = 7105793763157253892L;
  
  public final T val;
  
  public SimpleOutput(T val) {
    this.val = val;
  }
  
  @Override
  public boolean isConsistent() {
    return true;
  }

}
