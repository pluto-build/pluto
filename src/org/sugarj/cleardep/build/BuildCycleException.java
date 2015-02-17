package org.sugarj.cleardep.build;

public class BuildCycleException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = -8981404220171314788L;

  public BuildCycleException(String message) {
    super(message);
  }

}
