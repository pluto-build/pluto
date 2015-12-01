package build.pluto.stamp;

import java.io.File;
import java.io.Serializable;

/**
 * @author Sebastian Erdweg
 */
public interface Stamper extends Serializable {
  public Stamp stampOf(File p);
}
