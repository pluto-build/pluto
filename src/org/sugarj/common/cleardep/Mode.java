package org.sugarj.common.cleardep;

import java.io.Serializable;


public interface Mode<E extends CompilationUnit> extends Serializable {
  public Mode<E> getModeForRequiredModules();
  public boolean canAccept(E e);
  public E accept(E e);
}
