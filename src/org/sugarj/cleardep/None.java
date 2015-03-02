package org.sugarj.cleardep;

import java.io.Serializable;

public class None implements Serializable {

  private static final long serialVersionUID = -8968105966989986067L;

  private None() { }
  
  public final static None val = new None();
}
