package build.pluto.dependency;

import java.io.IOException;
import java.io.Serializable;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildUnitProvider;

public interface Requirement extends Serializable {
  public boolean isConsistent();
  public boolean isConsistentInBuild(BuildUnit<?> parent, BuildUnitProvider manager) throws IOException;
}