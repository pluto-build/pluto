package build.pluto.dependency;

import java.io.File;
import java.util.Collection;

public class IllegalDependencyException extends RuntimeException {

  private static final long serialVersionUID = -613704339507902355L;

  public final Collection<File> deps;
  
  public IllegalDependencyException(Collection<File> deps, String msg) {
    super(msg);
    this.deps = deps;
  }
  
}
