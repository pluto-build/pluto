package org.sugarj.cleardep.stamp;

import java.io.Serializable;

import org.sugarj.cleardep.CompilationUnit;

/**
 * @author Sebastian Erdweg
 */
public interface ModuleStamper extends Serializable {
  public static final ModuleStamper DEFAULT = ContentHashStamper.minstance;
  
  public ModuleStamp stampOf(CompilationUnit m);
}
