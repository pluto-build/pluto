package build.pluto.stamp;

import org.sugarj.common.path.Path;

public class Util {
  public static boolean stampEqual(Stamp s, Path p) {
    return s.equals(s.getStamper().stampOf(p));
  }
}
