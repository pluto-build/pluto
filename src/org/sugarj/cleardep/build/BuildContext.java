package org.sugarj.cleardep.build;

import java.util.Map;

import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.path.Path;

public class BuildContext {
  private final Map<Path, Stamp> editedSourceFiles = null;
  
//  @SuppressWarnings("unchecked")
//  public <T> Builder<T> findBuildUnit(Object buildType, Class<T> inputType) {
//    List<Builder<?>> units = builders.get(buildType);
//    
//    if (units == null)
//      throw new IllegalArgumentException("Could not find builder of type " + buildType);
//    
//    for (Builder<?> unit : units)
//      if (unit.inputType().isAssignableFrom(inputType))
//        return (Builder<T>) unit;
//    
//    throw new IllegalArgumentException("Could not find builder " + buildType + " with input type " + inputType);
//  }
  
  public Map<Path, Stamp> getEditedSourceFiles() {
    return editedSourceFiles;
  }
}
