package build.pluto.output;

import java.io.Serializable;

/**
 * @author Sebastian Erdweg
 */
public interface OutputStamper<OutT extends Output> extends Serializable {
  public OutputStamp stampOf(OutT p);
}
