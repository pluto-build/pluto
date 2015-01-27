package org.sugarj.cleardep.stamp;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.common.path.Path;

public class Util {
  public static boolean stampEqual(Stamp s, Path p) {
    return s.equals(s.getStamper().stampOf(p));
  }
  
  public static boolean stampEqual(ModuleStamp s, CompilationUnit m) {
    return s.equals(s.getModuleStamper().stampOf(m));
  }
}
