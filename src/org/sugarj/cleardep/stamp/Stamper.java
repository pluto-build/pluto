package org.sugarj.cleardep.stamp;

import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public interface Stamper {
  public static final Stamper DEFAULT = ContentHashStamper.instance;
  
  public Stamp stampOf(Path p);
}
