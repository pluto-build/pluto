package build.pluto.output;

import java.io.Serializable;

/**
 * @author Sebastian Erdweg
 */
public interface OutputStamper<OutT extends Serializable> extends Serializable {
  public OutputStamp<OutT> stampOf(OutT p);
}
