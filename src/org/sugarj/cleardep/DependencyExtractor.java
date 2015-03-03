package org.sugarj.cleardep;

import java.util.Set;

public interface DependencyExtractor {
  
  public Set<BuildUnit<?>> extractDependencies(BuildUnit<?> unit);

}
