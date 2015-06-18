package build.pluto.output;

import java.io.Serializable;

public interface OutputStamp<OutT extends Output> extends Serializable {
  public boolean equals(OutputStamp<?> o);
  public OutputStamper<OutT> getStamper();
}
