package build.pluto.output;

import java.io.Serializable;

public interface OutputStamp<OutT extends Serializable> extends Serializable {
  public boolean isConsistent(OutputStamp<?> o);
  public boolean isConsistentInBuild(OutputStamp<?> o);
  public OutputStamper<OutT> getStamper();
}
