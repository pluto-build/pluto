package build.pluto.output;

import java.io.Serializable;

public interface OutputStamp<OutT extends Serializable> extends Serializable {
  public boolean equals(Object o);
  public OutputStamper<OutT> getStamper();
}
