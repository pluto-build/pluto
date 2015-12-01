package build.pluto.dependency;

import java.io.IOException;
import java.io.Serializable;

import build.pluto.builder.BuildUnitProvider;

public interface Requirement extends Serializable {
  public static Requirement FALSE = new Requirement() {
    private static final long serialVersionUID = 6550975579546020627L;

    @Override
    public boolean tryMakeConsistent(BuildUnitProvider manager) throws IOException {
      return false;
    }

    @Override
    public boolean isConsistent() {
      return false;
    }

    public String toString() {
      return "FALSE";
    };
  };

  /**
   * @return true if this requirement is consistent.
   */
  public boolean isConsistent();

  /**
   * Try to make this requirement consistent, using the given build manager if needed.
   * 
   * @param manager The current build manager.
   * @return true if this requirement is or was made consistent.
   */
  public boolean tryMakeConsistent(BuildUnitProvider manager) throws IOException;
}
