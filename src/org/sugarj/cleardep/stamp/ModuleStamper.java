package org.sugarj.cleardep.stamp;

import org.sugarj.cleardep.CompilationUnit;

/**
 * @author Sebastian Erdweg
 */
public interface ModuleStamper {
  public static final ModuleStamper DEFAULT = ContentHashStamper.minstance;
  
  public ModuleStamp stampOf(CompilationUnit m);
}
