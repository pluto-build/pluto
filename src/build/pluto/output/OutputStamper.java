package build.pluto.output;

import java.io.Serializable;

/**
 * @author Sebastian Erdweg
 */
public interface OutputStamper<Out extends Serializable> extends Serializable {
  public OutputStamp<Out> stampOf(Out p);
}
