package org.sugarj.common.cleardep;

public class SimpleMode implements Mode<SimpleCompilationUnit> {

  private static final long serialVersionUID = 6963625915315242393L;

  @Override
  public Mode<SimpleCompilationUnit> getModeForRequiredModules() {
    return this;
  }

  @Override
  public boolean canAccept(SimpleCompilationUnit e) {
    return e.getMode() instanceof SimpleMode;
  }

  @Override
  public SimpleCompilationUnit accept(SimpleCompilationUnit e) {
    return e;
  }

}
