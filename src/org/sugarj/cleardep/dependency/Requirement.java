package org.sugarj.cleardep.dependency;

import java.io.IOException;
import java.io.Serializable;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildUnitProvider;

public interface Requirement extends Serializable {
  public boolean isConsistent();
  public boolean isConsistentInBuild(BuildUnitProvider manager) throws IOException;
}
