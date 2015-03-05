package org.sugarj.cleardep.output;


public class None implements BuildOutput {

  private static final long serialVersionUID = -8968105966989986067L;

  private None() { }
  
  public final static None val = new None();

  @Override
  public boolean isConsistent() {
    return true;
  }
}
