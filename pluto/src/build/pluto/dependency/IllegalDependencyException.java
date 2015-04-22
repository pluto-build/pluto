package build.pluto.dependency;

import java.nio.file.Path;

public class IllegalDependencyException extends RuntimeException {

  private static final long serialVersionUID = -613704339507902355L;

  public final Path dep;
  
  public IllegalDependencyException(Path dep, String msg) {
    super(msg);
    this.dep = dep;
  }
  
}
