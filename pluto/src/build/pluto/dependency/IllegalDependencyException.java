package build.pluto.dependency;

import java.io.File;

public class IllegalDependencyException extends RuntimeException {

  private static final long serialVersionUID = -613704339507902355L;

  public final File dep;
  
  public IllegalDependencyException(File dep, String msg) {
    super(msg);
    this.dep = dep;
  }
  
}
