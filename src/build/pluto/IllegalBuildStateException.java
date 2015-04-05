package build.pluto;

public class IllegalBuildStateException extends IllegalStateException {

  /**
   * 
   */
  private static final long serialVersionUID = -5766758537778975592L;

  public IllegalBuildStateException() {
    super();
  }

  public IllegalBuildStateException(String message, Throwable cause) {
    super(message, cause);
  }

  public IllegalBuildStateException(String s) {
    super(s);
  }

  public IllegalBuildStateException(Throwable cause) {
    super(cause);
  }

}
