package org.sugarj.cleardep;

import java.util.Map;

import org.sugarj.cleardep.build.BuildRequirement;

public interface DependencyExtractor {
  
  public Map<CompilationUnit, BuildRequirement<?, ?, ?, ?>> extractDependencies(CompilationUnit unit);

}
