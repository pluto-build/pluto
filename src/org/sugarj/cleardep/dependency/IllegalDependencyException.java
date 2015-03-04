package org.sugarj.cleardep.dependency;

public class IllegalDependencyException extends RuntimeException {

  private static final long serialVersionUID = -613704339507902355L;

  public IllegalDependencyException(String msg) {
    super(msg);
  }
  
}
