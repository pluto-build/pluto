package build.pluto.dependency;

import java.io.IOException;
import java.io.Serializable;

import build.pluto.builder.BuildUnitProvider;

public interface Requirement extends Serializable {
  public static Requirement FALSE = new Requirement() {
    private static final long serialVersionUID = 6550975579546020627L;

    @Override
    public boolean isConsistentInBuild(BuildUnitProvider manager) throws IOException {
      return false;
    }
    
    @Override
    public boolean isConsistent() {
      return false;
    }
  };
  
  public boolean isConsistent();
  public boolean isConsistentInBuild(BuildUnitProvider manager) throws IOException;
}
