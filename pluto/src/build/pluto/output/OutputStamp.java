package build.pluto.output;

import java.io.Serializable;

public interface OutputStamp extends Serializable {
  public boolean equals(OutputStamp o);
  public OutputStamper<Output> getStamper();
}
