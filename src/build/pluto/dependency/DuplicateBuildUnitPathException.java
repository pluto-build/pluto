package build.pluto.dependency;

public class DuplicateBuildUnitPathException extends RuntimeException {

  private static final long serialVersionUID = -613704339507902355L;

  public DuplicateBuildUnitPathException(String msg) {
    super(msg);
  }
  
}
