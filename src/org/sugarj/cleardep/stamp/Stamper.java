package org.sugarj.cleardep.stamp;

import java.io.Serializable;

import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public interface Stamper extends Serializable {
  public Stamp stampOf(Path p);
}
