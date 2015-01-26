package org.sugarj.common.cleardep;

import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public interface Stamper {
  public static final Stamper DEFAULT = ContentHashStamper.instance;
  
  public Stamp stampOf(Path p);
  public Stamp stampOf(CompilationUnit m);
}
