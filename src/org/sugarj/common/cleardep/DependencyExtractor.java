package org.sugarj.common.cleardep;

import java.util.Set;

public interface DependencyExtractor {
  
  public Set<CompilationUnit> extractDependencies(CompilationUnit unit);

}
